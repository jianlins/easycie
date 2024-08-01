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

import edu.utah.bmi.nlp.core.IOUtil;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Jianlin Shi
 * Created on 12/11/16.
 */
public class RecordRowIterator implements Iterator<RecordRow> {
    public static Logger logger = IOUtil.getLogger(RecordRowIterator.class);
    private ResultSet resultSet;
    private ColumnInfo columninfo;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private boolean nextRetrieved = false;

    public RecordRowIterator(ResultSet resultSet, ColumnInfo columninfo) {
        this.resultSet = resultSet;
        this.columninfo = columninfo;
    }

    public boolean hasNext() {
        if (nextRetrieved)
            return true;
        try {
            nextRetrieved = resultSet.next();
            return nextRetrieved;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public RecordRow next() {
        nextRetrieved = false;
        RecordRow record = new RecordRow();
        try {
            for (Map.Entry<String, String> col : columninfo.getColumnInfoSet()) {
                switch (col.getValue()) {
                    case "char":
                        record.addCell(col.getKey(), resultSet.getString(col.getKey()));
                        break;
                    case "bigint":
                        record.addCell(col.getKey(), resultSet.getLong(col.getKey()));
                        break;
                    case "mediumint":
                        record.addCell(col.getKey(), resultSet.getLong(col.getKey()));
                        break;
                    case "smallint":
                        record.addCell(col.getKey(), resultSet.getInt(col.getKey()));
                        break;
                    case "tinyint":
                        record.addCell(col.getKey(), resultSet.getShort(col.getKey()));
                        break;
                    case "int":
                        record.addCell(col.getKey(), resultSet.getInt(col.getKey()));
                        break;
                    case "integer":
                        record.addCell(col.getKey(), resultSet.getInt(col.getKey()));
                        break;
                    case "number":
                        record.addCell(col.getKey(), resultSet.getLong(col.getKey()));
                        break;
                    case "numeric":
                        record.addCell(col.getKey(), resultSet.getLong(col.getKey()));
                        break;
                    case "varchar":
                        record.addCell(col.getKey(), resultSet.getString(col.getKey()));
                        break;
                    case "varchar2":
                        record.addCell(col.getKey(), resultSet.getString(col.getKey()));
                        break;
                    case "text":
                        record.addCell(col.getKey(), resultSet.getString(col.getKey()));
                        break;
                    case "longtext":
                        record.addCell(col.getKey(), resultSet.getString(col.getKey()));
                        break;
                    case "mediumtext":
                        record.addCell(col.getKey(), resultSet.getString(col.getKey()));
                        break;
                    case "tinytext":
                        record.addCell(col.getKey(), resultSet.getString(col.getKey()));
                        break;
                    case "boolean":
                        record.addCell(col.getKey(), resultSet.getBoolean(col.getKey()));
                        break;
                    case "float":
                        record.addCell(col.getKey(), resultSet.getFloat(col.getKey()));
                        break;
                    case "binary_float":
                        record.addCell(col.getKey(), resultSet.getFloat(col.getKey()));
                        break;
                    case "double":
                        record.addCell(col.getKey(), resultSet.getDouble(col.getKey()));
                        break;
                    case "binary_double":
                        record.addCell(col.getKey(), resultSet.getDouble(col.getKey()));
                        break;
                    case "date":
                        record.addCell(col.getKey(), resultSet.getDate(col.getKey()));
                        break;
                    case "time":
                        record.addCell(col.getKey(), resultSet.getTime(col.getKey()));
                        break;
                    case "blob":
                        Blob blob = resultSet.getBlob(col.getKey());
                        if (blob == null)
                            record.addCell(col.getKey(), null);
                        else
                            record.addCell(col.getKey(), blob.getBinaryStream());
                        break;
                    case "datetime":
                        try {
                            record.addCell(col.getKey(), resultSet.getTimestamp(col.getKey()));
                        } catch (SQLException e) {
//							fix sqlite jdbc bug
                            Object value = resultSet.getObject(col.getKey());
                            if (value instanceof String) {
                                Date dateTime = null;
                                try {
                                    dateTime = simpleDateFormat.parse((String) value);
                                } catch (ParseException e1) {
                                    e1.printStackTrace();
                                }
                                record.addCell(col.getKey(), new Timestamp(dateTime.getTime()));
                            }
                        }
                        break;
                    case "timestamp":
                        try {
                            record.addCell(col.getKey(), resultSet.getTimestamp(col.getKey()));
                        } catch (SQLException e) {
//							fix sqlite jdbc bug
                            Object value = resultSet.getObject(col.getKey());
                            if (value instanceof String) {
                                Date dateTime = null;
                                try {
                                    dateTime = simpleDateFormat.parse((String) value);
                                } catch (ParseException e1) {
                                    e1.printStackTrace();
                                }
                                record.addCell(col.getKey(), new Timestamp(dateTime.getTime()));
                            }
                        }
                        break;
                    case "clob":
                        Clob clob = resultSet.getClob(col.getKey());
                        if (clob == null)
                            record.addCell(col.getKey(), null);
                        else
                            record.addCell(col.getKey(), clob.getSubString(1, (int) clob.length()));
                        break;
                    default:
                        logger.warning("Data type: '" + col.getValue() + "' for column '" + col.getValue() + "' has not been fully supported, read it as string instead.");
                        record.addCell(col.getKey(), resultSet.getString(col.getKey()));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return record;
    }

    public void updateValueByName(String columnName, Object value) {
        try {
            resultSet.updateObject(columnName, value);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateValueById(int columnId, Object value) {
        try {
            resultSet.updateObject(columnId, value);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void commitUpdates() {
        try {
            resultSet.updateRow();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet getResultSet() {
        return resultSet;
    }

    public ColumnInfo getColumninfo() {
        return columninfo;
    }
}
