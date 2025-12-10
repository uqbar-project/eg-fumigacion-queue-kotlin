package ar.edu.unsam.fumigacion.domain

import java.time.Instant

data class TiempoFumigacionCliente(
    val vueloId: String,
    val clienteId: Long,
    val duracionSegundos: Long,
    val timestampFin: Instant // Para tracking
)
