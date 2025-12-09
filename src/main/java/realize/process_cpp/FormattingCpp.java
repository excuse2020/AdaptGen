package realize.process_cpp;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.*;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.parser.*;

import java.util.*;
import java.util.stream.Collectors;

public class FormattingCpp {

    public static String formatCode(String code) {
        IASTTranslationUnit tu = parse(code, true);
        if (tu == null) tu = parse(code, false);
        String s = tu == null ? "" : formatAST(tu);
        return cleanOutput(s);
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

    private static String formatAST(IASTNode node) {
        if (node == null) return "";
        if (isProblemNode(node)) return "";

        if (node instanceof IASTTranslationUnit) {
            IASTTranslationUnit tu = (IASTTranslationUnit) node;

            IASTPreprocessorIncludeStatement[] includes = tu.getIncludeDirectives();
            StringBuilder sb = new StringBuilder();
            for (IASTPreprocessorIncludeStatement inc : includes) {
                sb.append(formatInclude(inc));
            }

            List<IASTDeclaration> decls = Arrays.stream(node.getChildren())
                    .filter(x -> x instanceof IASTDeclaration && !isProblemNode(x))
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

            for (IASTDeclaration t : sortByName(types)) sb.append(formatAST(t));
            for (IASTDeclaration v : sortByName(vars)) sb.append(formatAST(v));
            for (IASTDeclaration f : sortByName(funcs)) sb.append(formatAST(f));
            return sb.toString();
        }

        if (node instanceof IASTFunctionDefinition) {
            IASTFunctionDefinition f = (IASTFunctionDefinition) node;
            String ret = formatDeclSpecifier(f.getDeclSpecifier());
            String name = getDeclaratorName(f.getDeclarator());
            String params = getFunctionParams(f.getDeclarator());
            String body = formatAST(f.getBody()).replace("\n", "\n\t");
            return "\n" + ret + " " + name + "(" + params + ") {" + body + "\n}\n";
        }

        if (node instanceof IASTCompoundStatement) {
            IASTCompoundStatement b = (IASTCompoundStatement) node;
            return Arrays.stream(b.getStatements())
                    .filter(s -> !isProblemNode(s))
                    .map(FormattingCpp::formatAST)
                    .collect(Collectors.joining(""));
        }

        if (node instanceof IASTSimpleDeclaration) {
            IASTSimpleDeclaration d = (IASTSimpleDeclaration) node;

            if (d.getDeclSpecifier() instanceof ICPPASTCompositeTypeSpecifier) {
                return formatAST((ICPPASTCompositeTypeSpecifier) d.getDeclSpecifier());
            }
            if (d.getDeclSpecifier() instanceof IASTCompositeTypeSpecifier) {
                return formatAST((IASTCompositeTypeSpecifier) d.getDeclSpecifier());
            }
            if (d.getDeclSpecifier() instanceof IASTEnumerationSpecifier) {
                return formatAST((IASTEnumerationSpecifier) d.getDeclSpecifier());
            }

            String type = formatDeclSpecifier(d.getDeclSpecifier());
            IASTDeclarator[] ds = d.getDeclarators();
            String decls = Arrays.stream(ds).map(FormattingCpp::formatDeclarator).collect(Collectors.joining(", "));
            return "\n" + type + " " + decls + ";";
        }

        if (node instanceof IASTDeclarationStatement) {
            IASTDeclarationStatement s = (IASTDeclarationStatement) node;
            String inner = formatAST(s.getDeclaration());
            return inner.isEmpty() ? "" : "\n" + inner;
        }

        if (node instanceof IASTExpressionStatement) {
            IASTExpressionStatement s = (IASTExpressionStatement) node;
            return "\n" + formatExpression(s.getExpression()) + ";";
        }

        if (node instanceof IASTReturnStatement) {
            IASTReturnStatement r = (IASTReturnStatement) node;
            if (r.getReturnValue() == null) return "\nreturn;";
            return "\nreturn " + formatExpression(r.getReturnValue()) + ";";
        }

        if (node instanceof IASTIfStatement) {
            IASTIfStatement i = (IASTIfStatement) node;
            String cond = formatExpression(i.getConditionExpression());
            String thenS = formatAST(i.getThenClause()).replace("\n", "\n\t");
            IASTStatement elseS = i.getElseClause();
            if (elseS != null) {
                if (elseS instanceof IASTIfStatement) {
                    String elseIf = formatAST(elseS).replaceFirst("\n", "");
                    return "\nif (" + cond + ") {" + thenS + "\n} \nelse " + elseIf;
                }
                String ec = formatAST(elseS).replace("\n", "\n\t");
                return "\nif (" + cond + ") {" + thenS + "\n} \nelse {" + ec + "\n}";
            }
            return "\nif (" + cond + ") {" + thenS + "\n}";
        }

        if (node instanceof IASTWhileStatement) {
            IASTWhileStatement w = (IASTWhileStatement) node;
            return "\nwhile (" + formatExpression(w.getCondition()) + ") {" + formatAST(w.getBody()).replace("\n", "\n\t") + "\n}";
        }

        if (node instanceof IASTDoStatement) {
            IASTDoStatement d = (IASTDoStatement) node;
            return "\ndo {" + formatAST(d.getBody()).replace("\n", "\n\t") + "\n} while (" + formatExpression(d.getCondition()) + ");";
        }

        if (node instanceof IASTForStatement) {
            IASTForStatement f = (IASTForStatement) node;
            String init = formatForInit(f.getInitializerStatement());
            String cond = formatExpression(f.getConditionExpression());
            String iter = formatExpression(f.getIterationExpression());
            return "\nfor (" + init + "; " + cond + "; " + iter + ") {" + formatAST(f.getBody()).replace("\n", "\n\t") + "\n}";
        }

        if (node instanceof IASTSwitchStatement) {
            IASTSwitchStatement s = (IASTSwitchStatement) node;
            return "\nswitch (" + formatExpression(s.getControllerExpression()) + ") {" + formatAST(s.getBody()).replace("\n", "\n\t") + "\n}";
        }

        if (node instanceof IASTCaseStatement) {
            IASTCaseStatement c = (IASTCaseStatement) node;
            return "\ncase " + formatExpression(c.getExpression()) + ":";
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

        if (node instanceof IASTNullStatement) {
            return "\n;";
        }

        if (node instanceof IASTGotoStatement) {
            IASTGotoStatement g = (IASTGotoStatement) node;
            String label = g.getName() != null ? g.getName().toString() : "";
            return "\ngoto " + label + ";";
        }

        if (node instanceof ICPPASTAliasDeclaration) {
            ICPPASTAliasDeclaration ad = (ICPPASTAliasDeclaration) node;
            String aliasName = ad.getAlias() != null ? ad.getAlias().toString() : "";
            String mapped = ad.getMappingTypeId() != null ? formatTypeId(ad.getMappingTypeId()) : "";
            return "\nusing " + aliasName + " = " + mapped + ";";
        }

        if (node instanceof ICPPASTCompositeTypeSpecifier) {
            ICPPASTCompositeTypeSpecifier c = (ICPPASTCompositeTypeSpecifier) node;
            int key = c.getKey();
            String kind = (key == IASTCompositeTypeSpecifier.k_struct)
                    ? "struct"
                    : (key == IASTCompositeTypeSpecifier.k_union)
                        ? "union"
                        : "class";
            StringBuilder res = new StringBuilder("\n" + kind + " " + c.getName().toString() + " {");
            Arrays.stream(c.getMembers())
                    .filter(m -> !isProblemNode(m))
                    .map(m -> formatAST(m).replace("\n", "\n\t"))
                    .forEach(member -> res.append(member).append("\n"));
            return res.append("};\n").toString();
        }

        if (node instanceof IASTCompositeTypeSpecifier) {
            IASTCompositeTypeSpecifier c = (IASTCompositeTypeSpecifier) node;
            String kind = c.getKey() == IASTCompositeTypeSpecifier.k_struct ? "struct" : "union";
            StringBuilder res = new StringBuilder("\n" + kind + " " + c.getName().toString() + " {");
            Arrays.stream(c.getMembers())
                    .filter(m -> !isProblemNode(m))
                    .map(m -> formatAST(m).replace("\n", "\n\t"))
                    .forEach(member -> res.append(member).append("\n"));
            return res.append("};\n").toString();
        }

        if (node instanceof IASTEnumerationSpecifier) {
            IASTEnumerationSpecifier e = (IASTEnumerationSpecifier) node;
            StringBuilder res = new StringBuilder("\nenum " + e.getName().toString() + " {");
            IASTEnumerationSpecifier.IASTEnumerator[] es = e.getEnumerators();
            String items = Arrays.stream(es).map(x -> x.getName().toString()).collect(Collectors.joining(", "));
            res.append(items).append("};\n");
            return res.toString();
        }

        if (node instanceof ICPPASTVisibilityLabel) {
            ICPPASTVisibilityLabel v = (ICPPASTVisibilityLabel) node;
            String vis = (v.getVisibility() == ICPPASTVisibilityLabel.v_public) ? "public"
                    : (v.getVisibility() == ICPPASTVisibilityLabel.v_protected) ? "protected"
                    : "private";
            return "\n" + vis + ":";
        }

        if (node instanceof ICPPASTRangeBasedForStatement) {
            ICPPASTRangeBasedForStatement r = (ICPPASTRangeBasedForStatement) node;

            String varPart = "";
            IASTDeclaration decl = r.getDeclaration();
            if (decl instanceof IASTSimpleDeclaration) {
                IASTSimpleDeclaration d = (IASTSimpleDeclaration) decl;
                String type = formatDeclSpecifier(d.getDeclSpecifier());
                String name = "";
                IASTDeclarator[] ds = d.getDeclarators();
                if (ds != null && ds.length > 0) {
                    name = getDeclaratorName(ds[0]);
                }
                varPart = (type + " " + name).trim();
            } else if (decl != null) {
                varPart = decl.toString().replaceAll("[;\\s]+$", "");
            }

            String rangePart = formatInitializerClause(r.getInitializerClause());
            String body = formatAST(r.getBody()).replace("\n", "\n\t");
            return "\nfor (" + varPart + " : " + rangePart + ") {" + body + "\n}";
        }

        return node.toString();
    }

    private static String formatInclude(IASTPreprocessorIncludeStatement inc) {
        if (inc == null) return "";
        String name = inc.getName() != null ? inc.getName().toString() : "";
        if (name.isEmpty()) {
            String raw = inc.toString().trim();
            return "\n" + raw.replaceAll("\\s+$", "");
        }
        String target = inc.isSystemInclude() ? "<" + name + ">" : "\"" + name + "\"";
        return "\n#include " + target;
    }

    private static List<IASTDeclaration> sortByName(List<IASTDeclaration> decls) {
        return decls.stream().sorted(Comparator.comparing(FormattingCpp::declName)).collect(Collectors.toList());
    }

    private static String declName(IASTDeclaration d) {
        if (d instanceof IASTFunctionDefinition) return getDeclaratorName(((IASTFunctionDefinition) d).getDeclarator());
        if (d instanceof IASTSimpleDeclaration) {
            IASTDeclSpecifier spec = ((IASTSimpleDeclaration) d).getDeclSpecifier();
            if (spec instanceof IASTCompositeTypeSpecifier) return ((IASTCompositeTypeSpecifier) spec).getName().toString();
            if (spec instanceof IASTEnumerationSpecifier) return ((IASTEnumerationSpecifier) spec).getName().toString();
            IASTDeclarator[] ds = ((IASTSimpleDeclaration) d).getDeclarators();
            if (ds.length > 0) return getDeclaratorName(ds[0]);
        }
        return "";
    }

    private static String formatDeclSpecifier(IASTDeclSpecifier ds) {
        if (ds instanceof IASTNamedTypeSpecifier) {
            return ((IASTNamedTypeSpecifier) ds).getName().toString();
        }
        return ds.toString();
    }

    private static String formatDeclarator(IASTDeclarator d) {
        String name = getDeclaratorName(d);
        IASTInitializer init = d.getInitializer();
        String initS = "";
        if (init != null) {
            if (init instanceof IASTEqualsInitializer) {
                initS = " = " + formatInitializer(init);
            } else {
                initS = " " + formatInitializer(init);
            }
        }

        if (d instanceof IASTArrayDeclarator) {
            IASTArrayDeclarator a = (IASTArrayDeclarator) d;
            String dims = Arrays.stream(a.getArrayModifiers())
                    .map(m -> m instanceof IASTArrayModifier && ((IASTArrayModifier) m).getConstantExpression() != null
                            ? "[" + formatExpression(((IASTArrayModifier) m).getConstantExpression()) + "]"
                            : "[]")
                    .collect(Collectors.joining(""));
            return name + dims + initS;
        }

        if (d instanceof IASTFunctionDeclarator) {
            return name + "(" + getFunctionParams(d) + ")";
        }

        return name + initS;
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

    private static String getDeclaratorName(IASTDeclarator d) {
        if (d == null) return "";
        IASTName n = d.getName();
        if (n != null && n.toString().length() > 0) return n.toString();
        return Arrays.stream(d.getChildren())
                .filter(x -> x instanceof IASTDeclarator)
                .map(x -> getDeclaratorName((IASTDeclarator) x))
                .findFirst().orElse("");
    }

    private static String formatInitializer(IASTInitializer init) {
        if (init instanceof IASTEqualsInitializer) {
            return formatInitializerClause(((IASTEqualsInitializer) init).getInitializerClause());
        }
        if (init instanceof IASTInitializerList) {
            IASTInitializerList l = (IASTInitializerList) init;
            String items = Arrays.stream(l.getClauses())
                    .map(x -> formatInitializerClause((IASTInitializerClause) x))
                    .collect(Collectors.joining(", "));
            return "{" + items + "}";
        }
        if (init instanceof ICPPASTConstructorInitializer) {
            ICPPASTConstructorInitializer ci = (ICPPASTConstructorInitializer) init;
            IASTInitializerClause[] args = ci.getArguments();
            String items = Arrays.stream(args)
                    .map(FormattingCpp::formatInitializerClause)
                    .collect(Collectors.joining(", "));
            return "(" + items + ")";
        }
        return init.toString();
    }

    private static String formatInitializerClause(IASTInitializerClause clause) {
        if (clause == null) return "";
        if (clause instanceof IASTExpression) {
            return formatExpression((IASTExpression) clause);
        }
        if (clause instanceof IASTInitializerList) {
            IASTInitializerList l = (IASTInitializerList) clause;
            String items = Arrays.stream(l.getClauses())
                    .map(FormattingCpp::formatInitializerClause)
                    .collect(Collectors.joining(", "));
            return "{" + items + "}";
        }
        return clause.toString();
    }

    private static String formatExpression(IASTNode e) {
        if (e == null) return "";
        if (isProblemNode(e)) return "";

        if (e instanceof IASTExpressionList) {
            IASTExpressionList list = (IASTExpressionList) e;
            IASTExpression[] items = list.getExpressions();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < items.length; i++) {
                sb.append(formatExpression(items[i]));
                if (i + 1 < items.length) sb.append(", ");
            }
            return sb.toString();
        }

        if (e instanceof ICPPASTNewExpression) {
            ICPPASTNewExpression ne = (ICPPASTNewExpression) e;
            String type = ne.getTypeId() != null ? formatTypeId(ne.getTypeId()) : "";
            String init = ne.getInitializer() != null ? formatInitializer(ne.getInitializer()) : "";
            return ("new " + type + init).trim();
        }

        if (e instanceof ICPPASTDeleteExpression) {
            ICPPASTDeleteExpression de = (ICPPASTDeleteExpression) e;
            String target = de.getOperand() != null ? formatExpression(de.getOperand()) : "";
            String bracket = de.isVectored() ? "[]" : "";
            return ("delete" + bracket + " " + target).trim();
        }

        if (e instanceof IASTTypeIdExpression) {
            IASTTypeIdExpression te = (IASTTypeIdExpression) e;
            String type = te.getTypeId() != null ? formatTypeId(te.getTypeId()) : "";
            String op;
            switch (te.getOperator()) {
                case IASTTypeIdExpression.op_sizeof: op = "sizeof"; break;
                case IASTTypeIdExpression.op_typeid: op = "typeid"; break;
                case IASTTypeIdExpression.op_alignof: op = "alignof"; break;
                default: op = "/*typeid-expr*/"; break;
            }
            return op + "(" + type + ")";
        }

        if (e instanceof ICPPASTLambdaExpression) {
            ICPPASTLambdaExpression lam = (ICPPASTLambdaExpression) e;
            return formatLambdaSimple(lam);
        }

        if (e instanceof ICPPASTSimpleTypeConstructorExpression) {
            ICPPASTSimpleTypeConstructorExpression st = (ICPPASTSimpleTypeConstructorExpression) e;
            String type = formatDeclSpecifier(st.getDeclSpecifier());
            String init = st.getInitializer() != null ? formatInitializer(st.getInitializer()) : "()";
            return (type + init).trim();
        }

        if (e instanceof ICPPASTCastExpression) {
            ICPPASTCastExpression c = (ICPPASTCastExpression) e;
            String op = cppCastOpToString(c.getOperator());
            String type = formatTypeId(c.getTypeId());
            String expr = formatExpression(c.getOperand());
            return op + "<" + type + ">(" + expr + ")";
        }

        if (e instanceof IASTCastExpression) {
            IASTCastExpression c = (IASTCastExpression) e;
            String type = formatTypeId(c.getTypeId());
            String expr = formatExpression(c.getOperand());
            return "(" + type + ") " + expr;
        }

        if (e instanceof IASTLiteralExpression) {
            return e.toString();
        }
        if (e instanceof IASTIdExpression) {
            return ((IASTIdExpression) e).getName().toString();
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
                    return inner;
            }
        }

        if (e instanceof IASTBinaryExpression) {
            IASTBinaryExpression b = (IASTBinaryExpression) e;
            String lhs = formatExpression(b.getOperand1());
            String rhs = formatExpression(b.getOperand2());
            String op;
            switch (b.getOperator()) {
                case IASTBinaryExpression.op_assign: op = "="; break;
                case IASTBinaryExpression.op_plusAssign: op = "+="; break;
                case IASTBinaryExpression.op_minusAssign: op = "-="; break;
                case IASTBinaryExpression.op_multiplyAssign: op = "*="; break;
                case IASTBinaryExpression.op_divideAssign: op = "/="; break;
                case IASTBinaryExpression.op_moduloAssign: op = "%="; break;
                case IASTBinaryExpression.op_shiftLeftAssign: op = "<<="; break;
                case IASTBinaryExpression.op_shiftRightAssign: op = ">>="; break;
                case IASTBinaryExpression.op_binaryAndAssign: op = "&="; break;
                case IASTBinaryExpression.op_binaryOrAssign: op = "|="; break;
                case IASTBinaryExpression.op_binaryXorAssign: op = "^="; break;
                case IASTBinaryExpression.op_plus: op = "+"; break;
                case IASTBinaryExpression.op_minus: op = "-"; break;
                case IASTBinaryExpression.op_multiply: op = "*"; break;
                case IASTBinaryExpression.op_divide: op = "/"; break;
                case IASTBinaryExpression.op_modulo: op = "%"; break;
                case IASTBinaryExpression.op_equals: op = "=="; break;
                case IASTBinaryExpression.op_notequals: op = "!="; break;
                case IASTBinaryExpression.op_greaterThan: op = ">"; break;
                case IASTBinaryExpression.op_lessThan: op = "<"; break;
                case IASTBinaryExpression.op_greaterEqual: op = ">="; break;
                case IASTBinaryExpression.op_lessEqual: op = "<="; break;
                case IASTBinaryExpression.op_logicalAnd: op = "&&"; break;
                case IASTBinaryExpression.op_logicalOr: op = "||"; break;
                case IASTBinaryExpression.op_binaryAnd: op = "&"; break;
                case IASTBinaryExpression.op_binaryXor: op = "^"; break;
                case IASTBinaryExpression.op_binaryOr: op = "|"; break;
                case IASTBinaryExpression.op_shiftLeft: op = "<<"; break;
                case IASTBinaryExpression.op_shiftRight: op = ">>"; break;
                default:
                    return b.toString();
            }
            return (lhs + " " + op + " " + rhs).trim();
        }
        if (e instanceof IASTFunctionCallExpression) {
            IASTFunctionCallExpression c = (IASTFunctionCallExpression) e;
            String args = Arrays.stream(c.getArguments()).map(FormattingCpp::formatExpression).collect(Collectors.joining(", "));
            return formatExpression(c.getFunctionNameExpression()) + "(" + args + ")";
        }
        if (e instanceof IASTFieldReference) {
            IASTFieldReference f = (IASTFieldReference) e;
            String base = formatExpression(f.getFieldOwner());
            String sep = f.isPointerDereference() ? "->" : ".";
            String name = f.getFieldName().toString();
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
        if (e instanceof IASTInitializerList) {
            IASTInitializerList l = (IASTInitializerList) e;
            String items = Arrays.stream(l.getClauses())
                    .map(FormattingCpp::formatInitializerClause)
                    .collect(Collectors.joining(", "));
            return "{" + items + "}";
        }
        return e.toString();
    }


    private static String formatForInit(IASTStatement s) {
        if (s == null) return "";
        if (s instanceof IASTNullStatement) {
            return "";
        }
        if (s instanceof IASTDeclarationStatement) {
            return formatAST(((IASTDeclarationStatement) s).getDeclaration())
                    .replaceFirst("\n", "")
                    .replaceAll(";$", "");
        }
        if (s instanceof IASTExpressionStatement) {
            return formatExpression(((IASTExpressionStatement) s).getExpression());
        }
        return s.toString();
    }

    private static String cleanOutput(String s) {
        if (s == null || s.isEmpty()) return "";
        String[] lines = s.split("\n");
        List<String> out = new ArrayList<>(lines.length);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            String l = line.replaceAll("\\s+$", "");
            l = l.replaceAll("\\s+([\\)\\]\\,;])", "$1");
            l = l.replaceAll("([\\(\\[])[\\s]+", "$1");

            String indent = l.replaceFirst("^([\\t ]*).*", "$1");
            String body = l.substring(indent.length());
            body = body.replaceAll(" {2,}", " ");

            if (body.trim().isEmpty()) continue;

            out.add(indent + body);
        }
        return String.join("\n", out);
    }

    private static boolean isProblemNode(IASTNode node) {
    return node instanceof IASTProblem
            || node instanceof IASTProblemDeclaration
            || node instanceof IASTProblemStatement
            || node instanceof IASTProblemExpression
            || node.getClass().getSimpleName().contains("Problem");
    }

    private static String formatTypeId(IASTTypeId typeId) {
        if (typeId == null) return "";
        String spec = formatDeclSpecifier(typeId.getDeclSpecifier());
        IASTDeclarator decl = typeId.getAbstractDeclarator();
        if (decl != null) {
            String dec = formatDeclarator(decl);
            if (dec.isEmpty()) return spec;
            return (spec + " " + dec).trim();
        }
        return spec;
    }

    private static String cppCastOpToString(int op) {
        return switch (op) {
            case ICPPASTCastExpression.op_const_cast -> "const_cast";
            case ICPPASTCastExpression.op_dynamic_cast -> "dynamic_cast";
            case ICPPASTCastExpression.op_reinterpret_cast -> "reinterpret_cast";
            case ICPPASTCastExpression.op_static_cast -> "static_cast";
            default -> "static_cast";
        };
    }

    private static String formatLambdaSimple(ICPPASTLambdaExpression lam) {
        StringBuilder sb = new StringBuilder();
        sb.append("[]()");
        if (lam.getBody() != null) {
            String body = formatAST(lam.getBody());
            sb.append(" ").append(body);
        } else {
            sb.append(" {}");
        }
        return sb.toString();
    }
}