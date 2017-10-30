import java.util.*;

public class Tester {
    public static void main(String[] args){
        long begin, end;
        double duration;
        Table tempTable;

        //Begin testing!
        for(int i = 500; i < 15001; i *= 2){
            System.out.println("Tuples: " + i);
            Table tables[] = TestTupleGenerator(i);

            int id = (int)tables[0].getTuple(i/2)[0];
            int id2 = (int)tables[0].getTuple(3*i/4)[0];

            begin = System.nanoTime();
            for(Comparable[] tuple : tables[0].tuples){
                if(tuple[0].compareTo(id) == 0){
                    end = System.nanoTime();
                    break;
                }
            }
            duration = end - begin;
            duration /= 1000000.0;
            System.out.println ("Point Select (TableScan) took " + duration + " milliseconds to complete.\n\n\n");

            for(int i = 0; i < 4; i++){
                begin = System.nanoTime();
                tempTable = tables[0].select(new KeyType(studentID), index);
                end = System.nanoTime();
                duration = end - begin;
                duration /= 1000000.0;
                System.out.print("Point Select for ");

                if(i == 0)
                    System.out.print("TreeMap ");
                else if (i == 1)
                    System.out.print("BPTreeMap ");
                else if (i == 2)
                    System.out.print("LinHashMap ");
                else
                    System.out.print("ExtHashMap ");

                System.out.print("took " + duration + " milliseconds to complete.\n");
            }

            System.out.print("\n\n\n\n");//Create space for formatting purposes

            //Test NestedLoopJoin
            begin = System.nanoTime();
            temp = tables[0].join("id", "studId", tables[1], 0);
            end = System.nanoTime();
            duration = (end - begin) / 1000000.0;
            System.out.println("Join (Nested Loop Join) took " + duration + " milliseconds to complete.");

            //Test IndexJoin
            for(int i = 0; i < 4; i++){
                begin = System.nanoTime();
                temp = tables[0].indexJoin(tables[1], i);
                end = System.nanoTime();
                duration = end - begin;
                duration /= 1000000.0;
                System.out.print("Index Join for ");

                if(i == 0)
                    System.out.print("TreeMap ");
                else if (i == 1)
                    System.out.print("BPTreeMap ");
                else if (i == 2)
                    System.out.print("LinHashMap ");
                else
                    System.out.print("ExtHashMap ");

                System.out.print("took " + duration + " milliseconds to complete.\n");
            }//for

            for(int i = 0; i < 3; i++){
                begin = System.nanoTime();

                if(i == 1)
                    tables[0].bpIndex.subMap(new KeyType(id), new KeyType(id2)).entrySet().forEach(e -> e.getValue());
                else
                    tempTable = tables[0].select(t -> t[0].compareTo(id) >= 0 && t[0].compareTo(id2) <= 0, i);

                end = System.nanoTime();
                duration = (end - begin);
                duration /= 1000000.0;
                System.out.print("Range Select for ");
                if(i == 0)
                    System.out.print("TreeMap ");
                else if (i == 1)
                    System.out.print("BPTreeMap ");
                else if (i == 2)
                    System.out.print("LinHashMap ");

                System.out.print(" took " + duration + " milliseconds to complete.");
            }//for

         System.out.println("\n___________Round of Testing Complete___________\n\n");
        }
    }

        /*************************************************************************************
         * The main method is the driver for TestGenerator.
         * @param args  the command-line arguments
         */
        public static Table [] TestTupleGenerator(String [] args)
        {
            TupleGenerator test = new TupleGeneratorImpl ();

            test.addRelSchema ("Student",
                    "id name address status",
                    "Integer String String String",
                    "id",
                    null);

            test.addRelSchema ("Professor",
                    "id name deptId",
                    "Integer String String",
                    "id",
                    null);

            test.addRelSchema ("Course",
                    "crsCode deptId crsName descr",
                    "String String String String",
                    "crsCode",
                    null);

            test.addRelSchema ("Teaching",
                    "crsCode semester profId",
                    "String String Integer",
                    "crcCode semester",
                    new String [][] {{ "profId", "Professor", "id" },
                            { "crsCode", "Course", "crsCode" }});

            test.addRelSchema ("Transcript",
                    "studId crsCode semester grade",
                    "Integer String String String",
                    "studId crsCode semester",
                    new String [][] {{ "studId", "Student", "id"},
                            { "crsCode", "Course", "crsCode" },
                            { "crsCode semester", "Teaching", "crsCode semester" }});

            String [] tables = { "Student", "Professor", "Course", "Teaching", "Transcript" };

            int tups [] = new int [] { 10000, 1000, 2000, 50000, 5000 };

            Comparable [][][] resultTest = test.generate (tups);

            for (int i = 0; i < resultTest.length; i++) {
                out.println (tables [i]);
                for (int j = 0; j < resultTest [i].length; j++) {
                    for (int k = 0; k < resultTest [i][j].length; k++) {
                        out.print (resultTest [i][j][k] + ",");
                    } // for
                    out.println ();
                } // for
                out.println ();
            } // for
        }
}
