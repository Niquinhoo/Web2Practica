# TP2 · Persistencia, migraciones y arquitectura hexagonal

Continuación del TP1 en la rama `TP2`: Java 25, Spring Boot 4.1.1, PostgreSQL,
Spring Data JPA y Flyway. El catálogo sigue consultando DummyJSON, sin persistir productos.

## Ejecutar

Desde `api-blank`, con JDK 25 y Docker apuntando a un motor local:

```powershell
docker compose up -d --wait
.\mvnw.cmd --batch-mode verify
.\mvnw.cmd spring-boot:run
```

En Linux/macOS usar `./mvnw`. La API queda en [localhost:8080](http://localhost:8080).
También se puede ejecutar con `java -jar target/api-blank-0.0.1-SNAPSHOT.jar`.
Maven Wrapper descarga las dependencias; no hace falta instalar Maven.

El contenedor publica `127.0.0.1:55432`, evitando los PostgreSQL locales en 5432.
Comprobá el motor con `docker context show`: si apunta a otra máquina, su loopback
no es el de tu PC. Usá un motor local o PostgreSQL local con la misma conexión.

| Variable | Predeterminado | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://127.0.0.1:55432/web2` | Conexión de Spring |
| `DB_USER` | `web2` | Usuario de Spring y Compose |
| `DB_PASSWORD` | `web2` | Contraseña de desarrollo de Spring y Compose |
| `DB_NAME` | `web2` | Base creada por Compose |
| `DB_PORT` | `55432` | Puerto publicado por Compose |

Si cambiás nombre o puerto, ajustá también `DB_URL`. PostgreSQL aplica las variables
de inicialización solo con un volumen vacío. `docker compose stop` conserva los datos;
`docker compose down -v` elimina el volumen, por lo que no debe usarse para conservarlos.

Maven debe informar Java 25 con `.\mvnw.cmd --version`. En este equipo:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.2'
```

Si aparece `Unable to establish loopback connection` con una ruta temporal con
acentos, usar una ruta ASCII para los sockets del JDK:

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:\Users\Public'
```

### PostgreSQL local

Alternativamente, creá una base `web2` y un usuario `web2` con contraseña `web2`,
o configurá las variables para una base propia. El usuario necesita permisos para
crear tablas y esquemas. En este equipo se verificó con PostgreSQL 17 y un clúster
independiente `.local-postgres` (ignorado por Git), en 127.0.0.1:55432.
Para volver a levantar ese clúster o detenerlo, desde esta carpeta:

```powershell
& 'C:\Program Files\PostgreSQL\17\bin\pg_ctl.exe' -D .local-postgres -l target/postgres.log -o '-p 55432 -h 127.0.0.1' -w start
& 'C:\Program Files\PostgreSQL\17\bin\pg_ctl.exe' -D .local-postgres -m fast -w stop
```

Elegí Docker o PostgreSQL local: no pueden ocupar el mismo puerto a la vez.

## Migraciones

Flyway corre automáticamente antes de Hibernate; `ddl-auto=validate` comprueba
el mapeo y `open-in-view=false` cierra la sesión antes de responder por HTTP.
Spring Boot 4 requiere `spring-boot-starter-flyway` (incluye `flyway-core` y su
autoconfiguración), más `flyway-database-postgresql`, según la
[documentación oficial](https://docs.spring.io/spring-boot/how-to/data-initialization.html).

| Migración en `src/main/resources/db/migration` | Efecto |
|---|---|
| `V1__create_favoritos.sql` | ID, producto, nota y fecha |
| `V2__create_listas.sql` | ID y nombre de listas |
| `V3__add_lista_id_a_favoritos.sql` | Relación nullable, FK e índice |
| `V4__lista_id_obligatorio.sql` | Crea/localiza “Sin clasificar”, asigna filas antiguas y aplica NOT NULL |

La convención es `V{versión}__{descripción}.sql`, con doble guion bajo.
Una migración aplicada no se edita: Flyway guarda su checksum y rechaza cambios.
V4 conserva el historial y adapta los datos existentes antes de agregar NOT NULL.

```powershell
docker compose exec postgres psql -U web2 -d web2 -c "SELECT version, description, success FROM public.flyway_schema_history ORDER BY installed_rank;"
```

En PostgreSQL local ejecutar la misma consulta desde psql/pgAdmin.
La prueba de migraciones aplica V1, inserta un favorito antiguo, aplica V2/V3,
inserta otro ya clasificado y verifica que V4 conserve ambos correctamente.

## Endpoints

[Swagger UI](http://localhost:8080/swagger-ui.html) ·
[OpenAPI JSON](http://localhost:8080/v3/api-docs) ·
[Colección HTTP](docs/tp2.http) · [Evidencia TP2](docs/EVIDENCIA-TP2.md)

| Método | Ruta | Éxito |
|---|---|---|
| GET | `/health` | 200 |
| GET | `/api/productos` y `/api/productos/{id}` | 200 |
| POST | `/api/favoritos` | 201 + Location |
| GET | `/api/favoritos` y `/api/favoritos/{id}` | 200 |
| PUT | `/api/favoritos/{id}` | 200 |
| DELETE | `/api/favoritos/{id}` | 204 |
| POST | `/api/listas` | 201 + Location |
| GET | `/api/listas` y `/api/listas/{id}` | 200 |
| GET | `/api/listas/{id}/favoritos` | 200 |
| DELETE | `/api/listas/{id}` | 204 |
| POST | `/api/listas/{origenId}/mover-favoritos` | 204 |

Lista: `{"nombre":"Regalos"}`; nombre obligatorio, no blanco, hasta 255 caracteres.
Favorito (POST/PUT): `{"productoId":1,"nota":"Para regalar","listaId":2}`.
Producto y lista deben ser positivos; la lista debe existir. Nota opcional, hasta 500
caracteres. PUT reemplaza producto, nota y lista, conservando ID y fecha UTC.
La respuesta agrega `id` y `fechaAgregado`. La fecha se normaliza a microsegundos,
precisión de PostgreSQL. Listados ordenados por ID, sin paginación. Se permiten
favoritos repetidos y no se consulta DummyJSON al guardarlos.

Movimiento: `{"destinoId":3}`. Mueve todos los favoritos y elimina el origen, incluso
si estaba vacío. Conserva ID, nota y fecha de los favoritos y los favoritos previos
del destino. Origen igual a destino devuelve 400 sin modificar nada.

Formato de error: `{"status":400,"mensaje":"...","campos":{}}`.
400 para validaciones/JSON/IDs inválidos; 404 para recursos inexistentes; 409 para
conflictos de integridad (incluye borrar una lista con favoritos); 502 para errores
del catálogo. La FK protege también ante concurrencia: borrar una lista entre su
validación y la escritura provoca 409, sin referencias huérfanas.

## Puertos y adaptadores: comparación con TP1

```text
producto: Controller → Service → ProductoClient → DummyJsonProductoClient → DummyJSON
favorito: Controller → Service → FavoritoRepository → FavoritoRepositoryAdapter → JPA → PostgreSQL
lista:    Controller → Service → ListaRepository → ListaRepositoryAdapter → JPA → PostgreSQL
```

`FavoritoRepository` es el puerto: expresa operaciones sin tipos JPA. Se eliminó
`FavoritoRepositoryMemoria` y se agregaron `FavoritoEntity`, `FavoritoJpaRepository`
y `FavoritoRepositoryAdapter`. Spring Data implementa su interfaz; el adapter traduce
entidades a records. Las entidades JPA son clases mutables con constructor vacío.
La relación es `@ManyToOne` unidireccional; sin `@OneToMany` ni cascada de borrado.

Cambiar solamente el almacenamiento no exige tocar Service, Controller ni DTOs,
porque dependen del puerto. En la entrega final sí cambian `Favorito`, Request,
Response y el puerto para incorporar `listaId`, y `FavoritoService` para validarlo.
Los métodos y rutas de `FavoritoController` quedan iguales; solo cambia su documentación.
`ApiExceptionHandler` suma 400 para origen=destino y 409 de integridad; `ApiConfig`
actualiza Swagger. Todo `producto/`, `RestClientConfig`, `HealthController`, `ApiError`
y las excepciones de TP1 quedan exactamente iguales.

## Transacción y ACID

`ListaService.moverFavoritos` valida ambas listas y, dentro de `@Transactional`,
reasigna favoritos con una sola sentencia y borra el origen. Los adapters participan
de la misma transacción. Si falla el DELETE, PostgreSQL revierte también el UPDATE.
Sin la transacción, el UPDATE podría confirmarse y luego fallar el DELETE: quedarían
favoritos movidos y el origen aún presente. La atomicidad evita ese resultado parcial;
FK y NOT NULL mantienen la consistencia. La FK por sí sola impide referencias huérfanas,
pero no garantiza la atomicidad de ambas escrituras. El aislamiento es el predeterminado
de PostgreSQL (READ COMMITTED); la durabilidad conserva lo confirmado al reiniciar.

## Pruebas

Con PostgreSQL disponible: `.\mvnw.cmd --batch-mode verify`.
Los tests de Spring usan exclusivamente `tp2_test`, sin borrar tablas de `public`.
La prueba de migraciones crea y elimina un esquema aleatorio. El usuario de pruebas
necesita permisos para crear esquemas y funciones. No ejecutar suites simultáneas
sobre el mismo `tp2_test`. No se usa H2 ni se consulta Internet durante las pruebas.

Se conservan las pruebas de TP1 y se reemplazan las específicas del Map por pruebas
contra PostgreSQL: CRUD, validaciones, listas, conflicto 409, movimiento y migraciones.
La prueba de rollback instala temporalmente un trigger que hace fallar el DELETE
después del UPDATE real, y comprueba que el favorito siga en origen.
GitHub Actions levanta PostgreSQL y ejecuta `verify` en pushes a `main`, `TP1`, `TP2`
y PRs a `main`. Los documentos de TP1 quedan como registro histórico; para la API
actual usar `tp2.http`, que incluye el campo obligatorio `listaId`.
