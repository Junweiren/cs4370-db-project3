
/************************************************************************************
 * @file LinHashMap.java
 *
 * @author  John Miller
 */



import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;


/************************************************************************************
 * This class provides hash maps that use the Linear Hashing algorithm.
 * A hash table is created that is an array of buckets.
 * The buckets in turn will have chains of other buckets depending the hash value of the key associated.
 */
public class LinHashMap <K, V>
        extends AbstractMap <K, V>
        implements Serializable, Cloneable, Map <K, V>
{
    /** The number of slots (for key-value pairs) per bucket.
     */
    private static final int SLOTS = 4;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /********************************************************************************
     * This inner class defines buckets that are stored in the hash table.
     */
    private class Bucket
    		implements Serializable {
        int    nKeys;
        K []   key;
        V []   value;
        Bucket next;

        @SuppressWarnings("unchecked")
        Bucket (Bucket n)
        {
            nKeys = 0;
            key   = (K []) Array.newInstance (classK, SLOTS);
            value = (V []) Array.newInstance (classV, SLOTS);
            next  = n;
        } // constructor
    } // Bucket inner class

    /** The list of buckets making up the hash table.
     */
    private final List <Bucket> hTable;

    /** The modulus for low resolution hashing
     */
    private int mod1;

    /** The modulus for high resolution hashing
     */
    private int mod2;

    /** Counter for the number buckets accessed (for performance testing).
     */
    private int count = 0;

    /** The index of the next bucket to split.
     */
    private int split = 0;

    /** To store the number of keys inserted in the hashmap
     */
    public static int keysCount = 0;



    /********************************************************************************
     * Construct a hash table that uses Linear Hashing.
     * @param _classK    the class for keys (K)
     * @param _classV    the class for keys (V)
     */
    public LinHashMap (Class <K> _classK, Class <V> _classV)    // , int initSize)
    {
        classK = _classK;
        classV = _classV;
        hTable = new ArrayList <> ();
        mod1   = 4;                        // initSize;
        mod2   = 2 * mod1;

        //initializing the hashmap with 4 buckets at the starting.
        hTable.add(new Bucket(null));
        hTable.add(new Bucket(null));
        hTable.add(new Bucket(null));
        hTable.add(new Bucket(null));

    } // constructor

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet <> ();
        Map tempMap = new HashMap<K,V>();
        for (Bucket presentBucket :
                hTable) {
                while(presentBucket != null) {
                   for (int i = 0; i < presentBucket.nKeys; i++) {
                    tempMap.put(presentBucket.key[i],presentBucket.value[i]);
                }
                presentBucket = presentBucket.next;
            }
        }
        enSet.addAll(tempMap.entrySet());
        return enSet;
    } // entrySet

    /********************************************************************************
     * Given the key, look up the value in the hash table.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    public V get (Object key)
    {
        int i = h (key); 
        if(i < split)      
            i = h2(key);   
        Bucket bucket = hTable.get(i);
        V value = null;
        if(bucket.nKeys == 0)
            return null;
        while(bucket != null) {
            for (int j = 0; j < bucket.nKeys; j++) {
                if(key.getClass() == Integer.class){
                    if (((K)key).equals((K)bucket.key[j])) {
                        value = bucket.value[j];        
                        return value;
                    }
                }
                else {
                    if (((KeyType) key).equals((KeyType) bucket.key[j])) {
                        value = bucket.value[j];        
                        return value;
                    }
                }
            }
            bucket = bucket.next;
        }
        return null;
    } // get

    /********************************************************************************
     * Put the key-value pair in the hash table.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     */
    public V put (K key, V value)
    {
        int i = h (key);
        out.println ("LinearHashMap.put: key = " + key + ", h() = " + i + ", value = " + value);
        if(i < split) // to check if a split has been made on the bucket
            i = h2(key);
        out.println ("LinearHashMap.put: key = " + key + ", h() = " + i + ", value = " + value); 
        if(!containsKey(key)) {  
            Bucket bucket = hTable.get(i);
            if (bucket.nKeys < SLOTS) {
                bucket.key[bucket.nKeys] = key;
                bucket.value[bucket.nKeys] = value;
                if(bucket.nKeys < 4)
                    bucket.nKeys++;
                keysCount++;
            } 
            else {
                while(bucket != null) {
                    if (bucket.next == null) {
                        Bucket chainBucket = new Bucket(null);
                        bucket.next = chainBucket;
                        bucket = bucket.next;	
                        if (bucket.nKeys < SLOTS) {
                            bucket.key[bucket.nKeys] = key;
                            bucket.value[bucket.nKeys] = value;
                            if (bucket.nKeys < 4)
                                bucket.nKeys++;
                            keysCount++;
                        }
                    } else {
                        bucket = bucket.next;
                        if (bucket.nKeys < SLOTS) {
                            bucket.key[bucket.nKeys] = key;
                            bucket.value[bucket.nKeys] = value;
                            if (bucket.nKeys < 4)
                                bucket.nKeys++;
                            keysCount++;
                        }
                    }
                    bucket = bucket.next;
                }
            }
            int sizeValue = size();
            double loadValue = ((float)keysCount/sizeValue);
            if (loadValue > 0.5){
                Bucket splitBucket = hTable.get(split);
                Bucket newBucket = new Bucket(null);
                hTable.add(newBucket); 
                Map<K, V> mapKeys = new HashMap<K, V>();
                while (splitBucket != null) {
                    for (int sb = 0; sb < splitBucket.nKeys; sb++) {
                        mapKeys.put(splitBucket.key[sb], splitBucket.value[sb]);
                    }
                    splitBucket = splitBucket.next;
                }
                splitBucket = null;
                hTable.set(split,new Bucket(null));
                split++;
                for (K keyValue :
                        mapKeys.keySet()) {
                    int index = h(keyValue);
                    if (index < split)
                        index = h2(keyValue);
                    Bucket buck = hTable.get(index);
                    if (buck.nKeys < SLOTS) {
                        buck.key[buck.nKeys] = keyValue;
                        buck.value[buck.nKeys] = mapKeys.get(keyValue);
                        if(buck.nKeys < 4)
                            buck.nKeys++;
                    } 
                    else {
                        while(buck != null) {
                            if (buck.next == null) {
                                Bucket chainLoadBucket = new Bucket(null);
                                buck.next = chainLoadBucket;
                                buck = buck.next;
                                if(buck.nKeys < SLOTS) {
                                    buck.key[buck.nKeys] = keyValue;
                                    buck.value[buck.nKeys] = mapKeys.get(keyValue);
                                    if (buck.nKeys < 4)
                                        buck.nKeys++;
                                }
                            } else {
                                buck = buck.next;
                                if (buck.nKeys < SLOTS) {
                                    buck.key[buck.nKeys] = keyValue;
                                    buck.value[buck.nKeys] = mapKeys.get(keyValue);
                                    if (buck.nKeys < 4)
                                        buck.nKeys++;
                                }
                            }
                            buck = buck.next;
                        }
                    }
                }
                if (split == mod1) { 
                    split = 0;
                    mod1 = mod2;
                    mod2 = mod2 * 2;
                }
            } 
            count++;
            return null;
        }
        return null;
    } // put

    /********************************************************************************
     * Return the size (SLOTS * number of home buckets) of the hash table.
     * @return  the size of the hash table
     */
    public int size ()
    {
        return SLOTS * (mod1 + split);
    } // size

    /********************************************************************************
     * Print the hash table.
     */
    private void print ()
    {
        out.println ("Hash Table (Linear Hashing)");
        out.println ("-------------------------------------------");
        int position = 0;
        System.out.println("The hashtable size is "+hTable.size());
        for (Bucket bucket :
                hTable) {
            while (bucket != null) {
                for(int a=0;a<bucket.nKeys; a++){
                    System.out.println("At hash index " +position +" the bucket values are " +bucket.key[a]+" , "+bucket.value[a]);
                }
                bucket = bucket.next;
            }
            position++; // for checking the index of hashmap
        }
        out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Hash the key using the low resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h (Object key)
    {
//            return key.hashCode () % mod1;
            return Math.abs(key.hashCode () % mod1);
    } // h
    /********************************************************************************
     * Hash the key using the high resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h2 (Object key)
    {
//            return key.hashCode () % mod2;
            return Math.abs(key.hashCode () % mod2);
    } // h2

    /********************************************************************************
     * The main method used for testing.
     * @param args the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        int totalKeys    = 30;
        boolean RANDOMLY = false;
        
        LinHashMap <Integer, Integer> ht = new LinHashMap <> (Integer.class, Integer.class);
        if (args.length == 1)
            totalKeys = Integer.valueOf (args [0]);
        if (RANDOMLY) {
            Random rng = new Random ();
            for (int i = 1; i <= totalKeys; i += 2) ht.put (rng.nextInt (2 * totalKeys), i * i);
        } else {
            for (int i = 1; i <= totalKeys; i += 2) ht.put (i, i * i);
        } // if

        ht.print ();
        for (int i = 0; i <= totalKeys; i++) {
            out.println ("key = " + i + " value = " + ht.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of buckets accessed = " + ht.count / (double) totalKeys);
    } // main
} // LinHashMap class
