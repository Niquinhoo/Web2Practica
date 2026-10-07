# Favoritos: cerrar la arquitectura en capas

TP1  · Favoritos — arquitectura en capas   rama develop

TP1 · Spring Boot — rama develop

# Favoritos: cerrar la
arquitectura en capas

De un catálogo de solo lectura a un CRUD propio en memoria — con el porqué de cada decisión, hasta llegar a Swagger.

Silva Rodrigo Nahuel  •  demo · localhost:8080

Agenda  7 objetivos del TP1

## ¿Qué pide el TP1, y quién lo resuelve?

**Resuelto antes de hoy   **Se completa hoy (Favoritos)   **Ya armado — hoy se pone a prueba

| # | Objetivo | Estado |
| --- | --- | --- |
| 1 | Configurar un proyecto Spring Boot con Maven |  Setup  |
| 2 | Arquitectura en capas (Controller → Service → Repository) |  Hoy  |
| 3 | Consumir un servicio web externo desde el backend |  Productos  |
| 4 | API RESTful propia, con DTOs desacoplados del modelo interno |  Hoy  |
| 5 | Aplicar validación de datos de entrada |  Hoy  |
| 6 | Implementar manejo uniforme de errores |  Se activa  |
| 7 | Documentar la API con Swagger/OpenAPI |  Se amplía  |
Teoría · antes de tipear  Objetivo 2

## Controller → Service → Repository

¿Qué es la inversión de dependencias?   Que una capa dependa de una **interfaz** (el "qué") y no de una clase concreta (el "cómo") de la capa de abajo. Así se puede cambiar la implementación sin tocar quien la usa.

- Se puede testear cada capa aislada, mockeando la interfaz de la que depende.

- Cambiar el proveedor de datos no debería obligar a tocar las capas de arriba.

- **Cómo lo implementamos:** las mismas 3 capas para `productos` y `favoritos` — solo cambia qué hay debajo del Service (un cliente HTTP o un repository en memoria).

De dónde arrancamos  Estado real en main

## Las 8 consignas del TP1, evaluadas contra el código

El enunciado plantea **7 objetivos** generales (slide anterior) y los baja a estas **8 consignas** concretas — por eso cambia el número de una lista a la otra.

**Resuelto   **Infraestructura lista, sin lógica   **Sin empezar

| # | Consigna | Estado |
| --- | --- | --- |
| 1 | Setup del proyecto |  Completo  |
| 2 | Catálogo de productos (API externa) |  Solo andamiaje  |
| 3 | Dominio Favorito + repository en memoria |  Nada  |
| 4 | DTOs de favoritos |  Nada  |
| 5 | Controller de favoritos (CRUD) |  Nada  |
| 6 | Validación |  Infra lista  |
| 7 | Manejo uniforme de errores |  Casi completo  |
| 8 | Documentación (Swagger) |  Parcial  |
Esto es lo que muestra `main` — lo único que ven los alumnos. `develop` (esta rama) ya venía adelantada: el punto 2 se había resuelto **antes** de esta clase.

Productos · RestClient  Objetivo 3

## El primer contacto con una API externa

```
// RestClientConfig
@Configuration
public class   RestClientConfig  {

@Bean
public  RestClient dummyJsonRestClient(
@Value ( "${app.dummyjson.base-url}" ) String baseUrl) {
return  RestClient.builder()
.baseUrl(baseUrl)
.build();
}
}
```

¿Qué es RestClient?   El cliente HTTP declarativo de Spring para consumir APIs REST — la forma moderna de reemplazar a `RestTemplate`.

- **Cómo lo implementamos:** un único `@Bean` centraliza la URL base (leída de `application.properties`) — evita repetirla en cada llamada y permite apuntar a otra URL en tests sin tocar código.

- El JSON externo se modela aparte, en `DummyJsonProducto`/`DummyJsonProductosResponse` — dos records que calcan exactamente los campos de DummyJSON, solo para que Jackson los deserialice.

- Separar la *configuración* (URL, timeouts) de la *lógica de las llamadas*: si cambia la URL, se toca un solo lugar.

Productos · Cliente HTTP  Objetivo 3

## Traducir cualquier falla externa a una excepción propia

```
public  DummyJsonProducto obtenerPorId(Long id) {
try  {
return  restClient.get()
.uri( "/products/{id}" , id)
.retrieve()
.body(DummyJsonProducto.class);
}  catch  (HttpClientErrorException.NotFound e) {
throw new  RecursoNoEncontradoException(
"No existe el producto con id "  + id);
}  catch  (ResourceAccessException e) {
throw new  ServicioExternoException(
"No se pudo contactar a DummyJSON" , e);
}
// + HttpServerErrorException, RestClientException...
}
```

¿Qué es DDD?   **Domain-Driven Design**: un enfoque de diseño que organiza el código alrededor del vocabulario y las reglas del negocio (el "dominio"), en vez de alrededor de la tecnología. No lo seguimos completo acá — solo tomamos prestado uno de sus patrones.

¿Qué es un anti-corruption layer?   Ese patrón de DDD: una capa fina que aísla el "vocabulario" de un sistema externo para que no se filtre al resto de la app — acá, la única clase que conoce los nombres de campo de DummyJSON.

- **Cómo lo implementamos:** 4 `catch`, de más específico a más general (`NotFound` es un caso particular de `HttpClientErrorException`); `ResourceAccessException` es la falla de red pura — timeout, DNS, conexión rechazada, sin código HTTP.

- El service y el controller nunca ven una excepción de `RestClient` — mismo `GlobalExceptionHandler` que ya conocemos de favoritos traduce el resto.

Productos · Contrato propio  Objetivo 4

## De DummyJsonProducto a ProductoDTO

```
public record   ProductoDTO (
Long id, String nombre, String descripcion,
String categoria, String marca,  double  precio,
double  descuentoPorcentaje,  int  stock,
double  calificacion, String imagenUrl
) {}

// ProductoServiceImpl — único lugar del proyecto
// donde conviven los dos vocabularios
private  ProductoDTO aProductoDTO(DummyJsonProducto e) {
return new  ProductoDTO(
e.id(), e.title(), e.description(), ...);
}
```

¿Qué es un record?   Un tipo de Java pensado para **solo transportar datos, de forma inmutable**: con una firma, genera constructor, accesores (`id()`, `nombre()`...), `equals()`, `hashCode()` y `toString()` — sin escribirlos a mano. Lo vamos a usar en todos los DTOs y entidades del proyecto.

- **Cómo lo implementamos:** `aProductoDTO` es `private` a propósito — nadie fuera del service necesita conocer los nombres de campo de DummyJSON (`title` → `nombre`, `thumbnail` → `imagenUrl`...).

- `ProductoPageResponse` es un contrato de paginación propio: reusa `limit`/`skip` como parámetros de entrada, pero la forma de la respuesta es nuestra.

- Es de **solo lectura** (`GET`): no hay `@RequestBody`, así que no dispara validación — por eso el primer `@Valid` del proyecto recién aparece en favoritos.

Punto de partida de hoy  develop, antes de esta clase

## 8 archivos de diferencia

Con productos ya resuelto en `develop` (recién visto), esto es lo que faltaba para cerrar los puntos 3, 4, 5, y activar el 6 y el 8:

### Ya estaba

- `pom.xml`: webmvc, validation, springdoc

- Setup + `/health`, `/ping`

- `RestClientConfig`, `OpenApiConfig`

- Módulo `productos` completo (client, DTOs, service, controller)

- `GlobalExceptionHandler` + 2 excepciones propias

### Faltaba (hoy)

- `domain/Favorito`

- `repository/FavoritoRepository` + impl en memoria

- `dto/favorito/`: Request y Response

- `service/FavoritoService` + impl

- `controller/FavoritoController` — CRUD completo

Ni una línea de `GlobalExceptionHandler` cambió: se reusa tal cual.

01 · Dominio  Objetivo 2

## domain/Favorito

```
public record   Favorito (
Long  id,
Long  productoId,
String  nota,
LocalDateTime  fechaAgregado
) {}
```

- `Favorito` es otro `record` — mismo mecanismo que ya vimos en `ProductoDTO`: constructor, accesores y `equals`/`hashCode`/`toString` generados solos.

- **Cómo lo implementamos:** todos los campos quedan `final` — no hay setters. "Cambiar" un favorito es crear uno nuevo, nunca mutar el existente — clave cuando el repository comparte esa instancia entre threads (siguiente slide).

- Solo se guarda `productoId`, **no** nombre/precio/imagen: esos datos viven en `ProductoService` y duplicarlos los deja desactualizados si el producto cambia.

- Un favorito es una **referencia** a un producto externo + una nota — no una copia.

02 · Repository en memoria  Objetivo 2

## Interfaz primero, después el Map

```
// FavoritoRepository — el contrato
public interface   FavoritoRepository  {
List<Favorito> findAll();
Optional<Favorito> findById(Long id);
Favorito save(Favorito favorito);
void  deleteById(Long id);
}

// InMemoryFavoritoRepository — la implementación
private final  Map<Long, Favorito> datos
=  new  ConcurrentHashMap<>();
private final  AtomicLong nextId
=  new  AtomicLong(1);
```

¿Qué es una interfaz?   Un **contrato**: declara qué métodos existen (`findAll`, `save`...) sin decir cómo se resuelven. `FavoritoRepository` es el contrato; `InMemoryFavoritoRepository` es una forma de cumplirlo — podría haber otra.

¿Qué es un Map?   Una estructura **clave → valor**: para cada `Long` (el id) guarda un `Favorito`. Buscar, guardar o borrar por id es directo — no hay que recorrer una lista comparando uno por uno.

- El **service depende de la interfaz**, no del Map — en el TP2 se cambia la implementación por una con JPA sin tocar el service.

- Acá el Map vive *adentro* de `InMemoryFavoritoRepository`, como `ConcurrentHashMap` + `AtomicLong`: Tomcat atiende requests en paralelo, y un `HashMap`/`long++` normales pueden corromperse o repetir un id.

- **Gancho al TP2:** si reinicio la app, ¿qué pasa con los favoritos?

03 · DTOs  Objetivo 4

## Un DTO de entrada, otro de salida

```
// FavoritoRequest — lo que llega
public record   FavoritoRequest (
@NotNull (message =  "productoId es obligatorio" )
Long productoId,

@NotBlank (message =  "nota no puede estar vacía" )
String nota
) {}

// FavoritoResponse — lo que se devuelve
public record   FavoritoResponse (
Long id, Long productoId,
String nota, LocalDateTime fechaAgregado
) {}
```

¿Qué es un DTO?   **Data Transfer Object**: un objeto que solo transporta datos por la red, sin lógica de negocio. No es la entidad de dominio — es la forma que el cliente de la API ve desde afuera.

- **Cómo lo implementamos:** dos records distintos — `FavoritoRequest` (con anotaciones de validación) y `FavoritoResponse` (sin ellas: no tiene sentido validar lo que la propia API produce).

- Desacopla lo que el cliente ve del modelo interno (`Favorito`) — si el dominio cambia, no rompe necesariamente el contrato hacia afuera.

- `@NotNull` en `productoId` (un `Long` no tiene "vacío") vs `@NotBlank` en `nota` (cubre `null`, `""` y espacios en un solo chequeo).

04 · Validación  Objetivo 5

## Bean Validation en el borde de la API

```
// FavoritoController
@PostMapping
public  ResponseEntity<FavoritoResponse> crear(
@Valid   @RequestBody  FavoritoRequest request) {
...
}
```

¿Qué es Bean Validation?   Un estándar de Java (**JSR 380**) para declarar restricciones como anotaciones sobre un objeto (`@NotNull`, `@NotBlank`...) en vez de encadenar `if` a mano.

- **Cómo lo implementamos:** alcanza con `@Valid` antes de `@RequestBody` en la firma del controller — Spring evalúa las anotaciones del DTO **antes** de ejecutar una sola línea del método.

- Si falla, lanza `MethodArgumentNotValidException` — ya manejada por el `GlobalExceptionHandler` existente.

- Primer `@RequestBody` del proyecto: productos es de solo lectura.

curl -i POST /api/favoritos {"productoId":null,"nota":""}  `HTTP/1.1 400` `{"detail":"Uno o más campos no son válidos", "title":"Error de validación", "errores":{"productoId":"productoId es obligatorio", "nota":"nota no puede estar vacía"}}`

05 · Service  Objetivo 2 / 4

## Mapeo y la regla del 404, en un solo lugar

```
private  Favorito buscarOFallar(Long id) {
return  repository.findById(id)
.orElseThrow(() ->
new  RecursoNoEncontradoException(
"No existe el favorito con id "  + id));
}

@Override
public  FavoritoResponse actualizar(Long id, FavoritoRequest r) {
Favorito existente = buscarOFallar(id);
Favorito actualizado =  new  Favorito(
existente.id(), r.productoId(), r.nota(),
existente.fechaAgregado()  // no se pisa
);
return  aResponse(repository.save(actualizado));
}
```

¿Qué es `Optional`?   Un contenedor que dice explícitamente "puede haber un valor, o no" — `findById` devuelve `Optional<Favorito>` en vez de arriesgarse a un `null` que alguien se olvide de chequear.

- **Cómo lo implementamos:** `orElseThrow(...)` convierte un `Optional` vacío directamente en la excepción, en una sola línea, sin `if (resultado == null)`.

- `buscarOFallar` es el **único lugar** con la regla "si no existe, 404" — la usan `obtener`, `actualizar` y `eliminar` por igual.

- El controller **nunca ve la entidad** `Favorito`, solo DTOs — el mapeo vive acá. Al actualizar, se conserva la `fechaAgregado` original.

06 · Controller  Objetivo 4

## Verbos y códigos de estado correctos

Los **verbos HTTP** tienen semántica propia (GET no modifica nada, POST crea, PUT reemplaza, DELETE borra) y los **códigos de estado** le dicen al cliente qué pasó sin que tenga que leer el body.

| Operación | Método | Éxito |
| --- | --- | --- |
| Crear | `POST /api/favoritos` | 201 |
| Listar | `GET /api/favoritos` | 200 |
| Obtener uno | `GET /api/favoritos/{id}` | 200 |
| Actualizar | `PUT /api/favoritos/{id}` | 200 |
| Eliminar | `DELETE /api/favoritos/{id}` | 204 |
```
@PostMapping
public  ResponseEntity<FavoritoResponse> crear(
@Valid   @RequestBody  FavoritoRequest request) {
FavoritoResponse creado = service.crear(request);
return  ResponseEntity
.created(URI.create( "/api/favoritos/"  + creado.id()))
.body(creado);
}
```

**Cómo lo implementamos:** cuando hace falta controlar algo más que el body —acá, el header `Location` y el `201`— se usa `ResponseEntity`. Cuando el status es siempre el mismo, se devuelve el DTO directo y Spring arma el `200 OK` solo.

Transversal  Objetivo 6

## El manejo de errores ya existía — hoy se activa

¿Qué es RFC 7807 / ProblemDetail?   Un estándar para que las respuestas de error tengan una forma predecible (`status`, `title`, `detail`) en toda la API, en vez de que cada endpoint invente su propio JSON de error.

¿Qué es `@RestControllerAdvice`?   Una clase que intercepta las excepciones de **todos** los controllers a la vez — en vez de un `try/catch` repetido en cada uno.

- **Cómo lo implementamos:** favoritos no escribió ningún manejador nuevo — solo lanza `RecursoNoEncontradoException` y deja que Bean Validation dispare `MethodArgumentNotValidException`; el `GlobalExceptionHandler` que ya existía se encarga del resto.

- El **mismo handler** traduce la falla externa de `productos` a un `502`: las **tres formas** que pide la consigna —`404`, `400` y `5xx`— salen con un único formato `ProblemDetail`.

curl -i GET /api/favoritos/1 (borrado)  `HTTP/1.1 404` `{"detail":"No existe el favorito con id 1", "status":404,"title":"Not Found"}`

curl -i DELETE /api/favoritos/1  `HTTP/1.1 204 No Content`

curl -i GET /api/productos/1 (DummyJSON caído)  `HTTP/1.1 502 Bad Gateway` `{"detail":"No se pudo contactar a DummyJSON (timeout o caída del servicio)", "status":502, "title":"Falla al consumir un servicio externo"}`

Documentación  Objetivo 7

## Swagger/OpenAPI se genera desde el código

```
@RestController
@RequestMapping ( "/api/favoritos" )
@Tag (name =  "favoritos" ,
description =  "CRUD en memoria" )
public class   FavoritoController  {

@Operation (summary =  "Crear un favorito" )
@PostMapping
public  ResponseEntity<FavoritoResponse> crear(...)
```

¿Qué es OpenAPI?   Una especificación (un JSON/YAML con un schema definido) que describe una API HTTP de forma independiente del lenguaje. Swagger UI es solo una de las herramientas que saben renderizarla.

- **Cómo lo implementamos:** `@Tag` en la clase agrupa los endpoints bajo "favoritos"; `@Operation` en cada método le agrega el resumen — nada más, springdoc arma el resto solo.

- springdoc lee los `@RestController` reales — la documentación **nunca se desincroniza** de la implementación.

curl /v3/api-docs  `"tags":[{"name":"productos"},{"name":"favoritos"}] "paths": /api/productos, /api/productos/{id}, /api/favoritos, /api/favoritos/{id}`

Balance  7/7

## Los 7 objetivos, cerrados

| # | Objetivo | Estado |
| --- | --- | --- |
| 1 | Setup con Maven |  ✓  |
| 2 | Arquitectura en capas |  ✓ Productos + Favoritos  |
| 3 | Consumo de servicio externo |  ✓  |
| 4 | API RESTful con DTOs desacoplados |  ✓  |
| 5 | Validación de entrada |  ✓  |
| 6 | Manejo uniforme de errores |  ✓  |
| 7 | Documentación con Swagger |  ✓  |
Misma arquitectura, mismo manejo de errores, misma documentación — **reusada, no reinventada.**

Generalizando el patrón  Plantilla reusable

## ¿Y si mañana agregamos otro recurso?

1. **Dominio** — un `record` inmutable con los campos propios (ej. `Reseña`: id, productoId, puntaje, comentario, fecha).

2. **Repository** — interfaz (`findAll`/`findById`/`save`/`deleteById`) + implementación en memoria, mismo contrato que `FavoritoRepository`.

3. **DTOs** — uno de entrada (con `@NotNull`/`@NotBlank` donde corresponda) y uno de salida. Nunca el mismo objeto para las dos direcciones.

4. **Service** — mapea entidad ↔ DTOs y concentra las reglas de negocio, con un `buscarOFallar` propio, igual que en favoritos.

5. **Controller** — los endpoints con el verbo y el status code correctos; `@Valid` en los que reciben body.

6. **Nada más:** `GlobalExceptionHandler`, `RestClientConfig`, `OpenApiConfig` se reusan tal cual — no se tocan.

Es la misma receta que usamos hoy con `Favorito` — no hay nada nuevo que inventar, solo repetir el patrón con otro nombre.

Demo en vivo  Cierre

## Ahora, en Swagger UI

http://localhost:8080/swagger-ui/index.html

1. Abrir Swagger y mostrar los **dos grupos**: `productos` y `favoritos`.

2. `GET /api/productos/1` → mostrar el mapeo a `ProductoDTO` (nombre/precio/imagenUrl) que acabamos de ver en código.

3. `POST /api/favoritos` → Try it out → crear uno (con ese mismo `productoId`) → **201** + header `Location`.

4. `GET` listar y `GET /{id}` del que se acaba de crear.

5. `PUT /{id}` → mostrar que `fechaAgregado` no cambia.

6. `DELETE /{id}` → **204**, y `GET` de nuevo → **404** con `ProblemDetail`.

7. `POST` con body vacío → **400** con el detalle de qué campo falló.

Todo lo mostrado ya fue verificado end-to-end antes de esta clase.

Repositorio: [github.com/Rns2590/demo](https://github.com/Rns2590/demo)
