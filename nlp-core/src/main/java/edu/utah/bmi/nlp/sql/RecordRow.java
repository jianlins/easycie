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

import java.util.*;
import java.util.Map.Entry;

/**
 * Created by u0876964 on 12/9/16.
 */
public class RecordRow implements Cloneable {


    private LinkedHashMap<String, Object> name_cells = new LinkedHashMap<>();
    private HashMap<String, Integer> ColumnNamePos = new HashMap<>();
    private HashMap<Integer, Object> id_cells = new HashMap<>();
    private int columnId = 1;

    public RecordRow() {

    }

    public RecordRow(Object... values) {
        addCellValues(values);
    }

    public RecordRow(LinkedHashMap<String, Object> name_cells) {
        for (Entry<String, Object> entry : name_cells.entrySet()) {
            this.name_cells.put(entry.getKey(), entry.getValue());
            this.id_cells.put(columnId, entry.getValue());
            columnId++;
        }
    }

    public HashMap<Integer, Object> getId_cells() {
        return id_cells;
    }

    public RecordRow addCell(String columnName, Object value) {
        this.name_cells.put(columnName, value);
        int columnId;
        if (ColumnNamePos.containsKey(columnName))
            columnId = ColumnNamePos.get(columnName);
        else {
            columnId = this.columnId;
            ColumnNamePos.put(columnName, columnId);
            this.columnId++;
        }
        this.id_cells.put(columnId, value);
        return this;

    }

    public RecordRow addCellValue(Object value) {
        this.id_cells.put(columnId, value);
        columnId++;
        return this;
    }

    public RecordRow addCellValues(Object... values) {
        for (Object value : values) {
            this.id_cells.put(columnId, value);
            columnId++;
        }
        return this;
    }

    public RecordRow addCells(Object... keyValuePairs) {
        boolean key = true;
        String columnName = "";
        Object value = null;
        for (Object keyValue : keyValuePairs) {
            if (key) {
                columnName = (String) keyValue;
            } else {
                value = keyValue;
                addCell(columnName, value);
            }
            key = !key;
        }
        return this;
    }

    public Object getValueByColumnName(String columnName) {
        if (name_cells.containsKey(columnName))
            return name_cells.get(columnName);
        else
            return null;
    }

    public String getStrByColumnName(String columnName) {
        if (name_cells.containsKey(columnName))
            return name_cells.get(columnName) + "";
        else
            return null;
    }

    public Object getValueByColumnId(int columnId) {
        if (id_cells.containsKey(columnId))
            return id_cells.get(columnId);
        else
            return null;
    }

    public HashMap<Integer, Object> getColumnIdsValues() {
        return id_cells;
    }

    public LinkedHashMap<String, Object> getColumnNameValues() {
        return name_cells;
    }


    public String serialize(String... excludes) {
        StringBuilder ser = new StringBuilder();

        HashSet<String> excludedColumns = new HashSet<>();
        if (excludes != null) {
            Collections.addAll(excludedColumns, excludes);
        }
        for (Entry<String, Object> entry : name_cells.entrySet()) {
            String columnName = entry.getKey();
            if (excludedColumns.size() == 0 || !excludedColumns.contains(columnName)) {
                Object value = entry.getValue();
                if (value == null)
                    value = "NULL";
                ser.append("|");
                ser.append(columnName);
                ser.append(":");
                ser.append(Base64.getEncoder().encodeToString(value.toString().getBytes()));
            }
        }
        if (ser.length() > 1)
            ser.deleteCharAt(0);
        return ser.toString();
    }

    public void deserialize(String input) {
        for (String entry : input.split("\\|")) {
            if (entry.indexOf(":") == -1) {
                System.out.println(input);
                throw new IllegalArgumentException("Illegal input string to be deserialized:\n" + input);
            } else {
                String[] pair = entry.split(":");
                String key = pair[0];
                String value = new String(Base64.getDecoder().decode(pair[1].getBytes()));
                name_cells.put(key, value);
            }
        }
    }

    public String toString() {
        return toString("");
    }

    public String toString(String prefix) {
        if (this == null) {
            return "null record";
        }
        StringBuilder output = new StringBuilder();
        if (name_cells.size() > 0) {
            for (Entry<String, Object> entry : name_cells.entrySet()) {
                output.append(prefix);
                output.append(entry.getKey() + "\t" + entry.getValue());
                output.append("\n");
            }
        } else if (id_cells.size() > 0) {
            for (Entry<Integer, Object> entry : id_cells.entrySet()) {
                output.append(prefix);
                output.append(entry.getKey() + "\t" + entry.getValue());
                output.append("\n");
            }
        }
        return output.toString();
    }

    public void print() {
        System.out.println(this);
    }

    public RecordRow clone() {
        RecordRow newRecordRow = new RecordRow();
        newRecordRow.name_cells = (LinkedHashMap<String, Object>) this.name_cells.clone();
        newRecordRow.id_cells = (HashMap<Integer, Object>) this.id_cells.clone();
        newRecordRow.ColumnNamePos = (HashMap<String, Integer>) this.ColumnNamePos.clone();
        newRecordRow.columnId=this.columnId;
        return newRecordRow;
    }
}
