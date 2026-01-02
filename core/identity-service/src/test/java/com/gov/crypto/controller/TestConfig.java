package com.gov.crypto.controller;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Test configuration for controller tests.
 * This is needed because AuthController is in com.gov.crypto.controller,
 * which is not a subpackage of com.gov.crypto.identityservice.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.gov.crypto.controller")
public class TestConfig {
}
