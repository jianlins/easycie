
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
public class ConceptBASE_Type extends EntityBASE_Type {
  /** generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (ConceptBASE_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = ConceptBASE_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new ConceptBASE(addr, ConceptBASE_Type.this);
  			   ConceptBASE_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new ConceptBASE(addr, ConceptBASE_Type.this);
  	  }
    };
  /** generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = ConceptBASE.typeIndexID;
  /** generated 
     modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.utah.bmi.nlp.type.system.ConceptBASE");
 
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
      jcas.throwFeatMissing("Category", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Category);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setCategory(int addr, String v) {
        if (featOkTst && casFeat_Category == null)
      jcas.throwFeatMissing("Category", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    ll_cas.ll_setStringValue(addr, casFeatCode_Category, v);}
    
  
 
  /** generated */
  final Feature casFeat_Text;
  /** generated */
  final int     casFeatCode_Text;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getText(int addr) {
        if (featOkTst && casFeat_Text == null)
      jcas.throwFeatMissing("Text", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Text);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setText(int addr, String v) {
        if (featOkTst && casFeat_Text == null)
      jcas.throwFeatMissing("Text", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    ll_cas.ll_setStringValue(addr, casFeatCode_Text, v);}
    
  
 
  /** generated */
  final Feature casFeat_Section;
  /** generated */
  final int     casFeatCode_Section;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getSection(int addr) {
        if (featOkTst && casFeat_Section == null)
      jcas.throwFeatMissing("Section", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Section);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSection(int addr, String v) {
        if (featOkTst && casFeat_Section == null)
      jcas.throwFeatMissing("Section", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    ll_cas.ll_setStringValue(addr, casFeatCode_Section, v);}
    
  
 
  /** generated */
  final Feature casFeat_Negation;
  /** generated */
  final int     casFeatCode_Negation;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getNegation(int addr) {
        if (featOkTst && casFeat_Negation == null)
      jcas.throwFeatMissing("Negation", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Negation);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setNegation(int addr, String v) {
        if (featOkTst && casFeat_Negation == null)
      jcas.throwFeatMissing("Negation", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    ll_cas.ll_setStringValue(addr, casFeatCode_Negation, v);}
    
  
 
  /** generated */
  final Feature casFeat_Certainty;
  /** generated */
  final int     casFeatCode_Certainty;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getCertainty(int addr) {
        if (featOkTst && casFeat_Certainty == null)
      jcas.throwFeatMissing("Certainty", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Certainty);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setCertainty(int addr, String v) {
        if (featOkTst && casFeat_Certainty == null)
      jcas.throwFeatMissing("Certainty", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    ll_cas.ll_setStringValue(addr, casFeatCode_Certainty, v);}
    
  
 
  /** generated */
  final Feature casFeat_Temporality;
  /** generated */
  final int     casFeatCode_Temporality;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTemporality(int addr) {
        if (featOkTst && casFeat_Temporality == null)
      jcas.throwFeatMissing("Temporality", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Temporality);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTemporality(int addr, String v) {
        if (featOkTst && casFeat_Temporality == null)
      jcas.throwFeatMissing("Temporality", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    ll_cas.ll_setStringValue(addr, casFeatCode_Temporality, v);}
    
  
 
  /** generated */
  final Feature casFeat_Experiencer;
  /** generated */
  final int     casFeatCode_Experiencer;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getExperiencer(int addr) {
        if (featOkTst && casFeat_Experiencer == null)
      jcas.throwFeatMissing("Experiencer", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Experiencer);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setExperiencer(int addr, String v) {
        if (featOkTst && casFeat_Experiencer == null)
      jcas.throwFeatMissing("Experiencer", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    ll_cas.ll_setStringValue(addr, casFeatCode_Experiencer, v);}
    
  
 
  /** generated */
  final Feature casFeat_Annotator;
  /** generated */
  final int     casFeatCode_Annotator;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getAnnotator(int addr) {
        if (featOkTst && casFeat_Annotator == null)
      jcas.throwFeatMissing("Annotator", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Annotator);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAnnotator(int addr, String v) {
        if (featOkTst && casFeat_Annotator == null)
      jcas.throwFeatMissing("Annotator", "edu.utah.bmi.nlp.type.system.ConceptBASE");
    ll_cas.ll_setStringValue(addr, casFeatCode_Annotator, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public ConceptBASE_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_Category = jcas.getRequiredFeatureDE(casType, "Category", "uima.cas.String", featOkTst);
    casFeatCode_Category  = (null == casFeat_Category) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Category).getCode();

 
    casFeat_Text = jcas.getRequiredFeatureDE(casType, "Text", "uima.cas.String", featOkTst);
    casFeatCode_Text  = (null == casFeat_Text) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Text).getCode();

 
    casFeat_Section = jcas.getRequiredFeatureDE(casType, "Section", "uima.cas.String", featOkTst);
    casFeatCode_Section  = (null == casFeat_Section) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Section).getCode();

 
    casFeat_Negation = jcas.getRequiredFeatureDE(casType, "Negation", "uima.cas.String", featOkTst);
    casFeatCode_Negation  = (null == casFeat_Negation) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Negation).getCode();

 
    casFeat_Certainty = jcas.getRequiredFeatureDE(casType, "Certainty", "uima.cas.String", featOkTst);
    casFeatCode_Certainty  = (null == casFeat_Certainty) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Certainty).getCode();

 
    casFeat_Temporality = jcas.getRequiredFeatureDE(casType, "Temporality", "uima.cas.String", featOkTst);
    casFeatCode_Temporality  = (null == casFeat_Temporality) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Temporality).getCode();

 
    casFeat_Experiencer = jcas.getRequiredFeatureDE(casType, "Experiencer", "uima.cas.String", featOkTst);
    casFeatCode_Experiencer  = (null == casFeat_Experiencer) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Experiencer).getCode();

 
    casFeat_Annotator = jcas.getRequiredFeatureDE(casType, "Annotator", "uima.cas.String", featOkTst);
    casFeatCode_Annotator  = (null == casFeat_Annotator) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Annotator).getCode();

  }
}



    