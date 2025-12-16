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
}
