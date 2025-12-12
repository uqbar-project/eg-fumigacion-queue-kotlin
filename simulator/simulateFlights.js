// Simulador de vuelo para RabbitMQ con paso garantizado por 3 clientes
import amqp from "amqplib"

// --- Config RabbitMQ ---
const RABBITMQ_URL = "amqp://localhost"
const QUEUE_NAME = "q.posicion.raw"

// --- Utilidades de geometría ---
function rectangle(x1, y1, x2, y2) {
    return {
        minLat: Math.min(x1, x2),
        maxLat: Math.max(x1, x2),
        minLng: Math.min(y1, y2),
        maxLng: Math.max(y1, y2)
    }
}

function getCenter(rectangle) {
    return {
        lat: (rectangle.minLat + rectangle.maxLat) / 2,
        lng: (rectangle.minLng + rectangle.maxLng) / 2
    }
}

const CLIENTES = [
    { id: 1, parcela: rectangle(-35.0005, -60.0005, -34.9995, -59.9995) },
    { id: 2, parcela: rectangle(-35.002, -60.002, -35.001, -60.001) },
    { id: 3, parcela: rectangle(-34.998, -60.003, -34.997, -60.002) }
]

// --- Ruta: centro de cada parcela ---
const WAYPOINTS = CLIENTES.map(cliente => getCenter(cliente.parcela))

// Empezar en el primer punto
let currentIndex = 0
let currentPos = { ...WAYPOINTS[0] }

// Speed del avión por tick (0.00005 ≈ 5 metros)
const STEP = 0.00005

function moveTowards(target) {
    let dx = target.lat - currentPos.lat
    let dy = target.lng - currentPos.lng

    const dist = Math.sqrt(dx*dx + dy*dy)

    if (dist < STEP) {
        // Llegamos, pasamos al siguiente
        currentIndex = (currentIndex + 1) % WAYPOINTS.length
    } else {
        currentPos.lat += (dx / dist) * STEP
        currentPos.lng += (dy / dist) * STEP
    }
}

// --- Simulación ---
const AVION_ID = 42
const VUELO_ID = `VUELO-${Date.now()}`
const INTERVALO_MS = 500

async function simularVuelo() {
    let connection = await amqp.connect(RABBITMQ_URL)
    let channel = await connection.createChannel()
    await channel.assertQueue(QUEUE_NAME, { durable: true })

    console.log(`✈️ Vuelo ${VUELO_ID} iniciando. Pasando por 3 clientes en loop.`)

    setInterval(() => {
        const target = WAYPOINTS[currentIndex]
        moveTowards(target)

        const mensaje = {
            avionId: AVION_ID,
            vueloId: VUELO_ID,
            timestamp: new Date().toISOString(),
            latitud: +currentPos.lat.toFixed(6),
            longitud: +currentPos.lng.toFixed(6),
        }

        channel.sendToQueue(QUEUE_NAME, Buffer.from(JSON.stringify(mensaje)), {
            persistent: true,
            contentType: "application/json",
            priority: 1,
        })

        console.log(
            `[${new Date().toLocaleTimeString()}] Posición enviada → (${mensaje.latitud}, ${mensaje.longitud}) → Cliente ${currentIndex}`
        )
    }, INTERVALO_MS)
}

simularVuelo()
