package ar.edu.unsam.fumigacion

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EgFumigacionQueueKotlinApplication

fun main(args: Array<String>) {
	runApplication<EgFumigacionQueueKotlinApplication>(*args)
}
