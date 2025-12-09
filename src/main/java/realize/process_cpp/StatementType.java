package realize.process_cpp;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLambdaExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTRangeBasedForStatement;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;

import java.util.*;

public class StatementType {

    private static final int TYPE_CLASS = 6;
    private static final int TYPE_FUNCTION = 9;
    private static final int TYPE_VAR = 8;
    private static final int TYPE_ASSIGN = 7;
    private static final int TYPE_CALL = 10;
    private static final int TYPE_IF = 3;
    private static final int TYPE_ELSE = 2;
    private static final int TYPE_ELSE_IF = 1;
    private static final int TYPE_RETURN = 4;
    private static final int TYPE_LOOP = 0;
    private static final int TYPE_SWITCH = 5;
    private static final int TYPE_CASE = 12;
    private static final int TYPE_EXPR = 11;
    private static final int TYPE_BREAK = 12;
    private static final int TYPE_CONTINUE = 12;
    private static final int TYPE_OTHER = 12;
    private static final int TYPE_BLOCK_END = 13;

    public static List<Integer> codeToTypeList(String code) {
        IASTTranslationUnit tu = parse(code, true);
        if (tu == null) tu = parse(code, false);

        Map<Integer, List<Integer>> lineToTypes = new HashMap<>();
        if (tu != null) {
            traverse(tu, lineToTypes);
        }

        String[] lines = code.split("\n", -1);
        List<Integer> res = new ArrayList<>(lines.length);
        for (int i = 1; i <= lines.length; i++) {
            List<Integer> types = lineToTypes.getOrDefault(i, Collections.emptyList());
            if (!types.isEmpty()) {
                res.add(types.get(0));
            } else {
                String t = lines[i - 1].trim();
                if (t.equals("}") || t.equals("};")) {
                    res.add(TYPE_BLOCK_END);
                } else {
                    res.add(TYPE_OTHER);
                }
            }
        }
        return res;
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

    private static void traverse(IASTNode node, Map<Integer, List<Integer>> lineToTypes) {
        if (node == null) return;

        if (node instanceof IASTTranslationUnit) {
            for (IASTNode child : node.getChildren()) traverse(child, lineToTypes);
            return;
        }

        if (node instanceof IASTFunctionDefinition f) {
            recordType(node, TYPE_FUNCTION, lineToTypes);
            traverse(f.getBody(), lineToTypes);
            return;
        }

        if (node instanceof IASTCompoundStatement b) {
            for (IASTStatement s : b.getStatements()) traverse(s, lineToTypes);
            return;
        }

        if (node instanceof IASTDeclarationStatement s) {
            traverse(s.getDeclaration(), lineToTypes);
            return;
        }

        if (node instanceof IASTSimpleDeclaration d) {
            IASTDeclSpecifier spec = d.getDeclSpecifier();
            if (spec instanceof IASTCompositeTypeSpecifier
                    || spec instanceof IASTEnumerationSpecifier) {
                recordType(node, TYPE_CLASS, lineToTypes);
                if (spec instanceof ICPPASTCompositeTypeSpecifier c) {
                    for (IASTDeclaration m : c.getMembers()) {
                        traverse(m, lineToTypes);
                    }
                } else if (spec instanceof IASTCompositeTypeSpecifier c) {
                    for (IASTDeclaration m : c.getMembers()) {
                        traverse(m, lineToTypes);
                    }
                }
            } else {
                recordType(node, TYPE_VAR, lineToTypes);
            }
            return;
        }

        if (node instanceof IASTIfStatement i) {
            recordType(node, TYPE_IF, lineToTypes);
            traverse(i.getThenClause(), lineToTypes);
            IASTStatement elseClause = i.getElseClause();
            if (elseClause != null) {
                if (elseClause instanceof IASTIfStatement) {
                    recordType(elseClause, TYPE_ELSE_IF, lineToTypes);
                } else {
                    recordType(elseClause, TYPE_ELSE, lineToTypes);
                }
                traverse(elseClause, lineToTypes);
            }
            return;
        }

        if (node instanceof IASTSwitchStatement s) {
            recordType(node, TYPE_SWITCH, lineToTypes);
            traverse(s.getBody(), lineToTypes);
            return;
        }

        if (node instanceof IASTCaseStatement || node instanceof IASTDefaultStatement) {
            recordType(node, TYPE_CASE, lineToTypes);
            return;
        }

        if (node instanceof IASTBreakStatement) {
            recordType(node, TYPE_BREAK, lineToTypes);
            return;
        }

        if (node instanceof IASTContinueStatement) {
            recordType(node, TYPE_CONTINUE, lineToTypes);
            return;
        }

        if (node instanceof IASTForStatement
                || node instanceof IASTWhileStatement
                || node instanceof IASTDoStatement
                || node instanceof ICPPASTRangeBasedForStatement) {
            recordType(node, TYPE_LOOP, lineToTypes);
            if (node instanceof IASTForStatement f) {
                traverse(f.getBody(), lineToTypes);
            } else if (node instanceof IASTWhileStatement) {
                traverse(((IASTWhileStatement) node).getBody(), lineToTypes);
            } else if (node instanceof IASTDoStatement) {
                traverse(((IASTDoStatement) node).getBody(), lineToTypes);
            } else if (node instanceof ICPPASTRangeBasedForStatement) {
                traverse(((ICPPASTRangeBasedForStatement) node).getBody(), lineToTypes);
            }
            return;
        }

        if (node instanceof IASTReturnStatement) {
            recordType(node, TYPE_RETURN, lineToTypes);
            return;
        }

        if (node instanceof IASTExpressionStatement) {
            IASTExpression e = ((IASTExpressionStatement) node).getExpression();
            if (e instanceof IASTFunctionCallExpression) {
                recordType(node, TYPE_CALL, lineToTypes);
            } else if (e instanceof IASTBinaryExpression b) {
                if (isAssignmentOperator(b.getOperator())) {
                    recordType(node, TYPE_ASSIGN, lineToTypes);
                } else {
                    recordType(node, TYPE_EXPR, lineToTypes);
                }
            } else if (e instanceof ICPPASTLambdaExpression) {
                recordType(node, TYPE_EXPR, lineToTypes);
            } else {
                recordType(node, TYPE_EXPR, lineToTypes);
            }
            return;
        }

        for (IASTNode child : node.getChildren()) traverse(child, lineToTypes);
    }

    private static void recordType(IASTNode node, int type, Map<Integer, List<Integer>> lineToTypes) {
        IASTFileLocation loc = node.getFileLocation();
        if (loc == null) return;
        int line = loc.getStartingLineNumber();
        lineToTypes.computeIfAbsent(line, k -> new ArrayList<>()).add(type);
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
}