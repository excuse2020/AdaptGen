package realize.ga;

import realize.encode_cpp.CodeEncoding;
import realize.encode_cpp.CodeHash;
import realize.fitness.*;
import realize.utils.RandomUtils;

import java.io.IOException;
import java.util.*;

public class GA {

    private int populationSize;
    private double mutationRate;
    private Random random;

    public Set<Chromosome> population;

    public double fitnessSum = 0;

    public GA(int populationSize, double mutationRate) {
        this.populationSize = populationSize;
        this.random = new Random();
        this.population = new HashSet<>();
        this.mutationRate = mutationRate;
    }

    public void initPopulation3() throws IOException {
        this.population = new HashSet<>();
        while (population.size() < populationSize) {
            List<Integer> t = new ArrayList<>();
            int n = RandomUtils.genRandom(GAStart.codeMinLen, GAStart.codeMaxLen);
            for (int i = 0; i < n; i++) {
                List<Integer> list = CodeHash.codeToHash.values().stream().toList();
                int j = RandomUtils.genRandom(0, list.size() - 1);
                t.add(list.get(j));
            }
            population.add(new Chromosome(new CodeEncoding(t)));
        }
    }

    public List<Chromosome> selectParents() {

        List<Chromosome> list = new ArrayList<>(population);
        Collections.sort(list);

        int i = RandomUtils.genRandom(0, (list.size() - 1) / 2);
        int j = RandomUtils.genRandom(0, list.size() - 1);

        return List.of(list.get(i), list.get(j));
    }


    public List<Chromosome> crossover() throws IOException {
        List<Chromosome> offspring = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            List<Chromosome> parents = selectParents();
            offspring.addAll(parents.get(0).crossover(parents.get(1)));
        }
        return offspring;
    }


    public void mutate(List<Chromosome> p) throws IOException {
        List<Chromosome> t = new ArrayList<>(p);
        for (Chromosome chromosome : t) {
            if (random.nextDouble() <= mutationRate) {
                p.add(chromosome.mutate());
            }
        }
    }

    public Chromosome getFittest() {
        return Collections.max(population);
    }


    public void eliminate(List<Chromosome> newPopulation) {

        newPopulation = newPopulation.stream()
                .sorted(Comparator.reverseOrder()).toList();

        Set<Chromosome> set = new HashSet<>();
        for (Chromosome chromosome : newPopulation) {
            set.add(chromosome);
            if (set.size() == populationSize) break;
        }
        population = set;
        fitnessSum = set.stream().mapToDouble(x -> x.fitness).sum();
    }


    public void start() throws IOException {
        List<Chromosome> crossover = crossover();
        mutate(crossover);
        List<Chromosome> newPopulation = new ArrayList<>(population);
        newPopulation.addAll(crossover);
        eliminate(newPopulation);
    }
}


class Chromosome implements Comparable<Chromosome> {

    public CodeEncoding genes;
    public double fitness;

    public double freCbScore;
    public double editDis;
    public double csf;
    public double pf;
    public double validScore;
    public double repetition;
    public double coverage;

    public Chromosome(CodeEncoding genes) throws IOException {
        this.genes = genes;
        calculateFitness();
    }

    public void calculateFitness() {
        List<Integer> exps = genes.exps;
        List<Integer> hashs = genes.hashs;

        freCbScore = GAStart.f.getFreCBRatio(exps);

        editDis = 0;
        for (List<Integer> x : GAStart.expHashList) {
            editDis += EditDistanceCalculator.calculateEditDistance(exps, x);
        }
        editDis /= GAStart.expHashList.size();

        repetition = 0;
        for (List<Integer> x : GAStart.expHashList) {
            repetition += RepetitiveRate.cac(exps, x);
        }
        repetition /= GAStart.expHashList.size();

        coverage = 0;
        for (List<Integer> x : GAStart.codeHashList) {
            coverage = Math.max(coverage, RepetitiveRate.cac2(hashs, x));
        }

        validScore = CodeValid.isCodeValid(CodeHash.hashsToCodes(hashs)) ? 1 : 0;

        ControlStatementFeatureCpp c = new ControlStatementFeatureCpp(exps);
        csf = 0;
        if (validScore == 1) {
            for (List<Integer> x : GAStart.expHashList) {
                csf += c.calculateSimilarity(new ControlStatementFeatureCpp(x));
            }
            csf = csf / GAStart.expHashList.size();
        }

        ProgramFeatures p = new ProgramFeatures(exps);
        pf = 0;
        for (List<Integer> x : GAStart.expHashList) {
            pf += p.calculateSimilarity(new ProgramFeatures(x));
        }
        pf = pf / GAStart.expHashList.size();

        fitness = freCbScore * GAStart.freWeight + editDis * GAStart.editDisWeight
                + validScore * GAStart.validScoreWeight + csf * GAStart.csfWeight
                + pf * GAStart.pfWeight + repetition * GAStart.repetitionWeight
                + coverage * GAStart.coverageWeight;
    }

    public List<Chromosome> crossover(Chromosome partner) throws IOException {

        int i = RandomUtils.genRandom(0, genes.hashs.size() - 1);
        int j = RandomUtils.genRandom(0, partner.genes.hashs.size() - 1);

        List<Integer> codes1 = new ArrayList<>(genes.hashs);
        List<Integer> codes2 = new ArrayList<>(partner.genes.hashs);

        List<Integer> new1 = codes1.subList(0, i);
        List<Integer> new2 = codes2.subList(0, j);

        new1.addAll(codes2.subList(j, codes2.size()));
        new2.addAll(codes1.subList(i, codes1.size()));

        return List.of(new Chromosome(new CodeEncoding(new1)), new Chromosome(new CodeEncoding(new2)));
    }

    public Chromosome mutate() throws IOException {
        List<Integer> codes = new ArrayList<>(genes.hashs);
        int i = RandomUtils.genRandom(0, codes.size() - 1);
        int op = RandomUtils.genRandom(0, 3);
        List<Integer> list = CodeHash.codeToHash.values().stream().toList();
        int j = RandomUtils.genRandom(0, list.size() - 1);
        if (op == 0 && codes.size() > 1) { // 删除
            codes.remove(i);
        } else if (op == 1) { // 替换
            codes.set(i, list.get(j));
        } else if (op == 2) { // 添加
            codes.add(i, list.get(j));
        } else if (op == 3){ // 交换
            int t = RandomUtils.genRandom(0, codes.size() - 1);
            int v = codes.get(t);
            codes.set(t, codes.get(i));
            codes.set(i, v);
        } else if (codes.size() > 1){ // 移动
            int v = list.get(i);
            codes.remove(i);
            int t = RandomUtils.genRandom(0, codes.size() - 1);
            codes.set(t, v);
        }
        return new Chromosome(new CodeEncoding(codes));
    }

    public void print() {
        System.out.print("fitness: " + fitness);
        System.out.print("\t[freCbScore: " + freCbScore);
        System.out.print(",\teditDis: " + editDis);
        System.out.print(",\tcsf: " + csf);
        System.out.print(",\tpf: " + pf);
        System.out.println(",\tvalidScore: " + validScore + "]");
    }

    @Override
    public int compareTo(Chromosome o) {
        return Double.compare(this.fitness, o.fitness);
    }


    @Override
    public String toString() {
        return "\nrealize.ga.Chromosome:" +
                "\ngenes:\n" + genes +
                "\nfitness:\n" + fitness;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chromosome that = (Chromosome) o;
        return Objects.equals(genes, that.genes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genes);
    }
}