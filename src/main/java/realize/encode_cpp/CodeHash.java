package realize.encode_cpp;

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

        System.out.println("\nCode <-> Type:");
        codeToHash.forEach((k, v) -> System.out.println(k + ": " + hashToType.get(codeToExpHash(k))));
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
                        .replace(", ", ",").replace(",", ", "))
                .filter(x -> !x.trim().equals("Expression;")).toList();
    }

    public static int codeToExpHash(String code) {
        return codeHashToExpHash.get(codeToHash.get(code));
    }

    public static void insertCode(String code, String oriExp, Integer type) throws IOException {

        String exp = oriExp
                .replaceAll("SIMPLE_NAME\\$(.*?)\\$", "SIMPLE_NAME")
                .replaceAll("SIMPLE_TYPE\\$(.*?)\\$", "SIMPLE_TYPE"); // 兼容处理，C++ 版可能无需 SIMPLE_TYPE

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
            for (char c : exp.toCharArray()) {
                expHash += c;
            }
            while (expToHash.containsValue(expHash)) {
                expHash++;
            }
           
            hashToType.put(expHash, type);
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

