package com.testapp.controller;

import java.util.Map;
import java.sql.SQLException;
import static java.text.MessageFormat.format;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.testapp.model.Database;

@RestController
public class BotController {
  private int count;

  @GetMapping("/chat")
  public String chatbot() {
    
  }
}