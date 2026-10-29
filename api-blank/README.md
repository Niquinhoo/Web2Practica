# API TP3 · Seguridad, pruebas e integración continua

Java 25, Spring Boot 4.1.1, PostgreSQL, JPA y Flyway. El catálogo DummyJSON sigue público; listas y favoritos requieren JWT y autorización por dueño o rol `ADMIN`.

## Ejecutar

Con JDK 25 y Docker instalado, desde esta carpeta:

```powershell
docker compose up -d --wait postgres
$env:DATABASE_URL = 'jdbc:postgresql://127.0.0.1:55432/webii_tp2'
.\mvnw.cmd spring-boot:run
```

`55432` es el puerto configurado en el `.env` de este equipo; en un checkout nuevo se usa `5432` salvo que se configure `POSTGRES_PORT`. [Comandos y comprobaciones de PostgreSQL](docs/tp3/postgresql-docker.md).

En Linux/macOS: `./mvnw spring-boot:run`. `scripts/run-local.ps1` conserva la alternativa de PostgreSQL instalado; para usarla hay que indicar los binarios y datos válidos de ese equipo. Las pruebas de integración siempre requieren Docker.

## Variables

| Variable | Uso |
|---|---|
| `DATABASE_URL` | URL JDBC completa; por defecto se construye con `DB_HOST`, `DB_PORT` y `POSTGRES_DB` |
| `DB_HOST`, `DB_PORT`, `POSTGRES_DB` | Por defecto `localhost`, `5432`, `webii_tp2` |
| `POSTGRES_USER`, `POSTGRES_PASSWORD` | Credenciales de DB; valores locales de Compose por defecto |
| `JWT_SECRETO` | Al menos 32 bytes; obligatorio con perfil `prod` |
| `JWT_VIGENCIA_MINUTOS` | Vida del JWT; 30 minutos por defecto, debe ser positiva |
| `PORT` | Puerto HTTP; 8080 por defecto |
| `SPRING_PROFILES_ACTIVE` | `prod` en el contenedor de despliegue |
| `PG_TUNNEL_HOSTNAME`, `TUNNEL_SERVICE_TOKEN_ID`, `TUNNEL_SERVICE_TOKEN_SECRET` | Activa el cliente TCP autenticado de Cloudflare Access dentro del contenedor |

`.env` es configuración de Compose, no del proceso Maven. Los secretos no se guardan en Git. El valor JWT de desarrollo permite ejecutar localmente; el perfil `prod` exige un secreto explícito.

## Registro y login

`POST /api/auth/registro`, JSON:

```json
{"email":"usuario@example.com","password":"Una-clave-segura-123"}
```

Devuelve `201` con `id`, `email` y rol `USER`. El email se normaliza a minúsculas. Duplicados responden `409`, incluidos registros concurrentes gracias a la restricción única. La contraseña se valida y se almacena mediante BCrypt; nunca se devuelve el hash. BCrypt admite hasta 72 bytes UTF-8 y el registro exige al menos 8 caracteres.

`POST /api/auth/login` recibe el mismo JSON y devuelve `token`, `tokenType: Bearer` y `expiresIn` en segundos. Credenciales incorrectas responden `401`. Enviar `Authorization: Bearer <token>` en las operaciones protegidas.

La biblioteca Nimbus JOSE/JWT integrada en Spring Security emite y valida HS256, issuer, vencimiento, id positivo y rol permitido. El filtro Bearer nativo de Spring Security identifica al usuario. Sin token, con firma inválida o token vencido: `401`; identidad válida sin permiso: `403`.

No hay cookies de autenticación ni sesiones. CSRF se desactiva porque el token se adjunta explícitamente en el header. Los tokens no tienen revocación inmediata: un cambio de rol se refleja al hacer login nuevamente y los tokens anteriores vencen según `exp`.

## Endpoints y propiedad

| Ruta | Acceso |
|---|---|
| `GET /health`, `/swagger-ui/index.html`, `/v3/api-docs` | Público |
| `GET /api/productos`, `/api/productos/{id}` | Público, catálogo externo de solo lectura |
| `POST /api/auth/registro`, `/api/auth/login` | Público |
| `GET/POST /api/listas` | Usuario: solo sus listas; nuevas listas toman el dueño del token |
| `GET/DELETE /api/listas/{id}` | Dueño o ADMIN |
| `GET /api/listas/{id}/favoritos` | Dueño o ADMIN |
| `POST /api/listas/{origenId}/mover-favoritos` | Dueño de ambas listas o ADMIN; body `{"destinoId":2}` |
| `GET/POST /api/favoritos` | Filtrado por dueño; alta requiere lista autorizada |
| `GET/PUT/DELETE /api/favoritos/{id}` | Dueño de la lista o ADMIN; PUT también controla el destino |

La autorización por propiedad vive en `ListaServiceImpl.buscarAutorizada`, compartida por listas y `FavoritoService`. Así la regla protege también llamadas al servicio fuera del Controller, controla origen y destino antes de escribir y conserva el rollback transaccional del TP2. Los listados se filtran por usuario en las consultas a PostgreSQL.

`ADMIN` accede a todos los recursos; el registro público nunca asigna ese rol. Para preparar un administrador de la práctica, un operador autorizado debe cambiar `usuarios.rol` en la base y luego iniciar sesión nuevamente. No hay un endpoint público para promover usuarios.

Swagger incluye el esquema `bearerAuth`, el botón Authorize, endpoints auth y respuestas 401/403. Pegá el JWT sin el prefijo Bearer en Authorize.

## Migraciones y datos previos

V1–V4 permanecen intactas. V5 crea `usuarios`; V6 agrega la FK de usuario a listas y su índice. Las listas de TP2 se conservan con `usuario_id = NULL` y solo ADMIN puede acceder a ellas. Cualquier asignación posterior de esas listas debe ser una decisión explícita del operador. No se borran ni se reasignan datos existentes automáticamente.

## Pruebas

```powershell
.\mvnw.cmd --batch-mode test
.\mvnw.cmd --batch-mode verify
```

Linux/macOS: `./mvnw --batch-mode verify`. Surefire descubre las unitarias `*Test` y las regresiones existentes `*Tests`. Failsafe ejecuta `*IT` durante `verify`.

`PostgresTestDatabase` inicia un PostgreSQL 17 real con Testcontainers y puerto dinámico por ejecución. Cada integración usa un esquema aislado y lo elimina al terminar. Flyway aplica V1–V6; no se leen `TEST_DATABASE_*` ni la DB de desarrollo. Un Docker inaccesible produce un fallo, no un test omitido. La JVM de pruebas usa UTC para no depender de alias de zona horaria del equipo.

Ver [matriz de pruebas y evidencia](docs/EVIDENCIA-TP3.md). Reportes: `target/surefire-reports` y `target/failsafe-reports`.

## CI y despliegue

`.github/workflows/ci.yml` corre `./mvnw --batch-mode verify` en pushes a `tp3` y las ramas anteriores, y en pull requests. Usa Docker del runner mediante Testcontainers, sin servicio PostgreSQL redundante. Publica los reportes aun si una prueba falla. En GitHub, abrir Actions → Java CI → job → paso de Maven para encontrar la clase/aserción que falló.

[Despliegue Cloudflare Containers, secretos, túnel y diagnóstico](../../deployment/cloudflare/README.md). La imagen ejecuta con usuario sin privilegios. La publicación se realiza con `cf deploy` después de comprobar CI en verde; las credenciales de la aplicación se cargan como secretos del Worker.

Documentación histórica: [TP1](docs/EVIDENCIA-TP1.md), [TP2](docs/EVIDENCIA-TP2.md).
