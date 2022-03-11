
/* First created by JCasGen Thu Nov 03 11:46:43 MDT 2016 */
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

/** an automatic generated concept type
 * Updated by JCasGen Thu Nov 03 11:46:43 MDT 2016
 * @generated */
public class Digit2_Type extends Concept_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Digit2_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Digit2_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Digit2(addr, Digit2_Type.this);
  			   Digit2_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Digit2(addr, Digit2_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Digit2.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.utah.bmi.type.system.Digit2");
 
  /** @generated */
  final Feature casFeat_TestFeature;
  /** @generated */
  final int     casFeatCode_TestFeature;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTestFeature(int addr) {
        if (featOkTst && casFeat_TestFeature == null)
      jcas.throwFeatMissing("TestFeature", "edu.utah.bmi.type.system.Digit2");
    return ll_cas.ll_getStringValue(addr, casFeatCode_TestFeature);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTestFeature(int addr, String v) {
        if (featOkTst && casFeat_TestFeature == null)
      jcas.throwFeatMissing("TestFeature", "edu.utah.bmi.type.system.Digit2");
    ll_cas.ll_setStringValue(addr, casFeatCode_TestFeature, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Digit2_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_TestFeature = jcas.getRequiredFeatureDE(casType, "TestFeature", "uima.cas.String", featOkTst);
    casFeatCode_TestFeature  = (null == casFeat_TestFeature) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_TestFeature).getCode();

  }
}



    