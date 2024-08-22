package realize.fitness;

import realize.encode.CodeHash;

import java.util.List;

public class ProgramFeatures {

    public List<Integer> codes;
    public int[] matrix;

    private int typeN = 13;

    public ProgramFeatures() {
    }

    public ProgramFeatures(List<Integer> codes) {
        this.codes = codes;
        setMatrix();
    }

    public ProgramFeatures(int[] matrix) {
        this.matrix = matrix;
    }

    private void setMatrix() {
        matrix = new int[typeN];
        for (Integer code : codes) {
            int type = CodeHash.hashToType.get(code);
            if (type == typeN) continue;
            matrix[type]++;
        }
    }

    public double calculateSimilarity(ProgramFeatures p) {
        int[] pm = p.matrix;
        double sum = 0;
        for (int i = 0; i < matrix.length; i++) {
            if (pm[i] == 0) {
                if (p.matrix[i] == 0) sum += 1;
                continue;
            }
            int t = pm[i] - Math.abs(pm[i] - matrix[i]);
            t = Math.max(0, t);
            sum += 1.0 * t / pm[i];
        }
        return sum / matrix.length;
    }

    public static void main(String[] args) {
        ProgramFeatures p1 = new ProgramFeatures(new int[]{0, 0, 0, 4, 0, 0, 0, 0, 1, 0, 0, 0});
        ProgramFeatures p2 = new ProgramFeatures(new int[]{0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0});
        System.out.println(p1.calculateSimilarity(p2));
        System.out.println(p1.calculateSimilarity(p1));

    }
}
