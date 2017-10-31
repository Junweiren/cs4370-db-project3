
/************************************************************************************
 * @file BpTreeMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;

/************************************************************************************
 * This class provides B+Tree maps.  B+Trees are used as multi-level index structures
 * that provide efficient access for both point queries and range queries.
 */
public class BpTreeMap <K extends Comparable <K>, V>
        extends AbstractMap <K, V>
        implements Serializable, Cloneable, SortedMap <K, V>
{
    /** The maximum fanout for a B+Tree node.
     */
    private static final int ORDER = 5;

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
        boolean   isLeaf;
        int       nKeys;
        K []      key;
        Object [] ref;
        @SuppressWarnings("unchecked")
        Node (boolean _isLeaf)
        {
            isLeaf = _isLeaf;
            nKeys  = 0;
            key    = (K []) Array.newInstance (classK, ORDER);
            if (isLeaf) {
                //ref = (V []) Array.newInstance (classV, ORDER);
                ref = new Object [ORDER + 1];
            } else {
                ref = (Node []) Array.newInstance (Node.class, ORDER + 1);
            } // if
        } // constructor
    } // Node inner class

    /** The root of the B+Tree
     */
    private Node root;

    /** The counter for the number nodes accessed (for performance testing).
     */
    private int count = 0;

    private int treeSize = 0;

    /********************************************************************************
     * Construct an empty B+Tree map.
     * @param _classK  the class for keys (K)
     * @param _classV  the class for values (V)
     */
    public BpTreeMap (Class <K> _classK, Class <V> _classV)
    {
        classK = _classK;
        classV = _classV;
        root   = new Node (true);
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
        return subSet(root);
    } // entrySet

    /**
     * Creates a Set of keys and values from the leaves of the tree
     * @param n
     * @return the set of values of tree rooted at n
     */
    public Set <Map.Entry <K, V>> subSet (Node n)
    {
        if (!n.isLeaf)
        {
            return subSet((Node) n.ref[0]);
        }
        else
        {
            Node current = n;
            Set <Map.Entry <K, V>> returnSet = new HashSet <> ();
            while (current != null)
            {
                for(int i = 0;i < n.nKeys;i ++)
                {
                    returnSet.add(new AbstractMap.SimpleEntry(current.key[i], current.ref[i]));
                }
                current = (Node) current.ref[ORDER];
            }
            return returnSet;
        }
    }
    /********************************************************************************
     * Given the key, look up the value in the B+Tree map.
     * @param key  the key used for look up
     * @return  the value associated with the key
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
     * @return  null (not the previous value)
     */
    public V put (K key, V value)
    {
        insert (key, value, root, null);
        treeSize++;
        return null;
    } // put

    /********************************************************************************
     * Return the first (smallest) key in the B+Tree map.
     * @return  the first key in the B+Tree map.
     */


    public K firstKey ()
    {

        Node n = root;
        K smallestKey = n.key[0];

        //if node is a leaf return the smallest key(should be the first)
        if(n.isLeaf){
            return smallestKey;
        }

        //if node is not a leaf then set n to first node referenced by the current node
        while(!n.isLeaf){
            n = (Node)n.ref[0];
            smallestKey = n.key[0];
        }

        return smallestKey;
    } // firstKey

    /********************************************************************************
     * Return the last (largest) key in the B+Tree map.
     * @return  the last key in the B+Tree map.
     */
    public K lastKey ()
    {
        Node n = root;
        K largestKey = n.key[n.key.length - 2];

        //if node is a leaf then loop through keys in reverse looking
        //for first non-null value
        if(n.isLeaf){
            for(int i = n.key.length - 2; i >= 0; i--){
                if(n.key[i] != null){
                    largestKey = n.key[i];
                    return largestKey;
                }
            }
        }
        //if node is not a leaf loop through keys in reverse and set n to
        //the last reference
        while(!n.isLeaf){
            for(int i = n.nKeys - 1 ; i >= 0; i--){
                if(n.key[i]!= null){
                    n = (Node)n.ref[i + 1];
                    i = n.nKeys;
                    if(n.isLeaf){
                        for(int j = n.nKeys; j >= 0; j-- ){
                            if(n.key[j]!= null){
                                largestKey = n.key[j];
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }
        return largestKey;
    } // lastKey

    /********************************************************************************
     * Return the portion of the B+Tree map where key < toKey.
     * @return  the submap with keys in the range [firstKey, toKey)
     */
    public SortedMap <K,V> headMap (K toKey)
    {
        return subMap(null, toKey);
    } // headMap

    /********************************************************************************
     * Return the portion of the B+Tree map where fromKey <= key.
     * @return  the submap with keys in the range [fromKey, lastKey]
     */
    public SortedMap <K,V> tailMap (K fromKey)
    {
        return subMap(fromKey, null);
    } // tailMap

    /********************************************************************************
     * Return the portion of the B+Tree map whose keys are between fromKey and toKey,
     * i.e., fromKey <= key < toKey.
     * @return  the submap with keys in the range [fromKey, toKey)
     */
    public SortedMap <K,V> subMap (K fromKey, K toKey)
    {
        // Create empty submap
        BpTreeMap <K, V> newMap = new BpTreeMap <> (classK, classV);

        // Set start and end keys
        K first = fromKey == null ? firstKey() : fromKey;
        K second = toKey == null ? lastKey() : toKey;

        Node temp = root;
        boolean foundLeaf = false;
        while (!foundLeaf)
        {
            if (temp.ref[0].getClass() != Node.class)
            {
                break;
            }

            for (int i = 0;i < temp.nKeys;i ++)
            {
                K key = temp.key[i];
                if (first.compareTo(key) < 0)
                {
                    temp = (Node) temp.ref[i];
                    break;
                }
                temp = (Node) temp.ref[temp.nKeys];
            }
        }

        int index = 0;
        for (int i = 0;i < temp.nKeys;i ++)
        {
            if (temp.key[i].compareTo(first) == 0)
            {
                index = i;
                break;
            }
        }

        while (temp != null)
        {
            for (int i = index;i < temp.nKeys;i ++)
            {
                K key = temp.key[i];
                if (key.compareTo(second) > 0)
                {
                    return newMap;
                }
                newMap.put(key, (V) temp.ref[i]);
            }

            temp = (Node) temp.ref[ORDER];
            index = 0;
        }

        return newMap;
    } // subMap

    /********************************************************************************
     * Return the size (number of keys) in the B+Tree.
     * @return  the size of the B+Tree
     */
    public int size ()
    {
        return treeSize;
    } // size

    /********************************************************************************
     * Print the B+Tree using a pre-order traveral and indenting each level.
     * @param n      the current node to print
     * @param level  the current level of the B+Tree
     */
    @SuppressWarnings("unchecked")
    private void print (Node n, int level)
    {
        out.println ("BpTreeMap");
        out.println ("-------------------------------------------");

        for (int j = 0; j < level; j++) out.print ("\t");
        out.print ("[ . ");
        for (int i = 0; i < n.nKeys; i++) out.print (n.key [i] + " . ");
        out.println ("]");
        if ( ! n.isLeaf) {
            for (int i = 0; i <= n.nKeys; i++) print ((Node) n.ref [i], level + 1);
        } // if

        out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Recursive helper function for finding a key in B+trees.
     * @param key  the key to find
     * @param ney  the current node
     */
    @SuppressWarnings("unchecked")
    private V find (K key, Node n)
    {
        count++;
        for (int i = 0; i < n.nKeys; i++) {
            K k_i = n.key [i];
            if (key.compareTo(k_i) <= 0) {
                if (n.isLeaf) {
                    return (key.compareTo(k_i) == 0) ? (V) n.ref [i] : null;
                } else {
                    return find (key, (Node) n.ref [i]);
                } // if
            } // if
        } // for
        return (n.isLeaf) ? null : find (key, (Node) n.ref [n.nKeys]);
    } // find

    /********************************************************************************
     * Recursive helper function for inserting a key in B+trees.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param p    the parent node
     */
    private void insert (K key, V ref, Node n, Node p)
    {
        //if node is a leaf then wedge into its proper place in n.key[]
        if (n.isLeaf)
        {
            //if node does not have max number of keys then loop through n.key
            if (n.nKeys < ORDER - 1) {
                for (int i = 0; i < n.nKeys; i++) {
                    K k_i = n.key [i];
                    //if key is less then k_i wedge in key
                    if (key.compareTo(k_i) < 0) {
                        wedge (key, ref, n, i);
                        return;
                        //if key is equal to k_i then notify as duplicate
                    } else if (key.equals (k_i)) {
                        out.println ("BpTreeMap:insert: attempt to insert duplicate key = " + key);
                    }
                }
                wedge (key, ref, n, n.nKeys);
            }
            //if node has max number of keys then split it
            else {
                Node sib = split (key, ref, n);
                sib.ref[ORDER] = n.ref[ORDER];
                n.ref[ORDER] = sib;

                parentInsert(n, sib, p);
            }
        }

        //if node is not leaf
        else
        {
            boolean didInsert = false;
            //loop through keys
            for (int i = 0;i < n.nKeys;i ++)
            {
                K nKey = n.key[i];
                //insert if key is less then nKey, set didInsert to true
                if (key.compareTo(nKey) < 0)
                {
                    insert (key, ref, (Node) n.ref[i], n);
                    didInsert = true;
                    break;
                }
            }

            //if node has not been inserted then insert
            if (!didInsert) {
                Node tmpNode = (Node) n.ref[n.nKeys];
                insert (key, ref, tmpNode, n);
            }

            //if node is full then split it
            if (n.nKeys > ORDER - 1)
            {
                Node sib = split (null, null, n);
                parentInsert(n, sib, p);
            }
        }
    } // insert

    /******************************************************************************
     * Creates a new prent node with a right and left child
     * @param sib  the sibling node
     * @param p  the parent node
     */
    private void parentInsert (Node n, Node sib, Node p)
    {
        Node parent = p;
        //if node p is the root then set it equal to sib
        if (parent == null)
        {
            parent = new Node(false);
            root = parent;
            parent.ref[0] = n;
        }

        //create variable middle to the key where to split the node
        K middle = n.key[n.nKeys - 1];

        // Remove the key if a non leaf node
        if (!n.isLeaf)
        {
            n.key[n.nKeys] = null;
            n.nKeys--;
        }

        boolean didWedge = false;
        //loop through p.key[]
        for (int i = 0;i < parent.nKeys;i ++)
        {
            K pKey = parent.key[i];

            //wedge middle in its appropriate place
            if (middle.compareTo(pKey) < 0)
            {
                parentWedge(sib, middle, parent, i);
                didWedge = true;
                break;
            }
        }

        //wedge node if it has not been wedged yet
        if (!didWedge)
        {
            parentWedge(sib, middle, parent, parent.nKeys);
        }
    }

    private void parentWedge(Node n, K middle, Node p, int i)
    {
        for (int j = p.nKeys; j > i; j--)
        {
            p.key [j] = p.key [j - 1];
            p.ref [j + 1] = p.ref [j];
        }

        p.key [i] = middle;
        p.ref [i + 1] = n;
        p.nKeys++;
    }

    /********************************************************************************
     * Wedge the key-ref pair into node n.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param i    the insertion position within node n
     */
    private void wedge (K key, Object ref, Node n, int i)
    {
        for (int j = n.nKeys; j > i; j--) {
            n.key [j] = n.key [j - 1];
            n.ref [j] = n.ref [j - 1];
        } // for

        n.key [i] = key;
        n.ref [i] = ref;
        n.nKeys++;
    } // wedge

    /********************************************************************************
     * Split node n and return the newly created node.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     */
    private Node split (K key, Object ref, Node n)
    {
        //out.println ("split not implemented yet");

        //loop through node to wedge in new key before splitting
        if(key != null)
        {
            boolean didWedge = false;
            for (int i = 0; i < n.nKeys; i++ )
            {
                if(key.compareTo(n.key[i]) < 0)
                {
                    wedge(key, ref, n, i);
                    didWedge = true;
                    break;
                }
            }
            if (!didWedge)
            {
                wedge (key, ref, n, n.nKeys);
            }
        }

        //split on the middle key
        int middleIndex = (int)(ORDER/2);
        K middleKey = n.key[middleIndex];

        // Create sibling node
        Node sibling = new Node(n.isLeaf);

        //filling new children with keys and values
        for (int i = 0; i < n.nKeys; i++)
        {
            if (n.key[i].compareTo(middleKey) > 0) {
                wedge(n.key[i], n.ref[i], sibling, sibling.nKeys);
                n.key[i] = null;
                n.ref[i] = null;
            }
        }

        if (!n.isLeaf) {
            sibling.ref[sibling.nKeys] = n.ref[n.nKeys];
            n.ref[n.nKeys] = null;
        }

        n.nKeys = (int) Math.ceil(ORDER / 2.0);

        return sibling;
    } // split

    /********************************************************************************
     * The main method used for testing.
     * @param  the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        BpTreeMap <Integer, Integer> bpt = new BpTreeMap <> (Integer.class, Integer.class);
        int totKeys = 10;
        if (args.length == 1) totKeys = Integer.valueOf (args [0]);
        int values[] = {700701, 458642, 738714, 406377, 312281, 534527, 979993, 370723, 57288, 580918};
        for (int value : values)
        {
            bpt.put (value, value * value);
        }
        //for (int i = 1; i < totKeys; i += 2) {
        //bpt.put (i, i * i);
        // }
        Set <Map.Entry <Integer, Integer>> set = bpt.entrySet();
        bpt.print (bpt.root, 0);
        for (int i = 0; i < totKeys; i++) {
            out.println ("key = " + i + " value = " + bpt.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of nodes accessed = " + bpt.count / (double) totKeys);
    } // main

} // BpTreeMap class
