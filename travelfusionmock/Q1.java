import java.util.*;
import java.util.stream.*;


class Question1 {
    static int[] toInts(String[] a){return Arrays.stream(a).mapToInt(Integer::parseInt).toArray();}

    public static int[] closestPair(int[] prices, int target) {
        if (prices.length == 2) {
            return prices;
        }

        else {
            int[] cands = Arrays.copyOfRange(prices, 0, 2);
            for (int i = 2; i < prices.length; i++) {
                int newprice = prices[i];
                int gap = Math.abs(Arrays.stream(cands).sum() - target);
                if (gap == 0) {
                    return cands;
                }

                int[] _cands = Arrays.stream(cands).map(x -> Math.abs(x + newprice - target)).toArray();
                int min = IntStream.range(0, _cands.length)
                    .reduce((a, b) -> _cands[a] <= _cands[b] ? a : b)
                    .getAsInt();

                if (_cands[min] < gap) {
                    cands = new int[]{cands[min], newprice};

                }

            }

            return cands;
        }

    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        int[] prices = toInts(in.nextLine().split(" "));
        int target = in.nextInt();

        int[] result = closestPair(prices, target);
        System.out.print(Arrays.stream(result).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
    }

}