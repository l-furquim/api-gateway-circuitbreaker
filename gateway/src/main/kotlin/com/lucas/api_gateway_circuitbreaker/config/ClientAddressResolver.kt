package com.lucas.api_gateway_circuitbreaker.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.ServerRequest
import java.net.InetSocketAddress
import java.util.function.Function

@Component
class ClientAddressResolver(
    private final val logger: Logger = LoggerFactory.getLogger(ClientAddressResolver::class.java)
)  {

    fun resolveClientAddress(request: ServerRequest): String {
        val remoteAddress = request.remoteAddress()
            .map(Function { addr: InetSocketAddress? -> addr!!.address.hostAddress })
            .orElse("unknown")

        logger.info("Resolved client address: $remoteAddress, for request: ${request.uri()}, method: ${request.method()}, headers: ${request.headers().asHttpHeaders()}")

        return remoteAddress
    }

}