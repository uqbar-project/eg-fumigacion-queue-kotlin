package ar.edu.unsam.fumigacion.domain

import java.time.Instant

data class PosicionAvion(
    val avionId: Long,
    val vueloId: String,
    val timestamp: Instant,
    val latitud: Double,
    val longitud: Double
)