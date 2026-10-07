# TP2 · Persistencia, migraciones y arquitectura hexagonal

Punto de partida para el práctico. Sale directo de un TP1 ya resuelto
(`productos` + `favoritos`, ambos completos) — **la infraestructura de
PostgreSQL/JPA/Flyway ya está armada, pero la lógica de persistencia todavía
no se tocó**: `favoritos` sigue exactamente igual que en el TP1, guardado en
memoria.

> ⚠️ **Favoritos sigue en memoria.** `InMemoryFavoritoRepository` no se tocó.
> Reemplazarlo por un adapter JPA — sin modificar `FavoritoService` ni
> `FavoritoController` — es el corazón de este práctico.

La consigna completa (con el porqué de cada paso) y la presentación
**"TP2: persistencia y arquitectura hexagonal"** están publicadas en el sitio
de la materia, sección *Trabajos prácticos → TP2*.

## Cómo levantar el proyecto

Requiere Java 25. Usar siempre el wrapper, nunca un `mvn` instalado aparte.

### 1. Base de datos

**Opción A — Docker (recomendada):**

```
docker compose up -d
```

Levanta PostgreSQL con la base `webii_tp2` y el usuario `webii_tp2` (ver
`docker-compose.yml`), en el puerto `5432`.

**Opción B — PostgreSQL local:** si no podés usar Docker, instalá
PostgreSQL localmente y creá la base y el rol a mano (por ejemplo desde
pgAdmin, Query Tool sobre la base `postgres`):

```sql
CREATE ROLE webii_tp2 WITH LOGIN PASSWORD 'webii_tp2' SUPERUSER;
CREATE DATABASE webii_tp2 OWNER webii_tp2;
```

Los datos de conexión están en `application.properties`
(`spring.datasource.*`) — si usás otro usuario/base, ajustalos ahí.

### 2. Levantar la app

```
# Windows
.\mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw spring-boot:run
```

Con la base arriba, la app debería levantar sin errores — pero **todavía no
hay ninguna migración de Flyway**, así que no se crea ninguna tabla propia
todavía (eso es la consigna 2). Cuando el log muestre `Started
DemoApplication`, la app queda escuchando en `http://localhost:8080`.

Para compilar y correr los tests: `./mvnw test` (o `.\mvnw.cmd test`).

## Endpoints disponibles hoy

| Método | Path | Qué hace |
|---|---|---|
| GET | `/health` | Chequeo de salud básico |
| GET | `/ping` | Devuelve `pong`, sin JSON |
| GET | `/api/productos?limit=&skip=` | Catálogo, de solo lectura (consume DummyJSON) |
| GET | `/api/productos/{id}` | Un producto puntual. 404 si no existe |
| GET / POST | `/api/favoritos` | Listar / crear favoritos — **en memoria, TP1** |
| GET / PUT / DELETE | `/api/favoritos/{id}` | Obtener, actualizar o eliminar un favorito — **en memoria, TP1** |

Documentación interactiva (Swagger UI):
**http://localhost:8080/swagger-ui/index.html**
(el JSON crudo de OpenAPI está en `/v3/api-docs`).

## Estructura del proyecto

```
com.example.demo
├── controller/            → @RestController (HTTP in/out, nada de lógica)
│   ├── ProductoController
│   ├── FavoritoController
│   └── ListaController            ⬜ para armar en clase
├── service/               → interfaz + implementación, lógica de negocio
│   ├── ProductoService / ProductoServiceImpl
│   ├── FavoritoService / FavoritoServiceImpl
│   └── ListaService / ListaServiceImpl    ⬜ para armar en clase
├── repository/            → puertos + adapters
│   ├── FavoritoRepository          (puerto — no debería cambiar de forma)
│   ├── InMemoryFavoritoRepository  → se REEMPLAZA por un adapter JPA ⬜
│   ├── ListaRepository             ⬜ para armar en clase (puerto)
│   ├── FavoritoJpaRepository / FavoritoRepositoryAdapter  ⬜ para armar en clase
│   └── ListaJpaRepository / ListaRepositoryAdapter        ⬜ para armar en clase
├── entity/                ⬜ no existe todavía — @Entity de JPA (FavoritoEntity, ListaEntity)
├── domain/                → entidades de dominio (records, inmutables)
│   ├── Favorito                   (le va a faltar sumar listaId)
│   └── Lista                      ⬜ para armar en clase
├── dto/
│   ├── producto/          → ProductoDTO, ProductoPageResponse
│   ├── favorito/          → FavoritoRequest, FavoritoResponse (les va a faltar listaId)
│   └── lista/              ⬜ para armar en clase
├── client/dummyjson/      → todo lo que sabe hablar con la API externa
├── exception/             → manejo uniforme de errores
│   ├── RecursoNoEncontradoException  (404, ya existe)
│   ├── ServicioExternoException      (5xx, ya existe)
│   ├── ListaNoVaciaException         ⬜ para armar en clase (409)
│   └── GlobalExceptionHandler        (@RestControllerAdvice, ya existe)
└── config/
    ├── RestClientConfig
    └── OpenApiConfig
```

Las líneas marcadas ⬜ todavía no existen en el repo.

## Migraciones (Flyway)

`src/main/resources/db/migration/` **todavía no existe** — la primera
consigna del práctico es crearla, con `V1__create_favoritos.sql`. A partir de
ahí, cada cambio de esquema es una migración nueva (`V2`, `V3`...), nunca
editando una ya aplicada.

## Qué queda por hacer

1. **Migraciones iniciales** — `V1__create_favoritos.sql` (tabla `favoritos`).
2. **Favoritos sobre JPA** — `FavoritoEntity`, `FavoritoJpaRepository`,
   `FavoritoRepositoryAdapter` (implementa el puerto `FavoritoRepository` que
   ya existe). Eliminar `InMemoryFavoritoRepository`. Ni `FavoritoService` ni
   `FavoritoController` deberían cambiar.
3. **Documentar** qué cambió y qué no al migrar de memoria a JPA.
4. **Listas** — dominio `Lista`, puerto `ListaRepository`, `ListaEntity`,
   adapter, service, controller (`/api/listas`, con el endpoint de favoritos
   de una lista). Relación `@ManyToOne` en `FavoritoEntity` hacia
   `ListaEntity`, sin `@OneToMany` bidireccional — resolver el lado inverso
   con una consulta derivada. Migraciones `V2` (listas) y `V3` (`lista_id` en
   favoritos, nullable).
5. **Evolución del esquema** — `V4`: backfill de una lista por defecto para
   los favoritos existentes, y recién ahí `lista_id NOT NULL`.
6. **Transacción** — `POST /api/listas/{origenId}/mover-favoritos`, con
   `@Transactional` en el Service.
7. **Manejo de errores** — borrar una lista con favoritos debe responder
   `409 Conflict`, no `500`.
8. **Documentación** — Swagger con los tres grupos de endpoints, y el
   `README` actualizado con las dos justificaciones que pide la consigna.

## Dependencias

Ya agregadas al `pom.xml`, listas para usar:

- `spring-boot-starter-webmvc`, `spring-boot-starter-validation`,
  `springdoc-openapi-starter-webmvc-ui` — del TP1.
- `spring-boot-starter-data-jpa` — Spring Data JPA + Hibernate.
- `postgresql` — driver JDBC (scope `runtime`).
- `spring-boot-starter-flyway` + `flyway-database-postgresql` — migraciones.
  **Ojo:** en Spring Boot 4.x, `flyway-core` solo **no alcanza** — la
  autoconfiguración de Flyway se movió a este starter separado.
