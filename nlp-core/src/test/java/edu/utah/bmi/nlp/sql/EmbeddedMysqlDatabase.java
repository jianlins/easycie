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

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
//not working for jdk8 on mac yet.
@Disabled
public class EmbeddedMysqlDatabase {
    private static DBConfigurationBuilder configBuilder;
    private static DB db;

    public EmbeddedMysqlDatabase() {
        init();
    }

    @BeforeAll
    public static void init() {
//        System.setProperty("Dlog4j.configuration", "src/test/resources/db/log4j.properties");

        configBuilder = DBConfigurationBuilder.newBuilder();
        configBuilder.setPort(3306); // OR, default: setPort(0); => autom. detect free port
        configBuilder.setBaseDir("target/data/db");// just an example
        DBConfiguration config = configBuilder.build();
    }

    @Test
    @Disabled
    public void start() throws ManagedProcessException, SQLException {
        db = DB.newEmbeddedDB(configBuilder.build());
        db.start();
        Connection conn = DriverManager.getConnection(configBuilder.getURL("test"), "root", "");
        PreparedStatement stat = conn.prepareStatement("CREATE USER IF NOT EXISTS 'test'@'localhost' IDENTIFIED BY 'test';GRANT ALL PRIVILEGES ON *.* TO 'test'@'localhost';");
        stat.execute();
        conn.commit();
        stat.close();
        conn.close();
    }

    @AfterAll
    public static void stop() throws ManagedProcessException {
        db.stop();
    }

}