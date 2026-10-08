# TP2: persistencia y arquitectura hexagonal

TP2  · Persistencia, migraciones y arquitectura hexagonal   rama tp2

TP2 · Spring Boot — rama tp2

# De un Map a PostgreSQL,
sin tocar el Service

Flyway versiona el esquema, JPA/Hibernate lo mapea, y un puerto (`FavoritoRepository`) deja cambiar de memoria a base de datos real sin que el Service ni el Controller se enteren.

Silva Rodrigo Nahuel  •  demo · localhost:8080

Agenda  6 objetivos del TP2

## ¿Qué pide el TP2?

| # | Objetivo |
| --- | --- |
| 1 | Levantar PostgreSQL y conectarla con Spring Data JPA |
| 2 | Reemplazar el repository en memoria por un adapter JPA — sin tocar dominio, Service ni Controller |
| 3 | Versionar el esquema de base de datos con Flyway |
| 4 | Modelar una relación entre entidades y exponerla en la API |
| 5 | Delimitar una operación transaccional y justificarla en términos de atomicidad |
| 6 | Reconocer la diferencia entre un puerto (contrato) y un adapter (implementación) |
Los puntos 2 y 6 son la misma idea vista dos veces: **arquitectura hexagonal**, el término que da nombre al práctico.

Preparación del entorno  Objetivo 1

## PostgreSQL: dos caminos, mismo resultado

### Docker (recomendado)

- `docker-compose.yml` en la raíz del repo, imagen oficial `postgres:17`.

- `docker compose up -d` y listo — mismo entorno para cualquiera que clone el repo.

- **Estado en esta máquina:** pendiente — el motor de Docker Desktop quedó fallando al arrancar (`context deadline exceeded` en WSL2) incluso después de un reset de fábrica. El `docker-compose.yml` ya está commiteado y listo para cuando se resuelva.

### Local (lo que usamos hoy)

- PostgreSQL 18 instalado directo (instalador de EnterpriseDB) — la alternativa que la propia consigna permite si no se puede usar Docker.

- Rol y base creados a mano, desde pgAdmin (Query Tool sobre la base `postgres`):

```
CREATE ROLE  webii_tp2  WITH LOGIN PASSWORD   'webii_tp2'   SUPERUSER ;
CREATE DATABASE  webii_tp2  OWNER  webii_tp2;
```

Configuración del repo  Objetivo 1

## application.properties: la conexión y quién manda en el esquema

```
# Base de datos (ver docker-compose.yml)
spring.datasource.url=jdbc:postgresql://localhost:5432/webii_tp2
spring.datasource.username=webii_tp2
spring.datasource.password=webii_tp2

# El esquema lo maneja Flyway, no Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
```

¿Por qué `ddl-auto=validate` y no `update`?   Con `update` o `create`, Hibernate también podría modificar el esquema — dos herramientas (Hibernate y Flyway) compitiendo por la misma tabla. `validate` deja que Hibernate solo **chequee** que sus entidades coinciden con lo que Flyway ya creó.

- **Cómo lo implementamos:** mismo usuario/base tanto si se usa Docker como instalación local — solo cambia cómo se levantó el servidor, no cómo se conecta la app.

- `open-in-view=false`: evita que la conexión a la base quede abierta durante todo el ciclo de vida del request HTTP — buena práctica con JPA que no se ve a simple vista.

Flyway · teoría  Objetivo 3

## Versionar el esquema, no improvisarlo

¿Qué es una migración?   Un script SQL versionado que describe **un cambio puntual** del esquema. Flyway las aplica en orden, una sola vez cada una, y registra cuáles ya corrieron.

¿Para qué sirve, si ya tengo Hibernate?   Hibernate puede *inferir* un esquema a partir de las entidades, pero eso es implícito y difícil de auditar. Flyway deja un historial explícito, reproducible en cualquier máquina — y el único con `NOT NULL`, índices o backfills a mano.

- **Cómo lo implementamos:** convención `V{versión}__{descripción}.sql` en `src/main/resources/db/migration` — Flyway la lee del classpath sola, sin configuración extra.

- Cada migración aplicada queda registrada en `flyway_schema_history`, con su checksum.

SELECT version, description, success FROM flyway_schema_history;  `1 | create favoritos | t 2 | create listas | t 3 | add lista id a favoritos | t 4 | lista id obligatorio | t`

Flyway · cómo lo implementamos  Objetivo 3

## Cuatro migraciones, una por cambio

```
-- V1__create_favoritos.sql
CREATE TABLE  favoritos (
id  BIGINT   GENERATED ALWAYS AS IDENTITY PRIMARY KEY ,
producto_id  BIGINT   NOT NULL ,
nota  VARCHAR (500)  NOT NULL ,
fecha_alta  TIMESTAMP   NOT NULL
);

-- V3__add_lista_id_a_favoritos.sql
ALTER TABLE  favoritos
ADD COLUMN  lista_id  BIGINT   REFERENCES  listas (id);
```

- `V1` crea `favoritos`, `V2` crea `listas` (misma forma), `V3` suma `lista_id` — **nullable** a propósito, para no romper filas ya existentes.

- `V4` (dos diapositivas más adelante) es la que la vuelve obligatoria — ese orden es la parte interesante del práctico.

- Reiniciar la app no vuelve a correr `V1`-`V3`: Flyway ve que ya están en `flyway_schema_history` y solo valida sus checksums.

JPA / Hibernate · teoría  Objetivo 1

## Un ORM: ni una query SQL escrita a mano

¿Qué es un ORM?   **Object-Relational Mapping**: traduce automáticamente entre objetos Java y filas de una tabla, a partir de anotaciones (`@Entity`, `@Id`, `@Column`) — nosotros describimos la forma, Hibernate arma el SQL.

¿Y si no escribimos ninguna query?   `JpaRepository` ya trae `save`/`findById`/`deleteById` implementados en tiempo de ejecución (Spring genera un proxy — no hay una clase `.java` con ese código). Para `findByListaId`, Spring Data **lee el nombre del método** y arma el `WHERE` solo — se llama *derived query*.

- **Cómo lo implementamos:** ningún método nuestro tiene SQL, JPQL ni `@Query` — todo el CRUD sale de heredar `JpaRepository` + una convención de nombre. Si hiciera falta algo que el nombre no puede expresar, ahí sí se escribiría `@Query`, pero no fue necesario acá.

Lo que llamamos → lo que ejecuta Hibernate  `jpaRepository.save(entity)  // id nuevo  → INSERT INTO favoritos (...) VALUES (...) jpaRepository.save(entity)  // id existente  → UPDATE favoritos SET ... WHERE id = ? jpaRepository.findById(id) → SELECT * FROM favoritos WHERE id = ? jpaRepository.findByListaId(listaId)  // derivada del nombre  → SELECT * FROM favoritos WHERE lista_id = ? jpaRepository.deleteById(id) → DELETE FROM favoritos WHERE id = ?`

JPA · una acción específica  Objetivo 1

## ¿Method derivado, orquestación o @Query?

¿Dónde vive "mover favoritos"?   En el **Service**, no en el repository: no es una operación de la base, es *combinar* operaciones simples que ya existen (`findById`, `findByListaId`, `save`, `deleteById`) dentro de un `@Transactional`.

¿Y si el nombre de un método no alcanza?   Ahí sí se escribe **JPQL** con `@Query` — como SQL, pero habla de entidades y campos Java, no de tablas y columnas.

- **Cómo lo implementamos:** `moverFavoritos` no escribe ninguna query nueva — un `for` con `N` llamadas a `save()`, más fácil de leer que optimizar.

```
// Alternativa con @Query + @Modifying:
// un solo UPDATE en vez del for con N save()
@Modifying
@Query ( "UPDATE FavoritoEntity f "  +
"SET f.lista.id = :destinoId "  +
"WHERE f.lista.id = :origenId" )
void  reasignarLista(Long origenId, Long destinoId);

// más rápido con muchos favoritos, pero se salta
// las validaciones que Hibernate corre por save()
```

JPA / Hibernate · cómo lo implementamos  Objetivo 1

## FavoritoEntity: mutable, con constructor vacío

```
@Entity
@Table (name =  "favoritos" )
public class   FavoritoEntity  {

@Id
@GeneratedValue (strategy =  GenerationType .IDENTITY)
private  Long id;

@Column (name =  "producto_id" , nullable =  false )
private  Long productoId;

// constructor vacío + getters/setters
public  FavoritoEntity() {}
}
```

- `@Id` + `@GeneratedValue(IDENTITY)` delega en Postgres (`GENERATED ALWAYS AS IDENTITY` de `V1`) quién asigna el id — no lo calcula Java.

- `@Column(name = "producto_id")` es explícito: sin esa anotación, Hibernate ya adivina `producto_id` a partir de `productoId` (convención camelCase → snake_case), pero se deja explícito donde el nombre de columna importa para leer la migración.

- El `record Favorito` del dominio (TP1) **no se tocó** — sigue siendo inmutable y sin ninguna anotación de JPA.

Puerto y adapter  Objetivo 2 / 6

## Un contrato, dos formas de cumplirlo

```
// FavoritoRepository — el puerto (sin cambios de forma)
public interface   FavoritoRepository  {
List<Favorito> findAll();
Optional<Favorito> findById(Long id);
Favorito save(Favorito favorito);
void  deleteById(Long id);
}

// FavoritoRepositoryAdapter — el adapter nuevo (JPA)
@Override
public  Favorito save(Favorito favorito) {
FavoritoEntity entity =  new  FavoritoEntity();
entity.setProductoId(favorito.productoId());
entity.setLista(listaJpaRepository
.getReferenceById(favorito.listaId()));
return  aDominio(jpaRepository.save(entity));
}
```

¿Qué es un puerto?   Una interfaz que declara **qué** necesita el dominio, sin decir **cómo** se resuelve — `FavoritoRepository`, la misma del TP1, solo que ahora le ponemos nombre.

¿Qué es un adapter?   Una implementación concreta de infraestructura para ese puerto. `InMemoryFavoritoRepository` (TP1) y `FavoritoRepositoryAdapter` (TP2) son adapters intercambiables detrás de la misma interfaz.

- **Cómo lo implementamos:** `InMemoryFavoritoRepository` se **eliminó** — dos beans implementando el mismo puerto no son ambiguos por accidente, Spring no sabría cuál inyectar.

- `getReferenceById` arma un proxy con solo el id de la lista, sin ir a buscarla completa — alcanza para setear la FK.

Arquitectura hexagonal · teoría  El tema del título

## Ports & Adapters, con dibujito

- El dominio (`Favorito`, el puerto) queda en el centro; la infraestructura (memoria, JPA) queda **afuera** y es reemplazable.

- **Cómo lo implementamos:** el adapter tachado ya no existe en el código — se borró al reemplazarlo — pero el diagrama lo deja para mostrar que *pudo* seguir ahí, intercambiable.

- Nada que dependa del puerto (`FavoritoServiceImpl`, y por lo tanto `FavoritoController`) tuvo que enterarse del cambio.

Qué cambió y qué no  Objetivo 2

## El Service y el Controller, intactos

### No se tocó

- `FavoritoService` / `FavoritoServiceImpl`

- `FavoritoController`

- `FavoritoRequest` / `FavoritoResponse` (forma; solo sumaron `listaId`)

- `record Favorito` del dominio

- `GlobalExceptionHandler` (se le agregó un handler, no se modificó el existente)

### Nuevo / reemplazado

- `InMemoryFavoritoRepository` → `FavoritoRepositoryAdapter`

- `entity/`: `FavoritoEntity`, `ListaEntity`

- `FavoritoJpaRepository`, `ListaJpaRepository` (Spring Data)

- Módulo `Listas` completo (dominio, puerto, adapter, service, controller)

Esa lista de la izquierda, vacía, es **la** demostración de que `FavoritoRepository` cumplió su rol de puerto.

Relación entre entidades · teoría  Objetivo 4

## @ManyToOne, sin el lado inverso

¿Qué modela `@ManyToOne`?   Que **muchos** favoritos pueden apuntar a **una** lista — el lado "muchos" de la relación es quien tiene la columna de foreign key (`lista_id`).

¿Por qué no un `@OneToMany` del otro lado?   Un `@OneToMany` bidireccional (que `ListaEntity` tenga una lista de sus favoritos) trae complicaciones de carga perezosa (*lazy loading*) que no hacen falta para este práctico.

- **Cómo lo implementamos:** el lado inverso — "los favoritos de una lista" — se resuelve con una **consulta derivada** en el repository, no con una relación de objetos.

Relación entre entidades · cómo lo implementamos  Objetivo 4

## La FK en la entidad, la consulta en el repository

```
// FavoritoEntity
@ManyToOne (fetch = FetchType.LAZY)
@JoinColumn (name =  "lista_id" , nullable =  false )
private  ListaEntity lista;

// FavoritoJpaRepository — el lado inverso, sin @OneToMany
public interface   FavoritoJpaRepository
extends  JpaRepository<FavoritoEntity, Long> {
List<FavoritoEntity> findByListaId(Long listaId);
}
```

- `fetch = LAZY`: la lista completa no se trae de la base hasta que alguien la pida explícitamente — `entity.getLista().getId()` sí funciona sin ir a la base, porque el id ya está disponible en el proxy.

- `findByListaId` es una **consulta derivada**: Spring Data arma el SQL solo a partir del nombre del método — nada de HQL ni SQL nativo a mano.

- La usan `GET /api/listas/{id}/favoritos`, el chequeo del `409` (slide 17) y la operación de mover favoritos.

Evolución del esquema · teoría  Objetivo 3

## Nunca se edita una migración ya aplicada

¿Qué es el checksum de una migración?   Un hash del contenido del script, que Flyway calcula al aplicarlo y guarda en `flyway_schema_history`. Si el archivo cambia después, el checksum ya no coincide.

¿Por qué rechaza el arranque?   Porque una base que ya corrió la versión vieja de `V3` quedaría en un estado **distinto** del de una base nueva que corre la versión editada por primera vez — Flyway prefiere frenar la app antes que dejar bases divergentes.

- **El problema real:** las filas de `favoritos` creadas antes del punto 5 (relación con Listas) no tenían `lista_id`.

- **Cómo lo resolvimos:** una migración **nueva**, `V4` — nunca tocando `V1`-`V3` ya aplicadas.

Evolución del esquema · cómo lo implementamos  Objetivo 3

## Backfill primero, restricción después

```
-- V4__lista_id_obligatorio.sql
INSERT INTO  listas (nombre)
SELECT   'Sin clasificar'
WHERE NOT EXISTS  ( SELECT  1  FROM  listas
WHERE  nombre =  'Sin clasificar' );

UPDATE  favoritos
SET  lista_id = ( SELECT  id  FROM  listas
WHERE  nombre =  'Sin clasificar' )
WHERE  lista_id  IS NULL ;

ALTER TABLE  favoritos
ALTER COLUMN  lista_id  SET NOT NULL ;
```

- Las tres sentencias van **en ese orden**: crear la lista por defecto, reasignar los favoritos huérfanos, y recién ahí declarar `NOT NULL` — invertir el orden rompería contra datos existentes.

- Corrimos la migración contra una base con favoritos creados antes del punto 5 y no rompió ninguna fila.

Transacciones · teoría  Objetivo 5

## ACID, y en particular la "A"

¿Qué es una transacción?   Un conjunto de escrituras que la base trata como **una sola unidad**: o se aplican todas, o no se aplica ninguna.

¿Qué es la atomicidad (la "A" de ACID)?   La garantía de que no queda un estado a mitad de camino — ni el "antes" de la operación ni el "después", sino algo intermedio que la API nunca debería poder producir.

- **La operación:** `POST /api/listas/{origenId}/mover-favoritos` — reasigna todos los favoritos de una lista a otra, y borra la de origen. Varias escrituras, una sola operación de negocio.

Transacciones · cómo lo implementamos  Objetivo 5

## @Transactional en el Service, no en el Controller

```
@Override
@Transactional
public void  moverFavoritos(Long origenId,
MoverFavoritosRequest request) {
Lista origen = buscarOFallar(origenId);
Lista destino = buscarOFallar(request.listaDestinoId());

for  (Favorito f : favoritoRepository
.findByListaId(origen.id())) {
favoritoRepository.save( new  Favorito(
f.id(), f.productoId(), f.nota(),
f.fechaAgregado(), destino.id()));
}
listaRepository.deleteById(origen.id());
}
```

- **Sin `@Transactional`:** si la escritura que borra la lista origen fallara después de reasignar algunos favoritos (por ejemplo, una caída de conexión a mitad de camino), quedarían favoritos ya movidos a destino pero la lista origen seguiría existiendo — inconsistente.

- Ambas listas se validan **antes** de escribir nada (`404` si alguna no existe) — la transacción cubre las escrituras, no reemplaza la validación previa.

Manejo de errores  Consigna 5.6

## Un caso de negocio nuevo: 409, no 500

```
// ListaServiceImpl.eliminar
if  (!favoritoRepository
.findByListaId(lista.id()).isEmpty()) {
throw new  ListaNoVaciaException(
"La lista "  + id +
" todavía tiene favoritos, no se puede eliminar" );
}

// GlobalExceptionHandler — un handler más, ninguno tocado
@ExceptionHandler (ListaNoVaciaException.class)
public  ProblemDetail handleListaNoVacia(
ListaNoVaciaException ex) {
return  ProblemDetail.forStatusAndDetail(
HttpStatus.CONFLICT, ex.getMessage());
}
```

curl -i DELETE /api/listas/2 (con favoritos)  `HTTP/1.1 409` `{"detail":"La lista 2 todavía tiene favoritos, no se puede eliminar","status":409,"title":"Conflict"}`

curl -i POST /api/listas/2/mover-favoritos {"listaDestinoId":3}  `HTTP/1.1 204 No Content`

Balance  6/6

## Los 6 objetivos, cerrados

| # | Objetivo | Estado |
| --- | --- | --- |
| 1 | PostgreSQL + Spring Data JPA |  ✓  |
| 2 | Adapter JPA sin tocar dominio/Service/Controller |  ✓  |
| 3 | Esquema versionado con Flyway |  ✓ 4 migraciones  |
| 4 | Relación entre entidades expuesta en la API |  ✓ Favorito ↔ Lista  |
| 5 | Operación transaccional justificada |  ✓ mover-favoritos  |
| 6 | Puerto vs. adapter |  ✓  |
Mismo Service, mismo Controller, mismos DTOs (+1 campo) — **la infraestructura cambió, el dominio no se enteró.**

Demo en vivo  Cierre

## Ahora, en Swagger UI

http://localhost:8080/swagger-ui/index.html

1. Mostrar los **tres grupos**: `productos`, `favoritos` y `listas`.

2. `POST /api/listas` → crear "Regalos" → **201**.

3. `POST /api/favoritos` con ese `listaId` → **201**.

4. `DELETE` esa lista (todavía con el favorito adentro) → **409 Conflict**.

5. `POST /api/listas/{origenId}/mover-favoritos` a una segunda lista → **204**, y la lista origen desaparece.

6. Reiniciar la app (`Ctrl+C`, volver a `spring-boot:run`) → los datos siguen ahí.

Todo lo mostrado ya fue verificado end-to-end antes de esta clase, incluyendo la persistencia real entre reinicios.

Repositorio: [github.com/Rns2590/demo](https://github.com/Rns2590/demo) — rama `tp2`
