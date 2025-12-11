
# Fumigación - Ejemplo con Message Queue

Trabajamos en el área de sistemas de una empresa que se dedica a la fumigación ecológica de parcelas, para lo cual tenemos aviones que salen a hacer vuelos de fumigación. Nuestro input es que desde que despega hasta que aterriza el avión nos va enviando su geolocalización. Luego nosotros le cobramos un monto calculando en base al tiempo que estuvo sobrevolando el avión por cada una de las parcelas de nuestros clientes.

Necesitamos entonces

- servicios con una alta capacidad, que necesitan no solo durabilidad sino la posibilidad de ser tolerante a fallas (si nuestro servidor del backend esté caído debe procesarlo en otro momento pero de ninguna manera podemos perder el registro sobre los vuelos de los aviones para facturar)
- servicios que necesitan ser igualmente durables, pero que tienen una baja frecuencia de uso, como la que toma los acumulados para finalmente emitir la factura a cada cliente

## Iniciando la queue

```bash
docker compose up -d
```

## Ingestor de datos

Es el proceso que simula el despegue de un avión por lo que va a estar emitiendo coordenadas que simulan el vuelo sobre diferentes parcelas.

Para dispararlo posicionate en la carpeta [simulator](./simulator) y ejecutá

```bash
npm install
npm run dev
```

En la consola vas a ver la información al azar que se produce y se envía a la cola que la almacena:

```bash
✈️ SIMULADOR INICIADO - Vuelo: VUELO-1765412926306 (ID: 42)
Enviando posiciones cada 500ms a la cola: q.posicion.raw
 [9:28:46 PM] Posición enviada: (-35.002261, -60.001388)
 [9:28:47 PM] Posición enviada: (-34.998837, -60.001584)
 [9:28:47 PM] Posición enviada: (-35.002374, -60.000016)
 [9:28:48 PM] Posición enviada: (-34.999176, -59.998629)
 [9:28:48 PM] Posición enviada: (-35.000123, -59.997878)
 [9:28:49 PM] Posición enviada: (-35.001779, -60.000948)
 [9:28:49 PM] Posición enviada: (-35.000935, -59.999905)
 [9:28:50 PM] Posición enviada: (-35.00182, -60.000513)
 [9:28:50 PM] Posición enviada: (-34.997672, -59.998715)
```

Una vez finalizado el proceso podés ver la información generada en la cola siguiendo [estos pasos](./docs/como-ver-info-queue.md)
