package realize.encode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CodeEncoding {

    public List<Integer> codes;
    public List<Integer> exps;

    public CodeEncoding(List<Integer> codes) {
        this.codes = codes;
        exps = CodeHash.codesHashToExpsHash(codes);
    }

    public List<String> decode() {
        return codes.stream().map(x -> CodeHash.hashToCode.get(x)).toList();
    }

    @Override
    public String toString() {
        return "realize.encode.CodeEncoding:\n" +
                "codes:\n" + codes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeEncoding that = (CodeEncoding) o;
        return Objects.equals(codes, that.codes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codes);
    }

    public static void main(String[] args) {
        List<Integer> l1 = List.of(1, 2, 3);
        List<Integer> l2 = new ArrayList<>();
        l2.add(1);
        l2.add(2);
        l2.add(3);
        System.out.println(Objects.equals(l1, l2));
    }
}
