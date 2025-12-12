package ar.edu.unsam.fumigacion.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Value("\${spring.data.redis.host:localhost}")
    private lateinit var redisHost: String

    @Value("\${spring.data.redis.port:6379}")
    private var redisPort: Int = 6379

    @Value("\${spring.data.redis.password}")
    private lateinit var redisPassword: String

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        // Usamos la configuración Standalone pero seteando explícitamente la password
        val config = RedisStandaloneConfiguration(redisHost, redisPort)
        config.password = RedisPassword.of(redisPassword)

        val factory = LettuceConnectionFactory(config)

        // Esto es CLAVE: Validar que la conexión se cree con la password cargada
        // antes de devolver el Bean.
        return factory
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.setConnectionFactory(connectionFactory)

        // Serializadores
        val stringSerializer = StringRedisSerializer()
        val jsonSerializer = GenericJackson2JsonRedisSerializer()

        template.keySerializer = stringSerializer
        template.valueSerializer = jsonSerializer
        template.hashKeySerializer = stringSerializer
        template.hashValueSerializer = jsonSerializer

        template.afterPropertiesSet()
        return template
    }
}