package realize.ga;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import realize.encode.CodeHash;
import realize.fitness.FrequentCodeBlock;
import realize.process.Formatting;
import realize.process.MatchExp;
import realize.utils.CodeUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class generateTemplateByGA {

    public static int epoch = 500;
    public static int populationSize = 100;
    public static double mutationRate = 0.9;

    static class Score {
        double fitness;
        double freCbScore;
        double editDis;
        double csf;
        double pf;
        double validScore;
        double repetition;
        double coverage;

        public Score(Chromosome chromosome) {
            if (chromosome != null) {
                this.fitness = chromosome.fitness;
                this.freCbScore = chromosome.freCbScore;
                this.editDis = chromosome.editDis;
                this.csf = chromosome.csf;
                this.pf = chromosome.pf;
                this.validScore = chromosome.validScore;
                this.repetition = chromosome.repetition;
                this.coverage = chromosome.coverage;
            }
        }
    }

    static class Result {
        String Template;
        int Epoch;
        long Time;
        String Complete;
        int Success;
        Score Score;

        public Result(String template, int epoch, long time, String complete, int success, Chromosome fittest) {
            Template = template;
            Epoch = epoch;
            Time = time;
            Complete = complete;
            Success = success;
            Score = new Score(fittest);
        }
    }

    private static String getCurrentTime() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static void outputResult(Result result) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        System.out.println(gson.toJson(result));
    }

    public static void main(String[] args) {
        epoch = Integer.parseInt(args[0]);
        populationSize = Integer.parseInt(args[1]);
        mutationRate = Double.parseDouble(args[2]);
        start(epoch, populationSize, mutationRate, Arrays.stream(args, 3, args.length).toList());
    }

    public static void start(int epoch, int populationSize, double mutationRate, List<String> acCodeList) {
        long startTime = System.currentTimeMillis();
        List<List<Integer>> freCBDataset = new ArrayList<>();
        CodeHash.init();
        GAStart.init(epoch, populationSize, mutationRate);

        int lines = 0;
        for (String s : acCodeList) {
            try {
                List<String> codes = Arrays.stream(Formatting.formatCode(s).split("\n"))
                        .filter(x -> !x.trim().isEmpty()).toList();

                List<String> exps = Arrays.stream(MatchExp.getMatchExpByCode(s).split("\n"))
                        .filter(x -> !x.trim().isEmpty()).toList();

                for (int j = 0; j < codes.size(); j++) {
                    CodeHash.insertCode(codes.get(j), exps.get(j));
                }

                GAStart.codeMinLen = Math.min(GAStart.codeMinLen, codes.size());
                GAStart.codeMaxLen = Math.max(GAStart.codeMaxLen, codes.size());

                GAStart.codeList.add(codes);
                GAStart.codeHashList.add(CodeHash.codesToHashs(codes));
                GAStart.expHashList.add(CodeHash.codesToExpHashs(codes));
                freCBDataset.addAll(CodeUtils.cutting(codes).stream()
                        .map(x -> x.stream().map(CodeHash::codeToExpHash).toList()).toList());
                lines += codes.size();
            } catch (Exception e) {
                System.err.println(s + "\tException - " + e.getMessage());
            }
        }

        try {
            GAStart.f = new FrequentCodeBlock(freCBDataset, freCBDataset.size() / 2, lines / 4);
            GAStart.f.mine();

            GA ga = new GA(populationSize, mutationRate);
            ga.initPopulation3();

            double cur_best = 0;
            int actualEpoch = 0;

            for (int cnt = 0; cnt < epoch; ) {
                actualEpoch++;
                long timeElapsed = System.currentTimeMillis() - startTime;
                if (timeElapsed > TimeUnit.MINUTES.toMillis(10)) {
                    Chromosome fittest = ga.getFittest();
                    String template = "";
                    if (fittest != null) {
                        template = String.join("\n", CodeHash.hashsToCodes(fittest.genes.hashs));
                    }
                    outputResult(new Result(template, actualEpoch, timeElapsed, getCurrentTime(), 0, fittest));
                    return;
                }

                ga.start();
                Chromosome fittest = ga.getFittest();
                if (fittest.fitness > cur_best) {
                    cur_best = fittest.fitness;
                    cnt = 1;
                } else if (fittest.fitness == cur_best) {
                    cnt++;
                }
            }

            Chromosome fittest = ga.getFittest();
            long timeElapsed = System.currentTimeMillis() - startTime;
            if (fittest != null) {
                String template = String.join("\n", CodeHash.hashsToCodes(fittest.genes.hashs));
                outputResult(new Result(template, actualEpoch, timeElapsed, getCurrentTime(), 1, fittest));
            }

        } catch (Exception e) {
            long timeElapsed = System.currentTimeMillis() - startTime;
            outputResult(new Result("", 0, timeElapsed, getCurrentTime(), -1, null));
        }
    }
}