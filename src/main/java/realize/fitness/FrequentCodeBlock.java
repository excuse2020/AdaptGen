package realize.fitness;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrequentCodeBlock {

    public List<List<Integer>> dataset;
    public int min_support;
    public List<List<Integer>> frequentCodeBlock;

    public FrequentCodeBlock(List<List<Integer>> dataset, int min_support) throws IOException {
        this.dataset = dataset;
        this.min_support = min_support;
        this.frequentCodeBlock = getFrequentCodeBlock();
    }

    public List<List<Integer>> getFrequentCodeBlock() throws IOException {
        List<List<Integer>> res = new ArrayList<>();
        List<List<Integer>> len_x_list = new ArrayList<>();
        len_x_list.add(new ArrayList<>());

        Map<List<Integer>, List<List<Integer>>> postfixs = new HashMap<>();

        while (true) {

            List<List<Integer>> postfix;
            List<List<Integer>> len_temp = new ArrayList<>();

            boolean flag = false;

            for (List<Integer> s : len_x_list) {
                Map<Integer, Integer> len_x = new HashMap<>();
                if (s.isEmpty()) {
                    postfix = new ArrayList<>(dataset);
                } else {
                    postfix = getPostfix(s.subList(s.size() - 1, s.size()), postfixs.get(s.subList(0, s.size() - 1)));
                }

                postfixs.put(s, postfix);

                for (List<Integer> t : postfix) {
                    for (Integer c : t) {
                        len_x.put(c, len_x.getOrDefault(c, 0) + 1);
                    }
                }
                List<Integer> filter = len_x.entrySet().stream().filter(t -> t.getValue() >= min_support).map(Map.Entry::getKey).toList();
                if (!filter.isEmpty()) flag = true;
                len_temp.addAll(filter.stream().map(x -> {List<Integer> t = new ArrayList<>(s); t.add(x); return t;}).toList());
            }
            len_x_list = len_temp;
            res.addAll(len_temp);
            if (!flag) break;
        }
        return res;
    }

    public List<List<Integer>> getPostfix(List<Integer> s, List<List<Integer>> list) throws IOException {
        List<List<Integer>> res = new ArrayList<>();
        for (List<Integer> t : list) {
            int i = getSubListIndex(t, s);
            if (i != t.size()) {
                res.add(t.subList(i, t.size()));
            }
        }
        return res;
    }

    public int getSubListIndex(List<Integer> s, List<Integer> subList) {
        int i = 0, j = 0;
        while (i < s.size() && j < subList.size()) {
            if (s.get(i).equals(subList.get(j))) {
                j++;
            }
            i++;
        }
        return i;
    }

    public double getFreCBRatio(List<Integer> codes) {
        if (frequentCodeBlock.isEmpty()) {
            return 1;
        }
        int cnt = 0;
        for (List<Integer> cb : frequentCodeBlock) {
            if (getSubListIndex(codes, cb) != codes.size()) {
                cnt++;
            }
        }
        return 1.0 * cnt / frequentCodeBlock.size();
    }

    public static void main(String[] args) throws IOException {

        List<Integer> list1 = List.of(1, 2, 3, 5, 4);
        List<Integer> list2 = List.of(1, 2, 5, 3, 6);
        List<Integer> list3 = List.of(1, 7, 2, 3, 8);
        List<Integer> list4 = List.of(1, 2, 9, 10, 3);
        List<Integer> list5 = List.of(1, 11, 12, 3);
        List<List<Integer>> t = List.of(list1, list2, list3, list4, list5);

        System.out.println(new FrequentCodeBlock(t, 4).getFrequentCodeBlock());
    }
}