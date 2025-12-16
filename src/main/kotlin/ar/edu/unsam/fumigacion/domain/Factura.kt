package ar.edu.unsam.fumigacion.domain

import jakarta.persistence.*
import java.time.LocalDate

/**
 * Entidad que representa una Factura, asociada a un Cliente.
 */
@Entity
@Table(name = "facturas")
class Factura(
    @Column(nullable = false)
    var fechaEmision: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    var descripcion: String = "",

    @Column(nullable = false)
    var total: Double = 0.0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    var cliente: Cliente,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}