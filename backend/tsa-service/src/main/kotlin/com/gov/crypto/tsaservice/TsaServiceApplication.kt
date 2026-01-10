package com.gov.crypto.tsaservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.gov.crypto"])
class TsaServiceApplication

fun main(args: Array<String>) {
    runApplication<TsaServiceApplication>(*args)
}
