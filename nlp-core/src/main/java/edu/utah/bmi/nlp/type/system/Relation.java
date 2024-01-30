

/* First created by JCasGen Mon Oct 04 15:32:40 MDT 2021 */
package edu.utah.bmi.nlp.type.system;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Mon Oct 04 15:32:40 MDT 2021
 * XML source: desc/type/All_Types.xml
 * generated */
public class Relation extends Annotation {
  /** generated
   * ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Relation.class);
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
  protected Relation() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Relation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Relation(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Relation(JCas jcas, int begin, int end) {
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
  //* Feature: Note

  /** getter for Note - gets 
   * generated
   * @return value of the feature 
   */
  public String getNote() {
    if (Relation_Type.featOkTst && ((Relation_Type)jcasType).casFeat_Note == null)
      jcasType.jcas.throwFeatMissing("Note", "edu.utah.bmi.nlp.type.system.Relation");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Relation_Type)jcasType).casFeatCode_Note);}
    
  /** setter for Note - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setNote(String v) {
    if (Relation_Type.featOkTst && ((Relation_Type)jcasType).casFeat_Note == null)
      jcasType.jcas.throwFeatMissing("Note", "edu.utah.bmi.nlp.type.system.Relation");
    jcasType.ll_cas.ll_setStringValue(addr, ((Relation_Type)jcasType).casFeatCode_Note, v);}    
   
    
  //*--------------*
  //* Feature: Arg1

  /** getter for Arg1 - gets 
   * generated
   * @return value of the feature 
   */
  public Annotation getArg1() {
    if (Relation_Type.featOkTst && ((Relation_Type)jcasType).casFeat_Arg1 == null)
      jcasType.jcas.throwFeatMissing("Arg1", "edu.utah.bmi.nlp.type.system.Relation");
    return jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Relation_Type)jcasType).casFeatCode_Arg1));}
    
  /** setter for Arg1 - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setArg1(Annotation v) {
    if (Relation_Type.featOkTst && ((Relation_Type)jcasType).casFeat_Arg1 == null)
      jcasType.jcas.throwFeatMissing("Arg1", "edu.utah.bmi.nlp.type.system.Relation");
    jcasType.ll_cas.ll_setRefValue(addr, ((Relation_Type)jcasType).casFeatCode_Arg1, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: Arg2

  /** getter for Arg2 - gets 
   * generated
   * @return value of the feature 
   */
  public Annotation getArg2() {
    if (Relation_Type.featOkTst && ((Relation_Type)jcasType).casFeat_Arg2 == null)
      jcasType.jcas.throwFeatMissing("Arg2", "edu.utah.bmi.nlp.type.system.Relation");
    return jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Relation_Type)jcasType).casFeatCode_Arg2));}
    
  /** setter for Arg2 - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setArg2(Annotation v) {
    if (Relation_Type.featOkTst && ((Relation_Type)jcasType).casFeat_Arg2 == null)
      jcasType.jcas.throwFeatMissing("Arg2", "edu.utah.bmi.nlp.type.system.Relation");
    jcasType.ll_cas.ll_setRefValue(addr, ((Relation_Type)jcasType).casFeatCode_Arg2, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    