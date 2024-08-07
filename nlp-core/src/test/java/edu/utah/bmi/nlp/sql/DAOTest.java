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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;

/**
 * Created by Jianlin Shi on 7/22/17.
 */
public class DAOTest {

    @Test
    public void testCreate() {
        ClassLoader classLoader = getClass().getClassLoader();
        File dataDir=new File("target/data");
        if (!dataDir.exists()){
            dataDir.mkdirs();
        }
        File configFile = new File(classLoader.getResource("source/test_sqlite_config.xml").getFile());
        DAO dao = new DAO(configFile, true, false);
        dao.initiateTableFromTemplate("ANNOTATION_TABLE", "PE_REF", true);
        dao.insertRecord("PE_REF", new RecordRow(31, "v1", 1));
        dao.insertRecord("PE_REF", new RecordRow(35, "v2", 1));
        dao.initiateTableFromTemplate("ANNOTATION_TABLE", "VTE_REF", true);

        dao.initiateTableFromTemplate("DOCUMENTS_TABLE", "NLP_INPUT", true);
        dao.insertRecord("NLP_INPUT", new RecordRow(1, 13, 387282, "sample text"));
        assert (dao.checkTableExits("VTE_REF"));
        assert (dao.checkTableExits("NLP_INPUT"));
//        dao.dropTable("VTE_REF");
//        assert (!dao.checkTableExits("VTE_REF"));
        RecordRowIterator recordIter = dao.queryRecordsFromPstmt("PE_REF", "v1");
        assert (recordIter.hasNext());
        RecordRow recordRow=recordIter.next();
        Object value=recordRow.getValueByColumnName("DOC_NAME");
        assert (value.equals("31"));
        dao.close();
    }

    @Disabled
    @Test
    public void testDrop() throws SQLException {
        ClassLoader classLoader = getClass().getClassLoader();
        File configFile = new File(classLoader.getResource("source/edw_sqlite.xml").getFile());
        DAO dao = new DAO(configFile, false, false);
        dao.initiateTableFromTemplate("DOC_REFERENCE_TABLE", "VTE_REF", true);
        System.out.println("VTE_REF exist: " + dao.checkExists("VTE_REF"));
        if (dao.checkExists("VTE_REF"))
            dao.dropTable("VTE_REF");
        System.out.println("VTE_REF exist: " + dao.checkExists("VTE_REF"));
//        dao.con.commit();
//        dao.queryRecords("SELECT * FROM VTE_REF;");
//        dao.dropTable("NLP_INPUT");
//        dao.close();
//        Connection con = DriverManager.getConnection("jdbc:sqlite:target/test.db","test","");
//        Statement stmt = con.createStatement();
//        stmt.execute("DROP TABLE NLP_INPUT;");
    }
    @Disabled
    @Test
    public void testMySQL() {
        ClassLoader classLoader = getClass().getClassLoader();
        File configFile = new File(classLoader.getResource("db/mysqlconfig.xml").getFile());
        DAO dao = new DAO(configFile, true, false);
        dao.initiateTableFromTemplate("DOCUMENTS", "PE_REF", true);
        dao.insertRecord("PE_REF", new RecordRow(31, "y", "noth.txt", "sdoasdakld"));
        dao.insertRecord("PE_REF", new RecordRow(35, "n", "not2.txt", "sdoasdakld"));
        assert (dao.checkTableExits("PE_REF"));
    }
}