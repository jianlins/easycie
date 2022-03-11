package edu.utah.bmi.nlp.uima.writer;

import edu.utah.bmi.nlp.compiler.MemoryClassLoader;
import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import edu.utah.bmi.nlp.uima.common.BratOperator;
import edu.utah.bmi.nlp.uima.common.UIMATypeFunctions;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Jianlin Shi
 * Created on 7/9/16.
 */
public class BratExporter_AE extends XMIWritter_AE {
    private ArrayList<Class<? extends Annotation>> exportTypes = new ArrayList<>();
    private LinkedHashMap<Class, LinkedHashSet<Method>> typeMethods;
    private HashMap<String, HashSet<String>> attributeToConcepts;
    private HashMap<String, HashSet<String>> attributeToValues;
    public static String outputStr;
    public static final String PARAM_RULE_STR = DeterminantValueSet.PARAM_RULE_STR;

    public void initialize(UimaContext cont) {
        String ruleFileOrStr = readConfigureString(cont, PARAM_RULE_STR, "");
        IOUtil ioUtils = new IOUtil(ruleFileOrStr);
        LinkedHashMap<String, ArrayList<String>> includeTypes = new LinkedHashMap<>();
        for (ArrayList<String> row : ioUtils.getRuleCells()) {
            includeTypes.put(row.get(1), new ArrayList<>());
            if (row.size() > 2 && row.get(2) != null && row.get(2).trim().length() > 1) {
                if (row.get(2).toLowerCase().equals("null"))
                    includeTypes.put(row.get(1), null);
                else
                    includeTypes.get(row.get(1)).addAll(Arrays.asList(row.get(2).trim().split("\\s*[,;\\|]\\s*")));

            }
        }
        typeMethods = new LinkedHashMap<>();
        initGetMethods(includeTypes);
        attributeToConcepts = new HashMap<>();
        attributeToValues = new HashMap<>();
    }

    @Override
    public void process(JCas jCas) {
        outputStr = BratOperator.exportBrat(jCas, exportTypes, typeMethods, attributeToConcepts, attributeToValues);
    }

    private void initGetMethods(LinkedHashMap<String, ArrayList<String>> includeTypes) {
        for (Map.Entry<String, ArrayList<String>> typeFeaturePairs : includeTypes.entrySet()) {
            String typeName = DeterminantValueSet.checkNameSpace(typeFeaturePairs.getKey());
            ArrayList<String> featureNames = typeFeaturePairs.getValue();
            Class cls = AnnotationOper.getTypeClass(typeName);
            exportTypes.add(cls);
            if (featureNames == null) {
                typeMethods.put(cls, new LinkedHashSet<>());
            } else if (featureNames.size() == 0) {
                AnnotationOper.getMethods(cls,typeMethods.get(cls));
            } else {
                for (String featureName : featureNames) {
                    if (!typeMethods.containsKey(cls))
                        typeMethods.put(cls, new LinkedHashSet<>());
                    typeMethods.get(cls).add(AnnotationOper.getDefaultGetMethod(cls, AnnotationOper.inferGetMethodName(featureName)));
                }
            }
        }
    }

    public void collectionProcessComplete() {
        // no default behavior
        File configFile = new File(outputDirectory, "annotation.conf");
        StringBuilder config = new StringBuilder();
        if (!configFile.exists()) {
            config.append("[entities]\n");
            for (Map.Entry<Class, LinkedHashSet<Method>> entry : typeMethods.entrySet()) {
                config.append(entry.getKey().getSimpleName() + "\n");
            }
            config.append("[features]\n");
            for (Map.Entry<String, HashSet<String>> attribute : attributeToValues.entrySet()) {
                if (attribute.getValue().size() > 0 && !(attribute.getValue().size() == 1 && attribute.getValue().contains(""))) {
                    String attributeName = attribute.getKey();
                    config.append(attributeName);
                    config.append("\tArg:");
                    config.append(serializeHashSet(attributeToConcepts.get(attributeName)));
                    String values = serializeHashSet(attribute.getValue());
                    if (values.length() > 0) {
                        config.append(",\tValue:");
                        config.append(values);
                    }
                    config.append("\n");
                }
            }
            config.append("[relations]\n[events]");

        }

    }

    private String serializeHashSet(HashSet<String> set) {
        if (set.size() == 1) {
            if (set.contains(""))
                return "";
            else
                for (String ele : set)
                    return ele;
        }
        StringBuilder output = new StringBuilder();
        for (String ele : set) {
            if (ele.indexOf(":") != -1 || ele.indexOf(",") != -1)
//                illegal value for brat
                return "";
            output.append(ele);
            output.append("|");
        }
        output.deleteCharAt(output.length() - 1);
        return output.toString();
    }


}
