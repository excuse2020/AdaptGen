package realize.utils;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

import java.util.Map;

public class ASTUtils {

    public static void main(String[] args) {
        ASTNode ast = getASTNode("System.out.println(x);");
        System.out.println(ast.getNodeType());
        System.out.println(ast);
    }

    public static ASTNode getASTNode(String code) {
        ASTParser parser = getParser(ASTParser.K_STATEMENTS);
        parser.setSource(code.toCharArray());
        ASTNode node = parser.createAST(null);
        return node;
    }

    private static ASTParser getParser(int kExpression) {
        ASTParser parser = ASTParser.newParser(AST.JLS15);
        parser.setKind(kExpression);
        Map<String, String> pOptions = JavaCore.getOptions();
        pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_11);
        pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_11);
        pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_11);
        pOptions.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
        parser.setStatementsRecovery(true);
        parser.setCompilerOptions(pOptions);
        return parser;
    }
}
