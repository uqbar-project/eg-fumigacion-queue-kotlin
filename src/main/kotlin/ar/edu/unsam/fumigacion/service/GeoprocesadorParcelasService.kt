package ar.edu.unsam.fumigacion.service

import ar.edu.unsam.fumigacion.config.POSICION_DLQ
import ar.edu.unsam.fumigacion.config.POSICION_DLQ_EXCHANGE
import ar.edu.unsam.fumigacion.config.POSICION_QUEUE
import ar.edu.unsam.fumigacion.domain.Cliente
import ar.edu.unsam.fumigacion.dto.PosicionAvion
import ar.edu.unsam.fumigacion.repository.ClienteRepository
import ar.edu.unsam.fumigacion.repository.FumigacionRepository
import com.rabbitmq.client.Channel
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.AmqpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GeoprocesadorParcelasService(
    private val fumigacionRepository: FumigacionRepository,
    private val rabbitTemplate: RabbitTemplate
) {

    @Autowired
    private lateinit var clienteRepository: ClienteRepository

    @RabbitListener(queues = [POSICION_QUEUE], ackMode = "MANUAL")
    @Transactional
    fun procesarPosicion(
        posicion: PosicionAvion,
        channel: Channel,
        @Header(AmqpHeaders.DELIVERY_TAG) tag: Long,
        @Header(name = "x-death", required = false) xDeath: List<Map<String, Any>>?
    ) {
        try {
            val cliente = identificarClientePorUbicacion(
                posicion.longitud,
                posicion.latitud
            )

            if (cliente != null) {
                fumigacionRepository.registrarPaso(
                    posicion.vueloId,
                    cliente.id,
                    posicion.timestamp
                )
                println("Confirmamos vuelo a ${cliente.id} - ${cliente.razonSocial}")
            } else {
                // Simulamos error para que vaya a la DLQ
                // throw IllegalArgumentException("Invalid client")
                println("Volando en zona sin clientes")
            }
            channel.basicAck(tag, false)
        } catch (ex: Exception) {
            val retryCount = xDeath
                ?.firstOrNull { it["queue"] == POSICION_QUEUE }
                ?.get("count")
                ?.toString()
                ?.toLong() ?: 0

            if (retryCount >= 3) {
                println("☠️ Mensaje enviado a DLQ luego de $retryCount reintentos")
                // Publicar manualmente a DLQ (porque dead-letter de posicionQueue va a retry exchange)
                rabbitTemplate.convertAndSend(POSICION_DLQ_EXCHANGE, POSICION_DLQ, posicion)
                channel.basicAck(tag, false)
            } else {
                println("🔁 Retry #${retryCount + 1} (esperando 10s)")
                // Nack sin requeue → va a dead-letter-exchange (retry queue con TTL)
                channel.basicNack(tag, false, false)
            }
        }
    }

    // Simula la lógica de verificación (debería usar un servicio GIS o una base de datos)
    @Transactional(readOnly = true)
    fun identificarClientePorUbicacion(x: Double, y: Double): Cliente? {
        return clienteRepository.findByCoordenadasContienePunto(x, y)
    }

}