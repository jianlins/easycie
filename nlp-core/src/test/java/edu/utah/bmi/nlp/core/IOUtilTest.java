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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * @author Jianlin Shi on 5/9/17.
 */
public class IOUtilTest {
	@Deprecated
	public void getCells() throws Exception {
		IOUtil ioUtil=new IOUtil();
		ArrayList<ArrayList<String>> list = ioUtil.getCellsInList("@processer:\tfeatureinferencer\n" +
				"@splitter:\t\n" +
				"@casesensitive:\ttrue\n" +
				"#keep this comment for documentation purpose\n\n" +
				"@NegatedConcept\tConcept\tCertainty:certain\tTemporality:present\n" +
				"NegatedConcept\tCertainty:$Certainty,Temporality:Temporality\tConcept\tNegation:negated");
		System.out.println(list);
		System.out.println(ioUtil.settings.values());
		FileUtils.readFileToByteArray(new File(""));
	}

	@Test
	public void getCells2() throws Exception {

		String str="@processer:\tfeatureinferencer\n" +
				"@splitter:\t\n" +
				"@casesensitive:\ttrue\n" +
				"#keep this comment for documentation purpose\n\n" +
				"@NegatedConcept\tConcept\tCertainty:certain\tTemporality:present\n" +
				"NegatedConcept\tCertainty:$Certainty,Temporality:Temporality\tConcept\tNegation:negated";
		IOUtil ioUtil=new IOUtil(str);
		System.out.println(ioUtil.settings.values());
		System.out.println(ioUtil.initiations);
		System.out.println(ioUtil.ruleCells);
	}


	@Test
	public void testLogger(){
		Logger logger=IOUtil.getLogger(IOUtilTest.class);
		logger.severe("test severe");
		logger.warning("test warning");
		logger.info("tes info");
		logger.fine("test fine");
		logger.finer("test finer");
		logger.finest("test finest");
	}

	@Test
	@Disabled
	public void testExcel(){
		IOUtil ioUtil=new IOUtil("conf/ctakes_integrate_translatorInf2.xlsx");
	}

}