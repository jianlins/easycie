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

import java.util.LinkedHashMap;

/**
 * Use to dynamically generate annotations
 *
 * @author Jianlin Shi on 6/10/17.
 */
public class AnnotationDefinition extends TypeDefinition {

    //    in order to be compatible to old versions (only support string values) use this new field to handle generic values
    public LinkedHashMap<String, Object> featureValues = new LinkedHashMap<>();

    public AnnotationDefinition(String typeName, String superTypeName) {
        super(typeName, superTypeName);
    }

    public AnnotationDefinition(String typeName, String superTypeName, LinkedHashMap<String, Object> newFeatureValues) {
        super(typeName, superTypeName);
        this.featureValues = newFeatureValues;
        updateLegacyFeatureValues(newFeatureValues);
    }

    public AnnotationDefinition(TypeDefinition typeDefinition) {
        super(typeDefinition.fullTypeName, typeDefinition.fullSuperTypeName, (LinkedHashMap<String, Object>) typeDefinition.getFeatureValuePairs().clone());
        for (String featureName : featureDefaultValues.keySet()) {
            this.featureValues.put(featureName, featureDefaultValues.get(featureName));
        }
    }

    private void updateLegacyFeatureValues(LinkedHashMap<String, Object> newFeatureValues) {
        for (String featureName : newFeatureValues.keySet()) {
            Object value = newFeatureValues.get(featureName);
            if (value instanceof String) {
                this.featureDefaultValues.put(featureName, (String) value);
            }
        }
    }

    public LinkedHashMap<String, Object> getFullFeatureValuePairs() {
        return featureValues;
    }

    public void setFeatureValues(LinkedHashMap<String, Object> featureValues) {
        this.featureValues = featureValues;
        updateLegacyFeatureValues(featureValues);
    }

    public void setFeatureValue(String feature, Object value) {
        this.featureValues.put(feature, value);
        if (value instanceof String)
            this.featureDefaultValues.put(feature, (String) value);
    }

    public Object getFeatureValue(String feature) {
        return this.featureValues.getOrDefault(feature, null);
    }

    public AnnotationDefinition clone() {
        return new AnnotationDefinition(this.fullTypeName, this.fullSuperTypeName,
                (LinkedHashMap<String, Object>) this.featureValues.clone());
    }
}
