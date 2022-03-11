package edu.utah.bmi.nlp.runner;

import edu.utah.bmi.nlp.easycie.core.Compare;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Created by Jianlin Shi on 8/7/17.
 */
@Disabled
public class CompareTest {
    @Test
    public void main() throws Exception {
        Compare.main(new String[]{});
    }

    @Test
    public void compare1() throws Exception {
        Compare.main(new String[]{"-w","conf/dbconfig.xml","-wt","OUTPUT","-a","v2","-rt","OUTPUT","-ra","v1"});
    }

    @Test
    public void compare2() throws Exception {
        Compare.main(new String[]{"-w","conf/dbconfig.xml","-wt","OUTPUT","-a","v2","-rt","OUTPUT","-ra","v1","-dt","DIFF"});
    }

    @Test
    public void compare3() throws Exception {
        Compare.main(new String[]{"-w","conf/dbconfig.xml","-wt","OUTPUT","-a","v2","-rt","OUTPUT","-ra","v1","-dt","DIFF","-t","Concept,Digit"});
    }

    @Test
    public void compare4() throws Exception {
        Compare.main(new String[]{"-w","conf/dbconfig.xml","-wt","OUTPUT","-a","v2","-rt","OUTPUT","-ra","v1","-dt","DIFF","-t","Concept,Digit","-s"});
    }

}