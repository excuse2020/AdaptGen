package realize.process;

import org.eclipse.jdt.core.dom.*;
import realize.utils.ASTUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Formatting {

    public static String formatCode(String code) {
        ASTNode ast = ASTUtils.getASTNode(code);
        return formatAST(ast);
    }

    private static String formatAST(ASTNode ast) {

        if (ast == null) return "";

        //System.out.println(ast.getNodeType() + ": " + ast);

        if (ast.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION) {
            //System.out.println("\nANONYMOUS_CLASS_DECLARATION:\n" + ast);
            return "";
        }

        if (ast.getNodeType() == ASTNode.ARRAY_ACCESS) {
            //System.out.println("\nARRAY_ACCESS:\n" + ast);
            ArrayAccess a = ((ArrayAccess) ast);
            return a.getArray() + "[" + a.getIndex() + "]";
        }

        if (ast.getNodeType() == ASTNode.ARRAY_CREATION) {
            //System.out.println("\nARRAY_CREATION:\n" + ast);
            ArrayCreation a = (ArrayCreation) ast;
            int size = a.dimensions().size();

            String d = IntStream.range(0, size).mapToObj(x -> "[" + formatAST((ASTNode) a.dimensions().get(x)) + "]").collect(Collectors.joining(""));

            return "new " + a.getType().toString().replace("[]", "") + d + formatAST(a.getInitializer());
        }

        if (ast.getNodeType() == ASTNode.ARRAY_INITIALIZER) {
            //System.out.println("\nARRAY_INITIALIZER:\n" + ast);
            ArrayInitializer a = (ArrayInitializer) ast;
            String e = (String) a.expressions().stream().map(x -> formatAST((Expression) x)).collect(Collectors.joining(", "));
            return "{" + e + "}";
        }

        if (ast.getNodeType() == ASTNode.ARRAY_TYPE) {
            //System.out.println("\nARRAY_TYPE:\n" + ast);
            ArrayType a = (ArrayType) ast;
            int size = a.dimensions().size();
            String d = IntStream.range(0, size).mapToObj(x -> "[]").collect(Collectors.joining(""));
            return formatAST(a.getElementType()) + d;
        }

        if (ast.getNodeType() == ASTNode.ASSERT_STATEMENT) {
            //System.out.println("\nASSERT_STATEMENT:\n" + ast);
            AssertStatement a = (AssertStatement) ast;
            return "\nassert " + formatAST(a.getExpression()) + ";";
        }

        if (ast.getNodeType() == ASTNode.ASSIGNMENT) {
            //System.out.println("\nASSIGNMENT:\n" + ast);
            Assignment node = (Assignment) ast;
            Expression l = node.getLeftHandSide();
            String sl = formatAST(l);
            Expression r = node.getRightHandSide();
            String sr = formatAST(r);
            return sl + " " + node.getOperator() + " " + sr;
        }

        if (ast.getNodeType() == ASTNode.BLOCK) {
            //System.out.println("\nBLOCK:\n" + ast);
            Block b = (Block) ast;
            return  ((String) b.statements().stream().map(x -> formatAST((ASTNode) x)).collect(Collectors.joining("")));
        }

        if (ast.getNodeType() == ASTNode.BOOLEAN_LITERAL) {
            //System.out.println("\nBOOLEAN_LITERAL:\n" + ast);
            return ast.toString();
        }

        if (ast.getNodeType() == ASTNode.BREAK_STATEMENT) {
            //System.out.println("\nBREAK_STATEMENT:\n" + ast);
            return "\nbreak;";
        }

        if (ast.getNodeType() == ASTNode.CAST_EXPRESSION) {
            //System.out.println("\nCAST_EXPRESSION:\n" + ast);
            CastExpression c = (CastExpression) ast;
            return "(" + formatAST(c.getType()) + ") " + formatAST(c.getExpression());
        }

        if (ast.getNodeType() == ASTNode.CATCH_CLAUSE) {
            //System.out.println("\nCATCH_CLAUSE:\n" + ast);
            CatchClause c = (CatchClause) ast;
            //TODO
        }

        if (ast.getNodeType() == ASTNode.CHARACTER_LITERAL) {
            //System.out.println("\nCHARACTER_LITERAL:\n" + ast);
            return ast.toString();
        }

        if (ast.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
            //System.out.println("\nCLASS_INSTANCE_CREATION:\n" + ast);
            ClassInstanceCreation c = (ClassInstanceCreation) ast;
            //System.out.println(c.getExpression());
            return "new " + formatAST(c.getType()) + "(" + getArgs(c.arguments()) + ")";
        }

        if (ast.getNodeType() == ASTNode.COMPILATION_UNIT) {
            //System.out.println("\nCOMPILATION_UNIT:\n" + ast);
            //TODO
            CompilationUnit c = (CompilationUnit) ast;
            return "";
        }

        if (ast.getNodeType() == ASTNode.CONDITIONAL_EXPRESSION) {
            //System.out.println("CONDITIONAL_EXPRESSION:\n" + ast);
            ConditionalExpression c = (ConditionalExpression) ast;
            return formatAST(c.getExpression()) + " ? " + formatAST(c.getThenExpression()) + " : " + formatAST(c.getElseExpression());
        }

        if (ast.getNodeType() == ASTNode.CONSTRUCTOR_INVOCATION) {
            //System.out.println("\nCONSTRUCTOR_INVOCATION:\n" + ast);
            //TODO
            ConstructorInvocation c = (ConstructorInvocation) ast;
            return "";
        }

        if (ast.getNodeType() == ASTNode.CONTINUE_STATEMENT) {
            //System.out.println("\nCONTINUE_STATEMENT:\n" + ast);
            return "\ncontinue;";
        }

        if (ast.getNodeType() == ASTNode.DO_STATEMENT) {
            //System.out.println("\nDO_STATEMENT:\n" + ast);
            DoStatement c = (DoStatement) ast;
            return "\ndo {\n" + formatAST(c.getBody()).replace("\n", "\n\t") + "\n} while (" + formatAST(c.getExpression()) + ")";
        }

        if (ast.getNodeType() == ASTNode.EMPTY_STATEMENT) {
            //System.out.println("\nEMPTY_STATEMENT:\n" + ast);
            return "";
        }

        if (ast.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
            //System.out.println("\nEXPRESSION_STATEMENT:\n" + ast);
            ExpressionStatement e = (ExpressionStatement) ast;
            return "\n" + formatAST(e.getExpression()) + ";";
        }

        if (ast.getNodeType() == ASTNode.FIELD_ACCESS) {
            //System.out.println("\nFIELD_ACCESS:\n" + ast);
            FieldAccess f = (FieldAccess) ast;
            return f.getExpression() + "." + f.getName();
        }

        if (ast.getNodeType() == ASTNode.FIELD_DECLARATION) {
            //System.out.println("\nFIELD_DECLARATION:\n" + ast);
            FieldDeclaration f = (FieldDeclaration) ast;
            if (((VariableDeclarationFragment) (f.fragments().get(0))).getInitializer() == null) {
                return "\n\t" + formatAST(f.getType()) + " " + f.fragments().stream().map(x -> ((VariableDeclarationFragment) x).getName().getIdentifier()).collect(Collectors.joining(", "));
            }
            return "\n\t" + formatAST(f.getType()) + " " + f.fragments().stream().map(x ->  ((VariableDeclarationFragment) x).getName().getIdentifier() + " = "
                     + formatAST(((VariableDeclarationFragment) x).getInitializer())).collect(Collectors.joining(", "));
        }

        if (ast.getNodeType() == ASTNode.FOR_STATEMENT) {
            //System.out.println("\nFOR_STATEMENT:\n" + ast);
            ForStatement f = (ForStatement) ast;
            String inits = (String) f.initializers().stream().map(x -> formatAST(((Expression) x))).collect(Collectors.joining(", "));
            String exp = formatAST(f.getExpression());
            String updates = (String) f.updaters().stream().map(x -> formatAST(((Expression) x))).collect(Collectors.joining(", "));
            return "\nfor (" + inits + "; " + exp + "; " + updates  + ") {" + formatAST(f.getBody()).replace("\n", "\n\t") + "\n}";
        }

        if (ast.getNodeType() == ASTNode.IF_STATEMENT) {
            //System.out.println("\nIF_STATEMENT:\n" + ast);
            IfStatement i = (IfStatement) ast;
            Statement elseStatement = i.getElseStatement();
            if (elseStatement != null) {
                if (elseStatement.getNodeType() == ASTNode.IF_STATEMENT)
                    return "\nif (" + formatAST(i.getExpression()) + ") {" + formatAST(i.getThenStatement()).replace("\n", "\n\t") + "\n} \nelse " + formatAST(elseStatement).replaceFirst("\n", "");
                return "\nif (" + formatAST(i.getExpression()) + ") {" + formatAST(i.getThenStatement()).replace("\n", "\n\t") + "\n} \nelse {" + formatAST(elseStatement).replace("\n", "\n\t") + "\n}";
            }
            return "\nif (" + formatAST(i.getExpression()) + ") {" + formatAST(i.getThenStatement()).replace("\n", "\n\t") + "\n}";
        }

        if (ast.getNodeType() == ASTNode.IMPORT_DECLARATION) {
            //System.out.println("\nIMPORT_DECLARATION:\n" + ast);
            return "";
//            return ast.toString();
        }

        if (ast.getNodeType() == ASTNode.INFIX_EXPRESSION) {
            //System.out.println("\nINFIX_EXPRESSION:\n" + ast);
            InfixExpression i = (InfixExpression) ast;
            //System.out.println("*1");
            //System.out.println(i.getOperator().toString());
            return formatAST(i.getLeftOperand()) + " " + i.getOperator().toString() + " " + formatAST(i.getRightOperand());
        }

        if (ast.getNodeType() == ASTNode.INITIALIZER) {
            //TODO
            //System.out.println("\nINITIALIZER:\n" + ast);
            Initializer i = (Initializer) ast;
            return formatAST(i.getBody());
        }

        if (ast.getNodeType() == ASTNode.JAVADOC) {
            //System.out.println("\nJAVADOC:\n" + ast);
            return "";
        }

        if (ast.getNodeType() == ASTNode.LABELED_STATEMENT) {
            //System.out.println("\nLABELED_STATEMENT:\n" + ast);
            return ast.toString();
        }

        if (ast.getNodeType() == ASTNode.METHOD_DECLARATION) {
            //System.out.println("\nMETHOD_DECLARATION:\n" + ast);
            MethodDeclaration m = (MethodDeclaration) ast;
            String paras = (String) m.parameters().stream().map(x -> formatAST(((SingleVariableDeclaration) x))).collect(Collectors.joining(", "));
            return "\n" + formatAST(m.getReturnType2()) + " " + formatAST(m.getName()) + "(" + paras + ") {"
                    + formatAST(m.getBody()).replace("\n", "\n\t") + "\n}";
        }

        if (ast.getNodeType() == ASTNode.METHOD_INVOCATION) {
            //System.out.println("\nMETHOD_INVOCATION:\n");
            MethodInvocation mi = (MethodInvocation) ast;
            String args = (String) mi.arguments().stream().map(x -> formatAST((ASTNode) x)).collect(Collectors.joining(","));
            String s = formatAST(mi.getExpression());
            if (mi.getExpression() == null) {
                return formatAST(mi.getName()) + "(" + args + ")";
            }
            return s + "." + (s.equals(mi.getExpression().toString()) ? mi.getName() : formatAST(mi.getName())) + "(" + args + ")";
        }

        if (ast.getNodeType() == ASTNode.NULL_LITERAL) {
            //System.out.println("\nNULL_LITERAL:\n" + ast);
            return "null";
        }

        if (ast.getNodeType() == ASTNode.NUMBER_LITERAL) {
            //System.out.println("\nNUMBER_LITERAL:\n" + ast);
            return ast.toString();
        }

        if (ast.getNodeType() == ASTNode.PACKAGE_DECLARATION) {
            //System.out.println("\nPACKAGE_DECLARATION:\n" + ast);
            return "";
        }

        if (ast.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
            //System.out.println("\nPARENTHESIZED_EXPRESSION:\n" + ast);
            ParenthesizedExpression p = (ParenthesizedExpression) ast;
            return "(" + formatAST(p.getExpression()) + ")";
        }

        if (ast.getNodeType() == ASTNode.POSTFIX_EXPRESSION) {
            //System.out.println("\nPOSTFIX_EXPRESSION:\n" + ast);
            PostfixExpression p = (PostfixExpression) ast;
            return  formatAST(p.getOperand()) + p.getOperator().toString();
        }

        if (ast.getNodeType() == ASTNode.PREFIX_EXPRESSION) {
            //System.out.println("\nPREFIX_EXPRESSION:\n" + ast);
            PrefixExpression p = (PrefixExpression) ast;
            return p.getOperator().toString() + p.getOperand();
        }

        if (ast.getNodeType() == ASTNode.PRIMITIVE_TYPE) {
            //System.out.println("\nPRIMITIVE_TYPE:\n" + ast);
            return ast.toString();
        }

        if (ast.getNodeType() == ASTNode.QUALIFIED_NAME) {
            //System.out.println("\nQUALIFIED_NAME:\n" + ast);
            QualifiedName q = (QualifiedName) ast;
            return formatAST(q.getQualifier()) + "." + formatAST(q.getName());
        }

        if (ast.getNodeType() == ASTNode.RETURN_STATEMENT) {
            //System.out.println("\nRETURN_STATEMENT:\n" + ast);
            ReturnStatement r = (ReturnStatement) ast;
            Expression e = r.getExpression();
            if (e != null) {
                return "\nreturn " + formatAST(e) + ";";
            }
            return "\nreturn;";
        }

        if (ast.getNodeType() == ASTNode.SIMPLE_NAME) {
            //System.out.println("\nSIMPLE_NAME:\n" + ast);
            SimpleName s = (SimpleName) ast;
            return s.getIdentifier();
        }

        if (ast.getNodeType() == ASTNode.SIMPLE_TYPE) {
            //System.out.println("\nSIMPLE_TYPE:\n" + ast);
            SimpleType s = (SimpleType) ast;
            return s.toString();
        }

        if (ast.getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION) {
            //System.out.println("\nSINGLE_VARIABLE_DECLARATION:\n" + ast);
            SingleVariableDeclaration s = (SingleVariableDeclaration) ast;
            String res = formatAST(s.getType()) + " " + formatAST(s.getName());
            if (s.getInitializer() != null) {
                res = res + " = " + formatAST(s.getInitializer());
            }
            return res;
        }

        if (ast.getNodeType() == ASTNode.STRING_LITERAL) {
            //System.out.println("\nSTRING_LITERAL:\n" + ast);
            return ast.toString();
        }

        if (ast.getNodeType() == ASTNode.SUPER_CONSTRUCTOR_INVOCATION) {
            //System.out.println("\nSUPER_CONSTRUCTOR_INVOCATION:\n" + ast);
            SuperConstructorInvocation s = (SuperConstructorInvocation) ast;
            String args = (String) s.arguments().stream().map(x -> formatAST((ASTNode) x)).collect(Collectors.joining(", "));
            return "super(" + args + ")";
        }

        if (ast.getNodeType() == ASTNode.SUPER_FIELD_ACCESS) {
            //System.out.println("\nSUPER_FIELD_ACCESS:\n" + ast);
            SuperFieldAccess s = (SuperFieldAccess) ast;
            //System.out.println(s.getName());
            return "super." + formatAST(s.getName());
        }

        if (ast.getNodeType() == ASTNode.SUPER_METHOD_INVOCATION) {
            //System.out.println("\nSUPER_METHOD_INVOCATION:\n" + ast);
            SuperMethodInvocation s = (SuperMethodInvocation) ast;
            return "super." + formatAST(s.getName()) + "(" + getArgs(s.arguments()) + ")";
        }

        if (ast.getNodeType() == ASTNode.SWITCH_CASE) {
            //System.out.println("\nSWITCH_CASE:\n" + ast);
            SwitchCase s = (SwitchCase) ast;
            if (s.isDefault()) {
                return "default: ";
            }
            String exps = ((String) s.expressions().stream().map(x -> formatAST((ASTNode) x)).collect(Collectors.joining(":\n", "", ":")));
            return "case " + exps;
        }

        if (ast.getNodeType() == ASTNode.SWITCH_STATEMENT) {
            //System.out.println("\nSWITCH_STATEMENT:\n" + ast);
            SwitchStatement s = (SwitchStatement) ast;
            String stmts = ((String) s.statements().stream().map(x -> formatAST((ASTNode) x)).collect(Collectors.joining("\n\t", "\n\t", "")));
            return "\nswitch (" + formatAST(s.getExpression()) + ") {" + stmts + "\n}";
        }

        if (ast.getNodeType() == ASTNode.SYNCHRONIZED_STATEMENT) {
            //System.out.println("\nSYNCHRONIZED_STATEMENT:\n" + ast);
            SynchronizedStatement s = (SynchronizedStatement) ast;
            return "\nsynchronized(" + formatAST(s.getExpression()) + ") {" + formatAST(s.getBody()).replace("\n", "\n\t") + "\n}";
        }

        if (ast.getNodeType() == ASTNode.THIS_EXPRESSION) {
            //TODO
            //System.out.println("\nTHIS_EXPRESSION:\n" + ast);
            ThisExpression t = (ThisExpression) ast;
//            //System.out.println(t.getQualifier());
            if (t.getQualifier() != null) {
                return formatAST(t.getQualifier()) + ".this";
            }
            return "this";
        }

        if (ast.getNodeType() == ASTNode.THROW_STATEMENT) {
            //System.out.println("\nTHROW_STATEMENT:\n" + ast);
            ThrowStatement t = (ThrowStatement) ast;
            return "\nthrow " + formatAST(t.getExpression());
        }

        if (ast.getNodeType() == ASTNode.TRY_STATEMENT) {
            //System.out.println("\nTRY_STATEMENT:\n" + ast);
            TryStatement t = (TryStatement) ast;
            String res = (String) (t.catchClauses().stream().map(x -> " catch (" + formatAST(((CatchClause) x).getException()) +
                    ") {" + formatAST(((CatchClause) x).getBody()).replace("\n", "\n\t") + "\n}").collect(Collectors.joining("\n\t", "", "")));
            Block f = t.getFinally();
            if (f != null) {
                res += " finally {" + formatAST(f).replace("\n", "\n\t") + "\n}";
            }
            return "\ntry {" + formatAST(t.getBody()).replace("\n", "\n\t") + "\n}" + res;
        }


        if (ast.getNodeType() == ASTNode.TYPE_DECLARATION) {
            //System.out.println("\nTYPE_DECLARATION:\n" + ast);
            TypeDeclaration t = (TypeDeclaration) ast;
            StringBuilder res = new StringBuilder("class " + formatAST(t.getName()) + " {");
            FieldDeclaration[] fields = t.getFields();
            Arrays.sort(fields, Comparator.comparing(x -> ((VariableDeclarationFragment)(x.fragments().get(0))).getName().getIdentifier()));
            MethodDeclaration[] methods = t.getMethods();
            Arrays.sort(methods, Comparator.comparing(x -> x.getName().getIdentifier()));
            TypeDeclaration[] types = t.getTypes();
            Arrays.sort(types, Comparator.comparing(x -> x.getName().getIdentifier()));
            for (FieldDeclaration field : fields) {
                res.append(formatAST(field)).append(";\n");
            }
            for (MethodDeclaration method : methods) {
                res.append(formatAST(method).replace("\n", "\n\t"));
            }
            for (TypeDeclaration type : types) {
                res.append(formatAST(type).replace("\n", "\n\t"));
            }
            return "\n" + res + "\n}";
        }

        if (ast.getNodeType() == ASTNode.TYPE_DECLARATION_STATEMENT) {
            //System.out.println("\nTYPE_DECLARATION_STATEMENT:\n" + ast);
            TypeDeclarationStatement t = (TypeDeclarationStatement) ast;
            return "\n" + formatAST(t.getDeclaration());
        }

        if (ast.getNodeType() == ASTNode.TYPE_LITERAL) {
            //System.out.println("\nTYPE_LITERAL:\n" + ast);
            TypeLiteral t = (TypeLiteral) ast;
            return formatAST(t.getType());
        }

        if (ast.getNodeType() == ASTNode.VARIABLE_DECLARATION_EXPRESSION) {
            //System.out.println("\nVARIABLE_DECLARATION_EXPRESSION:\n" + ast);
            VariableDeclarationExpression v = (VariableDeclarationExpression) ast;
            String t = formatAST(v.getType());
            return t + " " + v.fragments().stream().map(x -> formatAST((VariableDeclarationFragment) x) ).collect(Collectors.joining(", "));
        }

        if (ast.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
            //System.out.println("\nVARIABLE_DECLARATION_FRAGMENT:\n" + ast);
            VariableDeclarationFragment v = (VariableDeclarationFragment) ast;
            if (v.getInitializer() == null) return formatAST(v.getName());
            return formatAST(v.getName()) + " = " + formatAST(v.getInitializer());
        }

        if (ast.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
            //System.out.println("\nVARIABLE_DECLARATION_STATEMENT:\n" + ast);
            VariableDeclarationStatement v = (VariableDeclarationStatement) ast;
            String t = formatAST(v.getType());
            return "\n" + t + " " + v.fragments().stream().map(x -> formatAST((VariableDeclarationFragment) x) ).collect(Collectors.joining(", ")) + ";";
        }

        if (ast.getNodeType() == ASTNode.WHILE_STATEMENT) {
            //System.out.println("\nWHILE_STATEMENT:\n" + ast);
            WhileStatement w = (WhileStatement) ast;
            return "\nwhile (" + formatAST(w.getExpression()) + ") {" + formatAST(w.getBody()).replace("\n", "\n\t") + "\n}";
        }

        if (ast.getNodeType() == ASTNode.INSTANCEOF_EXPRESSION) {
            //System.out.println("\nINSTANCEOF_EXPRESSION:\n" + ast);
            InstanceofExpression i = (InstanceofExpression) ast;
            return formatAST(i.getLeftOperand()) + " instanceof " + formatAST(i.getRightOperand());
        }

        if (ast.getNodeType() == ASTNode.LINE_COMMENT) {
            //System.out.println("\nLINE_COMMENT:\n" + ast);
            return "";
        }

        if (ast.getNodeType() == ASTNode.BLOCK_COMMENT) {
            //System.out.println("\nBLOCK_COMMENT:\n" + ast);
            return "";
        }

        if (ast.getNodeType() == ASTNode.TAG_ELEMENT) {
            //System.out.println("\nTAG_ELEMENT:\n" + ast);
            return "";
        }

        if (ast.getNodeType() == ASTNode.TEXT_ELEMENT) {
            //System.out.println("\nTEXT_ELEMENT:\n" + ast);
            return "";
        }

        if (ast.getNodeType() == ASTNode.MEMBER_REF) {
            //System.out.println("\nMEMBER_REF:\n" + ast);
            MemberRef m = (MemberRef) ast;
            return formatAST(m.getQualifier()) + "." + formatAST(m.getName());
        }

        if (ast.getNodeType() == ASTNode.METHOD_REF_PARAMETER) {
            //System.out.println("\nMETHOD_REF_PARAMETER:\n" + ast);
            return "";
        }

        if (ast.getNodeType() == ASTNode.ENHANCED_FOR_STATEMENT) {
//            System.out.println("\nENHANCED_FOR_STATEMENT:\n" + ast);
            EnhancedForStatement e = (EnhancedForStatement) ast;
//            System.out.println(e.getParameter());
            return "\nfor (" + formatAST(e.getParameter())  + ": " + formatAST(e.getExpression()) +") {\t" + formatAST(e.getBody()).replace("\n", "\n\t") + "\n}";
        }

        if (ast.getNodeType() == ASTNode.ENUM_DECLARATION) {
            //TODO
            //System.out.println("\nENUM_DECLARATION:\n" + ast);
            EnumDeclaration e = (EnumDeclaration) ast;
            String s = (String) e.enumConstants().stream().map(x -> formatAST((EnumConstantDeclaration) x)).collect(Collectors.joining(",\n\t", "\n\t", ""));
            return "enum " + formatAST(e.getName()) + "{" + s + "\n}";
        }

        if (ast.getNodeType() == ASTNode.ENUM_CONSTANT_DECLARATION) {
            //TODO
            //System.out.println("\nENUM_CONSTANT_DECLARATION:\n" + ast);
            EnumConstantDeclaration e = (EnumConstantDeclaration) ast;
            return formatAST(e.getName());
        }

        if (ast.getNodeType() == ASTNode.TYPE_PARAMETER) {
            //TODO
            //System.out.println("\nTYPE_PARAMETER:\n" + ast);
            return "";
        }

        if (ast.getNodeType() == ASTNode.PARAMETERIZED_TYPE) {
            //System.out.println("\nPARAMETERIZED_TYPE:\n" + ast);
            ParameterizedType p = (ParameterizedType) ast;
            return formatAST(p.getType()) + "<" + p.typeArguments().stream().map(x -> formatAST((Type) x)).collect(Collectors.joining(", ")) + ">";
        }

        if (ast.getNodeType() == ASTNode.WILDCARD_TYPE) {
            //TODO
            //System.out.println("\nWILDCARD_TYPE:\n" + ast);
            return "";
        }

        if (ast.getNodeType() == ASTNode.NORMAL_ANNOTATION) {
            //System.out.println("\nNORMAL_ANNOTATION:\n" + ast);
            NormalAnnotation n = (NormalAnnotation) ast;
            String ps = (String) n.values().stream().map(x -> formatAST(((MemberValuePair)x))).collect(Collectors.joining(", "));
            return "@" + formatAST(n.getTypeName()) + "(" + ps + ")";
        }

        if (ast.getNodeType() == ASTNode.MARKER_ANNOTATION) {
            //System.out.println("\nMARKER_ANNOTATION:\n" + ast);
            MarkerAnnotation m = (MarkerAnnotation) ast;
            return "@" + formatAST(m.getTypeName());
        }

        if (ast.getNodeType() == ASTNode.SINGLE_MEMBER_ANNOTATION) {
            //System.out.println("\nSINGLE_MEMBER_ANNOTATION:\n" + ast);
            SingleMemberAnnotation s = (SingleMemberAnnotation) ast;
            return "@" + formatAST(s.getTypeName()) + "(" + formatAST(s.getValue()) + ")";
        }

        if (ast.getNodeType() == ASTNode.ANNOTATION_TYPE_DECLARATION) {
            //TODO
            //System.out.println("\nANNOTATION_TYPE_DECLARATION:\n" + ast);
            return "";
        }

        if (ast.getNodeType() == ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION) {
            //TODO
            //System.out.println("\nANNOTATION_TYPE_MEMBER_DECLARATION:\n" + ast);
            return "";
        }

        if (ast.getNodeType() == ASTNode.MODIFIER) {
            //System.out.println("\nMODIFIER:\n" + ast);
            return "";
        }

        if (ast.getNodeType() == ASTNode.UNION_TYPE) {
            //System.out.println("\nUNION_TYPE:\n" + ast);
            UnionType u = (UnionType) ast;
            return (String) u.types().stream().map(x -> formatAST((Type) x)).collect(Collectors.joining(" | "));
        }

        if (ast.getNodeType() == ASTNode.DIMENSION) {
            //System.out.println("\nDIMENSION:\n" + ast);
            return "";
        }

        if (ast.getNodeType() == ASTNode.LAMBDA_EXPRESSION) {
            //System.out.println("\nLAMBDA_EXPRESSION:\n" + ast);
            LambdaExpression l = (LambdaExpression) ast;
            String paras = getArgs(l.parameters());
            return "(" + paras + ") -> {" + formatAST(l.getBody()) + "}";
        }

        if (ast.getNodeType() == ASTNode.INTERSECTION_TYPE) {
            //System.out.println("\nINTERSECTION_TYPE:\n" + ast);
            IntersectionType i = (IntersectionType) ast;
            return (String) i.types().stream().map(x -> formatAST((Type) x)).collect(Collectors.joining(" & "));
        }

        if (ast.getNodeType() == ASTNode.NAME_QUALIFIED_TYPE) {
            //System.out.println("\nNAME_QUALIFIED_TYPE:\n" + ast);
            NameQualifiedType n = (NameQualifiedType) ast;
            return formatAST(n.getQualifier()) + "." + formatAST(n.getName());
        }

        return "";

    }

    public static String getArgs(List arguments) {
        return  (String) arguments.stream().map(x -> formatAST((ASTNode) x)).collect(Collectors.joining(", "));
    }
}
