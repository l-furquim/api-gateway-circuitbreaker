package com.lucas.api_gateway_circuitbreaker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ApiGatewayCircuitbreakerApplication

fun main(args: Array<String>) {
	runApplication<ApiGatewayCircuitbreakerApplication>(*args)
}
