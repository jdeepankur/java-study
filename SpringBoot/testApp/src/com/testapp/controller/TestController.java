package com.testapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  @GetMapping("/")
  public String index() {
    return "The format of the API is <code>/greeting?name=YourName</code>";
  }

  @GetMapping("/greeting") 
  public String greeting(String name) {
    return "test";
  }

}