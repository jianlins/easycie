

/* First created by JCasGen Thu Nov 03 11:46:43 MDT 2016 */
package edu.utah.bmi.nlp.type.system;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** an automatic generated concept type
 * Updated by JCasGen Thu Nov 03 11:46:43 MDT 2016
 * XML source: /uufs/chpc.utah.edu/common/home/u0876964/Downloads/preannotator/desc/type/customized.xml
 * @generated */
public class Digit2 extends Concept {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Digit2.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected Digit2() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Digit2(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Digit2(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Digit2(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: TestFeature

  /** getter for TestFeature - gets URI of document. (For example, file:///MyDirectory/myFile.txt for a simple file or http://incubator.apache.org/uima/index.html for content from a web source.)
   * @generated
   * @return value of the feature 
   */
  public String getTestFeature() {
    if (Digit2_Type.featOkTst && ((Digit2_Type)jcasType).casFeat_TestFeature == null)
      jcasType.jcas.throwFeatMissing("TestFeature", "edu.utah.bmi.type.system.Digit2");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Digit2_Type)jcasType).casFeatCode_TestFeature);}
    
  /** setter for TestFeature - sets URI of document. (For example, file:///MyDirectory/myFile.txt for a simple file or http://incubator.apache.org/uima/index.html for content from a web source.) 
   * @generated
   * @param v value to set into the feature 
   */
  public void setTestFeature(String v) {
    if (Digit2_Type.featOkTst && ((Digit2_Type)jcasType).casFeat_TestFeature == null)
      jcasType.jcas.throwFeatMissing("TestFeature", "edu.utah.bmi.type.system.Digit2");
    jcasType.ll_cas.ll_setStringValue(addr, ((Digit2_Type)jcasType).casFeatCode_TestFeature, v);}    
  }

    