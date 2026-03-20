package com.testapp.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
  private int count;

  @GetMapping("/")
  public String index() {
    return "The format of the API is <code>/greeting?name=YourName</code>";
  }

  @GetMapping("/greeting") 
  public Map greeting(String name) {
    return Map.of(
        "id", ++count,
        "content", "Hello, " + name + "!"
    );
  }

}