import java.util.*;
import java.util.stream.*;


class Question1 {
    static int[] toInts(String[] a){return Arrays.stream(a).mapToInt(Integer::parseInt).toArray();}

    public static int[] closestPair(int[] prices, int target) {
        if (prices.length == 2) {
            return prices;
        }

        else {
            Arrays.sort(prices);
            
            int a = 0;
            int b = prices.length - 1;

            while (a < b - 1) {
                int sum = prices[a] + prices[b];

                if (sum == target) {
                    break;
                }
                else if (sum < target) {
                    a++;
                }
                else {
                    b--;
                }
            }

            return new int[]{prices[a], prices[b]};
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