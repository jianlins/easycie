
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
public class Doc_Base_Type extends EntityBASE_Type {
  /** generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Doc_Base_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Doc_Base_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Doc_Base(addr, Doc_Base_Type.this);
  			   Doc_Base_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Doc_Base(addr, Doc_Base_Type.this);
  	  }
    };
  /** generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Doc_Base.typeIndexID;
  /** generated 
     modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.utah.bmi.nlp.type.system.Doc_Base");
 
  /** generated */
  final Feature casFeat_Topic;
  /** generated */
  final int     casFeatCode_Topic;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTopic(int addr) {
        if (featOkTst && casFeat_Topic == null)
      jcas.throwFeatMissing("Topic", "edu.utah.bmi.nlp.type.system.Doc_Base");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Topic);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTopic(int addr, String v) {
        if (featOkTst && casFeat_Topic == null)
      jcas.throwFeatMissing("Topic", "edu.utah.bmi.nlp.type.system.Doc_Base");
    ll_cas.ll_setStringValue(addr, casFeatCode_Topic, v);}
    
  
 
  /** generated */
  final Feature casFeat_Features;
  /** generated */
  final int     casFeatCode_Features;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getFeatures(int addr) {
        if (featOkTst && casFeat_Features == null)
      jcas.throwFeatMissing("Features", "edu.utah.bmi.nlp.type.system.Doc_Base");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Features);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFeatures(int addr, String v) {
        if (featOkTst && casFeat_Features == null)
      jcas.throwFeatMissing("Features", "edu.utah.bmi.nlp.type.system.Doc_Base");
    ll_cas.ll_setStringValue(addr, casFeatCode_Features, v);}
    
  
 
  /** generated */
  final Feature casFeat_Note;
  /** generated */
  final int     casFeatCode_Note;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getNote(int addr) {
        if (featOkTst && casFeat_Note == null)
      jcas.throwFeatMissing("Note", "edu.utah.bmi.nlp.type.system.Doc_Base");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Note);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setNote(int addr, String v) {
        if (featOkTst && casFeat_Note == null)
      jcas.throwFeatMissing("Note", "edu.utah.bmi.nlp.type.system.Doc_Base");
    ll_cas.ll_setStringValue(addr, casFeatCode_Note, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Doc_Base_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_Topic = jcas.getRequiredFeatureDE(casType, "Topic", "uima.cas.String", featOkTst);
    casFeatCode_Topic  = (null == casFeat_Topic) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Topic).getCode();

 
    casFeat_Features = jcas.getRequiredFeatureDE(casType, "Features", "uima.cas.String", featOkTst);
    casFeatCode_Features  = (null == casFeat_Features) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Features).getCode();

 
    casFeat_Note = jcas.getRequiredFeatureDE(casType, "Note", "uima.cas.String", featOkTst);
    casFeatCode_Note  = (null == casFeat_Note) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Note).getCode();

  }
}



    