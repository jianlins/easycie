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

import org.junit.jupiter.api.Test;

/**
 * @author Jianlin Shi
 * Created on 7/5/16.
 */
public class IntervalSTTest {
    @Test
    public void test() {
        IntervalST<Integer> tree = new IntervalST<>();
        tree.put(new Interval1D(1, 3), 100);
        tree.put(new Interval1D(1, 3), 150);
        tree.put(new Interval1D(2, 5), 200);
        tree.put(new Interval1D(4, 6), 300);
        System.out.println(tree.getAll(new Interval1D(3, 3)));
        System.out.println(tree.getAll(new Interval1D(4, 4)));
        System.out.println(tree.getAll(new Interval1D(1, 4)));
    }

    @Test
    public void testRemove() {
        IntervalST<Integer> tree = new IntervalST<>();
        tree.put(new Interval1D(1, 3), 100);
        tree.put(new Interval1D(3, 7), 150);
        tree.put(new Interval1D(2, 5), 200);
        tree.put(new Interval1D(4, 6), 300);
//        System.out.println(tree.searchAll(new Interval1D(2,5)));
        Integer value = tree.get(new Interval1D(2, 5));
        System.out.println(value);
//        System.out.println(tree.searchAll(new Interval1D(2,5)));
    }

    @Test
    public void testPrint() {
        IntervalST<Integer> tree = new IntervalST<>();
        tree.put(0, 3, 100);
        tree.put(5, 8, 110);
        tree.put(6, 10, 120);
        tree.put(8, 9, 130);
        tree.put(15, 23, 140);
        tree.put(19, 20, 150);
        tree.put(17, 19, 160);
        tree.put(26, 26, 160);
        tree.put(25, 30, 160);
        tree.put(16, 21, 160);
        System.out.println(tree);
    }

    @Test
    public void testRemove2() {
        IntervalST<Double> tree = new IntervalST<>();
        for (int i = 0; i < 30; i += 5) {
            tree.put(i, i + 7, i + 0.5);
        }
        for (int i=0;i<20;i+=5){
            System.out.println(tree);
            tree.remove(new Interval1D(i,i+7));
        }
        System.out.println(tree);
    }


}