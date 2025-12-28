package com.gov.crypto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "com.gov.crypto", "com.gov.crypto.common.pqc" })
public class CaAuthorityApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaAuthorityApplication.class, args);
    }

}
