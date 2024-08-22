package realize.encode;

import realize.process.MatchExp;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeHash {

    /*
    code <-> hash
    exp <-> hash
    code hash -> exp hash
     */

    public static Map<String, Integer> codeToHash;
    public static Map<Integer, String> hashToCode;

    public static Map<String, Integer> expToHash;
    public static Map<Integer, String> hashToExp;

    public static Map<Integer, Integer> codeHashToExpHash;

    public static Map<String, String> codeToExp;

    public static Map<Integer, Integer> hashToType;

    public static Map<String, String> codeToOriExp;

    public static void init() {
        codeToHash = new HashMap<>();
        hashToCode = new HashMap<>();
        expToHash = new HashMap<>();
        hashToExp = new HashMap<>();
        codeHashToExpHash = new HashMap<>();
        codeToExp = new HashMap<>();
        hashToType = new HashMap<>();
        codeToOriExp = new HashMap<>();
    }

    public static void print() {
        System.out.println("Code <-> Hash:");
        codeToHash.forEach((k, v) -> System.out.println(k + ": " + v));

        System.out.println("\nExp <-> Hash:");
        expToHash.forEach((k, v) -> System.out.println(k + ": " + v));

        System.out.println("\nCode <-> ExpHash:");
        codeToHash.forEach((k, v) -> System.out.println(k + ": " + codeToExpHash(k)));

        System.out.println("\nCode <-> Exp:");
        codeToExp.forEach((k, v) -> System.out.println(k + ": " + v));
    }

    public static List<String> hashsToCodes(List<Integer> codes) {
        return codes.stream().map(x -> hashToCode.get(x)).toList();
    }

    public static List<Integer> codesToHashs(List<String> codes) {
        return codes.stream().map(x -> codeToHash.get(x)).toList();
    }

    public static List<Integer> codesToExpHashs(List<String> codes) {
        return codes.stream().map(CodeHash::codeToExpHash).toList();
    }

    public static List<Integer> codesHashToExpsHash(List<Integer> codesHash) {
        return codesHash.stream().map(x -> codeHashToExpHash.get(x)).toList();
    }

    public static List<String> codesToExps(List<String> codes) {
        return codes.stream().map(x -> codeToExp.get(x)).toList();
    }

    public static List<String> getFinalCode(List<Integer> codes) {
        return hashsToCodes(codes).stream().map(x -> codeToOriExp.get(x)
                        .replace("SIMPLE_NAME", "").replace("$", "")
                                .replace("SIMPLE_TYPE", "").replace("$", "")
                                .replace(", ", ",").replace(",", ", ")
                                .replaceAll("INFIX_EXPRESSION", "Expression")
                                .replace("PREFIX_EXPRESSION", "Expression")
                                .replace("POSTFIX_EXPRESSION", "Expression"))
                .filter(x -> !x.trim().equals("Expression;")).toList();
    }

    public static int codeToExpHash(String code) {
        return codeHashToExpHash.get(codeToHash.get(code));
    }


    public static void insertCode(String code, String oriExp) throws IOException {

        String exp = oriExp.replaceAll("SIMPLE_NAME\\$(.*?)\\$", "SIMPLE_NAME").replaceAll("SIMPLE_TYPE\\$(.*?)\\$", "SIMPLE_TYPE");

        if (codeToHash.containsKey(code)) return;

        int codeHash = 0;
        for (char c : code.toCharArray()) {
            codeHash += c;
        }
        while (codeToHash.containsValue(codeHash)) {
            codeHash++;
        }

        int expHash = expToHash.getOrDefault(exp, 0);

        // exp需要创建
        if (expHash == 0) {
            for (char c : code.toCharArray()) {
                expHash += c;
            }
            while (expToHash.containsValue(expHash)) {
                expHash++;
            }

            // 更新type
            MatchExp.getMatchExpByCode(code);
            if (MatchExp.type.contains(24) || MatchExp.type.contains(70) || MatchExp.type.contains(61)) {
                // 循环
                hashToType.put(expHash, 0);
            } else if (code.contains("else if")) {
                hashToType.put(expHash, 1);
            } else if (code.contains("else")) {
                hashToType.put(expHash, 2);
            } else if (MatchExp.type.contains(25)) {
                // if
                hashToType.put(expHash, 3);
            } else if (code.contains("return")) {
                // return
                hashToType.put(expHash, 4);
            } else if (MatchExp.type.contains(50)) {
                // switch
                hashToType.put(expHash, 5);
            } else if (MatchExp.type.contains(56)) {
                // class
                hashToType.put(expHash, 6);
            } else if (MatchExp.type.contains(7)) {
                // 赋值
                hashToType.put(expHash, 7);
            } else if (MatchExp.type.contains(60)) {
                // VARIABLE_DECLARATION_STATEMENT
                hashToType.put(expHash, 8);
            } else if (MatchExp.type.contains(31)) {
                // METHOD_DECLARATION
                hashToType.put(expHash, 9);
            } else if (MatchExp.type.contains(32)) {
                // METHOD_INVOCATION
                hashToType.put(expHash, 10);
            } else if (MatchExp.type.contains(21)) {
                // EXPRESSION_STATEMENT
                hashToType.put(expHash, 11);
            } else if (code.contains("=")) {
                hashToType.put(expHash, 7);
            } else if (code.contains("}")) {
                hashToType.put(expHash, 13);
            } else {
                hashToType.put(expHash, 12);
            }
        }

        codeToHash.put(code, codeHash);
        hashToCode.put(codeHash, code);

        expToHash.put(exp, expHash);
        hashToExp.put(expHash, exp);

        codeToExp.put(code, exp);

        codeHashToExpHash.put(codeHash, expHash);

        codeToOriExp.put(code, oriExp);
    }
}

