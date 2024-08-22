package realize.utils;

import java.util.ArrayList;
import java.util.List;

public class CodeUtils {

    public static List<List<String>> cutting(List<String> code) {
        List<List<String>> res = new ArrayList<>();
        List<String> t = new ArrayList<>();
        for (String s : code) {
            t.add(s);
            if (s.contains("{") || s.contains("}")) {
                res.add(t);
                t = new ArrayList<>();
            }
        }
        return res;
    }
}
