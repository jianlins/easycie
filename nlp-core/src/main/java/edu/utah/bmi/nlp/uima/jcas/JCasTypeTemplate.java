/*
 * Copyright  2017  Department of Biomedical Informatics, University of Utah
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.utah.bmi.nlp.uima.jcas;

import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.tools.jcasgen.IError;

import java.util.Iterator;

public class JCasTypeTemplate implements JcasGen.IJCasTypeTemplate {

    public String generate(Object argument) {
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append("\n\n");
        Object [] args = (Object [])argument;
        JcasGen JcasGen = (JcasGen)args[0];
        TypeDescription td = (TypeDescription)args[1];
        JcasGen.packageName = JcasGen.getJavaPkg(td);
        stringBuffer.append("/* First created by JCasGen ");
        stringBuffer.append(JcasGen.getDate());
        stringBuffer.append(" */\n");
        if (0 != JcasGen.packageName.length()) {
            stringBuffer.append("package ");
            stringBuffer.append(JcasGen.packageName);
            stringBuffer.append(";\n");
        }
        else
            JcasGen.error.newError(IError.WARN,
                    JcasGen.getString("pkgMissing", new Object[] {td.getName()}), null);
        stringBuffer.append("\nimport org.apache.uima.jcas.JCas; \nimport org.apache.uima.jcas.JCasRegistry;\nimport org.apache.uima.jcas.cas.TOP_Type;\n\n");
        for(Iterator i = JcasGen.collectImports(td, false).iterator(); i.hasNext();) {
            stringBuffer.append("import ");
            stringBuffer.append((String)i.next());
            stringBuffer.append(";\n");
        }
        stringBuffer.append("\n\n");
        String typeName = JcasGen.getJavaName(td);
        String typeName_Type = typeName + "_Type";
        String jcasTypeCasted = "((" + typeName_Type + ")jcasType)";

        stringBuffer.append("/** ");
        stringBuffer.append(JcasGen.nullBlank(td.getDescription()));
        stringBuffer.append("\n * Updated by JCasGen ");
        stringBuffer.append(JcasGen.getDate());
        stringBuffer.append("\n * XML source: ");
        stringBuffer.append(JcasGen.xmlSourceFileName);
        stringBuffer.append("\n * generated */\npublic class ");
        stringBuffer.append(typeName);
        stringBuffer.append(" extends ");
        stringBuffer.append(JcasGen.getJavaName(td.getSupertypeName()));
        stringBuffer.append(" {\n  /** generated\n   * ordered \n   */\n  @SuppressWarnings (\"hiding\")\n  public final static int typeIndexID = JCasRegistry.register(");
        stringBuffer.append(typeName);
        stringBuffer.append(".class);\n  /** generated\n   * ordered \n   */\n  @SuppressWarnings (\"hiding\")\n  public final static int type = typeIndexID;\n  /** generated\n   * @return index of the type  \n   */\n  @Override\n  public              int getTypeIndexID() {return typeIndexID;}\n \n  /** Never called.  Disable default constructor\n   * generated */\n  protected ");
        stringBuffer.append(typeName);
        stringBuffer.append("() {/* intentionally empty block */}\n    \n  /** Internal - constructor used by generator \n   * generated\n   * @param addr low level Feature Structure reference\n   * @param type the type of this Feature Structure \n   */\n  public ");
        stringBuffer.append(typeName);
        stringBuffer.append("(int addr, TOP_Type type) {\n    super(addr, type);\n    readObject();\n  }\n  \n  /** generated\n   * @param jcas JCas to which this Feature Structure belongs \n   */\n  public ");
        stringBuffer.append(typeName);
        stringBuffer.append("(JCas jcas) {\n    super(jcas);\n    readObject();   \n  } \n");
        if (JcasGen.isSubTypeOfAnnotation(td)) {
            stringBuffer.append("\n  /** generated\n   * @param jcas JCas to which this Feature Structure belongs\n   * @param begin offset to the begin spot in the SofA\n   * @param end offset to the end spot in the SofA \n  */  \n  public ");
            stringBuffer.append(typeName);
            stringBuffer.append("(JCas jcas, int begin, int end) {\n    super(jcas);\n    setBegin(begin);\n    setEnd(end);\n    readObject();\n  }   \n");
        }
        stringBuffer.append("\n  /** \n   * <!-- begin-user-doc -->\n   * Write your own initialization here\n   * <!-- end-user-doc -->\n   *\n   * generated modifiable \n   */\n  private void readObject() {/*default - does nothing empty block */}\n     \n");
        FeatureDescription[] fds = td.getFeatures();
        for (int i = 0; i < fds.length; i++) {
            FeatureDescription fd = fds[i];

            String featName = fd.getName();
            String featUName = JcasGen.uc1(featName);  // upper case first letter
            if (edu.utah.bmi.nlp.uima.jcas.JcasGen.reservedFeatureNames.contains(featUName))
                JcasGen.error.newError(IError.ERROR,
                        JcasGen.getString("reservedNameUsed", new Object[] { featName, td.getName() }),
                        null);

            String featDesc = JcasGen.nullBlank(fd.getDescription());
            String featDescCmt = featDesc;

            String rangeType = JcasGen.getJavaRangeType(fd);
            String elemType = JcasGen.getJavaRangeArrayElementType(fd);

            stringBuffer.append(" \n    \n  //*--------------*\n  //* Feature: ");
            stringBuffer.append(featName);
            stringBuffer.append("\n\n  /** getter for ");
            stringBuffer.append(featName);
            stringBuffer.append(" - gets ");
            stringBuffer.append(featDescCmt);
            stringBuffer.append("\n   * generated\n   * @return value of the feature \n   */\n  public ");
            stringBuffer.append(rangeType);
            stringBuffer.append(" get");
            stringBuffer.append(featUName);
            stringBuffer.append("() {\n    ");
            stringBuffer.append("if (");
            stringBuffer.append(typeName_Type);
            stringBuffer.append(".featOkTst && ");
            stringBuffer.append(jcasTypeCasted);
            stringBuffer.append(".casFeat_");
            stringBuffer.append(featName);
            stringBuffer.append(" == null)\n      jcasType.jcas.throwFeatMissing(\"");
            stringBuffer.append(featName);
            stringBuffer.append("\", \"");
            stringBuffer.append(td.getName());
            stringBuffer.append("\");\n");
            stringBuffer.append("    return ");
            stringBuffer.append(JcasGen.getFeatureValue(fd, td));
            stringBuffer.append(";}\n    \n  /** setter for ");
            stringBuffer.append(featName);
            stringBuffer.append(" - sets ");
            stringBuffer.append(featDescCmt);
            stringBuffer.append(" \n   * generated\n   * @param v value to set into the feature \n   */\n  public void set");
            stringBuffer.append(featUName);
            stringBuffer.append("(");
            stringBuffer.append(rangeType);
            stringBuffer.append(" v) {\n    ");
            stringBuffer.append("if (");
            stringBuffer.append(typeName_Type);
            stringBuffer.append(".featOkTst && ");
            stringBuffer.append(jcasTypeCasted);
            stringBuffer.append(".casFeat_");
            stringBuffer.append(featName);
            stringBuffer.append(" == null)\n      jcasType.jcas.throwFeatMissing(\"");
            stringBuffer.append(featName);
            stringBuffer.append("\", \"");
            stringBuffer.append(td.getName());
            stringBuffer.append("\");\n");
            stringBuffer.append("    ");
            stringBuffer.append(JcasGen.setFeatureValue(fd, td));
            stringBuffer.append(";}    \n  ");
            if (JcasGen.hasArrayRange(fd)) {
                stringBuffer.append("  \n  /** indexed getter for ");
                stringBuffer.append(featName);
                stringBuffer.append(" - gets an indexed value - ");
                stringBuffer.append(featDescCmt);
                stringBuffer.append("\n   * generated\n   * @param i index in the array to get\n   * @return value of the element at index i \n   */\n  public ");
                stringBuffer.append(elemType);
                stringBuffer.append(" get");
                stringBuffer.append(featUName);
                stringBuffer.append("(int i) {\n    ");
                stringBuffer.append("if (");
                stringBuffer.append(typeName_Type);
                stringBuffer.append(".featOkTst && ");
                stringBuffer.append(jcasTypeCasted);
                stringBuffer.append(".casFeat_");
                stringBuffer.append(featName);
                stringBuffer.append(" == null)\n      jcasType.jcas.throwFeatMissing(\"");
                stringBuffer.append(featName);
                stringBuffer.append("\", \"");
                stringBuffer.append(td.getName());
                stringBuffer.append("\");\n");
                stringBuffer.append("    ");
                stringBuffer.append("jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ");
                stringBuffer.append(jcasTypeCasted);
                stringBuffer.append(".casFeatCode_");
                stringBuffer.append(featName);
                stringBuffer.append("), i);\n");
                stringBuffer.append("    return ");
                stringBuffer.append(JcasGen.getArrayFeatureValue(fd, td));
                stringBuffer.append(";}\n\n  /** indexed setter for ");
                stringBuffer.append(featName);
                stringBuffer.append(" - sets an indexed value - ");
                stringBuffer.append(featDescCmt);
                stringBuffer.append("\n   * generated\n   * @param i index in the array to set\n   * @param v value to set into the array \n   */\n  public void set");
                stringBuffer.append(featUName);
                stringBuffer.append("(int i, ");
                stringBuffer.append(elemType);
                stringBuffer.append(" v) { \n    ");
                stringBuffer.append("if (");
                stringBuffer.append(typeName_Type);
                stringBuffer.append(".featOkTst && ");
                stringBuffer.append(jcasTypeCasted);
                stringBuffer.append(".casFeat_");
                stringBuffer.append(featName);
                stringBuffer.append(" == null)\n      jcasType.jcas.throwFeatMissing(\"");
                stringBuffer.append(featName);
                stringBuffer.append("\", \"");
                stringBuffer.append(td.getName());
                stringBuffer.append("\");\n");
                stringBuffer.append("    ");
                stringBuffer.append("jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ");
                stringBuffer.append(jcasTypeCasted);
                stringBuffer.append(".casFeatCode_");
                stringBuffer.append(featName);
                stringBuffer.append("), i);\n");
                stringBuffer.append("    ");
                stringBuffer.append(JcasGen.setArrayFeatureValue(fd, td));
                stringBuffer.append(";}\n  ");
            } /* of hasArray */
        } /* of Features iteration */
        if (td.getName().equals("uima.cas.Annotation")) {
            stringBuffer.append("  ");
            stringBuffer.append("  /** Constructor with begin and end passed as arguments \n    * generated\n    * @param jcas JCas this Annotation is in\n    * @param begin the begin offset\n    * @param end the end offset\n    */\n  public Annotation(JCas jcas, int begin, int end) { \n	  this(jcas); // forward to constructor \n	  this.setBegin(begin); \n	  this.setEnd(end); \n  } \n  \n  /** @see org.apache.uima.cas.text.AnnotationFS#getCoveredText() \n    * generated\n    * @return the covered Text \n    */ \n  public String getCoveredText() { \n    final CAS casView = this.getView();\n    final String text = casView.getDocumentText();\n    if (text == null) {\n      return null;\n    }\n    return text.substring(getBegin(), getEnd());\n  } \n  \n  /** @deprecated \n    * generated\n    * @return the begin offset \n    */\n  public int getStart() {return getBegin();}\n");
        } /* of Annotation if-statement */
        stringBuffer.append("}\n\n    ");
        return stringBuffer.toString();
    }
}