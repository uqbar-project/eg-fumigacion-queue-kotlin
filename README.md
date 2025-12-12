
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
nvm use
npm install
npm run dev
```

En la consola vas a ver la información al azar que se produce y se envía a la cola que la almacena:

```bash
✈️ Vuelo VUELO-1765505042727 iniciando. Pasando por 3 clientes en loop.
[11:04:03 PM] Posición enviada → (-35, -60) → hacia Cliente 2
[11:04:03 PM] Posición enviada → (-35.000035, -60.000035) → hacia Cliente 2
[11:04:04 PM] Posición enviada → (-35.000071, -60.000071) → hacia Cliente 2
```

El proceso simula un vuelo que pasa por 3 clientes conocidos en nuestra base Postgres.

Una vez finalizado el proceso podés ver la información generada en la cola siguiendo [estos pasos](./docs/como-ver-info-queue.md)

## Geoprocesador de Parcelas

TODO
