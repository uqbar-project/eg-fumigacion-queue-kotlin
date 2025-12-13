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

        if (clienteRepo.count() > 0L) return

        /*
         * Recorrido del avión (script JS):
         *
         * ORIGEN  : lat -35.0000 / lng -60.0000
         * DESTINO : lat -34.9970 / lng -60.0030
         *
         * Dirección: sureste -> noroeste
         */

        val clientes = listOf(

            // ✈️ Primer cliente (no entra enseguida)
            Cliente(
                razonSocial = "Cliente 1",
                parcela = Coordenadas(
                    xInicial = -34.9996, // lat
                    yInicial = -60.0008, // lng
                    xFinal   = -34.9990,
                    yFinal   = -60.0016
                )
            ),

            // 🌾 Cliente central (más tiempo dentro)
            Cliente(
                razonSocial = "Cliente 2",
                parcela = Coordenadas(
                    xInicial = -34.9990,
                    yInicial = -60.0016,
                    xFinal   = -34.9982,
                    yFinal   = -60.0024
                )
            ),

            // 🌾 Último cliente antes de salir
            Cliente(
                razonSocial = "Cliente 3",
                parcela = Coordenadas(
                    xInicial = -34.9982,
                    yInicial = -60.0025,
                    xFinal   = -34.9969,
                    yFinal   = -60.0031
                )
            )
        )

        clienteRepo.saveAll(clientes)
        println(">>> \uD83C\uDF3E Clientes precargados")
    }
}
