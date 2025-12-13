package ar.edu.unsam.fumigacion.repository

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Repository
class RedisFumigacionRepository(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val stringRedisTemplate: StringRedisTemplate
) {
    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH:mm")
        private val INCREMENT_SCRIPT = """
            local current = redis.call('HGET', KEYS[1], ARGV[1])
            if current == false then
                redis.call('HSET', KEYS[1], ARGV[1], 1)
                return 1
            else
                return redis.call('HINCRBY', KEYS[1], ARGV[1], 1)
        """.trimIndent()
    }

//    fun incrementCounter(clienteId: Long): Long {
//        val key = getCurrentMinuteKey()
//        val script = RedisScript.of<Long>(INCREMENT_SCRIPT, Long::class.java)
//        return redisTemplate.execute(script, listOf(key), clienteId.toString()) ?: 0
//    }

    fun incrementCounter(clienteId: Long): Long? {
        val key = "contador:cliente:$clienteId"
        return stringRedisTemplate.opsForValue().increment(key)
    }

    fun getCountsForMinute(minuteKey: String): Map<String, String>? {
        return redisTemplate.opsForHash<String, String>().entries(minuteKey)
    }

    private fun getCurrentMinuteKey(): String {
        return "fumigacion:${LocalDateTime.now().format(DATE_TIME_FORMATTER)}"
    }
}
