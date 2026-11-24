# TP4 · Asincronismo e integración

Relevamiento del 24 de noviembre de 2026. [Consigna oficial de Web II](https://unvime-web-2.matiendres.workers.dev/practicos/tp4/)

## Objetivo

Un grupo propone productos para un regalo y vota en una lista compartida. Cada participante ve el estado completo al entrar y los cambios posteriores en tiempo real. Una pantalla aparte muestra el ranking y cuántas personas están conectadas.

## Entrega

- Proyecto independiente de Java con Node.js LTS y TypeScript. Un servidor y un puerto sirven ambas páginas y sus conexiones. La lista se guarda en memoria y el frontend puede ser HTML, CSS y JavaScript.
- README con instalación y ejecución, contrato de mensajes y eventos, decisiones sobre nombres, repetidos, permisos y timeout, comportamiento ante fallos y comparación de WebSocket, SSE y polling.
- Video corto o GIF de tres participantes y la pantalla sincronizados. Capturas de una propuesta con fallos parciales, un catálogo lento y una respuesta a un mensaje inválido.

## Requisitos

| Área | Qué pide la cátedra |
| --- | --- |
| Participantes | Ingreso con nombre, estado inicial completo y presencia actualizada al conectar o cerrar una pestaña. Hay que definir las reglas de nombres. |
| Propuestas | Entre uno y diez IDs por acción. El servidor consulta DummyJSON en paralelo y toma de allí el título, el precio y la imagen. Agrega los resultados válidos y devuelve en privado un motivo por cada fallo, incluidos ID inexistente, timeout y producto repetido. |
| Votos y retiro | Cada participante puede sumar o retirar un voto por producto. El ranking se ordena por votos. El servidor aplica una regla documentada para quitar productos. |
| Protocolos | API WebSocket estándar en participantes y EventSource en pantalla. Mensajes JSON tipados y validados en el servidor. Las entradas inválidas y las acciones prohibidas devuelven un error privado. |
| Concurrencia | La espera al catálogo deja disponibles los votos y las demás acciones. Dos propuestas simultáneas del mismo ID producen una sola entrada y dos personas pueden votar a la vez. |
| Conexiones | El servidor deja de enviar a las conexiones cerradas y libera sus recursos. |
| Configuración | La URL, el límite de espera y la simulación de lentitud se pueden cambiar sin editar código. DummyJSON admite el parámetro `delay`. [Documentación del catálogo](https://dummyjson.com/docs/products) |

La cátedra propone abrir tres participantes y una pantalla para comprobar ingreso tardío, errores parciales privados, votos durante la espera, concurrencia, desconexión, manejo de mensajes inválidos y actualización sin recargas. La reconexión con identidad, ping/pong, múltiples listas, el acceso desde otro dispositivo y las pruebas automatizadas son opcionales.
