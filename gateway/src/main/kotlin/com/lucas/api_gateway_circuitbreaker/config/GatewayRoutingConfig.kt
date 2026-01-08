package com.lucas.api_gateway_circuitbreaker.config

import com.lucas.api_gateway_circuitbreaker.filters.LoggerFilter
import com.lucas.api_gateway_circuitbreaker.filters.RateLimiterFilter
import com.lucas.api_gateway_circuitbreaker.filters.UserContextFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri
import org.springframework.cloud.gateway.server.mvc.filter.CircuitBreakerFilterFunctions
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http
import org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.ServerResponse
import java.net.URI

@Configuration
class GatewayRoutingConfig(
    private final val resolver: ClientAddressResolver,
    private final val rateLimiter: RateLimiterFilter,
    private final val loggerFilter: LoggerFilter,
    private final val userContextFilter: UserContextFilter,

    @Value("\${routes.product-catalog-service}")
    private val productRoute: String,

    @Value("\${routes.payment-service}")
    private val paymentRoute: String,

    @Value("\${routes.user-service}")
    private val userRoute: String,
){

    @Bean
    fun productCatalogRoute(): RouterFunction<ServerResponse> {
        return route("product_catalog_service")
            .route(path("/api/v1/products/**"), http())
            .before(uri(productRoute))
            .filter(
                CircuitBreakerFilterFunctions.circuitBreaker(
                    "product_catalog_service_circuit_breaker", URI.create("forward://fallbackRoute")
                )
            )
            .filter(loggerFilter)
            .filter(rateLimiter)
            .filter(userContextFilter)
            .build()
    }

    @Bean
    fun paymentRoute(): RouterFunction<ServerResponse> {
        return route("payment_service")
            .route(path("/api/v1/payments/**"), http())
            .before(uri(paymentRoute))
            .filter(
                CircuitBreakerFilterFunctions.circuitBreaker(
                    "payment_service_circuit_breaker", URI.create("forward://fallbackRoute")
                )
            )
            .filter(loggerFilter)
            .filter(rateLimiter)
            .filter(userContextFilter)
            .build()
    }
    @Bean
    fun userRoute(): RouterFunction<ServerResponse> {
        return route("user_service")
            .route(path("/api/v1/users/**"), http())
            .before(uri(userRoute))
            .filter(
                CircuitBreakerFilterFunctions.circuitBreaker(
                    "user_service_circuit_breaker", URI.create("forward://fallbackRoute")
                )
            )
            .filter(loggerFilter)
            .filter(rateLimiter)
            .filter(userContextFilter)
            .build()
    }

    @Bean
    fun fallbackRoute(): RouterFunction<ServerResponse> {
        return route("fallbackRoute")
            .GET("/fallbackRoute", { request ->
                ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Service Unavailable, please try again later")
            })
            .build()
    }

}