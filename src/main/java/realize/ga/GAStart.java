package realize.ga;

import realize.fitness.FrequentCodeBlock;

import java.util.ArrayList;

import java.util.List;

public class GAStart {

    public static int epoch = 500;
    public static int populationSize = 100;
    public static double mutationRate = 0.7;

    public static double freWeight = 0.1;
    public static double pfWeight = 0.1;
    public static double csfWeight = 0.25;
    public static double editDisWeight = 0.25;
    public static double coverageWeight = 0.05;
    public static double repetitionWeight = 0.2;
    public static double validScoreWeight = 0.05;

    public static List<List<String>> codeList;
    public static List<List<Integer>> codeHashList;
    public static List<List<Integer>> expHashList;

    public static int codeMinLen;
    public static int codeMaxLen;

    public static FrequentCodeBlock f;

    public static void init(int epoch_, int populationSize_, double mutationRate_) {
        epoch = epoch_;
        populationSize = populationSize_;
        mutationRate = mutationRate_;
        codeList = new ArrayList<>();
        codeHashList = new ArrayList<>();
        expHashList = new ArrayList<>();
        codeMinLen = 1;
        codeMaxLen = 1;
        f = null;
    }
}