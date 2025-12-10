package ar.edu.unsam.fumigacion.service

import ar.edu.unsam.fumigacion.config.RabbitMQConfig.Companion.FACTURACION_QUEUE
import ar.edu.unsam.fumigacion.config.RabbitMQConfig.Companion.POSICION_QUEUE
import ar.edu.unsam.fumigacion.domain.PosicionAvion
import ar.edu.unsam.fumigacion.domain.TiempoFumigacionCliente
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class GeoprocesadorParcelasService(private val rabbitTemplate: RabbitTemplate) {

    // 💡 Usa la anotación @RabbitListener para escuchar automáticamente la cola.
    @RabbitListener(queues = [POSICION_QUEUE])
    fun procesarPosicion(posicion: PosicionAvion) {

        // --- 1. Lógica de Geoposicionamiento ---
        val clienteId = determinarClientePorCoordenada(posicion.latitud, posicion.longitud)

        if (clienteId != null) {
            // Asumiendo que el avión estuvo 5 segundos en esta parcela (intervalo entre pings)
            val duracion = Duration.ofSeconds(5).seconds

            val tiempo = TiempoFumigacionCliente(
                vueloId = posicion.vueloId,
                clienteId = clienteId,
                duracionSegundos = duracion,
                timestampFin = posicion.timestamp
            )

            // --- 2. Enviar a la siguiente cola para acumular ---
            rabbitTemplate.convertAndSend(FACTURACION_QUEUE, tiempo)
            println("▶️ Vuelo [${posicion.vueloId}] detectado en parcela de cliente ${clienteId}. Enviado a facturación.")
        } else {
            println("▶️ Vuelo [${posicion.vueloId}] fuera de parcelas, posición descartada.")
        }
    }

    // Simula la lógica de verificación (debería usar un servicio GIS o una base de datos)
    private fun determinarClientePorCoordenada(lat: Double, lon: Double): Long? {
        // Lógica de ejemplo: si la latitud es positiva, pertenece al cliente 101.
        return if (lat > 0) 101L else null
    }
}