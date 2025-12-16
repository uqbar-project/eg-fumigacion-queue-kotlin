package ar.edu.unsam.fumigacion.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    // ---------------------------
    // JSON
    // ---------------------------
    @Bean
    fun jsonMessageConverter(): MessageConverter {
        val objectMapper = ObjectMapper()
            .registerModule(
                KotlinModule.Builder()
                    .withReflectionCacheSize(512)
                    .build()
            )
            .registerModule(JavaTimeModule())

        return Jackson2JsonMessageConverter(objectMapper)
    }

    // ---------------------------
    // Exchanges
    // ---------------------------
    @Bean
    fun posicionExchange() =
        DirectExchange(POSICION_EXCHANGE)

    @Bean
    fun posicionRetryExchange() =
        DirectExchange(POSICION_RETRY_EXCHANGE)

    @Bean
    fun posicionDlqExchange() =
        DirectExchange(POSICION_DLQ_EXCHANGE)

    @Bean
    fun posicionDlq() =
        QueueBuilder.durable(POSICION_DLQ).build()

    @Bean
    fun finishedFlightExchange() =
        DirectExchange(FINISHED_FLIGHT_EXCHANGE)

    @Bean
    fun finishedFlightDlqExchange() =
        DirectExchange(FINISHED_FLIGHT_DLQ_EXCHANGE)

    // ---------------------------
    // Queues
    // ---------------------------
    @Bean
    fun posicionQueue() =
        QueueBuilder.durable(POSICION_QUEUE)
            // cuando falla → retry exchange
            .withArgument("x-dead-letter-exchange", POSICION_RETRY_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", POSICION_RETRY_QUEUE)
            .build()

    @Bean
    fun posicionRetryQueue() =
        QueueBuilder.durable(POSICION_RETRY_QUEUE)
            // ⏱ delay de retry
            .withArgument("x-message-ttl", 10_000)
            // cuando vence → vuelve a la main
            .withArgument("x-dead-letter-exchange", POSICION_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", POSICION_QUEUE)
            .build()

    @Bean
    fun finishedFlightQueue() =
        QueueBuilder.durable(FINISHED_FLIGHT_QUEUE)
            // cualquier reject → DLQ
            .withArgument("x-dead-letter-exchange", FINISHED_FLIGHT_DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", FINISHED_FLIGHT_DLQ)
            .build()

    @Bean
    fun finishedFlightDlq() =
        QueueBuilder.durable(FINISHED_FLIGHT_DLQ).build()

    // ---------------------------
    // Bindings
    // ---------------------------
    @Bean
    fun bindPosicionQueue() =
        BindingBuilder.bind(posicionQueue())
            .to(posicionExchange())
            .with(POSICION_QUEUE)

    @Bean
    fun bindRetryQueue() =
        BindingBuilder.bind(posicionRetryQueue())
            .to(posicionRetryExchange())
            .with(POSICION_RETRY_QUEUE)

    @Bean
    fun bindDlq() =
        BindingBuilder.bind(posicionDlq())
            .to(posicionDlqExchange())
            .with(POSICION_DLQ)

    @Bean
    fun bindFinishedFlightQueue() =
        BindingBuilder.bind(finishedFlightQueue())
            .to(finishedFlightExchange())
            .with(FINISHED_FLIGHT_QUEUE)

    @Bean
    fun bindFinishedFlightDlq() =
        BindingBuilder.bind(finishedFlightDlq())
            .to(finishedFlightDlqExchange())
            .with(FINISHED_FLIGHT_DLQ)

}
