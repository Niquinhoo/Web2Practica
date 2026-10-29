# Despliegue TP3 · Cloudflare Containers

El proveedor se cambió a Cloudflare por elección del usuario. Se conserva la aplicación Java/Spring Boot, dentro de un contenedor `basic`, con una única instancia nombrada `tp3` y reposo después de un minuto sin tráfico. El Worker usa la API nativa de Durable Objects y la política `durable-object`, requerida por esta cuenta. Requiere Workers Paid; puede generar consumo adicional. Esta elección difiere del proveedor gratuito sugerido en la consigna y debe acordarse con la cátedra si se exige gratuidad estricta.

## Recorrido

```text
Cliente HTTPS → Worker → contenedor Java + cloudflared
                                  ↓ Access (token de servicio)
                      tp3-postgres.osadiamarketing.com
                                  ↓ Tunnel saliente
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

`DATABASE_URL` apunta a `jdbc:postgresql://127.0.0.1:15432/webii_tp3?sslmode=disable`. El token de Access solo permite este hostname, mediante una política de tipo Service Auth. Se emitió con una vigencia de 30 días: renovarlo o rotarlo antes de expirar y actualizar los secretos del Worker. Nunca usar una regla Bypass para esta base.

## Verificar y diagnosticar

- Comprobar `/health` público y JSON `status: ok`.
- Pedir `/api/listas` sin bearer: `401`.
- Registrar un usuario, iniciar sesión y repetir la petición con JWT: `200`.
- Confirmar en logs que Flyway aplicó V1–V6 en `webii_tp3`.
- Si hay errores de conexión, revisar primero que la PC y `web2-tp3-tunnel.service` estén activos, después el token Access y por último usuario/contraseña de DB.
- Un arranque en frío tarda más que una petición con la JVM activa. La base persiste en el equipo remoto aunque el contenedor duerma o se reemplace.

Los resultados y enlaces de CI/despliegue se registran en [EVIDENCIA-TP3](../../api-blank/docs/EVIDENCIA-TP3.md).

Referencias: [Cloudflare Containers](https://developers.cloudflare.com/containers/), [precios](https://developers.cloudflare.com/containers/platform/pricing/), [tokens de servicio Access](https://developers.cloudflare.com/cloudflare-one/access-controls/service-credentials/service-tokens/).

## Operación del conector remoto

Desde la PC remota:

```sh
sudo systemctl status web2-tp3-tunnel
sudo systemctl restart web2-tp3-tunnel
sudo journalctl -u web2-tp3-tunnel -n 50 --no-pager
```

Detener ese servicio interrumpe el acceso de la API a la base, pero no borra datos ni apaga PostgreSQL. El servicio está habilitado para iniciar al arrancar el equipo.

La prueba deliberada de CI y su recuperación se hicieron únicamente en `tp3`. Antes de ejecutar `npm run deploy`, comprobar que los jobs Java y Cloudflare de la revisión que se va a publicar estén verdes. La publicación actual es manual; no se dejó una credencial de Cloudflare de uso general en GitHub.
