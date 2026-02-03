package com.lucas.api_gateway_circuitbreaker.config

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component


@Component
class OpenTelemetryAppenderConfig(
    private val openTelemetryProvider: ObjectProvider<OpenTelemetry>
) : InitializingBean {

    private val log = LoggerFactory.getLogger(OpenTelemetryAppenderConfig::class.java)

    override fun afterPropertiesSet() {
        val otel: OpenTelemetry? = openTelemetryProvider.getIfAvailable()

        if (otel != null) {
            try {
                OpenTelemetryAppender.install(otel)
                log.info("OpenTelemetryAppender installed")
            } catch (ex: Exception) {
                log.error("Failed to install OpenTelemetryAppender", ex)
            }
        } else {
            log.warn("OpenTelemetry bean not found; skipping OpenTelemetryAppender installation")
        }
    }

}