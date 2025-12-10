package ar.edu.unsam.fumigacion.config

// La cola que escucha este consumidor
private const val POSICION_QUEUE = "q.posicion.raw"
// La cola a donde envía el resultado para facturación
private val FACTURACION_QUEUE = "q.facturacion.totales"
