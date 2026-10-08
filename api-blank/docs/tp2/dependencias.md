# Dependencias y configuración de TP2

## Base del proyecto

El TP2 continúa el repositorio Maven del TP1. La base local usa Java 25, Spring Boot 4.1.1, Maven Wrapper y springdoc 3.1.0; estos datos se verificaron en el `pom.xml` y el `README.md` del proyecto al crear esta carpeta.

## Dependencias Maven

| Artefacto | Uso | Alcance indicado |
|---|---|---|
| `org.springframework.boot:spring-boot-starter-data-jpa` | Spring Data JPA y Hibernate | Predeterminado |
| `org.postgresql:postgresql` | Driver JDBC de PostgreSQL | `runtime` |
| `org.springframework.boot:spring-boot-starter-flyway` | Autoconfiguración e integración de Flyway con Spring Boot 4.x | Predeterminado |
| `org.flywaydb:flyway-database-postgresql` | Soporte de Flyway para PostgreSQL | Predeterminado |

Spring Boot 4.1.1 administra las versiones compatibles mediante el parent Maven; la consigna no fija versiones individuales. El starter de referencia ya contiene estos cuatro artefactos en su `pom.xml`, además de las dependencias del TP1: WebMVC, Bean Validation, springdoc y tests.

**Punto importante de la consigna:** no alcanza con agregar `flyway-core` a secas. En Spring Boot 4.x se debe incluir `spring-boot-starter-flyway` junto con `flyway-database-postgresql` para que Flyway se autoconfigure y ejecute las migraciones.

## PostgreSQL

Se puede usar Docker Compose (recomendado) o una instalación local de PostgreSQL. El ejemplo del starter usa `postgres:17`, puerto `5432`, volumen persistente y las variables `POSTGRES_DB`, `POSTGRES_USER` y `POSTGRES_PASSWORD`. Su configuración de ejemplo es local y didáctica; conviene parametrizar las credenciales con variables de entorno al adaptar el proyecto.

La conexión de Spring se configura con `spring.datasource.url`, `spring.datasource.username` y `spring.datasource.password`. La configuración pedida es:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
```

Flyway administra los cambios de esquema; Hibernate solo valida que las entidades coincidan con lo migrado.

## Secuencia de migraciones solicitada

1. `V1__create_favoritos.sql`: tabla `favoritos` con `id`, `producto_id`, `nota` y `fecha_alta`.
2. `V2__create_listas.sql`: tabla `listas`.
3. `V3__add_lista_id_a_favoritos.sql`: columna `lista_id` y referencia a `listas(id)`, inicialmente nullable.
4. `V4__lista_id_obligatorio.sql`: crear o asegurar una lista por defecto, asignarla a los favoritos existentes sin lista y luego declarar `lista_id NOT NULL`.

Flyway registra versiones, descripción y checksum en `flyway_schema_history`. Una migración aplicada no se edita: los cambios posteriores se incorporan con una migración nueva.

## Recursos archivados

| Tema | Fuente original | Captura local |
|---|---|---|
| Consigna de TP2 | [Web II · TP2](https://unvime-web-2.matiendres.workers.dev/practicos/tp2/) | [`consigna.md`](./consigna.md) y [`fuentes/html/consigna-tp2.html`](./fuentes/html/consigna-tp2.html) |
| Presentación de TP2 | [Presentación · persistencia y arquitectura hexagonal](https://unvime-web-2.matiendres.workers.dev/practicos/presentaciones/tp2-persistencia-presentacion/) | [`presentacion-tp2.md`](./presentacion-tp2.md) y [`fuentes/html/presentacion-tp2.html`](./fuentes/html/presentacion-tp2.html) |
| Starter | [Repositorio · rama `tp2-starter`](https://github.com/Rns2590/demo/tree/tp2-starter) | [`tp2-starter.zip`](./recursos/tp2-starter.zip), [`extracto`](./recursos/tp2-starter-extracto/README.md) |
| Docker Compose | [Docker Docs](https://docs.docker.com/compose/) | [`docker-compose.md`](./fuentes/externas/docker-compose.md) |
| Imagen oficial PostgreSQL | [Docker Hub](https://hub.docker.com/_/postgres) | [`postgres-imagen-oficial.md`](./fuentes/externas/postgres-imagen-oficial.md) |
| PostgreSQL local | [PostgreSQL Downloads](https://www.postgresql.org/download/) | [`postgres-descargas.md`](./fuentes/externas/postgres-descargas.md) |
| Flyway | [Enlace de la consigna](https://flywaydb.org/documentation) · [Documentación vigente](https://documentation.red-gate.com/flyway) | [`flyway-documentacion-actual.md`](./fuentes/externas/flyway-documentacion-actual.md) |
| Migraciones versionadas | [Redgate Flyway](https://documentation.red-gate.com/fd/versioned-migrations-273973333.html) | [`flyway-migraciones-versionadas.md`](./fuentes/externas/flyway-migraciones-versionadas.md) |

La página de PostgreSQL y la documentación de Flyway referencian capturas oficiales descargadas; los HTML originales están junto a las versiones Markdown.
