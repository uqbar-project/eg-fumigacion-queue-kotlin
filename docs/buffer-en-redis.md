
## Buffer de Redis

Como aproximadamente cada segundo nos llega información de un avión que está en vuelo, utilizamos una cola de RabbitMQ. Tenemos un proceso que mediante las coordenadas puede identificar al cliente, operación que si bien implica un costo tiene dos ventajas: 1. es de lectura, 2. podríamos cachear la información de la base porque tiene baja frecuencia de actualización.

Para actualizar la información de facturación podríamos:

- guardar los datos en la base relacional, a un costo muy alto: son operaciones de actualización, necesitamos garantizar la transaccionalidad y eso va a introducir demoras y una degradación de la performance de nuestra base para otras consultas
- almacenar la información temporal en memoria, para luego volcarla en la base relacional. La ventaja: trabajar con información en memoria es mucho más rápido, pero necesitamos que la población de clientes no exceda la memoria máxima de nuestro servidor. Trabajar con servidores en cluster también puede suponer un inconveniente de concurrencia al guardar la información en la base.
- utilizar un esquema híbrido memoria/disco con una solución clave/valor, como Redis. La ventaja: contaremos con una buena performance, el uso de memoria/disco es transparente para nosotros.

### Decisiones de diseño

La decisión más importante a tomar es cuál será nuestra clave con la que accederemos y actualizaremos la información.

Si elegimos el id de cliente, y almacenamos la cantidad de minutos...

| Id cliente | Segundos |
|------------|----------|
| 1          | 38       |
| 4          | 25       |

... el problema es cómo hacemos el corte, qué involucran los 38 segundos del primer cliente: es un vuelo, son muchos? Además cuando necesitemos generar la factura, no deberíamos pausar el procesamiento de la queue?

Utilizaremos como clave nuestro id de vuelo y luego el id de cliente y los segundos transcurridos. De yapa también almacenaremos el primer y el último timestamp que recibamos (no nos importa si el avión entra o sale varias veces, la cantidad de segundos es lo importante para facturar).

![clave por vuelo en Redis](../images/redis-vuelo-cliente-key.png)

