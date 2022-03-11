package edu.utah.bmi.nlp.test;

import edu.utah.bmi.nlp.easycie.core.Compare;
import edu.utah.bmi.nlp.easycie.core.EvalCounter;
import edu.utah.bmi.nlp.sql.RecordRow;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Jianlin Shi on 9/23/16.
 */
@Disabled
public class TestComparator {


    @Test
    public void testSimpleComparator() throws IOException {
        Compare.main(new String[]{"data/output.sqlite", "output", "v2", "data/output.sqlite", "output", "v1", "diff"});
    }

    @Test
    public void help() throws IOException {
        Compare.main(new String[]{});
    }

    @Test
    public void testCompare() throws IOException {
        Compare.main(new String[]{"-w","conf/dbconfig.xml","-wt","OUTPUT","-a","v2",
        "-rt","REFERENCE","-ra","gold"});
    }

    @Test
    public void testCompareRelax() throws IOException {
        Compare compare=new Compare();
        EvalCounter evalCounter=new EvalCounter();
        EvalCounter totalCounter=new EvalCounter();
        ArrayList<RecordRow> input=new ArrayList<>();
        input.add(new RecordRow().addCell("ABEGIN",5).addCell("AEND",8));
        ArrayList<RecordRow> refer=new ArrayList<>();
        refer.add(new RecordRow().addCell("ABEGIN",4).addCell("AEND",10));
        compare.relaxCompare(evalCounter, totalCounter,input,refer);
        System.out.println(evalCounter.report());
    }

}
