package realize.process_cpp;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.*;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.parser.*;

import java.util.*;
import java.util.stream.Collectors;

public class MatchExpCpp {

    public static String getMatchExpByCode(String code) {
        IASTTranslationUnit tu = parse(code, true);
        if (tu == null) tu = parse(code, false);
        String out = formatMaskAST(tu);
        // normalize: drop leading newline if present
        if (out.startsWith("\n")) out = out.substring(1);
        return out;
    }

    private static IASTTranslationUnit parse(String code, boolean cpp) {
        FileContent fc = FileContent.create("input." + (cpp ? "cpp" : "c"), code.toCharArray());
        IScannerInfo si = new ScannerInfo();
        IncludeFileContentProvider ifcp = IncludeFileContentProvider.getEmptyFilesProvider();
        IIndex idx = null;
        int opts = 0;
        IParserLogService log = new DefaultLogService();
        try {
            return (cpp ? GPPLanguage.getDefault() : GCCLanguage.getDefault())
                    .getASTTranslationUnit(fc, si, ifcp, idx, opts, log);
        } catch (Exception e) {
            return null;
        }
    }

    private static String indent(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replace("\n", "\n\t");
    }

    private static String formatMaskAST(IASTNode node) {
        if (node == null) return "";

        // Root: collect declarations, sort to stable output
        if (node instanceof IASTTranslationUnit) {
            IASTTranslationUnit tu = (IASTTranslationUnit) node;

            StringBuilder sb = new StringBuilder();
            IASTPreprocessorIncludeStatement[] includes = tu.getIncludeDirectives();
            for (IASTPreprocessorIncludeStatement inc : includes) {
                sb.append(formatMaskedInclude(inc));
            }

            List<IASTDeclaration> decls = Arrays.stream(node.getChildren())
                    .filter(x -> x instanceof IASTDeclaration)
                    .map(x -> (IASTDeclaration) x)
                    .collect(Collectors.toList());

            List<IASTDeclaration> types = decls.stream()
                    .filter(d -> {
                        if (!(d instanceof IASTSimpleDeclaration)) return false;
                        IASTDeclSpecifier spec = ((IASTSimpleDeclaration) d).getDeclSpecifier();
                        return spec instanceof IASTCompositeTypeSpecifier
                                || spec instanceof ICPPASTCompositeTypeSpecifier
                                || spec instanceof IASTEnumerationSpecifier;
                    })
                    .collect(Collectors.toList());

            List<IASTDeclaration> funcs = decls.stream()
                    .filter(d -> d instanceof IASTFunctionDefinition)
                    .collect(Collectors.toList());

            List<IASTDeclaration> vars = decls.stream()
                    .filter(d -> d instanceof IASTSimpleDeclaration && !types.contains(d))
                    .collect(Collectors.toList());

            for (IASTDeclaration t : sortByName(types)) sb.append(formatMaskAST(t));
            for (IASTDeclaration v : sortByName(vars)) sb.append(formatMaskAST(v));
            for (IASTDeclaration f : sortByName(funcs)) sb.append(formatMaskAST(f));
            return sb.toString();
        }

        // Method declaration: keep signature, mask body
        if (node instanceof IASTFunctionDefinition) {
            IASTFunctionDefinition f = (IASTFunctionDefinition) node;
            String ret = formatDeclSpecifier(f.getDeclSpecifier());
            String name = getDeclaratorName(f.getDeclarator());
            String params = getFunctionParams(f.getDeclarator());
            String body = indent(formatMaskAST(f.getBody()));
            return "\n" + ret + " " + name + "(" + params + ") {" + body + "\n}";
        }

        // Compound statement: concatenate masked inner statements
        if (node instanceof IASTCompoundStatement) {
            IASTCompoundStatement b = (IASTCompoundStatement) node;
            return Arrays.stream(b.getStatements())
                    .map(MatchExpCpp::formatMaskAST)
                    .collect(Collectors.joining(""));
        }

        // Simple declaration: class/struct/enum, or variable declaration(s)
        if (node instanceof IASTSimpleDeclaration) {
            IASTSimpleDeclaration d = (IASTSimpleDeclaration) node;

            // Treat struct/union/class/enum uniformly as "class"
            if (d.getDeclSpecifier() instanceof ICPPASTCompositeTypeSpecifier) {
                return formatClass((ICPPASTCompositeTypeSpecifier) d.getDeclSpecifier());
            }
            if (d.getDeclSpecifier() instanceof IASTCompositeTypeSpecifier) {
                return formatClass((IASTCompositeTypeSpecifier) d.getDeclSpecifier());
            }
            if (d.getDeclSpecifier() instanceof IASTEnumerationSpecifier) {
                return formatEnumAsClass((IASTEnumerationSpecifier) d.getDeclSpecifier());
            }

            // Variable declaration(s)
            String type = formatDeclSpecifier(d.getDeclSpecifier());
            String decls = Arrays.stream(d.getDeclarators())
                    .map(MatchExpCpp::formatMaskedDeclarator)
                    .collect(Collectors.joining(", "));
            String space = decls.isEmpty() ? "" : " ";
            return "\n" + type + space + decls + ";";
        }

        if (node instanceof IASTDeclarationStatement) {
            IASTDeclarationStatement s = (IASTDeclarationStatement) node;
            return "\n" + formatMaskAST(s.getDeclaration()).replaceFirst("^\n", "");
        }

        // Expression statements: assignment, method call, or generic Expression
        if (node instanceof IASTExpressionStatement) {
            IASTExpressionStatement s = (IASTExpressionStatement) node;
            IASTExpression e = s.getExpression();
            if (e instanceof IASTFunctionCallExpression) {
                return "\n" + formatMaskedCall((IASTFunctionCallExpression) e) + ";";
            }
            if (e instanceof IASTBinaryExpression) {
                IASTBinaryExpression b = (IASTBinaryExpression) e;
                if (isAssignmentOperator(b.getOperator())) {
                    String lhs = formatExpression(b.getOperand1());
                    String op = binaryOpToString(b.getOperator());
                    String rhs = maskExpressionToken(b.getOperand2());
                    return "\n" + lhs + " " + op + " " + rhs + ";";
                }
            }
            return "\nExpression;";
        }

        // Return statements: keep 'return', mask value by AST type
        if (node instanceof IASTReturnStatement) {
            IASTReturnStatement r = (IASTReturnStatement) node;
            if (r.getReturnValue() == null) return "\nreturn;";
            return "\nreturn " + maskExpressionToken(r.getReturnValue()) + ";";
        }

        // If / else-if / else with masked predicates
        if (node instanceof IASTIfStatement) {
            IASTIfStatement i = (IASTIfStatement) node;
            String thenS = indent(formatMaskAST(i.getThenClause()));
            IASTStatement elseS = i.getElseClause();

            String ifHeader = "\nif (...) {" + thenS + "\n}";

            if (elseS != null) {
                if (elseS instanceof IASTIfStatement) {
                    String elseChain = " \nelse " + formatMaskAST(elseS).replaceFirst("\n", "");
                    return ifHeader + elseChain;
                }
                String ec = indent(formatMaskAST(elseS));
                return ifHeader + " \nelse {" + ec + "\n}";
            }
            return ifHeader;
        }

        if (node instanceof IASTWhileStatement) {
            IASTWhileStatement w = (IASTWhileStatement) node;
            return "\nwhile (...) {" + indent(formatMaskAST(w.getBody())) + "\n}";
        }

        if (node instanceof IASTDoStatement) {
            IASTDoStatement d = (IASTDoStatement) node;
            return "\ndo {" + indent(formatMaskAST(d.getBody())) + "\n} while (...);";
        }

        if (node instanceof IASTForStatement) {
            IASTForStatement f = (IASTForStatement) node;
            return "\nfor (...) {" + indent(formatMaskAST(f.getBody())) + "\n}";
        }

        if (node instanceof ICPPASTRangeBasedForStatement) {
            ICPPASTRangeBasedForStatement rf = (ICPPASTRangeBasedForStatement) node;
            return "\nfor (...) {" + indent(formatMaskAST(rf.getBody())) + "\n}";
        }

        // Switch: mask controller expr with "..."
        if (node instanceof IASTSwitchStatement) {
            IASTSwitchStatement s = (IASTSwitchStatement) node;
            return "\nswitch (...) {" + indent(formatMaskAST(s.getBody())) + "\n}";
        }

        // case/default kept, but now typed explicitly
        if (node instanceof IASTCaseStatement) {
            IASTCaseStatement c = (IASTCaseStatement) node;
            return "\ncase Expression:";
        }
        if (node instanceof IASTDefaultStatement) {
            return "\ndefault:";
        }
        if (node instanceof IASTBreakStatement) {
            return "\nbreak;";
        }
        if (node instanceof IASTContinueStatement) {
            return "\ncontinue;";
        }
        if (node instanceof IASTLabelStatement) {
            IASTLabelStatement l = (IASTLabelStatement) node;
            return "\n" + l.getName().toString() + ":";
        }

        // Visibility labels: keep unchanged (public/private/protected)
        if (node instanceof ICPPASTVisibilityLabel) {
            ICPPASTVisibilityLabel v = (ICPPASTVisibilityLabel) node;
            String vis = v.getVisibility() == ICPPASTVisibilityLabel.v_public
                    ? "public"
                    : v.getVisibility() == ICPPASTVisibilityLabel.v_private
                        ? "private"
                        : "protected";
            return "\n" + vis + ":";
        }

        // Null statements (bare ';') -> Expression;
        if (node instanceof IASTNullStatement) {
            return "\nExpression;";
        }

        // Unhandled CDT internal cpp nodes -> treat as Expression per rule
        String cls = node.getClass().getName();
        if (cls.startsWith("org.eclipse.cdt.internal.core.dom.parser.cpp")) {
            if (node instanceof IASTStatement) {
                return "\nExpression;";
            }
            return "Expression";
        }

        // Fallback: treat as other
        return "\n" + node.toString().replace("\n", "");
    }

    // --- Declarators with masked initializers ---

    private static String formatMaskedDeclarator(IASTDeclarator d) {
        String name = getDeclaratorName(d);
        String ptrOps = Arrays.stream(d.getPointerOperators())
                .map(MatchExpCpp::formatPointerOperator)
                .collect(Collectors.joining(""));
        IASTInitializer init = d.getInitializer();
        String initS = "";
        if (init != null) {
            // replace init value by abstract token
            String maskedInit = maskInitializer(init);
            if (init instanceof IASTEqualsInitializer) initS = " = " + maskedInit;
            else initS = " " + maskedInit;
        }

        if (d instanceof IASTArrayDeclarator) {
            IASTArrayDeclarator a = (IASTArrayDeclarator) d;
            String dims = Arrays.stream(a.getArrayModifiers())
                    .map(m -> m instanceof IASTArrayModifier && ((IASTArrayModifier) m).getConstantExpression() != null
                            ? "[" + "Expression" + "]" : "[]")
                    .collect(Collectors.joining(""));
            return ptrOps + name + dims + initS;
        }

        if (d instanceof IASTFunctionDeclarator) {
            // method decl inside class: keep signature (no masking here)
            return ptrOps + name + "(" + getFunctionParams(d) + ")";
        }

        return ptrOps + name + initS;
    }

    private static String maskInitializer(IASTInitializer init) {
        if (init instanceof IASTEqualsInitializer) {
            IASTInitializerClause clause = ((IASTEqualsInitializer) init).getInitializerClause();
            return maskInitializerClause(clause);
        }
        if (init instanceof IASTInitializerList) {
            // Mask uniform to Expression
            return "Expression";
        }
        // Fallback
        return "Expression";
    }

    private static String maskInitializerClause(IASTInitializerClause clause) {
        if (clause == null) return "";
        if (clause instanceof IASTExpression) {
            return maskExpressionToken((IASTExpression) clause);
        }
        if (clause instanceof IASTInitializerList) {
            return "Expression";
        }
        return "Expression";
    }

    // --- Method call masking ---

    private static String formatMaskedCall(IASTFunctionCallExpression c) {
        String fn = formatExpression(c.getFunctionNameExpression());
        String args = Arrays.stream(c.getArguments()).map(MatchExpCpp::maskExpressionToken).collect(Collectors.joining(", "));
        return fn + "(" + args + ")";
    }

    // --- Expression printing for LHS, function names, etc. ---

    private static String maskExpressionToken(IASTNode e) {
        if (e == null) return "Expression";

        if (e instanceof IASTIdExpression) {
            IASTIdExpression ie = (IASTIdExpression) e;
            return wrapSimpleName(ie.getName().toString());
        }

        if (e instanceof IASTLiteralExpression) {
            IASTLiteralExpression le = (IASTLiteralExpression) e;
            switch (le.getKind()) {
                case IASTLiteralExpression.lk_integer_constant:
                case IASTLiteralExpression.lk_float_constant:
                    return "Num";
                case IASTLiteralExpression.lk_char_constant:
                    return "Char";
                case IASTLiteralExpression.lk_string_literal:
                    return "Str";
                case IASTLiteralExpression.lk_true:
                case IASTLiteralExpression.lk_false:
                    return "Bool";
                default:
                    return "Expression";
            }
        }
        if (e instanceof ICPPASTLambdaExpression) {
            return "LambdaExpression";
        }
        if (e instanceof IASTFunctionCallExpression) {
            return formatMaskedCall((IASTFunctionCallExpression) e);
        }
        return "Expression";
    }

    private static String formatDeclSpecifier(IASTDeclSpecifier ds) {
        if (ds instanceof IASTNamedTypeSpecifier) {
            return ((IASTNamedTypeSpecifier) ds).getName().toString();
        }
        return ds.toString();
    }

    private static String getFunctionParams(IASTDeclarator d) {
        if (d instanceof IASTFunctionDeclarator) {
            IASTParameterDeclaration[] ps = Arrays.stream(d.getChildren())
                    .filter(x -> x instanceof IASTParameterDeclaration)
                    .map(x -> (IASTParameterDeclaration) x)
                    .toArray(IASTParameterDeclaration[]::new);
            return Arrays.stream(ps)
                    .map(p -> formatDeclSpecifier(p.getDeclSpecifier()) + " " + getDeclaratorName(p.getDeclarator()))
                    .collect(Collectors.joining(", "));
        }
        return "";
    }

    private static String wrapSimpleName(String name) {
        if (name == null) return "SIMPLE_NAME$$";
        return "SIMPLE_NAME$" + name + "$";
    }

    private static String getDeclaratorName(IASTDeclarator d) {
        if (d == null) return "";
        IASTName n = d.getName();
        if (n != null && n.toString().length() > 0) return wrapSimpleName(n.toString());
        return Arrays.stream(d.getChildren())
                .filter(x -> x instanceof IASTDeclarator)
                .map(x -> getDeclaratorName((IASTDeclarator) x))
                .findFirst().orElse("");
    }

    private static List<IASTDeclaration> sortByName(List<IASTDeclaration> decls) {
        return decls.stream().sorted(Comparator.comparing(MatchExpCpp::declName)).collect(Collectors.toList());
    }

    private static String declName(IASTDeclaration d) {
        if (d instanceof IASTFunctionDefinition) return getDeclaratorName(((IASTFunctionDefinition) d).getDeclarator());
        if (d instanceof IASTSimpleDeclaration) {
            IASTDeclSpecifier spec = ((IASTSimpleDeclaration) d).getDeclSpecifier();
            if (spec instanceof IASTCompositeTypeSpecifier) return ((IASTCompositeTypeSpecifier) spec).getName().toString();
            if (spec instanceof ICPPASTCompositeTypeSpecifier) return ((ICPPASTCompositeTypeSpecifier) spec).getName().toString();
            if (spec instanceof IASTEnumerationSpecifier) return ((IASTEnumerationSpecifier) spec).getName().toString();
            IASTDeclarator[] ds = ((IASTSimpleDeclaration) d).getDeclarators();
            if (ds.length > 0) return getDeclaratorName(ds[0]);
        }
        return "";
    }

    // Expression formatter for LHS/method names
    private static String formatExpression(IASTNode e) {
        if (e == null) return "";
        if (e instanceof IASTLiteralExpression) {
            return e.toString();
        }
        if (e instanceof ICPPASTLambdaExpression) {
            return "LambdaExpression";
        }
        if (e instanceof IASTIdExpression) {
            String n = ((IASTIdExpression) e).getName().toString();
            return wrapSimpleName(n);
        }
        if (e instanceof IASTBinaryExpression) {
            IASTBinaryExpression b = (IASTBinaryExpression) e;
            return formatExpression(b.getOperand1()) + " " + binaryOpToString(b.getOperator()) + " " + formatExpression(b.getOperand2());
        }
       if (e instanceof IASTUnaryExpression) {
            IASTUnaryExpression u = (IASTUnaryExpression) e;
            String inner = formatExpression(u.getOperand());
            switch (u.getOperator()) {
                case IASTUnaryExpression.op_bracketedPrimary:
                    return "(" + inner + ")";
                case IASTUnaryExpression.op_sizeof:
                    return "sizeof(" + inner + ")";
                case IASTUnaryExpression.op_plus:
                    return "+" + inner;
                case IASTUnaryExpression.op_minus:
                    return "-" + inner;
                case IASTUnaryExpression.op_not:
                    return "!" + inner;
                case IASTUnaryExpression.op_tilde:
                    return "~" + inner;
                case IASTUnaryExpression.op_amper:
                    return "&" + inner;
                case IASTUnaryExpression.op_star:
                    return "*" + inner;
                case IASTUnaryExpression.op_prefixIncr:
                    return "++" + inner;
                case IASTUnaryExpression.op_prefixDecr:
                    return "--" + inner;
                case IASTUnaryExpression.op_postFixIncr:
                    return inner + "++";
                case IASTUnaryExpression.op_postFixDecr:
                    return inner + "--";
                case IASTUnaryExpression.op_sizeofParameterPack:
                    return "sizeof...(" + inner + ")";
                default:
                    String op = unaryOpToString(u.getOperator());
                    if (isPrefixUnary(u.getOperator())) return op + inner;
                    return inner + op;
            }
        }

        if (e instanceof IASTTypeIdExpression) {
            IASTTypeIdExpression te = (IASTTypeIdExpression) e;
            String op;
            switch (te.getOperator()) {
                case IASTTypeIdExpression.op_sizeof: op = "sizeof"; break;
                case IASTTypeIdExpression.op_alignof: op = "alignof"; break;
                case IASTTypeIdExpression.op_typeid: op = "typeid"; break;
                default: op = "/*typeid-expr*/"; break;
            }
            return op + "(Type)";
        }
        
        if (e instanceof IASTFunctionCallExpression) {
            IASTFunctionCallExpression c = (IASTFunctionCallExpression) e;
            String args = Arrays.stream(c.getArguments()).map(MatchExpCpp::formatExpression).collect(Collectors.joining(", "));
            return formatExpression(c.getFunctionNameExpression()) + "(" + args + ")";
        }
        if (e instanceof IASTFieldReference) {
            IASTFieldReference f = (IASTFieldReference) e;
            String base = formatExpression(f.getFieldOwner());
            String sep = f.isPointerDereference() ? "->" : ".";
            String name = wrapSimpleName(f.getFieldName().toString());
            return base + sep + name;
        }
        if (e instanceof IASTArraySubscriptExpression) {
            IASTArraySubscriptExpression a = (IASTArraySubscriptExpression) e;
            return formatExpression(a.getArrayExpression()) + "[" + formatExpression(a.getSubscriptExpression()) + "]";
        }
        if (e instanceof IASTConditionalExpression) {
            IASTConditionalExpression c = (IASTConditionalExpression) e;
            return formatExpression(c.getLogicalConditionExpression()) + " ? " +
                    formatExpression(c.getPositiveResultExpression()) + " : " +
                    formatExpression(c.getNegativeResultExpression());
        }
        return e.toString();
    }

    private static boolean isAssignmentOperator(int op) {
        return op == IASTBinaryExpression.op_assign
                || op == IASTBinaryExpression.op_multiplyAssign
                || op == IASTBinaryExpression.op_divideAssign
                || op == IASTBinaryExpression.op_moduloAssign
                || op == IASTBinaryExpression.op_plusAssign
                || op == IASTBinaryExpression.op_minusAssign
                || op == IASTBinaryExpression.op_shiftLeftAssign
                || op == IASTBinaryExpression.op_shiftRightAssign
                || op == IASTBinaryExpression.op_binaryAndAssign
                || op == IASTBinaryExpression.op_binaryXorAssign
                || op == IASTBinaryExpression.op_binaryOrAssign;
    }

    private static String binaryOpToString(int op) {
        switch (op) {
            case IASTBinaryExpression.op_multiply: return "*";
            case IASTBinaryExpression.op_divide: return "/";
            case IASTBinaryExpression.op_modulo: return "%";
            case IASTBinaryExpression.op_plus: return "+";
            case IASTBinaryExpression.op_minus: return "-";
            case IASTBinaryExpression.op_shiftLeft: return "<<";
            case IASTBinaryExpression.op_shiftRight: return ">>";
            case IASTBinaryExpression.op_lessThan: return "<";
            case IASTBinaryExpression.op_greaterThan: return ">";
            case IASTBinaryExpression.op_lessEqual: return "<=";
            case IASTBinaryExpression.op_greaterEqual: return ">=";
            case IASTBinaryExpression.op_equals: return "==";
            case IASTBinaryExpression.op_notequals: return "!=";
            case IASTBinaryExpression.op_binaryAnd: return "&";
            case IASTBinaryExpression.op_binaryXor: return "^";
            case IASTBinaryExpression.op_binaryOr: return "|";
            case IASTBinaryExpression.op_logicalAnd: return "&&";
            case IASTBinaryExpression.op_logicalOr: return "||";
            case IASTBinaryExpression.op_assign: return "=";
            case IASTBinaryExpression.op_plusAssign: return "+=";
            case IASTBinaryExpression.op_minusAssign: return "-=";
            case IASTBinaryExpression.op_multiplyAssign: return "*=";
            case IASTBinaryExpression.op_divideAssign: return "/=";
            case IASTBinaryExpression.op_moduloAssign: return "%=";
            case IASTBinaryExpression.op_binaryAndAssign: return "&=";
            case IASTBinaryExpression.op_binaryOrAssign: return "|=";
            case IASTBinaryExpression.op_binaryXorAssign: return "^=";
            case IASTBinaryExpression.op_shiftLeftAssign: return "<<=";
            case IASTBinaryExpression.op_shiftRightAssign: return ">>=";
            default: return "?";
        }
    }

    private static boolean isPrefixUnary(int op) {
        switch (op) {
            case IASTUnaryExpression.op_prefixIncr:
            case IASTUnaryExpression.op_prefixDecr:
            case IASTUnaryExpression.op_sizeof:
            case IASTUnaryExpression.op_alignOf:
            case IASTUnaryExpression.op_plus:
            case IASTUnaryExpression.op_minus:
            case IASTUnaryExpression.op_star:
            case IASTUnaryExpression.op_amper:
            case IASTUnaryExpression.op_tilde:
            case IASTUnaryExpression.op_not:
            case IASTUnaryExpression.op_bracketedPrimary:
                return true;
            default:
                return false;
        }
    }

    private static String unaryOpToString(int op) {
        switch (op) {
            case IASTUnaryExpression.op_prefixIncr: return "++";
            case IASTUnaryExpression.op_prefixDecr: return "--";
            case IASTUnaryExpression.op_postFixIncr: return "++";
            case IASTUnaryExpression.op_postFixDecr: return "--";
            case IASTUnaryExpression.op_sizeof: return "sizeof";
            case IASTUnaryExpression.op_alignOf: return "alignof";
            case IASTUnaryExpression.op_plus: return "+";
            case IASTUnaryExpression.op_minus: return "-";
            case IASTUnaryExpression.op_star: return "*";
            case IASTUnaryExpression.op_amper: return "&";
            case IASTUnaryExpression.op_tilde: return "~";
            case IASTUnaryExpression.op_not: return "!";
            default: return "?";
        }
    }

    // --- Class/struct/enum as class ---
    private static String formatClass(ICPPASTCompositeTypeSpecifier c) {
        String name = c.getName().toString();
        StringBuilder res = new StringBuilder("\nclass " + name + " {");
        for (IASTDeclaration m : c.getMembers()) {
            res.append(indent(formatMaskAST(m)));
        }
        return res.append("\n};").toString();
    }

    private static String formatClass(IASTCompositeTypeSpecifier c) {
        String name = c.getName().toString();
        StringBuilder res = new StringBuilder("\nclass " + name + " {");
        for (IASTDeclaration m : c.getMembers()) {
            res.append(indent(formatMaskAST(m)));
        }
        return res.append("\n};").toString();
    }

    private static String formatEnumAsClass(IASTEnumerationSpecifier e) {
        String name = e.getName().toString();
        String items = Arrays.stream(e.getEnumerators())
                .map(x -> x.getName().toString())
                .collect(Collectors.joining(", "));
        return "\nclass " + name + " {" + indent("\n" + items + ";") + "\n};";
    }

    // Pointer operator formatting to avoid internal class names
    private static String formatPointerOperator(IASTPointerOperator op) {
        if (op instanceof IASTPointer) return "*";
        if (op instanceof ICPPASTReferenceOperator) {
            ICPPASTReferenceOperator rop = (ICPPASTReferenceOperator) op;
            return rop.isRValueReference() ? "&&" : "&";
        }
        String s = op.toString();
        if (s == null || s.contains("@")) return "*";
        return s;
    }

    private static String formatMaskedInclude(IASTPreprocessorIncludeStatement inc) {
        return inc == null ? "" : "\n#include LIB";
    }
}