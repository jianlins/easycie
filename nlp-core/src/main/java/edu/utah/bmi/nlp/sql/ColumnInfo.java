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


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Jianlin Shi
 * Created on 12/11/16.
 */
public class ColumnInfo {
    private LinkedHashMap<String, String> columnInfo;
    private HashMap<String, String> recordColumnMapping;

    private HashMap<String, String> columnRecordMapping;
    private HashMap<String, Integer> columnName2Id;
    private HashMap<Integer, String> columnId2Name;
    private int currentColumnid = 1;

    public ColumnInfo() {
        this.columnInfo = new LinkedHashMap<>();
        this.columnName2Id = new HashMap<>();
        this.columnId2Name = new HashMap<>();
        this.recordColumnMapping = new HashMap<>();
        this.columnRecordMapping = new HashMap<>();
    }

    public ColumnInfo(LinkedHashMap<String, String> columnInfo, HashMap<String, String> recordColumnMapping) {
        this.columnInfo = columnInfo;
        int id = 1;
        for (Map.Entry<String, String> entry : columnInfo.entrySet()) {
            columnName2Id.put(entry.getKey(), id);
            columnId2Name.put(id, entry.getKey());
            id++;
        }
        this.recordColumnMapping = recordColumnMapping;
        for (Map.Entry<String, String> entry : recordColumnMapping.entrySet()) {
            this.columnRecordMapping.put(entry.getValue(), entry.getKey());
        }
    }

    public ColumnInfo(LinkedHashMap<String, String> columnInfo) {
        this.columnInfo = columnInfo;
        int id = 1;
        for (Map.Entry<String, String> entry : columnInfo.entrySet()) {
            columnName2Id.put(entry.getKey(), id);
            columnId2Name.put(id, entry.getKey());
            id++;
        }
    }

    public void addColumnInfo(String columnName, String dataType) {
        if (dataType == null || dataType.equals("null"))
            dataType = "text";
        columnInfo.put(columnName, dataType);
        columnName2Id.put(columnName, currentColumnid);
        columnId2Name.put(currentColumnid, columnName);
        currentColumnid++;
    }

    public void addColumnInfo(String columnName, String insertRecordColumn, String dataType) {
        if (dataType == null || dataType.equals("null"))
            dataType = "text";
        columnInfo.put(columnName, dataType);
        if (!columnName2Id.containsKey(columnName)) {
            columnName2Id.put(columnName, currentColumnid);
            columnId2Name.put(currentColumnid, columnName);
            recordColumnMapping.put(insertRecordColumn, columnName);
            columnRecordMapping.put(columnName, insertRecordColumn);
            currentColumnid++;
        }
    }

    public String getColumnType(String columnName) {
        if (columnInfo.containsKey(columnName))
            return columnInfo.get(columnName);
        else if (recordColumnMapping.containsKey(columnName))
            return columnInfo.get(recordColumnMapping.get(columnName));
        else
            return null;

    }

    public String getColumnType(int columnId) {
        return columnInfo.get(columnId2Name.get(columnId));
    }

    public Set<Map.Entry<String, String>> getColumnInfoSet() {
        return columnInfo.entrySet();
    }

    public int getColumnId(String columnName) {
        if (columnName2Id.containsKey(columnName))
            return columnName2Id.get(columnName);
        else if (recordColumnMapping.containsKey(columnName))
            return columnName2Id.get(recordColumnMapping.get(columnName));
        else
            return -1;
    }

    public boolean isMappabelRecordColumn(String columnName) {
        return this.recordColumnMapping.containsKey(columnName);
    }

    public boolean isMappabelColumn(String columnName) {
        return this.columnRecordMapping.containsKey(columnName);
    }

    public String getMappedRecordColumn(String columnName) {
        return this.columnRecordMapping.getOrDefault(columnName, "");
    }

    public String getColumnName(int columnId) {
        return columnId2Name.get(columnId);
    }

    public LinkedHashMap<String, String> getColumnInfo() {
        return columnInfo;
    }
}
