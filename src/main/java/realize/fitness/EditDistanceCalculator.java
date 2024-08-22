package realize.fitness;

import java.util.List;

public class EditDistanceCalculator {

    public static <T> double calculateEditDistance(List<T> list1, List<T> list2) {
        int[][] dp = new int[list1.size() + 1][list2.size() + 1];

        for (int i = 0; i <= list1.size(); i++) {
            for (int j = 0; j <= list2.size(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (list1.get(i - 1).equals(list2.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i][j - 1], dp[i - 1][j]));
                }
            }
        }
        return 1 - (2.0 * dp[list1.size()][list2.size()] / (list1.size() + list2.size()));
    }
}