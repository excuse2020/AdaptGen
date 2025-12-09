package realize.fitness;

import java.util.*;

public class FrequentCodeBlock {

    private List<List<Integer>> sequences; 
    private int minSupport; 
    private int maxStep; 
    private List<List<Integer>> frequentPatterns = new ArrayList<>(); 

    public FrequentCodeBlock(List<List<Integer>> sequences, int minSupport, int maxStep) {
        this.sequences = sequences;
        this.minSupport = minSupport;
        this.maxStep = maxStep;
    }

    public void mine() {
        Map<Integer, List<Integer>> initialProjection = buildInitialProjection(sequences);
        prefixSpan(new ArrayList<>(), initialProjection);
    }

    private Map<Integer, List<Integer>> buildInitialProjection(List<List<Integer>> sequences) {
        Map<Integer, List<Integer>> projection = new HashMap<>();
        for (int seqIndex = 0; seqIndex < sequences.size(); seqIndex++) {
            List<Integer> sequence = sequences.get(seqIndex);
            for (int i = 0; i < sequence.size(); i++) {
                int item = sequence.get(i);
                projection.computeIfAbsent(item, k -> new ArrayList<>()).add(seqIndex * 1000 + i);
            }
        }
        return projection;
    }

    private void prefixSpan(List<Integer> prefix, Map<Integer, List<Integer>> projection) {
        for (Map.Entry<Integer, List<Integer>> entry : projection.entrySet()) {
            int item = entry.getKey();
            List<Integer> projectedIndexes = entry.getValue();

            if (projectedIndexes.size() < minSupport) continue;

            List<Integer> newPattern = new ArrayList<>(prefix);
            newPattern.add(item);
            frequentPatterns.add(newPattern);

            Map<Integer, List<Integer>> newProjection = new HashMap<>();
            for (int index : projectedIndexes) {
                int seqIndex = index / 1000;
                int pos = index % 1000;
                List<Integer> sequence = sequences.get(seqIndex);

                for (int i = pos + 1; i < sequence.size() && i <= pos + maxStep; i++) {
                    int nextItem = sequence.get(i);
                    newProjection.computeIfAbsent(nextItem, k -> new ArrayList<>())
                            .add(seqIndex * 1000 + i);
                }
            }

            prefixSpan(newPattern, newProjection);
        }
    }

    public double getFreCBRatio(List<Integer> code) {
        if (frequentPatterns.isEmpty()) {
            return 1;
        }
        if (code == null || code.isEmpty()) return 0;
        int cnt = 0;
        for (List<Integer> cb : frequentPatterns) {
            int index = getSubListIndex(code, cb);
            if (index >= 0 && index < code.size()) {  
                cnt++;
            }
        }
        return 1.0 * cnt / frequentPatterns.size();
    }

    public int getSubListIndex(List<Integer> s, List<Integer> subList) {
        if (s == null || subList == null) {
            return -1;  
        }
        if (subList.isEmpty()) {
            return 0;  
        }
        if (s.isEmpty()) {
            return -1;  
        }

        for (int start = 0; start < s.size(); start++) {
            int i = start;
            int j = 0;
            while (i < s.size() && j < subList.size()) {
                if (s.get(i).equals(subList.get(j))) {
                    j++;
                }
                i++;
            }
            if (j == subList.size()) {  
                return start;  
            }
        }
        return s.size();  
    }

    // 获取结果
    public List<List<Integer>> getFrequentPatterns() {
        return frequentPatterns;
    }
}