package realize.fitness;

import java.util.List;
import java.util.Stack;

public class CodeValid {

    public static boolean isCodeValid(List<String> code) {
        Stack<String> stack = new Stack<>();

        stack.push("start");
        for (String t : code) {
            int tBCnt = findFirstNonSpaceChar(t);
            int curBCnt = findFirstNonSpaceChar(stack.peek());
            if (stack.peek().trim().charAt(stack.peek().trim().length() - 1) == '{') curBCnt++;
            if (t.trim().charAt(t.trim().length() - 1) == '}' || (t.trim().charAt(t.trim().length() - 2) == '}' && t.trim().charAt(t.trim().length() - 1) == ';')) curBCnt--;

            if (tBCnt != curBCnt) {
                return false;
            }

            if (t.trim().charAt(t.trim().length() - 1) == '}' || (t.trim().charAt(t.trim().length() - 2) == '}' && t.trim().charAt(t.trim().length() - 1) == ';')) {
                while (true) {
                    if (stack.isEmpty()) return false;
                    String p = stack.pop();
                    if (p.trim().charAt(p.trim().length() - 1) == '{' && findFirstNonSpaceChar(p) == tBCnt) break;
                }
                continue;
            }
            stack.push(t);
        }
        return stack.peek().equals("start");
    }

    public static int findFirstNonSpaceChar(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
