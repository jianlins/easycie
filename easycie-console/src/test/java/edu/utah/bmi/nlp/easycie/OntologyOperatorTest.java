package edu.utah.bmi.nlp.easycie;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

public class OntologyOperatorTest {

    @Disabled
    @Test
    public void test() {
        OntologyOperator ontologyOperator = new OntologyOperator("src/test/resources/colonoscopyQuality.owl");
        ontologyOperator.setMappingTypes("conf/contextMapping.tsv");
        ontologyOperator.exportModifiers(new File("target/generated-test-sources/tRule.xlsx"),
                new File("target/generated-test-sources/cRule.xlsx"),
                new File("target/generated-test-sources/context.xlsx"));
    }

}