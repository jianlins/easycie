

/* First created by JCasGen Mon Oct 04 15:32:40 MDT 2021 */
package edu.utah.bmi.nlp.type.system;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Mon Oct 04 15:32:40 MDT 2021
 * XML source: desc/type/All_Types.xml
 * generated */
public class ConceptBASE extends EntityBASE {
  /** generated
   * ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(ConceptBASE.class);
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
  protected ConceptBASE() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public ConceptBASE(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public ConceptBASE(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public ConceptBASE(JCas jcas, int begin, int end) {
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
  //* Feature: Category

  /** getter for Category - gets 
   * generated
   * @return value of the feature 
   */
  public String getCategory() {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Category == null)
      jcasType.jcas.throwFeatMissing("Category", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Category);}
    
  /** setter for Category - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setCategory(String v) {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Category == null)
      jcasType.jcas.throwFeatMissing("Category", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    jcasType.ll_cas.ll_setStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Category, v);}    
   
    
  //*--------------*
  //* Feature: Text

  /** getter for Text - gets 
   * generated
   * @return value of the feature 
   */
  public String getText() {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Text == null)
      jcasType.jcas.throwFeatMissing("Text", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Text);}
    
  /** setter for Text - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setText(String v) {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Text == null)
      jcasType.jcas.throwFeatMissing("Text", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    jcasType.ll_cas.ll_setStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Text, v);}    
   
    
  //*--------------*
  //* Feature: Section

  /** getter for Section - gets 
   * generated
   * @return value of the feature 
   */
  public String getSection() {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Section == null)
      jcasType.jcas.throwFeatMissing("Section", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Section);}
    
  /** setter for Section - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setSection(String v) {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Section == null)
      jcasType.jcas.throwFeatMissing("Section", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    jcasType.ll_cas.ll_setStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Section, v);}    
   
    
  //*--------------*
  //* Feature: Negation

  /** getter for Negation - gets 
   * generated
   * @return value of the feature 
   */
  public String getNegation() {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Negation == null)
      jcasType.jcas.throwFeatMissing("Negation", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Negation);}
    
  /** setter for Negation - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setNegation(String v) {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Negation == null)
      jcasType.jcas.throwFeatMissing("Negation", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    jcasType.ll_cas.ll_setStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Negation, v);}    
   
    
  //*--------------*
  //* Feature: Certainty

  /** getter for Certainty - gets 
   * generated
   * @return value of the feature 
   */
  public String getCertainty() {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Certainty == null)
      jcasType.jcas.throwFeatMissing("Certainty", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Certainty);}
    
  /** setter for Certainty - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setCertainty(String v) {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Certainty == null)
      jcasType.jcas.throwFeatMissing("Certainty", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    jcasType.ll_cas.ll_setStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Certainty, v);}    
   
    
  //*--------------*
  //* Feature: Temporality

  /** getter for Temporality - gets 
   * generated
   * @return value of the feature 
   */
  public String getTemporality() {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Temporality == null)
      jcasType.jcas.throwFeatMissing("Temporality", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Temporality);}
    
  /** setter for Temporality - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setTemporality(String v) {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Temporality == null)
      jcasType.jcas.throwFeatMissing("Temporality", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    jcasType.ll_cas.ll_setStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Temporality, v);}    
   
    
  //*--------------*
  //* Feature: Experiencer

  /** getter for Experiencer - gets 
   * generated
   * @return value of the feature 
   */
  public String getExperiencer() {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Experiencer == null)
      jcasType.jcas.throwFeatMissing("Experiencer", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Experiencer);}
    
  /** setter for Experiencer - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setExperiencer(String v) {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Experiencer == null)
      jcasType.jcas.throwFeatMissing("Experiencer", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    jcasType.ll_cas.ll_setStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Experiencer, v);}    
   
    
  //*--------------*
  //* Feature: Annotator

  /** getter for Annotator - gets 
   * generated
   * @return value of the feature 
   */
  public String getAnnotator() {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Annotator == null)
      jcasType.jcas.throwFeatMissing("Annotator", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Annotator);}
    
  /** setter for Annotator - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setAnnotator(String v) {
    if (ConceptBASE_Type.featOkTst && ((ConceptBASE_Type)jcasType).casFeat_Annotator == null)
      jcasType.jcas.throwFeatMissing("Annotator", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    jcasType.ll_cas.ll_setStringValue(addr, ((ConceptBASE_Type)jcasType).casFeatCode_Annotator, v);}    
  }

    