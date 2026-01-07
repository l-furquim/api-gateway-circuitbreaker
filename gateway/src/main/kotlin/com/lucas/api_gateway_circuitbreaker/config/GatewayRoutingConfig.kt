package com.lucas.api_gateway_circuitbreaker.config

import com.lucas.api_gateway_circuitbreaker.filters.LoggerFilter
import com.lucas.api_gateway_circuitbreaker.filters.RateLimiterFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.ServerResponse

@Configuration
class GatewayRoutingConfig(
    private final val resolver: ClientAddressResolver,
    private final val rateLimiter: RateLimiterFilter,
    private final val loggerFilter: LoggerFilter,

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
            .GET("/api/v1/products/**", http())
            .before(uri(productRoute))
            .filter(loggerFilter)
            .filter(rateLimiter)
            .build()
    }

    @Bean
    fun paymentRoute(): RouterFunction<ServerResponse> {
        return route("payment_service")
            .GET("/api/v1/payments/**", http())
            .before(uri(paymentRoute))
            .filter(loggerFilter)
            .filter(rateLimiter)
            .build()
    }
    @Bean
    fun userRoute(): RouterFunction<ServerResponse> {
        return route("user_service")
            .GET("/api/v1/users/**", http())
            .before(uri(userRoute))
            .filter(loggerFilter)
            .filter(rateLimiter)
            .build()
    }

}