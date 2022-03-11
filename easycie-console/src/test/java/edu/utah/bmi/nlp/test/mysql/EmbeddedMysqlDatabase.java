package edu.utah.bmi.nlp.test.mysql;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;

import java.sql.*;


public class EmbeddedMysqlDatabase {
    private DBConfigurationBuilder configBuilder;
    private DB db;

    public EmbeddedMysqlDatabase() {
        init();
    }

    public void init() {
        System.setProperty("Dlog4j.configuration", "src/test/resources/db/log4j.properties");

        configBuilder = DBConfigurationBuilder.newBuilder();
        configBuilder.setPort(3306); // OR, default: setPort(0); => autom. detect free port
        configBuilder.setDataDir("target/data/db"); // just an example
        DBConfiguration config = configBuilder.build();
    }


    public void start() throws ManagedProcessException, SQLException {
        db = DB.newEmbeddedDB(configBuilder.build());
        db.start();
        Connection conn = DriverManager.getConnection(configBuilder.getURL("test"), "root", "");
        Statement stmt = conn.createStatement();
        stmt.execute("SET GLOBAL time_zone = 'America/Denver';");
        conn.commit();
        PreparedStatement stat = conn.prepareStatement("CREATE USER IF NOT EXISTS 'test'@'localhost' IDENTIFIED BY 'test';GRANT ALL PRIVILEGES ON *.* TO 'test'@'localhost';");
        stat.execute();
        conn.commit();
        stat.close();
        conn.close();
    }

    public void stop() throws ManagedProcessException {
        db.stop();
    }

    public static void main(String[] args) throws ManagedProcessException, SQLException {
        new EmbeddedMysqlDatabase().start();

    }
}