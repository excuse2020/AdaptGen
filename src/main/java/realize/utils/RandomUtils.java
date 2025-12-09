package realize.utils;

import java.util.Random;

public class RandomUtils {

    public static int genRandom(int l, int r) {
        Random rand = new Random();
        return rand.nextInt(r - l + 1) + l;
    }
}

