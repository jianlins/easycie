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

import org.apache.commons.lang.math.NumberUtils;
import org.junit.jupiter.api.Test;


import java.lang.reflect.Method;
import java.util.ArrayList;

import static edu.utah.bmi.nlp.core.StdOut.print;

/**
 * Created by u0876964 on 11/3/16.
 */
public class TestDots {

    public void testPass(int a, Object... b) {
        System.out.println(a);
        if (b != null)
            System.out.println(b.length);
    }

    @Test
    public void test() {
        System.out.println(NumberUtils.createDouble(".73"));
        testPass(1);
    }

    @Test
    public void testReflection() throws ClassNotFoundException, NoSuchMethodException {
        Class cls=Class.forName(DeterminantValueSet.checkNameSpace("Concept"));
        Method m=cls.getMethod("getNegation");
        print(m);
    }

    @Test
    public void passArrayList(){
        ArrayList<Integer>a=new ArrayList<>();
        addValues(a);
        System.out.println(a.size());

    }

    private static void addValues(ArrayList<Integer>c){
        c.add(0);
        c.add(1);
        c.add(2);
    }
}
