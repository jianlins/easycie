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

import edu.utah.bmi.nlp.core.DeterminantValueSet.Determinants;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static edu.utah.bmi.nlp.core.DeterminantValueSet.checkNameSpace;
import static edu.utah.bmi.nlp.core.DeterminantValueSet.getShortName;

/**
 * @author Jianlin Shi on 5/9/17.
 */
public class IOUtil {
    public ArrayList<ArrayList<String>> ruleCells;
    public ArrayList<ArrayList<String>> initiations;
    public char splitter;

    protected StringBuilder concatenatedRuleStr = new StringBuilder();
    public LinkedHashMap<String, String> settings;

    public IOUtil() {
    }

    public IOUtil(String ruleStr, boolean includeLineNumber) {
        readCells(ruleStr, includeLineNumber);
    }

    public IOUtil(String ruleStr) {
        readCells(ruleStr, true);
    }

    public ArrayList<ArrayList<String>> getRuleCells() {
        return ruleCells;
    }

    public ArrayList<ArrayList<String>> getInitiations() {
        return initiations;
    }

    public LinkedHashMap<String, String> getSettings() {
        return settings;
    }

    public String getSetting(String settingName) {
        return settings.getOrDefault(settingName, "");
    }

    public void readCells(String ruleStr, boolean includeLineNumber) {
        ruleCells = new ArrayList<>();
        settings = new LinkedHashMap<>();
        initiations = new ArrayList<>();
        String extensionName = ruleStr.substring(ruleStr.length() - 5).toLowerCase();
        try {
            if (extensionName.endsWith(".xlsx")) {
                readExcel(ruleStr, includeLineNumber);
            } else {
                readCSV(ruleStr, extensionName, includeLineNumber);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readCSV(String ruleStr, String extensionName, boolean includeLineNumber) throws IOException {
        int lineNumber = 0;
        Iterable<CSVRecord> recordsIterator = null;
        Iterator<String> lineIterator;
        if (extensionName.endsWith(".csv")) {
            settings.put("splitter", ",");
            lineIterator = FileUtils.lineIterator(new File(ruleStr));
        } else if (extensionName.endsWith(".tsv") || extensionName.endsWith(".txt")) {
            settings.put("splitter", "\t");
            lineIterator = FileUtils.lineIterator(new File(ruleStr));
        } else {
            if (ruleStr.indexOf("@splitter") == -1 && ruleStr.indexOf("&splitter") == -1)
                settings.put("splitter", "\t");
            lineIterator = IOUtils.lineIterator(new StringReader(ruleStr));
        }
        while (lineIterator.hasNext()) {
            lineNumber++;
            String line = lineIterator.next();
            if ((line.startsWith("@") || line.startsWith("&")) && Character.isLowerCase(line.charAt(1))) {
                String[] settingValue = line.substring(1).split(":");
                String value = "";
                if (settingValue.length > 1)
                    value = settingValue[1];
                if (value.length() > 1)
                    value = value.trim();
//              accommodate editors' auto trim behavior that trims the tab character
                if (!value.equals("") || !settingValue[0].trim().equals("splitter"))
                    settings.put(settingValue[0].trim(), value);
                else
                    settings.put("splitter", "\t");
            } else {
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#") && !line.startsWith("\"#")) {
                    recordsIterator = CSVParser.parse(line, CSVFormat.newFormat(settings.get("splitter").charAt(0)));
                    CSVRecord row = recordsIterator.iterator().next();
                    ArrayList<String> cellsInRow = new ArrayList<>();
                    if (includeLineNumber)
                        cellsInRow.add(lineNumber + "");
                    for (String cell : row) {
                        cellsInRow.add(cell);
                    }
                    if (includeLineNumber) {
                        saveCellsInRow(cellsInRow, 1, lineNumber);
                    } else {
                        saveCellsInRow(cellsInRow, 0, lineNumber);
                    }
                }
            }
        }
    }


    private void readExcel(String fileName, boolean includeLineNumber) throws IOException {
        int lineNumber = 0;
        FileInputStream inputStream = new FileInputStream(new File(fileName));
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet firstSheet = workbook.getSheetAt(0);
        Iterator<Row> iterator = firstSheet.iterator();
        while (iterator.hasNext()) {
            lineNumber++;
            Row nextRow = iterator.next();
            ArrayList<String> cellsInRow = new ArrayList<>();
            if (includeLineNumber)
                cellsInRow.add(lineNumber + "");
            int blankCellPos = -1;
            int lastCellId = nextRow.getLastCellNum();
            for (int i = 0; i < lastCellId; i++) {
                Cell cell = nextRow.getCell(i);
                String value = "";
                if (cell != null) {
                    cell.setCellType(CellType.STRING);
                    value = cell.getStringCellValue();
                }
                if (value.startsWith("#"))
                    break;
                if (value.trim().length() == 0) {
                    if (blankCellPos == -1) {
                        blankCellPos = cellsInRow.size();
                    }
                } else if (blankCellPos > -1) {
                    blankCellPos = -1;
                }
                cellsInRow.add(value);
            }
            if (blankCellPos > -1 && cellsInRow.size() > blankCellPos) {
                for (int j = cellsInRow.size() - 1; j >= blankCellPos; j--) {
                    cellsInRow.remove(j);
                }
            }
            if (includeLineNumber) {
                if (cellsInRow.size() > 1)
                    saveCellsInRow(cellsInRow, 1, lineNumber);
            } else {
                if (cellsInRow.size() > 0)
                    saveCellsInRow(cellsInRow, 0, lineNumber);
            }
        }
    }

    public String getConcatenatedRuleStr() {
        return concatenatedRuleStr.toString();
    }

    private void saveCellsInRow(ArrayList<String> cellsInRow, int offset, int lineNumber) {
        if (cellsInRow.size() == offset)
            return;
        if (cellsInRow.get(offset).startsWith("@") || cellsInRow.get(offset).startsWith("&")) {

            if (Character.isLowerCase(cellsInRow.get(offset).charAt(1))) {
                String[] settingValue = cellsInRow.get(offset).substring(1).split(":");
                String value = "";
                if (settingValue.length > 1)
                    value = settingValue[1];
                if (value.length() > 1)
                    value = value.trim();
                settings.put(settingValue[0].trim(), value);
            } else
                initiations.add(cellsInRow);
        } else {
//            is a rule row
            concatenatedRuleStr.append(cellsInRow.get(offset)).append("\n");
            ruleCells.add(cellsInRow);
        }
    }


    @Deprecated
    public ArrayList<ArrayList<String>> getCellsInList(String ruleStr) {
        return getCellsInList(ruleStr, 3, true);
    }

    @Deprecated
    public ArrayList<ArrayList<String>> getCellsInList(String ruleStr, int firstNLineAsSetting, boolean includeLineNumber) {
        ArrayList<ArrayList<String>> cells = new ArrayList<>();
        getCellsInList(ruleStr, 3, true, cells);
        return cells;
    }

    @Deprecated
    public void getCellsInList(String ruleStr, int firstNLineAsSetting, boolean includeLineNumber, ArrayList<ArrayList<String>> cells) {
        int lineNumber = firstNLineAsSetting;
        for (CSVRecord csvRecord : getCells(ruleStr, firstNLineAsSetting)) {
            ArrayList<String> cellinRow = new ArrayList<>();
            lineNumber++;
            if (csvRecord.get(0).startsWith("#") || csvRecord.get(0).startsWith("\"#") || csvRecord.get(0).trim().length() == 0)
                continue;
            if (includeLineNumber)
                cellinRow.add(lineNumber + "");
            for (String cell : csvRecord) {
                cellinRow.add(cell);
            }
            cells.add(cellinRow);
        }
    }

    @Deprecated
    public Iterable<CSVRecord> getCells(String ruleStr, int firstNLineAsSetting) {
        settings = new LinkedHashMap<>();
        Iterable<CSVRecord> recordsIterator = null;
        String extensionName = ruleStr.substring(ruleStr.length() - 5).toLowerCase();
        if (extensionName.endsWith(".csv") || extensionName.endsWith(".tsv")) {
            try {
                ruleStr = FileUtils.readFileToString(new File(ruleStr));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int begin = readFirstNLines(ruleStr, firstNLineAsSetting, settings);
//        System.out.println(ruleStr.substring(0, begin));
        String splitterStr = settings.get("splitter:");
        splitter = splitterStr == null || splitterStr.length() == 0 ? '\t' : splitterStr.charAt(0);
        try {
            recordsIterator = CSVParser.parse(ruleStr.substring(begin), CSVFormat.newFormat(splitter));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return recordsIterator;
    }

    @Deprecated
    private int readFirstNLines(String ruleStr, int n, LinkedHashMap<String, String> settings) {
        int lineNumber = 0, i;
        char[] ruleChars = ruleStr.toCharArray();
        StringBuilder tmp = new StringBuilder();
        String key = "";
        boolean startRule = false;
        boolean tabed = false;
        for (i = 0; i < ruleChars.length && lineNumber < n; i++) {
            char thisChar = ruleChars[i];
            switch (thisChar) {
                case '\n':
                    if (key.length() > 0) {
                        settings.put(key, tmp.toString());
                        key = "";
                        tmp.setLength(0);
                        startRule = false;
                        tabed = false;
                        lineNumber++;
                    } else if (!tabed && tmp.length() > 0) {
                        key = tmp.toString().trim();
                        settings.put(key, "\t");
                        tmp.setLength(0);
                        startRule = false;
                        lineNumber++;
                    }
                    break;
                case '\r':
                    break;
                case '\t':
                    if (tmp.length() > 0) {
                        key = tmp.toString().trim();
                        tmp.setLength(0);
                    }
                    tabed = true;
                    break;
                case '@':
                case '&':
                    if (!startRule) {
                        startRule = true;
                    }
                    break;
                case '#':
                    break;
                default:
                    if (startRule)
                        tmp.append(thisChar);

            }

        }
        return i;
    }

    @Deprecated
    public static HashMap<Integer, Rule> parseRuleStr(String ruleStr, String splitter, boolean caseSensitive) {
        HashMap<Integer, Rule> rules = new HashMap<>();
        int strLength = ruleStr.trim().length();
        String testFileStr = ruleStr.trim().substring(strLength - 4).toLowerCase();
        boolean[] thisRuleType = new boolean[]{false, false, false};
        LinkedHashMap<String, TypeDefinition> typeDefinition = new LinkedHashMap<>();
        if (testFileStr.equals(".tsv") || testFileStr.equals(".csv") || testFileStr.equals("xlsx") || testFileStr.equals(".owl")) {
            thisRuleType = IOUtil.readAgnosticFile(ruleStr, rules, typeDefinition, caseSensitive);
        } else {
            thisRuleType = IOUtil.readCSVString(ruleStr, rules, typeDefinition, splitter, caseSensitive, thisRuleType);
        }
        return rules;
    }

    @Deprecated
    public static boolean[] readAgnosticFile(String agnosticFileName, HashMap<Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, boolean caseSensitive) {
        boolean[] thisRuleType = new boolean[]{false, false, false, false};
        readAgnosticFile(agnosticFileName, rules, typeDefinition, caseSensitive, thisRuleType);
        return thisRuleType;
    }

    @Deprecated
    public static String readAgnosticString(String inputStr) {
        File agnosticFile = new File(inputStr);
        if (agnosticFile.exists()) {
            String extension = FilenameUtils.getExtension(inputStr).toLowerCase();
            try {
                switch (extension) {
                    case "csv":
                        inputStr = FileUtils.readFileToString(agnosticFile);
                        break;
                    case "tsv":
                        inputStr = FileUtils.readFileToString(agnosticFile);
                        break;
                    case "txt":
                        inputStr = FileUtils.readFileToString(agnosticFile);
                        break;
                    default:
                        System.err.println("File format not supported: " + agnosticFile.getAbsolutePath());
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return inputStr;
    }

    @Deprecated
    public static boolean[] readAgnosticFile(String agnosticFileName, HashMap<Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, boolean caseSensitive, boolean[] thisRuleType) {
        File agnosticFile = new File(agnosticFileName);

        if (agnosticFile.exists()) {
//			if (agnosticFile.isDirectory()) {
//				thisRuleType = readOwlDirectory(agnosticFileName, rules, caseSensitive);
//			} else if (FilenameUtils.getExtension(agnosticFileName).equals("owl")) {
//				thisRuleType = readOwlFile(agnosticFileName, rules, typeDefinition, caseSensitive, thisRuleType);
//			} else
//				if (FilenameUtils.getExtension(agnosticFileName).equals("xlsx")) {
//				thisRuleType = readXLSXRuleFile(agnosticFileName, rules, typeDefinition, caseSensitive, thisRuleType);
//			} else
            if (FilenameUtils.getExtension(agnosticFileName).equals("csv")) {
                thisRuleType = readCSVFile(agnosticFileName, rules, typeDefinition, CSVFormat.DEFAULT, caseSensitive, thisRuleType);
            } else if (FilenameUtils.getExtension(agnosticFileName).equals("tsv")) {
                thisRuleType = readCSVFile(agnosticFileName, rules, typeDefinition, CSVFormat.TDF, caseSensitive, thisRuleType);
            }
        }
        return thisRuleType;
    }

//    public static HashMap<Integer, Rule> readXLSXRuleFile(String xlsxFileName) {
//        HashMap<Integer, Rule> rules = new HashMap<Integer, Rule>();
//        readXLSXRuleFile(xlsxFileName, rules, FASTRULEFILE, true);
//        return rules;
//    }

//
//	public static boolean[] readXLSXRuleFile(String xlsxFileName, HashMap<Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, boolean caseSensitive, boolean[] ruleSupports) {
//		try {
//			FileInputStream inputStream = new FileInputStream(new File(xlsxFileName));
//			Workbook workbook = new XSSFWorkbook(inputStream);
//			Sheet firstSheet = workbook.getSheetAt(0);
//			Iterator<Row> iterator = firstSheet.iterator();
//			int id = 0;
//			while (iterator.hasNext()) {
//				Row nextRow = iterator.next();
//				Iterator<Cell> cellIterator = nextRow.cellIterator();
//				ArrayList<String> cells = new ArrayList<>();
//				while (cellIterator.hasNext()) {
//					Cell cell = cellIterator.next();
//					switch (cell.getCellTypeEnum()) {
//						case NUMERIC:
//							cells.add(cell.getNumericCellValue() + "");
//							break;
//						default:
//							cells.add(cell.getStringCellValue());
//							break;
//					}
//				}
//				ruleSupports = parseCells(cells, id, rules, typeDefinition, caseSensitive, ruleSupports);
//				id++;
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return ruleSupports;
//	}

    @Deprecated
    public static boolean[] readCSVFile(String csvFileName, HashMap<Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, CSVFormat csvFormat, boolean caseSensitive, boolean[] ruleSupports) {
        try {
            Iterable<CSVRecord> recordsIterator = CSVParser.parse(new File(csvFileName), StandardCharsets.UTF_8, csvFormat);
            ruleSupports = readCSV(recordsIterator, rules, typeDefinition, caseSensitive, ruleSupports);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ruleSupports;
    }

    @Deprecated
    public static boolean[] readCSVString(String csvString, HashMap<Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, String splitter, boolean caseSensitive, boolean[] ruleSupports) {
        CSVFormat csvFormat = CSVFormat.DEFAULT;
        if (splitter.equals("\t")) {
            csvFormat = CSVFormat.TDF;
        }
        ruleSupports = readCSVString(csvString, rules, typeDefinition, csvFormat, caseSensitive, ruleSupports);
        return ruleSupports;
    }

    @Deprecated
    public static boolean[] readCSVString(String csvString, HashMap<Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, CSVFormat csvFormat, boolean caseSensitive, boolean[] ruleSupports) {
        try {
            Iterable<CSVRecord> recordsIterator = CSVParser.parse(csvString, csvFormat);
            ruleSupports = readCSV(recordsIterator, rules, typeDefinition, caseSensitive, ruleSupports);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ruleSupports;
    }

    @Deprecated
    private static boolean[] readCSV(Iterable<CSVRecord> recordsIterator, HashMap<Integer, Rule> rules,
                                     LinkedHashMap<String, TypeDefinition> typeDefinition, boolean caseSensitive, boolean[] ruleSupports) {
        int id = 0;
        for (CSVRecord record : recordsIterator) {
            ArrayList<String> cells = new ArrayList<>();
            for (String cell : record) {
                cells.add(cell);
            }
            ruleSupports = parseCells(cells, id, rules, typeDefinition, caseSensitive, ruleSupports);
            id++;
        }
        return ruleSupports;
    }

    @Deprecated
    private static boolean[] parseCells(ArrayList<String> cells, int id, HashMap<
            Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, boolean caseSensitive,
                                        boolean[] ruleSupports) {
        if (cells.get(0).startsWith("#") || cells.get(0).startsWith("\"#") || cells.get(0).trim().length() == 0)
            return ruleSupports;
        if (cells.get(0).startsWith("@") || cells.get(0).startsWith("&")) {
//          Rule type should be defined in the 1st line that starting with '@':  '@fastner' or '@fastcner'
            if (cells.size() == 1) {
                ruleSupports[0] = checkFastCRule(cells.get(0));
            } else if (cells.size() > 1) {
//          new UIMA type definition with '@typeName superTypeName'
//                or '@typeName superTypeName   newFeature1    newFeature2  newFeature3...'
                cells.set(0, cells.get(0).substring(1));
                typeDefinition.put(getShortName(cells.get(0)), new TypeDefinition(cells));
            }
            return ruleSupports;
        }
        if (cells.size() > 2) {
//            if (cells.get(2).indexOf(".") == -1)
//                cells.set(2, checkNameSpace(cells.get(2)));
            String rule = cells.get(0);
            String conceptShortName = getShortName(cells.get(2).trim());
            if (!typeDefinition.containsKey(conceptShortName)) {
                typeDefinition.put(conceptShortName, new TypeDefinition(cells.get(2).trim(), DeterminantValueSet.defaultSuperTypeName, new ArrayList<>()));
            }
            ruleSupports = addRule(rules, typeDefinition, new Rule(id, caseSensitive ? rule : rule.toLowerCase(), cells.get(2).trim(), Double.parseDouble(cells.get(1)), cells.size() > 3 ? Determinants.valueOf(cells.get(3)) : Determinants.ACTUAL), ruleSupports);
        } else
            System.out.println("Definition format error: line " + id + "\t\t" + cells);
        return ruleSupports;
    }

    @Deprecated
    public static HashMap<Integer, Rule> readCRuleString(String ruleString, String splitter) {
        int id = 0;
        HashMap<Integer, Rule> rules = new HashMap<>();
        for (String rule : ruleString.split("\n")) {
            rule = rule.trim();
            id++;
            if (rule.length() < 1 || rule.startsWith("#") || rule.startsWith("\"#"))
                continue;
            String[] definition = rule.split(splitter);
            Determinants determinant = Determinants.ACTUAL;

            if (definition.length > 3)
                determinant = Determinants.valueOf(definition[3]);
            if (definition.length > 2) {
                definition[2] = checkNameSpace(definition[2]);
            } else if (!rule.trim().startsWith("#")) {
                System.out.println("Definition format error: line " + id + "\t\t" + rule);
                continue;
            }
            rules.put(id, new Rule(id, definition[0], definition[2].trim(), Double.parseDouble(definition[1]), determinant));
        }
        return rules;
    }

    @Deprecated
    private static boolean[] addRule(HashMap<Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, Rule rule, boolean[] ruleSupports) {
//        support grouping
        if (!ruleSupports[1] && rule.rule.indexOf("(") != -1) {
            ruleSupports[1] = true;
        }
//        support square bracket
        if (!ruleSupports[2] && rule.rule.indexOf("[") != -1) {
            ruleSupports[2] = true;
        }
//        support replication grammar '+'
        if (!ruleSupports[3] && rule.rule.indexOf("+") != -1) {
            ruleSupports[3] = true;
        }

        rules.put(rule.id, rule);
        return ruleSupports;
    }

    /**
     * Rule type should be defined in the 1st line that starting with '@':  '@fastner' or '@fastcner'
     *
     * @param ruleString input String that contains rule definitions
     * @return if the rule is for FastCNER
     */
    @Deprecated
    private static boolean checkFastCRule(String ruleString) {
        int begin = ruleString.indexOf("@");
        if (begin == -1)
            begin = ruleString.indexOf("&");
        int end = ruleString.indexOf("\n", begin);
        String definition = ruleString.substring(begin, end == -1 ? ruleString.length() : end).toLowerCase();
        return definition.indexOf("fastc") != -1;
    }

    @Deprecated
    public static LinkedHashMap<String, TypeDefinition> readTypeDefinitions(ArrayList<ArrayList<String>> cells) {
        return readTypeDefinitions(cells, new HashMap<>());
    }

    @Deprecated
    public static LinkedHashMap<String, TypeDefinition> readTypeDefinitions(ArrayList<ArrayList<String>> cells, HashMap<String, String> valueFeatureMap) {
        LinkedHashMap<String, TypeDefinition> typeDefinitions = new LinkedHashMap<>();
        for (ArrayList<String> row : cells) {
            if (row.get(1).startsWith("@") || row.get(1).startsWith("&")) {
                if (row.get(1).substring(1).startsWith("CONCEPT_FEATURES")) {
                    String typeName = row.get(2).substring(1);
                    String superTypeName = row.get(3);
                    TypeDefinition typeDefinition = new TypeDefinition(typeName, superTypeName, new ArrayList<>());
                    if (row.size() > 4) {
                        for (int i = 4; i < row.size(); i++) {
                            String[] featureValuePair = row.get(i).split(":");
                            typeDefinition.addFeatureDefaultValue(featureValuePair[0].trim(), featureValuePair[1].trim());
                        }
                    }
                    typeDefinitions.put(typeDefinition.shortTypeName, typeDefinition);
                } else if (row.get(1).substring(1).startsWith("FEATURE_VALUES")) {
                    String featureName = row.get(2);
                    for (int i = 3; i < row.size(); i++) {
                        String value = row.get(i);
                        if (valueFeatureMap.containsKey(value)) {
                            System.err.println("You have more than one features have the value: " + value +
                                    ". You won't be able to use the value alone to define the conditions." +
                                    "Instead, you will need to use 'FeatureName:value' format.");
                        } else {
                            valueFeatureMap.put(value, featureName);
                        }
                    }
                } else {
                    String typeName = row.get(1).substring(1);
                    String superTypeName = row.get(2);
                    TypeDefinition typeDefinition = new TypeDefinition(typeName, superTypeName, new ArrayList<>());
                    if (row.size() > 3) {
                        for (int i = 3; i < row.size(); i++) {
                            String[] featureValuePair = row.get(i).split(":");
                            typeDefinition.addFeatureDefaultValue(featureValuePair[0].trim(), featureValuePair[1].trim());
                        }
                    }
                    typeDefinitions.put(typeDefinition.shortTypeName, typeDefinition);
                }
            } else {
                break;
            }
        }
        return typeDefinitions;
    }

    @Deprecated
    public LinkedHashMap<String, TypeDefinition> readTypeDefinitions(String ruleStr, int firstNLineAsSetting, boolean includeLineNumber) {
        LinkedHashMap<String, TypeDefinition> typeDefinitions;
        ArrayList<ArrayList<String>> cells = getCellsInList(ruleStr, firstNLineAsSetting, includeLineNumber);
        typeDefinitions = readTypeDefinitions(cells);
        return typeDefinitions;
    }

    /**
     * if local logging.properties file exist, overwrite system properties file.
     *
     * @param className the class name to log
     * @return a Logger
     */
    public static Logger getLogger(String className) {
        Logger logger = Logger.getLogger(className);
        try {
            if (new File("logging.properties").exists()) {
                if (System.getProperty("java.util.logging.config.file") == null) {
                    System.setProperty("java.util.logging.config.file", "logging.properties");
                }
                LogManager.getLogManager().readConfiguration(new FileInputStream(new File("logging.properties")));
            } else {
                LogManager.getLogManager().readConfiguration();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return logger;
    }

    /**
     * if local logging.properties file exist, overwrite system properties file.
     *
     * @param cls the class to log
     * @return a Logger
     */
    public static Logger getLogger(Class cls) {
        return getLogger(cls.getCanonicalName());
    }

}


