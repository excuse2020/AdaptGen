package realize.ga;

import realize.encode.CodeHash;
import realize.fitness.CodeValid;
import realize.fitness.FrequentCodeBlock;
import realize.process.Formatting;
import realize.process.MatchExp;
import realize.utils.CodeUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GAStart {

    public String path;

    // 连续epoch轮的fitness都没有变化，就停止演化
    public final int epoch = 500;
    public final int fileLength = 20;
    public final int minLength = 3;
    public final int populationSize = 100;
    public final double mutationRate = 0.7;

    // 0.15 0.3 0.35 0.1 0.1
    public static double freWeight = 0.1;
    public static double editDisWeight = 0.25;
    public static double repetitionWeight = 0.2;
    public static double csfWeight = 0.25;
    public static double pfWeight = 0.1;
    public static double validScoreWeight = 0.05;
    public static double coverageWeight = 0.05;

    public static List<List<String>> codeList;
    public static List<List<Integer>> codeHashList;
    public static List<List<Integer>> expHashList;

    public static int codeMinLen;
    public static int codeMaxLen;

    public static FrequentCodeBlock f;

    public GAStart(String path) {
        this.path = path;
        codeList = new ArrayList<>();
        codeHashList = new ArrayList<>();
        expHashList = new ArrayList<>();
        codeMinLen = 1;
        codeMaxLen = 1;
        f = null;
    }

    public void start() throws IOException {
        Path proPath = Paths.get(path);
        Path dir = Paths.get("result" + path.split("dataset5")[1]);
        if (Files.isDirectory(dir) && Files.exists(Path.of(dir + "/astringency"))) {
            return;
        }

        List<String> files = Files.list(proPath)
                .filter(Files::isRegularFile)
                .map(x -> x.getFileName().toString()).toList();

        CodeHash.init();
        List<List<Integer>> freCBDataset = new ArrayList<>();

        for (int i = 0, cnt = 0; i < files.size() && cnt < fileLength; i++) {

            Path p = null;
            try {
                p = proPath.resolve(files.get(i));
                String s = Files.readString(p);

                List<String> codes = Arrays.stream(Formatting.formatCode(s).split("\n"))
                        .filter(x -> !x.trim().equals("")).toList();

                List<String> exps = Arrays.stream(MatchExp.getMatchExpByCode(s).split("\n"))
                        .filter(x -> !x.trim().equals("")).toList();

                if (!CodeValid.isCodeValid(codes)) continue;

                if (codes.size() < minLength) continue;

                for (int j = 0; j < codes.size(); j++) {
                    CodeHash.insertCode(codes.get(j), exps.get(j));
                }

                codeMinLen = Math.min(codeMinLen, codes.size());
                codeMaxLen = Math.max(codeMaxLen, codes.size());

                codeList.add(codes);
                codeHashList.add(CodeHash.codesToHashs(codes));
                expHashList.add(CodeHash.codesToExpHashs(codes));
                freCBDataset.addAll(CodeUtils.cutting(codes).stream().map(x -> x.stream().map(CodeHash::codeToExpHash).toList()).toList());
                cnt++;
            } catch (Exception e) {
                System.out.println(p + ": Exception");
            } finally {
                continue;
            }
        }

        f = new FrequentCodeBlock(freCBDataset, files.size() / 2);
        writeDatasetInfo(files.size());

        GA ga = new GA(populationSize, mutationRate);
        ga.initPopulation3();

        double cur_best = 0;

        for (int k = 1, cnt = 0; cnt < epoch; k++) {
            ga.start();
            Chromosome fittest = ga.getFittest();
            if (fittest.fitness > cur_best) {
                cur_best = fittest.fitness;
                cnt = 1;
            } else if (fittest.fitness == cur_best){
                cnt++;
            }
            write(k + " " + fittest.fitness + " " + fittest.freCbScore + " " + fittest.editDis + " " +
                    fittest.csf + " " + fittest.pf + " " + fittest.validScore + " " + fittest.repetition + " " +
                    fittest.coverage, k != 1, "astringency");
        }

        Chromosome fittest = ga.getFittest();
        List<String> finalCode = CodeHash.getFinalCode(fittest.genes.codes);
        for (int i = 0; i < finalCode.size(); i++) {
            write(finalCode.get(i), i != 0, "code");
        }

        write(fittest.fitness + " " + fittest.freCbScore + " " + fittest.editDis + " " +
                fittest.csf + " " + fittest.pf + " " + fittest.validScore + " " + fittest.repetition + " " + fittest.coverage, true, "fittest");
        System.out.println("Completed.");
    }

    private void writeDatasetInfo(int m) {
        GA ga_t = new GA(m, 0.3);
        ga_t.initPopulation(codeHashList);
        double fitness_avg = 0;
        double freCbScore_avg = 0;
        double editDis_avg = 0;
        double csf_avg = 0;
        double pf_avg = 0;
        double validScore_avg = 0;
        double repetition_avg = 0;
        double coverage_avg = 0;
        for (Chromosome chromosome : ga_t.population) {
            fitness_avg += chromosome.fitness;
            freCbScore_avg += chromosome.freCbScore;
            editDis_avg += chromosome.editDis;
            csf_avg += chromosome.csf;
            pf_avg += chromosome.pf;
            validScore_avg += chromosome.validScore;
            repetition_avg += chromosome.repetition;
            coverage_avg += chromosome.coverage;
        }
        int n = ga_t.population.size();
        fitness_avg /= n;
        freCbScore_avg /= n;
        editDis_avg /= n;
        csf_avg /= n;
        pf_avg /= n;
        validScore_avg /= n;
        repetition_avg /= n;
        coverage_avg /= n;
        write(fitness_avg + " " + freCbScore_avg + " " + editDis_avg + " " +
                csf_avg + " " + pf_avg + " " + validScore_avg + " " + repetition_avg + " " + coverage_avg, false, "fittest");
        Chromosome dataset_fittest = ga_t.getFittest();
        // 原数据集的已有最优解
        write(dataset_fittest.fitness + " " + dataset_fittest.freCbScore + " " + dataset_fittest.editDis + " " +
                dataset_fittest.csf + " " + dataset_fittest.pf + " " + dataset_fittest.validScore + " " + dataset_fittest.repetition + " " + dataset_fittest.coverage, true, "fittest");
    }

    private void write(String s, boolean flag, String fn) {

        String directoryPath = "result" + path.split("dataset5")[1];
        String filePath = directoryPath + "/" + fn;

        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(filePath);
        try {
            FileWriter writer = new FileWriter(file, flag);
            writer.write(s + "\n");
            writer.close();
        } catch (IOException e) {
            System.out.println("Error creating file: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        String dir1 = "datasets/dataset5";
        File[] dir2s = new File(dir1).listFiles(File::isDirectory);
        int k = 1;
        for (File dir2 : dir2s) {
            File[] dir3s = dir2.listFiles(File::isDirectory);
            for (File dir3 : dir3s) {
                System.out.println("--------------------------------------------------------------------------------------------");
                long startTime = System.currentTimeMillis();
                GAStart g = new GAStart(dir3.getAbsolutePath());
                System.out.print(k + ": " + dir3.getAbsolutePath() + ": ");
                Thread gaThread = new Thread(() -> {
                    try {
                        g.start();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                gaThread.start();
                try {
                    gaThread.join(TimeUnit.MINUTES.toMillis(10)); // Wait for up to 10 minutes
                    if (gaThread.isAlive()) {
                        gaThread.interrupt(); // Interrupt if still running
                        System.out.println("Skipped due to timeout.");
                    }
                } catch (InterruptedException e) {
                    System.out.println("Interrupted.");
                }

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                System.out.println("Iteration duration: " + (duration / 1000) + " seconds.");
                System.out.println("--------------------------------------------------------------------------------------------");
                k++;
            }
        }
    }
}