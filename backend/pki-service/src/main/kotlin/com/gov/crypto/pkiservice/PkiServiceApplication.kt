package com.gov.crypto.pkiservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.gov.crypto"])
class PkiServiceApplication

fun main(args: Array<String>) {
    runApplication<PkiServiceApplication>(*args)
}
