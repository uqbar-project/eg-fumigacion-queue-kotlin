package ar.edu.unsam.fumigacion.service

import ar.edu.unsam.fumigacion.config.POSICION_QUEUE
import ar.edu.unsam.fumigacion.domain.Cliente
import ar.edu.unsam.fumigacion.domain.PosicionAvion
import ar.edu.unsam.fumigacion.repository.ClienteRepository
import ar.edu.unsam.fumigacion.repository.RedisFumigacionRepository
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GeoprocesadorParcelasService(
    private val redisFumigacionRepository: RedisFumigacionRepository
) {

    @Autowired
    private lateinit var clienteRepository: ClienteRepository

    @RabbitListener(queues = [POSICION_QUEUE])
    @Transactional
    fun procesarPosicion(posicion: PosicionAvion) {
        // --- 1. Lógica de Geoposicionamiento ---
        val cliente = identificarClientePorUbicacion(posicion.longitud, posicion.latitud)

        if (cliente != null) {
            // Incrementar el contador para este cliente en el minuto actual
            val count = redisFumigacionRepository.incrementCounter(cliente.id!!)
            
            println("▶️ Vuelo [${posicion.vueloId}] detectado en parcela de cliente ${cliente.razonSocial}. " +
                   "Contador actualizado: $count")
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