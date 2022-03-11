
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
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Mon Oct 04 15:32:40 MDT 2021
 * generated */
public class Relation_Type extends Annotation_Type {
  /** generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Relation_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Relation_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Relation(addr, Relation_Type.this);
  			   Relation_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Relation(addr, Relation_Type.this);
  	  }
    };
  /** generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Relation.typeIndexID;
  /** generated 
     modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.utah.bmi.nlp.type.system.Relation");
 
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
      jcas.throwFeatMissing("Note", "edu.utah.bmi.nlp.type.system.Relation");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Note);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setNote(int addr, String v) {
        if (featOkTst && casFeat_Note == null)
      jcas.throwFeatMissing("Note", "edu.utah.bmi.nlp.type.system.Relation");
    ll_cas.ll_setStringValue(addr, casFeatCode_Note, v);}
    
  
 
  /** generated */
  final Feature casFeat_Arg1;
  /** generated */
  final int     casFeatCode_Arg1;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArg1(int addr) {
        if (featOkTst && casFeat_Arg1 == null)
      jcas.throwFeatMissing("Arg1", "edu.utah.bmi.nlp.type.system.Relation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_Arg1);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArg1(int addr, int v) {
        if (featOkTst && casFeat_Arg1 == null)
      jcas.throwFeatMissing("Arg1", "edu.utah.bmi.nlp.type.system.Relation");
    ll_cas.ll_setRefValue(addr, casFeatCode_Arg1, v);}
    
  
 
  /** generated */
  final Feature casFeat_Arg2;
  /** generated */
  final int     casFeatCode_Arg2;
  /** generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArg2(int addr) {
        if (featOkTst && casFeat_Arg2 == null)
      jcas.throwFeatMissing("Arg2", "edu.utah.bmi.nlp.type.system.Relation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_Arg2);
  }
  /** generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArg2(int addr, int v) {
        if (featOkTst && casFeat_Arg2 == null)
      jcas.throwFeatMissing("Arg2", "edu.utah.bmi.nlp.type.system.Relation");
    ll_cas.ll_setRefValue(addr, casFeatCode_Arg2, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Relation_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_Note = jcas.getRequiredFeatureDE(casType, "Note", "uima.cas.String", featOkTst);
    casFeatCode_Note  = (null == casFeat_Note) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Note).getCode();

 
    casFeat_Arg1 = jcas.getRequiredFeatureDE(casType, "Arg1", "uima.tcas.Annotation", featOkTst);
    casFeatCode_Arg1  = (null == casFeat_Arg1) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Arg1).getCode();

 
    casFeat_Arg2 = jcas.getRequiredFeatureDE(casType, "Arg2", "uima.tcas.Annotation", featOkTst);
    casFeatCode_Arg2  = (null == casFeat_Arg2) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Arg2).getCode();

  }
}



    