import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Q3 {
    public static int binarySearch(int[] arr, int target) {
        int i, a, b;
        for (i = arr.length / 2, a = 0, b = arr.length; arr[i] != target && b > a; i = a + (b - a) / 2) {
            if (target > arr[i]) {
                if (a == i) {
                    return -1;
                }
                a = i;
            }
            else {
                if (b == i) {
                    return -1;
                }
                b = i;
            }
        }

        if (arr[i] == target) {
            return i;
        }
        else {
            return -1;
        } 
    }

    public static void main(String[] args) {
        var in = new Scanner(System.in);
        List<Integer> vals = new ArrayList<Integer>();
        do {
            vals.add(in.nextInt());
        } while (in.hasNextLine());

        int[] arr = vals.subList(0, vals.size() - 1).stream().mapToInt(x->x).toArray();
        int target = vals.getLast();

        System.out.println(binarySearch(arr, target));
    }
    
}
