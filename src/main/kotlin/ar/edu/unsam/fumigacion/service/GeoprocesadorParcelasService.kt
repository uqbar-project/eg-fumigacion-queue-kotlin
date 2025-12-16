package ar.edu.unsam.fumigacion.service

import ar.edu.unsam.fumigacion.config.POSICION_QUEUE
import ar.edu.unsam.fumigacion.domain.Cliente
import ar.edu.unsam.fumigacion.dto.PosicionAvion
import ar.edu.unsam.fumigacion.repository.ClienteRepository
import ar.edu.unsam.fumigacion.repository.RedisFumigacionRepository
import com.rabbitmq.client.Channel
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.support.AmqpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GeoprocesadorParcelasService(
    private val redisFumigacionRepository: RedisFumigacionRepository
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
                redisFumigacionRepository.registrarPaso(
                    posicion.vueloId,
                    cliente.id,
                    posicion.timestamp
                )
                println("Confirmamos vuelo a ${cliente.id} - ${cliente.razonSocial}")
            } else {
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
                channel.basicReject(tag, false)
                // si no tuviera configurada la dead letter exchange el mensaje se pierde
                // pero como está configurada va la DLQ (ver RabbitMQConfig)
                // basicReject con el segundo parámetro en true => vuelve a encolarse (puede
                // ser consumido por el mismo listener o por otro), ojo => puede llevarte a tener
                // un timeout
            } else {
                println("🔁 Retry #${retryCount + 1}")
                channel.basicNack(tag, false, false) // retry
            }
        }
    }

    // Simula la lógica de verificación (debería usar un servicio GIS o una base de datos)
    @Transactional(readOnly = true)
    fun identificarClientePorUbicacion(x: Double, y: Double): Cliente? {
        return clienteRepository.findByCoordenadasContienePunto(x, y)
    }

}