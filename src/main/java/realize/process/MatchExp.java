package realize.process;

import org.eclipse.jdt.core.dom.*;
import realize.utils.ASTUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MatchExp {

    public static Set<String> commonClasses;

    static {
        try {
            commonClasses = new HashSet<>(Files.readAllLines(Paths.get("files/commonClasses.txt")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        String code = "if (nums[mid] > target) {";
        code = getMatchExpByCode(code);
        System.out.println(code);
        System.out.println(type);
    }

    public static Set<Integer> type;

    private static String getArgs(List arguments) {
        return  (String) arguments.stream().map(x -> getExp((ASTNode) x)).collect(Collectors.joining(", "));
    }

    public static String getMatchExpByCode(String code) {
        type = new HashSet<>();
        return getExp(ASTUtils.getASTNode(code));
    }


    private static String getExp(ASTNode ast) {

        if (ast == null) return "";

        //System.out.println(ast.getNodeType() + ": " + ast);

        if (ast.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION) {
            type.add(ASTNode.ANONYMOUS_CLASS_DECLARATION);
            return "";
        }

        if (ast.getNodeType() == ASTNode.ARRAY_ACCESS) {
            type.add(ASTNode.ARRAY_ACCESS);
            ArrayAccess a = ((ArrayAccess) ast);
            return getExp(a.getArray()) + "[" + getExp(a.getIndex()) + "]";
        }

        if (ast.getNodeType() == ASTNode.ARRAY_CREATION) {
            type.add(ASTNode.ARRAY_CREATION);
            ArrayCreation a = (ArrayCreation) ast;
            int size = a.dimensions().size();
            String d = IntStream.range(0, size).mapToObj(x -> "[" + getExp((ASTNode) a.dimensions().get(x)) + "]").collect(Collectors.joining(""));
            return "new " + getExp(a.getType()).replace("[]", "") + d + getExp(a.getInitializer());
        }

        if (ast.getNodeType() == ASTNode.ARRAY_INITIALIZER) {
            type.add(ASTNode.ARRAY_INITIALIZER);
            ArrayInitializer a = (ArrayInitializer) ast;
            if (a == null) return "";
            String e = (String) a.expressions().stream().map(x -> getExp((Expression) x)).collect(Collectors.joining(", "));
            return "{" + e + "}";
        }

        if (ast.getNodeType() == ASTNode.ARRAY_TYPE) {
            type.add(ASTNode.ARRAY_TYPE);
            ArrayType a = (ArrayType) ast;
            int size = a.dimensions().size();
            String d = IntStream.range(0, size).mapToObj(x -> "[]").collect(Collectors.joining(""));
            return getExp(a.getElementType()) + d;
        }

        if (ast.getNodeType() == ASTNode.ASSERT_STATEMENT) {
            type.add(ASTNode.ASSERT_STATEMENT);
            AssertStatement a = (AssertStatement) ast;
            return "\nassert " + getExp(a.getExpression()) + ";";
        }

        if (ast.getNodeType() == ASTNode.ASSIGNMENT) {
            type.add(ASTNode.ASSIGNMENT);
            Assignment node = (Assignment) ast;
            Expression l = node.getLeftHandSide();
            String sl = getExp(l);
            Expression r = node.getRightHandSide();
            String sr = getExp(r);
            return sl + " " + node.getOperator() + " " + sr;
        }

        if (ast.getNodeType() == ASTNode.BLOCK) {
            type.add(ASTNode.BLOCK);
            Block b = (Block) ast;
            return  ((String) b.statements().stream().map(x -> getExp((ASTNode) x)).collect(Collectors.joining("")));
        }

        if (ast.getNodeType() == ASTNode.BOOLEAN_LITERAL) {
            type.add(ASTNode.BOOLEAN_LITERAL);
            return "Bool";
        }

        if (ast.getNodeType() == ASTNode.BREAK_STATEMENT) {
            type.add(ASTNode.BREAK_STATEMENT);
            return "\nbreak;";
        }

        if (ast.getNodeType() == ASTNode.CAST_EXPRESSION) {
            type.add(ASTNode.CAST_EXPRESSION);
            CastExpression c = (CastExpression) ast;
            //System.out.println("*******" + c.getType().getNodeType());
            return "(" + getExp(c.getType()) + ") " + getExp(c.getExpression());
        }

        if (ast.getNodeType() == ASTNode.CATCH_CLAUSE) {
            type.add(ASTNode.CATCH_CLAUSE);
            CatchClause c = (CatchClause) ast;
            //TODO
        }

        if (ast.getNodeType() == ASTNode.CHARACTER_LITERAL) {
            type.add(ASTNode.CHARACTER_LITERAL);
//            return ":[char" + charCnt++ + "~'.']";
            return "Char";
        }

        if (ast.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
            type.add(ASTNode.CLASS_INSTANCE_CREATION);
            ClassInstanceCreation c = (ClassInstanceCreation) ast;
            //System.out.println(c.getExpression());
            return "new " + getExp(c.getType()) + "(" + getArgs(c.arguments()) + ")";
        }

        if (ast.getNodeType() == ASTNode.COMPILATION_UNIT) {
            type.add(ASTNode.COMPILATION_UNIT);
            //TODO
            CompilationUnit c = (CompilationUnit) ast;
            return "COMPILATION_UNIT";
        }

        if (ast.getNodeType() == ASTNode.CONDITIONAL_EXPRESSION) {
//            //System.out.println("CONDITIONAL_EXPRESSION");
            ConditionalExpression c = (ConditionalExpression) ast;
            return getExp(c.getExpression()) + " ? " + getExp(c.getThenExpression()) + " : " + getExp(c.getElseExpression());
        }

        if (ast.getNodeType() == ASTNode.CONSTRUCTOR_INVOCATION) {
            type.add(ASTNode.CONSTRUCTOR_INVOCATION);
            //TODO
            ConstructorInvocation c = (ConstructorInvocation) ast;
            return "CONSTRUCTOR_INVOCATION";
        }

        if (ast.getNodeType() == ASTNode.CONTINUE_STATEMENT) {
            type.add(ASTNode.CONTINUE_STATEMENT);
            return "\ncontinue;";
        }

        if (ast.getNodeType() == ASTNode.DO_STATEMENT) {
            type.add(ASTNode.DO_STATEMENT);
            DoStatement c = (DoStatement) ast;
            return "\ndo {\n" + getExp(c.getBody()).replace("\n", "\n\t") + "\n} while (...)";
        }

        if (ast.getNodeType() == ASTNode.EMPTY_STATEMENT) {
            type.add(ASTNode.EMPTY_STATEMENT);
            return "";
        }

        if (ast.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
            type.add(ASTNode.EXPRESSION_STATEMENT);
            ExpressionStatement e = (ExpressionStatement) ast;
            return "\n" + getExp(e.getExpression()) + ";";
        }

        if (ast.getNodeType() == ASTNode.FIELD_ACCESS) {
            type.add(ASTNode.FIELD_ACCESS);
            FieldAccess f = (FieldAccess) ast;
            return f.getExpression() + "." + f.getName();
        }

        if (ast.getNodeType() == ASTNode.FIELD_DECLARATION) {
            type.add(ASTNode.FIELD_DECLARATION);
            FieldDeclaration f = (FieldDeclaration) ast;

            if (((VariableDeclarationFragment) (f.fragments().get(0))).getInitializer() == null) {
                return "\n\t" + getExp(f.getType()) + " " + IntStream.range(0, f.fragments().size()).
                        mapToObj(i -> "var" + i).collect(Collectors.joining(", "));
            }

            return "\n\t" + getExp(f.getType()) + " " + IntStream.range(0, f.fragments().size()).
                    mapToObj(i -> "var" + i + " = " + getExp(((VariableDeclarationFragment) f.fragments().get(i)).getInitializer()))
                    .collect(Collectors.joining(", "));
        }

        if (ast.getNodeType() == ASTNode.FOR_STATEMENT) {
            type.add(ASTNode.FOR_STATEMENT);
            ForStatement f = (ForStatement) ast;
            return "\nfor (...) {" + getExp(f.getBody()).replace("\n", "\n\t") + "\n}";
        }

        if (ast.getNodeType() == ASTNode.IF_STATEMENT) {
            type.add(ASTNode.IF_STATEMENT);
            IfStatement i = (IfStatement) ast;
            Statement elseStatement = i.getElseStatement();
            if (elseStatement != null) {
                if (elseStatement.getNodeType() == ASTNode.IF_STATEMENT)
                    return "\nif (...) {" + getExp(i.getThenStatement()).replace("\n", "\n\t") + "\n} \nelse " + getExp(elseStatement).replaceFirst("\n", "");
                return "\nif (...) {" + getExp(i.getThenStatement()).replace("\n", "\n\t") + "\n} \nelse {" + getExp(elseStatement).replace("\n", "\n\t") + "\n}";
            }
            return "\nif (...) {" + getExp(i.getThenStatement()).replace("\n", "\n\t") + "\n}";
        }

        if (ast.getNodeType() == ASTNode.IMPORT_DECLARATION) {
            type.add(ASTNode.IMPORT_DECLARATION);
//            return ast.toString();
            return "";
        }

        if (ast.getNodeType() == ASTNode.INFIX_EXPRESSION) {
            type.add(ASTNode.INFIX_EXPRESSION);
            InfixExpression i = (InfixExpression) ast;
            return "INFIX_EXPRESSION";
        }

        if (ast.getNodeType() == ASTNode.INITIALIZER) {
            //TODO
            type.add(ASTNode.INITIALIZER);
            Initializer i = (Initializer) ast;
            return getExp(i.getBody());
        }

        if (ast.getNodeType() == ASTNode.JAVADOC) {
            type.add(ASTNode.JAVADOC);
            return "";
        }

        if (ast.getNodeType() == ASTNode.LABELED_STATEMENT) {
            type.add(ASTNode.LABELED_STATEMENT);
            return ast.toString();
        }

        if (ast.getNodeType() == ASTNode.METHOD_DECLARATION) {
            type.add(ASTNode.METHOD_DECLARATION);
            MethodDeclaration m = (MethodDeclaration) ast;
            String paras = (String) m.parameters().stream().map(x -> getExp(((SingleVariableDeclaration) x))).collect(Collectors.joining(", "));
            return "\n" + getExp(m.getReturnType2()) + " " + getExp(m.getName()) + "(" + paras + ") {"
                    + getExp(m.getBody()).replace("\n", "\n\t") + "\n}";
        }

        if (ast.getNodeType() == ASTNode.METHOD_INVOCATION) {
            type.add(ASTNode.METHOD_INVOCATION);
            MethodInvocation mi = (MethodInvocation) ast;
            String args = (String) mi.arguments().stream().map(x -> getExp((ASTNode) x)).collect(Collectors.joining(","));
            String s = getExp(mi.getExpression());
            if (mi.getExpression() == null) {
                return getExp(mi.getName()) + "(" + args + ")";
            }
            return s + "." + (s.equals(mi.getExpression().toString()) ? mi.getName() : getExp(mi.getName())) + "(" + args + ")";
        }

        if (ast.getNodeType() == ASTNode.NULL_LITERAL) {
            type.add(ASTNode.NULL_LITERAL);
            return "null";
        }

        if (ast.getNodeType() == ASTNode.NUMBER_LITERAL) {
            type.add(ASTNode.NUMBER_LITERAL);
//            return ":[int" + intCnt++ + "~\\d+]";
            return "Num";
        }

        if (ast.getNodeType() == ASTNode.PACKAGE_DECLARATION) {
            type.add(ASTNode.PACKAGE_DECLARATION);
            return "";
        }

        if (ast.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
            type.add(ASTNode.PARENTHESIZED_EXPRESSION);
            ParenthesizedExpression p = (ParenthesizedExpression) ast;
            return "(" + getExp(p.getExpression()) + ")";
        }

        if (ast.getNodeType() == ASTNode.POSTFIX_EXPRESSION) {
            type.add(ASTNode.POSTFIX_EXPRESSION);
            PostfixExpression p = (PostfixExpression) ast;
            return "POSTFIX_EXPRESSION";
        }

        if (ast.getNodeType() == ASTNode.PREFIX_EXPRESSION) {
            type.add(ASTNode.PREFIX_EXPRESSION);
            PrefixExpression p = (PrefixExpression) ast;
            return "PREFIX_EXPRESSION";
        }

        if (ast.getNodeType() == ASTNode.PRIMITIVE_TYPE) {
            type.add(ASTNode.PRIMITIVE_TYPE);
            return ast.toString();
        }

        if (ast.getNodeType() == ASTNode.QUALIFIED_NAME) {
            type.add(ASTNode.QUALIFIED_NAME);
            QualifiedName q = (QualifiedName) ast;
            return getExp(q.getQualifier()) + "." + getExp(q.getName());
        }

        if (ast.getNodeType() == ASTNode.RETURN_STATEMENT) {
            type.add(ASTNode.RETURN_STATEMENT);
            ReturnStatement r = (ReturnStatement) ast;
            Expression e = r.getExpression();
            return "\nreturn " + getExp(e) + ";";
        }

        if (ast.getNodeType() == ASTNode.SIMPLE_NAME) {
            type.add(ASTNode.SIMPLE_NAME);
            SimpleName s = (SimpleName) ast;
            return "SIMPLE_NAME$" + ((SimpleName) ast).getIdentifier() + "$";
        }

        if (ast.getNodeType() == ASTNode.SIMPLE_TYPE) {
            type.add(ASTNode.SIMPLE_TYPE);
            SimpleType s = (SimpleType) ast;
            return "SIMPLE_TYPE$" + ((SimpleType) ast).toString() + "$";
            // return commonClasses.contains(s.toString()) ? s.toString() : "TYPE";
        }

        if (ast.getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION) {
            type.add(ASTNode.SINGLE_VARIABLE_DECLARATION);
            SingleVariableDeclaration s = (SingleVariableDeclaration) ast;
            String res = getExp(s.getType()) + " " + getExp(s.getName());
            if (s.getInitializer() != null) {
                res = res + " = " + getExp(s.getInitializer());
            }
            return res;
        }

        if (ast.getNodeType() == ASTNode.STRING_LITERAL) {
            type.add(ASTNode.STRING_LITERAL);
//            return ":[STR" + strCnt + "~\"\\w+\"]";
            return "Str";
        }

        if (ast.getNodeType() == ASTNode.SUPER_CONSTRUCTOR_INVOCATION) {
            type.add(ASTNode.SUPER_CONSTRUCTOR_INVOCATION);
            SuperConstructorInvocation s = (SuperConstructorInvocation) ast;
            String args = (String) s.arguments().stream().map(x -> getExp((ASTNode) x)).collect(Collectors.joining(", "));
            return "super(" + args + ")";
        }

        if (ast.getNodeType() == ASTNode.SUPER_FIELD_ACCESS) {
            type.add(ASTNode.SUPER_FIELD_ACCESS);
            SuperFieldAccess s = (SuperFieldAccess) ast;
            //System.out.println(s.getName());
            return "super." + getExp(s.getName());
        }

        if (ast.getNodeType() == ASTNode.SUPER_METHOD_INVOCATION) {
            type.add(ASTNode.SUPER_METHOD_INVOCATION);
            SuperMethodInvocation s = (SuperMethodInvocation) ast;
            return "super." + getExp(s.getName()) + "(" + getArgs(s.arguments()) + ")";
        }

        if (ast.getNodeType() == ASTNode.SWITCH_CASE) {
            type.add(ASTNode.SWITCH_CASE);
            SwitchCase s = (SwitchCase) ast;
            if (s.isDefault()) {
                return "default: ";
            }
            String exps = ((String) s.expressions().stream().map(x -> getExp((ASTNode) x)).collect(Collectors.joining(":\n", "", ":")));
            return "case " + exps;
        }

        if (ast.getNodeType() == ASTNode.SWITCH_STATEMENT) {
            type.add(ASTNode.SWITCH_STATEMENT);
            SwitchStatement s = (SwitchStatement) ast;
            String stmts = ((String) s.statements().stream().map(x -> getExp((ASTNode) x)).collect(Collectors.joining("\n\t", "\n\t", "")));
            return "\nswitch (" + getExp(s.getExpression()) + ") {" + stmts + "\n}";
        }

        if (ast.getNodeType() == ASTNode.SYNCHRONIZED_STATEMENT) {
            type.add(ASTNode.SYNCHRONIZED_STATEMENT);
            SynchronizedStatement s = (SynchronizedStatement) ast;
            return "\nsynchronized(" + getExp(s.getExpression()) + ") {" + getExp(s.getBody()).replace("\n", "\n\t") + "\n}";
        }

        if (ast.getNodeType() == ASTNode.THIS_EXPRESSION) {
            //TODO
            type.add(ASTNode.THIS_EXPRESSION);
            ThisExpression t = (ThisExpression) ast;
//            //System.out.println(t.getQualifier());
            if (t.getQualifier() != null) {
                return getExp(t.getQualifier()) + ".this";
            }
            return "this";
        }

        if (ast.getNodeType() == ASTNode.THROW_STATEMENT) {
            type.add(ASTNode.THROW_STATEMENT);
            ThrowStatement t = (ThrowStatement) ast;
            return "\nthrow " + getExp(t.getExpression());
        }

        if (ast.getNodeType() == ASTNode.TRY_STATEMENT) {
            type.add(ASTNode.TRY_STATEMENT);
            TryStatement t = (TryStatement) ast;
            String res = (String) (t.catchClauses().stream().map(x -> " catch (" + getExp(((CatchClause) x).getException()) +
                    ") {" + getExp(((CatchClause) x).getBody()).replace("\n", "\n\t") + "\n}").collect(Collectors.joining("\n\t", "", "")));
            Block f = t.getFinally();
            if (f != null) {
                res += " finally {" + getExp(f).replace("\n", "\n\t") + "\n}";
            }
            return "\ntry {" + getExp(t.getBody()).replace("\n", "\n\t") + "\n}" + res;
        }


        if (ast.getNodeType() == ASTNode.TYPE_DECLARATION) {
            type.add(ASTNode.TYPE_DECLARATION);
            TypeDeclaration t = (TypeDeclaration) ast;
            StringBuilder res = new StringBuilder("class " + getExp(t.getName()) + " {");

            FieldDeclaration[] fields = t.getFields();
            Arrays.sort(fields, Comparator.comparing(x -> ((VariableDeclarationFragment)(x.fragments().get(0))).getName().getIdentifier()));
            MethodDeclaration[] methods = t.getMethods();
            Arrays.sort(methods, Comparator.comparing(x -> x.getName().getIdentifier()));
            TypeDeclaration[] types = t.getTypes();
            Arrays.sort(types, Comparator.comparing(x -> x.getName().getIdentifier()));

            for (FieldDeclaration field : fields) {
                res.append(getExp(field)).append(";\n");
            }
            for (MethodDeclaration method : methods) {
                res.append(getExp(method).replace("\n", "\n\t"));
            }
            for (TypeDeclaration type : types) {
                res.append(getExp(type).replace("\n", "\n\t"));
            }

            return "\n" + res + "\n}";
        }

        if (ast.getNodeType() == ASTNode.TYPE_DECLARATION_STATEMENT) {
            type.add(ASTNode.TYPE_DECLARATION_STATEMENT);
            TypeDeclarationStatement t = (TypeDeclarationStatement) ast;
            return "\n" + getExp(t.getDeclaration());
        }

        if (ast.getNodeType() == ASTNode.TYPE_LITERAL) {
            type.add(ASTNode.TYPE_LITERAL);
            TypeLiteral t = (TypeLiteral) ast;
            return getExp(t.getType());
        }

        if (ast.getNodeType() == ASTNode.VARIABLE_DECLARATION_EXPRESSION) {
            type.add(ASTNode.VARIABLE_DECLARATION_EXPRESSION);
            VariableDeclarationExpression v = (VariableDeclarationExpression) ast;
            String t = getExp(v.getType());
            return t + " " + v.fragments().stream().map(x -> getExp((VariableDeclarationFragment) x) ).collect(Collectors.joining(", "));
        }

        if (ast.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
            type.add(ASTNode.VARIABLE_DECLARATION_FRAGMENT);
            VariableDeclarationFragment v = (VariableDeclarationFragment) ast;
            if (v.getInitializer() == null) return getExp(v.getName());
            return getExp(v.getName()) + " = " + getExp(v.getInitializer());
        }

        if (ast.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
            type.add(ASTNode.VARIABLE_DECLARATION_STATEMENT);
            VariableDeclarationStatement v = (VariableDeclarationStatement) ast;
            String t = getExp(v.getType());
            return "\n" + t + " " + v.fragments().stream().map(x -> getExp((VariableDeclarationFragment) x) ).collect(Collectors.joining(", ")) + ";";
        }

        if (ast.getNodeType() == ASTNode.WHILE_STATEMENT) {
            type.add(ASTNode.WHILE_STATEMENT);
            WhileStatement w = (WhileStatement) ast;
            return "\nwhile (...) {" + getExp(w.getBody()).replace("\n", "\n\t") + "\n}";
        }

        if (ast.getNodeType() == ASTNode.INSTANCEOF_EXPRESSION) {
            type.add(ASTNode.INSTANCEOF_EXPRESSION);
            InstanceofExpression i = (InstanceofExpression) ast;
            return getExp(i.getLeftOperand()) + " instanceof " + getExp(i.getRightOperand());
        }

        if (ast.getNodeType() == ASTNode.LINE_COMMENT) {
            type.add(ASTNode.LINE_COMMENT);
            return "";
        }

        if (ast.getNodeType() == ASTNode.BLOCK_COMMENT) {
            type.add(ASTNode.BLOCK_COMMENT);
            return "";
        }

        if (ast.getNodeType() == ASTNode.TAG_ELEMENT) {
            type.add(ASTNode.TAG_ELEMENT);
            return "";
        }

        if (ast.getNodeType() == ASTNode.TEXT_ELEMENT) {
            type.add(ASTNode.TEXT_ELEMENT);
            return "";
        }

        if (ast.getNodeType() == ASTNode.MEMBER_REF) {
            type.add(ASTNode.MEMBER_REF);
            MemberRef m = (MemberRef) ast;
            return getExp(m.getQualifier()) + "." + getExp(m.getName());
        }

        if (ast.getNodeType() == ASTNode.METHOD_REF_PARAMETER) {
            type.add(ASTNode.METHOD_REF_PARAMETER);
            return "";
        }

        if (ast.getNodeType() == ASTNode.ENHANCED_FOR_STATEMENT) {
            type.add(ASTNode.ENHANCED_FOR_STATEMENT);
            EnhancedForStatement e = (EnhancedForStatement) ast;
            return "\nfor (...) {\t" + getExp(e.getBody()).replace("\n", "\n\t") + "\n}";
        }

        if (ast.getNodeType() == ASTNode.ENUM_DECLARATION) {
            //TODO
            type.add(ASTNode.ENUM_DECLARATION);
            EnumDeclaration e = (EnumDeclaration) ast;
            String s = (String) e.enumConstants().stream().map(x -> getExp((EnumConstantDeclaration) x)).collect(Collectors.joining(",\n\t", "\n\t", ""));
            return "enum " + getExp(e.getName()) + "{" + s + "\n}";
        }

        if (ast.getNodeType() == ASTNode.ENUM_CONSTANT_DECLARATION) {
            //TODO
            type.add(ASTNode.ENUM_CONSTANT_DECLARATION);
            EnumConstantDeclaration e = (EnumConstantDeclaration) ast;
            return getExp(e.getName());
        }

        if (ast.getNodeType() == ASTNode.TYPE_PARAMETER) {
            //TODO
            type.add(ASTNode.TYPE_PARAMETER);
            return "";
        }

        if (ast.getNodeType() == ASTNode.PARAMETERIZED_TYPE) {
            type.add(ASTNode.PARAMETERIZED_TYPE);
            ParameterizedType p = (ParameterizedType) ast;
            return getExp(p.getType()) + "<" + p.typeArguments().stream().map(x -> getExp((Type) x)).collect(Collectors.joining(", ")) + ">";
        }

        if (ast.getNodeType() == ASTNode.WILDCARD_TYPE) {
            //TODO
            type.add(ASTNode.WILDCARD_TYPE);
            return "";
        }

        if (ast.getNodeType() == ASTNode.NORMAL_ANNOTATION) {
            type.add(ASTNode.NORMAL_ANNOTATION);
            NormalAnnotation n = (NormalAnnotation) ast;
            String ps = (String) n.values().stream().map(x -> getExp(((MemberValuePair)x))).collect(Collectors.joining(", "));
            return "@" + getExp(n.getTypeName()) + "(" + ps + ")";
        }

        if (ast.getNodeType() == ASTNode.MARKER_ANNOTATION) {
            type.add(ASTNode.MARKER_ANNOTATION);
            MarkerAnnotation m = (MarkerAnnotation) ast;
            return "@" + getExp(m.getTypeName());
        }

        if (ast.getNodeType() == ASTNode.SINGLE_MEMBER_ANNOTATION) {
            type.add(ASTNode.SINGLE_MEMBER_ANNOTATION);
            SingleMemberAnnotation s = (SingleMemberAnnotation) ast;
            return "@" + getExp(s.getTypeName()) + "(" + getExp(s.getValue()) + ")";
        }

        if (ast.getNodeType() == ASTNode.ANNOTATION_TYPE_DECLARATION) {
            //TODO
            type.add(ASTNode.ANNOTATION_TYPE_DECLARATION);
            return "";
        }

        if (ast.getNodeType() == ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION) {
            //TODO
            type.add(ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION);
            return "";
        }

        if (ast.getNodeType() == ASTNode.MODIFIER) {
            type.add(ASTNode.MODIFIER);
            return "";
        }

        if (ast.getNodeType() == ASTNode.UNION_TYPE) {
            type.add(ASTNode.UNION_TYPE);
            UnionType u = (UnionType) ast;
            return (String) u.types().stream().map(x -> getExp((Type) x)).collect(Collectors.joining(" | "));
        }

        if (ast.getNodeType() == ASTNode.DIMENSION) {
            type.add(ASTNode.DIMENSION);
            return "";
        }

        if (ast.getNodeType() == ASTNode.LAMBDA_EXPRESSION) {
            type.add(ASTNode.LAMBDA_EXPRESSION);
            LambdaExpression l = (LambdaExpression) ast;
            String paras = getArgs(l.parameters());
            return "(" + paras + ") -> {" + getExp(l.getBody()) + "}";
        }

        if (ast.getNodeType() == ASTNode.INTERSECTION_TYPE) {
            type.add(ASTNode.INTERSECTION_TYPE);
            IntersectionType i = (IntersectionType) ast;
            return (String) i.types().stream().map(x -> getExp((Type) x)).collect(Collectors.joining(" & "));
        }

        if (ast.getNodeType() == ASTNode.NAME_QUALIFIED_TYPE) {
            type.add(ASTNode.NAME_QUALIFIED_TYPE);
            NameQualifiedType n = (NameQualifiedType) ast;
            return getExp(n.getQualifier()) + "." + getExp(n.getName());
        }

        return "";
    }
}
