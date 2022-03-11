/*******************************************************************************
 * Copyright  Apr 11, 2015  Department of Biomedical Informatics, University of Utah
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package edu.utah.bmi.nlp.sectiondectector;

import edu.utah.bmi.nlp.core.*;
import edu.utah.bmi.nlp.fastcner.FastCNER;
import edu.utah.bmi.nlp.uima.ae.RuleBasedAEInf;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.commons.lang3.AnnotationUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnnotationFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


/**
 * @author Jianlin Shi
 */
public class SectionDetectorR_AE extends JCasAnnotator_ImplBase implements RuleBasedAEInf {
    public static final String PARAM_RULE_STR = DeterminantValueSet.PARAM_RULE_STR;
    public static final String PARAM_ENABLE_DEBUG = "EnableDebug";
    public static final String PARAM_REPLICATION_SUPPORT = "ReplicationSupport";
    public static final String PARAM_MAXREPEATLENGTH = "MaxRepeatLength";
    public static final String PARAM_DEFAULT_DECTION="DefaultSection";
    protected FastCNER fastCNER;
    protected boolean debug;
    protected String defaultSection="DefaultSection";
    protected HashMap<String, Class<? extends Annotation>> sectionTypes = new HashMap<>();


    public void initialize(UimaContext cont) {
        int maxRepeatLength = 50;
        boolean replicationSupport;
        String ruleStr = (String) (cont
                .getConfigParameterValue(PARAM_RULE_STR));
        Object obj = cont.getConfigParameterValue(PARAM_ENABLE_DEBUG);
        if (obj != null && obj instanceof Boolean && (Boolean) obj != false)
            debug = true;

        obj = cont.getConfigParameterValue(PARAM_REPLICATION_SUPPORT);

        if (obj == null)
            replicationSupport = true;
        else
            replicationSupport = (Boolean) obj;
        obj = cont.getConfigParameterValue(PARAM_MAXREPEATLENGTH);
        if (obj != null)
            maxRepeatLength = (int) obj;

        obj = cont.getConfigParameterValue(PARAM_DEFAULT_DECTION);
        if (obj != null && obj instanceof String && ((String)obj).trim().length()>0) {
            defaultSection = (String) obj;

        }

        fastCNER = new FastCNER(ruleStr);
        fastCNER.setReplicationSupport(replicationSupport);
        fastCNER.setMaxRepeatLength(maxRepeatLength);

        LinkedHashMap<String, TypeDefinition> sectionNames = fastCNER.getTypeDefinitions();
//      default section name could have different domain name, needs to be generic
        sectionNames.put("DefaultSection",new TypeDefinition(defaultSection,Annotation.class.getCanonicalName()));
        for (Map.Entry<String, TypeDefinition> sectionTypeSuperTypePair : sectionNames.entrySet()) {
            TypeDefinition typeDefinition = sectionTypeSuperTypePair.getValue();
            String sectionName = sectionTypeSuperTypePair.getKey();
            String SectionTypeFullName = typeDefinition.fullTypeName;
            Class sectionTypeClass = AnnotationOper.getTypeClass(SectionTypeFullName);
            sectionTypes.put(sectionName, sectionTypeClass);
        }
    }


    public void process(JCas jcas) throws AnalysisEngineProcessException {
        String docText = jcas.getDocumentText();
        char[] docTextChars = docText.toCharArray();
        HashMap<String, ArrayList<Span>> sectionHeaders = fastCNER.processString(docText);
        ArrayList<Span> sortedHeaders = saveSectionHeaders(jcas, sectionHeaders);

        int begin = 0;
        String sectionName = "DefaultSection";
        for (Span sectionHeader : sortedHeaders) {
            saveTrimmedSectionBody(jcas, begin, sectionHeader.getBegin(), docTextChars, sectionName);

            sectionName = sectionHeader.getText();
            sectionName = sectionName.substring(0, sectionName.length() - 6);
            begin = sectionHeader.getEnd() + 1;
        }
        saveTrimmedSectionBody(jcas, begin, docTextChars.length, docTextChars, sectionName);
    }

    private void saveTrimmedSectionBody(JCas jCas, int begin, int end, char[] textChars, String sectionName) {
        while (begin < textChars.length && Character.isWhitespace(textChars[begin])) {
            begin++;
        }
        while (end > begin && Character.isWhitespace(textChars[end - 1])) {
            end--;
        }
        if (end > begin) {
            saveAnnotation(jCas, begin, end, sectionTypes.get(sectionName));
        }
    }

    /**
     * You will need to change to your own Type system. Also can use determinants to annotate different Annotations.
     *
     * @param jcas           UIMA JCas object
     * @param sectionHeaders Section header spans grouped by section header type
     * @return a list of sorted header spans
     */

    protected ArrayList<Span> saveSectionHeaders(JCas jcas, HashMap<String, ArrayList<Span>> sectionHeaders) {
        ArrayList<Span> sortedHeaders = cleanOverlappedHeaders(sectionHeaders);
        for (Span span : sortedHeaders) {
            String sectionHeaderTypeName = span.text;
            if (getSpanType(span) == DeterminantValueSet.Determinants.ACTUAL) {
                saveAnnotation(jcas, span.begin, span.end, this.sectionTypes.get(sectionHeaderTypeName));
            }
        }
        return sortedHeaders;
    }

    private ArrayList<Span> cleanOverlappedHeaders(Map<String, ArrayList<Span>> sectionHeaders) {
        IntervalST<Integer> tree = new IntervalST<>();
        HashMap<Integer, Span> allHeaders = new HashMap<>();
        HashSet<Integer> toRemove = new HashSet<>();
        int id = 0;
        for (Map.Entry<String, ArrayList<Span>> entry : sectionHeaders.entrySet()) {
            for (Span span : entry.getValue()) {
//                use text field to store section header type
                span.text = entry.getKey();
                allHeaders.put(id, span);
                Interval1D intervalD = new Interval1D(span.begin, span.end);
                if (tree.search(intervalD) == null) {
                    tree.put(intervalD, id);
                } else {
                    int previousId = tree.get(intervalD);
                    Span previousSpan = allHeaders.get(previousId);
                    if (previousSpan.width < span.width || (previousSpan.width == span.width &&
                            fastCNER.getMatchedRuleString(previousSpan).score < fastCNER.getMatchedRuleString(span).score)) {
                        toRemove.add(previousId);
                        tree.remove(intervalD);
                        tree.put(intervalD, id);
                    } else {
                        toRemove.add(id);
                    }
                }
                id++;
            }
        }
        for (int i : toRemove) {
            allHeaders.remove(i);
        }
        ArrayList<Span> sortHeaders = new ArrayList<>();
        sortHeaders.addAll(allHeaders.values());
        Collections.sort(sortHeaders, new SpanComparator());
        return sortHeaders;

    }


//    // TODO need test
//    private void cleanOverlappedHeaders(HashMap<String, ArrayList<Span>> sectionHeaders) {
//        IntervalST<Map.Entry<String, Integer>> tree = new IntervalST<>();
//        HashMap<String, ArrayList<Integer>> toRemove = new HashMap<>();
//        for (Map.Entry<String, ArrayList<Span>> entry : sectionHeaders.entrySet()) {
//            for (int i = 0; i < entry.getValue().size(); i++) {
//                Span span = entry.getValue().get(i);
//                Interval1D intervalD = new Interval1D(span.begin, span.end);
//                if (tree.search(intervalD) == null) {
//                    tree.put(intervalD, createEntry(entry.getKey(), i));
//                } else {
//                    Map.Entry<String, Integer> previousEntry = tree.get(intervalD);
//                    Span previousSpan = sectionHeaders.get(previousEntry.getKey()).get(previousEntry.getValue());
//                    if (fastCNER.getMatchedRuleString(previousSpan).score < fastCNER.getMatchedRuleString(span).score) {
//                        if (!toRemove.containsKey(previousEntry.getKey()))
//                            toRemove.put(previousEntry.getKey(), new ArrayList<>());
//                        toRemove.get(previousEntry.getKey()).add(previousEntry.getValue());
//                        tree.remove(intervalD);
//                        tree.put(intervalD, createEntry(entry.getKey(), i));
//                    } else {
//                        if (!toRemove.containsKey(entry.getKey()))
//                            toRemove.put(entry.getKey(), new ArrayList<>());
//                        toRemove.get(entry.getKey()).add(i);
//                    }
//                }
//            }
//            for (Map.Entry<String, ArrayList<Integer>> ent : toRemove.entrySet()) {
//                ArrayList<Integer> toRemoveIds = ent.getValue();
////				remove by reverse order, so that the position id won't be shifted.
//                Collections.sort(toRemoveIds, Collections.reverseOrder());
//                for (Integer id : toRemoveIds) {
//                    ArrayList<Span> list = sectionHeaders.get(ent.getKey());
//                    list.remove(id.intValue());
//                }
//            }
//        }
//    }


    private Map.Entry<String, Integer> createEntry(String sectionHeaderName, int pos) {
        return new Map.Entry<String, Integer>() {
            public String getKey() {
                return sectionHeaderName;
            }

            public Integer getValue() {
                return pos;
            }

            public Integer setValue(Integer value) {
                return null;
            }
        };
    }


    protected void saveAnnotation(JCas jcas,  int begin, int end, Class sectionTypeClass) {
        Annotation anno = null;
        anno=AnnotationFactory.createAnnotation(jcas, begin,end, sectionTypeClass);
        anno.addToIndexes();
    }

    protected DeterminantValueSet.Determinants getSpanType(Span span) {
        return fastCNER.getMatchedNEType(span);
    }

    public static LinkedHashMap<String, TypeDefinition> getTypeDefinitions(String ruleStr) {
        return new FastCNER(ruleStr).getTypeDefinitions();

    }

    @Override
    public LinkedHashMap<String, TypeDefinition> getTypeDefs(String ruleStr) {
        return new FastCNER(ruleStr).getTypeDefinitions();
    }
}
