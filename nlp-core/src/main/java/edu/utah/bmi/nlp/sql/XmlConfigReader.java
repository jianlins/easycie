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

import org.apache.commons.io.input.ReaderInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * XmlConfigReader is a class that implements the ConfigReader interface. It reads XML configuration files and provides methods to retrieve configuration values.
 */
public class XmlConfigReader implements ConfigReader {
	private LinkedHashMap<String, Object> configs;
	private Document doc;

	public XmlConfigReader(File jsonFile) {
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

	public XmlConfigReader(String str) {
		StringReader strReader = new StringReader(str);
		init(strReader);
	}

	public XmlConfigReader(Reader reader) {
		init(reader);
	}

	private void init(Reader reader) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
			doc = db.parse(new ReaderInputStream(reader));
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		parse(doc);

	}


	public LinkedHashMap<String, Object> parse(Object doc) {
		if (doc instanceof Document) {
			Node config = ((Document) doc).getElementsByTagName("config").item(0);
			configs = new LinkedHashMap<>();
			parse(config.getChildNodes(), "", configs);
		} else {
			System.err.println("Not a XML Document to parse");
		}
		return configs;
	}


	/**
	 * Parses a NodeList recursively and populates a configuration map.
	 *
	 * @param children the NodeList to parse
	 * @param mapKey the key to use in the configuration map
	 * @param configs the configuration map to populate
	 */
	private void parse(NodeList children, String mapKey, Object configs) {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeName().equals("#text") && child.getNodeValue().trim().length() == 0)
				continue;
			parse(child, mapKey, configs);
		}
	}

	private Object parse(Node node, String mapKey, Object configs) {
		String name = node.getNodeName();
		String value = node.getNodeValue();
		if (value != null)
			value = value.trim();
		if (name.equals("#text") && value != null && value.length() > 0) {
			LinkedHashMap<String, Object> configMap = (LinkedHashMap<String, Object>) configs;
			configMap.put(mapKey, value);
		} else if (name.equals("item")) {
			LinkedHashMap<String, Object> configMap = (LinkedHashMap<String, Object>) configs;
			LinkedHashMap<String, Object> childConfig = new LinkedHashMap<>();
			parse(node.getChildNodes(), "", childConfig);
			String key = "";
			if (configMap.get(mapKey) == null) {
				if (childConfig.containsKey("name")) {
					key = (String) childConfig.get("name");
				} else if (childConfig.containsKey("id")) {
					key = (String) childConfig.get("id");
				}
				if (key.length() > 0) {
					configMap.put(mapKey, new LinkedHashMap<String, Object>());

				} else {
					configMap.put(mapKey, new ArrayList<String>());
				}
			}
			Object child = configMap.get(mapKey);
			if (child instanceof LinkedHashMap) {
				key = (String) childConfig.get("name");
				((LinkedHashMap<String, Object>) child).put(key, childConfig);
			} else
				((ArrayList<String>) child).add((String) childConfig.values().iterator().next());
		} else if (!name.equals("#text") && value == null) {
			((LinkedHashMap<String, Object>) configs).put(name, null);
			parse(node.getChildNodes(), name, configs);
		}
		return configs;
	}

	/**
	 * Retrieves the value associated with the given key string from the configuration map.
	 *
	 * @param keyString the key string with the format "key1/key2/key3"
	 * @return the value associated with the key string, or null if the key is not found
	 */
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
