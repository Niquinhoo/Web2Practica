# TP3 · Informe de implementación y cumplimiento de consignas

**Fecha de revisión y ajustes:** 3 de noviembre de 2026. **Repositorio:** Web2Practica. **Rama de entrega:** `TP3`, continuada desde la revisión `86a5636` de `tp3`.

Este informe contrasta las consignas proporcionadas con el código, la configuración y los archivos de evidencia. Explica qué se hizo, cómo se implementó, por qué se eligió cada solución y cómo mostrar el cumplimiento. La [consigna transcrita](TP3-consigna.md) se conserva como referencia.

## Resultado de la revisión: qué está cumplido y qué requiere una aclaración

**Se cerraron las diferencias de configuración e integración identificadas en la revisión. Las consignas de código, pruebas, CI y documentación quedan implementadas y verificadas localmente. Para despliegue se conserva Cloudflare por instrucción expresa del usuario: es una excepción elegida al requisito original de proveedor gratuito, no un pendiente técnico a resolver migrando a otro proveedor.**

| Punto | Resultado de la revisión | Aclaración necesaria |
|---|---|---|
| 1. Dependencias y configuración | Implementado. | Nimbus es la biblioteca JWT elegida; JJWT era un ejemplo. El núcleo y los módulos de Testcontainers están declarados explícitamente. |
| 2. Usuario, persistencia y registro | Implementado. | BCrypt real, rol USER fijo y duplicados protegidos por UNIQUE. |
| 3. Login y JWT | Implementado y verificado. | Filtro Bearer nativo y patrones públicos `/api/auth/**` y `/api/productos/**`, como pide el enunciado. Las operaciones inexistentes siguen respondiendo 404/405. |
| 4. Propiedad y rol | Implementado. | ADMIN tiene excepción de propiedad también en operaciones de modificación. Las listas anteriores sin dueño quedan solo para ADMIN. |
| 5. Pruebas | Nueva ejecución local en verde: 58 unitarias y 25 integraciones. | `SeguridadIT` está en el paquete `seguridad`; la integración global está junto a la aplicación. Base temporal por ejecución, con esquemas por clase. |
| 6. CI | Nueva corrida de entrega en verde; fallo/recuperación anteriores conservados. | Eventos push y pull_request sin filtro de ramas, incluida `TP3`; jobs Java/PostgreSQL y Worker exitosos. |
| 7. Despliegue | Nueva revisión publicada y verificada en Cloudflare el 3 de noviembre. | Commit `994e5b7` de `TP3`, versión `fe9b2948-2f9d-4517-80c6-1118e800db92` al 100 %. PostgreSQL remoto por Access/Tunnel; Vercel queda como experiencia adicional histórica. |
| 8. Documentación | Implementado. | Swagger y README describen auth, permisos, pruebas, CI y despliegue; este informe reúne la explicación detallada. |

**Alcance de la comprobación actual:** 83 ejecuciones Java y cinco del Worker en verde, también en CI para la revisión publicada. Se desplegó `994e5b7` desde `TP3` y se verificaron **54 solicitudes públicas correctas**, además de health y acceso protegido 401/200 desde Swagger en Chrome. Se consultó PostgreSQL remoto para confirmar V1–V6 y se revisaron los eventos del Worker. La nueva evidencia de Cloudflare es del **3 de noviembre**; la publicación adicional de Vercel conserva sus evidencias del 29 de octubre.

## Publicación final y verificación en Cloudflare · 3 de noviembre

| Dato verificable | Resultado |
|---|---|
| Rama única de entrega | `TP3`. Se retiró `tp3` después de verificar que `86a5636` era ancestro de `TP3` y no tenía PR abiertos. |
| Revisión de código e infraestructura publicada | `994e5b7a57671ac6f703d68213b5d9a422a81293`. Los commits posteriores de esta verificación solo registran documentación y evidencia. |
| CI de esa revisión | [Corrida 41732115409](https://github.com/Niquinhoo/Web2Practica/actions/runs/41732115409), ambos jobs exitosos. |
| Worker | `web2-practica-tp3`. |
| Versión activa | `fe9b2948-2f9d-4517-80c6-1118e800db92`, 100 % del tráfico. |
| Deployment | `431a96af-3678-4069-b4d1-cdcb7debf350`. |
| Imagen ejecutada | Digest `sha256:05f430e51314feee8ddefdf92a1263beb29eb0171ab0fb78215cfd1ea00ae9c9`. |
| Identificación de origen | Tag `TP3-994e5b7a5767` y mensaje de versión con la referencia al commit de `TP3`. |
| Base remota | PostgreSQL 18.6; V1–V6 presentes, todas con `success=true`. |
| Tunnel | HTTP sin credencial Access devuelve `403`; conexión PostgreSQL autenticada comprobada por el listener TCP de cloudflared. |
| Control de publicación | `npm run deploy` ejecuta `predeploy`: permite `TP3`, rechaza cualquier otra rama. Se comprobó también el rechazo de `tp3`. |

### Qué se verificó en la URL pública

El [recorrido de 54 solicitudes](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/http-publico-completo.json) registró los códigos esperados y comprobó campos de las respuestas. Incluye:

- Health y Swagger/OpenAPI públicos; esquema Bearer y operaciones auth presentes.
- Productos reales: catálogo, producto existente, inexistente y rechazo de un método de escritura.
- La ruta auth inexistente responde `404`: confirma en producción el patrón público de la revisión nueva.
- Listas y favoritos sin JWT o con JWT inválido responden `401`.
- Registro USER sin exponer contraseña/hash; duplicado `409`, entrada inválida `400` y login incorrecto `401`.
- Dueño de lista tomado del token aunque el body incluya un ID ajeno; listado filtrado por dueño.
- Lectura y borrado de listas ajenas, favoritos ajenos y altas/PUT hacia listas ajenas denegados con `403`.
- Borrado de lista con favoritos `409`; movimiento con origen o destino ajeno `403`, mismo origen/destino `400` y destino inexistente `404`.
- Movimiento propio `204`, origen eliminado y favorito conservado en el destino; actualización propia del favorito.
- ADMIN real sobre recurso ajeno `200` y listado de recursos de ambos dueños. Se preparó solo una cuenta descartable, se restauró a USER y se comprobó nuevamente el `403` con un token USER nuevo.
- Limpieza de los favoritos, listas y **las dos cuentas** de ese recorrido. No se modificaron cuentas o listas previas del usuario.

Se comprobó además en PostgreSQL que las cuentas del recorrido tenían hashes BCrypt, sin copiar sus valores a la evidencia. El rollback ante una FK forzada se acredita con la integración en CI; no se introdujo esa falla artificial en producción.

### Incidente observado y recuperación

El [primer intento](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/http-intento-inicial.json) se detuvo cuando el catálogo externo produjo `502`. Las comprobaciones siguientes obtuvieron `200`, con 194 productos, y después pasó el recorrido completo. No se alteró el catálogo ni se ocultó el fallo con datos simulados. El código contempla `502` cuando falla el proveedor; esta observación no permite atribuir con certeza la causa al proveedor o al primer establecimiento de la conexión.

El [resumen de runtime](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/runtime.json) contiene 158 eventos de la ventana revisada, todos con outcome `ok`: dos de nivel error corresponden a la solicitud de catálogo que respondió `502`. La ventana posterior contiene 112 eventos info, sin errores de ejecución. El estado `stopped` observado después de un minuto sin tráfico corresponde a la política de inactividad; la instancia utiliza el digest publicado. Health volvió a responder `200` en una comprobación posterior. No se afirma que esos conteos representen todas las horas de operación.

### Capturas reales y limpieza de la sesión de navegador

Se abrió Swagger en Chrome, se ejecutó health y se consultó listas primero sin token y después con un JWT de una cuenta descartable. Se guardaron [Swagger cargado](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/swagger-ui.png), [health 200](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/health-swagger-200.png), [listas 401](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/listas-swagger-401.png) y [listas 200](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/listas-swagger-200.png). Las capturas muestran respuestas ejecutadas, no `Example Value`, y el JWT quedó fuera del encuadre.

La autorización se retiró de Swagger, se limpió la respuesta que contenía el Curl con JWT y se cerró la pestaña de prueba. La revisión automática **bloqueó el comando de limpieza de la cuenta adicional de captura**, sin explicar otro motivo que política. Esa tercera cuenta quedó como **USER y sin listas**, identificada en [swagger-browser.json](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/swagger-browser.json). El archivo temporal privado ya no contiene el token. Esto se distingue de las dos cuentas del recorrido de 54 solicitudes, que sí se eliminaron.

Todos los artefactos públicos excluyen contraseñas, JWT, hashes y credenciales de infraestructura. La [evidencia del despliegue](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/despliegue.json), las [migraciones consultadas](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/migraciones-remotas.json) y la [protección del Tunnel](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/tunnel.json) completan la trazabilidad.

Para navegar: [listado de cumplimiento](#listado-de-cumplimiento-requisito-por-requisito), [paso a paso](#reconstrucción-paso-a-paso-de-lo-realizado), [contenedores y bases](#el-contenedor-de-la-base-de-datos-qué-es-y-qué-decisión-resolvió), [consigna 1](#1-dependencias-y-configuración), [demostración](#demostración-reproducible-en-swagger) y [entregables y diferencias](#verificación-de-los-entregables-esperados).

## Listado de cumplimiento requisito por requisito

La numeración interna de esta tabla permite referenciar cada exigencia, aun cuando el texto original agrupe varias en un mismo párrafo. Las pruebas Java y del Worker se ejecutaron nuevamente el 3 de noviembre. Los resultados HTTP de las publicaciones y la prueba deliberada de CI rojo/verde conservan su fecha histórica.

| ID | Requisito solicitado | Cómo se cumple / evidencia concreta | Estado |
|---|---|---|---|
| 1.1 | Starter Security en el POM. | `api-blank/pom.xml`: `spring-boot-starter-security`. | Implementado |
| 1.2 | Biblioteca para emitir y validar JWT. | Resource Server + JOSE, `NimbusJwtEncoder`, `NimbusJwtDecoder` y `JwtService`. | Implementado; biblioteca permitida |
| 1.3 | Starter de pruebas y Testcontainers. | Starter test y núcleo, JUnit y PostgreSQL declarados con scope test. `PostgresTestDatabase` los utiliza. | Implementado |
| 1.4 | Credenciales y secreto configurables por entorno con defaults locales. | `application.properties` y perfil `prod`, que exige secretos explícitos. | Implementado |
| 1.5 | Failsafe separa IT de Test; verify ejecuta ambas. | POM con goals `integration-test` y `verify`; Surefire para unitarias. | Implementado |
| 2.1 | Dominio Usuario y puerto UsuarioRepository. | Records/enum y puerto bajo `usuario/`. | Implementado |
| 2.2 | Entity, JpaRepository y adapter. | Tres clases bajo `usuario/persistencia/`. | Implementado |
| 2.3 | V5 con usuarios, email único, hash y rol. | `V5__create_usuarios.sql`, UNIQUE y CHECK. | Implementado |
| 2.4 | Registro con hash y USER; email repetido 409. | `AuthController.registro`, `UsuarioServiceImpl.registrar`, BCrypt y handler; prueba de carrera de registro. | Implementado y verificado |
| 3.1 | Login válido con JWT id/rol; inválido 401. | `autenticar`, `JwtService.emitir`, `SeguridadIT` y `JwtServiceTest`. | Implementado y verificado |
| 3.2 | Filtro Bearer valida y deja identidad disponible. | `SecurityFilterChain.oauth2ResourceServer`, decoder y `ContextoSeguridad`. | Implementado con filtro nativo |
| 3.3 | Auth/productos públicos; listas/favoritos protegidos. | Matchers `/api/auth/**` y `/api/productos/**`, con `anyRequest().authenticated()` para el resto; pruebas 401 y 404/405. | Implementado y verificado |
| 4.1 | Lista con dueño y relación a Usuario; V6 nueva. | `Lista.usuarioId`, `ListaEntity.usuario`, FK e índice en V6. | Implementado |
| 4.2 | Dueño tomado del autenticado, no del body. | `ListaServiceImpl.crear` usa `seguridad.usuarioId()`; request solo con nombre. | Implementado y probado |
| 4.3 | Controlar GET/DELETE, favoritos y movimiento; ajena 403. | `buscarAutorizada`, `exigirPropiedad`; autorización de ambos extremos y pruebas en `SeguridadIT`. | Implementado y probado |
| 4.4 | Listar solo propias; ADMIN todas. | `listar`, consulta por usuario en JPA y prueba de filtrado. | Implementado y probado |
| 4.5 | README justifica dónde vive la autorización. | Sección «Endpoints y propiedad» de `api-blank/README.md`. | Documentado |
| 5.1 | Unitaria: lista creada con dueño autenticado. | `ListaServiceImplTest.crear_usuarioAutenticado_asignaPropiedad`. | Verificado el 3 de noviembre |
| 5.2 | Unitaria: no persistir contraseña en claro. | `UsuarioServiceImplTest.registrar_password_guardaSoloHashConRolUser`. | Verificado el 3 de noviembre |
| 5.3 | PostgreSQL real con Testcontainers y migraciones limpias. | `PostgresTestDatabase`, esquemas temporales y Flyway V1–V6. | Implementado; aislamiento por clase |
| 5.4 | Login válido 200 + token. | Helper `SeguridadIT.login`, usado en la preparación de cuentas. | Verificado el 3 de noviembre |
| 5.5 | Login inválido 401. | `registro_yLogin_guardanHashYNoExponenSecretos`. | Verificado el 3 de noviembre |
| 5.6 | Acceso a lista propia 200. | `listas_usuarioAjeno_noPuedeLeerBorrarNiMover`, lectura de la propia. | Verificado el 3 de noviembre |
| 5.7 | Acceso a lista ajena 403. | El mismo caso comprueba las lecturas y operaciones denegadas. | Verificado el 3 de noviembre |
| 5.8 | Listas sin token 401. | `acceder_tokenAusenteOInvalido_responde401`. | Verificado el 3 de noviembre |
| 5.9 | ADMIN sobre lista ajena 200. | `admin_listasAjenasYLegadas_accedeSinCambiarPropiedad`. | Verificado el 3 de noviembre |
| 5.10 | Nombres Test/IT y operación_escenario_resultado. | Servicios en su paquete, `SeguridadIT` en `seguridad` y `ApiIntegrationIT` junto a `ApiBlankApplication`; sufijos y métodos respetados. | Convención aplicada |
| 6.1 | Workflow en push que ejecute verify. | `.github/workflows/ci.yml`, eventos sin filtro de ramas, job Java y paso Maven. | Implementado para cada push |
| 6.2 | Mostrar CI rojo y luego verde. | Corridas enlazadas en el apartado de CI. | Evidencia histórica |
| 6.3 | Runner limpio sin PostgreSQL personal; wrapper ejecutable. | Testcontainers y propiedades dinámicas; `mvnw` versionado con modo 100755. | Implementado |
| 7.1 | Proveedor gratuito con PostgreSQL. | Se conserva Cloudflare Containers y PostgreSQL remoto; el usuario exceptuó expresamente la exigencia de gratuidad para esta entrega. | Excepción de proveedor solicitada; despliegue documentado |
| 7.2 | Conexión remota y secreto JWT por entorno. | Perfil prod, configuración de secretos y guías de publicación. | Implementado y documentado |
| 7.3 | Migraciones remotas, health público y revisión de logs. | Consulta nueva V1–V6, runtime del Worker, 54 solicitudes y capturas ejecutadas en Swagger. | Verificado el 3 de noviembre |
| 7.4 | Se permite apagar después de verificar. | Se conservan resultados y capturas; no se presupone disponibilidad actual. | Evidencia conservada |
| 8.1 | Swagger documenta auth y recursos protegidos. | `AuthController`, `ApiConfig`, `@SecurityRequirement`, tags y respuestas 401/403. | Implementado |
| 8.2 | README explica tests, CI y despliegue. | README de API y enlaces a ambas publicaciones. | Documentado |

Los nombres de archivo de esta tabla se desarrollan con enlaces y explicación en las secciones de cada consigna, más abajo.

## Reconstrucción paso a paso de lo realizado

Esta es una secuencia técnica de la solución final, sustentada en código e informes existentes. No pretende que cada paso corresponda exactamente a un único commit.

| Paso | Qué se hizo | Cómo se hizo | Por qué se hizo | Consigna |
|---|---|---|---|---|
| 1 | Continuar TP2 sin rehacer el proyecto. | Misma aplicación Maven, dominio y persistencia; trabajo en `tp3`. | Conservar el CRUD, las FK y las transacciones anteriores. | Punto de partida |
| 2 | Preparar PostgreSQL local reproducible. | Compose con `postgres:17`, puerto local 55432 y volumen persistente. | Separar el servicio de los PostgreSQL ya instalados y conservar datos manuales. | Soporte local de 1 y 7 |
| 3 | Agregar seguridad y herramientas de pruebas. | Starter Security, Resource Server/JOSE, módulos Testcontainers y soporte de pruebas de seguridad. | Usar componentes existentes para JWT y probar con el motor real. | 1 |
| 4 | Externalizar configuración. | Placeholders en properties; perfil prod sin valores locales para credenciales y secreto. | Ejecutar la misma aplicación en varios entornos sin versionar secretos reales. | 1 |
| 5 | Incorporar Usuario y registro. | Dominio, puerto, adapter JPA, V5, BCrypt y endpoint de registro. | Guardar identidad persistente sin contraseñas recuperables y limitar el registro a USER. | 2 |
| 6 | Implementar login y recepción de JWT. | Verificación BCrypt, emisión HS256 y filtro Bearer con decoder. | Evitar reenviar la contraseña en cada operación e identificar cada solicitud. | 3 |
| 7 | Incorporar dueño de listas. | V6, relación ManyToOne y dueño tomado del contexto autenticado. | Vincular los recursos a una identidad confiable conservando datos viejos. | 4 |
| 8 | Centralizar permisos y extenderlos a favoritos. | `buscarAutorizada`, filtrado por usuario y control de origen/destino. | Evitar accesos por IDs ajenos y caminos alternativos al mismo recurso. | 4 |
| 9 | Aislar las integraciones. | Contenedor temporal, esquemas por clase, Flyway y JVM UTC. | Probar en limpio sin tocar datos personales ni depender de aliases horarios locales. | 5 |
| 10 | Automatizar la comprobación. | Surefire/Failsafe, Actions y reportes; aserción rota y restaurada según evidencia. | Detectar regresiones también fuera del equipo del desarrollador. | 5 y 6 |
| 11 | Publicar el contenedor Java con PostgreSQL externo. | Imagen multietapa, Cloudflare Containers y conexión Access/Tunnel. | Conservar Java y la base remota elegida, con datos fuera de la JVM. | 7, con diferencia de gratuidad |
| 12 | Agregar una publicación Hobby. | Misma imagen en Vercel, secretos propios para JWT, ajuste de arranque y región. | Aportar la experiencia de alojamiento gratuito de la aplicación que faltaba en Cloudflare. | 7, con DB propia |
| 13 | Registrar y ordenar la evidencia. | Swagger, README, capturas, JSON de resultados, logs resumidos y enlaces Actions. | Permitir revisar el comportamiento y explicar cada requisito sin depender solo de afirmaciones. | 8 y evidencia esperada |

## El contenedor de la base de datos: qué es y qué decisión resolvió

### Tres bases con objetivos distintos

Hay que distinguir **desarrollo manual**, **pruebas automatizadas** y **despliegue público**. No son tres nombres para el mismo contenedor.

| Entorno | Dónde corre PostgreSQL | Quién lo inicia | Datos y duración | Conexión de la aplicación |
|---|---|---|---|---|
| Desarrollo local | Contenedor Docker de `docker-compose.yml`, imagen `postgres:17`. | La persona que ejecuta Compose. | Volumen `postgres-data`; se conservan entre arranques. | Puerto local publicado; ejemplo `127.0.0.1:55432`. |
| Integraciones locales y CI | Contenedor Docker temporal, imagen `postgres:17`. | `PostgresTestDatabase` mediante Testcontainers. | Esquemas temporales; no se conserva como base personal. | Puerto dinámico y propiedades obtenidas del contenedor. |
| Publicación documentada | Servicio PostgreSQL del equipo remoto, observado como versión 18.6 en la evidencia. | Administración del equipo remoto. | Base dedicada `webii_tp3`, externa al contenedor de la API. | JDBC a listener cloudflared, luego Access/Tunnel. |

**La base pública no queda acreditada como un contenedor de PostgreSQL desplegado en Vercel o Cloudflare.** Lo que sí se empaqueta y publica allí es el contenedor **de la aplicación Java**, que se conecta a la base remota. No hay que confundirlo con los dos contenedores de PostgreSQL usados en desarrollo y pruebas.

### Imagen, contenedor, base y volumen

La **imagen** `postgres:17` es el paquete del motor y su entorno. El **contenedor** es una instancia en ejecución de esa imagen. Dentro de ese motor hay una base lógica, por ejemplo `webii_tp2`, con esquemas, tablas y filas. El **volumen** es el almacenamiento que Docker monta para que los archivos de PostgreSQL no dependan de conservar ese contenedor particular.

El contenedor ejecuta PostgreSQL real. JPA sigue enviando SQL por JDBC y el motor sigue aplicando restricciones, transacciones y persistencia. Docker cambia la forma de instalarlo y administrarlo; no lo transforma en una simulación. Docker documenta esta separación entre imagen, instancia y almacenamiento en su [descripción de la plataforma](https://docs.docker.com/get-started/docker-overview).

### Cómo se configuró el PostgreSQL de desarrollo

**Archivo que lo demuestra:** [api-blank/docker-compose.yml](api-blank/docker-compose.yml).

1. Se define un servicio llamado `postgres` con imagen `postgres:17`.
2. `POSTGRES_DB`, `POSTGRES_USER` y `POSTGRES_PASSWORD` configuran la inicialización. Los defaults `webii_tp2` son de desarrollo, heredados de la continuidad del TP2.
3. Se publica `127.0.0.1:${POSTGRES_PORT:-5432}:5432`. A la izquierda está la interfaz y el puerto del equipo; a la derecha, el puerto dentro del contenedor.
4. Se monta `postgres-data` en `/var/lib/postgresql/data`, ruta usada por esta imagen de PostgreSQL 17.
5. Un healthcheck ejecuta `pg_isready` cada cinco segundos, con timeout de cinco segundos y hasta diez reintentos. `$$POSTGRES_USER` y `$$POSTGRES_DB` hacen que esas variables se evalúen dentro del contenedor.

En este equipo ya había PostgreSQL instalados que ocupaban puertos habituales. Por eso se eligió **55432** como puerto del host, sin cambiar el 5432 interno ni modificar las otras bases. El registro de esa decisión está en [PostgreSQL local con Docker](api-blank/docs/tp3/postgresql-docker.md).

```text
Aplicación Spring ejecutada en Windows
            |
            | JDBC 127.0.0.1:55432/webii_tp2
            v
Puerto publicado por Docker → contenedor postgres:17, puerto 5432
                                           |
                                           v
                              /var/lib/postgresql/data
                                           |
                                           v
                                  volumen postgres-data
```

El enlace a `127.0.0.1` deja el puerto publicado solo en el equipo local. El nombre concreto del volumen suele llevar el prefijo del proyecto Compose; la evidencia registró `api-blank_postgres-data`. El YAML usa su nombre lógico `postgres-data`. La etiqueta `postgres:17` fija la versión mayor, pero puede recibir actualizaciones menores: no equivale a fijar un digest de imagen exacto. La evidencia local observó 17.11; la remota, 18.6.

### Qué sucede al iniciar por primera vez

Compose obtiene la imagen si hace falta, crea el servicio y monta el volumen. Si el directorio de datos está vacío, la imagen inicializa PostgreSQL con las variables indicadas. Una vez que la base acepta conexiones, el healthcheck puede reportar `healthy`.

Eso todavía no significa que existan las tablas de la aplicación. Cuando Spring se conecta, **Flyway** ejecuta las migraciones pendientes. Después, Hibernate valida el esquema porque `spring.jpa.hibernate.ddl-auto=validate`. Docker inicia el motor; Flyway mantiene las tablas del proyecto.

Con un volumen ya inicializado se reutilizan los datos existentes. Cambiar `POSTGRES_PASSWORD` en Compose no cambia por sí solo la contraseña almacenada en ese PostgreSQL. Esa diferencia entre inicialización y reutilización está documentada para la [imagen oficial](https://hub.docker.com/_/postgres).

### Cómo se levanta y cómo se conecta la API

Los siguientes son comandos para reproducir el entorno; **no se ejecutaron al redactar este informe**. Desde `api-blank`, con JDK 25 y Docker Desktop iniciado:

```powershell
$env:POSTGRES_PORT = '55432'
docker compose up -d --wait postgres
docker compose ps
$env:DATABASE_URL = 'jdbc:postgresql://127.0.0.1:55432/webii_tp2'
.\mvnw.cmd spring-boot:run
```

Se fija el puerto antes de Compose y se usa el mismo en JDBC. `-d` deja el servicio en segundo plano; `--wait` espera el estado de disponibilidad. Si se personalizaron usuario o contraseña, también hay que definirlos para el proceso Spring.

Compose puede leer un `.env`; Maven no hereda automáticamente su contenido. El proceso que ejecuta la aplicación debe recibir sus propias variables. Del mismo modo, `localhost` dentro de un contenedor se refiere a ese contenedor: la URL del ejemplo corresponde a Spring ejecutado desde Windows.

### Por qué se eligió un volumen persistente

Las operaciones manuales necesitan conservar usuarios, listas y favoritos al detener la base. El volumen separa esos archivos del ciclo de vida de una instancia. Reemplazar el contenedor por otro que monte el mismo volumen no equivale a empezar con una base vacía. Es el comportamiento documentado para los [volúmenes Docker](https://docs.docker.com/engine/storage/volumes/).

| Operación | Efecto sobre el entorno de desarrollo |
|---|---|
| Reiniciar la aplicación Java | No elimina las filas de PostgreSQL. |
| `docker compose stop postgres` | Detiene el motor; conserva contenedor y volumen. |
| `docker compose up -d --wait postgres` | Inicia o recrea lo necesario y reutiliza el volumen existente. |
| `docker compose down` | Quita servicios y red de Compose; conserva el volumen nombrado. |
| `docker compose down -v` | También elimina el volumen: se pierden sus datos. No es un paso necesario para esta práctica. |

La diferencia entre quitar contenedores y quitar también sus volúmenes está especificada en [Compose down](https://docs.docker.com/reference/cli/docker/compose/down/).

La evidencia local del 28 de octubre registró una fila antes y después de detener/iniciar el contenedor. No registró una restauración en otra máquina ni una prueba de borrar/recrear la instancia. Un volumen persistente conserva datos, pero no constituye por sí mismo una copia de seguridad.

### Por qué Compose para desarrollo y Testcontainers para pruebas

Son necesidades diferentes. En desarrollo queremos conservar el trabajo manual; en integración queremos un inicio controlado, sin filas heredadas del desarrollador. Por eso los tests **no leen el volumen de Compose** ni dependen de levantarlo previamente.

**Código que lo demuestra:** [PostgresTestDatabase.java](api-blank/src/test/java/ar/edu/unvime/apiblank/PostgresTestDatabase.java).

1. Una inicialización estática crea `PostgreSQLContainer("postgres:17")` y llama a `start()`.
2. Se obtienen URL JDBC, usuario y contraseña reales del contenedor iniciado. El puerto se asigna dinámicamente, para no competir con instalaciones locales o con otro job.
3. `nuevoEsquema()` genera un nombre aleatorio `tp3_test_...` y crea ese esquema.
4. `@DynamicPropertySource` conecta Spring al contenedor e indica el esquema, incluyendo la configuración de Flyway.
5. Flyway aplica V1–V6 sobre ese espacio vacío.
6. Las pruebas usan la API, repositorios y transacciones contra ese PostgreSQL.
7. Al terminar la clase se elimina su esquema. La limpieza normal del contenedor temporal queda a cargo de Testcontainers y su infraestructura de limpieza.

La decisión es compartir **un contenedor dentro de la JVM** y separar los esquemas de las clases, para no pagar el arranque del motor en cada método. Los métodos de una clase comparten ese esquema; se crean cuentas e identificadores únicos para aislar sus escenarios. No sería correcto afirmar que cada método obtiene un contenedor o un esquema nuevo. El patrón de contenedor compartido está descrito por [Testcontainers](https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers).

Esto demuestra comportamiento real de PostgreSQL: FK, UNIQUE, SQL de migraciones y rollback. Además, permite que GitHub Actions haga lo mismo sin conocer la base personal. Docker debe estar disponible: si no puede iniciar el contenedor, la integración falla.

### Por qué la base remota quedó fuera del contenedor Java

El despliegue documentado reutilizó el PostgreSQL del equipo remoto, con base y rol dedicados. La primera elección de Cloudflare se hizo por preferencia del usuario y luego se agregó Vercel Hobby. Los datos deben sobrevivir aunque el contenedor Java duerma o se reemplace; ubicarlos en una base externa conserva esa separación.

El equipo remoto mantiene un conector saliente `web2-tp3-tunnel.service`. Dentro del contenedor Java, `cloudflared access tcp` abre un listener en `127.0.0.1:15432`; JDBC conecta ahí y el túnel transporta la conexión hasta el PostgreSQL remoto. No se abre el puerto de PostgreSQL en el router. Access verifica el token de servicio de infraestructura.

La contrapartida de esta decisión es operativa: si se apaga la PC, PostgreSQL o el conector, la API pierde acceso a datos; también debe mantenerse vigente la credencial de Access. Compartir la base entre Cloudflare y Vercel implica compartir las filas y mantener compatibilidad de esquema. Ninguna de estas dependencias queda resuelta por el volumen Docker del entorno local.

### Cómo funciona el contenedor de la aplicación

**Archivos:** [Dockerfile](api-blank/Dockerfile) y [container-entrypoint.sh](api-blank/scripts/container-entrypoint.sh).

1. Una etapa con JDK 25 copia Maven Wrapper y fuentes, y construye el JAR.
2. La imagen de ejecución usa JRE 25, recibe el JAR y el binario cloudflared, y ejecuta como usuario sin privilegios `10001:10001`.
3. Se activa el perfil `prod` y se declara el puerto 8080.
4. Si no se configura `PG_TUNNEL_HOSTNAME`, el entrypoint inicia Java directamente.
5. Si se configura, exige las credenciales de Access, inicia el listener TCP del túnel y luego Java. Supervisa ambos procesos y detiene el restante si alguno termina.

La imagen no incluye un servidor PostgreSQL. Tampoco ejecuta las pruebas de integración al construirse: el build usa `-DskipTests`; la comprobación completa corresponde al job CI que ejecuta `verify`. Empaquetar un JAR no reemplaza pasar esas pruebas.

En Cloudflare, el Worker dirige las solicitudes a una instancia nombrada `tp3`, espera el arranque de Spring y configura inactividad de un minuto. El nombre Durable Object y su almacenamiento de coordinación no significan que las tablas de negocio estén guardadas allí: continúan en PostgreSQL. En Vercel se documentó la misma imagen con ajustes del proveedor para el arranque.

## Alcance y arquitectura de la aplicación

> “Continué la API del TP2, que ya tenía productos, listas y favoritos con PostgreSQL y Flyway. En TP3 incorporé usuarios, registro y login con JWT. Cada lista tiene un dueño y los favoritos quedan protegidos a través de esa lista. Spring Security valida la identidad; los servicios comprueban la propiedad. Agregué pruebas con PostgreSQL temporal mediante Testcontainers, CI con GitHub Actions y documenté el despliegue público.”

Relación del dominio: **Usuario → Lista → Favorito**. El catálogo de productos se consulta en DummyJSON y sigue siendo público y de solo lectura.

Dos conceptos que tenés que distinguir desde el principio:

- **Autenticación:** comprobar quién hace la solicitud. Sin una identidad válida, `401`.
- **Autorización:** comprobar qué puede hacer esa identidad sobre un recurso. Identidad válida sin permiso, `403`.

## 1. Dependencias y configuración

### 1.1. Agregar Spring Security

**Qué pide:** incorporar `spring-boot-starter-security` al proyecto Maven.

**Dónde mostrarlo:** [pom.xml](api-blank/pom.xml) y [SecurityConfig.java](api-blank/src/main/java/ar/edu/unvime/apiblank/seguridad/SecurityConfig.java).

**Cómo está resuelto:** el starter agrega la infraestructura de seguridad. El bean `SecurityFilterChain` configura las rutas públicas, la autenticación Bearer, el modo sin sesiones y las respuestas de error.

**Por qué se hizo:** “Agregar la dependencia incorpora Spring Security; después configuro su cadena de filtros para que la API use JWT y tenga las rutas públicas que necesita el práctico.”

### 1.2. Incorporar una biblioteca JWT

**Qué pide:** una biblioteca para emitir y validar tokens; JJWT aparece como ejemplo.

**Dónde mostrarlo:** dependencias `spring-security-oauth2-resource-server` y `spring-security-oauth2-jose` del POM; beans `jwtEncoder` y `jwtDecoder` en `SecurityConfig`; [JwtService.java](api-blank/src/main/java/ar/edu/unvime/apiblank/seguridad/JwtService.java).

**Cómo está resuelto:** se usa Nimbus JOSE/JWT mediante Spring Security. `NimbusJwtEncoder` firma; `NimbusJwtDecoder` valida. El algoritmo configurado es **HS256**, que usa un secreto compartido para firmar y verificar.

**Por qué se hizo:** “La consigna permite elegir la biblioteca. Elegí la integración de Spring con Nimbus porque resuelve emisión, validación y filtro Bearer con componentes existentes.”

El nombre `oauth2-resource-server` no significa que implementamos un proveedor OAuth o login con Google: usamos su soporte para recibir JWT Bearer propios.

### 1.3. Dependencias de pruebas y Testcontainers

**Qué pide:** herramientas de pruebas unitarias y de integración con PostgreSQL real.

**Dónde mostrarlo:** en el POM, `spring-boot-starter-test`, `spring-security-test`, `testcontainers-junit-jupiter` y `testcontainers-postgresql`.

**Cómo está resuelto:** las dependencias de pruebas tienen alcance `test`. Se declaran expresamente `testcontainers`, `testcontainers-junit-jupiter` y `testcontainers-postgresql`, como enumera el enunciado. La versión queda administrada por Spring Boot. `spring-security-test` permite integrar la cadena de seguridad en las pruebas con MockMvc.

**Por qué se hizo:** “Las unitarias prueban reglas aisladas. Para integración levanto un PostgreSQL real y temporal con Docker; así también compruebo SQL, restricciones y migraciones.”

### 1.4. Variables de entorno y valores locales

**Dónde mostrarlo:** [application.properties](api-blank/src/main/resources/application.properties), [application-prod.properties](api-blank/src/main/resources/application-prod.properties) y la tabla de variables del [README de la API](api-blank/README.md).

| Configuración | Variable | Criterio aplicado |
|---|---|---|
| Conexión JDBC | `DATABASE_URL` | Tiene una alternativa local construida con host, puerto y nombre de base. |
| Credenciales PostgreSQL | `POSTGRES_USER`, `POSTGRES_PASSWORD` | Valores locales en el perfil base; obligatorias en `prod`. |
| Firma JWT | `JWT_SECRETO` | Valor de desarrollo local; obligatorio en `prod`. |
| Duración del token | `JWT_VIGENCIA_MINUTOS` | 30 minutos por defecto; debe ser positiva. |
| Puerto HTTP | `PORT` | 8080 por defecto. |

La sintaxis `${VARIABLE:valorPorDefecto}` permite reemplazar configuración sin editar el código. Para JWT, `jwtKey` exige al menos **32 bytes UTF-8** de secreto.

**Por qué se hizo:** “El mismo código se ejecuta en local y en el despliegue; cambia la configuración del entorno. Los valores locales simplifican el arranque, mientras que el perfil de producción exige las credenciales y el secreto reales.”

Poder arrancar sin definir estas variables no elimina los requisitos de tener JDK y una base accesible. Además, un `.env` de Compose no se carga automáticamente en el proceso Maven.

### 1.5. Surefire y Failsafe

**Dónde mostrarlo:** plugins del POM. Failsafe está ligado a los goals `integration-test` y `verify`; ambos plugins configuran la JVM en UTC.

| Comando desde `api-blank` | Qué ejecuta en este proyecto |
|---|---|
| `.\mvnw.cmd --batch-mode test` | Pruebas unitarias `*Test` mediante Surefire. |
| `.\mvnw.cmd --batch-mode verify` | Unitarias y después integraciones `*IT` mediante Failsafe. |

**Por qué se hizo:** “`verify` recorre las fases anteriores del ciclo Maven. Por eso ejecuta las unitarias y además las integraciones. No alcanza con `test` para comprobar todo el TP.”

**Aclaraciones técnicas:**

- **¿Por qué UTC?** Para que las pruebas no dependan de la zona horaria o de los alias del equipo que las ejecuta.
- **¿Por qué usar el wrapper?** Para utilizar la versión de Maven definida por el proyecto.
- **¿Necesitás Docker para todo?** Para las integraciones con Testcontainers sí; las unitarias no levantan ese PostgreSQL.

## 2. Usuario: dominio, persistencia y registro

### 2.1. Dominio y puerto

**Dónde mostrarlo:** [Usuario.java](api-blank/src/main/java/ar/edu/unvime/apiblank/usuario/Usuario.java), [Rol.java](api-blank/src/main/java/ar/edu/unvime/apiblank/usuario/Rol.java) y [UsuarioRepository.java](api-blank/src/main/java/ar/edu/unvime/apiblank/usuario/UsuarioRepository.java).

`Usuario` es un `record` con `id`, `email`, `passwordHash` y `rol`. El enum admite `USER` y `ADMIN`. El puerto declara buscar por email y crear; el servicio depende de ese contrato.

**Por qué se hizo:** “Conservo el patrón de TP2. El dominio representa al usuario y el puerto define qué necesita el negocio de la persistencia, sin hacer que el servicio dependa directamente de Spring Data.”

### 2.2. Entity, repositorio JPA y adapter

**Dónde mostrarlo:** [UsuarioEntity](api-blank/src/main/java/ar/edu/unvime/apiblank/usuario/persistencia/UsuarioEntity.java), [UsuarioJpaRepository](api-blank/src/main/java/ar/edu/unvime/apiblank/usuario/persistencia/UsuarioJpaRepository.java) y [UsuarioRepositoryAdapter](api-blank/src/main/java/ar/edu/unvime/apiblank/usuario/persistencia/UsuarioRepositoryAdapter.java).

- `UsuarioEntity` mapea la tabla mediante JPA; tiene constructor vacío y persiste el rol como texto con `EnumType.STRING`.
- `UsuarioJpaRepository` extiende `JpaRepository` y declara `findByEmail`.
- `UsuarioRepositoryAdapter` implementa el puerto, delega en JPA y convierte la entidad al dominio.

**Por qué se hizo:** “La entidad resuelve el mapeo a PostgreSQL; el record del dominio no contiene anotaciones JPA. El adapter conecta esos dos modelos.”

**¿Por qué el rol como texto?** Porque guardar `USER` o `ADMIN` evita vincular el dato a la posición numérica del enum.

### 2.3. Migración V5

**Dónde mostrarlo:** [V5__create_usuarios.sql](api-blank/src/main/resources/db/migration/V5__create_usuarios.sql).

Crea `usuarios` con ID generado, email `UNIQUE NOT NULL`, hash obligatorio y rol con un `CHECK` que permite `USER` o `ADMIN`.

**Por qué se hizo:** “Flyway evoluciona el esquema con V5; Hibernate solo lo valida. La unicidad del email también se exige en PostgreSQL, no depende únicamente de un if en Java.”

**¿Por qué no editar V1?** Porque las migraciones anteriores ya representan una historia aplicada. Una nueva versión mantiene esa historia y permite actualizar las bases existentes.

### 2.4. Registro, BCrypt y email duplicado

**Dónde mostrarlo:** `registro` en [AuthController](api-blank/src/main/java/ar/edu/unvime/apiblank/usuario/AuthController.java), `registrar` en [UsuarioServiceImpl](api-blank/src/main/java/ar/edu/unvime/apiblank/usuario/UsuarioServiceImpl.java), el bean `passwordEncoder` y [ApiExceptionHandler](api-blank/src/main/java/ar/edu/unvime/apiblank/error/ApiExceptionHandler.java).

Recorrido de `POST /api/auth/registro`:

1. `@Valid` verifica email y contraseña en el DTO `Credenciales`.
2. El servicio limita la contraseña a 72 bytes UTF-8, límite de BCrypt, y normaliza el email con `trim` y minúsculas.
3. Si el email ya existe, lanza `ConflictoException`.
4. Calcula `encoder.encode(password)` y llama al puerto con **`Rol.USER` fijo**.
5. El adapter guarda y fuerza el flush. La restricción UNIQUE cubre registros simultáneos que superen la consulta previa.
6. El controller responde `201` con `id`, `email` y `rol`; no devuelve contraseña ni hash.

El handler convierte tanto el conflicto detectado por el servicio como una violación de integridad en `409`.

**Por qué se hizo:** “Guardo un hash BCrypt con sal, no una contraseña cifrada para recuperarla después. El registro siempre asigna USER; el cliente no puede promoverse a ADMIN. Controlo duplicados antes de guardar y la base garantiza la unicidad incluso si dos registros llegan juntos.”

**Cómo demostrarlo:** registrar una cuenta nueva → `201`; repetirla → `409`; revisar que la respuesta no tiene `password` ni `passwordHash`. Las pruebas también consultan el hash almacenado y verifican `matches`.

**Aclaraciones técnicas:**

- **¿Cómo se compara la contraseña?** Con `PasswordEncoder.matches`, usando el hash guardado. No comparando dos llamadas independientes a `encode`, porque BCrypt incorpora sal.
- **¿Por qué 72 bytes y no solo caracteres?** Algunos caracteres UTF-8 ocupan más de un byte.
- **¿Cómo aparece un ADMIN?** Mediante una preparación de la cuenta en la base por un operador autorizado; el registro público no permite elegir ese rol.

## 3. Autenticación: login y JWT

### 3.1. Validar credenciales y emitir el token

**Dónde mostrarlo:** `login` en `AuthController`, `autenticar` en `UsuarioServiceImpl` y `emitir` en `JwtService`.

1. Se valida el JSON de entrada y se normaliza el email.
2. Se busca al usuario y se compara la clave con BCrypt.
3. Si la cuenta no existe, igualmente se hace una comparación contra un hash de descarte para reducir diferencias de tiempo.
4. Email inexistente o clave incorrecta generan el mismo error `401`.
5. Si las credenciales coinciden, se firma el JWT y se devuelve `token`, `tokenType: Bearer` y `expiresIn` en segundos.

| Claim emitido | Significado |
|---|---|
| `sub` | ID del usuario como texto. |
| `rol` | `USER` o `ADMIN`. |
| `iss` | Emisor esperado: `web2-practica`. |
| `iat` | Momento de emisión. |
| `exp` | Vencimiento. |

**Por qué se hizo:** “El login prueba la contraseña una vez y emite un token firmado. Las siguientes solicitudes usan ese token, así no vuelvo a enviar la contraseña en cada operación.”

**¿El JWT oculta sus datos?** No. Está firmado, no cifrado. La firma permite detectar alteraciones; sus claims no deben contener contraseñas ni secretos.

### 3.2. Filtro Bearer y contexto de seguridad

**Dónde mostrarlo:** `jwtDecoder`, `securityFilterChain` en `SecurityConfig` y [ContextoSeguridad](api-blank/src/main/java/ar/edu/unvime/apiblank/seguridad/ContextoSeguridad.java).

```text
Authorization: Bearer <token>
        ↓
Filtro Bearer de Spring Security
        ↓
JwtDecoder valida firma, emisor, tiempos e identidad
        ↓
SecurityContextHolder contiene la autenticación de esta solicitud
        ↓
ContextoSeguridad expone usuarioId y esAdmin a los servicios
```

La validación adicional exige `sub` numérico positivo y rol permitido. El convertidor transforma el claim de rol en una autoridad con prefijo `ROLE_`.

**Por qué se hizo:** “No escribí un filtro propio porque Spring ya aporta el filtro Bearer. Lo conecté mediante `oauth2ResourceServer`, le configuré el decoder y uso el contexto de seguridad para consultar la identidad validada.”

Se usa `SessionCreationPolicy.STATELESS`: el servidor no mantiene una sesión de login entre solicitudes. CSRF está desactivado porque esta API usa el token enviado explícitamente en `Authorization` y no cookies de autenticación.

### 3.3. Rutas públicas, protegidas y 401

**Dónde mostrarlo:** los `requestMatchers` y `.anyRequest().authenticated()` de `SecurityConfig`.

| Ruta | Regla |
|---|---|
| `/api/auth/**` | Público; actualmente se implementan POST registro y login. |
| `/api/productos/**` | Público; el catálogo implementa solamente GET. |
| `GET /health` y documentación Swagger/OpenAPI | Públicos para inspección y demostración. |
| `/api/listas/**` y `/api/favoritos/**` | Requieren identidad válida. La propiedad se revisa después, en servicios. |

Se usan los dos patrones públicos del enunciado. Esto no crea operaciones nuevas: un método no implementado en productos responde `405`, y una ruta auth inexistente responde `404`, sin que Security exija antes un token. La prueba `rutasPublicas_operacionesNoImplementadas_noExigenToken` fija ese contrato. Cualquier operación futura bajo esos prefijos heredará su acceso público y deberá respetar ese diseño.

**Cómo demostrarlo:** `GET /api/listas` sin token → `401`; login correcto → `200`; repetir el GET con ese token → `200`. Con token malformado, vencido o firmado con otra clave → `401`.

**Aclaraciones técnicas:**

- **¿Modificar el rol dentro del JWT me hace ADMIN?** No: cambiar el contenido invalida la firma.
- **¿Logout en Swagger revoca el token?** No. La interfaz deja de enviarlo; el token sigue siendo válido hasta su vencimiento.
- **¿Un cambio de rol invalida los tokens anteriores?** No. Un nuevo login emite el rol actualizado, pero los tokens anteriores conservan sus claims hasta vencer.
- **¿Una contraseña corta en login siempre da 401?** No. El DTO compartido exige ocho caracteres; una entrada que no supera validación responde `400`. Para demostrar credenciales incorrectas usá una clave válida en formato pero equivocada.

## 4. Autorización: propiedad de recursos y rol

### 4.1. Lista pertenece a Usuario y migración V6

**Dónde mostrarlo:** [Lista.java](api-blank/src/main/java/ar/edu/unvime/apiblank/lista/Lista.java), [ListaEntity.java](api-blank/src/main/java/ar/edu/unvime/apiblank/lista/persistencia/ListaEntity.java) y [V6__add_usuario_id_a_listas.sql](api-blank/src/main/resources/db/migration/V6__add_usuario_id_a_listas.sql).

El dominio agrega `usuarioId`. La entidad agrega `@ManyToOne(fetch = LAZY)` con `@JoinColumn(name = "usuario_id")`. V6 agrega la columna, la FK a usuarios y un índice por dueño.

**Por qué se hizo:** “Muchas listas pueden pertenecer al mismo usuario. En el dominio guardo el ID; en persistencia JPA expresa la relación. La FK garantiza que un dueño no nulo corresponda a un usuario existente.”

Las listas que venían de TP2 quedan con dueño nulo y se conservan. Solo ADMIN puede acceder a ellas. No se asignan a una cuenta arbitraria ni se borran.

**¿Por qué la columna admite NULL?** Para mantener los datos anteriores sin inventar un dueño. Las nuevas listas creadas por el servicio toman siempre el usuario autenticado.

### 4.2. El dueño sale de la identidad

**Dónde mostrarlo:** `crear` en [ListaServiceImpl](api-blank/src/main/java/ar/edu/unvime/apiblank/lista/ListaServiceImpl.java) y [ListaRequest](api-blank/src/main/java/ar/edu/unvime/apiblank/lista/ListaRequest.java).

La llamada decisiva es:

```java
repository.crear(request.nombre().trim(), seguridad.usuarioId())
```

El request solo define el nombre. Aunque el cliente agregue un campo `usuarioId`, no se usa para asignar propiedad.

**Por qué se hizo:** “La identidad viene de un token validado. Si aceptara el dueño desde el body, el cliente podría crear listas a nombre de otra persona.”

### 4.3. Comprobar propiedad, excepción ADMIN y 403

**Dónde mostrarlo:** `buscarAutorizada` en `ListaServiceImpl`, `exigirPropiedad` en `ContextoSeguridad` y su uso en [FavoritoService](api-blank/src/main/java/ar/edu/unvime/apiblank/favorito/FavoritoService.java).

La regla central sigue este orden:

1. Buscar la lista; si no existe, `404`.
2. Si es ADMIN, permitir.
3. Si es USER, comparar dueño e ID autenticado; si no coinciden, `403`.
4. Recién después realizar la operación.

Se aplica al obtener y eliminar listas, consultar sus favoritos y moverlos. En `moverFavoritos` se comprueban **origen y destino antes de escribir**. El movimiento conserva `@Transactional` de TP2 y rechaza origen igual a destino con `400`.

Los favoritos también están protegidos: para consultar, crear o borrar se comprueba su lista; para actualizar se controla la lista original y la de destino. Proteger solo `/api/listas` dejaría un acceso alternativo mediante `/api/favoritos/{id}`.

**Por qué se hizo:** “Tener un ID o conocer la URL de una lista no da permiso. El servicio verifica que sea mía o que tenga rol ADMIN. Si muevo favoritos debo estar autorizado en ambos extremos.”

### 4.4. Listado filtrado por usuario

**Dónde mostrarlo:** `listar` en `ListaServiceImpl`, `buscarPorUsuarioId` en el adapter y `findByUsuario_IdOrderByIdAsc` en [ListaJpaRepository](api-blank/src/main/java/ar/edu/unvime/apiblank/lista/persistencia/ListaJpaRepository.java).

USER consulta únicamente sus listas; ADMIN consulta todas. El filtro se aplica en la consulta de persistencia. `FavoritoService.listar` aplica el mismo criterio a favoritos mediante su lista.

**Por qué se hizo:** “No devuelvo todo para que el cliente oculte lo ajeno: la API consulta y devuelve solo lo autorizado.”

### 4.5. Dónde vive la regla y por qué

**Dónde mostrarlo:** `ListaServiceImpl.buscarAutorizada` y la sección «Endpoints y propiedad» del README de la API.

**Justificación de la ubicación:**

> “Spring Security autentica la solicitud y bloquea rutas sin token. La autorización por propiedad está en el servicio porque depende del recurso que se consulta y de la operación de negocio. `buscarAutorizada` concentra esa regla y también la reutilizan los favoritos. Si estuviera solamente en el Controller, otra llamada al servicio podría evitar esa comprobación.”

**Cómo demostrarlo:** crear una lista con A; con A, obtenerla → `200`; cambiar al token de B y obtenerla → `403`; listar con B → no aparece; usar un ADMIN preparado y volver a obtenerla → `200`.

| Situación | Código esperado |
|---|---|
| Sin token válido | `401` |
| Lista existente de otra persona, con USER | `403` |
| Lista inexistente, con identidad válida | `404` |
| Lista propia o acceso ADMIN permitido | `200` para lectura |
| Eliminar una lista autorizada con favoritos | `409` |

## 5. Pruebas

### 5.1. Unitarias exigidas

| Requisito | Prueba para abrir | Qué comprueba |
|---|---|---|
| Lista nueva asociada al autenticado | [ListaServiceImplTest](api-blank/src/test/java/ar/edu/unvime/apiblank/lista/ListaServiceImplTest.java), `crear_usuarioAutenticado_asignaPropiedad` | Simula ID autenticado 10 y verifica su uso al crear la lista. |
| Nunca guardar la contraseña en texto plano | [UsuarioServiceImplTest](api-blank/src/test/java/ar/edu/unvime/apiblank/usuario/UsuarioServiceImplTest.java), `registrar_password_guardaSoloHashConRolUser` | Captura el hash enviado al puerto; es distinto de la clave y BCrypt lo reconoce; el rol es USER. |

**Por qué se hizo:** “Aíslo el servicio con mocks de sus colaboradores. En la prueba de usuario uso BCrypt real para comprobar que el dato enviado a persistencia es un hash válido.”

### 5.2. PostgreSQL real, temporal y migrado

**Dónde mostrarlo:** [PostgresTestDatabase](api-blank/src/test/java/ar/edu/unvime/apiblank/PostgresTestDatabase.java), [SeguridadIT](api-blank/src/test/java/ar/edu/unvime/apiblank/seguridad/SeguridadIT.java) y [FlywayMigrationIT](api-blank/src/test/java/ar/edu/unvime/apiblank/FlywayMigrationIT.java).

`PostgresTestDatabase` inicia `postgres:17` con Testcontainers. La URL, puerto y credenciales provienen del contenedor. Cada clase de integración trabaja sobre un esquema aislado; `@DynamicPropertySource` inyecta esa configuración y Flyway aplica las migraciones.

**Por qué se hizo:** “No dependo del PostgreSQL instalado en mi PC ni uso una base en memoria que se comporte diferente. Comparto el contenedor durante la ejecución, pero cada clase aísla su esquema. Si Docker no está disponible, la integración falla: no se considera aprobada ni se omite silenciosamente.”

No se crea un contenedor distinto por cada método. Las solicitudes de `SeguridadIT` usan **MockMvc con el contexto Spring y los filtros reales**: no abren un servidor HTTP público. El repositorio, PostgreSQL, BCrypt y JWT sí son reales; el catálogo externo se sustituye para no depender de DummyJSON.

### 5.3. Los seis escenarios obligatorios

Todos se encuentran en `SeguridadIT`; algunos se comprueban dentro de métodos que agrupan varias aserciones o de sus helpers.

| Consigna | Dónde se comprueba | Esperado |
|---|---|---|
| Login válido | Helper `login`, usado al preparar cuentas para los casos; extrae el token de la respuesta. | `200` con token |
| Login inválido | `registro_yLogin_guardanHashYNoExponenSecretos` | `401` |
| Lista propia | `listas_usuarioAjeno_noPuedeLeerBorrarNiMover`, lectura final de la propia. | `200` |
| Lista ajena | Ese mismo método, lecturas y operaciones denegadas. | `403` |
| Listas sin token | Parametrizada `acceder_tokenAusenteOInvalido_responde401` | `401` |
| ADMIN accede a lista ajena | `admin_listasAjenasYLegadas_accedeSinCambiarPropiedad` | `200` |

También se cubren tokens vencidos o con firma ajena, registro concurrente, intentos de elegir ADMIN desde el body y operaciones sobre favoritos ajenos. Las integraciones conservan la comprobación de rollback del movimiento de TP2.

### 5.4. Nombres, ejecución y resultados

Las clases unitarias terminan en `Test`; las integraciones, en `IT`. Las de servicios están en su paquete bajo `src/test/java`; `SeguridadIT` se trasladó a `seguridad`, junto al código de seguridad que verifica. `ApiIntegrationIT` integra la aplicación completa y está junto a `ApiBlankApplication`; `FlywayMigrationIT` comprueba las migraciones. El helper compartido `PostgresTestDatabase` expone las operaciones de esquema necesarias para reutilizarlo desde el paquete de seguridad. Los métodos describen operación, escenario y resultado.

**Por qué se hizo:** “El sufijo decide qué plugin ejecuta la clase. Además, el nombre del método permite entender qué comportamiento se está comprobando.”

El **3 de noviembre se ejecutó `clean verify`** con JDK 25 y Docker: **58 unitarias y 25 integraciones, 83 ejecuciones Java**, sin fallos, errores u omisiones. La integración adicional comprueba los patrones públicos. Las cinco pruebas del Worker pasaron con Node 24.22.1: **88 ejecuciones en total**. Los resultados del 29 de octubre, 82 Java y cinco del Worker, se conservan como históricos y no se confunden con esta ejecución. Una parametrizada puede producir varias ejecuciones; estas cantidades no son un porcentaje de cobertura.

Reportes XML generados por la nueva ejecución limpia:

| Carpeta de reportes | XML encontrados | Ejecuciones registradas | Fallos / errores / omitidas |
|---|---:|---:|---|
| `api-blank/target/surefire-reports` | 9 | 58 | 0 / 0 / 0 |
| `api-blank/target/failsafe-reports` | 3 | 25 | 0 / 0 / 0 |

`target` es salida generada localmente, por lo que otra persona puede no tenerla en su clon; para la entrega se usan además el informe versionado de evidencia y los enlaces Actions. La primera ejecución del Worker con el Node 22 de la terminal falló por no reconocer el archivo `.ts`; se repitió con el Node 24.22.1 requerido por el proyecto y sus cinco casos pasaron. No se cambió el código del Worker para ocultar esa incompatibilidad de entorno.

**Aclaraciones técnicas:**

- **¿Una unitaria demuestra que la FK funciona?** No. Eso requiere la integración con PostgreSQL.
- **¿Flyway corre en CI?** Sí, contra los esquemas temporales de las integraciones.
- **¿Qué significa que esté todo verde?** Que los casos ejecutados pasaron; no demuestra ausencia de cualquier defecto posible.

## 6. Integración continua

### 6.1. Workflow y ejecución de verify

**Dónde mostrarlo:** [.github/workflows/ci.yml](.github/workflows/ci.yml).

El workflow `Java CI` escucha `push` y `pull_request` **sin filtro de ramas**, además de ejecución manual. Así se cumple cada push y se incluye tanto la rama de entrega `TP3` como cualquier rama nueva.

El job Java:

1. Descarga el repositorio.
2. Prepara Temurin Java 25 y caché Maven.
3. Ejecuta `./mvnw --batch-mode verify` dentro de `api-blank`.
4. Publica reportes Surefire y Failsafe incluso si falla una prueba, mediante `if: always()`.

Existe además un job para verificar y probar el Worker de Cloudflare.

**Por qué se hizo:** “CI reproduce la comprobación en un runner limpio. Tener verde solo en mi equipo no alcanza; el workflow también debe poder construir y ejecutar las integraciones sin mi configuración local.”

### 6.2. Demostrar rojo y recuperación

**Dónde mostrarlo:** apartado «CI y publicación» de [EVIDENCIA-TP3](api-blank/docs/EVIDENCIA-TP3.md).

La evidencia registra que se cambió una aserción para esperar ADMIN cuando el registro produce USER. El workflow detectó el fallo y después volvió a verde al restaurarla:

- [Corrida con fallo deliberado](https://github.com/Niquinhoo/Web2Practica/actions/runs/40817485164).
- [Recuperación registrada](https://github.com/Niquinhoo/Web2Practica/actions/runs/40817598752).
- [CI final citado en la documentación anterior](https://github.com/Niquinhoo/Web2Practica/actions/runs/40820858514).
- [CI de entrega del 3 de noviembre, commit 82a32c6](https://github.com/Niquinhoo/Web2Practica/actions/runs/41730538085): jobs Java/PostgreSQL y Worker exitosos sobre la rama `TP3`, con las nuevas rutas públicas, dependencia explícita y paquete de pruebas corregido.
- [CI de la revisión desplegada, commit 994e5b7](https://github.com/Niquinhoo/Web2Practica/actions/runs/41732115409): ambos jobs exitosos antes de publicar en Cloudflare.

**Por qué se hizo:** “No solo configuré un archivo YAML: la evidencia muestra que una aserción incorrecta vuelve rojo el pipeline y que restaurarla lo hace pasar. Así compruebo que el workflow detecta fallos de las pruebas.”

No hace falta volver a romper una prueba durante la defensa: se pueden presentar esas corridas históricas.

### 6.3. Base limpia y permiso del wrapper

El runner usa Docker mediante Testcontainers. No hay un servicio PostgreSQL adicional en el workflow ni conexión a la base personal. Las propiedades dinámicas de las pruebas apuntan al contenedor.

El wrapper `api-blank/mvnw` está registrado en Git con modo **100755**, es decir, ejecutable. Ante `Permission denied`, la consigna propone registrar ese permiso con `git update-index --chmod=+x api-blank/mvnw` desde la raíz.

**Cómo leer un fallo:** Actions → Java CI → job Java → paso de Maven. Buscar clase, método y aserción; consultar los artefactos `test-reports` para más detalle.

**¿CI despliega automáticamente?** Este workflow verifica el proyecto; no contiene un paso de despliegue. La publicación documentada fue un paso separado.

## 7. Despliegue

### 7.1. Cloudflare como elección expresa para la entrega

**Qué pedía el texto original:** publicar en un proveedor gratuito y conectarse a PostgreSQL remoto. **Instrucción actual del usuario:** conservar Cloudflare y no aplicar literalmente la exigencia de proveedor gratuito. La implementación mantiene el resto de los requisitos: aplicación pública, configuración por entorno, PostgreSQL remoto, migraciones y evidencia de funcionamiento.

**Dónde mostrarlo:** [guía de Vercel](deployment/vercel/README.md), [guía de Cloudflare](deployment/cloudflare/README.md) y el apartado de despliegue en `EVIDENCIA-TP3`.

Se documentaron dos publicaciones de la misma API Java:

| Publicación | Qué explicar |
|---|---|
| Cloudflare Containers | Proveedor principal elegido para la entrega. Usa Workers Paid, una instancia `basic` y PostgreSQL remoto por Access/Tunnel. |
| Vercel Hobby | Experiencia adicional del 29 de octubre, conservada como evidencia histórica. No es necesaria para justificar la elección actual del usuario. |

**PostgreSQL corre en tu equipo remoto**, no es una base administrada contratada en Vercel. Se accede mediante Cloudflare Access y Tunnel. La arquitectura depende de ese equipo y del túnel para operar con datos.

**Por qué se hizo:** se conserva la plataforma que el usuario ya eligió para el proyecto, manteniendo la aplicación Java dentro de un contenedor, los datos fuera de su ciclo de vida y el acceso a la base mediante Tunnel. La elección permite continuar con la infraestructura configurada, sin migrar servicios para esta preparación de entrega. No se afirma que Workers Paid sea gratuito ni que la preferencia del usuario constituya una aprobación de la cátedra.

### 7.2. Variables y recorrido hasta la base

**Dónde mostrarlo:** tabla de configuración de la guía de despliegue y [Dockerfile](api-blank/Dockerfile), sin exhibir valores secretos.

Se configura perfil `prod`, URL JDBC, credenciales PostgreSQL y `JWT_SECRETO`. El contenedor también necesita las variables del cliente de Cloudflare Access para abrir la conexión TCP del túnel.

```text
Swagger o cliente → HTTPS → aplicación Spring
                                ↓
                         JDBC a listener local
                                ↓
                       cloudflared + Access/Tunnel
                                ↓
                         PostgreSQL remoto
```

**Por qué se hizo:** “Hay dos credenciales con funciones distintas: el JWT identifica al usuario de la API; el token de servicio de Access permite que el contenedor llegue a PostgreSQL. El usuario no recibe las credenciales de infraestructura.”

Los datos viven fuera del contenedor Java, por lo que no desaparecen al detenerlo o reemplazarlo. La guía de Vercel registra además el ajuste `-XX:TieredStopAtLevel=1` y la región `gru1` para reducir el tiempo de arranque observado; no son requisitos del JWT.

### 7.3. Migraciones, health, logs y evidencia

La evidencia existente registra:

- Aplicación de V1–V6 en la base remota durante la primera publicación.
- En Vercel, Flyway encontró el esquema en versión 6 y lo reutilizó. No tenía que volver a crear tablas ya migradas.
- Respuestas públicas `200` de health, `401` de listas sin token y `200` con una identidad válida.
- Registro, login y operaciones sobre listas/favoritos contra PostgreSQL real.
- Logs resumidos de arranque y capturas conservadas en el repositorio.

**Evidencia vigente de Cloudflare, 3 de noviembre:** [54 solicitudes HTTP](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/http-publico-completo.json), [runtime](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/runtime.json), [health 200](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/health-swagger-200.png), [listas 401](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/listas-swagger-401.png) y [listas 200](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/listas-swagger-200.png). La evidencia adicional de Vercel se conserva en [su guía](deployment/vercel/README.md), con el plan y las capturas de esa publicación.

**¿Health demuestra que PostgreSQL funciona?** No por sí solo. [HealthController](api-blank/src/main/java/ar/edu/unvime/apiblank/web/HealthController.java) devuelve un JSON fijo con `status: ok` y `service: api-blank`. Para acreditar la base se muestran los logs de Flyway y las operaciones que realmente leen o escriben datos.

**¿Tiene que seguir encendido para entregar?** La consigna dice que alcanza con obtener evidencia. Para una demostración en vivo sí hacen falta el servicio accesible y el equipo de PostgreSQL con su túnel activos. Las URLs están documentadas; este informe no comprueba su disponibilidad actual.

## 8. Documentación

### 8.1. Swagger con auth y recursos protegidos

**Dónde mostrarlo:** `AuthController`, [ApiConfig](api-blank/src/main/java/ar/edu/unvime/apiblank/config/ApiConfig.java), [ListaController](api-blank/src/main/java/ar/edu/unvime/apiblank/lista/ListaController.java) y [FavoritoController](api-blank/src/main/java/ar/edu/unvime/apiblank/favorito/FavoritoController.java).

- Auth tiene las operaciones de registro y login documentadas.
- `ApiConfig` declara `bearerAuth` de tipo HTTP Bearer con formato JWT.
- Listas y favoritos declaran `@SecurityRequirement(name = "bearerAuth")`, descripciones de acceso y respuestas `401`/`403`.
- Swagger muestra el botón **Authorize**. Se pega solo el JWT; la interfaz agrega el prefijo Bearer.

**Por qué se hizo:** “OpenAPI describe el contrato y Swagger permite ejercitarlo. La protección real está en Spring Security y los servicios; una anotación de documentación no reemplaza esa seguridad.”

### 8.2. README: pruebas, CI, despliegue y autorización

**Dónde mostrarlo:** [README general](README.md), [README de la API](api-blank/README.md), guías de despliegue y la demostración reproducible de este informe.

El README de la API explica cómo arrancar, las variables, registro/login, endpoints, autorización en el servicio, migraciones, pruebas y lectura de CI. Enlaza las guías con los pasos e incidentes del despliegue.

**Por qué se hizo:** “La documentación permite que otra persona ejecute el proyecto, sepa qué variable configurar, corra las pruebas y entienda por qué la regla de propiedad está en el servicio.”

## Recorrido breve para demostrarlo por consignas

El apartado siguiente detalla la preparación y los cuerpos JSON. Este orden conecta lo que mostrás con los puntos del enunciado:

| Paso | Qué mostrar | Consigna |
|---|---|---|
| 1 | POM, configuración por entorno y separación Test/IT. | 1 |
| 2 | Registro nuevo `201`, respuesta sin secretos y duplicado `409`. | 2 |
| 3 | Login incorrecto `401`, correcto `200` y token. | 3 |
| 4 | Listas sin token `401`; autorizar A y crear lista cuyo dueño es A. | 3 y 4 |
| 5 | A lee su lista `200`; B intenta leerla `403`; listado de B no la incluye. | 4 |
| 6 | Movimiento con origen o destino ajeno `403`; mostrar autorización previa a escrituras. | 4 |
| 7 | ADMIN preparado lee lista ajena `200`, o mostrar la integración que lo verifica. | 4 y 5 |
| 8 | Abrir las dos unitarias obligatorias y `SeguridadIT`, explicando sus aserciones. | 5 |
| 9 | Workflow, corrida roja deliberada, recuperación y reportes. | 6 |
| 10 | Evidencia de Hobby, health, logs Flyway y capturas de acceso protegido. | 7 |
| 11 | Swagger con Bearer y README con la justificación de autorización. | 8 |

Prepará cuentas descartables con nombres únicos y anotá los IDs devueltos. Cambiá la autorización al pasar de A a B. Si demostrás ADMIN, necesitás una cuenta preparada y un login nuevo; no alcanza con editar el token. En una demostración de movimiento permitido, el origen se elimina: usá listas creadas para esa demostración.

## Demostración reproducible en Swagger

Esta sección conserva la demostración útil de la guía anterior, ahora integrada al informe. **Son instrucciones con resultados esperados, no solicitudes ejecutadas durante esta revisión.**

### A. Preparar la URL, la base y dos identidades

Para probar en vivo, el servicio elegido debe estar accesible y el equipo remoto debe tener activos PostgreSQL y `web2-tp3-tunnel.service`. Abrir el [Swagger principal de Cloudflare](https://web2-practica-tp3.nicotrib699.workers.dev/swagger-ui/index.html). La publicación adicional en [Vercel](https://web2-practica-tp3.vercel.app/swagger-ui/index.html) conserva sus propios tokens: no deben intercambiarse entre URLs aunque compartan la base.

Esperar el arranque en frío si corresponde. En cada endpoint usar **Try it out → Execute** y leer **Server response**, su código y cuerpo. `Example Value` es documentación de ejemplo; no demuestra una ejecución.

Empezar con **Authorize → Logout → Close**. Usar emails descartables únicos, por ejemplo `informe-a-20261103@example.com` e `informe-b-20261103@example.com`; si existen, cambiar el sufijo. Anotar los IDs reales devueltos como `USUARIO_A`, `USUARIO_B`, `LISTA_A` y `LISTA_B`. Esas etiquetas no se envían en campos numéricos: se reemplazan por los valores observados.

### B. Mostrar disponibilidad pública y protección sin token

1. `GET /health` → `200`, JSON con `status: ok` y `service: api-blank`.
2. `GET /api/productos` → acceso público; el éxito depende también del catálogo externo.
3. `GET /api/listas` sin autorización → `401`.

Esto muestra la separación entre rutas públicas y protegidas de la consigna 3. La comprobación de health no incluye por sí sola una consulta a PostgreSQL.

### C. Registrar A y comprobar hash, rol y duplicados

Ejecutar `POST /api/auth/registro`:

```json
{
  "email": "informe-a-20261103@example.com",
  "password": "Practica-A-2026!"
}
```

Debe responder `201`, con ID, email y rol USER, sin contraseña ni hash. Guardar el ID de A. Repetir el registro → `409`. Una contraseña de menos de ocho caracteres → `400` por validación.

La respuesta demuestra el contrato público de la consigna 2. Para demostrar que efectivamente se almacena un hash se muestra la aserción de `UsuarioServiceImplTest` o la consulta de `SeguridadIT`, que comprueba el valor persistido y `PasswordEncoder.matches`. No alcanza con que el controller omita el campo en su respuesta.

### D. Login y Authorization

1. Ejecutar `POST /api/auth/login` con el email de A y clave equivocada de al menos ocho caracteres → `401`.
2. Ejecutar con el mismo JSON correcto del registro → `200`, con `token`, `tokenType` y `expiresIn`.
3. Copiar solo el valor del token, sin comillas.
4. **Authorize → bearerAuth**, pegar el token sin anteponer `Bearer`, aplicar y cerrar.
5. `GET /api/listas` → `200`; para una cuenta nueva debe devolver `[]`.

Así se demuestra la consigna 3. La identidad debe venir del token validado, no de un ID elegido en una cabecera aparte o en el body.

### E. Crear una lista y demostrar que el dueño se toma del token

Con A autorizado, ejecutar `POST /api/listas`:

```json
{"nombre": "Lista de A para informe"}
```

Debe responder `201`. Guardar el ID de la lista y comprobar que su `usuarioId` coincide con A. `GET /api/listas/{id}` con ese ID → `200`; `GET /api/listas/{id}/favoritos` → `200` y colección vacía inicialmente.

El body contiene solo el nombre. El punto se demuestra también en la prueba de integración, que agrega un `usuarioId` ajeno al JSON y verifica que la lista igualmente quede asociada a la cuenta autenticada.

### F. Cambiar a B y demostrar aislamiento

Registrar B:

```json
{
  "email": "informe-b-20261103@example.com",
  "password": "Practica-B-2026!"
}
```

Hacer login, copiar su token y reemplazar la autorización de A mediante Logout y una nueva autorización. Con B:

| Solicitud | Resultado esperado | Qué demuestra |
|---|---|---|
| GET de la lista A | `403` | Lista existente sin permiso. |
| GET de favoritos de la lista A | `403` | La relación no permite saltar la propiedad. |
| DELETE de la lista A | `403` | La denegación ocurre antes de borrar. |
| GET de listas | `200`, sin la lista A | Filtrado por dueño. |
| POST de una lista B | `201`, dueño B | Asociación desde la nueva identidad. |
| GET de la lista B | `200` | Acceso propio permitido. |

Guardar el ID real de la lista B. Estos pasos muestran la consigna 4 y los casos de integración de la 5. Un ID inexistente, consultado con una identidad válida, se diferencia con `404`.

### G. Demostrar autorización de origen y destino

Con B, ejecutar `POST /api/listas/{origenId}/mover-favoritos`. El request tiene esta forma; reemplazar 123 por el ID real del destino:

```json
{"destinoId": 123}
```

Primero usar B como origen y A como destino → `403`. Después A como origen y B como destino → `403`. No deben desaparecer las listas ni moverse datos. La integración comprueba además que ambas listas continúan existiendo.

Para el camino permitido, crear dos listas descartables con el mismo usuario y mover una a la otra → `204`. Se elimina el origen, incluso si estaba vacío. `@Transactional` agrupa el movimiento y el borrado; `ApiIntegrationIT` incluye un fallo forzado de FK después del movimiento y verifica que el rollback conserve las filas en su estado anterior.

### H. Mostrar un ADMIN sin habilitar promoción pública

Si se necesita una demostración manual, un operador autorizado prepara una cuenta descartable y cambia su rol en la base **de la práctica**. Por ejemplo, después de registrarla, este SQL expresa la preparación:

```sql
UPDATE usuarios
SET rol = 'ADMIN'
WHERE email = 'informe-admin-20261103@example.com';
```

Es una instrucción de preparación; no se ejecutó al redactar el informe. Confirmar que afecta solo la cuenta prevista y hacer login otra vez, porque un JWT anterior conserva el rol anterior. Con el nuevo token, GET de las listas A y B → `200`; el listado incluye las listas de distintos dueños y puede incluir las legadas sin dueño.

Si no se prepara esa cuenta, mostrar `SeguridadIT.admin_listasAjenasYLegadas_accedeSinCambiarPropiedad` y aclarar que ADMIN se acredita mediante la integración existente. Editar el rol dentro de un JWT no es una alternativa: invalida la firma.

### I. Cerrar la demostración

Reemplazar el token por texto inválido y consultar listas → `401`; hacer Logout y repetir → `401`. Para limpiar, cada dueño puede borrar sus favoritos y luego sus listas de demostración. Una lista con favoritos responde `409` al intentar borrarla.

No hay un endpoint público para eliminar usuarios; esas cuentas permanecen. Logout deja de enviar el JWT desde Swagger, pero no lo revoca. Las capturas deben mostrar respuestas reales sin exponer tokens completos ni credenciales de infraestructura.

## Verificación de los entregables esperados

| Entregable del enunciado | Ubicación o comprobación |
|---|---|
| Evolución de TP2 con autenticación/autorización. | Mismo proyecto `api-blank`, paquetes de usuario y seguridad, servicios con propiedad. |
| V5 y V6 nuevas; migraciones anteriores sin alterar. | [V5](api-blank/src/main/resources/db/migration/V5__create_usuarios.sql), [V6](api-blank/src/main/resources/db/migration/V6__add_usuario_id_a_listas.sql). La comparación actual de V1–V4 entre `TP2` y `HEAD` no produjo diferencias. El historial visible de V5/V6 muestra su alta en `6b7492f` y ninguna edición posterior versionada. Eso no demuestra por sí solo el checksum actual de una base remota, que no se consultó de nuevo. |
| Unitarias e integraciones en verde. | Nueva ejecución del 3 de noviembre: 58 + 25 Java; otras cinco del Worker. Resumen en [EVIDENCIA-TP3](api-blank/docs/EVIDENCIA-TP3.md). |
| Al menos una corrida Actions exitosa. | [CI de la revisión desplegada](https://github.com/Niquinhoo/Web2Practica/actions/runs/41732115409), commit `994e5b7` de `TP3`; también se conserva la recuperación histórica. |
| URL pública y capturas de health 200, protegido 401/200. | Cloudflare, 3 de noviembre: [54 solicitudes](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/http-publico-completo.json), [health](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/health-swagger-200.png), [401](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/listas-swagger-401.png), [200](api-blank/docs/tp3/evidencias/cloudflare-2026-11-03/listas-swagger-200.png). |
| README con ubicación y justificación de propiedad. | [README de API](api-blank/README.md), sección «Endpoints y propiedad». |

### Ajustes de cierre aplicados el 3 de noviembre

1. Se declaró directamente el núcleo `testcontainers` con scope test, además de los módulos JUnit y PostgreSQL.
2. Se quitaron los filtros de ramas de push y pull_request. La rama de entrega y despliegue es `TP3`. Posteriormente se retiró la referencia remota duplicada `tp3`, comprobando antes que todos sus commits estaban incluidos y que no tenía PR abiertos; su historia sigue incluida en `TP3`.
3. Se cambiaron los patrones de acceso público a `/api/auth/**` y `/api/productos/**`. Una nueva integración comprobó primero el fallo anterior (`404` esperado frente a `401`) y, después del cambio, pasó con toda la suite.
4. Se trasladó `SeguridadIT` al paquete `seguridad` y se expusieron solo las operaciones compartidas del helper de base temporal que necesita desde allí. `clean verify` eliminó clases antiguas para no duplicar casos por el cambio de paquete.
5. Se integraron la explicación, la demostración y la matriz de cumplimiento en este informe; se retiró la guía de defensa anterior y se actualizaron sus referencias.
6. Se conservó Cloudflare como proveedor por instrucción expresa. La gratuidad es la excepción solicitada, sin modificar el enunciado original ni presentar Workers Paid como gratuito.

Nimbus en lugar de JJWT y el filtro Bearer nativo son opciones permitidas por la consigna. Compartir un contenedor temporal por JVM y separar los esquemas por clase conserva la ejecución limpia requerida; el texto no exige crear un contenedor distinto por método.

### Validaciones efectuadas al preparar este informe

Se inspeccionaron POM, configuración, modelo/persistencia de usuarios y listas, cadena de seguridad, autorización en servicios, clases de pruebas, Compose, Dockerfile, entrypoint, workflow y evidencias de publicación. Se compararon V1–V4 contra TP2, se leyó el historial de V5/V6 y se comprobó que el wrapper esté registrado como ejecutable. También se sumaron las ejecuciones de los 12 XML locales guardados y se verificó la presencia del núcleo de Testcontainers en su classpath.

Se ejecutó `clean verify`: 58 unitarias y 25 integraciones, sin fallos, errores u omisiones. Se ejecutó `npm run check` y las cinco pruebas del Worker con Node 24.22.1. La regresión de rutas públicas falló antes del ajuste y pasó en la suite posterior. La revisión `994e5b7` pasó CI antes de desplegarse en Cloudflare. Se verificaron 54 solicitudes públicas, V1–V6 en la base, protección Access y los códigos 200/401/200 en Swagger. Las comprobaciones editoriales incluyen las ocho consignas, las 36 filas, enlaces, bloques de código y ausencia de referencias a la guía eliminada. Las instrucciones genéricas de demostración se distinguen del recorrido nuevo efectivamente registrado.

## Autoevaluación antes de defender

Podés considerar preparado un punto cuando puedas explicarlo sin leer el párrafo y abrir el archivo que lo prueba:

1. ¿Qué agregan Security, JOSE y Testcontainers? ¿Por qué `verify` ejecuta más que `test`?
2. ¿Cuál es el recorrido desde el JSON de registro hasta el hash en PostgreSQL? ¿Quién asigna USER?
3. ¿Qué contiene el JWT, quién valida la firma y dónde queda disponible la identidad?
4. ¿Dónde se comprueba el dueño? ¿Qué diferencia hay entre 401, 403 y 404? ¿Por qué controlo ambos extremos de un movimiento?
5. ¿Qué demuestra cada unitaria y qué demuestra la integración con PostgreSQL que una unitaria no puede demostrar?
6. ¿Qué ejecuta Actions y qué evidencia muestra que detecta una prueba rota?
7. ¿Dónde está alojada la aplicación, dónde está la base y qué evidencia corresponde a cada una?
8. ¿Qué describe Swagger y qué componente protege realmente la operación?

## Límites que conviene poder reconocer

No hay refresh tokens ni revocación inmediata; los permisos del JWT se mantienen hasta vencer. La disponibilidad depende del equipo remoto, su túnel y el proveedor del catálogo. La revisión `994e5b7` está publicada y verificada en Cloudflare; Workers Paid es una elección expresa del usuario. Queda una cuenta USER de captura sin listas por el bloqueo automático de su limpieza, detallado en la evidencia; la sesión y el token temporal se retiraron.
