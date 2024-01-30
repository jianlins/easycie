package edu.utah.bmi.nlp.uima.ae;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import edu.utah.bmi.nlp.uima.common.BratOperator;
import edu.utah.bmi.nlp.uima.common.UIMATypeFunctions;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This is an UIMA component that communicates with an external processor server through Brat-format string messages
 * Internally, it convert Brat-format string to/from JCas object.
 * <p>
 * It assumes that the server is set up locally, no authentication is required.
 *
 * @author Jianlin Shi
 * created on 12/15/2018
 */
public class RESTCommunicator extends JCasAnnotator_ImplBase implements RuleBasedAEInf {

    public static final String PARAM_RULE_STR = DeterminantValueSet.PARAM_RULE_STR;
    //  The following three parameters are suppose to be set in Rule files, so that the getTypeDefs interface can be used
    //  without JCasAnnotator_ImplBase's initiation.
    //  The rule file should have two columns, three rows, the 1st column is the parameter name, the 2nd is the URL.

    //  REST URL to read output type definitions
    protected static final String PARAM_OUTPUT_TYPES_DEF_URL = "OutputTypesURL";
    //  REST URL to read input type definitions
    protected static final String PARAM_INPUT_TYPES_NAMES_URL = "InputTypesURL";
    //  REST URL to send and received annotations
    protected static final String PARAM_PROCESSOR_URL = "ProcessorURL";


    protected String processorURL;

    protected ArrayList<Class<? extends Annotation>> inputTypeClasses = new ArrayList<>();
    protected HashMap<String, Class<? extends Annotation>> outputTypeClassesMap = new HashMap<>();

    protected LinkedHashMap<Class, LinkedHashSet<Method>> inputConceptGetFeatures = new LinkedHashMap<>();
    protected HashMap<Class, HashMap<String, Method>> outputConceptSetFeatures = new HashMap<>();

    private HashMap<String, HashSet<String>> attributeToConcepts;
    private HashMap<String, HashSet<String>> attributeToValues;

    protected HashMap<String, Constructor<? extends Annotation>> outputConceptConstructors = new HashMap<>();


    public void initialize(UimaContext cont) {
        attributeToConcepts = new HashMap<>();
        attributeToValues = new HashMap<>();
        Object obj = cont.getConfigParameterValue(PARAM_RULE_STR);
        if (obj != null) {
            String settings = (String) obj;
            IOUtil ioUtil = new IOUtil(settings);
            for (ArrayList<String> row : ioUtil.getRuleCells()) {
                switch (row.get(1).trim()) {
                    case PARAM_INPUT_TYPES_NAMES_URL:
                        String inputTypeString = post(row.get(2).trim());
                        LinkedHashMap<String, ArrayList<String>> includeTypes = new LinkedHashMap<>();
                        for (ArrayList<String> inputDefRow : new IOUtil(inputTypeString).getRuleCells()) {
                            includeTypes.put(inputDefRow.get(1), new ArrayList<>());
                            if (inputDefRow.size() > 2 && inputDefRow.get(2) != null && inputDefRow.get(2).trim().length() > 1) {
                                if (inputDefRow.get(2).equalsIgnoreCase("null"))
                                    includeTypes.put(inputDefRow.get(1), null);
                                else
                                    includeTypes.get(inputDefRow.get(1)).addAll(Arrays.asList(inputDefRow.get(2).trim().split("\\s*[,;\\|]\\s*")));

                            }
                        }
                        initGetMethods(includeTypes, inputTypeClasses, inputConceptGetFeatures);
                        break;
                    case PARAM_OUTPUT_TYPES_DEF_URL:
                        LinkedHashMap<String, TypeDefinition> typeDefinitions = this.parseTypeDefinitions(post(row.get(2).trim()));
                        AnnotationOper.initSetReflections(typeDefinitions, outputTypeClassesMap,
                                outputConceptConstructors, outputConceptSetFeatures);
                        break;
                    case PARAM_PROCESSOR_URL:
                        processorURL = row.get(2).trim();
                        break;
                }
            }
        }
    }

    private void initGetMethods(LinkedHashMap<String, ArrayList<String>> includeTypes,
                                ArrayList<Class<? extends Annotation>> inputTypeClasses, LinkedHashMap<Class,
            LinkedHashSet<Method>> inputConceptGetFeatures) {
        for (Map.Entry<String, ArrayList<String>> typeFeaturePairs : includeTypes.entrySet()) {
            String typeName = DeterminantValueSet.checkNameSpace(typeFeaturePairs.getKey());
            ArrayList<String> featureNames = typeFeaturePairs.getValue();
            Class cls = AnnotationOper.getTypeClass(typeName);
            inputTypeClasses.add(cls);
            if (featureNames == null) {
                inputConceptGetFeatures.put(cls, new LinkedHashSet<>());
            } else if (featureNames.size() == 0) {
                if (!inputConceptGetFeatures.containsKey(cls))
                    inputConceptGetFeatures.put(cls, new LinkedHashSet<>());
                AnnotationOper.getMethods(cls, inputConceptGetFeatures.get(cls));
            } else {
                for (String featureName : featureNames) {
                    if (!inputConceptGetFeatures.containsKey(cls))
                        inputConceptGetFeatures.put(cls, new LinkedHashSet<>());
                    inputConceptGetFeatures.get(cls).add(AnnotationOper.getDefaultGetMethod(cls, AnnotationOper.inferGetMethodName(featureName)));
                }
            }
        }
    }


    private void initGetMethods(Iterable<String> includeTypes) {
        for (String typeName : includeTypes) {
            typeName = DeterminantValueSet.checkNameSpace(typeName);
            Class cls = AnnotationOper.getTypeClass(typeName);
            inputTypeClasses.add(cls);
            if (!inputConceptGetFeatures.containsKey(cls))
                inputConceptGetFeatures.put(cls, new LinkedHashSet<>());
            AnnotationOper.getMethods(cls, inputConceptGetFeatures.get(cls));
        }
    }


    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String request = BratOperator.exportBrat(aJCas, inputTypeClasses, inputConceptGetFeatures, attributeToConcepts, attributeToValues);
        String returnedString = post(processorURL, aJCas.getDocumentText(), request);
        if (returnedString.trim().length() > 0) {
            List<String> returns = Arrays.asList(returnedString.split("\n"));
            BratOperator.importBrat(aJCas, returns, outputConceptConstructors, outputTypeClassesMap, outputConceptSetFeatures);
        }
    }


    @Override
    public LinkedHashMap<String, TypeDefinition> getTypeDefs(String ruleStr) {
        IOUtil ioUtil = new IOUtil(ruleStr);
        for (ArrayList<String> row : ioUtil.getRuleCells()) {
            if (row.get(1).trim().equals(PARAM_OUTPUT_TYPES_DEF_URL)) {
                String typeString = post(row.get(2).trim());
                return parseTypeDefinitions(typeString);
            }
        }
        return null;
    }


    private String post(String URL, String... data) {
        HttpResponse<String> jsonResponse = null;
        try {
            if (data.length > 1)
                jsonResponse
                        = Unirest.post(URL).field("txt", data[0]).field("brat", data[1]).asString();
            else
                jsonResponse
                        = Unirest.post(URL).asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        String output = toJsonObj(jsonResponse.getRawBody()).toString();
        return output;
    }

    public static Object toJsonObj(InputStream inputStream) {
        String theString = null;
        try {
            theString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONParser jsonParser = new JSONParser();
        Object jsonObject = new JSONObject();
        try {
            jsonObject = jsonParser.parse(theString);
        } catch (ParseException e) {
            return theString;
        }
        return jsonObject;
    }

    /**
     * Reuse the FeatureAnnotationInferencer's method
     *
     * @param ruleString server returned type definition string
     * @return Type definitions
     */
    private LinkedHashMap<String, TypeDefinition> parseTypeDefinitions(String ruleString) {
        return new FeatureAnnotationInferencer().getTypeDefinitions(ruleString);
    }


}
