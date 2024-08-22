package realize.fitness;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepetitiveRate {

    public static double cac(List<Integer> exps1, List<Integer> exps2) {
        int res = 0;
        Map<Integer, Integer> map1 = new HashMap<>();
        Map<Integer, Integer> map2 = new HashMap<>();
        exps1.forEach(exp -> map1.put(exp, map1.getOrDefault(exp, 0) + 1));
        exps2.forEach(exp -> map2.put(exp, map2.getOrDefault(exp, 0) + 1));
        // 2中的所有代码1中有的 越小越好
        for (Map.Entry<Integer, Integer> e : map2.entrySet()) {
            int k = e.getKey();
            int v = e.getValue();
            int count = map1.getOrDefault(k, 0);
            res += Math.abs(v - count);
        }
        // 1中代码2中没有的 冗余 越小越好
        for (Map.Entry<Integer, Integer> e : map1.entrySet()) {
            int k = e.getKey();
            int v = e.getValue();
            if (!map2.containsKey(k)) {
                res += v;
            }
        }
        return 1 - 1.0 * res / exps2.size();
    }

    // 计算代码覆盖率，代码1 / 代码2  公共行数 / 代码二行数
    public static double cac2(List<Integer> code1, List<Integer> code2) {
        int cnt = 0;
        Map<Integer, Integer> map1 = new HashMap<>();
        Map<Integer, Integer> map2 = new HashMap<>();
        code1.forEach(code -> map1.put(code, map1.getOrDefault(code, 0) + 1));
        code2.forEach(code -> map2.put(code, map2.getOrDefault(code, 0) + 1));
        for (Map.Entry<Integer, Integer> e : map2.entrySet()) {
            int count = map1.getOrDefault(e.getKey(), 0);
            cnt += Math.min(e.getValue(), count);
        }
        return 1.0 * cnt / code2.size();
    }

    public static void main(String[] args) {
        List<Integer> exp1 = Arrays.asList(6, 7, 8, 1, 2, 3, 4, 5);
        List<Integer> exp2 = Arrays.asList(6, 7, 8, 9);
        System.out.println(cac(exp1, exp2));
    }
}
