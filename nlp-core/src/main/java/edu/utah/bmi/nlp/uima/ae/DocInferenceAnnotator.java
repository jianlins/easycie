//

//* Copyright  2017  Department of Biomedical Informatics, University of Utah

//* <p>

//* Licensed under the Apache License, Version 2.0 (the "License");

//* you may not use this file except in compliance with the License.

//* You may obtain a copy of the License at

//* <p>

//* http://www.apache.org/licenses/LICENSE-2.0

//* <p>

//* Unless required by applicable law or agreed to in writing, software

//* distributed under the License is distributed on an "AS IS" BASIS,

//* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

//* See the License for the specific language governing permissions and

//* limitations under the License.

//

package edu.utah.bmi.nlp.uima.ae;


import edu.utah.bmi.nlp.core.*;
import edu.utah.bmi.nlp.type.system.Doc_Base;
import edu.utah.bmi.nlp.type.system.Sentence;
import edu.utah.bmi.nlp.type.system.Token;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import edu.utah.bmi.nlp.uima.common.UIMATypeFunctions;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.uima.UimaContext;
//import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnnotationFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.utah.bmi.nlp.core.DeterminantValueSet.*;
import static edu.utah.bmi.nlp.uima.common.AnnotationOper.initSetReflections;


/**
 * From the existence of one concept, conclude a document type annotation.
 * For instance, if a Fever annotation is found, then this document can be concluded as FeverPresentDoc.
 *
 * @author Jianlin Shi
 * Created on 7/6/16.
 */
public class DocInferenceAnnotator extends JCasAnnotator_ImplBase implements RuleBasedAEInf {
    public static Logger logger = IOUtil.getLogger(DocInferenceAnnotator.class);

    //    list document annotation type paired with concept types,    //
    //    "|" is used to differentiate document types,
    //    list the document types in priority order, the left ones have higher priority--the left document type will overwrite the right one if both exist
    //    e.g. "Doc_Yes:Concept1,Concept2|Doc_No:Concept3,Concept4"
    public static final String PARAM_RULE_STR = DeterminantValueSet.PARAM_RULE_STR;
    @ConfigurationParameter(name = PARAM_RULE_STR, mandatory = true)
    protected String inferenceStr;


    public static final String FIRSTWORD = "FIRSTWORD", LASTWORD = "LASTWORD",
            FIRSTSENTENCE = "FIRSTSENTENCE", LASTSENTENCE = "LASTSENTENCE",
            FIRSTEVIDENCE = "FIRSTEVIDENCE", LASTEVIDENCE = "LASTEVIDENCE";

    public static final String PARAM_ANNO_POSITION = "AnnotatePosition";
    @ConfigurationParameter(name = PARAM_ANNO_POSITION, mandatory = false, defaultValue = FIRSTWORD, description = "where to place the conclusion annotation.")
    protected String annotatePosition;

    /**
     * Use concatenated evidence type names as Note values
     */
    public static final String PARAM_OVERWRITE_NOTE = "OverWriteNote";
    @ConfigurationParameter(name = PARAM_OVERWRITE_NOTE, mandatory = false, defaultValue = "true", description = "whether Use concatenated evidence type names as Note values")
    protected boolean overWriteNote;

    public static final String PARAM_REPLACING = "Replacing";
    @ConfigurationParameter(name = PARAM_REPLACING, mandatory = false, defaultValue = "false", description = "whether replace the specified (by PARAM_ANNO_POSITION ) evidence annotation")
    protected boolean replacing;

    //  this configuration is set in RuleStr, using "@aggregate:false"
    protected boolean aggregateFeatures = true;

    public LinkedHashMap<String, ArrayList<ArrayList<Object>>> inferenceMap = new LinkedHashMap<>();

    protected HashMap<String, HashMap<String, Method>> evidenceConceptGetFeatures = new LinkedHashMap<>();

    protected HashMap<String, Class<? extends Annotation>> conceptClassMap = new HashMap<>();


    protected HashMap<Class, HashMap<String, Method>> conclusionConceptSetFeatures = new HashMap<>();

    protected HashMap<String, String> defaultDocTypes = new HashMap<>();


    protected HashMap<String, String> valueFeatureMap = new HashMap<>();

    //    map feature name to class name, if the feature name is unique, otherwise, map the feature name  to the 1st referenced class name
    protected HashMap<String, String> uniqueFeatureClassMap = new HashMap<>();

    protected ArrayList<ArrayList<String>> ruleCells = new ArrayList<>();


    protected HashMap<Class<? extends Annotation>, IntervalST<Annotation>> evidenceAnnotationTree = new HashMap<>();
    protected HashMap<Class<? extends Annotation>, IntervalST<Integer>> scopeAnnotationTree = new HashMap<>();
    protected HashMap<Class<? extends Annotation>, ArrayList<Annotation>> scopeAnnotations = new HashMap<>();


    //
    //    record current document answers, Key for topic, value for document type
    protected HashMap<Integer, String> currentDocTypes = new HashMap<>();
    protected LinkedHashMap<String, TypeDefinition> typeDefinitions = new LinkedHashMap<>();

    protected Pattern pattern = Pattern.compile("^\\s*(\\w+)");

    public void initialize(UimaContext cont) throws ResourceInitializationException {
        super.initialize(cont);
        parseRuleStr(inferenceStr);
    }

    protected void parseRuleStr(String ruleStr) {
        inferenceMap.clear();
        conceptClassMap.clear();
        evidenceConceptGetFeatures.clear();
        conclusionConceptSetFeatures.clear();
        IOUtil ioUtil = new IOUtil(ruleStr, true);
        getTypeDefs(ioUtil);
        for (ArrayList<String> row : ioUtil.getRuleCells()) {
            try {
                String topic = row.get(1).trim();
                if (!inferenceMap.containsKey(topic))
                    inferenceMap.put(topic, new ArrayList<>());
                ArrayList<Object> inference = new ArrayList<>();
//			add doc conclusion type
                String docTypeName = row.get(2).trim();
                inference.add(docTypeName);
//			add evidences
                ArrayList<Class> evidences = new ArrayList<>();
                for (String evidenceTypeName : row.get(4).split(",")) {
                    Class evidenceType;
                    if (!conceptClassMap.containsKey(evidenceTypeName)) {
                        evidenceType = AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace(evidenceTypeName));
                        if (evidenceType == null) {
                            logger.warning("Type: " + evidenceTypeName + " has not been defined before using as an evidence.");
                            logger.warning("Rule: " + row + " skipped.");
                            continue;
                        }
                        conceptClassMap.put(evidenceTypeName, evidenceType);
                    } else {
                        evidenceType = conceptClassMap.get(evidenceTypeName);
                    }
                    evidences.add(evidenceType);
                }
//			add feature reader

                String featureSetting = row.get(3).trim();
                inference.add(new DocInferenceFeatureReader(featureSetting, conceptClassMap, evidenceConceptGetFeatures, evidences));

                inference.add(evidences);
//			add scope
                Class scopeType = null;
                if (row.size() > 5) {
                    scopeType = AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace(row.get(5)));
                    if (scopeType == SourceDocumentInformation.class || scopeType == DocumentAnnotation.class)
                        scopeType = null;
                    else {
//				evidenceAnnotationTree.put(scopeType, new IntervalST<>());
//				only need to index evidence when scope is not "Document"
                        for (Class evidenceType : evidences) {
                            if (!evidenceAnnotationTree.containsKey(evidenceType)) {
                                evidenceAnnotationTree.put(evidenceType, new IntervalST<>());
                            }
                        }
                        scopeAnnotationTree.put(scopeType, new IntervalST<>());
                    }
                }
                inference.add(scopeType);
//			add evidence string for note
                inference.add(row.get(4));
                int windowSize = 0;
                if (row.size() > 6) {
                    String windowSizeStr = row.get(6);
                    try {
                        windowSize = NumberUtils.toInt(windowSizeStr);
                    } catch (Exception e) {

                    }
                }
                inference.add(windowSize);
                inferenceMap.get(topic).add(inference);
            } catch (Exception e) {
                logger.warning("Error parse rule: " + row);
            }
        }

    }


    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        clearCaches(scopeAnnotations, scopeAnnotationTree, evidenceAnnotationTree);
        indexAnnotations(jCas);
        if (logger.isLoggable(Level.FINE)) {
            SourceDocumentInformation so = JCasUtil.selectByIndex(jCas, SourceDocumentInformation.class, 0);
            logger.fine("Write Doc inference for: " + so.getUri());
        }

        ArrayList<Annotation> evidenceAnnotations = null;
        for (String topic : inferenceMap.keySet()) {
            ArrayList<ArrayList<Object>> inferences = inferenceMap.get(topic);
            boolean matched = false;
            for (ArrayList<Object> inference : inferences) {
                logger.finest("\tTrying " + inference.get(4) + "\t" + inference.get(3));
                DocInferenceFeatureReader featureReader = (DocInferenceFeatureReader) inference.get(1);
                ArrayList<Class> evidences = (ArrayList<Class>) inference.get(2);
                Object scope = inference.get(3);
                int windowSize = (int) inference.get(5);
                if (scope == null) {
                    evidenceAnnotations = checkMatchInDoc(jCas, evidences);
                    if (evidenceAnnotations != null && evidenceAnnotations.size() == evidences.size()) {
                        addConclusion(jCas, topic, (String) inference.get(0), evidenceAnnotations, featureReader, (String) inference.get(4), "");
                        matched = true;
                        break;
                    }
                } else {
                    evidenceAnnotations = checkMatchInScope(jCas, evidences, (Class) scope, windowSize);
                    if (evidenceAnnotations != null && evidenceAnnotations.size() == evidences.size()) {
                        addConclusion(jCas, topic, (String) inference.get(0), evidenceAnnotations, featureReader, (String) inference.get(4), ((Class) scope).getSimpleName());
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched && defaultDocTypes.containsKey(topic)) {
                addDefaultConclusion(jCas, topic);
            }
        }
    }

    protected void clearCaches(HashMap<Class<? extends Annotation>, ArrayList<Annotation>> scopeAnnotations,
                               HashMap<Class<? extends Annotation>, IntervalST<Integer>> scopeAnnotationTree,
                               HashMap<Class<? extends Annotation>, IntervalST<Annotation>> evidenceAnnotationTree) {
        for (Class annoClass : scopeAnnotations.keySet()) {
            scopeAnnotations.get(annoClass).clear();
            scopeAnnotationTree.put(annoClass, new IntervalST<>());
        }
        for (Class annoClass : evidenceAnnotationTree.keySet()) {
            evidenceAnnotationTree.put(annoClass, new IntervalST<>());
        }
    }

    protected void addDefaultConclusion(JCas jCas, String topic) {
        Span span = getAnnotationPosition(jCas);
        if (aggregateFeatures) {
            addFeatureAggregatedDocAnnotation(jCas, span, topic, defaultDocTypes.get(topic),
                    aggregateDefaultFeatureValues(defaultDocTypes.get(topic)), "default value");
        } else {
            String resultTypeShortName = defaultDocTypes.get(topic);
            AnnotationDefinition conclusionDef = AnnotationOper.createConclusionAnnotationDefinition(
                    new AnnotationDefinition(typeDefinitions.get(resultTypeShortName)),
                    evidenceConceptGetFeatures, uniqueFeatureClassMap, Arrays.asList(new Annotation[]{}),
                    typeDefinitions);
            if (overWriteNote) {
                conclusionDef.setFeatureValue("Note", "default conclusion");
            }
            addFeatureSeparatedDocAnnotation(jCas, span, conclusionDef, AnnotationOper.getTypeClass(resultTypeShortName));
        }
    }

    protected void addConclusion(JCas jCas, String topic, String resultTypeShortName,
                                 ArrayList<Annotation> evidenceAnnotations, DocInferenceFeatureReader featureReader,
                                 String evidencesString, String scope) {
        if (evidenceAnnotations.size() > 0) {
            if (!scope.equals("SourceDocumentInformation") && !scope.equals("DocumentAnnotation"))
                evidencesString += "(" + scope + ")";
            ArrayList<Annotation> toRemove = new ArrayList<>();
            Span span = getAnnotationPosition(jCas, evidenceAnnotations, toRemove);
            if (aggregateFeatures) {
                addFeatureAggregatedDocAnnotation(jCas, span, topic, resultTypeShortName,
                        featureReader.getFeaturesString(evidenceAnnotations, evidenceConceptGetFeatures),
                        evidencesString);
            } else {
                AnnotationDefinition conclusionDef = featureReader.getAnnotationDef(
                        typeDefinitions.get(resultTypeShortName), evidenceAnnotations,
                        evidenceConceptGetFeatures);
                if (overWriteNote) {
                    conclusionDef.setFeatureValue("Note", evidencesString);
                }
                addFeatureSeparatedDocAnnotation(jCas, span, conclusionDef, AnnotationOper.getTypeClass(resultTypeShortName));
            }
            if (toRemove.size() > 0) {
                for (Annotation anno : toRemove) {
                    anno.removeFromIndexes();
                }
            }
        }
    }

    protected void addFeatureSeparatedDocAnnotation(JCas jCas, Span span, AnnotationDefinition conclusionDef,
                                                    Class<? extends Annotation> docTypeClass) {
        Annotation docAnnotation = AnnotationOper.createAnnotation(jCas, conclusionDef, docTypeClass,
                span.getBegin(), span.getEnd());
        docAnnotation.addToIndexes();
    }

    @Deprecated
    protected void addFeatureSeparatedDocAnnotation(JCas jCas, Span span, String resultTypeShortName, AnnotationDefinition conclusionDef,
                                                    Constructor<? extends Annotation> docTypeConstructor,
                                                    HashMap<String, Method> conclusionSetFeatures) {
        Annotation docAnnotation = AnnotationOper.createAnnotation(jCas, conclusionDef, conceptClassMap.get(resultTypeShortName),
                span.getBegin(), span.getEnd());
        docAnnotation.addToIndexes();
    }

    protected String aggregateDefaultFeatureValues(String docType) {
        if (typeDefinitions.containsKey(docType)) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : typeDefinitions.get(docType).getFeatureValuePairs().entrySet()) {
                String featureName = entry.getKey();
                if (featureName.equals("Features"))
                    return ""+entry.getValue();
                String value =""+ entry.getValue();
                sb.append("\t\t");
                sb.append(featureName);
                sb.append(":\t");
                sb.append(value);
                sb.append("\n");
            }
            return sb.length() > 1 ? sb.substring(0, sb.length() - 1) : "";
        } else {
            return null;
        }
    }

    //	TODO need additional test cases.
    protected ArrayList<Annotation> checkMatchInScope(JCas jCas, ArrayList<Class> evidences,
                                                      Class scopeType, int windowSize) {
        if (!scopeAnnotationTree.containsKey(scopeType))
            return new ArrayList<>();
        for (Annotation firstEvidenceAnnotation : (Collection<Annotation>) JCasUtil.select(jCas, evidences.get(0))) {
            ArrayList<Annotation> evidenceAnnotations = new ArrayList<>();
            evidenceAnnotations.add(firstEvidenceAnnotation);
            Integer scopeAnnotationId = scopeAnnotationTree.get(scopeType).get(new Interval1D(firstEvidenceAnnotation.getBegin(), firstEvidenceAnnotation.getEnd()));
            if (scopeAnnotationId != null) {
                ArrayList<Annotation> scopeAnnotationList = scopeAnnotations.get(scopeType);
                int begin = scopeAnnotationList.get(scopeAnnotationId).getBegin();
                int end = scopeAnnotationList.get(scopeAnnotationId).getEnd();
                if (windowSize > 0) {
                    int beginId = scopeAnnotationId - windowSize;
                    if (beginId < 0)
                        beginId = 0;
                    int endId = scopeAnnotationId + windowSize;
                    if (endId >= scopeAnnotationList.size())
                        endId = scopeAnnotationList.size() - 1;
                    begin = scopeAnnotationList.get(beginId).getBegin();
                    end = scopeAnnotationList.get(endId).getEnd();
                }
                if (evidences.size() == 1) {
                    evidenceAnnotations.add(firstEvidenceAnnotation);
                    return evidenceAnnotations;
                } else {
                    for (int i = 1; i < evidences.size(); i++) {
                        Class evidenceType = evidences.get(i);
                        Annotation evidence = evidenceAnnotationTree.get(evidenceType).get(new Interval1D(begin, end));
                        if (evidence != null) {
                            evidenceAnnotations.add(evidence);
                        } else {
                            break;
                        }
                    }
                    if (evidenceAnnotations.size() != evidences.size())
                        continue;
                    return evidenceAnnotations;
                }
            }
            return new ArrayList<>();
        }
//
//        for (Annotation scopeAnnotation : (Collection<Annotation>) JCasUtil.select(jCas, scopeType)) {
//
//            for (Class evidenceType : evidences) {
//                Annotation evidenceAnnotation = hasAnnotation(jCas, evidenceType, scopeAnnotation);
//                if (evidenceAnnotation == null) {
//                    break;
//                }
//                evidenceAnnotations.add(evidenceAnnotation);
//            }
//            if (evidenceAnnotations.size() == evidences.size())
//                return evidenceAnnotations;
//        }
        return null;
    }


    protected ArrayList<Annotation> checkMatchInDoc(JCas jCas, ArrayList<Class> evidences) {
        ArrayList<Annotation> evidenceAnnotations = new ArrayList<>();
        for (Class evidenceType : evidences) {
            Annotation evidenceAnnotation = hasAnnotation(jCas, evidenceType);
            if (evidenceAnnotation == null) {
                return null;
            }
            evidenceAnnotations.add(evidenceAnnotation);
        }
        return evidenceAnnotations;
    }

    protected void addFeatureAggregatedDocAnnotation(JCas jCas, Span span, String topic, String docTypeName, String featuresString, String evidencesString) {
        if (docTypeName != null) {
            logger.finest("Try add doc annotation: " + docTypeName);
            Annotation anno=AnnotationOper.createAnnotation(jCas, new AnnotationDefinition(typeDefinitions.get(docTypeName)), AnnotationOper.getTypeClass(docTypeName), span.getBegin(), span.getEnd());
            if (anno instanceof Doc_Base) {
                Doc_Base docAnno = (Doc_Base) anno;
                docAnno.setTopic(topic);
                docAnno.setNote(evidencesString);
                docAnno.setFeatures(featuresString);
                docAnno.addToIndexes();
            }
        }
    }


    protected Span getAnnotationPosition(JCas jCas, ArrayList<Annotation> evidenceAnnotations, ArrayList<Annotation> toRemove) {
        Span span = null;
        Annotation anno = null;
        switch (annotatePosition) {
            case FIRSTEVIDENCE:
                anno = evidenceAnnotations.get(0);
                span = new Span(anno.getBegin(), anno.getEnd());
                if (replacing)
                    toRemove.add(anno);
                break;
            case LASTEVIDENCE:
                anno = evidenceAnnotations.get(evidenceAnnotations.size() - 1);
                span = new Span(anno.getBegin(), anno.getEnd());
                if (replacing)
                    toRemove.add(anno);
                break;
            default:
                span = getAnnotationPosition(jCas);
                break;
        }
        return span;
    }

    protected Span getAnnotationPosition(JCas jCas) {
        Annotation anno = null;
        Span span = null;
        switch (annotatePosition) {
            case FIRSTWORD:
                anno = JCasUtil.selectByIndex(jCas, Token.class, 0);
                break;
            case FIRSTSENTENCE:
                anno = JCasUtil.selectByIndex(jCas, Sentence.class, 0);
                break;
            case LASTWORD:
                anno = JCasUtil.selectByIndex(jCas, Token.class, -1);
                break;
            case LASTSENTENCE:
                anno = JCasUtil.selectByIndex(jCas, Sentence.class, -1);
                break;
            default:
//                check if the configured value is a type name or a snippet of text
                if (annotatePosition.startsWith("edu.") || annotatePosition.startsWith("org.")
                        || annotatePosition.startsWith("com.") || annotatePosition.startsWith("net.")) {
                    anno = JCasUtil.selectByIndex(jCas, AnnotationOper.getTypeClass(annotatePosition), 0);
                } else {
                    String docText = jCas.getDocumentText();
                    int begin = docText.indexOf(annotatePosition);
                    int end = begin + annotatePosition.length();
                    span = new Span(begin, end);
                }
                break;
        }
        if (anno != null)
            return new Span(anno.getBegin(), anno.getEnd());
        else {
            String txt = jCas.getDocumentText();
            Matcher matched = pattern.matcher(jCas.getDocumentText());
            if (matched.find()) {
                span = new Span(matched.start(1), matched.end(1));
            }
            return span;
        }
    }

    protected Annotation hasAnnotation(JCas jcas, Class evidenceType) {
        Iterator annoIter = JCasUtil.iterator(jcas, evidenceType);
        if (annoIter.hasNext())
            return (Annotation) annoIter.next();
        return null;
    }

    protected Annotation hasAnnotation(JCas jCas, Class evidenceType, Annotation scopeAnnotation) {
        if (evidenceAnnotationTree.get(evidenceType) == null) {
            return null;
        }
        Annotation res = evidenceAnnotationTree.get(evidenceType).get(new Interval1D(scopeAnnotation.getBegin(), scopeAnnotation.getEnd()));
        return res;
    }

    protected void indexAnnotations(JCas jCas) {
        for (Class<? extends Annotation> conceptType : evidenceAnnotationTree.keySet()) {
            IntervalST<Annotation> intervalST = new IntervalST<>();
            for (Annotation anno : JCasUtil.select(jCas, conceptType)) {
                intervalST.put(new Interval1D(anno.getBegin(), anno.getEnd()), anno);
            }
            evidenceAnnotationTree.put(conceptType, intervalST);
        }
        for (Class<? extends Annotation> scopeType : scopeAnnotationTree.keySet()) {
            IntervalST<Integer> intervalST = new IntervalST<>();
            ArrayList<Annotation> annotationList = new ArrayList<>();
            for (Annotation anno : JCasUtil.select(jCas, scopeType)) {
                intervalST.put(new Interval1D(anno.getBegin(), anno.getEnd()), annotationList.size());
                annotationList.add(anno);
            }
            scopeAnnotationTree.put(scopeType, intervalST);
            scopeAnnotations.put(scopeType, annotationList);
        }
    }


    protected static String getDefaultRuleStr() {
        return "@splitter:\t\n" +
                "@Doc\tNeg_Doc\n" +
                "Doc\tPos_Doc\tConcept";
    }

    /**
     * Because implement a reinforced interface method (static is not reinforced), this is deprecated, just to
     * enable back-compatibility.
     *
     * @param ruleStr Rule file path or rule content string
     * @return Type name--Type definition map
     */
    @Deprecated
    public static LinkedHashMap<String, TypeDefinition> getTypeDefinitions(String ruleStr) {
        return new DocInferenceAnnotator().getTypeDefs(ruleStr);
    }


    public LinkedHashMap<String, TypeDefinition> getTypeDefs(IOUtil ioUtil) {
        if (ioUtil.getInitiations().size() > 0) {
            UIMATypeFunctions.getTypeDefinitions(ioUtil, ruleCells,
                    valueFeatureMap, new HashMap<>(), defaultDocTypes, typeDefinitions);
        }
//      Need to be set in rules, because when generate type definitions (pipeline not initiated yet),
//      need to know whether features need to be separated,
        if (ioUtil.settings.containsKey("aggregate") && ioUtil.settings.get("aggregate").toLowerCase().startsWith("f")) {
            aggregateFeatures = false;
        }

        if (aggregateFeatures) {
            ruleCells = ioUtil.getRuleCells();
            for (ArrayList<String> initRow : ioUtil.getInitiations()) {
                logger.finest("Parse initiation rule:" + initRow);
                String docTypeName = checkNameSpace(initRow.get(2).trim());
                String shortName = DeterminantValueSet.getShortName(docTypeName);
                typeDefinitions.put(shortName, new TypeDefinition(docTypeName, Doc_Base.class.getCanonicalName()));
                if (initRow.size() > 4) {
                    String defaultFeatureStr = "\t\t" + String.join("\n\t\t", initRow.subList(4, initRow.size()));
                    defaultFeatureStr = defaultFeatureStr.replaceAll(":\\s*", ":\t");
                    typeDefinitions.get(shortName).addFeatureDefaultValue("Features", defaultFeatureStr);
                }
            }

            for (ArrayList<String> row : ruleCells) {
                logger.finest("Parse rule:" + row);
                String docTypeName = checkNameSpace(row.get(2).trim());
                String shortName = DeterminantValueSet.getShortName(docTypeName);
                if (!typeDefinitions.containsKey(shortName))
                    typeDefinitions.put(shortName, new TypeDefinition(docTypeName, Doc_Base.class.getCanonicalName()));
            }

        }
        return typeDefinitions;
    }


    @Override
    public LinkedHashMap<String, TypeDefinition> getTypeDefs(String ruleStr) {
        if (ruleStr.trim().length() == 0 || ruleStr.equals("default")) {
            ruleStr = getDefaultRuleStr();
        } else if (ruleStr.indexOf("|") != -1) {
            ruleStr = ruleStr.replaceAll("\\|", "\n");
        }
        IOUtil ioUtil = new IOUtil(ruleStr, true);
        typeDefinitions = getTypeDefs(ioUtil);
        return typeDefinitions;
    }
}
