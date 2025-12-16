package ar.edu.unsam.fumigacion.repository

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class RedisFumigacionRepository(
    private val stringRedisTemplate: StringRedisTemplate
) {

    fun registrarPaso(
        vueloId: String,
        clienteId: Long,
        timestamp: Instant
    ): Long {

        val key = "fumigacion:vuelo:$vueloId:cliente:$clienteId"
        val ops = stringRedisTemplate.opsForHash<String, String>()

        // contador de segundos
        val count = ops.increment(key, "cantidad", 1)

        // primera vez
        ops.putIfAbsent(key, "desde", timestamp.toString())

        // última vez (siempre se pisa)
        ops.put(key, "hasta", timestamp.toString())

        return count
    }

    fun obtenerDatosDeVuelo(vueloId: String): List<FumigacionCliente> {
        val pattern = "fumigacion:vuelo:$vueloId:cliente:*"
        val keys = stringRedisTemplate.keys(pattern)

        if (keys.isEmpty()) return emptyList()

        val ops = stringRedisTemplate.opsForHash<String, String>()

        return keys.mapNotNull { key ->
            val data = ops.entries(key)
            if (data.isEmpty()) return@mapNotNull null

            val clienteId = key.substringAfterLast(":").toLong()

            FumigacionCliente(
                clienteId = clienteId,
                cantidad = data["cantidad"]?.toLong() ?: 0L,
                desde = data["desde"]?.let { Instant.parse(it) },
                hasta = data["hasta"]?.let { Instant.parse(it) }
            )
        }
    }

    fun borrarDatosDeVuelo(vueloId: String) {
        val pattern = "fumigacion:vuelo:$vueloId:cliente:*"
        val keys = stringRedisTemplate.keys(pattern)

        if (keys.isNotEmpty()) {
            stringRedisTemplate.delete(keys)
        }
    }

}

data class FumigacionCliente(
    val clienteId: Long,
    val cantidad: Long,
    val desde: Instant?,
    val hasta: Instant?
)
