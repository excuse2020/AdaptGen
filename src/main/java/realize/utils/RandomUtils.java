package realize.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomUtils {

    public static int genRandom(int l, int r) {
        Random rand = new Random();
        return rand.nextInt(r - l + 1) + l;
    }

    public static void main(String[] args) {
        List<Integer> l = new ArrayList<>();
        System.out.println(l.get(1));

    }
}

