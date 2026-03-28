import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class StringGenerator {

    private static final String ALPHABET =
            "abcdefghijklmnopqrstuvwxyz" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "0123456789" +
            " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    private static final int MAX_LENGTH = 20;

    public static List<String> generateValid(Function<String, Boolean> validator, int n) {
        List<String> results = new ArrayList<>();
        dfs("", validator, results, n);
        return results;
    }

    private static void dfs(String current, Function<String, Boolean> validator,
                             List<String> results, int n) {
        if (results.size() == n) return;

        if (!hasValidExtension(current, validator)) return;

        if (!current.isEmpty() && validator.apply(current)) {
            results.add(current);
        }

        if (current.length() >= MAX_LENGTH) return;

        for (char c : ALPHABET.toCharArray()) {
            if (results.size() == n) return;
            dfs(current + c, validator, results, n);
        }
    }

    private static boolean hasValidExtension(String prefix, Function<String, Boolean> validator) {
        if (prefix.isEmpty()) return true;

        if (validator.apply(prefix)) return true;

        for (int extraLen = 1; extraLen <= MAX_LENGTH - prefix.length(); extraLen++) {
            if (probeExtensions(prefix, extraLen, validator)) return true;
        }

        return false;
    }

    private static boolean probeExtensions(String prefix, int extraLen,
                                            Function<String, Boolean> validator) {
        long total = pow(ALPHABET.length(), extraLen);
        for (long i = 0; i < total; i++) {
            if (validator.apply(prefix + indexToSuffix(i, extraLen))) return true;
        }
        return false;
    }

    private static String indexToSuffix(long index, int length) {
        int base = ALPHABET.length();
        char[] chars = new char[length];
        for (int i = length - 1; i >= 0; i--) {
            chars[i] = ALPHABET.charAt((int)(index % base));
            index /= base;
        }
        return new String(chars);
    }

    private static long pow(long base, long exp) {
        long result = 1;
        for (int i = 0; i < exp; i++) result *= base;
        return result;
    }

    // --- Example usage ---
    public static void main(String[] args) {

        // Strings that are exactly 3 chars and palindromes
        List<String> palindromes = generateValid(
            s -> s.length() == 3 && s.equals(new StringBuilder(s).reverse().toString()),
            5
        );
        System.out.println("Palindromes: " + palindromes);
        // ["aaa", "bbb", "ccc", "ddd", "eee"]

        // Strings that look like simple arithmetic expressions, e.g. "1+1"
        List<String> expressions = generateValid(
            s -> s.matches("\\d+[+\\-*/]\\d+"),
            5
        );
        System.out.println("Expressions: " + expressions);
        // ["0+0", "0+1", "0+2", "0+3", "0+4"]

        // With a method reference
        List<String> results = generateValid(StringGenerator::isEvenNumeric, 5);
        System.out.println("Even numerics: " + results);
        // ["00", "02", "04", "06", "08"]
    }

    public static boolean isEvenNumeric(String s) {
        if (!s.matches("\\d+")) return false;
        try {
            return Integer.parseInt(s) % 2 == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}