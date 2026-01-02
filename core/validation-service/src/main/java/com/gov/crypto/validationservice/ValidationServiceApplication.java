package com.gov.crypto.validationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "com.gov.crypto.validationservice", "com.gov.crypto.common" })
public class ValidationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValidationServiceApplication.class, args);
    }
}
