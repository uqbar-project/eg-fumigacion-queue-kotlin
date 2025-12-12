package ar.edu.unsam.fumigacion.service

import ar.edu.unsam.fumigacion.config.FACTURACION_QUEUE
import ar.edu.unsam.fumigacion.config.POSICION_QUEUE
import ar.edu.unsam.fumigacion.domain.Cliente
import ar.edu.unsam.fumigacion.domain.PosicionAvion
import ar.edu.unsam.fumigacion.domain.TiempoFumigacionCliente
import ar.edu.unsam.fumigacion.repository.ClienteRepository
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
class GeoprocesadorParcelasService(private val rabbitTemplate: RabbitTemplate) {

    @Autowired
    private lateinit var clienteRepository: ClienteRepository

    // 💡 Usa la anotación @RabbitListener para escuchar automáticamente la cola.
    @RabbitListener(queues = [POSICION_QUEUE])
    @Transactional
    fun procesarPosicion(posicion: PosicionAvion) {

        // --- 1. Lógica de Geoposicionamiento ---
        val cliente = identificarClientePorUbicacion(posicion.longitud, posicion.latitud)

        if (cliente != null) {
            // Asumiendo que el avión estuvo 5 segundos en esta parcela (intervalo entre pings)
            val duracion = Duration.ofSeconds(5).seconds

            val tiempo = TiempoFumigacionCliente(
                vueloId = posicion.vueloId,
                clienteId = cliente.id,
                duracionSegundos = duracion,
                timestampFin = posicion.timestamp
            )

            // --- 2. Enviar a la siguiente cola para acumular ---
            rabbitTemplate.convertAndSend(FACTURACION_QUEUE, tiempo)
            println("▶️ Vuelo [${posicion.vueloId}] detectado en parcela de cliente ${cliente.razonSocial}. Enviado a facturación.")
        } else {
            println("▶️ Vuelo [${posicion.vueloId}] fuera de parcelas, posición descartada.")
        }
    }

    // Simula la lógica de verificación (debería usar un servicio GIS o una base de datos)
    @Transactional(readOnly = true)
    fun identificarClientePorUbicacion(x: Double, y: Double): Cliente? {
        return clienteRepository.findByCoordenadasContienePunto(x, y)
    }
}