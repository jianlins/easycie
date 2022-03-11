
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
public class Context_Type extends EntityBASE_Type {
  /** generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Context_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Context_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Context(addr, Context_Type.this);
  			   Context_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Context(addr, Context_Type.this);
  	  }
    };
  /** generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Context.typeIndexID;
  /** generated 
     modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.utah.bmi.nlp.type.system.Context");
 
  /** generated */
  final Feature casFeat_ModifierName;
  /** generated */
  final int     casFeatCode_ModifierName;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getModifierName(int addr) {
        if (featOkTst && casFeat_ModifierName == null)
      jcas.throwFeatMissing("ModifierName", "edu.utah.bmi.nlp.type.system.Context");
    return ll_cas.ll_getStringValue(addr, casFeatCode_ModifierName);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setModifierName(int addr, String v) {
        if (featOkTst && casFeat_ModifierName == null)
      jcas.throwFeatMissing("ModifierName", "edu.utah.bmi.nlp.type.system.Context");
    ll_cas.ll_setStringValue(addr, casFeatCode_ModifierName, v);}
    
  
 
  /** generated */
  final Feature casFeat_ModifierValue;
  /** generated */
  final int     casFeatCode_ModifierValue;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getModifierValue(int addr) {
        if (featOkTst && casFeat_ModifierValue == null)
      jcas.throwFeatMissing("ModifierValue", "edu.utah.bmi.nlp.type.system.Context");
    return ll_cas.ll_getStringValue(addr, casFeatCode_ModifierValue);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setModifierValue(int addr, String v) {
        if (featOkTst && casFeat_ModifierValue == null)
      jcas.throwFeatMissing("ModifierValue", "edu.utah.bmi.nlp.type.system.Context");
    ll_cas.ll_setStringValue(addr, casFeatCode_ModifierValue, v);}
    
  
 
  /** generated */
  final Feature casFeat_TargetConcept;
  /** generated */
  final int     casFeatCode_TargetConcept;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTargetConcept(int addr) {
        if (featOkTst && casFeat_TargetConcept == null)
      jcas.throwFeatMissing("TargetConcept", "edu.utah.bmi.nlp.type.system.Context");
    return ll_cas.ll_getStringValue(addr, casFeatCode_TargetConcept);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTargetConcept(int addr, String v) {
        if (featOkTst && casFeat_TargetConcept == null)
      jcas.throwFeatMissing("TargetConcept", "edu.utah.bmi.nlp.type.system.Context");
    ll_cas.ll_setStringValue(addr, casFeatCode_TargetConcept, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Context_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_ModifierName = jcas.getRequiredFeatureDE(casType, "ModifierName", "uima.cas.String", featOkTst);
    casFeatCode_ModifierName  = (null == casFeat_ModifierName) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_ModifierName).getCode();

 
    casFeat_ModifierValue = jcas.getRequiredFeatureDE(casType, "ModifierValue", "uima.cas.String", featOkTst);
    casFeatCode_ModifierValue  = (null == casFeat_ModifierValue) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_ModifierValue).getCode();

 
    casFeat_TargetConcept = jcas.getRequiredFeatureDE(casType, "TargetConcept", "uima.cas.String", featOkTst);
    casFeatCode_TargetConcept  = (null == casFeat_TargetConcept) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_TargetConcept).getCode();

  }
}



    