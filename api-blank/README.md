# TP2 · Persistencia, migraciones y arquitectura hexagonal

Evolución del TP1 en la rama `TP2`. API REST con Java 25, Spring Boot 4.1.1,
Spring Data JPA/Hibernate, PostgreSQL 17, Flyway y springdoc 3.1.0.
El catálogo de productos sigue consultando DummyJSON; solamente listas y favoritos se persisten.

[Consigna y material del TP2](docs/tp2/README.md) ·
[Colección HTTP del TP2](docs/tp2.http) · [Evidencia de ejecución](docs/EVIDENCIA-TP2.md).

## Arranque con Docker

Desde `api-blank/`, con Java 25 y Docker Compose:

```powershell
docker compose up -d --wait
.\mvnw.cmd --batch-mode verify
.\mvnw.cmd spring-boot:run
```

En Linux/macOS, reemplazar `.\mvnw.cmd` por `./mvnw`.
La primera ejecución descarga dependencias Maven. La base es `webii_tp2`,
con usuario y contraseña locales `webii_tp2`, puerto 5432.
El contenedor usa un volumen persistente: `docker compose stop` no borra los datos.

API: [http://localhost:8080](http://localhost:8080).
[Swagger UI](http://localhost:8080/swagger-ui.html) permite probar todos los endpoints.
[OpenAPI JSON](http://localhost:8080/v3/api-docs) y [YAML](http://localhost:8080/v3/api-docs.yaml).

Para personalizar Compose, copiar `.env.example` a `.env` y editar sus valores.
**Spring no lee automáticamente el archivo .env**: exportar los mismos valores al proceso Java.
Si cambia la base o el puerto, también configurar `DATABASE_URL`:

```powershell
$env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/webii_tp2'
$env:POSTGRES_USER = 'webii_tp2'
$env:POSTGRES_PASSWORD = 'webii_tp2'
.\mvnw.cmd spring-boot:run
```

## Arranque local sin Docker

También sirve una instalación de PostgreSQL: crear un usuario con permiso de crear esquemas,
crear `webii_tp2` con ese propietario y configurar las variables anteriores.
El usuario debe poder crear tablas para que Flyway gestione el esquema.

En este equipo quedaron preparados binarios de PostgreSQL 17.11 en
`C:\Users\Public\codex-tp2-runtime\pgsql` y datos en
`C:\Users\Public\codex-tp2-runtime\data`, escuchando en `127.0.0.1:55432`.
El script reutiliza esa instancia, establece las variables y ejecuta Maven:

```powershell
.\scripts\run-local.ps1 -Verify
.\scripts\run-local.ps1
```

Para usar otros binarios o un directorio de datos propio:

```powershell
.\scripts\run-local.ps1 -PostgresDirectory 'C:\postgresql\pgsql' -DataDirectory 'C:\postgresql\datos' -Port 55432
```

El script inicializa un cluster si no existe, crea `webii_tp2` si falta y deja PostgreSQL
encendido al detener la aplicación. Acepta `-JavaDirectory` para seleccionar otro JDK 25.
Para detener la instancia preparada en este equipo:

```powershell
& 'C:\Users\Public\codex-tp2-runtime\pgsql\bin\pg_ctl.exe' -D 'C:\Users\Public\codex-tp2-runtime\data' -w stop
```

Maven debe informar Java 25 (`.\mvnw.cmd --version`). En este equipo:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.4'
```

Si Java muestra `Unable to establish loopback connection` y la carpeta temporal contiene
acentos, usar una carpeta ASCII; el script local ya realiza este ajuste:

```powershell
$tp2Temp = Join-Path $env:PUBLIC 'codex-tp2-temp'
New-Item -ItemType Directory -Force -Path $tp2Temp | Out-Null
$env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Djdk.net.unixdomain.tmpdir=$tp2Temp".Trim()
```

Para ejecutar el paquete generado por `verify`:

```powershell
java -jar target/api-blank-0.0.1-SNAPSHOT.jar
```

En Windows, detener la aplicación si ejecuta ese JAR antes de volver a empaquetarlo;
el proceso Java mantiene abierto el archivo y Maven no puede reemplazarlo.

## Endpoints

| Método | Ruta | Éxito | Comportamiento |
|---|---|---|---|
| GET | `/health` | 200 | Estado del servicio |
| GET | `/api/productos` | 200 | Catálogo externo completo |
| GET | `/api/productos/{id}` | 200 | Producto externo |
| POST | `/api/favoritos` | 201 | Crear; devuelve Location |
| GET | `/api/favoritos` | 200 | Listar por ID ascendente |
| GET | `/api/favoritos/{id}` | 200 | Obtener favorito |
| PUT | `/api/favoritos/{id}` | 200 | Reemplazar producto, nota y lista |
| DELETE | `/api/favoritos/{id}` | 204 | Eliminar favorito |
| POST | `/api/listas` | 201 | Crear; devuelve Location |
| GET | `/api/listas` | 200 | Listar por ID ascendente |
| GET | `/api/listas/{id}` | 200 | Obtener lista |
| GET | `/api/listas/{id}/favoritos` | 200 | Favoritos por ID; array vacío si no tiene |
| DELETE | `/api/listas/{id}` | 204 | Eliminar lista vacía; 409 si tiene favoritos |
| POST | `/api/listas/{origenId}/mover-favoritos` | 204 | Mover todos al destino y eliminar origen |

Crear una lista:

```json
{"nombre":"Regalos"}
```

El nombre es obligatorio, admite hasta 100 caracteres y se guarda sin espacios en los extremos.
Se permiten nombres repetidos. El nombre “Sin clasificar” identifica la lista usada por V4,
sin convertirla en una lista protegida: también se puede eliminar si está vacía.

POST y PUT de favoritos requieren el ID de una lista existente:

```json
{"productoId":1,"nota":"Comprar para regalar","listaId":1}
```

`productoId` y `listaId` son obligatorios y positivos. La nota es opcional, admite null
o cadena vacía y como máximo 500 caracteres. El producto se referencia por ID sin consultarlo
al proveedor; se permiten varios favoritos sobre el mismo producto.

```json
{
  "id":1,
  "productoId":1,
  "nota":"Comprar para regalar",
  "fechaAgregado":"2026-10-06T23:00:00.123456Z",
  "listaId":1
}
```

El servidor genera el ID y la fecha UTC. PUT conserva ambos; omitir la nota la deja en null.
La columna SQL se llama `fecha_alta`; el nombre público `fechaAgregado` se conserva del TP1.
Se normaliza a microsegundos, la precisión de PostgreSQL, para que POST y GET coincidan.

Mover favoritos recibe:

```json
{"destinoId":2}
```

Ambas listas deben existir (404 si alguna falta); usar la misma lista devuelve 400.
El destino conserva sus favoritos y recibe todos los del origen, con sus IDs, notas y fechas.
También se puede mover una lista vacía: se elimina el origen. El éxito devuelve 204 sin cuerpo.

## Errores

Todas las respuestas de error usan el formato del TP1:

```json
{"status":400,"mensaje":"Hay datos inválidos","campos":{"listaId":"es obligatorio"}}
```

| Código | Situación |
|---|---|
| 400 | Validación, JSON inválido, ID de tipo incorrecto o mover a la misma lista |
| 404 | Producto, favorito o lista inexistente |
| 405 | Método HTTP no permitido |
| 415 | Content-Type no soportado |
| 409 | Lista con favoritos o conflicto de integridad referencial |
| 500 | Error inesperado sin detalles internos |
| 502 | Falla, timeout o contenido inválido de DummyJSON |

`campos` es un objeto vacío cuando el error no corresponde a un campo.
La FK impide borrar una lista referenciada incluso si otra solicitud crea un favorito
entre la comprobación y el borrado; el handler traduce el conflicto a 409.

## Flyway y evolución del esquema

Spring ejecuta automáticamente los archivos de `src/main/resources/db/migration` antes de JPA:

| Versión | Archivo | Cambio |
|---|---|---|
| V1 | `V1__create_favoritos.sql` | Favoritos con ID, producto, nota y fecha |
| V2 | `V2__create_listas.sql` | Listas con ID y nombre |
| V3 | `V3__add_lista_id_a_favoritos.sql` | Relación nullable, FK sin borrado en cascada e índice |
| V4 | `V4__lista_id_obligatorio.sql` | Crear lista por defecto si falta, completar filas anteriores y exigir NOT NULL |

En el primer arranque el log informa `Successfully applied 4 migrations`.
En los siguientes valida los checksums e informa `No migration necessary`.
Hibernate solamente comprueba el esquema (`ddl-auto=validate`).
`open-in-view=false` obliga a convertir las entidades a dominio y DTO dentro de la transacción.

Confirmar el historial con Docker:

```powershell
docker compose exec postgres psql -U webii_tp2 -d webii_tp2 -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

La convención es `V{versión}__{descripción}.sql`, con dos guiones bajos.
Flyway guarda el checksum de cada migración aplicada; editarla produce un error de validación
porque el historial deja de coincidir. La evolución se agrega en una migración nueva.

V4 completa primero los datos anteriores y solamente después impone NOT NULL.
Modificar V1, V2 o V3 no actualizaría correctamente las bases donde esas versiones ya se
ejecutaron y rompería sus checksums.

## Puertos y adapters: comparación con TP1

```text
producto/    Controller → Service → ProductoClient → DummyJsonProductoClient → DummyJSON
favorito/    Controller → Service → FavoritoRepository → FavoritoRepositoryAdapter → JPA → PostgreSQL
lista/       Controller → Service → ListaRepository → ListaRepositoryAdapter → JPA → PostgreSQL
error/       ApiError, excepciones y ApiExceptionHandler
config/      OpenAPI, Clock UTC y RestClient
```

`FavoritoRepository` es el puerto: el contrato que usa el servicio.
`FavoritoRepositoryMemoria` implementaba ese contrato en el TP1; ahora
`FavoritoRepositoryAdapter` lo implementa delegando en `FavoritoJpaRepository`.
Los records de dominio siguen separados de entidades JPA mutables; JPA queda en
los paquetes `persistencia/`. El service y el controller no importan entidades ni repositorios JPA.
La relación es `@ManyToOne(fetch=LAZY)`, sin colección bidireccional:
los favoritos de una lista se consultan con `findByLista_IdOrderByIdAsc`.

Para sustituir memoria por JPA bastaba cambiar el adapter y la configuración.
**La incorporación posterior de listas sí cambia el contrato funcional**, por eso esta entrega
modifica las siguientes clases del TP1:

| Clase/capa | Cambio final |
|---|---|
| `FavoritoRepositoryMemoria` | Eliminado; reemplazado por adapter, entidad y repositorio JPA |
| `Favorito` | Agrega `listaId`; mantiene record, fecha y campos previos |
| `FavoritoRepository` | Agrega listaId en escrituras y operaciones de consulta/movimiento por lista |
| `FavoritoRequest`, `FavoritoResponse` | Incorporan listaId y su validación/documentación |
| `FavoritoService` | Valida lista existente, pasa listaId y delimita transacciones |
| `FavoritoController` | Conserva rutas y lógica HTTP; actualiza descripciones Swagger |
| `ApiExceptionHandler` | Agrega 409 y el 400 de mover a la misma lista |
| `ApiConfig` | Título TP2 y ejemplos de 409 |
| Maven y properties | Dependencias y configuración de PostgreSQL/Flyway |

Quedaron exactamente iguales `ProductoClient`, `DummyJsonProductoClient`,
`ProductoController`, `ProductoService`, sus DTOs/records, `RestClientConfig`,
`HealthController`, `ApiError`, las excepciones previas y `ApiBlankApplication`.
Productos mantiene su comportamiento y sus pruebas del TP1.

## Transacciones y ACID

`ListaService.moverFavoritos` usa `@Transactional`: valida las listas,
ejecuta el UPDATE que reasigna favoritos y después borra el origen.
Los adapters participan en esa misma transacción. Si una escritura o el commit falla,
Spring revierte toda la operación.

Sin esa transacción, el UPDATE podría confirmarse y luego fallar el DELETE:
la solicitud respondería con un error, pero los favoritos ya estarían en el destino
y el origen seguiría existiendo. La atomicidad (A de ACID) exige que ambos cambios
se confirmen juntos o ninguno. Las FK y NOT NULL contribuyen a la consistencia;
PostgreSQL aporta aislamiento y durabilidad. La prueba de rollback agrega temporalmente
una FK que impide el DELETE y confirma que los favoritos siguen en el origen.

## Pruebas y evidencia

```powershell
# Pruebas unitarias, sin base ni Internet:
.\mvnw.cmd --batch-mode test

# Unitarias + integración, migraciones y empaquetado; requiere PostgreSQL:
.\mvnw.cmd --batch-mode verify

# En este equipo, configura el PostgreSQL local antes de verify:
.\scripts\run-local.ps1 -Verify
```

Las pruebas de integración `*IT` las ejecuta Maven Failsafe en `verify`.
Usan PostgreSQL real y crean esquemas aleatorios `tp2_test_*`, que eliminan al finalizar.
No borran ni modifican las tablas del esquema de la aplicación.
Para otra conexión de pruebas, configurar `TEST_DATABASE_URL`, `TEST_DATABASE_USER`
y `TEST_DATABASE_PASSWORD` (por defecto localhost:5432 y credenciales webii_tp2).

Cobertura: CRUD de favoritos y listas, validación y referencias inexistentes,
borrado con conflicto, movimiento incluyendo lista vacía, rollback por una falla SQL real,
migración desde V1 con filas previas, conservación de favoritos clasificados y reinicio de Flyway,
productos con cliente simulado, Swagger/OpenAPI y cliente HTTP con servidor local.
Las pruebas automatizadas no consultan Internet.

GitHub Actions incluye PostgreSQL 17 y ejecuta `verify` en pushes a main, TP1, TP2 y tp2,
y en PRs a main. Mockito conserva `mock-maker-subclass`, sin agente adjuntado a Java 25.

[docs/tp2.http](docs/tp2.http) contiene requests ordenados de éxito/error para la entrega.
[docs/EVIDENCIA-TP2.md](docs/EVIDENCIA-TP2.md) registra la ejecución real y la comprobación
de persistencia después de reiniciar la aplicación.

El material y [evidencia del TP1](docs/EVIDENCIA-TP1.md) se conservan como antecedente.
Su colección `tp1.http` representa el contrato anterior: para esta versión usar `tp2.http`.

## Configuración del catálogo

`DUMMYJSON_BASE_URL` permite reemplazar https://dummyjson.com.
Los timeouts predeterminados son 3s de conexión y 5s de lectura.
Los endpoints de productos necesitan conexión al proveedor; listas y favoritos funcionan
independientemente de ese servicio.

## Referencias

- [Material local del TP2](docs/tp2/consigna.md)
- [PostgreSQL para Windows y binarios](https://www.postgresql.org/download/windows/)
- [Flyway: migraciones versionadas](https://documentation.red-gate.com/fd/versioned-migrations-273973333.html)
- [DummyJSON Products](https://dummyjson.com/docs/products)
