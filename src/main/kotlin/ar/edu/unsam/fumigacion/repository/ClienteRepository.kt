package ar.edu.unsam.fumigacion.repository

import ar.edu.unsam.fumigacion.domain.Cliente
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ClienteRepository : JpaRepository<Cliente, Long> {

    /**
     * Implementación de alta performance usando SQL Nativo y PostGIS.
     * Determina si un punto de coordenadas (x, y) está contenido
     * dentro de la parcela rectangular (Envelope) del cliente.
     * * NOTA: La consulta usa los nombres de columna definidos en Cliente.kt
     * mediante @AttributeOverrides: parcela_x_inicio, parcela_y_inicio, etc.
     *
     * @param x Coordenada X del punto a verificar.
     * @param y Coordenada Y del punto a verificar.
     * @return Lista de clientes cuyas parcelas contienen el punto (x, y).
     */
    @Query(nativeQuery = true, value = """
        SELECT * FROM clientes c 
        WHERE 
            -- 1. ST_MakeEnvelope crea una geometría rectangular a partir de las 4 coordenadas.
            ST_Contains(
                ST_MakeEnvelope(
                    c.parcela_x_inicio, c.parcela_y_inicio, 
                    c.parcela_x_fin, c.parcela_y_fin
                ), 
                -- 2. ST_MakePoint crea la geometría de un punto (la ubicación actual).
                ST_MakePoint(:x, :y)
            )
        LIMIT 1
    """)
    fun findByCoordenadasContienePunto(@Param("x") x: Double, @Param("y") y: Double): Cliente?
}