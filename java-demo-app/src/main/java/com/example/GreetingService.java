package com.example;

public class GreetingService {
    public String message(String name) {
        if (name == null || name.isBlank()) {
            return "Hello from Jenkins Java Demo";
        }
        return "Hello " + name + " from Jenkins Java Demo";
    }
}

