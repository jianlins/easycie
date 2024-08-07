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

package edu.utah.bmi.nlp.core;


import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSUtil;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
/**
 * A class that holds all the information needed to define a new Type from a parent Type.
 * Use to dynamically generate annotations
 *
 * @author Jianlin Shi on 5/1/17.
 */
public class TypeDefinition implements Comparable<TypeDefinition>, Cloneable {
    public String shortTypeName;
    public String shortSuperTypeName;
    public String fullSuperTypeName;
    public String fullTypeName;
    public LinkedHashMap<String, String> featureTypes = new LinkedHashMap<>();
    public LinkedHashMap<String, Object> featureDefaultValues = new LinkedHashMap<>();


    public TypeDefinition(String typeName, String superTypeName) {
        init(typeName, superTypeName, null);

    }

    public TypeDefinition(String typeName, String superTypeName, List<String> newFeatureNames) {
        init(typeName, superTypeName, newFeatureNames);
    }

    public TypeDefinition(String typeName, String superTypeName, Set<String> newFeatureNames) {
        init(typeName, superTypeName, newFeatureNames);
    }

    public TypeDefinition(List<String> definition) {
        if (definition.size() > 2) {
            init(definition.get(0), definition.get(1), definition.subList(2, definition.size()));
        } else {
            init(definition.get(0), definition.get(1), null);
        }
    }

    public TypeDefinition(String typeName, String superTypeName, LinkedHashMap<String, Object> newFeatureNames) {
        initWFeaturValuePairs(typeName, superTypeName, newFeatureNames);
    }


    public void initWFeaturValuePairs(String typeName, String superTypeName, LinkedHashMap<String, Object> newFeatureDefaultValues) {
        init(typeName, superTypeName, null);
        setFeatureDefaultValues(newFeatureDefaultValues);
    }

    public void init(String typeName, String superTypeName, Collection<String> newFeatureNames) {
        this.fullTypeName = DeterminantValueSet.checkNameSpace(typeName);
        this.shortTypeName = DeterminantValueSet.getShortName(typeName);
        if (superTypeName.trim().length() == 0 || superTypeName == null) {
            this.fullSuperTypeName = DeterminantValueSet.defaultNameSpace + "Concept";
            this.shortSuperTypeName = "Concept";
        } else {
            this.fullSuperTypeName = DeterminantValueSet.checkNameSpace(superTypeName);
            this.shortSuperTypeName = DeterminantValueSet.getShortName(superTypeName);
        }
        setNewFeatureNames(newFeatureNames);
    }

    public String getFullSuperTypeName() {
        return fullSuperTypeName;
    }


    public String getFullTypeName() {
        return fullTypeName;
    }


    public String getShortTypeName() {
        return shortTypeName;
    }

    public String getShortSuperTypeName() {
        return shortSuperTypeName;
    }

    public Set<String> getNewFeatureNames() {
        return featureDefaultValues.keySet();
    }

    public LinkedHashMap<String, Object> getFeatureValuePairs() {
        return featureDefaultValues;
    }

    public void setFeatureDefaultValues(LinkedHashMap<String, Object> featureDefaultValues) {
        this.featureDefaultValues = featureDefaultValues;
    }

    public void setNewFeatureNames(Collection<String> newFeatureNames) {
        if (newFeatureNames == null)
            return;
        for (String featureName : newFeatureNames) {
            if (featureName.indexOf(":")==-1)
                this.featureDefaultValues.put(featureName, "");
            else{
                String[]featureNameParts = featureName.split(":");
                featureName=featureNameParts[0];
                String featureType = featureNameParts[1];
                this.featureTypes.put(featureName,featureType);
                switch (featureType) {
                    case "int":
                        this.featureDefaultValues.put(featureName, 0);
                        break;
                    case "String":
                        this.featureDefaultValues.put(featureName, "");
                        break;
                    case "boolean":
                        this.featureDefaultValues.put(featureName, false);
                        break;
                    case "long":
                        this.featureDefaultValues.put(featureName, (long) 0);
                        break;
                    case "short":
                        this.featureDefaultValues.put(featureName, (short) 0);;
                        break;
                    case "double":
                        this.featureDefaultValues.put(featureName, (double) 0);
                        break;
                    case "float":
                        this.featureDefaultValues.put(featureName, (float) 0);
                        break;
                    default:
                        this.featureDefaultValues.put(featureName, null);
                        break;
                }
            }
        }
    }

    public void addFeatureName(String featureName) {
        this.featureDefaultValues.put(featureName, "");
    }

    public void addFeatureDefaultValue(String featureName, String defaultValue) {
        this.featureDefaultValues.put(featureName, defaultValue);
    }

    public void setFeatureType(String featureName, String typeName) {
        this.featureTypes.put(featureName, typeName);
    }

    public String getFeatureType(String featureName) {
        if (featureTypes.containsKey(featureName))
            return featureTypes.get(featureName);
        else
            return "";
    }

    @Override
    public int compareTo(TypeDefinition o) {
        if (o==null)
            return 0;
        if (!this.fullTypeName.equals(o.fullTypeName))
            return 1;
        if (!this.fullSuperTypeName.equals(o.fullSuperTypeName))
            return 1;
        Set<String> thisKeys = this.featureDefaultValues.keySet();
        thisKeys.removeAll(o.featureDefaultValues.keySet());
        if (thisKeys.size() > 0)
            return 1;
        return 0;
    }

    public TypeDefinition clone() {
        return new TypeDefinition(this.fullTypeName, this.fullSuperTypeName,
                (LinkedHashMap<String, Object>) this.featureDefaultValues.clone());
    }

    public String toString() {
        return this.fullTypeName;
    }
}
