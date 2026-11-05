# Despliegue TP3 · Cloudflare Containers

Cloudflare es el proveedor elegido expresamente por el usuario y ratificado para la entrega el 3 de noviembre de 2026, exceptuando la exigencia original de gratuidad. Se conserva la aplicación Java/Spring Boot, dentro de un contenedor `basic`, con una única instancia nombrada `tp3` y reposo después de un minuto sin tráfico. El Worker usa la API nativa de Durable Objects y la política `durable-object`, requerida por esta cuenta. Requiere Workers Paid; se documenta como un recurso pago, sin presentarlo como alojamiento gratuito. La rama de entrega es `TP3`; preparar esa rama no vuelve a desplegar automáticamente.

## Recorrido

```text
Cliente HTTPS → Worker → contenedor Java + cloudflared
                                  ↓ WebSocket TLS + credencial privada
                web2-tp3-postgres.nicotrib699.workers.dev
                                  ↓ binding VPC al Tunnel dedicado
                      PostgreSQL del equipo remoto
```

El equipo remoto debe estar encendido y conectado. No se abre el puerto PostgreSQL al router ni se conecta JDBC directamente al hostname público: `cloudflared access tcp` proporciona un listener `127.0.0.1:15432` dentro del contenedor. Ese listener viaja por TLS/WebSocket hasta Cloudflare y el Tunnel llega a PostgreSQL por loopback. `sslmode=disable` solo corresponde al tramo local entre JVM y cloudflared; la conexión pública está cifrada por el túnel.

En el equipo remoto se crearon el rol y la base exclusivos `webii_tp3`. PostgreSQL sigue escuchando según su configuración previa. `web2-tp3-tunnel.service` inicia el conector saliente con systemd; su token vive en un archivo privado fuera del repositorio.

## Herramientas y secretos

Se usa el CLI oficial `cf`, configuración `cloudflare.config.ts` y plugin Vite, con Node 24.22 o superior. Desde esta carpeta:

```sh
npm ci
npm run check
npm run build
cf auth login
npm run deploy
```

`npm run deploy` lee `.secrets/production.json`, ignorado por Git. El archivo debe contener `DATABASE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRETO`, `PG_TUNNEL_HOSTNAME`, `TUNNEL_SERVICE_TOKEN_ID` y `TUNNEL_SERVICE_TOKEN_SECRET`. Los valores se publican como secretos del Worker y se pasan al contenedor al iniciarlo. No se incluyen tokens o contraseñas en la imagen ni en la configuración versionada.

Opcionalmente, `.secrets/production.json` lleva `DATABASE_FALLBACK_URL`, `DATABASE_FALLBACK_USER` y `DATABASE_FALLBACK_PASSWORD` (PostgreSQL de Render): si el Tunnel no responde al iniciar la API, esta usa esa base. Las tres claves están declaradas como secretos del Worker en `cloudflare.config.ts`, por lo que el archivo debe incluirlas antes de `npm run deploy`. Ver [Base de respaldo en Render](../../api-blank/README.md#base-de-respaldo-en-render).

`DATABASE_URL` apunta a `jdbc:postgresql://127.0.0.1:15432/webii_tp3?sslmode=disable`. `PG_TUNNEL_HOSTNAME` es `web2-tp3-postgres.nicotrib699.workers.dev`. El puente valida identificador y secreto antes de aceptar WebSocket y solo permite el destino fijo `127.0.0.1:5432` mediante el binding VPC del Tunnel `web2-tp3-postgres`. No acepta destinos del cliente ni contiene la contraseña PostgreSQL.

Los nombres `TUNNEL_SERVICE_TOKEN_*` y las cabeceras `CF-Access-Client-*` se conservan por compatibilidad con `cloudflared access tcp`. La credencial actual la valida el Worker: no es un token emitido por Access ni depende de una aplicación Access, una zona DNS o un dominio propio. Si se rota, actualizar el puente, la API Cloudflare y Vercel juntos. La contraseña PostgreSQL y el JWT de usuario son credenciales separadas.

El puente se configura en `database-proxy/cloudflare.config.ts` y su código está en `src/database-proxy.ts`. Usa el Tunnel existente por UUID, con ingress público residual `http_status:404`. Su configuración no crea registros en dominios de clientes. Desde `TP3`, después de CI:

```sh
npm run deploy:db-proxy -- --tag TP3-<commit>
npm run deploy -- --tag TP3-<commit> --containers-rollout immediate
```

El primer comando lee `.secrets/database-proxy.json`, con solo identificador y secreto del puente. El segundo lee `.secrets/production.json`. Ambos archivos están ignorados por Git. Workers VPC está en beta; su disponibilidad y cambios de plataforma forman parte del límite operativo.

## Verificar y diagnosticar

- Comprobar `/health` público y JSON `status: ok`.
- Pedir `/api/listas` sin bearer: `401`.
- Registrar un usuario, iniciar sesión y repetir la petición con JWT: `200`.
- Confirmar en logs que Flyway aplicó V1–V6 en `webii_tp3`.
- Si hay errores de conexión, revisar PC, PostgreSQL y `web2-tp3-tunnel.service`, el binding VPC, la coincidencia del secreto del puente en sus clientes y finalmente el usuario/contraseña de DB. El puente sin credenciales debe devolver `401`.
- Un arranque en frío tarda más que una petición con la JVM activa. La base persiste en el equipo remoto aunque el contenedor duerma o se reemplace.

## Arranque en frío

Antes de optimizar, el 4 de noviembre de 2026, `GET /health` tardó entre 52 y 59 s en cinco arranques en frío de la URL pública, y cerca de 0,6 s con la JVM activa. La instancia siguió activa tras 40 s sin tráfico y ya estaba apagada a los 50 s (una sola muestra).

Comparación en un banco local con la misma imagen, 0,5 CPU, 1 GB y PostgreSQL 17 ya migrado. Sirve para comparar variantes entre sí: no incluye el túnel y no equivale al tiempo de Cloudflare.

| Variante | Hasta `/health` 200 | Consultas SQL |
|---|---|---|
| Base | 88 s | 62 |
| `-XX:TieredStopAtLevel=1` | 48 s | 62 |
| AOT Cache | 40 s | 62 |
| AOT Cache y `TieredStopAtLevel=1` | 22 s | 61 |
| Lo anterior y `minimum-idle=1` (versión aplicada) | 22 s | 53 |

Con 0,5 CPU, la mediana del login (BCrypt) fue de 300 ms en la base y 305 ms en la versión aplicada. El recorrido de registro, login, listas y favoritos pasó en ambas. El AOT Cache agrega 130 MB a la imagen.

No se aplicó `hibernate.boot.allow_jdbc_metadata_access=false`: junto con `ddl-auto=validate` subió a 84 consultas, y bajarlas a 43 exigía apagar la validación del esquema.

Los resultados y enlaces de CI/despliegue se registran en [EVIDENCIA-TP3](../../api-blank/docs/EVIDENCIA-TP3.md).

Referencias: [Cloudflare Containers](https://developers.cloudflare.com/containers/), [precios](https://developers.cloudflare.com/containers/platform/pricing/), [Workers VPC Networks](https://developers.cloudflare.com/workers-vpc/configuration/vpc-networks/) y [API TCP de VPC](https://developers.cloudflare.com/workers-vpc/api/).

## Operación del conector remoto

Desde la PC remota:

```sh
sudo systemctl status web2-tp3-tunnel
sudo systemctl restart web2-tp3-tunnel
sudo journalctl -u web2-tp3-tunnel -n 50 --no-pager
```

Detener ese servicio interrumpe el acceso de la API a la base, pero no borra datos ni apaga PostgreSQL. El servicio está habilitado para iniciar al arrancar el equipo.

La prueba deliberada de CI y su recuperación se hicieron originalmente en `tp3`. La rama vigente de código y despliegue es **`TP3`**. `npm run deploy` ejecuta un control previo que rechaza cualquier otra rama, incluso `tp3`. Antes de publicar, comprobar que los jobs Java y Cloudflare de esa revisión estén verdes, que los archivos a desplegar estén commiteados y usar `--tag TP3-<commit>` y `--message` para registrar la revisión. La publicación es manual; no se dejó una credencial de Cloudflare de uso general en GitHub. El identificador de instancia `tp3` y el nombre del Worker no cambian: no son referencias Git.

## Publicación vigente sin dominio propio · 3 de noviembre

Código `229c8ce` desde `TP3`, [CI verde con 93 pruebas](https://github.com/Niquinhoo/Web2Practica/actions/runs/41753033540). API versión `ab1a015e-ec01-4680-82dc-1350be30943c`; puente versión `8510c80d-6ea5-4f39-980b-71b35e605fc0`, ambos al 100 %. Se repitieron 54 casos públicos sobre cada proveedor; PostgreSQL verificó seis migraciones, respuesta Unicode grande y consulta de 200 KB. [Evidencia vigente](../../api-blank/docs/tp3/evidencias/sin-dominio-2026-11-03/postgres-vpc.json).

Se retiraron exclusivamente el CNAME, la aplicación Access, la política y el token que se habían creado para el TP en un dominio de cliente. La configuración actual no depende de ese dominio. El Tunnel privado y los datos permanecen.

## Publicación previa, reemplazada por la corrección de conexión

Código publicado desde `TP3`: `994e5b7a57671ac6f703d68213b5d9a422a81293`, con CI en verde antes de publicar. Versión `fe9b2948-2f9d-4517-80c6-1118e800db92`, al 100 %, tag `TP3-994e5b7a5767`. [Metadata del despliegue](../../api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/despliegue.json).

Se verificaron 54 solicitudes HTTP, V1–V6 remotas y acceso ADMIN con cuenta descartable; también se ejecutaron health 200 y listas 401/200 en Swagger desde Chrome. La evidencia conserva un 502 inicial del catálogo y la recuperación posterior. [Informe y límites de la comprobación](../../informe.md). Los commits posteriores que guardan evidencia no cambian el código de la imagen ya publicada.

La referencia remota duplicada `tp3` fue retirada tras comprobar que su historia estaba incluida en `TP3` y que no tenía PR abiertos. Solo se usa **`TP3` en mayúsculas** para futuras publicaciones de código y despliegue.
