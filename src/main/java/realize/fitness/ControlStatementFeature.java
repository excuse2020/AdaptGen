package realize.fitness;

import realize.encode.CodeHash;
import realize.process.Formatting;
import realize.process.MatchExp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ControlStatementFeature {

    public List<Integer> codes;
    public List<int[]> matrix;
    public List<Integer> csf;

    private int typeN = 13;

    public ControlStatementFeature() {
    }

    public ControlStatementFeature(List<Integer> codes) {
        this.codes = codes;
        setMatrix();
    }

    private void setMatrix() {

        matrix = new ArrayList<>();
        csf = new ArrayList<>();
        int i = 0;
        while (i < codes.size()) {
            int type = CodeHash.hashToType.get(codes.get(i));
            if (type <= 3) {
                i = dfs(i, type);
            } else {
                i++;
            }
        }
    }

    public int dfs(int i, int ty) {
        csf.add(ty);
        int[] t = new int[typeN];
        matrix.add(t);
        int size = matrix.size();

        int j = i + 1;
        while (j < codes.size()) {
            int type = CodeHash.hashToType.get(codes.get(j));
            if (type <= 3) {
                t[type]++;
                j = dfs(j, type);
            } else if (type == typeN) {
                matrix.set(size - 1, t);
                return j + 1;
            } else {
                t[type]++;
                j++;
            }
        }
        return j;
    }

    private List<Double> sims;
    public double calculateSimilarity2(ControlStatementFeature p) {
        sims = new ArrayList<>();
        if (matrix.isEmpty() && p.matrix.isEmpty()) {
            return 1.0;
        } else if (matrix.isEmpty() || p.matrix.isEmpty()) {
            return 0;
        }
        dfs(matrix, p.matrix, new ArrayList<>(), 0, csf, p.csf);
        // return (int) (sims.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100);
        //System.out.println(sims);
        return sims.stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }

    public void dfs(List<int[]> a, List<int[]> b, List<Integer> t, int k, List<Integer> csf1, List<Integer> csf2) {
        if (t.size() == a.size()) {
            double avg = 0;
            for (int i = 0; i < a.size(); i++) {
                int j = t.get(i);
                if (!Objects.equals(csf1.get(i), csf2.get(j))) continue;
                avg += cac(a.get(i), b.get(j));
            }
            avg /= a.size();
            sims.add(avg);
            return;
        }
        for (int i = k; i < b.size(); i++) {
            List<Integer> tt = new ArrayList<>(t);
            tt.add(i);
            dfs(a, b, tt, i + 1, csf1, csf2);
        }
    }

    // |ai - bi|
    public double cac(int[] a, int[] b) {
        double res = 0;
        for (int i = 0; i < a.length; i++) {
            if (b[i] == 0) {
                if (a[i] == 0) res += 1;
                continue;
            }
            int t = b[i] - Math.abs(b[i] - a[i]);
            t = Math.max(0, t);
            res += 1.0 * t / b[i];
        }
        return res / a.length;
    }

    // (2 * min_size) / (size1 + size2) * max_sim
    public double calculateSimilarity(ControlStatementFeature p) {
        sims = new ArrayList<>();
        if (matrix.isEmpty() && p.matrix.isEmpty()) {
            return 1.0;
        } else if (matrix.isEmpty() || p.matrix.isEmpty()) {
            return 0;
        }
        if (matrix.size() == p.matrix.size()) {
            double res = 0;
            for (int i = 0; i < matrix.size(); i++) {
                if (Objects.equals(csf.get(i), p.csf.get(i))) {
                    res += cac(matrix.get(i), p.matrix.get(i));
                }
            }
            return res / p.matrix.size();
        }
        if (matrix.size() < p.matrix.size()) {
            dfs(matrix, p.matrix, new ArrayList<>(), 0, csf, p.csf);
        } else {
            dfs(p.matrix, matrix, new ArrayList<>(), 0, p.csf, csf);
        }
        int min = Math.min(matrix.size(), p.matrix.size());
        // return (int) (sims.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100);
        //System.out.println(sims);
        double maxCsf = sims.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        return maxCsf * 2 * min / (matrix.size() + p.matrix.size());
    }

    // |a[i] - b[i]| / (a[i] + b[i])
    public double cac2(int[] a, int[] b) {
        int s1 = 0, s2 = 0;
        for (int i = 0; i < a.length; i++) {
            s1 += Math.abs(a[i] - b[i]);
            s2 += a[i] + b[i];
        }
        return 1 - (1.0 * s1 / s2);
    }

    @Override
    public String toString() {
        String res = "";
        for (int i = 0; i < csf.size(); i++) {
            int type = csf.get(i);
            if (type == 0) res += "L ";
            else res += "C ";
            for (int j : matrix.get(i)) {
                res += j + " ";
            }
            res += "\n";
        }
        return res;
    }


    public static void main(String[] args) throws IOException {
        String code = "class Solution {\n" +
                "int fun() {\n" +
                "for (int i = 0; i < 10; i++) {\n" +
                "if (i % 2 == 0) {\n" +
                "f1(i);\n" +
                "} else {\n" +
                "f2(i);\n" +
                "}\n" +
                "}\n" +
                "return sum;\n" +
                "}\n" +
                "}";
        code = Formatting.formatCode(code);
        List<String> codes = Arrays.stream(code.split("\n")).toList();
        List<String> exps = Arrays.stream(MatchExp.getMatchExpByCode(code).split("\n")).toList();
        CodeHash.init();
        for (int i = 0; i < codes.size(); i++) {
            CodeHash.insertCode(codes.get(i), exps.get(i));
        }

        ControlStatementFeature csf1 = new ControlStatementFeature(CodeHash.codesToExpHashs(codes));
        System.out.println(csf1);
    }

}
