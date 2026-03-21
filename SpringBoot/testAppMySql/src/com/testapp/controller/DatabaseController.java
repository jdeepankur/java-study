package com.testapp.controller;

import java.util.Map;
import java.sql.SQLException;
import static java.text.MessageFormat.format;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.testapp.model.Database;

@RestController
public class DatabaseController {
  private int count;
  private final String fullDbUrl = "/all";
  private final String tableName = "Users";
  private final String validEmail = "^[A-Za-z0-9+_.-]+@(.+)$";

  @GetMapping("/")
  public String index() {
    
    return format("""
    Navigate to
    <a href={0}>{0}</a>
    to see all the entries in the database.
    """,
    fullDbUrl);
  }

  @GetMapping(fullDbUrl) 
  public Map[] allTables() throws SQLException {
    return Database.getTable(tableName);
  }

  @GetMapping("/add")
  public Map addEntry(@RequestParam String name, @RequestParam String email) throws SQLException {
    String[] columns = {"name", "email"};
    if (email.matches(validEmail)) {
      String[] values = {"'" + name + "'", "'" + email + "'"};
      Database.insertValue(tableName, columns, values);
      return Map.of(
        "status", "ok",
        "message", "Entry added successfully"
      );
    }
    else {
      return Map.of(
        "status", "error",
        "message", "Invalid email format"
      );
    }
    
  }

  @RequestMapping("/error")
  public Map error() {
    return Map.of(
      "status", "error",
      "message", "If you're seeing this error, you probably did not send a well-formed POST request."
    );
  }

}