package ar.edu.unsam.fumigacion.service

import ar.edu.unsam.fumigacion.config.FINISHED_FLIGHT_QUEUE
import ar.edu.unsam.fumigacion.domain.Factura
import ar.edu.unsam.fumigacion.dto.VueloTerminado
import ar.edu.unsam.fumigacion.repository.ClienteRepository
import ar.edu.unsam.fumigacion.repository.FacturaRepository
import ar.edu.unsam.fumigacion.repository.FumigacionRepository
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.support.AmqpHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service
import com.rabbitmq.client.Channel
import org.springframework.transaction.annotation.Transactional

@Service
class FacturadorService(
    private val fumigacionRepository: FumigacionRepository,
    private val clienteRepository: ClienteRepository,
    private val facturaRepository: FacturaRepository,
) {

    @RabbitListener(queues = [FINISHED_FLIGHT_QUEUE], ackMode = "MANUAL")
    @Transactional
    fun procesarVueloTerminado(
        vueloTerminado: VueloTerminado,
        channel: Channel,
        @Header(AmqpHeaders.DELIVERY_TAG) tag: Long
    ) {
        println("Vuelo terminado: ${vueloTerminado.vueloId}")
        try {
            val facturas = fumigacionRepository.obtenerDatosDeVuelo(vueloTerminado.vueloId).map {
                fumigacionCliente -> Factura(
                    descripcion = "Servicio de fumigación - ${vueloTerminado.vueloId}",
                    total = fumigacionCliente.cantidad * 150.0,
                    // no hace SELECT, solo crea un proxy (no lo usamos así que no dispara queries a la BD)
                    cliente = clienteRepository.getReferenceById(fumigacionCliente.clienteId),
                )
            }
            println("Facturación - ${facturas.map { it.total }.joinToString(", ")}")
            // ver la propiedad batch_size de application.yml para jpa:hibernate
            // guarda en lotes de x elementos
            facturaRepository.saveAll(facturas)

            // Eliminamos los datos del buffer de Redis
            fumigacionRepository.borrarDatosDeVuelo(vueloTerminado.vueloId)

            channel.basicAck(tag, false)
        } catch (ex: Exception) {
            // directo a DLQ
            channel.basicReject(tag, false)
        }
    }

}