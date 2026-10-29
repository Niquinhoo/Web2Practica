# TP3: segundo despliegue con Vercel Hobby y PostgreSQL por Tunnel

## Objetivo y estado

Se agregó una publicación verificada de la misma API Java/Spring en Vercel Hobby, conservando Cloudflare Containers. Proyecto `web2-practica-tp3` (`prj_LCk2AFQHG7vDY4ocRw6GzdGUuAEF`), equipo `niquinhoos-projects`. El 29 de octubre de 2026 se confirmó mediante la API autenticada que el plan es `hobby`, activo y sin trial: [evidencia de plan](../../api-blank/docs/tp3/evidencias/vercel/plan.json).

- [API / health](https://web2-practica-tp3.vercel.app/health).
- [Swagger](https://web2-practica-tp3.vercel.app/swagger-ui/index.html).
- Despliegue verificado: `dpl_BBn32Wt21tKgCn6NpCJLgRa6ythP`, Production, región `gru1` (São Paulo), fuente `tp3` en commit `9c8be6510b5b45423a982cbb025445aad021eb58`.
- [Prueba HTTP completa](../../api-blank/docs/tp3/evidencias/vercel/http-publico.json), [logs resumidos](../../api-blank/docs/tp3/evidencias/vercel/runtime.json).

No se modificó ni eliminó la aplicación de Cloudflare. La base sigue siendo PostgreSQL del equipo remoto, mediante el Tunnel y la política Access existentes.

## Por qué puede funcionar en Vercel

La documentación consultada el 29 de octubre de 2026 indica que [Container Runtime](https://vercel.com/docs/functions/runtimes/container) está en beta para todos los planes. Permite construir una imagen desde Dockerfile y ejecutar un servidor HTTP. Por eso se conserva Java 25, Spring Boot y el cliente cloudflared de la imagen existente; no se traduce el backend a JavaScript.

[Hobby](https://vercel.com/docs/plans/hobby) es gratuito para uso personal no comercial, con cuotas. No es lo mismo un proyecto Hobby que un proyecto alojado dentro de un equipo Pro con recursos incluidos. En esta ejecución se comprobó el plan real por API; no se cambió el plan ni se activaron complementos pagos.

La base propia y el Tunnel evitan contratar una base administrada para este segundo despliegue; el equipo doméstico debe permanecer encendido y conectado. El Tunnel transporta TCP mediante un cliente cloudflared: JDBC no conecta directamente al hostname público.

## Pasos reproducibles

1. Verificar que las pruebas de la revisión de `tp3` estén en verde. El proyecto procede de `Niquinhoo/Web2Practica`, no de otro repositorio ni de `main`.
2. Crear un proyecto Vercel separado llamado `web2-practica-tp3`, seleccionar el preset `Container` y Root Directory `api-blank`. Usar `api-blank/Dockerfile` como Dockerfile del contexto de construcción.
3. Confirmar que el equipo es Hobby. Elegir una sola región `gru1`, cercana a la base doméstica. Se deshabilitaron los despliegues Preview de este proyecto: las credenciales se configuraron para Production y esta experiencia se publica manualmente desde `tp3`.
4. Configurar para Production las variables descritas abajo, mediante el CLI autenticado si el conector no tiene permisos. Los secretos no se escriben en el repositorio. Se preparó un archivo privado ignorado por Git, `deployment/cloudflare/.secrets/production-vercel.json`, con una clave JWT nueva para esta segunda publicación; no debe copiarse a la documentación ni a capturas.
5. Crear explícitamente un despliegue Production del ref `tp3`; no usar el default `main` del repositorio. La publicación no implica fusionar ramas ni cambiar `main`.
6. Esperar el estado Ready y revisar logs de construcción y ejecución. Confirmar el inicio de Spring y la verificación de Flyway sobre la base remota. Una base ya migrada puede mostrar que está actualizada en v6; no debe volver a crear ni borrar sus tablas.
7. Registrar la URL real y ejecutar el smoke HTTP. No dar por probado PostgreSQL solamente porque responde health.

| Variable | Valor o función |
|---|---|
| `PORT` | `8080`, coincide con el puerto HTTP del contenedor |
| `JAVA_TOOL_OPTIONS` | `-XX:TieredStopAtLevel=1`, ajuste de arranque solo en Vercel |
| `SPRING_PROFILES_ACTIVE` | `prod`, también definido en la imagen |
| `DATABASE_URL` | `jdbc:postgresql://127.0.0.1:15432/webii_tp3?sslmode=disable` |
| `POSTGRES_USER` | Rol dedicado de la práctica |
| `POSTGRES_PASSWORD` | Secreto de la base; nunca versionarlo |
| `JWT_SECRETO` | Secreto de firma de al menos 32 bytes |
| `PG_TUNNEL_HOSTNAME` | `tp3-postgres.osadiamarketing.com` |
| `TUNNEL_SERVICE_TOKEN_ID` | Identificador del token de servicio Access |
| `TUNNEL_SERVICE_TOKEN_SECRET` | Secreto del token de servicio Access |

`sslmode=disable` corresponde al tramo loopback entre JDBC y el cliente cloudflared. El trayecto público del Tunnel usa cifrado; no se abre el puerto 5432 en el router. El JWT de los usuarios y el token Access tienen propósitos distintos.

## Comprobación y defensa

Desde la raíz del repositorio, con PowerShell 7:

```powershell
./api-blank/scripts/smoke-tp3.ps1 -BaseUrl https://web2-practica-tp3.vercel.app
```

El script verifica health 200, listas sin JWT 401, registro 201, login 200, lista propia 200, lista/favorito ajenos 403 y limpieza de listas/favoritos 204. Quedan usuarios de prueba con un prefijo único. Guardar la salida como evidencia solo después de que termine correctamente.

Abrir `/swagger-ui/index.html` en esa misma URL y seguir la [guía de defensa](../../api-blank/docs/GUIA-DEFENSA-TP3.md). Capturar respuestas reales de health, 401 y 200 con JWT, sin mostrar el token de Authorization ni las variables del proyecto.

Para defender la arquitectura: «Publiqué la misma imagen Java en un segundo proveedor con plan gratuito; los datos persisten en PostgreSQL de mi equipo. Vercel inicia el contenedor, el cliente cloudflared autentica la conexión de infraestructura con Access y Spring valida el JWT del usuario y la propiedad de sus listas».

## Incidentes y resolución observada

El conector creó el proyecto y los despliegues, pero rechazó la carga de variables de producción con `403`. Se usó Vercel CLI 65.7.0, con Node 24.22.1, mediante inicio de sesión por dispositivo autorizado por el usuario. Se inspeccionó explícitamente el proyecto y equipo antes de escribir las variables. Se cargaron por stdin y como Secret, sin pasarlas como argumentos visibles. `PORT`, perfil y `JAVA_TOOL_OPTIONS` son configuración; la firma JWT tiene una clave nueva y distinta de Cloudflare. Los datos de la base son compartidos, pero un JWT de una URL no sirve en la otra.

El primer despliegue compiló, pero la prueba `GET /health` devolvió `500`: la plataforma no detectó el puerto 8080 antes de su timeout observado de unos 28 segundos. Los logs mostraban el listener del túnel en 15432 y conexión a PostgreSQL; el problema estaba en el tiempo de arranque de Spring.

Se agregó únicamente en Vercel `JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1`. El recorrido HTTP completo pasó; los arranques observados en `iad1` quedaron cerca de 25–26 segundos. Luego se seleccionó `gru1`, cerca de la fuente de datos, para reducir la latencia de las consultas de inicialización. Se observaron arranques de Spring de 13,320 a 15,298 segundos y se repitió el smoke completo con éxito. Esta opción limita la optimización JIT al nivel 1: favorece el arranque con una posible pérdida de rendimiento máximo; no se desactivó la verificación de bytecode.

Flyway informó `Current version of schema "public": 6`. Como la base ya tenía V1–V6 aplicadas por Cloudflare, no era correcto volver a crearlas: Vercel verificó y reutilizó ese esquema. El smoke hizo registro, login y operaciones de listas sobre PostgreSQL real, no solo una respuesta estática de health.

Los cambios de esta etapa son documentación y configuración del proveedor; no se cambió código Java ni la imagen publicada en Cloudflare. Los logs originales incluyen metadatos de solicitudes y no se versionan; se guarda un resumen sin credenciales. [Captura health](../../api-blank/docs/tp3/evidencias/vercel/health-200.jpg), [401](../../api-blank/docs/tp3/evidencias/vercel/listas-401.jpg), [200 autenticado](../../api-blank/docs/tp3/evidencias/vercel/listas-200.jpg).

## Límites

Container Runtime está en beta y escala a cero; hay arranques en frío y cuotas de Hobby. La disponibilidad de ambos despliegues depende de la misma PC, DB, Tunnel y vencimiento de Access. Compartir la base significa compartir los datos de la práctica entre las dos URLs. Una modificación de esquema posterior debe seguir siendo compatible con ambas revisiones.

Esta experiencia acredita alojamiento de aplicación en Hobby gratuito y uso de PostgreSQL propio accesible por Tunnel. No se contrató una base administrada en Vercel. No se hicieron pruebas de carga ni se garantiza disponibilidad doméstica de producción; las cuotas vigentes se consultan en la documentación de Hobby.
