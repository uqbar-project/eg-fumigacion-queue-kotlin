package ar.edu.unsam.fumigacion.initialization

import ar.edu.unsam.fumigacion.domain.Cliente
import ar.edu.unsam.fumigacion.domain.Coordenadas
import ar.edu.unsam.fumigacion.repository.ClienteRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class FumigacionSeeder(
    private val clienteRepo: ClienteRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {

        if (clienteRepo.count() == 0L) {
            val clientes = listOf(
                Cliente(
                    razonSocial = "Cliente Norte",
                    parcela = Coordenadas(
                        -60.003, -34.998,
                        -59.998, -34.995
                    )
                ),
                Cliente(
                    razonSocial = "Cliente Centro",
                    parcela = Coordenadas(
                        -60.002, -35.002,
                        -59.997, -34.999
                    )
                ),
                Cliente(
                    razonSocial = "Cliente Sur",
                    parcela = Coordenadas(
                        -60.001, -35.004,
                        -59.996, -35.001
                    )
                )
            )

            clienteRepo.saveAll(clientes)
            println(">>> Clientes precargados!")
        }
    }
}
