package ar.edu.unsam.fumigacion.service

import ar.edu.unsam.fumigacion.domain.Cliente
import ar.edu.unsam.fumigacion.domain.Coordenadas
import ar.edu.unsam.fumigacion.dto.VueloTerminado
import ar.edu.unsam.fumigacion.repository.ClienteRepository
import ar.edu.unsam.fumigacion.repository.FacturaRepository
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
class FacturadorServiceTest {

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
    private lateinit var facturadorService: FacturadorService

    @Autowired
    private lateinit var fumigacionRepository: FumigacionRepository

    @Autowired
    private lateinit var clienteRepository: ClienteRepository

    @Autowired
    private lateinit var facturaRepository: FacturaRepository

    @MockitoSpyBean
    private lateinit var spyFumigacionRepository: FumigacionRepository

    @BeforeAll
    fun enablePostgisExtension() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis")
    }

    @BeforeEach
    fun cleanDb() {
        facturaRepository.deleteAll()
        clienteRepository.deleteAll()
    }

    @Test
    fun `cuando termina el vuelo crea facturas y limpia el buffer en redis y hace ack`() {
        val cliente1 = clienteRepository.save(
            Cliente(
                razonSocial = "ACME",
                parcela = Coordenadas(0.0, 0.0, 10.0, 10.0)
            )
        )
        val cliente2 = clienteRepository.save(
            Cliente(
                razonSocial = "Globex",
                parcela = Coordenadas(20.0, 20.0, 30.0, 30.0)
            )
        )

        val vueloId = "vuelo-fact-it-1"
        fumigacionRepository.borrarDatosDeVuelo(vueloId)
        reset(spyFumigacionRepository)

        // cliente1: 2 pasos => 2 * 150
        fumigacionRepository.registrarPaso(vueloId, cliente1.id, Instant.parse("2025-01-01T00:00:00Z"))
        fumigacionRepository.registrarPaso(vueloId, cliente1.id, Instant.parse("2025-01-01T00:00:01Z"))

        // cliente2: 1 paso => 1 * 150
        fumigacionRepository.registrarPaso(vueloId, cliente2.id, Instant.parse("2025-01-01T00:00:00Z"))

        val channel = mock(Channel::class.java)
        val tag = 20L

        facturadorService.procesarVueloTerminado(VueloTerminado(vueloId), channel, tag)

        verify(channel).basicAck(tag, false)
        verify(spyFumigacionRepository).borrarDatosDeVuelo(vueloId)

        assertEquals(2L, facturaRepository.count())

        val totalesPorCliente: Map<Long, Double> = jdbcTemplate
            .queryForList("SELECT cliente_id, total FROM facturas")
            .associate { row ->
                val clienteId = (row["cliente_id"] as Number).toLong()
                val total = (row["total"] as Number).toDouble()
                clienteId to total
            }

        assertEquals(300.0, totalesPorCliente[cliente1.id])
        assertEquals(150.0, totalesPorCliente[cliente2.id])

        assertTrue(fumigacionRepository.obtenerDatosDeVuelo(vueloId).isEmpty())
    }

    @Test
    fun `si falla el procesamiento manda a DLQ (reject) y no limpia el buffer`() {
        val cliente = clienteRepository.save(
            Cliente(
                razonSocial = "ACME",
                parcela = Coordenadas(0.0, 0.0, 10.0, 10.0)
            )
        )

        val vueloId = "vuelo-fact-it-2"
        fumigacionRepository.borrarDatosDeVuelo(vueloId)
        reset(spyFumigacionRepository)
        fumigacionRepository.registrarPaso(vueloId, cliente.id, Instant.parse("2025-01-01T00:00:00Z"))

        doThrow(RuntimeException("boom"))
            .`when`(spyFumigacionRepository)
            .obtenerDatosDeVuelo(vueloId)

        val channel = mock(Channel::class.java)
        val tag = 21L

        facturadorService.procesarVueloTerminado(VueloTerminado(vueloId), channel, tag)

        verify(channel).basicReject(tag, false)
        verify(spyFumigacionRepository, never()).borrarDatosDeVuelo(vueloId)
        assertEquals(0L, facturaRepository.count())
    }
}
