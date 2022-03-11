package edu.utah.bmi.nlp.uima.common;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;



public class AnnotationOperTest {

    @Test
    public void indexAnnotation() {
    }

    @Test
    public void indexAnnotation1() {
    }

    @Test
    public void indexAnnotation2() {
    }

    @Test
    public void indexAnnotation3() {
    }

    @Test
    public void buildAnnoMap() {
    }

    @Test
    public void getTypeId() {
    }

    @Test
    public void getTypeId1() {
    }

    @Test
    public void getTypeClass() {
    }

    @Test
    public void setFeatureValue() {
    }

    @Test
    public void inferMethodName() {
    }

    @Test
    public void inferSetMethodName() {
    }

    @Test
    public void inferGetMethodName() {
    }

    @Test
    public void getFeatureValue() {
    }

    @Test
    public void getMethod() {
        Method m = AnnotationOper.getMethod(TestMethodClass.class, "getTest");
        assert (m != null);
        assert (m.getParameterCount() == 0);
        m = AnnotationOper.getMethod(TestMethodClass.class, "getTest2");
        assert (m == null);
        m = AnnotationOper.getMethod(TestMethodClass.class, "setTest");
        assert (m == null);
        m = AnnotationOper.getMethod(TestMethodClass.class, "setTest", String.class);
        assert (m.getParameterCount()==1);
        assert (m.getParameterTypes()[0]==String.class);
        m = AnnotationOper.getMethod(TestMethodClass.class, "setTest", AnnotationOper.getMethod(TestMethodClass.class, "getTest").getReturnType());
        assert (m.getParameterCount()==1);
        assert (m.getParameterTypes()[0]==int.class);
        m=AnnotationOper.getDefaultSetMethod(TestMethodClass.class, "Test");
        assert (m.getParameterTypes()[0]==int.class);
    }

    @Test
    public void initGetReflections() {
    }

    @Test
    public void initSetReflections() {
    }


}
