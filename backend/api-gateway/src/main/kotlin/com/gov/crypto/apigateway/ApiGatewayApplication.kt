package com.gov.crypto.apigateway

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.DispatcherHandler

@SpringBootApplication
class ApiGatewayApplication {

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            .route("identity-service") { r ->
                r.path("/api/v1/auth/**")
                    .filters { f -> f.stripPrefix(2) }
                    .uri("http://identity-service:8081")
            }
            .route("pki-service") { r ->
                r.path("/api/v1/pki/**")
                    .filters { f -> f.stripPrefix(2) }
                    .uri("http://pki-service:8082")
            }
            .route("tsa-service") { r ->
                r.path("/api/v1/tsa/**")
                    .filters { f -> f.stripPrefix(2) }
                    .uri("http://tsa-service:8083")
            }
            .route("document-service") { r ->
                r.path("/api/v1/documents/**")
                    .filters { f -> f.stripPrefix(2) }
                    .uri("http://document-service:8084")
            }
            .build()
    }
}

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}
