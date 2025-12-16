package ar.edu.unsam.fumigacion.repository

import ar.edu.unsam.fumigacion.domain.Factura
import org.springframework.data.jpa.repository.JpaRepository

interface FacturaRepository : JpaRepository<Factura, Long>