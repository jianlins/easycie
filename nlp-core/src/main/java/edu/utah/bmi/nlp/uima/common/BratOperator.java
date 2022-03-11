package edu.utah.bmi.nlp.uima.common;

import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class BratOperator {
    protected static final String beginOffset = "<begin>";
    protected static final String endOffset = "<end>";
    protected static final String typeName = "<typeName>";

    public static String exportBrat(JCas jCas, ArrayList<Class<? extends Annotation>> exportTypes, LinkedHashMap<Class, LinkedHashSet<Method>> typeMethods,
                                    HashMap<String, HashSet<String>> attributeToConcepts,
                                    HashMap<String, HashSet<String>> attributeToValue) {
        HashSet<String> haveReadTypes = new HashSet<>();
        ArrayList<String> bratAnnotations = new ArrayList<>();
        int i = 0, attrId = 0;
        for (Class<? extends Annotation> cls : exportTypes) {
            String thisClassName = "";
            for(Annotation con: JCasUtil.select(jCas, cls)) {
                thisClassName = con.getClass().getCanonicalName();
                if (haveReadTypes.contains(thisClassName)) {
                    continue;
                }

//            System.out.println(con.getType().getName() + "\t" + con.getCoveredText());
                bratAnnotations.add("T" + i + "\t" + con.getType().getShortName() + " " + con.getBegin() + " " + con.getEnd()
                        + "\t" + con.getCoveredText().replaceAll("[\n\r]", " "));
                attrId = readAttributes(con, typeMethods.get(cls), bratAnnotations, attrId, "T" + i, attributeToConcepts, attributeToValue);
                i++;

            }
            if (thisClassName.length() > 0)
                haveReadTypes.add(thisClassName);
        }
        String outputStr = String.join("\n", bratAnnotations);
        return outputStr;
    }

    private static int readAttributes(Annotation con, LinkedHashSet<Method> methods, ArrayList<String> bratAnnotations,
                                      int attrId, String conceptID,
                                      HashMap<String, HashSet<String>> attributeToConcepts,
                                      HashMap<String, HashSet<String>> attributeToValue) {
        for (Method method : methods) {
            String attribute = method.getName().substring(3);
            String value = "";
            try {
                Object valueObj = method.invoke(con);
                if (valueObj == null)
                    continue;
                if (valueObj instanceof FSArray) {
                    value = serilizeFSArray((FSArray) valueObj).replaceAll("\n", ";");
                } else {
                    value = valueObj + "";
                }
                if (value.trim().length() == 0)
                    continue;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            bratAnnotations.add("A" + attrId + "\t" + attribute + " " + conceptID + " " + value);
            addAttributeValue(con.getClass().getSimpleName(), attribute, value, attributeToConcepts, attributeToValue);
            attrId++;
        }
        return attrId;
    }

    protected static String serilizeFSArray(FSArray ary) {
        StringBuilder sb = new StringBuilder();
        int size = ary.size();
        String[] values = new String[size];
        ary.copyToArray(0, values, 0, size);
        for (FeatureStructure fs : ary) {
            List<Feature> features = fs.getType().getFeatures();
            for (Feature feature : features) {
                String domain = feature.getDomain().getShortName();
                if (domain.equals("AnnotationBase") || domain.equals("Annotation"))
                    continue;
                Type range = feature.getRange();
                if (!range.isPrimitive()) {
                    FeatureStructure child = fs.getFeatureValue(feature);
                    sb.append(child + "");
                } else {
                    sb.append("\t" + feature.getShortName() + ":" + fs.getFeatureValueAsString(feature) + "\n");
                }
            }

        }
        return sb.toString();
    }

    private static void addAttributeValue(String concept, String attribute, String value,
                                          HashMap<String, HashSet<String>> attributeToConcepts,
                                          HashMap<String, HashSet<String>> attributeToValues) {
        if (!attributeToConcepts.containsKey(attribute)) {
            attributeToConcepts.put(attribute, new HashSet<>());
            attributeToValues.put(attribute, new HashSet<>());
        }
        attributeToConcepts.get(attribute).add(concept);
        attributeToValues.get(attribute).add(value);
    }


    public static void importBrat(JCas jcas, List<String> annoContent,
                                  HashMap<String, Constructor<? extends Annotation>> typeConstructors,
                                  HashMap<String, Class<? extends Annotation>> typeClasses,
                                  HashMap<Class, HashMap<String, Method>> typeSetMethods) {
//      concept id,      attribute names, values
        LinkedHashMap<String, LinkedHashMap<String, String>> annotations = new LinkedHashMap<>();
        for (int i = 0; i < annoContent.size(); i++) {
            String line = annoContent.get(i);
            String[] elements = line.split("\\t");
            switch (line.charAt(0)) {
                case 'T':
                    annotations.put(elements[0], new LinkedHashMap<>());
                    String[] properties = elements[1].split("\\s+");
                    annotations.get(elements[0]).put(typeName, properties[0]);
                    annotations.get(elements[0]).put(beginOffset, properties[1]);
                    annotations.get(elements[0]).put(endOffset, properties[2]);
                    break;
                case 'A':
                    String[] linkage = elements[1].split("\\s+");
                    if (!annotations.containsKey(linkage[1])) {
//                      if the concept is not added, the put this attribute to the end.
                        if (i < annoContent.size() - 1)
                            annoContent.add(line);
                    } else {
                        annotations.get(linkage[1]).put(linkage[0], linkage[2]);
                    }
                    break;
            }
        }

        for (LinkedHashMap<String, String> annotation : annotations.values()) {
            String type = annotation.get(typeName);
            annotation.remove(typeName);
            try {
                addAnnotation(jcas, type, annotation, typeConstructors, typeClasses, typeSetMethods);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }


    }

    protected static void addAnnotation(JCas jcas, String typeName, LinkedHashMap<String, String> attributes,
                                        HashMap<String, Constructor<? extends Annotation>> typeConstructors,
                                        HashMap<String, Class<? extends Annotation>> typeClasses,
                                        HashMap<Class, HashMap<String, Method>> typeSetMethods)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        int begin = Integer.parseInt(attributes.get(beginOffset));
        int end = Integer.parseInt(attributes.get(endOffset));
        attributes.remove(beginOffset);
        attributes.remove(endOffset);

        Annotation annotation = typeConstructors.get(typeName).newInstance(jcas, begin, end);


//      Set feature values
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            String featureName = attribute.getKey();
            String value = attribute.getValue();
            String methodName = featureName.substring(0, 1).toUpperCase() + featureName.substring(1);
            if (typeClasses.containsKey(typeName) && typeSetMethods.get(typeClasses.get(typeName)).containsKey(methodName)) {
                Method featureMethod = typeSetMethods.get(typeClasses.get(typeName)).get(methodName);
                featureMethod.invoke(annotation, value);
            } else {
                System.out.println(methodName + "doesn't exist in " + typeName);
            }
        }
        annotation.addToIndexes();
    }


}
