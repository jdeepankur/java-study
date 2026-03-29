import java.util.*;
import com.google.gson.Gson;

class Q4 {
    public static void main(String[] args) {
        var in = new Scanner(System.in);
        var json = "";

        while (in.hasNextLine()) {
            json = json + in.nextLine();
        }

        LinkedList list = new Gson().fromJson(json, LinkedList.class);
        System.out.println(hasCycle(list));
    }

    private static boolean hasCycle(LinkedList list) {
        LinkedList express = list.copy();
        boolean end = false;

        while (!end) {
            list.next();
            express.next();
            end = !express.next();

            if (list.equals(express) && !end) {
                return true;
            }
        }

        return false;
    }
}

class LinkedList {
    private int head;
    private Node[] nodes;

    private class Node {
        int id;
        int val;
        int next;
    }

    public Node currentNode;
    public LinkedList(int head, LinkedList.Node[] nodes) {
        this.head = head;
        this.nodes = nodes;
    }

    private void init() {
        if (currentNode == null) {
            for (Node node:nodes) {
                if (node.id == head) {
                    currentNode = node;
                    break;
                }
            }
        }
    }

    public int currentVal() {
        init();
        return currentNode.val;
    }

    public boolean next() {
        init();
        if (currentNode.next == -1) {
            return false;
        }

        for (Node node:nodes) {
            if (node.id == currentNode.next) {
                currentNode = node;
                return true;
            }
        }

        return false;
    }

    public LinkedList copy() {
        return new LinkedList(head, nodes);
    }

    public boolean equals(LinkedList ll) {
        return currentNode == ll.currentNode;
    }
}