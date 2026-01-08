package com.lucas.api_gateway_circuitbreaker.filters

import com.lucas.api_gateway_circuitbreaker.model.Session
import com.lucas.api_gateway_circuitbreaker.service.SessionService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.HandlerFilterFunction
import org.springframework.web.servlet.function.HandlerFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse

@Component
class UserContextFilter(
    private final val sessionService: SessionService,
    private final val logger: Logger = LoggerFactory.getLogger(UserContextFilter::class.java)
): HandlerFilterFunction<ServerResponse, ServerResponse> {

    override fun filter(
        request: ServerRequest,
        next: HandlerFunction<ServerResponse>
    ): ServerResponse {

        val sessionId = this.extractSessionId(request)

        if(sessionId.isEmpty()) {
            return this.handleUnauthorized("Please, first authenticate")
        }

        val session =
            sessionService.getSession(sessionId) ?: return this.handleUnauthorized("Invalid or expired session")

        logger.debug("Session validated for user: {}", session.userId);

        MDC.put("user_id", session.userId);
        MDC.put("session_id", sessionId);

        val mutatedRequest = this.mutateHeader(request,session, sessionId)

        return next.handle(mutatedRequest)
    }

    private fun extractSessionId(request: ServerRequest): String {
        val sessionId: String = request
            .headers()
            .firstHeader("X-Session-Id")
            .orEmpty()

        if (sessionId.isNotEmpty()) {
            return sessionId
        }

        // For fallback try to catch a cookie
        return request.cookies()
            .getFirst("SESSION_ID")?.value
            .orEmpty()
    }

    private fun handleUnauthorized(message: String?): ServerResponse {
        return ServerResponse.status(401)
            .body("Unauthorized request: %s".format(message ?: ""))
    }

    private fun mutateHeader(request: ServerRequest, session: Session, sessionId: String): ServerRequest {
        return ServerRequest.from(request)
            .header("Authorization", "Bearer " + session.token)
            .header("X-User-Id", session.userId)
            .header("X-User-Email", session.email)
            .header("X-User-Role", session.role)
            .header("X-Session-Id", sessionId)
            .build();
    }
}