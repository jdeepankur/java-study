package com.testapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.testapp.model.Database;

@SpringBootApplication
public class App {
    private static final int PORT = 8082;
    private static final String[] COLUMNS = {"name", "email"};
    private static final String TABLENAME = "Users";

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(App.class);

        try {
            Database.connect();
            Database.createTable(TABLENAME, COLUMNS);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        app.setDefaultProperties(java.util.Map.of("server.port", String.valueOf(PORT)));
        app.run(args);
    }

}