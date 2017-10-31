import java.util.*;

import static java.lang.System.out;

public class Tester {
    public static void main(String[] args) {
        long begin, end = 0;
        double duration;
        Table tempTable;

        //Begin testing!
        for (int i = 500; i < 15001; i *= 2) {
            System.out.println("Tuples: " + i);
            Table tables[] = TestTupleGenerator(i);

            int id = (int) tables[0].getTuple(i / 2)[0];
            int id2 = (int) tables[0].getTuple(3 * i / 4)[0];

            begin = System.nanoTime();
            for (Comparable[] tuple : tables[0].getTuple()) {
                if (tuple[0].compareTo(id) == 0) {
                    end = System.nanoTime();
                    break;
                }
            }
            duration = end - begin;
            duration /= 1000000.0;
            System.out.println("Point Select (TableScan) took " + duration + " milliseconds to complete.\n\n\n");

            //Test Point select
            begin = System.nanoTime();
            tempTable = tables[0].select(new KeyType(id));
            end = System.nanoTime();
            duration = end - begin;
            duration /= 1000000.0;
            System.out.print("Point select took " + duration + " milliseconds to complete.\n");

            System.out.print("\n");

            //Test NestedLoopJoin
            begin = System.nanoTime();
            Table temp = tables[0].join("id", "profId", tables[1]);
            end = System.nanoTime();
            duration = (end - begin) / 1000000.0;
            System.out.println("Join (Nested Loop Join) took " + duration + " milliseconds to complete.");

            //Test IndexJoin
            begin = System.nanoTime();
            temp = tables[0].i_join("id", "profId", tables[1]);
            end = System.nanoTime();
            duration = end - begin;
            duration /= 1000000.0;
            System.out.print("Index join took " + duration + " milliseconds to complete.\n");

            //Test Range Select
            begin = System.nanoTime();

//                if(tables[0].mType == Table.MapType.BPTREE_MAP)
//                    ((BpTreeMap<> bpmap )tables[0].index)
//                            .subMap(new KeyType(id), new KeyType(id2)).entrySet().forEach(e -> e.getValue());
//                else
            tempTable = tables[0].select(t -> t[0].compareTo(id) >= 0 && t[0].compareTo(id2) <= 0);

            end = System.nanoTime();
            duration = (end - begin);
            duration /= 1000000.0;

            System.out.print("Range select took " + duration + " milliseconds to complete.");
        }//for

        System.out.println("\n___________Round of Testing Complete___________\n\n");
    }



    /*************************************************************************************
     * The main method is the driver for TestGenerator.
     * @param args  the command-line arguments
     */
    public static Table[] TestTupleGenerator(int inc) {
        TupleGenerator test = new TupleGeneratorImpl();

        test.addRelSchema("Student",
                "id name address status",
                "Integer String String String",
                "id",
                null);

        test.addRelSchema("Professor",
                "id name deptId",
                "Integer String String",
                "id",
                null);

        test.addRelSchema("Course",
                "crsCode deptId crsName descr",
                "String String String String",
                "crsCode",
                null);

        test.addRelSchema("Teaching",
                "crsCode semester profId",
                "String String Integer",
                "crcCode semester",
                new String[][]{{"profId", "Professor", "id"},
                        {"crsCode", "Course", "crsCode"}});

        test.addRelSchema("Transcript",
                "studId crsCode semester grade",
                "Integer String String String",
                "studId crsCode semester",
                new String[][]{{"studId", "Student", "id"},
                        {"crsCode", "Course", "crsCode"},
                        {"crsCode semester", "Teaching", "crsCode semester"}});

        String[] tables = {"Student", "Professor", "Course", "Teaching", "Transcript"};

        int tups[] = new int[]{inc, inc, inc, inc, inc};

        Comparable[][][] resultTest = test.generate(tups);

        Table[] retTables = new Table[2];
        retTables[0] = new Table("Professor",
                "id name deptId",
                "Integer Integer Integer",
                "id");
        for (int i = 0; i < resultTest[1].length; i++) {
            retTables[0].insert(resultTest[1][i]);
        }
        retTables[1] = new Table("Teaching",
                "crsCode semester profId",
                "Integer Integer Integer",
                "crsCode semester");
        for (int i = 0; i < resultTest[1].length; i++) {
            retTables[1].insert(resultTest[3][i]);
        }
        return retTables;
    }
}
