package realize.customization;

import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class Modularization {
    // Module types
    public static final int BASIC_STATEMENT = 0;
    public static final int DATA_STRUCTURE_DEFINITION = 1;
    public static final int DATA_STRUCTURE_SELECTION = 2;
    public static final int ALGORITHM_PROCESSING = 3;
    public static final int IO_OPERATION = 4;

    private boolean algorithmBegin = false;

    // Map to store line numbers and their corresponding module types
    private final Map<Integer, Integer> lineModuleMap;
    // Set to store declared variables
    private final Set<String> declaredVariables;
    // Map to store control statement lines
    private final Set<Integer> controlStatementLines;
    // List of common IO method names
    private static final Set<String> IO_METHODS = new HashSet<>(Arrays.asList(
            "print", "println", "printf", "read", "write", "next", "nextInt", "nextLine",
            "nextDouble", "nextFloat", "nextLong", "nextBoolean", "nextByte", "nextShort",
            "readLine", "readInt", "readDouble", "readFloat", "readLong", "readBoolean",
            "readByte", "readShort", "readChar", "readString", "readUTF", "readFully"
    ));

    private CompilationUnit cu;

    // Map to store module start positions
    private final Map<Integer, Integer> moduleStartLines;
    // Map to store module end positions
    private final Map<Integer, Integer> moduleEndLines;
    // Map to store module content
    private final Map<Integer, List<String>> moduleContent;

    public Modularization() {
        lineModuleMap = new HashMap<>();
        declaredVariables = new HashSet<>();
        controlStatementLines = new HashSet<>();
        moduleStartLines = new HashMap<>();
        moduleEndLines = new HashMap<>();
        moduleContent = new HashMap<>();
    }

    public String analyzeCode(String code) {
        // Create AST parser
        ASTParser parser = ASTParser.newParser(AST.JLS15);
        parser.setSource(code.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        // Get the CompilationUnit
        cu = (CompilationUnit) parser.createAST(null);
        if (cu == null) return "";

        lineModuleMap.clear();
        declaredVariables.clear();
        controlStatementLines.clear();
        moduleStartLines.clear();
        moduleEndLines.clear();
        moduleContent.clear();

        // Analyze the AST
        analyzeAST(cu);
        return formatCodeWithModules(code);
    }

    private void analyzeAST(ASTNode ast) {
        if (ast == null) return;

        ast.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                if (!"Solution".equals(node.getName().getIdentifier())) {
                    markNode(node, DATA_STRUCTURE_DEFINITION);
                    return false;
                }
                return true;
            }

            @Override
            public boolean visit(VariableDeclarationStatement node) {
                if (algorithmBegin) {
                    return false;
                }
                markNode(node, DATA_STRUCTURE_SELECTION);
                // Record declared variables
                for (Object fragment : node.fragments()) {
                    if (fragment instanceof VariableDeclarationFragment) {
                        VariableDeclarationFragment vdf = (VariableDeclarationFragment) fragment;
                        declaredVariables.add(vdf.getName().getIdentifier());
                    }
                }
                return false;
            }

            @Override
            public boolean visit(MethodInvocation node) {
                String methodName = node.getName().getIdentifier();
                if (IO_METHODS.contains(methodName)) {
                    markNode(node, IO_OPERATION);
                }
                return false;
            }

            @Override
            public boolean visit(IfStatement node) {
                int lineNumber = cu.getLineNumber(node.getStartPosition());
                controlStatementLines.add(lineNumber);
                // Also add the line numbers of else and else if statements
                Statement elseStmt = node.getElseStatement();
                if (elseStmt != null) {
                    int elseLine = cu.getLineNumber(elseStmt.getStartPosition());
                    controlStatementLines.add(elseLine);
                }
                markNode(node, ALGORITHM_PROCESSING);
                return true; // Return true to visit children
            }

            @Override
            public boolean visit(ForStatement node) {
                int lineNumber = cu.getLineNumber(node.getStartPosition());
                controlStatementLines.add(lineNumber);
                markNode(node, ALGORITHM_PROCESSING);
                return true; // Return true to visit children
            }

            @Override
            public boolean visit(WhileStatement node) {
                int lineNumber = cu.getLineNumber(node.getStartPosition());
                controlStatementLines.add(lineNumber);
                markNode(node, ALGORITHM_PROCESSING);
                return true; // Return true to visit children
            }

            @Override
            public boolean visit(DoStatement node) {
                int lineNumber = cu.getLineNumber(node.getStartPosition());
                controlStatementLines.add(lineNumber);
                markNode(node, ALGORITHM_PROCESSING);
                return true; // Return true to visit children
            }

            @Override
            public boolean visit(SwitchStatement node) {
                int lineNumber = cu.getLineNumber(node.getStartPosition());
                controlStatementLines.add(lineNumber);
                markNode(node, ALGORITHM_PROCESSING);
                return true; // Return true to visit children
            }

            @Override
            public boolean visit(EnhancedForStatement node) {
                int lineNumber = cu.getLineNumber(node.getStartPosition());
                controlStatementLines.add(lineNumber);
                markNode(node, ALGORITHM_PROCESSING);
                return true; // Return true to visit children
            }

            private void markNode(ASTNode node, int moduleType) {
                int startLine = cu.getLineNumber(node.getStartPosition());
                int endLine = cu.getLineNumber(node.getStartPosition() + node.getLength());
                if (moduleType == ALGORITHM_PROCESSING && endLine - startLine >= 3) {
                    algorithmBegin = true;
                }

                // Record module boundaries
                if (!moduleStartLines.containsKey(moduleType) || startLine < moduleStartLines.get(moduleType)) {
                    moduleStartLines.put(moduleType, startLine);
                }
                if (!moduleEndLines.containsKey(moduleType) || endLine > moduleEndLines.get(moduleType)) {
                    moduleEndLines.put(moduleType, endLine);
                }

                markLines(startLine, endLine, moduleType);
            }
        });
    }

    private void markLines(int startLine, int endLine, int moduleType) {
        for (int i = startLine; i <= endLine; i++) {
            lineModuleMap.put(i, moduleType);
        }
    }

    private String formatCodeWithModules(String code) {
        String[] lines = code.split("\n");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            result.append(lines[i]).append("\t");
            result.append("// Module: ").append(getModuleName(lineModuleMap.getOrDefault(lineNumber, BASIC_STATEMENT)))
                    .append("\n");
        }
        return result.toString();
    }

    private String getModuleName(int moduleType) {
        return switch (moduleType) {
            case DATA_STRUCTURE_DEFINITION -> "Data Structure Definition";
            case DATA_STRUCTURE_SELECTION -> "Data Structure Selection";
            case ALGORITHM_PROCESSING -> "Algorithm Processing";
            case IO_OPERATION -> "IO Operation";
            case BASIC_STATEMENT -> "Basic Statement";
            default -> "Unknown";
        };
    }

    private int getIndentationLevel(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ' || line.charAt(i) == '\t') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    private String createIndentedComment(String line, String comment) {
        int indentLevel = getIndentationLevel(line);
        StringBuilder sb = new StringBuilder();
        sb.append(" ".repeat(Math.max(0, indentLevel)));
        sb.append(comment);
        return sb.toString();
    }

    public String filterModules(String code, Set<Integer> visibleModules) {
        String[] lines = code.split("\n");
        StringBuilder result = new StringBuilder();

        // Initialize module content and track existing modules
        Set<Integer> existingModules = new HashSet<>();
        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            if (lineModuleMap.containsKey(lineNumber)) {
                int moduleType = lineModuleMap.get(lineNumber);
                moduleContent.computeIfAbsent(moduleType, k -> new ArrayList<>()).add(lines[i]);
                existingModules.add(moduleType);
            }
        }

        // Process each line
        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            if (lineModuleMap.containsKey(lineNumber)) {
                int moduleType = lineModuleMap.get(lineNumber);

                // Check if this is the start of a new module
                if (moduleStartLines.containsKey(moduleType) && lineNumber == moduleStartLines.get(moduleType)) {
                    // Only add comment if the module exists in the code
                    if (existingModules.contains(moduleType)) {
                        String comment = "\n" + createIndentedComment(lines[i], "// " + getModuleName(moduleType) + ": \n");
                        result.append(comment);
                    }

                    // If module is hidden, skip to the end and add empty lines
                    if (!visibleModules.contains(moduleType)) {
                        int endLine = moduleEndLines.get(moduleType);
                        // Add empty lines for hidden code
                        while (i < endLine - 1) {
                            result.append("\n");
                            i++;
                        }
                        continue;
                    }
                }

                if (visibleModules.contains(moduleType)) {
                    // Always show control statements and their immediate children
                    if (controlStatementLines.contains(lineNumber)) {
                        result.append(lines[i]).append("\n");
                    } else {
                        // For non-control statements, check variable dependencies
                        if (moduleType != DATA_STRUCTURE_SELECTION) {
                            boolean containsHiddenVar = false;
                            for (String var : declaredVariables) {
                                if (lines[i].contains(var)) {
                                    containsHiddenVar = true;
                                    break;
                                }
                            }
                            if (!containsHiddenVar) {
                                result.append(lines[i]).append("\n");
                            } else {
                                result.append("\n");
                            }
                        } else {
                            result.append(lines[i]).append("\n");
                        }
                    }
                } else {
                    result.append("\n");
                }
            } else {
                // Lines without module type are considered basic statements
                if (visibleModules.contains(BASIC_STATEMENT)) {
                    result.append(lines[i]).append("\n");
                } else {
                    result.append("\n");
                }
            }
        }

        // Format the output to ensure no more than 2 consecutive empty lines
        StringBuilder res = new StringBuilder();
        int emptyLineCount = 0;
        for (String s : result.toString().split("\n")) {
            if (s.trim().isEmpty()) {
                emptyLineCount++;
                if (emptyLineCount <= 1) {
                    res.append(s).append("\n");
                }
            } else {
                emptyLineCount = 0;
                res.append(s).append("\n");
            }
        }
        return res.toString();
    }

    // Helper method to get all module types
    public static Set<Integer> getAllModuleTypes() {
        Set<Integer> types = new HashSet<>();
        types.add(DATA_STRUCTURE_DEFINITION);
        types.add(DATA_STRUCTURE_SELECTION);
        types.add(ALGORITHM_PROCESSING);
        types.add(IO_OPERATION);
        types.add(BASIC_STATEMENT);
        return types;
    }

    public Set<String> getDeclaredVariables() {
        return declaredVariables;
    }
}