package ar.edu.unsam.fumigacion.config

import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuración de Spring AMQP para declarar las colas de RabbitMQ.
 * Cuando la aplicación Spring Boot se inicializa, registrará estos beans
 * y, si no existen en el broker de RabbitMQ, los creará automáticamente.
 */
@Configuration
class RabbitMQConfig {

    // --- Nombres de las Colas ---
    companion object {
        const val POSICION_QUEUE = "q.posicion.raw"
        const val FACTURACION_QUEUE = "q.facturacion.acumular"
    }

    /**
     * Declara la cola para las posiciones crudas del avión.
     * Propiedades:
     * - name: POSICION_QUEUE
     * - durable: true (La cola sobrevivirá a los reinicios del broker. ES CRÍTICO para facturación).
     */
    @Bean
    fun rawPositionQueue(): Queue {
        return Queue(POSICION_QUEUE, true)
    }

    /**
     * Declara la cola para los eventos que deben ser acumulados para la facturación.
     * Propiedades:
     * - name: FACTURACION_QUEUE
     * - durable: true (También debe ser duradera para no perder datos de facturación).
     */
    @Bean
    fun billingAccumulationQueue(): Queue {
        return Queue(FACTURACION_QUEUE, true)
    }

    // Nota: Por simplicidad, estamos usando el Exchange por defecto (Default Exchange)
    // de RabbitMQ, que rutea automáticamente al nombre de la cola.
}