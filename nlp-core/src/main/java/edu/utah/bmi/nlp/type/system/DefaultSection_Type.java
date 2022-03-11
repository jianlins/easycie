
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
public class DefaultSection_Type extends SectionBody_Type {
  /** generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (DefaultSection_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = DefaultSection_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new DefaultSection(addr, DefaultSection_Type.this);
  			   DefaultSection_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new DefaultSection(addr, DefaultSection_Type.this);
  	  }
    };
  /** generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = DefaultSection.typeIndexID;
  /** generated 
     modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.utah.bmi.nlp.type.system.DefaultSection");
 
  /** generated */
  final Feature casFeat_Category;
  /** generated */
  final int     casFeatCode_Category;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getCategory(int addr) {
        if (featOkTst && casFeat_Category == null)
      jcas.throwFeatMissing("Category", "edu.utah.bmi.nlp.type.system.DefaultSection");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Category);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setCategory(int addr, String v) {
        if (featOkTst && casFeat_Category == null)
      jcas.throwFeatMissing("Category", "edu.utah.bmi.nlp.type.system.DefaultSection");
    ll_cas.ll_setStringValue(addr, casFeatCode_Category, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public DefaultSection_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_Category = jcas.getRequiredFeatureDE(casType, "Category", "uima.cas.String", featOkTst);
    casFeatCode_Category  = (null == casFeat_Category) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Category).getCode();

  }
}



    