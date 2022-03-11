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

import edu.utah.bmi.nlp.sql.DAO;
import edu.utah.bmi.nlp.sql.JsonConfigReader;
import edu.utah.bmi.nlp.sql.XmlConfigReader;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.json.XML;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * @author Jianlin Shi on 6/14/17.
 */
public class TestJsonXML {
	@Test
	public void json2xml() throws IOException {
		JSONObject json = new JSONObject(FileUtils.readFileToString(new File("conf/edw.json")));
		String xml = "<xml>" + XML.toString(json) + "</xml>";
		FileUtils.writeStringToFile(new File("conf/test2.xml"), xml);

	}

	@Test
	public void xml2json() throws IOException {

		JSONObject xmlJSONObj = XML.toJSONObject(FileUtils.readFileToString(new File("conf/edw.xml")));

		String jsonPrettyPrintString = xmlJSONObj.toString(4);
		FileUtils.writeStringToFile(new File("conf/test.json"), jsonPrettyPrintString);

	}

	@Disabled
	@Test
	public void testDAO2() {
		DAO dao = new DAO(new File("conf/edw.xml"));
		System.out.println(dao.databaseName);
	}

	@Test
	public void testDom() throws IOException, SAXException, ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new File("conf/edw.xml"));
		NodeList nodes = doc.getChildNodes();
		printNode(doc.getElementsByTagName("config").item(0), 0);

	}

	private void printNode(Node node, int offset) {
		StringBuilder pad = new StringBuilder();
		for (int i = 0; i <= offset; i++) {
			pad.append("\t");
		}
		System.out.println(pad.toString() + node.getNodeName() + "\t=\t" + node.getNodeValue());
		if (node.hasChildNodes()) {
			NodeList nodes = node.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				printNode(nodes.item(i), offset + 1);
			}
		}
	}

	@Test
	public void testXmlConfigReader() {
		XmlConfigReader xmlConfigReader = new XmlConfigReader(new File("conf/edw.xml"));
		System.out.println(xmlConfigReader.getValue("username"));
		System.out.println(xmlConfigReader.getValue("queryStatements/getColumnsInfo/sql"));
	}

	@Test
	public void testJsonConfigReader() {
		JsonConfigReader xmlConfigReader = new JsonConfigReader(new File("conf/edw.json"));
		System.out.println(xmlConfigReader.getValue("username"));
		System.out.println(xmlConfigReader.getValue("queryStatements/getColumnsInfo/sql"));
	}
}
