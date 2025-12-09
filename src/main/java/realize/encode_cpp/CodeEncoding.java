package realize.encode_cpp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CodeEncoding {

    public List<Integer> hashs;
    public List<Integer> exps;

    public CodeEncoding(List<Integer> hashs) {
        this.hashs = hashs;
        exps = CodeHash.codesHashToExpsHash(hashs);
    }

    public List<String> decode() {
        return hashs.stream().map(x -> CodeHash.hashToCode.get(x)).toList();
    }

    @Override
    public String toString() {
        return "realize.encode_cpp.CodeEncoding:\n" +
                "codes:\n" + hashs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeEncoding that = (CodeEncoding) o;
        return Objects.equals(hashs, that.hashs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hashs);
    }
}
