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

package edu.utah.bmi.nlp.sql;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * @author Jianlin Shi on 6/15/17.
 */
public class ConfigReaderFactory {
	public static ConfigReader createConfigReader(File configFile) {
		if (FilenameUtils.getExtension(configFile.getName()).equals("xml"))
			return new XmlConfigReader(configFile);
		else
			return new JsonConfigReader(configFile);
	}

	public static ConfigReader createConfigReader(Reader inputReader, String typeName) {
		if (typeName.equalsIgnoreCase("xml"))
			return new XmlConfigReader(inputReader);
		else
			return new JsonConfigReader(inputReader);
	}

	public static ConfigReader createConfigReader(InputStream inputStream, String typeName) {
		if (typeName.equalsIgnoreCase("xml"))
			return new XmlConfigReader(new InputStreamReader(inputStream));
		else
			return new JsonConfigReader(new InputStreamReader(inputStream));
	}
}
