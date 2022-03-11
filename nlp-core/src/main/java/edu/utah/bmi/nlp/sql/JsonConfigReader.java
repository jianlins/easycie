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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Jianlin Shi
 *         Created on 12/11/16.
 */
public class JsonConfigReader implements ConfigReader {
	private JSONObject jsonObject;
	private LinkedHashMap<String, Object> configs;

	public JsonConfigReader(File jsonFile) {
		try {
			FileReader reader = new FileReader(jsonFile);
			init(reader);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (NullPointerException ex) {
			ex.printStackTrace();
		}
	}

	public JsonConfigReader(String str) {
		StringReader strReader = new StringReader(str);
		init(strReader);
	}

	public JsonConfigReader(Reader reader) {
		init(reader);
	}

	private void init(Reader reader) {
		JSONParser jsonParser = new JSONParser();
		try {
			jsonObject = (JSONObject) jsonParser.parse(reader);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		configs=parse(jsonObject);
	}


	public LinkedHashMap<String, Object> parse(Object configObject) {
		if (configObject instanceof JSONObject)
			return parse((JSONObject) configObject);
		System.err.println("Not a JSONObject to parse");
		return null;
	}

	private LinkedHashMap<String, Object> parse(JSONObject jsonObject) {
		LinkedHashMap<String, Object> configs = new LinkedHashMap<>();
		for (Object obj : jsonObject.entrySet()) {
			Map.Entry<String, Object> entry = (Map.Entry<String, Object>) obj;
//            System.out.println(entry.getKey());
//            System.out.println(entry.getValue());
			if (entry.getValue() instanceof JSONArray) {
				JSONArray values = (JSONArray) entry.getValue();
				Iterator i = values.iterator();
				LinkedHashMap<String, Object> parsedValues = new LinkedHashMap<>();
				ArrayList<String> parseStrings = new ArrayList<>();
				while (i.hasNext()) {
					Object innerObj = i.next();
					if (innerObj instanceof String) {
						parseStrings.add((String) innerObj);
					} else if (innerObj instanceof JSONObject) {
						JSONObject innerJsonObj = (JSONObject) innerObj;
						String key = "";
						if (innerJsonObj.containsKey("id")) {
							key = (String) innerJsonObj.get("id");
						} else if (innerJsonObj.containsKey("name")) {
							key = (String) innerJsonObj.get("name");
						} else {
							System.out.println("name or id is missing for Object: \n\t" + innerJsonObj.toJSONString());
						}
						parsedValues.put(key, parse(innerJsonObj));
					} else {
						System.out.println("unknown object: " + innerObj.getClass().getCanonicalName());
					}
				}
				if (parsedValues.size() > 0)
					configs.put(entry.getKey(), parsedValues);
				else
					configs.put(entry.getKey(), parseStrings);
			} else if (entry.getValue() instanceof String) {
				configs.put(entry.getKey(), entry.getValue());
			}
		}
		return configs;
	}

	public Object getValue(String keyString) {
		String[] keys = keyString.split("/");
		Object result = configs;
		for (String key : keys) {
			if (result instanceof LinkedHashMap)
				result = ((LinkedHashMap<String, Object>) result).get(key);
			else {
				System.out.println("no key of " + key + " found in keyString");
			}
		}
		return result;
	}
}
