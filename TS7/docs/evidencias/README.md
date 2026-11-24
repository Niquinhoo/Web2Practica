# TS7 · Evidencias de funcionamiento

Registro del 24 de noviembre de 2026. Ejecutado en Windows con Node.js 22.17.0 y Chromium, contra el catálogo real de DummyJSON. La grabación reúne tres vistas de participante y la vista de pantalla en una sola ventana.

| Evidencia | Qué se observa |
| --- | --- |
| [Video de la demostración](demo-ts7.mp4) | Ana y Bruno ingresan, Carla recibe la lista al sumarse, el ranking cambia con los votos y el conteo baja al cerrar su vista. |
| [Propuesta con fallos parciales](01-fallos-parciales.png) | La propuesta de Ana agrega los productos 1 y 2. Solo Ana recibe el error del ID 999999. |
| [Catálogo lento](02-catalogo-lento.png) | Ana espera sus productos 5 y 6 con una demora de 3,5 s en el catálogo. Bruno y Carla votan el producto 4 y el ranking muestra dos votos mientras sigue abierta la consulta. |
| [Mensaje inválido](03-mensaje-invalido.png) | Carla recibe un error de JSON inválido. Las otras vistas conservan el ranking y Carla puede seguir votando. |

También se verificó que dos propuestas simultáneas del ID 3 dejan una entrada y que dos personas pueden votar ese producto. Repetir el voto no aumenta el conteo. El servidor rechaza quitar un producto con votos, retira los votos de una persona al desconectarse y permite quitar el producto cuando queda en cero. Con un límite de 800 ms y una demora de 3 s, la propuesta devuelve un timeout privado.
