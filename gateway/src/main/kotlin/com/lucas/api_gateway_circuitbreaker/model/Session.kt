package com.lucas.api_gateway_circuitbreaker.model

data class Session(
    val userId: String,
    val email: String,
    val role: String,
    val token: String,
    val createdAt: Long,
    val lastAccessAt: Long,
    val expiresAt: Long,
    val remoteAddress: String,
    val agent: String
) {
}