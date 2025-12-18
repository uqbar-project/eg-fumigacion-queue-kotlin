package ar.edu.unsam.fumigacion.service

import ar.edu.unsam.fumigacion.config.POSICION_QUEUE
import ar.edu.unsam.fumigacion.domain.Cliente
import ar.edu.unsam.fumigacion.domain.Coordenadas
import ar.edu.unsam.fumigacion.dto.PosicionAvion
import ar.edu.unsam.fumigacion.repository.ClienteRepository
import ar.edu.unsam.fumigacion.repository.FumigacionRepository
import com.rabbitmq.client.Channel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Instant

@SpringBootTest(
    properties = [
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.spatial.dialect.postgis.PostgisPG95Dialect"
    ]
)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GeoprocesadorParcelasServiceTest {

    companion object {
        private val postgisImage =
            DockerImageName.parse("postgis/postgis:15-3.4")
                .asCompatibleSubstituteFor("postgres")

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>(postgisImage).apply {
            withDatabaseName("facturacion_db")
            withUsername("postgres")
            withPassword("postgres")
        }

        @Container
        @JvmStatic
        val redis = GenericContainer<Nothing>(DockerImageName.parse("redis:7-alpine")).apply {
            withExposedPorts(6379)
            withCommand("redis-server", "--requirepass", "redisquero")
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }

            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
            registry.add("spring.data.redis.password") { "redisquero" }
        }

        init {
            postgres.start()
            redis.start()
        }
    }


    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var geoprocesadorParcelasService: GeoprocesadorParcelasService

    @Autowired
    private lateinit var clienteRepository: ClienteRepository

    @Autowired
    private lateinit var fumigacionRepository: FumigacionRepository

    @MockitoSpyBean
    private lateinit var spyFumigacionRepository: FumigacionRepository

    @BeforeAll
    fun enablePostgisExtension() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis")
    }

    @BeforeEach
    fun cleanDb() {
        clienteRepository.deleteAll()
    }

    @Test
    fun `cuando la posicion cae dentro de una parcela registra paso y hace ack`() {
        val cliente = Cliente(
            razonSocial = "ACME",
            parcela = Coordenadas(0.0, 0.0, 10.0, 10.0)
        )
        clienteRepository.save(cliente)

        val vueloId = "vuelo-it-1"
        fumigacionRepository.borrarDatosDeVuelo(vueloId)

        val timestamp = Instant.parse("2025-01-01T00:00:00Z")
        val posicion = PosicionAvion(
            avionId = 1,
            vueloId = vueloId,
            timestamp = timestamp,
            latitud = 5.0,
            longitud = 5.0
        )

        val channel = mock(Channel::class.java)
        val tag = 10L

        geoprocesadorParcelasService.procesarPosicion(posicion, channel, tag, null)

        verify(channel).basicAck(tag, false)

        val datos = fumigacionRepository.obtenerDatosDeVuelo(vueloId)
        assertEquals(1, datos.size)
        val fumigacion = datos.first()
        assertEquals(cliente.id, fumigacion.clienteId)
        assertEquals(1L, fumigacion.cantidad)
        assertEquals(timestamp, fumigacion.desde)
        assertEquals(timestamp, fumigacion.hasta)
    }

    @Test
    fun `cuando la posicion no coincide con ningun cliente hace ack y no registra fumigacion`() {
        val vueloId = "vuelo-it-2"
        fumigacionRepository.borrarDatosDeVuelo(vueloId)

        val posicion = PosicionAvion(
            avionId = 1,
            vueloId = vueloId,
            timestamp = Instant.parse("2025-01-01T00:00:00Z"),
            latitud = 50.0,
            longitud = 50.0
        )

        val channel = mock(Channel::class.java)
        val tag = 11L

        geoprocesadorParcelasService.procesarPosicion(posicion, channel, tag, null)

        verify(channel).basicAck(tag, false)

        val datos = fumigacionRepository.obtenerDatosDeVuelo(vueloId)
        assertTrue(datos.isEmpty())
    }

    @Test
    fun `ante una excepcion y sin xDeath hace nack para retry`() {
        val cliente = Cliente(
            razonSocial = "ACME",
            parcela = Coordenadas(0.0, 0.0, 10.0, 10.0)
        )
        clienteRepository.save(cliente)

        val vueloId = "vuelo-it-3"
        val timestamp = Instant.parse("2025-01-01T00:00:00Z")

        doThrow(RuntimeException("boom"))
            .`when`(spyFumigacionRepository)
            .registrarPaso(
                vueloId,
                cliente.id,
                timestamp
            )

        val posicion = PosicionAvion(
            avionId = 1,
            vueloId = vueloId,
            timestamp = timestamp,
            latitud = 5.0,
            longitud = 5.0
        )

        val channel = mock(Channel::class.java)
        val tag = 12L

        geoprocesadorParcelasService.procesarPosicion(posicion, channel, tag, null)

        verify(channel).basicNack(tag, false, false)
    }

    @Test
    fun `ante una excepcion con 3 reintentos hace reject sin requeue`() {
        val cliente = Cliente(
            razonSocial = "ACME",
            parcela = Coordenadas(0.0, 0.0, 10.0, 10.0)
        )
        clienteRepository.save(cliente)

        val vueloId = "vuelo-it-4"
        val timestamp = Instant.parse("2025-01-01T00:00:00Z")

        doThrow(RuntimeException("boom"))
            .`when`(spyFumigacionRepository)
            .registrarPaso(
                vueloId,
                cliente.id,
                timestamp
            )

        val posicion = PosicionAvion(
            avionId = 1,
            vueloId = vueloId,
            timestamp = timestamp,
            latitud = 5.0,
            longitud = 5.0
        )

        val xDeath: List<Map<String, Any>> = listOf(
            mapOf(
                "queue" to POSICION_QUEUE,
                "count" to 3
            )
        )

        val channel = mock(Channel::class.java)
        val tag = 13L

        geoprocesadorParcelasService.procesarPosicion(posicion, channel, tag, xDeath)

        verify(channel).basicReject(tag, false)
    }
}
