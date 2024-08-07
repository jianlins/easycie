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

package edu.utah.bmi.nlp.uima.common;

import edu.utah.bmi.nlp.compiler.MemoryClassLoader;
import edu.utah.bmi.nlp.core.*;
import edu.utah.bmi.nlp.sql.RecordRow;
import org.apache.uima.cas.*;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.factory.AnnotationFactory;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.jcas.tcas.Annotation_Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

import static java.lang.Character.isUpperCase;

/**
 * @author Jianlin on 5/29/15.
 */
public class AnnotationOper {
    public static Logger logger = IOUtil.getLogger(AnnotationOper.class);


    public static IntervalST<Annotation> indexAnnotation(JCas jcas, int typeId) {
        FSIndex annoIndex = jcas.getAnnotationIndex(typeId);
        FSIterator annoIter = annoIndex.iterator();
        IntervalST<Annotation> index = new IntervalST<>();
        while (annoIter.hasNext()) {
            Annotation annotation = (Annotation) annoIter.next();
//           assume there is no overlapping annotations
            index.put(new Interval1D(annotation.getBegin(), annotation.getEnd()), annotation);
        }
        return index;
    }

    public static IntervalST<Integer> indexAnnotation(JCas jcas, int typeId, ArrayList<Annotation> annotations) {
        FSIndex annoIndex = jcas.getAnnotationIndex(typeId);
        FSIterator annoIter = annoIndex.iterator();
        IntervalST<Integer> index = new IntervalST<>();
        int i = 0;
        while (annoIter.hasNext()) {
            Annotation annotation = (Annotation) annoIter.next();
            annotations.add(annotation);
//           assume there is no overlapping annotations
            index.put(new Interval1D(annotation.getBegin(), annotation.getEnd()), i);
            i++;
        }
        return index;
    }

    public static IntervalST<Annotation> indexAnnotation(JCas jcas, Class<? extends Annotation> type) {
        Iterator<? extends Annotation> annotationIter = JCasUtil.iterator(jcas, type);
        IntervalST<Annotation> index = new IntervalST<>();
        while (annotationIter.hasNext()) {
            Annotation annotation = type.cast(annotationIter.next());
//           assume there is no overlapping annotations
            index.put(new Interval1D(annotation.getBegin(), annotation.getEnd()), annotation);
        }
        return index;
    }

    public static IntervalST<Integer> indexAnnotation(JCas jcas, Class<? extends Annotation> type, ArrayList<Annotation> annotations) {
        Iterator<? extends Annotation> annotationIter = JCasUtil.iterator(jcas, type);
        IntervalST<Integer> index = new IntervalST<>();
        int i = 0;
        while (annotationIter.hasNext()) {
            Annotation annotation = type.cast(annotationIter.next());
            annotations.add(annotation);
//           assume there is no overlapping annotations
            index.put(new Interval1D(annotation.getBegin(), annotation.getEnd()), i);
            i++;
        }
        return index;
    }

    /**
     * Construct three annotation_id - annotation_id maps and one annotation_id - annotation maps, which makes
     * it easier and faster to find related annotations.
     * All four Maps are constructed in one iteration.
     *
     * @param parents            the sorted sentence list
     * @param children           the sorted token list
     * @param parent2ChildrenMap sentence Id - token Ids Map (Id here refers to the index in the token list)
     */
    public static void buildAnnoMap(ArrayList<Annotation> parents, ArrayList<Annotation> children,
                                    TreeMap<Integer, TreeSet<Integer>> parent2ChildrenMap) {
        int parentPointer = 0;
        for (int i = 0; i < children.size(); i++) {
            Annotation thisChild = children.get(i);
//            System.out.println(sentencePointer);
            if (parentPointer >= parents.size())
                break;
            Annotation thisParent = parents.get(parentPointer);
//          if token is inside current sentence, add it to sentence2TokenMap
            if (thisChild.getBegin() >= thisParent.getBegin() && thisChild.getEnd() <= thisParent.getEnd()) {//
                addAnnoMapValue(parent2ChildrenMap, parentPointer, i);
//          if token goes beyond the sentence's end, add it to the next sentence.
            } else if (thisChild.getBegin() >= thisParent.getEnd()) {
                if (parentPointer < parents.size() - 1 && thisChild.getBegin() >= parents.get(parentPointer + 1).getBegin()) {
                    parentPointer++;
                    thisParent = parents.get(parentPointer);
                    addAnnoMapValue(parent2ChildrenMap, parentPointer, i);
                }
            }

        }
    }

    private static void addAnnoMapValue(TreeMap<Integer, TreeSet<Integer>> annoMap, int key, int value) {
        TreeSet<Integer> valueSet = new TreeSet<Integer>();
        if (annoMap.containsKey(key)) {
            valueSet = annoMap.get(key);
        }
        valueSet.add(value);
        annoMap.put(key, valueSet);
    }


    /**
     * Get Type System registered Id
     *
     * @param typeClass input type class
     * @return corresponding type id
     */
    public static int getTypeId(Class typeClass) {
        int id = 0;
        if (typeClass == null) {
            logger.warning(typeClass.getCanonicalName() + " has not been initiated. Please check the definition in rule files.");
            return id;
        }
        try {
            id = typeClass.getField("type").getInt(null);
        } catch (IllegalAccessException e) {
            logger.fine(e.toString());
        } catch (NoSuchFieldException e) {
            logger.fine(e.toString());
        }
        return id;
    }

    /**
     * Get Type System registered Id
     *
     * @param typeFullName input type name
     * @return corresponding type id
     */
    public static int getTypeId(String typeFullName) {
        Class typeClass = getTypeClass(typeFullName);
        return getTypeId(typeClass);
    }

    public static Class getClass(String typeFullName, Class superClass) {
        if (typeFullName.equals("uima.tcas.Annotation")) {
            return Annotation.class;
        }
        Class<? extends superClass> typeClass = null;
        try {
            typeClass = Class.forName(typeFullName).asSubclass(superClass);
            logger.finest(String.format("Load class %s from AppClassLoader", typeFullName));
        } catch (ClassNotFoundException e) {
            try {
                MemoryClassLoader loader = MemoryClassLoader.getInstance(MemoryClassLoader.CURRENT_LOADER_NAME);
                if (loader == null)
                    return null;
                Thread.currentThread().setContextClassLoader(loader);
                typeClass =
//                        Class.forName(typeFullName, true, ).asSubclass(superClass);
                        loader.load(typeFullName);
                logger.finest(String.format("Load class %s from MemoryClassLoader", typeFullName));
            } catch (ClassNotFoundException ex) {
                logger.finest(e.toString());
            }
        }
        return typeClass;
    }

    public static Class<? extends Annotation_Type> get_TypeClass(String typeFullName) {
        return getClass(typeFullName, Annotation_Type.class);
    }

    public static Class<? extends Annotation> getTypeClass(String typeName) {
        return getTypeClass(typeName, Annotation.class, true);
    }

    public static Class<? extends Annotation> getTypeClass(String typeFullName, boolean checkDomain) {
        return getTypeClass(typeFullName, Annotation.class, checkDomain);
    }

    public static Class<? extends Annotation> getTypeClass(String typeFullName, Class superClass) {
        return getTypeClass(typeFullName, superClass, true);
    }


    public static Class<? extends Annotation> getTypeClass(String typeFullName, Class superClass, boolean checkDomain) {
        return getClass(DeterminantValueSet.checkNameSpace(typeFullName), superClass);
    }

    public static Class getUIMAClass(String typeFullName) {
        if (typeFullName.endsWith("_Type")) {
            return get_TypeClass(typeFullName);
        } else {
            return getTypeClass(typeFullName);
        }
    }

    @Deprecated
    public static void setFeatureValue(Method method, Annotation annotation, String value) {
        try {
            method.invoke(annotation, value);
        } catch (IllegalAccessException e) {
            logger.fine(e.toString());
        } catch (InvocationTargetException e) {
            logger.fine(e.toString());
        }
    }

    public static void setFeatureValue(String featureName, Annotation annotation, String value) {
        FSUtil.setFeature(annotation, featureName, value);
    }

    public static void setFeatureObjValue(String featureName, Annotation annotation, Object value) {
        String valueType = value.getClass().getSimpleName();
        switch (valueType) {
            case "Integer":
                FSUtil.setFeature(annotation, featureName, (Integer) value);
                break;
            case "String":
                FSUtil.setFeature(annotation, featureName, (String) value);
                break;
            case "Boolean":
                FSUtil.setFeature(annotation, featureName, (Boolean) value);
                break;
            case "Double":
                FSUtil.setFeature(annotation, featureName, (Double) value);
                break;
            case "Float":
                FSUtil.setFeature(annotation, featureName, (Float) value);
                break;
            case "Collection":
                FSUtil.setFeature(annotation, featureName, (Collection) value);
                break;
            case "Long":
                FSUtil.setFeature(annotation, featureName, (Long) value);
                break;
            case "Short":
                FSUtil.setFeature(annotation, featureName, (Short) value);
                break;
            case "FeatureStructrue":
                FSUtil.setFeature(annotation, featureName, (FeatureStructure) value);
                break;
            case "Byte":
                FSUtil.setFeature(annotation, featureName, (Byte) value);
                break;
            default:
                logger.warning(String.format("%s is not a supported Type %s", value, valueType));
        }
    }

    @Deprecated
    public static void setFeatureObjValue(Method method, Annotation annotation, Object value) {
        try {
            method.invoke(annotation, value);
        } catch (IllegalAccessException e) {
            logger.fine(e.toString());
        } catch (InvocationTargetException e) {
            logger.fine(e.toString());
        }
    }

    // Base on Feature Name of the Type System, infer the Method Name of the corresponding java class
    public static String inferMethodName(String featureName, String prefix) {
        String methodName = "";
        if (featureName.trim().length() == 0) {
            methodName = "";
        } else if (featureName.startsWith(prefix)) {
            methodName = featureName;
        } else if (Character.isUpperCase(featureName.charAt(0))) {
            methodName = prefix + featureName;
        } else {
            methodName = prefix + Character.toUpperCase(featureName.charAt(0)) + featureName.substring(1);
        }
        return methodName;
    }

    public static String inferSetMethodName(String featureName) {
        return inferMethodName(featureName, "set");
    }

    public static String inferGetMethodName(String featureName) {
        return inferMethodName(featureName, "get");
    }

    @Deprecated
    public static Object getFeatureValue(Method method, Annotation annotation) {
        Object output = null;
        try {
            output = method.invoke(annotation);
        } catch (IllegalAccessException e) {
            logger.fine(e.toString());
        } catch (InvocationTargetException e) {
            logger.fine(e.toString());
        }
        return output;
    }

    public static Object getFeatureValue(String featureName, Annotation annotation) {
        Feature f = annotation.getType().getFeatureByBaseName(featureName);

        if(f==null){
            logger.info("Feature: "+featureName+" doesn't exists in annotation: "+annotation.getType());
            return null;
        }
        Object value = FSUtil.getFeature(annotation, f, Object.class);
//        FSUtil.getFeature()
        return value;
    }

    @Deprecated
    public static void getMethods(Class c, LinkedHashSet<Method> methods) {
//        System.out.println(c.getSimpleName());
        if (c.getSimpleName().equals("Annotation")) {
            return;
        }
        for (Method method : c.getDeclaredMethods()) {
            String methodName = method.getName();
            if (methodName.startsWith("get") && !methodName.equals("getTypeIndexID") && method.getParameterCount() == 0)
                methods.add(method);
        }
        getMethods(c.getSuperclass(), methods);
    }


    public static Method getMethod(Class cls, String methodName, Class<?>... paraTypes) {
        Method m = null;
        try {
            m = cls.getMethod(methodName, paraTypes);
        } catch (NoSuchMethodException e) {
        }
        return m;
    }

    /**
     * Get the "get" method of annotation class with no input parameter
     *
     * @param cls        annotation class
     * @param methodName method name, can w or w/o prefix "get"
     * @return the get method of given method name
     */
    public static Method getDefaultGetMethod(Class cls, String methodName) {
        methodName = inferGetMethodName(methodName);
        return getMethod(cls, methodName);
    }


    /**
     * Get the "set" method of annotation class with default parameter (same type as default "get" method returns)
     *
     * @param cls        annotation class
     * @param methodName method name, can w or w/o prefix "set"
     * @return the get method of given method name
     */
    public static Method getDefaultSetMethod(Class cls, String methodName) {
        methodName = inferSetMethodName(methodName);
        if (methodName.length() < 3) {
            logger.warning("Method name: '" + methodName + "' could be wrong");
            return null;
        }
        Method getMethod = getDefaultGetMethod(cls, methodName.substring(3));
        if (getMethod == null)
            return null;
        return getMethod(cls, methodName, getMethod.getReturnType());
    }

    public static void initGetReflections(HashMap<String, HashMap<String, Method>> evidenceConceptGetFeatures,
                                          String evidenceTypeShortName, Class evidenceTypeClass,
                                          String[] featureValueMatchRule, ArrayList<String> needEvidenceFeatures) {
        if (!evidenceConceptGetFeatures.containsKey(evidenceTypeShortName)) {
            evidenceConceptGetFeatures.put(evidenceTypeShortName, new HashMap<>());
        }
        HashMap<String, Method> methods = evidenceConceptGetFeatures.get(evidenceTypeShortName);
        for (int i = 0; i < featureValueMatchRule.length - 1; i += 2) {
            String featureName = featureValueMatchRule[i];
            if (featureName == null)
                System.out.println(Arrays.asList(featureValueMatchRule));
            if (featureName.indexOf(":") != -1) {
                featureName = featureName.split(":")[0].trim();
            }
            if (!methods.containsKey(featureName)) {
                Method m = getFeatureMethod(evidenceTypeClass, featureName);
                if (m != null)
                    methods.put(featureName, m);
            }
        }
        for (String featureName : needEvidenceFeatures) {
            if (featureName.indexOf(":") != -1) {
                featureName = featureName.split(":")[0].trim();
            }
            if (!methods.containsKey(featureName)) {
                Method m = getFeatureMethod(evidenceTypeClass, featureName);
                if (m != null)
                    methods.put(featureName, m);
                methods.put(featureName, m);
            }
        }
    }

    public static Method getFeatureMethod(Class evidenceTypeClass, String featureName) {
        Method m = null;
        try {
            m = evidenceTypeClass.getMethod(AnnotationOper.inferGetMethodName(featureName));
        } catch (Exception e) {
            logger.fine(e.toString());
        }
        return m;
    }

    public static void initSetReflections(LinkedHashMap<String, TypeDefinition> typeDefinitions,
                                          HashMap<String, Class<? extends Annotation>> conceptClassMap,
                                          HashMap<String, Constructor<? extends Annotation>> conceptTypeConstructors,
                                          HashMap<Class, HashMap<String, Method>> conclusionConceptSetFeatures) {
        for (TypeDefinition typeDefinition : typeDefinitions.values()) {
            try {
                String shortTypeName = typeDefinition.shortTypeName;
                Class<? extends Annotation> typeClass = getTypeClass(typeDefinition.fullTypeName);
                if (typeClass == null) {
                    logger.warning(typeDefinition.shortTypeName + " has not been initiated. Please check the definition in rule files.");
                    continue;
                }
                conceptClassMap.put(shortTypeName, typeClass);
                conceptTypeConstructors.put(shortTypeName, typeClass.getConstructor(JCas.class, int.class, int.class));
                if (!conclusionConceptSetFeatures.containsKey(shortTypeName)) {
                    conclusionConceptSetFeatures.put(typeClass, new HashMap<>());
                }
                for (String featureName : typeDefinition.getNewFeatureNames()) {
                    if (featureName.indexOf(":") != -1) {
                        featureName = featureName.split(":")[0].trim();
                    }
                    String setMethodName = AnnotationOper.inferSetMethodName(featureName);
                    logger.fine("try set: " + typeClass.getCanonicalName() + "\t" + setMethodName);
                    conclusionConceptSetFeatures.get(typeClass).put(featureName,
                            AnnotationOper.getDefaultSetMethod(typeClass, setMethodName));
                }
            } catch (NoSuchMethodException e) {
                logger.fine(e.toString() + "\n\t" + typeDefinition);
            }
        }
    }

    @Deprecated
    public static AnnotationDefinition createConclusionAnnotationDefinition(AnnotationDefinition conclusionDefinition,
                                                                            HashMap<String, HashMap<String, Method>> evidenceConceptGetFeatures,
                                                                            HashMap<String, String> uniqueFeatureClassMap,
                                                                            List<Annotation> evidenceAnnotations,
                                                                            LinkedHashMap<String, TypeDefinition> typeDefinitions) {
        AnnotationDefinition conclusionAnnotatoinDef = conclusionDefinition.clone();
        String resultTypeShortName = conclusionDefinition.getShortTypeName();
        LinkedHashMap<String, Annotation> sortedEvidenceAnnotations = new LinkedHashMap<>();
        for (Annotation anno : evidenceAnnotations) {
            sortedEvidenceAnnotations.put(anno.getClass().getSimpleName(), anno);
        }
        for (Map.Entry<String, Object> featureValueEntry : conclusionDefinition.getFeatureValuePairs().entrySet()) {
            String featureName = featureValueEntry.getKey();
//            TODO check if this is necessary
            if (featureName.indexOf(":") != -1)
                featureName = featureName.split(":")[0].trim();
            Object value = featureValueEntry.getValue();
            if (value instanceof String && ((String) value).length() == 0) {
                logger.fine("Conclusion type '" + resultTypeShortName + "' feature '" + featureName + "' value is empty.");
                value = null;
            }
            Annotation evidenceAnnotation;
            try {
                if (value == null) {
//              In the form of "FeatureName"
                    if (evidenceAnnotations.size() == 0) {
                        logger.warning("Evidence annotation for conclusion: '" + resultTypeShortName + "' feature " + featureName + " is needed");
                        continue;
                    }
                    evidenceAnnotation = evidenceAnnotations.get(0);
                    String evidenceClassName = evidenceAnnotation.getClass().getSimpleName();
                    Method featureMethod = null;
                    if (evidenceAnnotations.size() > 1) {
//                      if there are multiple evidence annotations, try to find the best match
                        if (!uniqueFeatureClassMap.containsKey(featureName)) {
//                          if the feature is not known to be unique, try to find if it is unique, otherwise, choose the 1st referenced class
                            for (String className : sortedEvidenceAnnotations.keySet()) {
                                if (evidenceConceptGetFeatures.get(className).containsKey(featureName)) {
                                    if (uniqueFeatureClassMap.containsKey(featureName)) {
                                        logger.warning("Feature " + featureName + " is not unique in current evidences: "
                                                + sortedEvidenceAnnotations.keySet() + ". Only the 1st referenced Class will be used: "
                                                + uniqueFeatureClassMap.get(featureName));
                                    } else {
                                        uniqueFeatureClassMap.put(featureName, className);
                                    }
                                }
                            }
                        }
                        evidenceClassName = uniqueFeatureClassMap.get(featureName);
                        evidenceAnnotation = sortedEvidenceAnnotations.get(evidenceClassName);
                    }
                    HashMap<String, Method> evidenceFeatureMap = null;
                    if (evidenceConceptGetFeatures.containsKey(evidenceClassName)) {
                        evidenceFeatureMap = evidenceConceptGetFeatures.get(evidenceClassName);
                    } else {
                        Class parentClass = evidenceAnnotation.getClass();
                        while (parentClass != null && parentClass != Annotation.class) {
                            parentClass = parentClass.getSuperclass();
                            if (evidenceConceptGetFeatures.containsKey(parentClass.getSimpleName())) {
                                evidenceFeatureMap = evidenceConceptGetFeatures.get(parentClass);
                                break;
                            }
                        }
                    }
                    if (evidenceFeatureMap == null) {
                        evidenceConceptGetFeatures.put(evidenceClassName, new HashMap<>());
                        evidenceFeatureMap = evidenceConceptGetFeatures.get(evidenceClassName);
                    }


                    if (evidenceFeatureMap.containsKey(featureName) && evidenceFeatureMap.get(featureName) != null) {
                        featureMethod = evidenceFeatureMap.get(featureName);
                    }
                    if (featureMethod == null) {
                        featureMethod = getDefaultGetMethod(evidenceAnnotation.getClass(), featureName);
                    }
                    if (featureMethod != null)
                        value = featureMethod.invoke(evidenceAnnotation);
                    else {
                        logger.fine(evidenceClassName + " doesn't have the feature: " + featureName);
                    }
                } else if (value instanceof String && isUpperCase(((String) value).charAt(0))) {
//              In form of "FeatureName:ConceptName" (in short form)
                    String evidenceClassName = (String) value;
                    evidenceAnnotation = sortedEvidenceAnnotations.get(evidenceClassName);
                    if (evidenceAnnotation != null) {
                        Method featureMethod = null;
                        HashMap<String, Method> evidenceFeatureMap = evidenceConceptGetFeatures.get(evidenceClassName);
                        if (evidenceFeatureMap.containsKey(featureName) && evidenceFeatureMap.get(featureName) != null) {
                            featureMethod = evidenceFeatureMap.get(featureName);
                        } else {
                            featureMethod = AnnotationOper.getDefaultGetMethod(evidenceAnnotation.getClass(), featureName);
                            evidenceFeatureMap.put(featureName, featureMethod);
                        }
                        value = featureMethod.invoke(evidenceAnnotation);
                    }
                }
//              In the form of "FeatureName:value", need to make sure the 1st char of value is not Capitalized
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            if (value != null)
                conclusionAnnotatoinDef.setFeatureValue(featureName, value);
            else if (typeDefinitions.containsKey(conclusionAnnotatoinDef.shortTypeName) && typeDefinitions.get(conclusionAnnotatoinDef.getShortTypeName()).getFeatureValuePairs().containsKey(featureName)) {
                conclusionAnnotatoinDef.setFeatureValue(featureName, typeDefinitions.get(conclusionAnnotatoinDef.getShortTypeName()).getFeatureValuePairs().get(featureName));
            }
        }
        return conclusionAnnotatoinDef;
    }

    /**
     * This is more preferred method than above: createConclusionAnnotationDefinition
     *
     * @param conclusionDefinition
     * @param evidenceConceptGetFeatures
     * @param uniqueFeatureClassMap
     * @param evidenceAnnotations
     * @return AnnotationDefinition
     */

    public static AnnotationDefinition createConclusionAnnotationDefinition(AnnotationDefinition conclusionDefinition,
                                                                            HashMap<String, List<String>> evidenceConceptGetFeatures,
                                                                            HashMap<String, String> uniqueFeatureClassMap,
                                                                            List<Annotation> evidenceAnnotations) {
        AnnotationDefinition conclusionAnnotatoinDef = conclusionDefinition.clone();
        String resultTypeShortName = conclusionDefinition.getShortTypeName();
        LinkedHashMap<String, Annotation> sortedEvidenceAnnotations = new LinkedHashMap<>();
        for (Annotation anno : evidenceAnnotations) {
            sortedEvidenceAnnotations.put(anno.getClass().getSimpleName(), anno);
        }
        for (Map.Entry<String, Object> featureValueEntry : conclusionDefinition.getFeatureValuePairs().entrySet()) {
            String featureName = featureValueEntry.getKey();
//            TODO check if this is necessary
            if (featureName.indexOf(":") != -1)
                featureName = featureName.split(":")[0].trim();
            Object value = featureValueEntry.getValue();
            if (value instanceof String && ((String) value).length() == 0) {
                logger.info("Conclusion type '" + resultTypeShortName + "' feature '" + featureName + "' value is empty.");
                value = null;
            }
            Annotation evidenceAnnotation;
            if (value == null) {
//              In the form of "FeatureName"
                if (evidenceAnnotations.size() == 0) {
                    logger.warning("Evidence annotation for conclusion: '" + resultTypeShortName + "' feature " + featureName + " is needed");
                    continue;
                }
                evidenceAnnotation = evidenceAnnotations.get(0);
                String evidenceClassName = evidenceAnnotation.getClass().getSimpleName();
                if (evidenceAnnotations.size() > 1) {
//                      if there are multiple evidence annotations, try to find the best match
                    if (!uniqueFeatureClassMap.containsKey(featureName)) {
//                          if the feature is not known to be unique, try to find if it is unique, otherwise, choose the 1st referenced class
                        for (String className : sortedEvidenceAnnotations.keySet()) {
                            if (evidenceConceptGetFeatures.get(className).contains(featureName)) {
                                if (uniqueFeatureClassMap.containsKey(featureName)) {
                                    logger.warning("Feature " + featureName + " is not unique in current evidences: "
                                            + sortedEvidenceAnnotations.keySet() + ". Only the 1st referenced Class will be used: "
                                            + uniqueFeatureClassMap.get(featureName));
                                } else {
                                    uniqueFeatureClassMap.put(featureName, className);
                                }
                            }
                        }
                    }
                    evidenceClassName = uniqueFeatureClassMap.get(featureName);
                    evidenceAnnotation = sortedEvidenceAnnotations.get(evidenceClassName);
                }
                List<String> evidenceFeatures = new ArrayList<>();
                if (evidenceConceptGetFeatures.containsKey(evidenceClassName)) {
                    evidenceFeatures = evidenceConceptGetFeatures.get(evidenceClassName);
                } else {
                    Class parentClass = evidenceAnnotation.getClass();
                    while (parentClass != null && parentClass != Annotation.class) {
                        parentClass = parentClass.getSuperclass();
                        if (evidenceConceptGetFeatures.containsKey(parentClass.getSimpleName())) {
                            evidenceFeatures = evidenceConceptGetFeatures.get(parentClass);
                            break;
                        }
                    }
                }
                if (evidenceFeatures == null) {
                    evidenceConceptGetFeatures.put(evidenceClassName, new ArrayList<>());
                    evidenceFeatures = evidenceConceptGetFeatures.get(evidenceClassName);
                }
                if (evidenceFeatures.contains(featureName)) {
                    value=AnnotationOper.getFeatureValue(featureName, evidenceAnnotation);
                }else{
                    logger.fine(evidenceClassName + " doesn't have the feature: " + featureName);
                }
            } else if (value instanceof String && isUpperCase(((String) value).charAt(0))) {
//              In form of "FeatureName:ConceptName" (in short form)
                String evidenceClassName = (String) value;
                evidenceAnnotation = sortedEvidenceAnnotations.get(evidenceClassName);
                if (evidenceAnnotation != null) {
                    List<String> evidenceFeatures = evidenceConceptGetFeatures.get(evidenceClassName);
                    if (evidenceFeatures.contains(featureName)) {
                        value=AnnotationOper.getFeatureValue(featureName, evidenceAnnotation);
                    }else{
                        value=null;
                        logger.fine(evidenceClassName + " doesn't have the feature: " + featureName);
                    }
                }
            }
//              In the form of "FeatureName:value", need to make sure the 1st char of value is not Capitalized
            conclusionAnnotatoinDef.setFeatureValue(featureName, value);
        }
        return conclusionAnnotatoinDef;
    }

    public static Annotation createAnnotation(JCas jcas, AnnotationDefinition conclusionAnnotationDefinition,
                                              Class<? extends Annotation> annoClass, int begin, int end) {
        Annotation anno = AnnotationFactory.createAnnotation(jcas, begin, end, annoClass);
        for (Map.Entry<String, Object> featureValueEntry : conclusionAnnotationDefinition.getFullFeatureValuePairs().entrySet()) {
            String featureName = featureValueEntry.getKey();
            if (featureName.indexOf(":") != -1)
                featureName = featureName.split(":")[0].trim();
            Object value = featureValueEntry.getValue();
            if(value==null){
                continue;
            }
            String type = value.getClass().getSimpleName();
            try {
                switch (type) {
                    case "int":
                        FSUtil.setFeature(anno, featureName, (int) value);
                        break;
                    case "String":
                        FSUtil.setFeature(anno, featureName, (String) value);
                        break;
                    case "boolean":
                        FSUtil.setFeature(anno, featureName, (boolean) value);
                        break;
                    case "long":
                        FSUtil.setFeature(anno, featureName, (long) value);
                        break;
                    case "short":
                        FSUtil.setFeature(anno, featureName, (short) value);
                        break;
                    case "double":
                        FSUtil.setFeature(anno, featureName, (double) value);
                        break;
                    case "float":
                        FSUtil.setFeature(anno, featureName, (float) value);
                        break;
                    case "FeatureStructure":
                        FSUtil.setFeature(anno, featureName, (FeatureStructure) value);
                        break;
                    case "Collection":
                        FSUtil.setFeature(anno, featureName, (Collection) value);
                        break;
                    case "byte":
                        FSUtil.setFeature(anno, featureName, (byte) value);
                        break;

                }
            }catch (Exception e){
                logger.info(e.toString());
            }
        }
        return anno;
    }

//    @Deprecated
//    public static Annotation createAnnotation(JCas jcas, AnnotationDefinition conclusionAnnotationDefinition,
//                                              Constructor<? extends Annotation> annoConstructor, int begin, int end,
//                                              HashMap<String, Method> conclusionSetFeatureMethods) {
//        Annotation anno = null;
//        try {
//            anno = annoConstructor.newInstance(jcas, begin, end);
//        } catch (InstantiationException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
//        for (Map.Entry<String, Object> featureValueEntry : conclusionAnnotationDefinition.getFullFeatureValuePairs().entrySet()) {
//            String featureName = featureValueEntry.getKey();
//            if (featureName.indexOf(":") != -1)
//                featureName = featureName.split(":")[0].trim();
//            Object value = featureValueEntry.getValue();
//            Method setMethod = conclusionSetFeatureMethods.get(featureName);
//            try {
//                if (setMethod == null) {
//                    logger.fine(anno + " doesn't initiate the set method for: " + featureName);
//                    setMethod = getDefaultSetMethod(anno.getClass(), featureName);
//                    if (setMethod == null)
//                        logger.info("the type: " + anno.getType() + "'s method:\t'" + featureName + "' has not be set up in rules.\t" +
//                                "\n Fail to set the value: '" + value + "' for this method.");
//                    else
//                        conclusionSetFeatureMethods.put(featureName, setMethod);
//                }
//                setMethod.invoke(anno, value);
//            } catch (Exception e) {
//                logger.warning("When trying to set value: '" + value + "' to the method:" + featureName + "' of annotation: '" + anno.getType().getShortName() + "', throw error: ");
//                e.printStackTrace();
//            }
//
//        }
//        return anno;
//    }

    private static LinkedHashSet<Method> retrieveMethods(Class<? extends Annotation> aClass,
                                                         LinkedHashMap<Class, LinkedHashSet<Method>> typeMethods) {
        if (typeMethods.containsKey(aClass))
            return typeMethods.get(aClass);
        else {
            Class superClass = aClass.getSuperclass();
            while (!typeMethods.containsKey(superClass)) {
                if (superClass.equals(Annotation.class))
                    return null;
            }
            typeMethods.put(aClass, typeMethods.get(superClass));
            return typeMethods.get(superClass);
        }
    }

    public static RecordRow deserializeDocSrcInfor(JCas jCas) {
        RecordRow recordRow = new RecordRow();
        FSIterator it = jCas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
        if (it.hasNext()) {
            SourceDocumentInformation e = (SourceDocumentInformation) it.next();
            String serializedString = e.getUri();
            recordRow.deserialize(serializedString);
        }
        return recordRow;
    }

    public static boolean classLoaded(String className) {
        return getTypeClass(className) != null;
    }
}
