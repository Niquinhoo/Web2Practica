# TS7 · Paso a paso

El práctico se desarrolla en `TP4`, creada desde `main` de Web2Practica. Todos los archivos de la aplicación y su documentación viven en `TS7/` para poder trasladar el proyecto y sus commits a un repositorio independiente.

| Paso | Cambio | Cómo verificarlo |
| --- | --- | --- |
| 1 | Relevar la [consigna](consigna.md) y definir los hitos. | Comparar los requisitos, la entrega y los casos con el sitio de la cátedra. |
| 2 | Preparar el paquete y el servidor HTTP con Node.js y TypeScript. | Instalar con `npm ci`, compilar con `npm run build` e iniciar con `npm start`. |
| 3 | Incorporar el contrato JSON y el estado compartido. | Verificar presencia, duplicados de nombre y producto, votos y permisos de retiro. |
| 4 | Conectar DummyJSON, WebSocket y SSE. | Verificar resultados parciales, estados iniciales, concurrencia y liberación de conexiones. |
| 5 | Servir las interfaces de participante y pantalla y documentar el protocolo. | Abrir las dos páginas desde el mismo puerto y contrastar los mensajes con el README. |
| 6 | Reproducir los casos de la cátedra y guardar las evidencias. | Registrar tres participantes y una pantalla, y capturar los fallos parciales, la lentitud y el mensaje inválido. |
