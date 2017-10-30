
/************************************************************************************
 * @file BpTreeMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

import static java.lang.Math.addExact;
import static java.lang.Math.ceil;
import static java.lang.System.out;

/************************************************************************************
 * The BpTreeMap class provides B+Tree maps.  B+Trees are used as multi-level index
 * structures that provide efficient access for both point queries and range queries.
 * All keys will be at the leaf level with leaf nodes linked by references.
 * Internal nodes will contain divider keys such that each divider key corresponds to
 * the largest key in its left subtree (largest left).  Keys in left subtree are "<=",
 * while keys in right subtree are ">".
 */

/**
 * @ startuml
 *
 */
public class BpTreeMap <K extends Comparable <K>, V>
       extends AbstractMap <K, V>
       implements Serializable, Cloneable, SortedMap <K, V>
{
    /** The debug flag
     */
    private static final boolean DEBUG = true;

    /** The maximum fanout (number of children) for a B+Tree node.
     *  May wish to increase for better performance for Program 3.
     */
    private static final int ORDER = 5;

    /** The maximum fanout (number of children) for a big B+Tree node.
     */
    private static final int BORDER = ORDER + 1;

    /** The ceiling of half the ORDER.
     */
    private static final int MID = (int) ceil (ORDER / 2.0);

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /********************************************************************************
     * This inner class defines nodes that are stored in the B+tree map.
     */
    private class Node
    {
        boolean   isLeaf;                             // whether the node is a leaf 
        int       nKeys;                              // number of active keys
        K []      key;                                // array of keys
        Object [] ref;                                // array of references/pointers

        /****************************************************************************
         * Construct a node.
         * @param p       the order of the node (max refs)
         * @param _isLeaf  whether the node is a leaf
         */
        @SuppressWarnings("unchecked")
        Node (int p, boolean _isLeaf)
        {
            isLeaf = _isLeaf;
            nKeys  = 0;
            key    = (K []) Array.newInstance (classK, p-1);
            if (isLeaf) {
                ref = new Object [p];
            } else {
                ref = (Node []) Array.newInstance (Node.class, p);
            } // if
        } // constructor

        /****************************************************************************
         * Copy keys and ref from node n to this node.
         * @param n     the node to copy from
         * @param from  where in n to start copying from
         * @param num   the number of keys/refs to copy
         */
        void copy (Node n, int from, int num)
        {
            nKeys = num;
            for (int i = 0; i < num; i++) { key[i] = n.key[from+i]; ref[i] = n.ref[from+i]; }
            ref[num] = n.ref[from+num];
        } // copy

        /****************************************************************************
         * Find the "<=" match position in this node.
         * @param k  the key to be matched.
         * @return  the position of match within node, where nKeys indicates no match
         */
        int find (K k)
        {
            for (int i  = 0; i < nKeys; i++) if (k.compareTo (key[i]) <= 0) return i;
            return nKeys;
        } // find
        
        /****************************************************************************
         * Overriding toString method to print the Node. Prints out the keys.
         */
        @Override
        public String toString () 
        {
            return Arrays.deepToString (key);
        } // toString

    } // Node inner class

    /** The root of the B+Tree
     */
    private Node root;

    /** The first (leftmost) leaf in the B+Tree
     */
    private final Node firstLeaf;

    /** A big node to hold all keys and references/pointers before splitting
     */
    private final Node bn;
    
    /** Flag indicating whether a split at the level below has occurred that needs to be handled
     */
    private boolean hasSplit = false;

    /** The counter for the number nodes accessed (for performance testing)
     */
    private int count = 0;

    /** The counter for the total number of keys in the B+Tree Map
     */
    private int keyCount = 0;

    /********************************************************************************
     * Construct an empty B+Tree map.
     * @param _classK  the class for keys (K)
     * @param _classV  the class for values (V)
     */
    public BpTreeMap (Class <K> _classK, Class <V> _classV)
    {
        classK    = _classK;
        classV    = _classV;
        root      = new Node (ORDER, true);
        firstLeaf = root;
        bn        = new Node (BORDER, true);
    } // constructor

    /********************************************************************************
     * Return null to use the natural order based on the key type.  This requires the
     * key type to implement Comparable.
     */
    public Comparator <? super K> comparator () 
    {
        return null;
    } // comparator

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet <> ();

        //TODO 1   I M P L E M E N T E D
        addEntry(root, enSet);
        return enSet;
    } // entrySet

    private void addEntry(Node n, Set <Map.Entry<K, V>> sets){
        if(n.isLeaf){
            for(int i = 0; i < n.nKeys; i++) {
                sets.add(new MyEntry <> (n.key[i], (V)n.ref[i]));
            }
        }else{
            for (int i = 0; i < n.nKeys + 1; i++) {
                addEntry((Node) n.ref[i] , sets);
            }
        }
    }

    private class MyEntry<K, V> implements Entry<K, V> {

        private K key;
        private V value;

        public MyEntry(K k, V v){
            key = k;
            value = v;
        }
        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V oldval = this.value;
            this.value = value;
            return oldval;
        }
    }

    /********************************************************************************
     * Given the key, look up the value in the B+Tree map.
     * @param key  the key used for look up
     * @return  the value associated with the key or null if not found
     */
    @SuppressWarnings("unchecked")
    public V get (Object key)
    {
        return find ((K) key, root);
    } // get

    /********************************************************************************
     * Put the key-value pair in the B+Tree map.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null, not the previous value for this key
     */
    public V put (K key, V value)
    {
        insert (key, value, root);
        return null;
    } // put

    /********************************************************************************
     * Return the first (smallest) key in the B+Tree map.
     * @return  the first key in the B+Tree map.
     */
    public K firstKey () 
    {
        return firstLeaf.key[0];
    } // firstKey

    /********************************************************************************
     * Return the last (largest) key in the B+Tree map.
     * @return  the last key in the B+Tree map.
     */
    public K lastKey () 
    {
        //TODO 2  I M P L E M E N T E D
        K lastKey = firstKey();
        Set set = entrySet();

        // Get an iterator
        Iterator i = set.iterator();

        // find largest key
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();
            if(lastKey.compareTo((K) me.getKey()) < 0)
                lastKey = (K) me.getKey();
        }
        return lastKey;
    } // lastKey

    /********************************************************************************
     * Return the portion of the B+Tree map where key < toKey.
     * @return  the submap with keys in the range [firstKey, toKey)
     */
    public SortedMap <K,V> headMap (K toKey)
    {
        //TODO 3  I M P L E M E N T E D
        BpTreeMap<K, V> headbpt = new BpTreeMap<>( classK, classV);
        Set set = entrySet();
        Iterator i = set.iterator();

        // find largest key
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry) i.next();
            if (toKey.compareTo((K) me.getKey()) > 0)
                headbpt.put((K) me.getKey(), (V) me.getValue());

        }

        return headbpt;
    } // headMap

    /********************************************************************************
     * Return the portion of the B+Tree map where fromKey <= key.
     * @return  the submap with keys in the range [fromKey, lastKey]
     */
    public SortedMap <K,V> tailMap (K fromKey)
    {
        //TODO 4  I M P L E M E N T E D
        BpTreeMap<K, V> tailbpt = new BpTreeMap<>( classK, classV);
        Set set = entrySet();
        Iterator i = set.iterator();

        // find largest key
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();
            if(fromKey.compareTo((K) me.getKey()) <= 0)
                tailbpt.put((K) me.getKey(),(V) me.getValue());
        }

        return tailbpt;
    } // tailMap

    /********************************************************************************
     * Return the portion of the B+Tree map whose keys are between fromKey and toKey,
     * i.e., fromKey <= key < toKey.
     * @return  the submap with keys in the range [fromKey, toKey)
     */
    public SortedMap <K,V> subMap (K fromKey, K toKey)
    {
        //TODO 5   I M P L E M E N T E D
        BpTreeMap<K, V> midbpt = new BpTreeMap<>( classK, classV);
        Set set = entrySet();
        Iterator i = set.iterator();

        // find largest key
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();
            if(fromKey.compareTo((K) me.getKey()) <= 0 && toKey.compareTo((K) me.getKey()) > 0)
                midbpt.put((K) me.getKey(),(V) me.getValue());
        }

        return midbpt;
    } // subMap

    /********************************************************************************
     * Return the size (number of keys) in the B+Tree.
     * @return  the size of the B+Tree
     */
    public int size ()
    {
        return keyCount;
    } // size

    /********************************************************************************
     * Print the B+Tree using a pre-order traversal and indenting each level.
     * @param n      the current node to print
     * @param level  the current level of the B+Tree
     */
    @SuppressWarnings("unchecked")
    private void print (Node n, int level)
    {
        if (n == root) out.println ("BpTreeMap");
        out.println ("-------------------------------------------");

        for (int j = 0; j < level; j++) out.print ("\t");
        out.print ("[ . ");
        for (int i = 0; i < n.nKeys; i++) out.print (n.key[i] + " . ");
        out.println ("]");
        if ( ! n.isLeaf) {
            for (int i = 0; i <= n.nKeys; i++) print ((Node) n.ref[i], level + 1);
        } // if

        if (n == root) out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Recursive helper function for finding a key in B+trees.
     * @param key  the key to find
     * @param n    the current node
     */
    @SuppressWarnings("unchecked")
    private V find (K key, Node n)
    {
        count++;
        int i = n.find (key);
        if (i < n.nKeys) {
            K k_i = n.key[i];
            if (n.isLeaf) return (key.compareTo (k_i) == 0) ? (V) n.ref[i] : null;
            else          return find (key, (Node) n.ref[i]);
        } else {
            return (n.isLeaf) ? null : find (key, (Node) n.ref[n.nKeys]);
        } // if
    } // find

    /********************************************************************************
     * Recursive helper function for inserting a key in B+trees.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @return  the newly allocated right sibling node of n 
     */
    @SuppressWarnings("unchecked")
    private Node insert (K key, V ref, Node n){

    boolean inserted = false;
        if (n.isLeaf) {                                  // handle leaf node
    if (n.nKeys < ORDER - 1) {//enough space to insert into this node
        for (int i = 0; i < n.nKeys; i++) {
            K k_i = n.key [i];
            if (key.compareTo (k_i) < 0) {//the position to insert
                wedgeL (key, ref, n, i);//wedge the leafnode n to add <key,ref>
                inserted = true;
                break;
            } else if (key.equals (k_i)) {
                out.println ("BpTreeMap.insert: attempt to insert duplicate key = "+key);
                inserted = true;
                break;
            } // if
        } // for
        if (! inserted) wedgeL (key, ref, n, n.nKeys);//'wedge' to the right
        return null; // handled, nothing to return.
    } else {//not enough space
        if (find(key,n) != null) {//check if duplicate key or not before split
            out.println ("BpTreeMap.insert: attempt to insert duplicate key = " + key);
            return null; // handled, nothing to return
        }
        Node sib = splitL (key, ref, n);//split leaf and insert to the correct place
        //  T O   B E   I M P L E M E N T E D
        if (n == root) {//n is both leaf and root, create new root
            Node newRoot = new Node(ORDER, false);
            newRoot.nKeys = 1;
            newRoot.key[0] = n.key[n.nKeys-1];
            newRoot.ref[0] = n;
            newRoot.ref[1] = sib;
            root = newRoot;
            return null;
        }
        Node toReturn = new Node(ORDER, false);
        toReturn.nKeys = 1;
        toReturn.ref[0] = n;
        toReturn.ref[1] = sib;
        toReturn.key[0] = n.key[n.nKeys-1];
        return toReturn;
        // IMPLIMENTATION DONE
    } // if-else to check enough space to insert k to a leaf node or not.

} else {                                         // handle internal node
    //  T O   B E   I M P L E M E N T E D
    //find the correct children to insert
    int i=0;
    for (; i<n.nKeys; i++) {
        K k_i = n.key[i];
        if (key.compareTo(k_i)<=0) break;
    }

    //recursively call insert
    Node gift = insert(key,ref,(Node)n.ref[i]);

    //if no gift received, just return null.
    if (gift==null) return null;//nothing to return up

    //a gift is received from the lower level, needs to handle it.
    K giftKey = gift.key[0];
    Node giftRef = (Node) gift.ref[1];

    //if enough space, insert into this node
    inserted = false;
    if (n.nKeys < ORDER - 1) {
        for (i=0; i < n.nKeys; i++) {
            K k_i = n.key[i];
            if (key.compareTo (k_i) < 0) {//the position to insert
                wedgeI (giftKey, giftRef, n, i);//wedge to add <key,ref>
                inserted = true;
                break;
            } // if
        } // for
        if (! inserted) wedgeI (giftKey, giftRef, n, n.nKeys);//'wedge' to the right
        return null; // no gift to return
    }

    //not enough space, split leaf and insert <K,V> to the correct place
    Node sib = splitI (giftKey, giftRef, n);
    //if n is the root, create new root
    if (n == root) {
        Node newRoot = new Node(1,false);
        newRoot.nKeys = 1;//new root has 1 key and 2 children: n and sib.
        newRoot.key[0] = n.key[n.nKeys];//the hidden key
        newRoot.ref[0] = n;
        newRoot.ref[1] = sib;
        root = newRoot;
        return null;
    }
    //n is not the root, create a gift to the upper level
    Node toReturn = new Node(1,false);
    toReturn.nKeys = 1;
    toReturn.key[0] = n.key[n.nKeys];//the hidden key
    toReturn.ref[0] = n;
    toReturn.ref[1] = sib;
    return toReturn;
    // IMPLIMENTATION DONE.
} // if
} // insert

    /********************************************************************************
     * Wedge the key-ref pair into leaf node n.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param i    the insertion position within node n
     */
    private void wedgeL (K key, V ref, Node n, int i){
        //Implemented: handle the nextLeaf pointer
        n.ref[n.nKeys+1] = n.ref[n.nKeys];
        //Implementation done.
        for (int j = n.nKeys; j > i; j--) {
            n.key [j] = n.key [j-1];
            n.ref [j] = n.ref [j-1];
        } // for
        n.key [i] = key;
        n.ref [i] = ref;
        n.nKeys++;
    } // wedgeL

    /********************************************************************************
     * Wedge the key-ref pair into internal node n.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param i    the insertion position within node n
     */
    private void wedgeI (K key, Node ref, Node n, int i) {
        //  T O   B E   I M P L E M E N T E D
        for (int j = n.nKeys; j > i; j--) {
            n.key [j] = n.key [j-1];
            n.ref [j+1] = n.ref [j];
        } // for
        n.key [i] = key;
        n.ref [i+1] = ref;
        n.nKeys++;
    } // wedgeI

    /********************************************************************************
     * Split leaf node n and return the newly created right sibling node rt.
     * Split first (MID keys for both node n and node rt), then add the new key and ref.
     * @param key  the new key to insert
     * @param ref  the new value/node to insert
     * @param n    the current node
     * @return  the right sibling node (may wish to provide more information)
     */
    private Node splitL (K key, V ref, Node n) {
        Node rt = new Node (ORDER, true);
        //  T O   B E   I M P L E M E N T E D
        n.nKeys = MID;//2, truncate n's keys
        rt.nKeys = MID;//2, assign rt MID keys
        //copy the second half of n to rf
        for (int i=0; i<MID; i++) {
            rt.ref[i]=n.ref[MID+i];
            rt.key[i]=n.key[MID+i];
        }
        //store the last ref of n (pointer to next leaf)
        Node nextInLine =(Node) n.ref[ORDER-1];
        //insert <K,V> to the correct place
        if (key.compareTo(n.key[n.nKeys-1])<=0) {//insert into n
            boolean inserted = false;
            for (int i = 0; i < n.nKeys; i++) {
                K k_i = n.key [i];
                if (key.compareTo (k_i) < 0) {//the position to insert
                    wedgeL (key, ref, n, i);//wedge the leafnode n to add <key,ref>
                    inserted = true;
                    break;
                } // if
            } // for
            if (! inserted) wedgeL (key, ref, n, n.nKeys);//'wedge' to the right
        } else {//insert into rt
            boolean inserted = false;
            for (int i = 0; i < rt.nKeys; i++) {
                K k_i = rt.key [i];
                if (key.compareTo (k_i) < 0) {//the position to insert
                    wedgeL (key, ref, rt, i);//wedge the leafnode n to add <key,ref>
                    inserted = true;
                    break;
                } // if
            } // for
            if (! inserted) wedgeL (key, ref, rt, rt.nKeys);//'wedge' to the right
        }
        //handle the tail pointer pointing to next leaf
        rt.ref[rt.nKeys]=nextInLine;//let rf points to the original next leaf
        n.ref[n.nKeys]=rt;//let n points to rf as n's next leaf
        // IMPLIMENTATION DONE
        return rt;
    } // splitL

    /********************************************************************************
     * Split internal node n and return the newly created right sibling node rt.
     * Split first (MID keys for node n and MID-1 for node rt), then add the new key and ref.
     * @param key  the new key to insert
     * @param ref  the new value/node to insert
     * @param n    the current node
     * @return  the right sibling node (may wish to provide more information)
     */
    private Node splitI (K key, Node ref, Node n){
        Node rt = new Node (ORDER, false);
        //  T O   B E   I M P L E M E N T E D
        K[] keys = (K[]) new Comparable[ORDER];//5
        Object[] refs = new Object[ORDER+1];//6
        //locate the correct place for key
        int i=0;
        for (;i<ORDER-1 && n.key[i].compareTo(key)<0; i++) {
            keys[i] = n.key[i];
            refs[i] = (Node) n.ref[i];
        }
        refs[i] = (Node) n.ref[i];
        keys[i] = key;
        refs[i+1] = ref;
        i++;
        for (; i<=n.nKeys; i++) {
            keys[i] = n.key[i-1];
            refs[i+1] = (Node) n.ref[i];
        }
        //insert key into n or rt, according to its location
        n.nKeys = MID;
        rt.nKeys = MID;
        for (i=0; i<=MID; i++) {
            n.key[i] = keys[i];
            n.ref[i] = refs[i];
        }
        for (int j=0; j<=MID; j++, i++) {
            if (j<MID) rt.key[j] = keys[i];
            rt.ref[j] = refs[i];
        }
        //MID key is hidden in n at position n.key[n.nKeys].
        return rt;
    } // splitI // insert

    /********************************************************************************
     * Make a new root, linking to left and right child node, separated by a divider key.
     * @param ref0  the reference to the left child node
     * @param key0  the divider key - largest left
     * @param ref1  the reference to the right child node
     * @return  the node for the new root
     */
    private Node makeRoot (Node ref0, K key0, Node ref1)
    {
        Node nr   = new Node (ORDER, false);                          // make a node to become the new root
        nr.nKeys  = 1;                                                
        nr.ref[0] = ref0;                                             // reference to left node
        nr.key[0] = key0;                                             // divider key - largest left
        nr.ref[1] = ref1;                                             // reference to right node
        return nr;
    } // makeRoot
    
    /********************************************************************************
     * Wedge the key-ref pair into node n.  Shift right to make room if needed.
     * @param key   the key to insert
     * @param ref   the value/node to insert
     * @param n     the current node
     * @param i     the insertion position within node n
     * @param left  whether to start from the left side of the key
     * @return  whether wedge succeeded (i.e., no duplicate)
     */
    private boolean wedge (K key, Object ref, Node n, int i, boolean left)
    {
        if (i < n.nKeys && key.compareTo(n.key[i]) == 0) {
             out.println ("BpTreeMap.insert: attempt to insert duplicate key = " + key);
             return false;
        } // if
        n.ref[n.nKeys + 1] = n.ref[n.nKeys];                          // preserving the last ref
        for (int j = n.nKeys; j > i; j--) {
            n.key[j] = n.key[j-1];                                    // make room: shift keys right
            if (left || j > i + 1) n.ref[j] = n.ref[j-1];             // make room: shift refs right
        } // for
        n.key[i] = key;                                               // place new key
        if (left) n.ref[i] = ref; else n.ref[i+1] = ref;              // place new ref
        n.nKeys++;                                                    // increment number of keys
        return true;
    } // wedge

    /********************************************************************************
     * Split node n and return the newly created right sibling node rt.  The bigger half
     * should go in the current node n, with the remaining going in rt.
     * @param key  the new key to insert
     * @param ref  the new value/node to insert
     * @param n    the current node
     * @return  the right sibling node, if allocated, else null
     */
    private Node split (K key, Object ref, Node n, boolean left)
    {
        bn.copy (n, 0, ORDER-1);                                          // copy n into big node                           
        if (wedge (key, ref, bn, bn.find (key), left)) {                  // if wedge (key, ref) into big node was successful
            n.copy (bn, 0, MID);                                          // copy back first half to node n
            Node rt = new Node (ORDER, n.isLeaf);                         // make a right sibling node (rt)
            rt.copy (bn, MID, ORDER-MID);                                 // copy second to node rt    
            return rt;                                                    // return right sibling
        } // if     
        return null;                                                      // no new node created as key is duplicate
    } // split

    /********************************************************************************
     * The main method used for testing.
     * @param args The command-line arguments (args[0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        int totalKeys    = 40;
        boolean RANDOMLY = false;

        BpTreeMap <Integer, Integer> bpt = new BpTreeMap <> (Integer.class, Integer.class);
        if (args.length == 1) totalKeys = Integer.valueOf (args[0]);
   
        if (RANDOMLY) {
            Random rng = new Random ();
            for (int i = 1; i <= totalKeys; i += 2) bpt.put (rng.nextInt (2 * totalKeys), i * i);
        } else {
            for (int i = 1; i <= totalKeys; i += 2) bpt.put (i, i * i);
        } // if

        bpt.print (bpt.root, 0);
        for (int i = 0; i <= totalKeys; i++) {
            out.println ("key = " + i + " value = " + bpt.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of nodes accessed = " + bpt.count / (double) totalKeys);

        Set set = bpt.entrySet();

        // Get an iterator
        Iterator i = set.iterator();

        // Display elements
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();
            System.out.print(me.getKey() + ": ");
            System.out.println(me.getValue());
        }
        System.out.println();
        System.out.println(bpt.firstKey());
        System.out.println(bpt.lastKey());

    } // main

} // BpTreeMap class

/**
 * @enduml
 *
 */