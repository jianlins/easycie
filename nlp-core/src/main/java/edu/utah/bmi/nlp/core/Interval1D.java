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
 * Compilation:  javac Interval1D.java
 * Execution:    java Interval1D
 * <p>
 * Interval data type with integer coordinates.
 *
 * Originally from http://algs4.cs.princeton.edu/93intersection/
 *
 * Extended By Jianlin Shi to support Hashing use.
 ******************************************************************************/


public class Interval1D implements Comparable<Interval1D> {
    public final int min;  // min endpoint
    public final int max;  // max endpoint

    // precondition: min <= max
    public Interval1D(int min, int max) {
        if (min <= max) {
            this.min = min;
            this.max = max;
        } else throw new RuntimeException("Illegal interval");
    }

    // does this interval intersect that one?
    public boolean intersects(Interval1D that) {
        if (that.max < this.min) return false;
        return this.max >= that.min;
    }

    // does this interval a intersect b?
    public boolean contains(int x) {
        return (min <= x) && (x <= max);
    }

    public int compareTo(Interval1D that) {
        if (this.min < that.min) return -1;
        else if (this.min > that.min) return +1;
        else if (this.max < that.max) return -1;
        else if (this.max > that.max) return +1;
        else return 0;
    }

    public String toString() {
        return "[" + min + ", " + max + "]";
    }


    public boolean equals(Object obj){
        return (obj instanceof Interval1D) && this.toString().equals(obj.toString());
    }

    public int hashCode(){
        return toString().hashCode();
    }

}