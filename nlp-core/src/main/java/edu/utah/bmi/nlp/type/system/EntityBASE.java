

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
public class EntityBASE extends Annotation {
  /** generated
   * ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EntityBASE.class);
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
  protected EntityBASE() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public EntityBASE(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public EntityBASE(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public EntityBASE(JCas jcas, int begin, int end) {
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
    if (EntityBASE_Type.featOkTst && ((EntityBASE_Type)jcasType).casFeat_Note == null)
      jcasType.jcas.throwFeatMissing("Note", "edu.utah.bmi.nlp.type.system.EntityBASE");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EntityBASE_Type)jcasType).casFeatCode_Note);}
    
  /** setter for Note - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setNote(String v) {
    if (EntityBASE_Type.featOkTst && ((EntityBASE_Type)jcasType).casFeat_Note == null)
      jcasType.jcas.throwFeatMissing("Note", "edu.utah.bmi.nlp.type.system.EntityBASE");
    jcasType.ll_cas.ll_setStringValue(addr, ((EntityBASE_Type)jcasType).casFeatCode_Note, v);}    
  }

    