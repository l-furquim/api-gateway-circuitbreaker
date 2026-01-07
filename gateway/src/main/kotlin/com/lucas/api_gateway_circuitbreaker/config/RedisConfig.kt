package com.lucas.api_gateway_circuitbreaker.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class RedisConfig(

    @Value("\${spring.redis.host:redis}")
    private val redisHost: String,

    @Value("\${spring.redis.port:6379}")
    private val redisPort: String,

    @Value("\${spring.redis.password:}")
    private val redisPassword: String

) {
    @Bean(destroyMethod = "shutdown")
    fun redissonClient(): RedissonClient {
        val config: Config = Config()

        val address = "redis://$redisHost:$redisPort"

        config.useSingleServer() // TODO: move to a cluster of multi cluster server
            .setAddress(address)
            .setPassword(redisPassword)
            .setConnectionPoolSize(10)
            .setConnectionMinimumIdleSize(5)
            .setConnectTimeout(5000).timeout = 3000

        return Redisson.create(config)
    }

}