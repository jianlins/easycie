

/* First created by JCasGen Mon Oct 04 15:32:40 MDT 2021 */
package edu.utah.bmi.nlp.type.system;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Mon Oct 04 15:32:40 MDT 2021
 * XML source: desc/type/All_Types.xml
 * generated */
public class Date extends Concept {
  /** generated
   * ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Date.class);
  /** generated
   * ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * generated */
  protected Date() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Date(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Date(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Date(JCas jcas, int begin, int end) {
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
   * generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: NormDate

  /** getter for NormDate - gets 
   * generated
   * @return value of the feature 
   */
  public String getNormDate() {
    if (Date_Type.featOkTst && ((Date_Type)jcasType).casFeat_NormDate == null)
      jcasType.jcas.throwFeatMissing("NormDate", "edu.utah.bmi.nlp.type.system.Date");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Date_Type)jcasType).casFeatCode_NormDate);}
    
  /** setter for NormDate - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setNormDate(String v) {
    if (Date_Type.featOkTst && ((Date_Type)jcasType).casFeat_NormDate == null)
      jcasType.jcas.throwFeatMissing("NormDate", "edu.utah.bmi.nlp.type.system.Date");
    jcasType.ll_cas.ll_setStringValue(addr, ((Date_Type)jcasType).casFeatCode_NormDate, v);}    
   
    
  //*--------------*
  //* Feature: Elapse

  /** getter for Elapse - gets 
   * generated
   * @return value of the feature 
   */
  public long getElapse() {
    if (Date_Type.featOkTst && ((Date_Type)jcasType).casFeat_Elapse == null)
      jcasType.jcas.throwFeatMissing("Elapse", "edu.utah.bmi.nlp.type.system.Date");
    return jcasType.ll_cas.ll_getLongValue(addr, ((Date_Type)jcasType).casFeatCode_Elapse);}
    
  /** setter for Elapse - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setElapse(long v) {
    if (Date_Type.featOkTst && ((Date_Type)jcasType).casFeat_Elapse == null)
      jcasType.jcas.throwFeatMissing("Elapse", "edu.utah.bmi.nlp.type.system.Date");
    jcasType.ll_cas.ll_setLongValue(addr, ((Date_Type)jcasType).casFeatCode_Elapse, v);}    
  }

    