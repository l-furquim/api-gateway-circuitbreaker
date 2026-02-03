package com.lucas.api_gateway_circuitbreaker.config

import com.lucas.api_gateway_circuitbreaker.model.Session
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer


@Configuration
class RedisConfig(

    @Value("\${spring.data.redis.host:redis}")
    private val redisHost: String,

    @Value("\${spring.data.redis.port:6379}")
    private val redisPort: Int,

    @Value("\${spring.data.redis.password:}")
    private val redisPassword: String
) {

    @Bean
    fun connectionFactory(): LettuceConnectionFactory {
        val standalone = RedisStandaloneConfiguration()
        standalone.hostName = redisHost
        standalone.port = redisPort
        if (redisPassword.isNotBlank()) {
            standalone.setPassword(redisPassword)
        }
        return LettuceConnectionFactory(standalone)
    }

    @Bean
    fun sessionTemplate(
        connectionFactory: RedisConnectionFactory,
    ): RedisTemplate<String, Session> {

        val template = RedisTemplate<String, Session>()
        template.connectionFactory = connectionFactory

        val jsonSerializer = RedisSerializer.json()

        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = jsonSerializer
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = jsonSerializer

        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun userSessionTemplate(
        connectionFactory: RedisConnectionFactory,
    ): RedisTemplate<String, Set<String>> {

        val template = RedisTemplate<String, Set<String>>()
        template.connectionFactory = connectionFactory

        val jsonSerializer = RedisSerializer.json()

        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = jsonSerializer
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = jsonSerializer

        template.afterPropertiesSet()
        return template
    }

    @Bean(destroyMethod = "shutdown")
    fun redissonClient(): RedissonClient {
        val config: Config = Config()

        val address = "redis://$redisHost:$redisPort"

        config.useSingleServer()
            .setAddress(address)
            .setPassword(redisPassword.ifBlank { null })
            .setConnectionPoolSize(10)
            .setConnectionMinimumIdleSize(5)
            .setConnectTimeout(5000)
            .timeout = 3000

        return Redisson.create(config)
    }

}
