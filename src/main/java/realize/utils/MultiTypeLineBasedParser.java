package realize.utils;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.ASTParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class MultiTypeLineBasedParser {

    private static final Map<Class<?>, Integer> NODE_TYPE_MAP = new HashMap<>();

    static {
        NODE_TYPE_MAP.put(TypeDeclaration.class, 1); 
        NODE_TYPE_MAP.put(MethodDeclaration.class, 2); 
        NODE_TYPE_MAP.put(VariableDeclarationStatement.class, 3); 
        NODE_TYPE_MAP.put(Assignment.class, 4); 
        NODE_TYPE_MAP.put(MethodInvocation.class, 5); 
        NODE_TYPE_MAP.put(IfStatement.class, 6);
        NODE_TYPE_MAP.put(ReturnStatement.class, 9);
        NODE_TYPE_MAP.put(ForStatement.class, 10);
        NODE_TYPE_MAP.put(WhileStatement.class, 10);
        NODE_TYPE_MAP.put(EnhancedForStatement.class, 10);
        NODE_TYPE_MAP.put(SwitchStatement.class, 11);
        NODE_TYPE_MAP.put(SwitchCase.class, 12);
        NODE_TYPE_MAP.put(InfixExpression.class, 13);
        NODE_TYPE_MAP.put(PostfixExpression.class, 13);
        NODE_TYPE_MAP.put(PrefixExpression.class, 13);
        NODE_TYPE_MAP.put(LambdaExpression.class, 13);
        NODE_TYPE_MAP.put(BreakStatement.class, 14);
        NODE_TYPE_MAP.put(ContinueStatement.class, 15);
        NODE_TYPE_MAP.put(FieldDeclaration.class, 16);
    }

    public static String codeToTypeSeq(String code) {
        code = code.replaceAll("\\([^()]*?\\)\\s*->\\s*\\{.*?\\}", "null")
                .replaceAll("(?<=\\d)_(?=\\d)", "")
                .replaceAll("new\\s+\\w+\\s*\\{[^}]*\\}", "null")
                .replaceAll("\\s*throw.*+", "");
        ASTParser parser = ASTParser.newParser(AST.JLS15);
        parser.setSource(code.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        Map<Integer, List<Integer>> lineToTypesMap = new HashMap<>();
        
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                recordNode(node);
                return true;
            }

            @Override
            public boolean visit(MethodDeclaration node) {
                recordNode(node);
                return true;
            }

            @Override
            public boolean visit(FieldDeclaration node) {
                recordNode(node);
                return true;
            }

            @Override
            public boolean visit(VariableDeclarationStatement node) {
                recordNode(node);
                return false;
            }

            @Override
            public boolean visit(MethodInvocation node) {
                recordNode(node);
                return false;
            }

            @Override
            public boolean visit(IfStatement node) {
                recordNode(node);
                Statement thenBlock = node.getThenStatement();
                if (thenBlock != null) {
                    thenBlock.accept(this);
                }
                Statement elseBlock = node.getElseStatement();
                if (elseBlock != null) {
                    Statement elseStatement = node.getElseStatement();
                    if (elseStatement instanceof IfStatement) {
                        int lineNumber = cu.getLineNumber(elseStatement.getStartPosition());
                        lineToTypesMap.computeIfAbsent(lineNumber, k -> new ArrayList<>()).add(8);
                    } else {
                        int lineNumber = cu.getLineNumber(elseStatement.getStartPosition());
                        lineToTypesMap.computeIfAbsent(lineNumber, k -> new ArrayList<>()).add(7);
                    }
                    elseBlock.accept(this);
                }
                return false;
            }

            @Override
            public boolean visit(SwitchStatement node) {
                recordNode(node);
                return true;
            }

            @Override
            public boolean visit(SwitchCase node) {
                recordNode(node);
                return false;
            }

            @Override
            public boolean visit(BreakStatement node) {
                recordNode(node);
                return false;
            }

            @Override
            public boolean visit(ContinueStatement node) {
                recordNode(node);
                return false;
            }

            @Override
            public boolean visit(ForStatement node) {
                recordNode(node);
                Statement body = node.getBody();
                if (body != null) {
                    body.accept(this);
                }
                return false;
            }

            @Override
            public boolean visit(WhileStatement node) {
                recordNode(node);
                Statement body = node.getBody();
                if (body != null) {
                    body.accept(this);
                }
                return false;
            }

            @Override
            public boolean visit(EnhancedForStatement node) {
                recordNode(node);
                Statement body = node.getBody();
                if (body != null) {
                    body.accept(this);
                }
                return false;
            }

            @Override
            public boolean visit(ReturnStatement node) {
                recordNode(node);
                return false;
            }

            @Override
            public boolean visit(Assignment node) {
                recordNode(node);
                return false;
            }

            @Override
            public boolean visit(InfixExpression node) {
                recordNode(node);
                return false;
            }

            @Override
            public boolean visit(PostfixExpression node) {
                recordNode(node);
                return false;
            }

            @Override
            public boolean visit(PrefixExpression node) {
                recordNode(node);
                return false;
            }

            public boolean visit(LambdaExpression node) {
                recordNode(node);
                return false;
            }

            private void recordNode(ASTNode node) {
                int lineNumber = cu.getLineNumber(node.getStartPosition());
                Integer nodeType = NODE_TYPE_MAP.get(node.getClass());
                if (nodeType != null) {
                    lineToTypesMap.computeIfAbsent(lineNumber, k -> new ArrayList<>()).add(nodeType);
                }
            }
        });

        String[] codeLines = code.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= codeLines.length; i++) {
            List<Integer> types = lineToTypesMap.getOrDefault(i, new ArrayList<>());
            if (!types.isEmpty()) {
                int type = types.get(0);
                sb.append(type).append("*");
                if (codeLines[i - 1].trim().endsWith("{") || codeLines[i - 1].trim().endsWith(":")) {
                    sb.append("(");
                }
            } else if (codeLines[i - 1].trim().equals("}")) {
                sb.append(")");
            } else {
                return null;
            }
        }
        return sb.toString();
    }

    @Test
    public void test() {
        String code = """
                class Solution {
                	String removeDuplicates(String s) {
                		if (s == null) {
                			throw new IllegalArgumentException("Input string is null")
                		}
                		int len = s.length();
                		if (len <= 1) {
                			return s;
                		}
                		StringBuilder sb = new StringBuilder();
                		for (int i = 0; i < len; i++) {
                			char curChar = s.charAt(i);
                			int sbLen = sb.length();
                			if (sbLen > 0 && sb.charAt(sbLen - 1) == curChar) {
                				sb.setLength(sbLen - 1);
                			}
                			else {
                				sb.append(curChar);
                			}
                		}
                		return sb.toString();
                	}
                }
                """;
        System.out.println(codeToTypeSeq(code));
    }
}
