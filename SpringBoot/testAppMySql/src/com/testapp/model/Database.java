package com.testapp.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {
    private static final int port = 3306;
    private static final String url = "jdbc:mysql://localhost:" + port + "/testdb";
    private static final String user = "root";
    private static final String password = "password";
    private static final int MAX_ATTEMPTS = 50;

    private static Connection connection;


    public static void createTable(String tableName) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255))";
        connection.createStatement().execute(sql);
    }

    public static void insertValue(String tableName, String[] columns, String[] values) throws SQLException {
        String sql = "INSERT INTO " + tableName + " (" + String.join(", ", columns) + ") VALUES (" + String.join(", ", values) + ")";
        connection.createStatement().execute(sql);
    }

    public static Map[] getTable(String tableName) throws SQLException {
        var rs = connection.createStatement().executeQuery("SELECT * FROM " + tableName);
        var meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        List<Map> result = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(meta.getColumnName(i), rs.getObject(i));
            }
            result.add(row);
        }
        return result.toArray(new Map[0]);
    }

    public static Map getRow(String tableName, int id) throws SQLException {
        var rs = connection.createStatement().executeQuery("SELECT * FROM " + tableName + " WHERE id = " + id);
        if (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            var meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                row.put(meta.getColumnName(i), rs.getObject(i));
            }
            return row;
        }
        return null;
    }

    public static String[] allTables() throws SQLException {
        var rs = connection.createStatement().executeQuery("SHOW TABLES");
        List<String> tables = new ArrayList<>();
        while (rs.next()) {
            tables.add(rs.getString(1));
        }
        return tables.toArray(new String[0]);
    }

    public static void startServer() {
        try {
            var running = new ProcessBuilder("docker", "inspect", "-f", "{{.State.Running}}", "testdb-mysql")
                .start();
            if (running.waitFor() == 0) {
                String out = new String(running.getInputStream().readAllBytes()).trim();
                if ("true".equals(out)) return;
                new ProcessBuilder("docker", "start", "testdb-mysql").start().waitFor();
                return;
            }
            new ProcessBuilder("docker", "run", "--name", "testdb-mysql",
                "-e", "MYSQL_ROOT_PASSWORD=" + password,
                "-e", "MYSQL_DATABASE=testdb",
                "-p", port + ":" + port,
                "-d", "mysql:8.4").start().waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void connect() throws SQLException {
        try {
            if (connection != null && !connection.isClosed() && connection.isValid(2)) return;
        } catch (SQLException ignored) {}
        startServer();
        for (int i = 1; ; i++) {
            try {
                System.out.println("Attempt " + i);
                connection = java.sql.DriverManager.getConnection(url, user, password);
                return;
            } catch (SQLException e) {
                try { Thread.sleep(1000); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted", ie);
                }
            }
        }
    }

}
