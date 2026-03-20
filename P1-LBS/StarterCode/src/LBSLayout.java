import java.util.ArrayList;
import java.util.Random ;
import java.util.Iterator ;

// Starter by Ian Gent, Oct 2022
// 
// // This class is provided to save you writing some of the basic parts of the code
// // Also to provide a uniform layout generator
//
// // You may freely edit this code if you wish, e.g. adding methods to it. 
// // Obviously we are aware the starting point is provided so there is no need to explicitly credit us
// // Please clearly mark any new code that you have added/changed to make finding new bits easier for us
//
// V1 3 Oct 2022

public class LBSLayout {

    protected int numranks;
    protected int numsuits;
    protected int numpiles;

    protected ArrayList<Integer> layout;

    public int numRanks() { 
        return numranks;
    }
    public int numSuits() { 
        return numsuits;
    }
    public int numPiles() { 
        return layout.size();
    }
    public int cardsInDeck() { 
        return numranks * numsuits; 
    }

    public int cardAt(int pile) { 
        if(pile >= numPiles()) { 
            return -1; }
        else {
            return layout.get(pile);
        }
    }

    private ArrayList<Integer> copyLayout() {
         ArrayList<Integer> copy = new  ArrayList<Integer>(0);
         for(int i=0;i<numPiles();i++) { 
             copy.add(cardAt(i));
         }
         return copy; 
    }


    public void randomise (int seed, int numInLayout)   { 
        int maxindex = cardsInDeck();

        int[] cards = new int[maxindex] ;
        for (int i = 0; i < maxindex; i++) {
          cards[i] = i+1 ;
        }
        // Completely shuffle the deck 
        //
        // Choose the sequence uniformly at random
        // NB random.nextInt(k) gives a value in range 0..k-1

        Random random = new Random(seed) ;

        for (int i = 0; i < maxindex-1; i++) {
          int temp = cards[i] ;
          int index = i+random.nextInt(maxindex-i) ;
          cards[i] = cards[index] ;
          cards[index] = temp ;
        }

        // Now empty the piles
        //
        layout.clear();

        // Now put the right number of cards on the layout
        // For safety don't allow more cards than are available 

        int remaining=Math.min(numInLayout,maxindex);

        int nextpile = 0; 

        for (int i = 0; i < remaining ; i++) { 
            layout.add(cards[i]);
        }

    }

	public void print() {

		System.out.println(numRanks() + " " + numSuits() + " " + numPiles());
        for (int i=0; i < numPiles() ; i++) { 
                System.out.print(layout.get(i)+" ");
            }
        System.out.println();
	}


    


// Helper function for constructors 
//


    private void createLayout() { 
        this.layout = new ArrayList<Integer>(0);
    }

    //
    //// Variety of Constructors
    //
	

    // create empty problem for standard deck 
    
    public LBSLayout() {
        this(13,4) ; 
    }

    // create empty problem with given parameters 
    //
   public LBSLayout(Integer ranks, Integer suits) {
        this.numranks = ranks;
        this.numsuits = suits;
        createLayout();
	}



    public LBSLayout(ArrayList<Integer> integers) { 
		Iterator<Integer> reader = integers.iterator();
        int r = (reader.hasNext() ? reader.next() : 0);
        int s = (reader.hasNext() ? reader.next() : 0);
        int p = (reader.hasNext() ? reader.next() : 0);
        this.numranks = r;
        this.numsuits = s;
        this.numpiles = p;
        createLayout();
        int nextpile = 0;
        while( reader.hasNext() && (nextpile < p) ) { 
            int card = reader.next();
            layout.add(card);
        }
    }

    // Copy Constructor
    //
    
    public LBSLayout(LBSLayout old) { 
        this.numranks = old.numRanks();
        this.numsuits = old.numSuits();
        this.numpiles = old.numPiles();
        this.layout = old.copyLayout();
    }

}




