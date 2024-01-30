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
    private final LinkedHashMap<String, String> columnInfo;
    private HashMap<String, Integer> columnName2Id;
    private HashMap<Integer, String> columnId2Name;
    private int currentColumnid = 1;

    public ColumnInfo() {
        this.columnInfo = new LinkedHashMap<>();
        this.columnName2Id = new HashMap<>();
        this.columnId2Name = new HashMap<>();
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

    public String getColumnType(String columnName) {
        return columnInfo.get(columnName);
    }

    public String getColumnType(int columnId) {
        return columnInfo.get(columnId2Name.get(columnId));
    }

    public Set<Map.Entry<String, String>> getColumnInfoSet() {
        return columnInfo.entrySet();
    }

    public int getColumnId(String columnName) {
        return columnName2Id.get(columnName);
    }

    public String getColumnName(int columnId) {
        return columnId2Name.get(columnId);
    }

    public LinkedHashMap<String, String> getColumnInfo() {
        return columnInfo;
    }
}
