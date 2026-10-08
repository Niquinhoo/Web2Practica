# TP2 · Persistencia, migraciones y arquitectura hexagonal

Evolución de una aplicación con PostgreSQL, JPA/Hibernate, Flyway, transacciones y puertos y adaptadores.

📽️ [TP2: persistencia y arquitectura hexagonal](https://unvime-web-2.matiendres.workers.dev/practicos/presentaciones/tp2-persistencia-presentacion/)

## Objetivos


- Levantar una base de datos PostgreSQL y conectarla a una aplicación Spring Boot con Spring Data JPA.

- Reemplazar el repository en memoria de Favoritos por un adapter respaldado por JPA/Hibernate, sin modificar el dominio, el Service ni el Controller.

- Versionar la evolución del esquema de base de datos con Flyway.

- Modelar una relación entre entidades y exponerla en la API.

- Delimitar una operación transaccional explícita y justificarla en términos de atomicidad.

- Reconocer la diferencia entre un puerto (contrato) y un adapter (implementación concreta de infraestructura).

## El dominio del práctico


El catálogo de productos (`/api/productos`) **no cambia**: sigue siendo un espejo de solo lectura de una API externa, sin persistencia propia. Este práctico persiste únicamente lo que ya es propio de tu aplicación:

1. **Favoritos** — deja de vivir en un `Map` en memoria y pasa a guardarse en PostgreSQL.

2. **Listas** — una entidad nueva, propia de la aplicación, para organizar favoritos (por ejemplo “Regalos”, “Para comprar en oferta”). Cada favorito pertenece a una lista.

La relación entre ambas es la excusa para practicar modelado relacional, migraciones y una operación que debe ejecutarse de forma atómica.

## Forma de trabajo


Seguí trabajando sobre el mismo repositorio Maven del TP1. Sumá al `README` cómo levantar PostgreSQL y cómo se aplican las migraciones.

## Consignas


### 1. PostgreSQL y dependencias


1. Levantá una instancia de PostgreSQL. La forma recomendada es un [`docker-compose.yml`](https://docs.docker.com/compose/) en la raíz del proyecto con la [imagen oficial `postgres`](https://hub.docker.com/_/postgres), definiendo base, usuario y contraseña por variables de entorno. Si no podés usar Docker, una [instalación local de PostgreSQL](https://www.postgresql.org/download/) con los mismos datos de conexión también sirve.

2. Agregá al `pom.xml`: `spring-boot-starter-data-jpa`, el driver `org.postgresql:postgresql` (scope `runtime`), y [Flyway](https://flywaydb.org/documentation) (`spring-boot-starter-flyway` + `flyway-database-postgresql`).

3. Configurá la conexión en `application.properties` (url, usuario, contraseña).

4. Configurá `spring.jpa.hibernate.ddl-auto=validate` y `spring.jpa.open-in-view=false`. El esquema lo va a manejar Flyway, no Hibernate — si dejás `update` o `create`, tenés dos herramientas compitiendo por el mismo esquema.

5. Verificá que la aplicación levanta y se conecta sin errores.

### 2. Migraciones iniciales con Flyway


1. Creá `src/main/resources/db/migration/V1__create_favoritos.sql` con la tabla `favoritos` (`id`, `producto_id`, `nota`, `fecha_alta`).

2. Reiniciá la aplicación y confirmá que Flyway aplicó la migración (revisá el log y la tabla `flyway_schema_history` que Flyway crea sola).

3. Investigá la [convención de nombres](https://documentation.red-gate.com/fd/versioned-migrations-273973333.html) `V{versión}__{descripción}.sql` y por qué Flyway rechaza una migración ya aplicada si la modificás después.

### 3. Favoritos sobre JPA: el adapter


1. Creá `FavoritoEntity` (`@Entity`, tabla `favoritos`) con los mismos campos que la tabla. Una entidad JPA no puede ser un `record`: necesita constructor vacío y estado mutable para que Hibernate gestione el ciclo de vida del objeto. El `record Favorito` del dominio (TP1) **no se toca**.

2. Creá `FavoritoJpaRepository extends JpaRepository<FavoritoEntity, Long>` — la interfaz que Spring Data implementa sola.

3. Creá `FavoritoRepositoryAdapter implements FavoritoRepository` (el puerto que ya definiste en el TP1). Puertas adentro, convierte `FavoritoEntity` ↔ `Favorito` y delega en `FavoritoJpaRepository`.

4. Eliminá `InMemoryFavoritoRepository` — dos beans implementando `FavoritoRepository` no son ambiguos por accidente, Spring no sabría cuál inyectar.

5. Probá el CRUD completo de favoritos contra PostgreSQL real (Swagger o tu cliente HTTP de siempre) y confirmá que los datos sobreviven a un reinicio de la aplicación.

### 4. Puertos y adaptadores: qué cambió y qué no


1. Compará `FavoritoService`, `FavoritoController` y los DTOs de favoritos antes y después de este práctico.

2. Documentá en el `README` qué clases tuviste que tocar para migrar de memoria a JPA y cuáles quedaron exactamente iguales. Explicá por qué eso fue posible: `FavoritoRepository` es un puerto (un contrato), y tanto la versión en memoria del TP1 como `FavoritoRepositoryAdapter` son adapters intercambiables detrás de esa misma interfaz.

### 5. Relación: listas de favoritos


1. Definí el dominio `Lista` (`id`, `nombre`) y su puerto `ListaRepository`, con la misma forma que `FavoritoRepository`.

2. Implementá `ListaEntity`, `ListaJpaRepository` y `ListaRepositoryAdapter`, repitiendo la receta del punto 3.

3. Sumá a `FavoritoEntity` una relación `@ManyToOne` hacia `ListaEntity` (columna `lista_id`). Para el lado inverso (los favoritos de una lista), evitá un `@OneToMany` bidireccional — trae complicaciones de carga perezosa que no hacen falta acá — y resolvelo con una consulta derivada, por ejemplo `List<FavoritoEntity> findByListaId(Long listaId)`.

4. Sumá `listaId` al dominio `Favorito`, a `FavoritoRequest` (validado con `@NotNull`) y a `FavoritoResponse`. Igual que `productoId`, se referencia por id — el dominio no navega objetos completos.

5. Migración `V2__create_listas.sql` (tabla `listas`) y `V3__add_lista_id_a_favoritos.sql` (columna `lista_id` en `favoritos`, referenciando `listas(id)`).

6. Exponé los endpoints de la tabla de abajo y sumá al manejador de errores del TP1 un caso nuevo: intentar borrar una lista que todavía tiene favoritos debe responder `409 Conflict`, no `500`.

| Operación  | Método  | Código de éxito  |
| --- | --- | --- |
| Crear lista  | `POST /api/listas`  | `201 Created`  |
| Listar listas  | `GET /api/listas`  | `200 OK`  |
| Obtener una lista  | `GET /api/listas/{id}`  | `200 OK`  |
| Favoritos de una lista  | `GET /api/listas/{id}/favoritos`  | `200 OK`  |
| Eliminar una lista vacía  | `DELETE /api/listas/{id}`  | `204 No Content`  |
### 6. Evolución del esquema


Las filas de `favoritos` creadas antes del punto 5 no tienen `lista_id`. Resolvelo con una migración nueva, nunca editando `V1`, `V2` o `V3` ya aplicadas:

1. `V4__lista_id_obligatorio.sql` debe, en este orden: crear una lista por defecto (por ejemplo “Sin clasificar”) si no existe, actualizar los favoritos con `lista_id` nulo para que apunten a ella, y recién ahí declarar la columna `NOT NULL`.

2. Corré la aplicación contra una base con datos previos del punto 3 y confirmá que la migración no rompe filas existentes.

3. Documentá en el `README`, en una o dos líneas, por qué esto se resuelve con una migración nueva y no modificando una anterior.

### 7. Transacciones


1. Implementá `POST /api/listas/{origenId}/mover-favoritos`, que recibe la lista destino y: valida que ambas listas existan (`404` si no), reasigna todos los favoritos de la lista origen a la destino, y elimina la lista origen.

2. Marcá el método del Service que implementa esta operación con `@Transactional`. Si una de las escrituras falla a mitad de camino, ninguna debe quedar aplicada.

3. Documentá en el `README`, con tus palabras y relacionándolo con ACID visto en teoría, qué podría quedar inconsistente en la base si sacaras el `@Transactional` y una de las escrituras fallara después de otra ya confirmada.

### 8. Documentación


1. Actualizá Swagger para que muestre también `listas`, con al menos una descripción por endpoint (`@Operation`).

2. Actualizá el `README` con las instrucciones para levantar PostgreSQL (Docker o instalación local) y confirmar que las migraciones corrieron.

## Evidencia esperada


- Repositorio con el proyecto del TP1 evolucionado, funcionando contra PostgreSQL real.

- Carpeta `db/migration` con al menos cuatro migraciones versionadas, sin ediciones posteriores a una ya aplicada.

- `README` actualizado: cómo levantar la base, qué cambió respecto del TP1 en la capa de persistencia, y las dos justificaciones escritas pedidas (evolución del esquema y transacciones).

- Swagger UI navegable, mostrando `productos`, `favoritos` y `listas`.

- Al menos un caso de éxito y un caso de error probado por recurso nuevo, incluyendo la operación de mover favoritos entre listas (capturas o colección de Postman/Insomnia/`.http`).

[ Página anterior
Repaso · Parcial 1  ](https://unvime-web-2.matiendres.workers.dev/practicos/repaso-parcial-1/)[ Siguiente página
TP3 · Seguridad y CI  ](https://unvime-web-2.matiendres.workers.dev/practicos/tp3/)
