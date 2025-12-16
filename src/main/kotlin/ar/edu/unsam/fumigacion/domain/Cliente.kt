package ar.edu.unsam.fumigacion.domain

import jakarta.persistence.*

/**
 * Representa las coordenadas de un rectángulo (parcela) dentro del sistema,
 * que puede ser usado como Value Object de JPA.
 */
@Embeddable
data class Coordenadas(
    @Column(nullable = false) val xInicial: Double,
    @Column(nullable = false) val yInicial: Double,
    @Column(nullable = false) val xFinal: Double,
    @Column(nullable = false) val yFinal: Double
)

/**
 * Entidad principal que representa a un Cliente.
 * Contiene los datos de la parcela y las facturas generadas.
 * * NOTA: Los nombres de columna de las coordenadas están mapeados
 * a 'parcela_x_inicio', etc., para que la consulta nativa PostGIS funcione.
 */
@Entity
@Table(name = "clientes")
class Cliente(
    @Column(nullable = false)
    var razonSocial: String = "",

    @Embedded // Permite incrustar la clase Coordenadas en la tabla 'cliente'
    @AttributeOverrides(
        AttributeOverride(name = "xInicial", column = Column(name = "parcela_x_inicio")),
        AttributeOverride(name = "yInicial", column = Column(name = "parcela_y_inicio")),
        AttributeOverride(name = "xFinal", column = Column(name = "parcela_x_fin")),
        AttributeOverride(name = "yFinal", column = Column(name = "parcela_y_fin"))
    )
    var parcela: Coordenadas? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

}