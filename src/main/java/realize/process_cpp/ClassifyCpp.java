package realize.process_cpp;

import realize.encode_cpp.CodeHash;
import realize.fitness.ControlStatementFeatureCpp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassifyCpp {

    String title;
    List<List<String>> codeList;
    List<String> filenameList;
    List<List<Integer>> hashList;
    List<ControlStatementFeatureCpp> csfList;
    double[][] sim;
    List<List<Integer>> classified;
    String dir;

    public ClassifyCpp(String dir, String d) throws IOException {
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

    public ClassifyCpp(List<List<String>> codeList) {
        this.codeList = codeList;
        sim = new double[codeList.size()][codeList.size()];
    }

    public void init() throws IOException {
        hashList = new ArrayList<>();
        csfList = new ArrayList<>();
        for (int i = 0; i < codeList.size(); i++) {
            try {
                CodeHash.init();
                List<String> code = codeList.get(i);
                List<String> exps = code.stream().filter(x -> !x.trim().equals("")).toList();
                List<Integer> typesPerLine = StatementType.codeToTypeList(String.join("\n", code));
                for (int j = 0; j < code.size(); j++) {
                    CodeHash.insertCode(code.get(j), exps.get(j), typesPerLine.get(j));
                }
                List<Integer> expHashs = CodeHash.codesToExpHashs(code);
                hashList.add(expHashs);
                csfList.add(new ControlStatementFeatureCpp(expHashs));
            } catch (OutOfMemoryError oom) {
                System.err.println("OutOfMemory on item " + i + " (" + (filenameList != null && i < filenameList.size() ? filenameList.get(i) : "unknown") + "), skip.");
                hashList.add(null);
                csfList.add(null);
                System.gc();
            } catch (Exception ex) {
                System.err.println("Error on item " + i + " (" + (filenameList != null && i < filenameList.size() ? filenameList.get(i) : "unknown") + "), skip: " + ex.getMessage());
                hashList.add(null);
                csfList.add(null);
            }
        }

        for (int i = 0; i < codeList.size(); i++) {
            for (int j = 0; j < codeList.size(); j++) {
                if (i == j) {
                    sim[i][j] = 1;
                    continue;
                }
                ControlStatementFeatureCpp fi = (csfList.size() > i ? csfList.get(i) : null);
                ControlStatementFeatureCpp fj = (csfList.size() > j ? csfList.get(j) : null);
                if (fi == null || fj == null) {
                    sim[i][j] = 0;
                    continue;
                }
                try {
                    sim[i][j] = (sim[j][i] == 0 ? fi.calculateSimilarity(fj) : sim[j][i]);
                } catch (OutOfMemoryError oom) {
                    System.err.println("OutOfMemory comparing " + i + " and " + j + ", set sim=0 and continue.");
                    sim[i][j] = 0;
                } catch (Exception ex) {
                    System.err.println("Error comparing " + i + " and " + j + ": " + ex.getMessage());
                    sim[i][j] = 0;
                }
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
            if (sim[i][j] < 0.45) {
                return false;
            }
        }
        return true;
    }
}
