/*
 * Copyright  2017  Department of Biomedical Informatics, University of Utah
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.utah.bmi.nlp.core;

/******************************************************************************
 *  Compilation:  javac IntervalST.java
 *  Execution:    java IntervalST
 *  Dependencies: Interval1D.java
 *
 *  Interval search tree implemented using a randomized BST.
 *
 *  Duplicate policy:  if an interval is inserted that already
 *                     exists, the new value overwrite the old one
 *
 *  Originally from http://algs4.cs.princeton.edu/93intersection/
 *
 *  Fixed get() Bug , add getAll () By Jianlin Shi
 *
 ******************************************************************************/

import java.util.LinkedList;
import java.util.logging.Logger;


public class IntervalST<Value> {
    private static final Logger logger = IOUtil.getLogger(IntervalST.class);

    private Node root;   // root of the BST
    @Deprecated
    public boolean debug = false;

    // BST helper node data type
    private class Node {
        Interval1D interval;      // key
        Value value;              // associated data
        Node left, right;         // left and right subtrees
        int N;                    // size of subtree rooted at this node
        int max;                  // max endpoint in subtree rooted at this node

        Node(Interval1D interval, Value value) {
            this.interval = interval;
            this.value = value;
            this.N = 1;
            this.max = interval.max;
        }

        public String toString(int level) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Inv(%d, %d, d=%s)", interval.min, interval.max, value));
            sb.append("\n");
            if (left != null) {
                sb.append("l: " + String.format("%" + (level + 1)*2 + "s", "") + left.toString(level + 1));
            }
            if (right != null) {
                sb.append("r: " + String.format("%" + (level + 1)*2 + "s", "") + right.toString(level + 1));
            }
            return sb.toString();
        }

        public String toString() {
            return toString(0);
        }

    }


    /***************************************************************************
     *  BST search
     *  @param interval an input interval for search
     *  @return whether this tree contains the input interval
     ***************************************************************************/

    public boolean contains(Interval1D interval) {
        return (get(interval) != null);
    }

    // return value associated with the given key
    // if no such value, return null
    public Value get(Interval1D interval) {
        return get(root, interval);
    }

    private Value get(Node x, Interval1D interval) {
        while (x != null) {
            if (interval.intersects(x.interval)) return x.value;
            else if (x.left == null) x = x.right;
            else if (x.left.max < interval.min) x = x.right;
            else x = x.left;
        }
        return null;
    }


    /***************************************************************************
     *  randomized insertion
     *  @param interval  an interval linked with value
     *  @param value     the value corresponding to the interval
     ***************************************************************************/
    public void put(Interval1D interval, Value value) {
        if (contains(interval)) {
            logger.fine("duplicate interval, remove the older one");
            remove(interval);
        }
        root = randomizedInsert(root, interval, value);
    }

    public void put(int min, int max, Value value) {
        Interval1D interval = new Interval1D(min, max);
        put(interval, value);
    }

    // make new node the root with uniform probability
    private Node randomizedInsert(Node x, Interval1D interval, Value value) {
        if (x == null) return new Node(interval, value);
        if (Math.random() * size(x) < 1.0) return rootInsert(x, interval, value);
        int cmp = interval.compareTo(x.interval);
        if (cmp < 0) x.left = randomizedInsert(x.left, interval, value);
        else x.right = randomizedInsert(x.right, interval, value);
        fix(x);
        return x;
    }

    private Node rootInsert(Node x, Interval1D interval, Value value) {
        if (x == null) return new Node(interval, value);
        int cmp = interval.compareTo(x.interval);
        if (cmp < 0) {
            x.left = rootInsert(x.left, interval, value);
            x = rotR(x);
        } else {
            x.right = rootInsert(x.right, interval, value);
            x = rotL(x);
        }
        return x;
    }


    /***************************************************************************
     *  deletion
     ***************************************************************************/
    private Node joinLR(Node a, Node b) {
        if (a == null) return b;
        if (b == null) return a;

        if (Math.random() * (size(a) + size(b)) < size(a)) {
            a.right = joinLR(a.right, b);
            fix(a);
            return a;
        } else {
            b.left = joinLR(a, b.left);
            fix(b);
            return b;
        }
    }

    // remove and return value associated with given interval;
    // if no such interval exists return null
    public Value remove(Interval1D interval) {
        Value value = get(interval);
        root = remove(root, interval);
        return value;
    }

    private Node remove(Node h, Interval1D interval) {
        if (h == null) return null;
        int cmp = interval.compareTo(h.interval);
        if (cmp < 0) h.left = remove(h.left, interval);
        else if (cmp > 0) h.right = remove(h.right, interval);
        else h = joinLR(h.left, h.right);
        fix(h);
        return h;
    }


    /***************************************************************************
     *  Interval searching
     *  @param interval input interval for search
     *
     *  @return return an interval in data structure that intersects the given inteval;
     *  return null if no such interval exists
     *  running time is proportional to log N
     ***************************************************************************/


    public Interval1D search(Interval1D interval) {
        return search(root, interval);
    }

    // look in subtree rooted at x
    public Interval1D search(Node x, Interval1D interval) {
        while (x != null) {
            if (interval.intersects(x.interval)) return x.interval;
            else if (x.left == null) x = x.right;
            else if (x.left.max < interval.min) x = x.right;
            else x = x.left;
        }
        return null;
    }


    public Iterable<Value> getAll(Interval1D interval) {
        LinkedList<Value> list = new LinkedList<Value>();
        getAll(root, interval, list);
        return list;
    }

    public LinkedList<Value> getAllAsList(Interval1D interval) {
        LinkedList<Value> list = new LinkedList<Value>();
        getAll(root, interval, list);
        return list;
    }

    public boolean getAll(Node x, Interval1D interval, LinkedList<Value> list) {
        boolean found1 = false;
        boolean found2 = false;
        boolean found3 = false;
        if (x == null)
            return false;
        if (interval.intersects(x.interval)) {
            list.add(x.value);
            found1 = true;
        }
        if (x.left != null && x.left.max >= interval.min)
            found2 = getAll(x.left, interval, list);
        if (found2 || x.left == null || x.left.max < interval.min)
            found3 = getAll(x.right, interval, list);
        return found1 || found2 || found3;
    }

    // return *all* intervals in data structure that intersect the given interval
    // running time is proportional to R log N, where R is the number of intersections
    public Iterable<Interval1D> searchAll(Interval1D interval) {
        LinkedList<Interval1D> list = new LinkedList<Interval1D>();
        searchAll(root, interval, list);
        return list;
    }

    public LinkedList<Interval1D> searchAllAsList(Interval1D interval) {
        LinkedList<Interval1D> list = new LinkedList<Interval1D>();
        searchAll(root, interval, list);
        return list;
    }


    // look in subtree rooted at x
    public boolean searchAll(Node x, Interval1D interval, LinkedList<Interval1D> list) {
        boolean found1 = false;
        boolean found2 = false;
        boolean found3 = false;
        if (x == null)
            return false;
        if (interval.intersects(x.interval)) {
            list.add(x.interval);
            found1 = true;
        }
        if (x.left != null && x.left.max >= interval.min)
            found2 = searchAll(x.left, interval, list);
        if (found2 || x.left == null || x.left.max < interval.min)
            found3 = searchAll(x.right, interval, list);
        return found1 || found2 || found3;
    }


    // @return number of nodes in subtree rooted at x
    public int size() {
        return size(root);
    }

    private int size(Node x) {
        if (x == null) return 0;
        else return x.N;
    }

    //  height of tree (empty tree height = 0)
    public int height() {
        return height(root);
    }

    private int height(Node x) {
        if (x == null) return 0;
        return 1 + Math.max(height(x.left), height(x.right));
    }


    /***************************************************************************
     *  helper BST functions
     ***************************************************************************/

    // fix auxilliar information (subtree count and max fields)
    private void fix(Node x) {
        if (x == null) return;
        x.N = 1 + size(x.left) + size(x.right);
        x.max = max3(x.interval.max, max(x.left), max(x.right));
    }

    private int max(Node x) {
        if (x == null) return Integer.MIN_VALUE;
        return x.max;
    }

    // precondition: a is not null
    private int max3(int a, int b, int c) {
        return Math.max(a, Math.max(b, c));
    }

    // right rotate
    private Node rotR(Node h) {
        Node x = h.left;
        h.left = x.right;
        x.right = h;
        fix(h);
        fix(x);
        return x;
    }

    // left rotate
    private Node rotL(Node h) {
        Node x = h.right;
        h.right = x.left;
        x.left = h;
        fix(h);
        fix(x);
        return x;
    }


    // check integrity of subtree count fields
    public boolean check() {
        return checkCount() && checkMax();
    }

    // check integrity of count fields
    private boolean checkCount() {
        return checkCount(root);
    }

    private boolean checkCount(Node x) {
        if (x == null) return true;
        return checkCount(x.left) && checkCount(x.right) && (x.N == 1 + size(x.left) + size(x.right));
    }

    private boolean checkMax() {
        return checkMax(root);
    }

    private boolean checkMax(Node x) {
        if (x == null) return true;
        return x.max == max3(x.interval.max, max(x.left), max(x.right));
    }

    public String toString() {
        return root.toString();
    }


}
