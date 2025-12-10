package ar.edu.unsam.fumigacion.service

import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class FacturadorService() {

    @RabbitListener(queues = [FACTURACION_QUEUE])
    fun acumularTiempo(tiempo: TiempoCliente) {

        // --- 1. Lógica de Agrupación y Persistencia ---

        // Buscar el registro de facturación para (vueloId, clienteId)
        // val factura = facturaRepository.findByVueloIdAndClienteId(tiempo.vueloId, tiempo.clienteId)

        // Si existe: factura.minutosAcumulados += tiempo.duracionSegundos
        // Si no existe: crear nuevo registro

        // 2. Persistir
        // facturaRepository.save(factura)

        println("💰 Facturación: Acumulando ${tiempo.duracionSegundos}s para cliente ${tiempo.clienteId} en vuelo ${tiempo.vueloId}")

        // Si la base de datos falla, RabbitMQ reintentará la entrega hasta que tenga éxito,
        // ¡asegurando que no pierdes un solo segundo de facturación!
    }
}