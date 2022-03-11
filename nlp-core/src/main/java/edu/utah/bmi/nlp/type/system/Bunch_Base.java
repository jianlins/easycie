

/* First created by JCasGen Mon Oct 04 15:32:40 MDT 2021 */
package edu.utah.bmi.nlp.type.system;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Mon Oct 04 15:32:40 MDT 2021
 * XML source: desc/type/All_Types.xml
 * generated */
public class Bunch_Base extends EntityBASE {
  /** generated
   * ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Bunch_Base.class);
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
  protected Bunch_Base() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Bunch_Base(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Bunch_Base(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Bunch_Base(JCas jcas, int begin, int end) {
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
  //* Feature: Topic

  /** getter for Topic - gets 
   * generated
   * @return value of the feature 
   */
  public String getTopic() {
    if (Bunch_Base_Type.featOkTst && ((Bunch_Base_Type)jcasType).casFeat_Topic == null)
      jcasType.jcas.throwFeatMissing("Topic", "edu.utah.bmi.nlp.type.system.Bunch_Base");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Bunch_Base_Type)jcasType).casFeatCode_Topic);}
    
  /** setter for Topic - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setTopic(String v) {
    if (Bunch_Base_Type.featOkTst && ((Bunch_Base_Type)jcasType).casFeat_Topic == null)
      jcasType.jcas.throwFeatMissing("Topic", "edu.utah.bmi.nlp.type.system.Bunch_Base");
    jcasType.ll_cas.ll_setStringValue(addr, ((Bunch_Base_Type)jcasType).casFeatCode_Topic, v);}    
   
    
  //*--------------*
  //* Feature: Features

  /** getter for Features - gets 
   * generated
   * @return value of the feature 
   */
  public String getFeatures() {
    if (Bunch_Base_Type.featOkTst && ((Bunch_Base_Type)jcasType).casFeat_Features == null)
      jcasType.jcas.throwFeatMissing("Features", "edu.utah.bmi.nlp.type.system.Bunch_Base");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Bunch_Base_Type)jcasType).casFeatCode_Features);}
    
  /** setter for Features - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setFeatures(String v) {
    if (Bunch_Base_Type.featOkTst && ((Bunch_Base_Type)jcasType).casFeat_Features == null)
      jcasType.jcas.throwFeatMissing("Features", "edu.utah.bmi.nlp.type.system.Bunch_Base");
    jcasType.ll_cas.ll_setStringValue(addr, ((Bunch_Base_Type)jcasType).casFeatCode_Features, v);}    
   
    
  //*--------------*
  //* Feature: Note

  /** getter for Note - gets 
   * generated
   * @return value of the feature 
   */
  public String getNote() {
    if (Bunch_Base_Type.featOkTst && ((Bunch_Base_Type)jcasType).casFeat_Note == null)
      jcasType.jcas.throwFeatMissing("Note", "edu.utah.bmi.nlp.type.system.Bunch_Base");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Bunch_Base_Type)jcasType).casFeatCode_Note);}
    
  /** setter for Note - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setNote(String v) {
    if (Bunch_Base_Type.featOkTst && ((Bunch_Base_Type)jcasType).casFeat_Note == null)
      jcasType.jcas.throwFeatMissing("Note", "edu.utah.bmi.nlp.type.system.Bunch_Base");
    jcasType.ll_cas.ll_setStringValue(addr, ((Bunch_Base_Type)jcasType).casFeatCode_Note, v);}    
  }

    