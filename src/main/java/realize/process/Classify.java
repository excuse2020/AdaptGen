package realize.process;

import realize.encode.CodeHash;
import realize.fitness.ControlStatementFeature;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Classify {

    String title;
    List<List<String>> codeList;
    List<String> filenameList;
    List<List<Integer>> hashList;
    List<ControlStatementFeature> csfList;
    double[][] sim;
    List<List<Integer>> classified;
    String dir;

    public Classify(String dir, String d) throws IOException {
        this.dir = d;
        this.codeList = new ArrayList<>();
        this.filenameList = new ArrayList<>();
        Path proPath = Paths.get(dir);
        this.title = proPath.getFileName().toString();
        List<String> files = Files.list(proPath).filter(Files::isRegularFile).map(x -> x.getFileName().toString()).toList();
        for (String file : files) {
            Path p = proPath.resolve(file);
            String s = Files.readString(p);
            List<String> codes = Arrays.stream(s.split("\n")).toList();
            codeList.add(codes);
            filenameList.add(p.getFileName().toString());
        }
        sim = new double[codeList.size()][codeList.size()];
    }

    public Classify(List<List<String>> codeList) {
        this.codeList = codeList;
        sim = new double[codeList.size()][codeList.size()];
    }

    public void init() throws IOException {
        hashList = new ArrayList<>();
        csfList = new ArrayList<>();
        for (List<String> code : codeList) {
            CodeHash.init();
            List<String> exps = code.stream().filter(x -> !x.trim().equals("")).toList();
            for (int j = 0; j < code.size(); j++) {
                CodeHash.insertCode(code.get(j), exps.get(j));
            }
            List<Integer> expHashs = CodeHash.codesToExpHashs(code);
            hashList.add(expHashs);
            csfList.add(new ControlStatementFeature(expHashs));
        }

        for (int i = 0; i < codeList.size(); i++) {
            for (int j = 0; j < codeList.size(); j++) {
                if (i == j) {
                    sim[i][j] = 1;
                    continue;
                }
                sim[i][j] = (sim[j][i] == 0 ? csfList.get(i).calculateSimilarity(csfList.get(j)) : sim[j][i]);
            }
        }
    }

    public void classify() {
        classified = new ArrayList<>();
        for (int i = 0; i < codeList.size(); i++) {
            boolean flag = false;
            for (List<Integer> al : classified) {
                if (check(i, al)) {
                    al.add(i);
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                List<Integer> t = new ArrayList<>();
                t.add(i);
                classified.add(t);
            }
        }
    }

    public void printClassified() {
        classified.forEach(System.out::println);
    }

    private boolean check(int i, List<Integer> al) {
        for (Integer j : al) {
            if (sim[i][j] < 0.6) {
                return false;
            }
        }
        return true;
    }
}
