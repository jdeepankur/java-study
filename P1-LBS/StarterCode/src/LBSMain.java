import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import Model.Tree;

import java.io.File;
import java.io.FileNotFoundException;

// Starter by Ian Gent, Oct 2022
//
// // This class is provided to save you writing some of the basic parts of the code
// // Also to provide a uniform command line structure
//
// // You may freely edit this code if you wish, e.g. adding methods to it. 
// // Obviously we are aware the starting point is provided so there is no need to explicitly credit us
// // Please clearly mark any new code that you have added/changed to make finding new bits easier for us
//
//
// // Edit history:
// // V1 released 3 Oct 2022
//
//


public class LBSMain {
	  final static boolean $ = false; 

      public static void printUsage() { 
          System.out.println("Input not recognised.  Usage is:");
          System.out.println("java LBSmain GEN|CHECK|SOLVE|GRACECHECK|GRACESOLVE <arguments>"  ); 
          System.out.println("     GEN arguments are seed [numpiles=17] [numranks=13] [numsuits=4] ");
          System.out.println("                       all except seed may be omitted, defaults shown");
          System.out.println("     SOLVE/GRACESOLVE argument is file]");
          System.out.println("                     if file is or is - then stdin is used");
          System.out.println("     CHECK/GRACECHECK argument is file1 [file2]");
          System.out.println("                     if file1 - then stdin is used");
          System.out.println("                     if file2 is ommitted or is - then stdin is used");
          System.out.println("                     at least one of file1/file2 must be a filename and not stdin");
	}


      public static ArrayList<Integer> readIntArray(String filename) {
        // File opening sample code from
        // https://www.w3schools.com/java/java_files_read.asp
	ArrayList<Integer> result  ;
	Scanner reader;
        try {
			File file = new File(filename);
			reader = new Scanner(file);
			result=readIntArray(reader);
			reader.close();
			return result;
            }
        catch (FileNotFoundException e) {
			System.out.println("File not found");
			e.printStackTrace();
            }
	// drop through case
	return new ArrayList<Integer>(0);
	
        }
        

      public static ArrayList<Integer> readIntArray(Scanner reader) {
	  ArrayList<Integer> result = new ArrayList<Integer>(0);
          while( reader.hasNextInt()  ) {
              result.add(reader.nextInt());
          }
	  return result;
      }



	public static void main(String[] args) {

	Scanner stdInScanner = new Scanner(System.in);
	ArrayList<Integer> workingList;

        LBSLayout layout;

        int seed ;
        int ranks ;
        int suits ;
        int numpiles ;
       
        if(args.length < 1) { printUsage(); return; };


	switch (args[0].toUpperCase()) {
            //
            // Add additional commands if you wish for your own testing/evaluation
            //

		case "GEN":
			if(args.length < 2) { printUsage(); return; };
			seed = Integer.parseInt(args[1]);
			numpiles = (args.length < 3 ? 17 : Integer.parseInt(args[2])) ;
			ranks = (args.length < 4 ? 13 : Integer.parseInt(args[3])) ;
			suits = (args.length < 5 ? 4 : Integer.parseInt(args[4])) ;


			layout = new LBSLayout(ranks,suits);
			layout.randomise(seed,numpiles);
			layout.print();
			stdInScanner.close();
			return;
			
		case "SOLVE":
			if (args.length<2 || args[1].equals("-")) {
				layout = new LBSLayout(readIntArray(stdInScanner));
			}
			else { 
				layout = new LBSLayout(readIntArray(args[1]));
			}

			System.out.println(solve(layout, stdInScanner, false));
			return;

		case "GRACESOLVE":
		case "SOLVEGRACE":
			if (args.length<2 || args[1].equals("-")) {
				layout = new LBSLayout(readIntArray(stdInScanner));
			}
			else { 
				layout = new LBSLayout(readIntArray(args[1]));
			}
			
			System.out.println(solve(layout, stdInScanner, true));
			return;

		case "CHECK":
			if (args.length < 2 || 
			    ( args[1].equals("-") && args.length < 3) || 
			    ( args[1].equals("-") && args[2].equals("-"))
			   ) 
			{ printUsage(); return; };
			if (args[1].equals("-")) {
				layout = new LBSLayout(readIntArray(stdInScanner));
			}
			else { 
				layout = new LBSLayout(readIntArray(args[1]));
			}
			if (args.length < 3 || args[2].equals("-")) {
				workingList = readIntArray(stdInScanner);
			}
			else { 
				workingList = readIntArray(args[2]);
			}

            System.out.println(check(layout, workingList, stdInScanner, false));
			return;

		case "GRACECHECK":
		case "CHECKGRACE":
			if (args.length < 2 || 
			    ( args[1].equals("-") && args.length < 3) || 
			    ( args[1].equals("-") && args[2].equals("-"))
			   ) 
			   { printUsage(); return; };
			if (args[1].equals("-")) {
				layout = new LBSLayout(readIntArray(stdInScanner));
			}
			else { 
				layout = new LBSLayout(readIntArray(args[1]));
			}
			if (args.length < 3 || args[2].equals("-")) {
				workingList = readIntArray(stdInScanner);
			}
			else { 
				workingList = readIntArray(args[2]);
			}

            System.out.println(check(layout, workingList, stdInScanner, true));
			return;

		default : 
			printUsage(); 
			return;
		}

	
	}


	private static Object solve(LBSLayout layout, Scanner stdInScanner, boolean grace) {
		List<Integer> play = new ArrayList<Integer>();
			for (int i = 0; i < layout.numPiles(); i++){
				int c = layout.cardAt(i);
				if (c < 1 || c > (layout.numRanks()*layout.numSuits())) {
					//System.out.println("a");
					return false;
				}
				play.add(c);
			}

			if (play.size() == 0){
				return -1;
			}

			Tree moveSpace = new Tree(play.toArray(), layout.numRanks(), grace);
			return moveSpace.dFSearch();
	}


	private static boolean check(LBSLayout layout, ArrayList<Integer> workingList, Scanner stdInScanner, boolean grace) {
		boolean graced = false;
		
		//layout does not include suit or rank information
		if (layout.numRanks() < 0 || layout.numSuits() < 0 || layout.numPiles() < 0) {
			//System.out.println("i");
			return false;
		}

		//no solution provided
		else if (workingList.isEmpty()){
			//System.out.println("h");
			return false;
		}

		//number of moves does not correspond to the given count
		else if ((2*workingList.get(0) + 1) != workingList.size()){
			//System.out.println("g");
			return false;
		}
		
		//1-card set-up always solvable
		else if (layout.numPiles() == 1) {
			return true;
		}


		ArrayList<Integer> play = new ArrayList<Integer>();
		for (int i = 0; i < layout.numPiles(); i++){
			int c = layout.cardAt(i);
			if (c < 1 || c > (layout.numRanks()*layout.numSuits())) {
				//System.out.println("f");
				return false;
			}
			play.add(c);
		}

		if (play.size() == 0 || workingList.size() == 0){
			//System.out.println("e");
			return false;
		}

		//Verify solution
		for (int i = 1; i<1+2*workingList.get(0); i=i+2) {
			//check that card exists in play
			if (!play.contains(workingList.get(i))){
				return false;
			}
			/*for (Integer r:play){
				System.out.print(r + " ");
			}System.out.println(" ");
			System.out.println(workingList.get(i) + " " + workingList.get(i+1));*/
			
			//check that the move is legal
			int oldTop;
			try { 
				oldTop = play.get(workingList.get(i+1));
			}
			catch (IndexOutOfBoundsException e){
				return false;
			}
			int gap = play.indexOf(workingList.get(i)) - workingList.get(i+1);
			//if (i==7) System.out.println(gap);
			//case 0: distance between piles is not correct
			if ( gap != 3 && gap != 1){
				//case 0.1: grace is allowed
				if (grace?!graced?workingList.get(i+1)==0:$:$){
					graced = true;
					play.remove(play.indexOf(workingList.get(i)));
					play.set(0, workingList.get(i));
				}
				//case 0.2: grace is not given
				else{
					//System.out.println("c");
					return false;
				}	
			}
			//case 1: same value
			else if (oldTop % layout.numRanks() == workingList.get(i) % layout.numRanks()) {
				//System.out.println("same val");
				play.remove(play.indexOf(workingList.get(i)));
				play.set(workingList.get(i+1), workingList.get(i));
			}
			//case 2: same suit
			else if (Math.floor((oldTop-1) / layout.numRanks()) == Math.floor((workingList.get(i)-1) / layout.numRanks())) {
				//System.out.println("same suit");
				play.remove(play.indexOf(workingList.get(i)));
				play.set(workingList.get(i+1), workingList.get(i));
			}
			else{
				//case 4.1: grace is allowed
				if (grace?!graced?workingList.get(i+1)==0:$:$){
					graced = true;
					play.remove(play.indexOf(workingList.get(i)));
					play.set(0, workingList.get(i));
				}
				//case 4.2: grace is not given
				else{
					return false;
				}
			}
		}
					
		stdInScanner.close();
		return true;
	}
}
