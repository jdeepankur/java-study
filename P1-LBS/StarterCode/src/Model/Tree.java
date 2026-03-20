package Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tree implements Comparable{

    private Integer[] board;
    private List<Tree> kids = new ArrayList<Tree>(); 
    private int ranks;
    private String route = ""; //how to get here from the parent
    private boolean grace;

    private final long TIMELIMIT = 270; //feel free to adjust this value based on what is felt to be the appropriate search time 


    public Tree(Object[] array, int ranks, boolean grace) {
        board = new Integer[array.length];
        this.grace = grace;

        int i = 0;
        for (Object o:array){
            board[i] = (Integer) o;
            i++;
        }

        this.ranks = ranks;
    }

    //a metric that attempts to rank layouts based on how many future possibilities they have
    //0: no further moves are possible
    //n: the average card has n possible moves 
    private double entropy = -1; //memoization 
    private Double entropy() {
        double moves = 0;
        if (entropy >= 0){
            return entropy;
        }
        else {
        for (int i = board.length - 1; i > 0; i--){
            //case a1: same value
			if (board[i] % ranks == board[i-1] % ranks) {
			    moves++;
			}
			//case a2: same suit
			else if (Math.floor((board[i]-1) / ranks) == Math.floor((board[i-1]-1) / ranks)) {
				moves++;
			}
            //case b: a move 3 piles to the left is possible
            if (i-3 >= 0) {
                if (board[i] % ranks == board[i-3] % ranks) {
			        moves++;
			    }
			    else if (Math.floor((board[i]-1) / ranks) == Math.floor((board[i-3]-1) / ranks)) {
				    moves++;
			    }
            }
            //case c: a saving grace is possible 
             if (grace) moves++;
        }
        return moves/board.length;
        }
    }

    @Override
    public String toString(){
        String s = "";

        for (Integer i : board){
            s = s + i;
        }

        return s;
    }

    //expand the space of possible moves
    public void grow(){
        for (int i = board.length - 1; i > 0; i--){
            //case a1: same value
			if (board[i] % ranks == board[i-1] % ranks) {
                List<Integer> nextBoard = new ArrayList<Integer>();
                for (Integer e:board){
                    nextBoard.add(e);
                }
                nextBoard.set(i-1, board[i]);
                nextBoard.remove(i); 
                Tree thisMove = new Tree(nextBoard.toArray(), ranks, grace);
                thisMove.route = board[i] + " " + (i-1);
			    addChild(thisMove);
			}
			//case a2: same suit
			if (Math.floor((board[i]-1) / ranks) == Math.floor((board[i-1]-1) / ranks)) {
				List<Integer> nextBoard = new ArrayList<Integer>();
                for (Integer e:board){
                    nextBoard.add(e);
                }
                nextBoard.set(i-1, board[i]);
                nextBoard.remove(i); 
                Tree thisMove = new Tree(nextBoard.toArray(), ranks, grace);
                thisMove.route = board[i] + " " + (i-1);
			    addChild(thisMove);
			}
            //case b: a move 3 piles to the left is possible
            if (i-3 >= 0) {
                if (board[i] % ranks == board[i-3] % ranks) {
			        List<Integer> nextBoard = new ArrayList<Integer>();
                    for (Integer e:board){
                        nextBoard.add(e);
                    }
                    nextBoard.set(i-3, board[i]);
                    nextBoard.remove(i); 
                    Tree thisMove = new Tree(nextBoard.toArray(), ranks, grace);
                    thisMove.route = board[i] + " " + (i-3);
			        addChild(thisMove);
			    }
			    if (Math.floor((board[i]-1) / ranks) == Math.floor((board[i-3]-1) / ranks)) {
                    List<Integer> nextBoard = new ArrayList<Integer>();
                    for (Integer e:board){
                        nextBoard.add(e);
                    }
                    nextBoard.set(i-3, board[i]);
                    nextBoard.remove(i); 
                    Tree thisMove = new Tree(nextBoard.toArray(), ranks, grace);
                    thisMove.route = board[i] + " " + (i-3);
			        addChild(thisMove);
			    }
            }
            //case c: a saving grace is possible
            if (grace && i!=1 && i!=3) {
                List<Integer> nextBoard = new ArrayList<Integer>();
                    for (Integer e:board){
                        nextBoard.add(e);
                    }
                nextBoard.set(0, board[i]);
                nextBoard.remove(i); 
                Tree thisMove = new Tree(nextBoard.toArray(), ranks, false/*grace no longer possible after this move */);
                thisMove.route = board[i] + " " + 0;
			    addChild(thisMove);
            } 
        }
    }

    private void addChild(Tree t) {
        kids.add(t);
    }

    public Object dFSearch() {
        Search s = search(System.currentTimeMillis() / 1000L);
        return (s.attempt == status.SOLVED) ? (s.moves.split(" ").length)/2 + " " + s.moves : (s.attempt == status.TIMEOUT) ? -2 : -1;
    }

    static Map<Tree, Search> history = new HashMap<Tree, Search>(); //memoization
    private Search search(long start) {
        Search hunt = new Search();

        if (history.containsKey(this)) {
            return history.get(this);
        }
        else{
            history.put(this, hunt);
        }

        if (board.length == 1){
            hunt.attempt = status.SOLVED;
            return hunt;
        }
        else if ((System.currentTimeMillis() / 1000L) - start > TIMELIMIT){
            hunt.attempt = status.TIMEOUT;
            return hunt;
        }

        grow();

        Tree[] orderedKids = kids.toArray(new Tree[0]);
        Arrays.sort(orderedKids);
        
        for (Tree t:orderedKids){
            /*for (Integer i:t.board){
                System.out.print(i + " ");
            }System.out.println(" ");*/
            if (t.board.length == 1){
                hunt.moves = t.route;
                hunt.attempt = status.SOLVED;
                return hunt;
            }
            else if (t.entropy() == 0) {
                continue;
            }
            else {
                Search s = t.search(start);
                switch(s.attempt){
                    case UNSOLVED:
                        break;
                    case SOLVED:
                        /*for (Integer i:board){
                            System.out.print(i + " ");
                        }
                        System.out.println();*/
                        s.moves = t.route + " " + s.moves;
                        return s;
                    case TIMEOUT:
                        return s;
                }
            }
        }

        return hunt;
    }

    @Override
    public int compareTo(Object o) {
        Tree other = (Tree) o;
        //invert the comparison
        //we want the sort to put higher entropy layouts first
        //every legal move decreases number of piles by 1
        //so a future layout with higher entropy is more likely to eventually lead to a solved state (i.e. single pile)
        return other.entropy().compareTo(entropy());
    }

    @Override
    public boolean equals(Object o){
        Tree other = (Tree) o;
        return (other.board.equals(board)) && (other.grace==grace);
    }
}
