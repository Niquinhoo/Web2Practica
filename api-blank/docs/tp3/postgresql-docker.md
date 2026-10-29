# PostgreSQL local con Docker

Configurado y comprobado el 28 de octubre de 2026 en la rama `tp3` de `Web2Practica`.

Se utiliza el servicio `postgres` del `api-blank/docker-compose.yml` existente, con imagen `postgres:17` y volumen nombrado `api-blank_postgres-data` montado en `/var/lib/postgresql/data`.

En este equipo los servicios Windows `postgresql-x64-16` y `postgresql-x64-17` ya estaban ejecutándose. Se comprobó con `pg_isready` que los puertos 5432 y 5433 aceptaban conexiones; no se modificaron esas bases ni se ejecutó SQL sobre ellas. Docker Desktop estaba apagado y se inició para levantar el contenedor.

## Configuración local

Se creó `api-blank/.env`, ignorado por Git, con:

```dotenv
POSTGRES_PORT=55432
```

Este puerto evita competir con los servicios instalados. Compose utiliza los valores de desarrollo definidos en el YAML para la base y el usuario (`webii_tp2`) y su contraseña local. No usar esas credenciales de ejemplo en producción.

## Levantar, consultar y apagar

Desde PowerShell, dentro de `api-blank` y con Docker Desktop iniciado:

```powershell
docker compose up -d --wait postgres
docker compose ps
docker compose logs --tail 50 postgres
docker compose exec postgres psql -U webii_tp2 -d webii_tp2 -c "SELECT current_database(), current_user;"
```

Para apagarlo y volver a levantarlo:

```powershell
docker compose stop postgres
docker compose up -d --wait postgres
```

`stop` conserva el contenedor y el volumen. `docker compose down` quita los contenedores y la red de este Compose y conserva el volumen nombrado. **`docker compose down -v` elimina también los datos del volumen.**

## Conectar la aplicación desde Windows

La dirección es `127.0.0.1:55432`, base y usuario `webii_tp2`. El puerto está publicado únicamente en la interfaz local.

La aplicación existente lee `DATABASE_URL`; para iniciarla contra este contenedor:

```powershell
$env:DATABASE_URL = 'jdbc:postgresql://127.0.0.1:55432/webii_tp2'
.\mvnw.cmd spring-boot:run
```

El `.env` es leído por Compose; no configura por sí solo las variables del proceso Maven. Si se personalizan usuario y contraseña, también hay que pasar `POSTGRES_USER` y `POSTGRES_PASSWORD` al proceso de la aplicación. Las variables de inicialización de la imagen no cambian automáticamente credenciales de una base ya creada.

## Comprobaciones realizadas

- Docker Engine respondió con versión `29.9.1`.
- `docker compose up -d --wait postgres` terminó correctamente y reportó `healthy`.
- Conexión TCP desde Windows mediante `psql` al puerto `55432`, autenticada como `webii_tp2` sobre la base `webii_tp2`.
- `SELECT version()` devolvió PostgreSQL `17.11`.
- Se creó una tabla de prueba, se insertó y leyó `persistencia-ok`.
- Se detuvo el contenedor, se volvió a iniciar y se verificó que la fila seguía presente.
- Se eliminó la tabla de prueba tras la comprobación.
- Se inspeccionó el montaje del volumen persistente y se dejó el contenedor ejecutándose, `healthy`.

No se inició la aplicación ni se ejecutó la suite Maven. La prueba de persistencia cubrió detener e iniciar el contenedor; no se probó eliminarlo y recrearlo ni restaurar datos en otro equipo.

## Relación con TP3 y portabilidad

Este PostgreSQL persistente sirve para desarrollo manual. Las pruebas de integración del TP3 deben obtener su propia base limpia mediante Testcontainers y aplicar las migraciones con Flyway. Esta operación no implementa todavía esa integración.

El Compose permite reproducir el servicio en otra máquina con Docker. El volumen y los datos permanecen en este equipo; para trasladarlos se puede exportar con `pg_dump` y restaurar con `pg_restore`. Un volumen persistente no reemplaza una copia de seguridad.

Referencias: [imagen oficial PostgreSQL](https://hub.docker.com/_/postgres), [Compose down y volúmenes](https://docs.docker.com/reference/cli/docker/compose/down/), [Testcontainers PostgreSQL](https://java.testcontainers.org/modules/databases/postgres/).
