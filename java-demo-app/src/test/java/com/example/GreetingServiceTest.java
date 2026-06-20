package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GreetingServiceTest {

    @Test
    void shouldReturnMessageWithName() {
        GreetingService service = new GreetingService();

        assertEquals(
                "Hello Jenkins Students from Jenkins Java Demo",
                service.message("Jenkins Students")
        );
    }

    @Test
    void shouldReturnDefaultMessageForBlankName() {
        GreetingService service = new GreetingService();

        assertEquals(
                "Hello from Jenkins Java Demo",
                service.message("")
        );
    }
}

