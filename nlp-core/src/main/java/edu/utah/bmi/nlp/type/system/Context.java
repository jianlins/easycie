

/* First created by JCasGen Mon Oct 04 15:32:40 MDT 2021 */
package edu.utah.bmi.nlp.type.system;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Mon Oct 04 15:32:40 MDT 2021
 * XML source: desc/type/All_Types.xml
 * generated */
public class Context extends EntityBASE {
  /** generated
   * ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Context.class);
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
  protected Context() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Context(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Context(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Context(JCas jcas, int begin, int end) {
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
  //* Feature: ModifierName

  /** getter for ModifierName - gets 
   * generated
   * @return value of the feature 
   */
  public String getModifierName() {
    if (Context_Type.featOkTst && ((Context_Type)jcasType).casFeat_ModifierName == null)
      jcasType.jcas.throwFeatMissing("ModifierName", "edu.utah.bmi.nlp.type.system.Context");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Context_Type)jcasType).casFeatCode_ModifierName);}
    
  /** setter for ModifierName - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setModifierName(String v) {
    if (Context_Type.featOkTst && ((Context_Type)jcasType).casFeat_ModifierName == null)
      jcasType.jcas.throwFeatMissing("ModifierName", "edu.utah.bmi.nlp.type.system.Context");
    jcasType.ll_cas.ll_setStringValue(addr, ((Context_Type)jcasType).casFeatCode_ModifierName, v);}    
   
    
  //*--------------*
  //* Feature: ModifierValue

  /** getter for ModifierValue - gets 
   * generated
   * @return value of the feature 
   */
  public String getModifierValue() {
    if (Context_Type.featOkTst && ((Context_Type)jcasType).casFeat_ModifierValue == null)
      jcasType.jcas.throwFeatMissing("ModifierValue", "edu.utah.bmi.nlp.type.system.Context");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Context_Type)jcasType).casFeatCode_ModifierValue);}
    
  /** setter for ModifierValue - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setModifierValue(String v) {
    if (Context_Type.featOkTst && ((Context_Type)jcasType).casFeat_ModifierValue == null)
      jcasType.jcas.throwFeatMissing("ModifierValue", "edu.utah.bmi.nlp.type.system.Context");
    jcasType.ll_cas.ll_setStringValue(addr, ((Context_Type)jcasType).casFeatCode_ModifierValue, v);}    
   
    
  //*--------------*
  //* Feature: TargetConcept

  /** getter for TargetConcept - gets 
   * generated
   * @return value of the feature 
   */
  public String getTargetConcept() {
    if (Context_Type.featOkTst && ((Context_Type)jcasType).casFeat_TargetConcept == null)
      jcasType.jcas.throwFeatMissing("TargetConcept", "edu.utah.bmi.nlp.type.system.Context");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Context_Type)jcasType).casFeatCode_TargetConcept);}
    
  /** setter for TargetConcept - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setTargetConcept(String v) {
    if (Context_Type.featOkTst && ((Context_Type)jcasType).casFeat_TargetConcept == null)
      jcasType.jcas.throwFeatMissing("TargetConcept", "edu.utah.bmi.nlp.type.system.Context");
    jcasType.ll_cas.ll_setStringValue(addr, ((Context_Type)jcasType).casFeatCode_TargetConcept, v);}    
  }

    