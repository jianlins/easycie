
/* First created by JCasGen Mon Oct 04 15:32:40 MDT 2021 */
package edu.utah.bmi.nlp.type.system;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;

/** 
 * Updated by JCasGen Mon Oct 04 15:32:40 MDT 2021
 * generated */
public class Stend_Type extends EntityBASE_Type {
  /** generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Stend_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Stend_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Stend(addr, Stend_Type.this);
  			   Stend_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Stend(addr, Stend_Type.this);
  	  }
    };
  /** generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Stend.typeIndexID;
  /** generated 
     modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.utah.bmi.nlp.type.system.Stend");



  /** initialize variables to correspond with Cas Type and Features
	 * generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Stend_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

  }
}



    