import amqp from "amqplib"

// --------------------
// Config RabbitMQ
// --------------------
const RABBITMQ_URL = "amqp://localhost"
const QUEUE_NAME = "q.posicion.raw"

// --------------------
// Config vuelo
// --------------------
const AVION_ID = 42
const VUELO_ID = `VUELO-${Date.now()}`

// Origen y destino (lat, lng)
const ORIGEN = {x: -35.0000, y: -60.0000}
const DESTINO = {x: -34.9970, y: -60.0030}

// Velocidad en grados / segundo (~5 m)
const VELOCIDAD = 0.00005

// Cada cuánto publicamos (1 segundo)
const INTERVALO_MS = 1000

// --------------------
// Lógica de movimiento
// --------------------
function crearAvion(origen, destino, velocidad) {
  const dx = destino.x - origen.x
  const dy = destino.y - origen.y
  const distanciaTotal = Math.sqrt(dx * dx + dy * dy)

  return {
    posicion: {...origen},
    destino,
    velocidad,
    ux: dx / distanciaTotal,
    uy: dy / distanciaTotal,
    distanciaRecorrida: 0,
    distanciaTotal
  }
}

function avanzarUnSegundo(avion) {
  if (avion.distanciaRecorrida >= avion.distanciaTotal) {
    avion.posicion.x = avion.destino.x
    avion.posicion.y = avion.destino.y
    return false
  }

  avion.posicion.x += avion.ux * avion.velocidad
  avion.posicion.y += avion.uy * avion.velocidad
  avion.distanciaRecorrida += avion.velocidad

  return true
}

// --------------------
// Simulación + Queue
// --------------------
async function simularVuelo() {
  const connection = await amqp.connect(RABBITMQ_URL)
  const channel = await connection.createChannel()
  await channel.assertQueue(QUEUE_NAME, {durable: true})

  const avion = crearAvion(ORIGEN, DESTINO, VELOCIDAD)

  console.log(`✈️ Vuelo ${VUELO_ID} iniciado`)
  console.log(`Desde (${ORIGEN.x}, ${ORIGEN.y}) → (${DESTINO.x}, ${DESTINO.y})`)

  const interval = setInterval(() => {
    const sigue = avanzarUnSegundo(avion)

    const mensaje = {
      avionId: AVION_ID,
      vueloId: VUELO_ID,
      timestamp: new Date().toISOString(),
      longitud: +avion.posicion.x.toFixed(6),
      latitud: +avion.posicion.y.toFixed(6),
    }

    channel.sendToQueue(QUEUE_NAME, Buffer.from(JSON.stringify(mensaje)), {
      persistent: true, contentType: "application/json", priority: 1,
    })

    console.log(`[${new Date().toLocaleTimeString()}] → (${mensaje.latitud}, ${mensaje.longitud})`)

    if (!sigue) {
      console.log("🛬 Avión llegó al aeroclub destino")
      clearInterval(interval)
      setTimeout(() => {
        channel.close()
        connection.close()
        process.exit(0)
      }, 500)
    }
  }, INTERVALO_MS)
}

simularVuelo().catch(err => {
  console.error("❌ Error en simulación", err)
  process.exit(1)
})
