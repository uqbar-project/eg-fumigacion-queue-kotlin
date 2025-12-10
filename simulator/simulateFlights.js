// Script de Node.js para simular un avión enviando posiciones a RabbitMQ.
// Actúa como proceso ingestor, también llamado productor
import amqp from 'amqplib'

// --- Configuración de la Cola ---
// Usamos la misma cola definida en el Geoprocesador de Kotlin
const RABBITMQ_URL = 'amqp://localhost'
const QUEUE_NAME = 'q.posicion.raw'

// --- Configuración del Vuelo ---
const AVION_ID = 42
const VUELO_ID = `VUELO-${Date.now()}`
const INTERVALO_MS = 500 // Envío de posición cada 0.5 segundos (simulando "cada x segundos")

// Coordenadas iniciales (simulando un campo de fumigación)
// Usaremos una latitud/longitud de ejemplo en el sur de Argentina.
const LATITUD_BASE = -35.0
const LONGITUD_BASE = -60.0
// Rango de movimiento (0.005 grados es un cuadrado de ~550 metros)
const RANGE = 0.005

// 5 minutos
const TIEMPO_SIMULACION = 5 * 60 * 1000

// --- Funciones de Simulación ---

/**
 * Genera una posición geográfica aleatoria dentro de un rango.
 * @returns {object} Un objeto con latitud y longitud.
 */
function generarPosicionAleatoria() {
    const latitud = LATITUD_BASE + (Math.random() - 0.5) * RANGE
    const longitud = LONGITUD_BASE + (Math.random() - 0.5) * RANGE
    return { latitud, longitud }
}

async function simularVuelo() {
    let connection
    let channel

    try {
        // Conexión al broker
        connection = await amqp.connect(RABBITMQ_URL)
        channel = await connection.createChannel()

        // Aseguramos que la cola exista antes de enviar
        await channel.assertQueue(QUEUE_NAME, { durable: true })

        console.log(`✈️ SIMULADOR INICIADO - Vuelo: ${VUELO_ID} (ID: ${AVION_ID})`)
        console.log(`Enviando posiciones cada ${INTERVALO_MS}ms a la cola: ${QUEUE_NAME}`)

        // Bucle de envío de mensajes
        const intervalId = setInterval(() => {
            const { latitud, longitud } = generarPosicionAleatoria()

            const mensaje = {
                avionId: AVION_ID,
                vueloId: VUELO_ID,
                timestamp: new Date().toISOString(), // Formato ISO para el Instant de Kotlin
                latitud: parseFloat(latitud.toFixed(6)),
                longitud: parseFloat(longitud.toFixed(6)),
            }

            // Convertir el objeto a buffer JSON
            const mensajeBuffer = Buffer.from(JSON.stringify(mensaje))

            // Enviar el mensaje a la cola
            channel.sendToQueue(QUEUE_NAME, mensajeBuffer, { persistent: true })

            console.log(` [${new Date().toLocaleTimeString()}] Posición enviada: (${mensaje.latitud}, ${mensaje.longitud})`)

        }, INTERVALO_MS)

        // Opcional: Detener la simulación después de un tiempo (ej: 30 segundos)
        setTimeout(() => {
            clearInterval(intervalId)
            channel.close()
            connection.close()
            console.log('\n🛑 SIMULACIÓN DETENIDA después de 30 segundos.')
            process.exit(0)
        }, TIEMPO_SIMULACION)

    } catch (error) {
        console.error('❌ Error fatal en RabbitMQ o conexión:', error)
        if (connection) connection.close()
        process.exit(1)
    }
}

simularVuelo()