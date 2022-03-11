

/* First created by JCasGen Mon Oct 04 15:32:40 MDT 2021 */
package edu.utah.bmi.nlp.type.system;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Mon Oct 04 15:32:40 MDT 2021
 * XML source: desc/type/All_Types.xml
 * generated */
public class SectionBody extends EntityBASE {
  /** generated
   * ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(SectionBody.class);
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
  protected SectionBody() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public SectionBody(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public SectionBody(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public SectionBody(JCas jcas, int begin, int end) {
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
    if (SectionBody_Type.featOkTst && ((SectionBody_Type)jcasType).casFeat_Category == null)
      jcasType.jcas.throwFeatMissing("Category", "edu.utah.bmi.nlp.type.system.SectionBody");
    return jcasType.ll_cas.ll_getStringValue(addr, ((SectionBody_Type)jcasType).casFeatCode_Category);}
    
  /** setter for Category - sets  
   * generated
   * @param v value to set into the feature 
   */
  public void setCategory(String v) {
    if (SectionBody_Type.featOkTst && ((SectionBody_Type)jcasType).casFeat_Category == null)
      jcasType.jcas.throwFeatMissing("Category", "edu.utah.bmi.nlp.type.system.SectionBody");
    jcasType.ll_cas.ll_setStringValue(addr, ((SectionBody_Type)jcasType).casFeatCode_Category, v);}    
  }

    