import java.util.*;
import java.util.stream.Stream;

// import com.google.gson.GsonBuilder;

public class Q2 {
    // public static void print(Object o) {
    //     System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(o));
    // }

    public static boolean isBalanced(String html) {
        Tree root =  new Tree();

        Tree currNode = root;
        String currTag = "";
        String currText = "";
        boolean closing = false;
        List<Tree> nodes = new ArrayList<Tree>();

        for (char c:html.toCharArray()) {
            if (c == '<') {
                if (!currTag.isEmpty()) {
                    return false;
                }
                else {
                    currTag += c;
                }
            }
            else if (c == '>') {
                currTag += c;
                if (!closing) {
                    Tree child = new Tree();
                    child.parent = currNode;
                    currNode.put(currTag, child);
                    currNode = currNode.get(currTag);
                    nodes.add(currNode);
                }
                else {
                    currNode.closed = true;
                    currNode = currNode.parent;
                    closing = false;
                }

                currTag = "";
            }
            else {
                currTag += c;
                if (c == '/') {
                    closing = true;
                }
            }
        }

        boolean result = true;
        for (Tree node:nodes) {
            result = result && node.closed;
        }
        return result;
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        boolean result = isBalanced(in.nextLine());
        System.out.println(result);
    }
}

class Tree extends HashMap<String, Tree> {
    public Tree parent;
    public boolean closed = false;
}
