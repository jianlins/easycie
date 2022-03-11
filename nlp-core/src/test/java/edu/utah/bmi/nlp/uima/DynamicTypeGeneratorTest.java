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

package edu.utah.bmi.nlp.uima;

import edu.utah.bmi.nlp.compiler.MemoryClassLoader;
import edu.utah.bmi.nlp.compiler.MemoryJavaCompiler;
import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.type.system.Concept;
import edu.utah.bmi.nlp.type.system.Doc_Base;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;


/**
 * Created by u0876964 on 11/4/16.
 */
public class DynamicTypeGeneratorTest {
	@BeforeEach
	public void initLoader(){
		MemoryClassLoader.CURRENT_LOADER_NAME = DeterminantValueSet.randomString();
	}
	@AfterAll
	public static void resetLoader(){
		MemoryClassLoader.resetLoaderName();
	}

	@Test
	public void test() {

		DynamicTypeGenerator dynamicTypeGenerator = new DynamicTypeGenerator("desc/type/All_Types");
		dynamicTypeGenerator.setCompiledRootPath("target/classes");
		dynamicTypeGenerator.addConceptType("Test");
		dynamicTypeGenerator.reInitTypeSystem("target/generated-test-sources/customized.xml", "target/generated-test-sources/");

	}

	@Test
	public void test2(){
		DynamicTypeGenerator dynamicTypeGenerator = new DynamicTypeGenerator("desc/type/All_Types");
		dynamicTypeGenerator.setCompiledRootPath("target/generated-sources/");
		dynamicTypeGenerator.addConceptType("Test", Arrays.asList("Feature1", "Feature2"), "edu.utah.bmi.nlp.type.system.Concept");
		dynamicTypeGenerator.addConceptType("TestD", Arrays.asList("Feature1", "Feature2"), "edu.utah.bmi.nlp.type.system.Doc_Base");
		dynamicTypeGenerator.reInitTypeSystem("target/generated-test-sources/customized.xml", "target/generated-test-sources/");
		Class<? extends Annotation> cls = AnnotationOper.getTypeClass("Test", Concept.class);
		assert (Concept.class.isAssignableFrom(cls));
		cls = AnnotationOper.getTypeClass("TestD");
		assert (Doc_Base.class.isAssignableFrom(cls));
		System.out.println(cls.getSimpleName());
	}
}
