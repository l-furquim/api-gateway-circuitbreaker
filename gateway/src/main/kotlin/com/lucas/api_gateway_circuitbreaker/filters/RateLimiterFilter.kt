package com.lucas.api_gateway_circuitbreaker.filters

import com.lucas.api_gateway_circuitbreaker.config.ClientAddressResolver
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.proxy.ProxyManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.HandlerFilterFunction
import org.springframework.web.servlet.function.HandlerFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import java.time.Duration
import java.util.function.Supplier

@Component
class RateLimiterFilter(
    private final val proxyManager: ProxyManager<String>,
    private final val addressResolver: ClientAddressResolver,
    private final val logger: Logger = LoggerFactory.getLogger(RateLimiterFilter::class.java)
): HandlerFilterFunction<ServerResponse, ServerResponse> {

    override fun filter(
        request: ServerRequest,
        next: HandlerFunction<ServerResponse>
    ): ServerResponse {
        val clientKey = this.addressResolver.resolveClientAddress(request)

        val configSupplier: Supplier<BucketConfiguration> = Supplier {
            BucketConfiguration
                .builder()
                .addLimit { limit -> limit
                                    .capacity(100)
                                    .refillGreedy(100, Duration.ofMinutes(1))
                }
                .build()
        }

        val bucket = proxyManager
            .builder()
            .build(clientKey, configSupplier)

        if (bucket.tryConsume(1)) {
            logger.debug("Client $clientKey passed")

            return next.handle(request)
        }

        logger.warn("Rate limit exceed for $clientKey")

        return ServerResponse
            .status(429)
            .header("X-Rate-Limit-Retry-After-Seconds", "60")
            .header("Content-Type", "application/json")
            .body("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}")
    }


}