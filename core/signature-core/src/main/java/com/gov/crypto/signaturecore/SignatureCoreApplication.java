package com.gov.crypto.signaturecore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@ComponentScan(basePackages = { "com.gov.crypto.signaturecore", "com.gov.crypto.common" })
public class SignatureCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignatureCoreApplication.class, args);
    }
}
