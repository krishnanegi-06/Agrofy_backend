package com.agripulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgroPulse – Smart Crop Moisture & Irrigation System
 * Java Spring Boot Backend
 */
@SpringBootApplication
public class AgripulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgripulseApplication.class, args);
        System.out.println("""
                ╔══════════════════════════════════════════╗
                ║   🌱 AgroPulse Backend Started           ║
                ║   Running at http://localhost:8080       ║
                ║   API Base: http://localhost:8080/api    ║
                ╚══════════════════════════════════════════╝
                """);
    }
}
