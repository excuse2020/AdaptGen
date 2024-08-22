package evalute;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RQ1_QR {

    public static void main(String[] args) {
        String dir1 = "results/result_nk";
        int cnt = 0, sum = 0;
        File[] dir2s = new File(dir1).listFiles(File::isDirectory);
        System.out.println(dir2s.length);
        for (File dir2 : dir2s) {
            File[] dir3s = dir2.listFiles(File::isDirectory);
            for (File dir3 : dir3s) {
                String filePath = dir3.getAbsolutePath() + "/fittest";
                System.out.println(filePath + ": ");
                if (check(filePath)) {
                    System.out.println("true");
                    cnt++;
                } else {
                    System.out.println("false");
                }
                sum++;
            }
        }
        System.out.println(cnt);
        System.out.println(sum);
        System.out.println(1.0 * cnt / sum);
    }

    static boolean check(String path) {

        List<int[]> fittness = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                int i = 0;
                int[] temp = new int[6];
                for (String s : line.split(" ")) {
                    temp[i++] = Integer.parseInt(s);
                }
                fittness.add(temp);
            }

            for (int i = 0; i < 6; i++) {
                if (fittness.get(0)[i] > fittness.get(2)[i]) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            System.err.println("An error occurred while reading the file: " + e.getMessage());
        } finally {
            return false;
        }
    }
}
