package realize.ga;

import realize.encode.CodeEncoding;
import realize.encode.CodeHash;
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
    List<List<Integer>> dataset;

    public GA(int populationSize, double mutationRate) {
        this.populationSize = populationSize;
        this.random = new Random();
        this.population = new HashSet<>();
        this.mutationRate = mutationRate;
    }

    public void initPopulation(List<List<Integer>> dataset) {
        dataset.forEach(x -> {
            try {
                population.add(new Chromosome(new CodeEncoding(x)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        fitnessSum = population.stream().mapToDouble(x -> x.fitness).sum();
    }

    public void initPopulation2(List<List<Integer>> dataset) {

        int n = dataset.size();
        this.population = new HashSet<>();
        this.dataset = dataset;
        initPopulation(dataset);

        while (population.size() < populationSize) {
            List<Integer> t = new ArrayList<>();
            int size = dataset.get(RandomUtils.genRandom(0, n - 1)).size();
            for (int i = 0; i < size; i++) {
                int k = RandomUtils.genRandom(0, n - 1);
                if (i >= dataset.get(k).size()) continue;
                t.add(dataset.get(k).get(i));
            }
            try {
                population.add(new Chromosome(new CodeEncoding(t)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
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
            if (random.nextDouble() <= mutationRate
                    || population.contains(chromosome)
                    || chromosome.fitness < 0.7) {
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

        // 重复代码块的数量
        freCbScore = GAStart.f.getFreCBRatio(exps);

        // 编辑距离
        editDis = 0;
        for (List<Integer> x : GAStart.expHashList) {
            editDis += EditDistanceCalculator.calculateEditDistance(exps, x);
        }
        editDis /= GAStart.expHashList.size();

        // 重复率
        repetition = 0;
        for (List<Integer> x : GAStart.expHashList) {
            repetition += RepetitiveRate.cac(exps, x);
        }
        repetition /= GAStart.expHashList.size();

        // 覆盖率
        coverage = 0;
        for (List<Integer> x : GAStart.codeHashList) {
            coverage = Math.max(coverage, RepetitiveRate.cac2(genes.codes, x));
        }

        // 代码合法
        validScore = CodeValid.isCodeValid(CodeHash.hashsToCodes(genes.codes)) ? 1 : 0;

        // 控制语句相似度
        ControlStatementFeature c = new ControlStatementFeature(exps);
        csf = 0;
        if (validScore == 1) {
            for (List<Integer> x : GAStart.expHashList) {
                csf += c.calculateSimilarity(new ControlStatementFeature(x));
            }
            csf = csf / GAStart.expHashList.size();
        }

        // 程序相似度
        ProgramFeatures p = new ProgramFeatures(exps);
        pf = 0;
        for (List<Integer> x : GAStart.expHashList) {
            pf += p.calculateSimilarity(new ProgramFeatures(x));
        }
        pf = pf / GAStart.expHashList.size();

        // 加权
        fitness = freCbScore * GAStart.freWeight + editDis * GAStart.editDisWeight
                + validScore * GAStart.validScoreWeight + csf * GAStart.csfWeight
                + pf * GAStart.pfWeight + repetition * GAStart.repetitionWeight
                + coverage * GAStart.coverageWeight;
    }

    public List<Chromosome> crossover(Chromosome partner) throws IOException {

        int i = RandomUtils.genRandom(0, genes.codes.size() - 1);
        int j = RandomUtils.genRandom(0, partner.genes.codes.size() - 1);

        List<Integer> codes1 = new ArrayList<>(genes.codes);
        List<Integer> codes2 = new ArrayList<>(partner.genes.codes);

        List<Integer> new1 = codes1.subList(0, i);
        List<Integer> new2 = codes2.subList(0, j);

        new1.addAll(codes2.subList(j, codes2.size()));
        new2.addAll(codes1.subList(i, codes1.size()));

        return List.of(new Chromosome(new CodeEncoding(new1)), new Chromosome(new CodeEncoding(new2)));
    }

    public Chromosome mutate() throws IOException {
        List<Integer> codes = new ArrayList<>(genes.codes);
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