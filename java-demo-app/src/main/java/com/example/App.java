package com.example;

public class App {
    public static void main(String[] args) {
        GreetingService service = new GreetingService();
        System.out.println(service.message("Jenkins Students"));
    }
}

