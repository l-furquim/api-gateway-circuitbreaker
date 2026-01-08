package com.lucas.api_gateway_circuitbreaker.service

import com.lucas.api_gateway_circuitbreaker.model.Session
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@Service
class SessionService(
    private final val sessionTemplate: RedisTemplate<String, Session>,
    private final val userTemplate: RedisTemplate<String, String>,
    private final val SESSION_PREFIX: String = "session:",
    private final val USER_SESSION_PREFIX: String = "session:user:",
    private final val SESSION_TIMEOUT_IN_MINUTES: Duration = Duration.ofMinutes(30),
    private final val SESSION_SLIDING_WINDOW_IN_MINUTES: Duration = Duration.ofMinutes(5),
    private final val logger: Logger = LoggerFactory.getLogger(SessionService::class.java)
) {

    fun createSession(
        userId: String,
        email: String,
        role: String,
        token: String,
        createdAt: Long,
        lastAccessAt: Long,
        expiresAt: Long,
        remoteAddress: String,
        agent: String
    ): String {
        val sessionId = UUID.randomUUID().toString()

        val session = Session(
            userId = userId,
            email = email,
            role = role,
            token = token,
            createdAt = createdAt,
            lastAccessAt = lastAccessAt,
            expiresAt = expiresAt,
            remoteAddress = remoteAddress,
            agent = agent
        )

        this.deleteAllUserSessions(userId)

        sessionTemplate.opsForValue().set(
            SESSION_PREFIX + sessionId,
            session,
            SESSION_TIMEOUT_IN_MINUTES
        )

        val userSessionKey = USER_SESSION_PREFIX + userId

        userTemplate.opsForSet().add(
            sessionId,
        )

        userTemplate.expire(userSessionKey, SESSION_TIMEOUT_IN_MINUTES)

        logger.info("Session created: sessionId={}, userId={}, address={}", sessionId, userId, remoteAddress);

        return sessionId
    }

    fun getSession(sessionId: String): Session? {
        val session = sessionTemplate.opsForValue().get(SESSION_PREFIX + sessionId) ?: return null

        if(session.expiresAt < System.currentTimeMillis()) {
            // Session expired
            // In a scenario of auto refresh token, here we could implement a token refresh logic in the gateway
            // of sending a signal to the auth service to refresh the token
            this.deleteSession(sessionId)
            return null
        }

        val timeSinceLastAccess = session.lastAccessAt - System.currentTimeMillis()

        if(timeSinceLastAccess <= SESSION_SLIDING_WINDOW_IN_MINUTES.toMillis()) {
            return this.refreshSession(sessionId, session)
        }

        return session
    }

    fun deleteSession(sessionId: String) {
        try {
            val sessionKey = SESSION_PREFIX + sessionId

            val session= sessionTemplate.opsForValue().get(sessionKey)

            if (session != null) {
                val userSessionsKey: String = USER_SESSION_PREFIX + session.userId
                userTemplate.opsForSet().remove(userSessionsKey, sessionId)
                logger.info("Revoked session %s for user %s".format(sessionId, session.userId))
            }
        } catch (e: Exception) {
            logger.error("Failed to delete session: {}", sessionId, e)
        }
    }

    private fun deleteAllUserSessions(userId: String) {
        try {
            val userSessionsKey: String = USER_SESSION_PREFIX + userId
            val sessionIds = userTemplate.opsForSet().members(userSessionsKey)

            if (sessionIds != null) {
                for (sessionId in sessionIds) {
                    this.deleteSession(sessionId)
                }
                sessionTemplate.delete(userSessionsKey)

                logger.info("All sessions deleted for user: {} (count: {})", userId, sessionIds.size)
            }
        } catch (e: Exception) {
            logger.error("Failed to delete all user sessions: {}", userId, e)
        }
    }

    private fun refreshSession(sessionId: String, session: Session): Session {
        val updatedSession = session.copy(
            lastAccessAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + SESSION_TIMEOUT_IN_MINUTES.toMillis()
        )

        sessionTemplate.opsForValue().set(
            SESSION_PREFIX + sessionId,
            updatedSession,
            SESSION_TIMEOUT_IN_MINUTES
        )

        logger.info("Refreshed session %s for user %s".format(sessionId, session.userId))

        return updatedSession
    }

}