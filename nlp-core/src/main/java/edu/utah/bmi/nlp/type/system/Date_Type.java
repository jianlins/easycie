
/* First created by JCasGen Mon Oct 04 15:32:40 MDT 2021 */
package edu.utah.bmi.nlp.type.system;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** 
 * Updated by JCasGen Mon Oct 04 15:32:40 MDT 2021
 * generated */
public class Date_Type extends Concept_Type {
  /** generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Date_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Date_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Date(addr, Date_Type.this);
  			   Date_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Date(addr, Date_Type.this);
  	  }
    };
  /** generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Date.typeIndexID;
  /** generated 
     modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.utah.bmi.nlp.type.system.Date");
 
  /** generated */
  final Feature casFeat_NormDate;
  /** generated */
  final int     casFeatCode_NormDate;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getNormDate(int addr) {
        if (featOkTst && casFeat_NormDate == null)
      jcas.throwFeatMissing("NormDate", "edu.utah.bmi.nlp.type.system.Date");
    return ll_cas.ll_getStringValue(addr, casFeatCode_NormDate);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setNormDate(int addr, String v) {
        if (featOkTst && casFeat_NormDate == null)
      jcas.throwFeatMissing("NormDate", "edu.utah.bmi.nlp.type.system.Date");
    ll_cas.ll_setStringValue(addr, casFeatCode_NormDate, v);}
    
  
 
  /** generated */
  final Feature casFeat_Elapse;
  /** generated */
  final int     casFeatCode_Elapse;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public long getElapse(int addr) {
        if (featOkTst && casFeat_Elapse == null)
      jcas.throwFeatMissing("Elapse", "edu.utah.bmi.nlp.type.system.Date");
    return ll_cas.ll_getLongValue(addr, casFeatCode_Elapse);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setElapse(int addr, long v) {
        if (featOkTst && casFeat_Elapse == null)
      jcas.throwFeatMissing("Elapse", "edu.utah.bmi.nlp.type.system.Date");
    ll_cas.ll_setLongValue(addr, casFeatCode_Elapse, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Date_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_NormDate = jcas.getRequiredFeatureDE(casType, "NormDate", "uima.cas.String", featOkTst);
    casFeatCode_NormDate  = (null == casFeat_NormDate) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_NormDate).getCode();

 
    casFeat_Elapse = jcas.getRequiredFeatureDE(casType, "Elapse", "uima.cas.Long", featOkTst);
    casFeatCode_Elapse  = (null == casFeat_Elapse) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Elapse).getCode();

  }
}



    