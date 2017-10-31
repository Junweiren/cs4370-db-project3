 
/*****************************************************************************************
 * @file  TestPointQuery_BpTree.java
 *
 * @author  XXX
 */

import static java.lang.System.out;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;


public class TestPointQuery_BpTree
{
    /*************************************************************************************
     * The main method is the driver for TestGenerator.
     * @param args  the command-line arguments
     */
    public static void main (String [] args)
    {
	out.println("-----------------------------------------------------------------------");
	out.println("Testing point select by B+ Tree");
	
	for (int numPt = 1; numPt < 9; ++numPt) {
	    int numTupStd = numPt * 10000;             
	    
	    TupleGenerator test = new TupleGeneratorImpl ();

	    test.addRelSchema ("Student",
			       "id name address status",
			       "Integer String String String",
			       "id",
			       null);

	    String [] tables = { "Student" };
	    int tups [] = new int [] { numTupStd };
    
	    Comparable [][][] resultTest = test.generate (tups);

	
	    /**********************************************************************************
	     */	

	    Table student = new Table("student", "id name address status",
				      "Integer String String String", "id");


	    for (int i = 0; i < resultTest[0].length; ++i) 
		student.insert(resultTest[0][i]);
	
	    int numSam = 0;                             
	    int numSel = 100000;                        

	    out.println("-----------------------------------------------------------------------");
	    out.println("Point " + numPt + " : " + numTupStd + " tuples");
	    out.println("Each sample has " + numSel + " point select");
	    

	    while (numSam < 15) {
		Random rand = new Random();             


		for (int i = 0; i < 10; ++i) {
		    int tupleIndex = rand.nextInt(numTupStd);   
		    

		    Table selectTable = student.select(new KeyType(resultTest[0][tupleIndex][0]));
		} 


		long startTime = System.currentTimeMillis();    
		for (int i = 0; i < numSel; ++i) {
		    int tupleIndex = rand.nextInt(numTupStd);   
		    

		    Table selectTable = student.select(new KeyType(resultTest[0][tupleIndex][0]));  
		} 
		long endTime = System.currentTimeMillis();      
		
		numSam++;
		long runTime = endTime - startTime;             
		out.println("Sample " + numSam + " " + " takes " +
			    runTime + " ms");

	    } 

	} 
	
    } 

} 

