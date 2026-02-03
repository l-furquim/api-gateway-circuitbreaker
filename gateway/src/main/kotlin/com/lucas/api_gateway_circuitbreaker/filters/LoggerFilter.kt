package com.lucas.api_gateway_circuitbreaker.filters

import io.micrometer.tracing.Tracer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.HandlerFilterFunction
import org.springframework.web.servlet.function.HandlerFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse

@Component
class LoggerFilter(
    private final val logger: Logger = LoggerFactory.getLogger(LoggerFilter::class.java),
    private final val tracer: Tracer
): HandlerFilterFunction<ServerResponse, ServerResponse> {
    override fun filter(
        request: ServerRequest,
        next: HandlerFunction<ServerResponse>
    ): ServerResponse {
        val startTime = System.currentTimeMillis()

        MDC.put("method", request.method().name())
        MDC.put("path", request.path())
        MDC.put(
            "client_ip", request.remoteAddress()
                .map{ addr -> addr.address.hostAddress }
                .orElse("unknown")
        )

        logger.info("Incoming request - {} {}", request.method(), request.path());

        try {
            val response = next.handle(request)
            val duration = System.currentTimeMillis() - startTime

            MDC.put("status", response.statusCode().value().toString())
            MDC.put("duration_ms", duration.toString())

            logger.info("Completed request - {} {} with status {} in {} ms",
                request.method(), request.path(), response.statusCode().value(), duration)

            return response
        }catch(e: Exception) {
            val duration = System.currentTimeMillis() - startTime

            MDC.put("duration_ms", "500")
            logger.error("Request failed - duration: {}ms", duration, e);

            throw e
        } finally {
            MDC.clear()
        }
    }

}