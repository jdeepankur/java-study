import java.util.*;
import java.util.stream.Stream;

import com.google.gson.GsonBuilder;

public class Q2 {
    public static void print(Object o) {
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(o));
    }

    public static boolean isBalanced(String html) {
        Tree root =  new Tree();

        Tree currNode = root;
        String currTag = "";
        String currText = "";
        boolean closing = false;
        List<Tree> nodes = new ArrayList<Tree>();

        for (char c:html.toCharArray()) {
            if (c == '<') {
                if (!currText.isEmpty()) {
                    Tree child = new Tree();
                    child.parent = currNode;
                    currNode.put("\"" + currText + "\"", child);
                    child.closed = true;
                    nodes.add(child);
                    currText = "";
                    currTag += c;
                }
                else if (!currTag.isEmpty()) {
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
                    String tag = currTag.substring(currTag.indexOf('<') + 1, currTag.indexOf('>'));
                    currNode.put(tag, child);
                    currNode = currNode.get(tag);
                    nodes.add(currNode);
                }
                else {
                    currNode.closed = true;
                    currNode = currNode.parent;
                    closing = false;
                    String tag = currTag.substring(currTag.indexOf('/') + 1, currTag.indexOf('>'));
                    if (currNode == null || !currNode.containsKey(tag)) {
                        return false;
                    }
                    else {
                        print(currNode);
                    }
                }

                currTag = "";
            }
            else {
                if (!currTag.isEmpty()) {
                    currTag += c;
                    if (c == '/') {
                        closing = true;
                    }
                }
                else {
                    currText += c;
                }
            }
        }

        boolean result = true;
        for (Tree node:nodes) {
            result = result && node.closed;
        }
        return result && currTag.isEmpty();
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
