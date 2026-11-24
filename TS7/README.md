# TS7 · Regalo grupal

Práctico de Web II. Tres o más personas pueden proponer productos de DummyJSON y votar desde el navegador. Una pantalla aparte muestra el ranking en vivo. [Consigna de la cátedra](https://unvime-web-2.matiendres.workers.dev/practicos/tp4/) · [Relevamiento](docs/consigna.md) · [Paso a paso](docs/paso-a-paso.md)

## Instalación y ejecución

Desde el directorio que contiene este README, con Node.js 22, 24 o 26 LTS:

```bash
npm ci
npm run build
npm start
```

Abrí `http://127.0.0.1:3000/` en varias pestañas para participar y `http://127.0.0.1:3000/pantalla` en otra para mostrar el ranking. El mismo servidor sirve las dos páginas, el WebSocket en `/ws` y el stream SSE en `/events`. `GET /health` devuelve `{"status":"ok"}`. Al reiniciar el proceso se pierde la lista.

## Configuración

Podés copiar `.env.example` a `.env` o definir las variables en el entorno antes de ejecutar `npm start`. Reiniciá el servidor al cambiarlas.

| Variable | Valor inicial | Uso |
| --- | --- | --- |
| `HOST` | `127.0.0.1` | Interfaz del servidor. |
| `PORT` | `3000` | Puerto de HTTP, WebSocket y SSE. |
| `CATALOG_URL` | `https://dummyjson.com` | URL base a la que se agrega `/products/{id}`. |
| `CATALOG_TIMEOUT_MS` | `2500` | Tiempo máximo por producto, incluida la lectura de la respuesta. Admite de 1 a 30 000 ms. |
| `CATALOG_DELAY_MS` | `0` | Parámetro `delay` para simular demora en DummyJSON. Admite de 0 a 5000 ms. |

Para reproducir la espera, configurá `CATALOG_DELAY_MS=3000` y `CATALOG_TIMEOUT_MS=4500`. Cargá un producto, proponé otro desde una pestaña y votá el primero desde otra durante la consulta. Para provocar un timeout, reiniciá con el mismo delay y `CATALOG_TIMEOUT_MS=1000`, y proponé un producto nuevo.

## Decisiones

- Se recortan espacios de los extremos y se normaliza Unicode a NFC. El nombre admite de 1 a 30 caracteres Unicode sin caracteres de control. Dos personas conectadas no pueden usar el mismo nombre aunque cambien mayúsculas y minúsculas. Al salir se libera el nombre.
- Cada producto tiene una sola entrada. La segunda aparición de un ID en un mismo envío se informa como `repeated_id`. Si el producto ya estaba o ingresó desde otra propuesta mientras se consultaba el catálogo, se informa `already_listed`.
- Cada conexión activa puede dar un voto por producto y retirarlo. Un voto repetido con el mismo valor es idempotente. Al cerrar la conexión se retiran sus votos. Cualquier participante puede quitar un producto si tiene cero votos. La lista ordena por votos descendentes y desempata por ID ascendente.
- El límite inicial de 2,5 s deja margen para consultas normales de catálogo sin imponer esperas largas a todo un lote. Las consultas de IDs nuevos salen al mismo tiempo. Cuando termina el lote, la inserción verifica los duplicados de nuevo y agrega sus éxitos en una operación sin espera intermedia.
- Mientras se consulta el catálogo, quien propone ve el progreso y luego recibe los IDs agregados y el motivo de cada fallo. Los demás siguen votando y ven el ranking cuando cambian los datos. Al cerrar la conexión se cancelan las consultas de esa persona y se descartan sus propuestas pendientes. Los productos ya agregados quedan en la lista.
- WebSocket permite que cada participante envíe acciones y reciba cambios por la misma conexión. SSE mantiene una conexión HTTP en la que el servidor envía el ranking a la pantalla. Con polling la pantalla tendría que consultar en intervalos, con solicitudes aun cuando no cambia el ranking y una demora de hasta un intervalo para ver cambios.

## Contrato de comunicación

Los mensajes WebSocket son objetos JSON de texto con `type`. Un ID válido es un entero positivo representable con precisión en JavaScript. El cliente tiene que ingresar antes de actuar.

### WebSocket · navegador → servidor

| `type` | Campos | Cuándo |
| --- | --- | --- |
| `join` | `name` (string) | Al entrar. Solo se admite un ingreso exitoso por conexión. |
| `propose` | `ids` (array de 1 a 10 IDs) | Al proponer uno o más productos. |
| `vote` | `productId` (ID), `voted` (boolean) | `true` al dar el voto y `false` al retirarlo. |
| `remove` | `productId` (ID) | Al quitar un producto sin votos. |

### WebSocket · servidor → navegador

| `type` | Campos | Cuándo |
| --- | --- | --- |
| `state` | `name` (string), `participants` (string[]), `participantCount` (number), `products` (Producto[]), `votedProductIds` (number[]) | Después del ingreso y ante cada cambio de presencia, productos o votos. `name` y `votedProductIds` corresponden a quien recibe el estado. |
| `proposal_result` | `addedIds` (number[]), `failures` (array de objetos con `id` y `code`) | Cuando termina una propuesta. Solo lo recibe quien la envió. |
| `error` | `code` (string), `message` (string) | Cuando el mensaje o la acción se rechazan. Solo lo recibe quien envió la acción. |

**Producto** contiene `id` (number), `title` (string), `price` (number en USD), `thumbnail` (string con URL HTTP/S), `proposedBy` (string) y `votes` (number). Los primeros cuatro campos provienen del catálogo consultado por el servidor.

Los motivos de propuesta son `repeated_id`, `already_listed`, `not_found` (404 del catálogo), `timeout` y `catalog_unavailable` (otro error del servicio, de red o de formato). Los códigos de error WebSocket son `invalid_message`, `join_required`, `already_joined`, `name_taken`, `unknown_product` y `product_has_votes`. Una trama que supera 16 KiB se cierra con el código WebSocket 1009.

### SSE · servidor → pantalla

`GET /events` abre un stream `text/event-stream`. El evento `ranking` contiene `{"type":"ranking","products":[],"participantCount":0}` con la lista ordenada y el conteo actual. El servidor lo envía al suscribirse y cuando cambia la lista o la presencia. Cada 15 s manda un comentario SSE para sostener la conexión. Al reconectar, EventSource recibe el estado completo.

## Reproducción en el navegador

1. Entrá con dos nombres distintos y proponé `1, 999999, 2`. La propuesta agrega los válidos y muestra el fallo únicamente en la pestaña que la hizo.
2. Entrá con un tercer nombre y abrí la pantalla. Ambos reciben el ranking desde el primer estado.
3. Votá el mismo producto desde dos pestañas. Retirá un voto y cerrá una pestaña para observar los cambios de ranking y presencia.
4. Desde la consola de un participante, ejecutá `window.ts7Socket.send("esto no es JSON")`. El error aparece en esa pestaña y podés seguir votando.
5. Usá la configuración de catálogo lento y repetí las acciones desde dos pestañas mientras la propuesta está pendiente.

## Evidencias

El video y las capturas muestran las cuatro vistas sincronizadas, los fallos parciales, la espera del catálogo y el rechazo de JSON inválido. [Ver el registro de la demostración](docs/evidencias/README.md)
