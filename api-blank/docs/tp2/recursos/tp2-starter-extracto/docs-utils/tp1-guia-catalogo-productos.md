# Guía TP1 — Catálogo de productos: paso a paso con código (`develop`)

Guía detallada de cómo se armó el módulo de productos en `develop`, en el
orden en que conviene construirlo. Cada paso explica **qué estamos
haciendo y por qué**, con el código real tal como quedó en el repo — para
que se pueda replicar exactamente.

Orden de construcción (de abajo hacia arriba, siguiendo las dependencias):

```
1. Config (RestClientConfig, OpenApiConfig)
2. Forma del JSON externo (DummyJsonProducto, DummyJsonProductosResponse)
3. Cliente HTTP (DummyJsonClient) + excepciones de dominio
4. Contrato propio (ProductoDTO, ProductoPageResponse)
5. Service (mapeo externo -> propio)
6. Controller (expone la API)
7. Manejo uniforme de errores (GlobalExceptionHandler)
```

---

## Los objetivos del TP1 y dónde se ven acá

El TP1 plantea siete objetivos generales. Este módulo (productos) no
demuestra los siete al 100% — es el que corresponde reforzar en `favoritos`
para completar lo que falta —, pero conviene tenerlos presentes en la
demo porque es donde se ve *por qué* está hecho así, no solo *cómo*.

### 1. Configurar un proyecto Spring Boot con Maven

Maven administra el ciclo de vida del proyecto (compilar, testear,
empaquetar) y las dependencias de forma declarativa (`pom.xml`) en vez de
manejar `.jar` a mano. Spring Boot suma **starters**: paquetes de
dependencias que ya vienen probadas para funcionar juntas (por ejemplo
`spring-boot-starter-webmvc` trae Spring MVC + Tomcat embebido + Jackson,
todo en versiones compatibles entre sí) y **auto-configuración**: si
detecta el starter de web en el classpath, configura solo un
`DispatcherServlet` y un servidor embebido sin que haya que escribir XML.
Esto es "convención sobre configuración": funciona con cero configuración
manual, y se sobreescribe puntualmente solo donde hace falta (como
`RestClientConfig`, que sí necesitamos nosotros).

### 2. Arquitectura en capas (Controller → Service → Repository)

La idea de fondo es **separación de responsabilidades**: cada capa hace
una sola cosa y depende de una *interfaz*, no de una implementación
concreta, de la capa de abajo (principio de inversión de dependencias).
Eso trae dos beneficios concretos:
- Se puede testear cada capa aislada (mockeando la interfaz de la capa de
  abajo, sin levantar toda la app).
- Se puede cambiar una implementación sin tocar las demás capas — por
  ejemplo, cambiar de DummyJSON a otro proveedor solo tocaría
  `DummyJsonClient`, ni el `Service` ni el `Controller` se enterarían.

En este módulo el rol de "Repository" (acceso a datos) lo cumple
`DummyJsonClient` — no hay base de datos todavía, pero el principio es el
mismo: es la única capa que sabe *de dónde* vienen los datos.

### 3. Consumir un servicio web externo desde el backend

Esto es, en esencia, el patrón **anti-corruption layer** (así se lo conoce
en Domain-Driven Design): una capa fina que aísla el "vocabulario" de un
sistema externo para que no se filtre al resto de la aplicación. Los
problemas típicos a resolver son siempre los mismos, sin importar qué API
externa sea: cómo hacer la llamada HTTP, cómo deserializar la respuesta, y
—el que más se subestima— **qué hacer cuando el servicio externo falla**
(no responde, tarda demasiado, devuelve un error). Ignorar el tercer punto
es la causa más común de que una falla ajena tire abajo tu propia API.

### 4. Diseñar una API RESTful propia, con DTOs desacoplados del modelo interno

Un **DTO** (Data Transfer Object) es un objeto cuyo único propósito es
transportar datos por la red — no tiene lógica de negocio. Desacoplarlo
del modelo interno (en este caso, del modelo externo de DummyJSON) protege
en las dos direcciones: si DummyJSON cambia un campo, no rompe el contrato
que le prometimos a nuestros clientes; y si nosotros queremos cambiar
cómo mostramos algo, no dependemos de que el proveedor externo lo permita.
Además de RESTful implica usar los verbos HTTP con su semántica real
(`GET` no modifica nada, es cacheable e idempotente) y URIs que
identifiquen recursos (`/api/productos/{id}`), no acciones
(`/api/getProducto?id=`).

### 5. Aplicar validación de datos de entrada

Este módulo es de **solo lectura** (`GET`), así que no hay ningún
`@RequestBody` que validar — por eso no se ve un ejemplo funcionando acá.
Lo que sí está listo es la infraestructura: la dependencia
`spring-boot-starter-validation` y el handler de
`MethodArgumentNotValidException` en `GlobalExceptionHandler` (Paso 8).
Bean Validation (JSR 380) permite declarar restricciones como anotaciones
sobre el DTO (`@NotNull`, `@NotBlank`) en vez de si-encadenados a mano, y
Spring las evalúa automáticamente en el borde de la aplicación (antes de
que el controller llegue a ejecutar una sola línea) cuando el parámetro
tiene `@Valid`. El ejemplo funcionando está en
[tp1-guia-favoritos.md](tp1-guia-favoritos.md), que sí tiene un `POST`/`PUT`
con body.

### 6. Implementar manejo uniforme de errores

Sin un manejador centralizado, cada controller terminaría con su propio
`try/catch` armando un JSON de error distinto — inconsistente para quien
consume la API. `ProblemDetail` no es un formato inventado para este TP:
es la representación en Java de **RFC 7807** ("Problem Details for HTTP
APIs"), un estándar para que las respuestas de error tengan una forma
predecible (`status`, `title`, `detail`, y extensiones propias como
`errores`). Centralizarlo en un `@RestControllerAdvice` significa que
agregar un recurso nuevo (favoritos) no requiere volver a escribir el
manejo de errores — se reusa tal cual.

### 7. Documentar la API con Swagger/OpenAPI

OpenAPI es una especificación (un JSON/YAML con un schema definido) que
describe una API HTTP de forma independiente del lenguaje — cualquier
herramienta que entienda OpenAPI puede generar clientes, mocks o
documentación a partir de ese archivo. La ventaja de que `springdoc` lo
**genere a partir del código** (leyendo los `@RestController` reales) en
vez de escribirlo a mano es que no se puede desincronizar de la
implementación real: si el controller cambia, la documentación cambia
sola. Swagger UI es solo una de las tantas herramientas que saben renderizar
ese JSON como una página interactiva.

---

## Paso 1 — Configurar el `RestClient` (`config/RestClientConfig`)

**Qué hacemos:** darle a Spring un bean de `RestClient` ya apuntado a la
URL base de la API externa, para no repetir la URL completa en cada
llamada y para poder inyectarlo donde haga falta.

**Por qué acá y no en el cliente directamente:** separar la *configuración*
(cuál es la URL base, qué timeouts tiene) de la *lógica de las llamadas*
(qué endpoint pego, cómo interpreto la respuesta). Si mañana cambia la URL
o hay que agregar un header común a todas las llamadas, se toca un solo
lugar.

```java
package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient dummyJsonRestClient(@Value("${app.dummyjson.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
```

`app.dummyjson.base-url` vive en `application.properties`:

```properties
app.dummyjson.base-url=https://dummyjson.com
```

Usar una property en vez de un literal hardcodeado permite, por ejemplo,
apuntar a otra URL en tests sin tocar código.

## Paso 2 — Configurar Swagger (`config/OpenApiConfig`)

**Qué hacemos:** un único `@Bean` con la metadata general que va a
aparecer arriba de todo en Swagger UI (título, descripción, versión). No
documenta ningún endpoint puntual — eso lo hacen las anotaciones en cada
controller (Paso 6).

```java
package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI demoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TP1 · Catálogo y Favoritos")
                        .description("Catálogo de productos (consumo de una API externa) + favoritos "
                                + "(CRUD propio en memoria). Práctico de introducción a Spring Boot.")
                        .version("v1"));
    }
}
```

Con solo esto y la dependencia `springdoc-openapi-starter-webmvc-ui` en el
`pom.xml`, ya responden `/v3/api-docs` y `/swagger-ui/index.html` — sin
haber escrito ningún controller todavía.

## Paso 3 — Describir el JSON externo (`client/dummyjson/`)

**Qué hacemos:** dos `record` que calcan **exactamente** los campos que
devuelve `https://dummyjson.com/products` — ni un nombre de campo
inventado. Sirven solo para que Jackson deserialice la respuesta.

**Por qué separado del contrato propio:** si mañana DummyJSON cambia un
nombre de campo, o cambiamos de proveedor externo, el daño queda contenido
acá — nada fuera de este paquete conoce estos nombres.

```java
package com.example.demo.client.dummyjson;

public record DummyJsonProducto(
        Long id,
        String title,
        String description,
        String category,
        String brand,
        double price,
        double discountPercentage,
        int stock,
        double rating,
        String thumbnail
) {
}
```

```java
package com.example.demo.client.dummyjson;

import java.util.List;

public record DummyJsonProductosResponse(
        List<DummyJsonProducto> products,
        int total,
        int skip,
        int limit
) {
}
```

Los nombres de los records/campos son un calco a propósito del JSON real
de DummyJSON — conviene mostrar el JSON crudo (`curl https://dummyjson.com/products/1`)
al lado de este código para que se vea que no hay magia, es una
transcripción directa.

## Paso 4 — El cliente HTTP (`client/dummyjson/DummyJsonClient`)

**Qué hacemos:** la clase que efectivamente llama a DummyJSON usando el
`RestClient` del Paso 1, y traduce **cualquier** falla (HTTP 4xx/5xx,
timeout, lo que sea) a una excepción propia del dominio, para que el resto
de la app nunca vea una excepción de `RestClient`.

Antes de escribir esto hace falta tener las dos excepciones propias:

```java
package com.example.demo.exception;

public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
```

```java
package com.example.demo.exception;

public class ServicioExternoException extends RuntimeException {
    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
```

Y ahora el cliente:

```java
package com.example.demo.client.dummyjson;

import com.example.demo.exception.RecursoNoEncontradoException;
import com.example.demo.exception.ServicioExternoException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DummyJsonClient {

    private final RestClient restClient;

    public DummyJsonClient(RestClient dummyJsonRestClient) {
        this.restClient = dummyJsonRestClient;
    }

    public DummyJsonProductosResponse listar(int limit, int skip) {
        try {
            return restClient.get()
                    .uri("/products?limit={limit}&skip={skip}", limit, skip)
                    .retrieve()
                    .body(DummyJsonProductosResponse.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new ServicioExternoException("DummyJSON respondió con error al listar productos", e);
        } catch (ResourceAccessException e) {
            throw new ServicioExternoException("No se pudo contactar a DummyJSON (timeout o caída del servicio)", e);
        } catch (RestClientException e) {
            throw new ServicioExternoException("Error inesperado al consumir DummyJSON", e);
        }
    }

    public DummyJsonProducto obtenerPorId(Long id) {
        try {
            return restClient.get()
                    .uri("/products/{id}", id)
                    .retrieve()
                    .body(DummyJsonProducto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new RecursoNoEncontradoException("No existe el producto con id " + id);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new ServicioExternoException("DummyJSON respondió con error al buscar el producto " + id, e);
        } catch (ResourceAccessException e) {
            throw new ServicioExternoException("No se pudo contactar a DummyJSON (timeout o caída del servicio)", e);
        } catch (RestClientException e) {
            throw new ServicioExternoException("Error inesperado al consumir DummyJSON", e);
        }
    }
}
```

**Por qué 4 catch distintos y no uno solo `catch (Exception e)`:**
- `HttpClientErrorException.NotFound` es un caso especial de `HttpClientErrorException` → hay que capturarlo **antes** (Java elige el primer catch que matchea, de arriba hacia abajo).
- `HttpClientErrorException | HttpServerErrorException` cubre cualquier otro 4xx/5xx que devuelva DummyJSON.
- `ResourceAccessException` es lo que tira `RestClient` cuando ni siquiera hay respuesta (timeout, DNS, conexión rechazada) — no es un código HTTP, es una falla de red.
- El último `catch (RestClientException e)` es una red de seguridad por si aparece algún otro caso de la jerarquía que no contemplamos explícitamente.

## Paso 5 — El contrato propio (`dto/producto/`)

**Qué hacemos:** el DTO que va a ver el cliente de *nuestra* API — con
nuestros propios nombres de campo, eligiendo qué mostrar y qué no (por
ejemplo, no exponemos `sku`, `warrantyInformation` ni `returnPolicy`, que
sí vienen en el JSON de DummyJSON).

```java
package com.example.demo.dto.producto;

public record ProductoDTO(
        Long id,
        String nombre,
        String descripcion,
        String categoria,
        String marca,
        double precio,
        double descuentoPorcentaje,
        int stock,
        double calificacion,
        String imagenUrl
) {
}
```

Y el contrato de paginación propio — reusa `limit`/`skip` como parámetros
de entrada, pero la forma de la respuesta es nuestra:

```java
package com.example.demo.dto.producto;

import java.util.List;

public record ProductoPageResponse(
        List<ProductoDTO> productos,
        int total,
        int limit,
        int skip
) {
}
```

## Paso 6 — El service (`service/ProductoService` + `ProductoServiceImpl`)

**Qué hacemos:** la interfaz declara el contrato de negocio (lo que el
controller necesita); la implementación llama al cliente y mapea cada
`DummyJsonProducto` a `ProductoDTO`.

```java
package com.example.demo.service;

import com.example.demo.dto.producto.ProductoDTO;
import com.example.demo.dto.producto.ProductoPageResponse;

public interface ProductoService {
    ProductoPageResponse listar(int limit, int skip);
    ProductoDTO obtenerPorId(Long id);
}
```

```java
package com.example.demo.service;

import com.example.demo.client.dummyjson.DummyJsonClient;
import com.example.demo.client.dummyjson.DummyJsonProducto;
import com.example.demo.client.dummyjson.DummyJsonProductosResponse;
import com.example.demo.dto.producto.ProductoDTO;
import com.example.demo.dto.producto.ProductoPageResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoServiceImpl implements ProductoService {

    private final DummyJsonClient dummyJsonClient;

    public ProductoServiceImpl(DummyJsonClient dummyJsonClient) {
        this.dummyJsonClient = dummyJsonClient;
    }

    @Override
    public ProductoPageResponse listar(int limit, int skip) {
        DummyJsonProductosResponse respuesta = dummyJsonClient.listar(limit, skip);
        List<ProductoDTO> productos = respuesta.products().stream()
                .map(this::aProductoDTO)
                .toList();
        return new ProductoPageResponse(productos, respuesta.total(), respuesta.limit(), respuesta.skip());
    }

    @Override
    public ProductoDTO obtenerPorId(Long id) {
        DummyJsonProducto producto = dummyJsonClient.obtenerPorId(id);
        return aProductoDTO(producto);
    }

    private ProductoDTO aProductoDTO(DummyJsonProducto externo) {
        return new ProductoDTO(
                externo.id(),
                externo.title(),
                externo.description(),
                externo.category(),
                externo.brand(),
                externo.price(),
                externo.discountPercentage(),
                externo.stock(),
                externo.rating(),
                externo.thumbnail()
        );
    }
}
```

`aProductoDTO` es el único lugar de todo el proyecto donde conviven los
dos vocabularios (el de DummyJSON y el propio) — por eso es `private`, no
tiene sentido que nadie más lo llame.

## Paso 7 — El controller (`controller/ProductoController`)

**Qué hacemos:** exponer los dos endpoints de solo lectura, con
`@Tag`/`@Operation` para que Swagger los agrupe y describa.

```java
package com.example.demo.controller;

import com.example.demo.dto.producto.ProductoDTO;
import com.example.demo.dto.producto.ProductoPageResponse;
import com.example.demo.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productos")
@Tag(name = "productos", description = "Catálogo de productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
            summary = "Listar productos",
            description = "Devuelve una página del catálogo externo mapeada a nuestro propio contrato. "
                    + "limit/skip se reenvían a DummyJSON tal como llegan."
    )
    public ProductoPageResponse listar(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int skip) {
        return service.listar(limit, skip);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener un producto por id",
            description = "Busca un producto puntual en el catálogo externo. Responde 404 si no existe."
    )
    public ProductoDTO obtener(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }
}
```

Notar que el controller **no tiene ningún `try/catch`** — si
`ProductoService` tira `RecursoNoEncontradoException` o
`ServicioExternoException`, sube tal cual hasta el `GlobalExceptionHandler`
del paso siguiente.

## Paso 8 — Manejo uniforme de errores (`exception/GlobalExceptionHandler`)

**Qué hacemos:** un único `@RestControllerAdvice` para toda la API, que
convierte cualquier excepción en una respuesta `ProblemDetail` consistente
(RFC 7807), en vez de que cada controller arme su propio JSON de error.

```java
package com.example.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail handleNoEncontrado(RecursoNoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ProblemDetail handleServicioExterno(ServicioExternoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        problema.setTitle("Falla al consumir un servicio externo");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Uno o más campos no son válidos");
        problema.setTitle("Error de validación");
        problema.setProperty("errores", errores);
        return problema;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenerico(Exception ex) {
        // Único caso donde se loguea la excepción completa: es un error no
        // anticipado, y sin esto el detalle real se pierde (el cliente solo
        // debe ver un mensaje genérico, nunca un stack trace).
        log.error("Error inesperado no manejado", ex);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado");
        problema.setTitle("Error interno");
        return problema;
    }
}
```

**Punto para señalar en clase:** el handler genérico es el único que
loguea — a `RecursoNoEncontradoException` y `ServicioExternoException` no
hace falta ponerles logging porque ya son casos esperados y bien
identificados (se sabe exactamente qué pasó); loguear ahí sería ruido. El
`Exception.class` es la red de seguridad para lo que **no** se anticipó, y
ahí sí hace falta la traza completa — si no se loguea, un bug real
queda invisible (nos pasó armando la guía de favoritos: un 500 sin ningún
rastro en el log hasta que se agregó este `log.error`).

El handler de `MethodArgumentNotValidException` todavía no lo dispara nada
de productos (no hay `@Valid` en este módulo, es de solo lectura) — se va
a activar recién cuando favoritos tenga un `@RequestBody @Valid`.

---

## Cómo probarlo (resultados reales, ya verificados)

```
curl "http://localhost:8080/api/productos?limit=2"
```
```json
{"productos":[{"id":1,"nombre":"Essence Mascara Lash Princess", ...}],"total":194,"limit":2,"skip":0}
```

```
curl "http://localhost:8080/api/productos/1"
```
```json
{"id":1,"nombre":"Essence Mascara Lash Princess","precio":9.99, ...}
```

```
curl -i "http://localhost:8080/api/productos/999999"
```
```json
{"status":404,"title":"Not Found","detail":"No existe el producto con id 999999","instance":"/api/productos/999999"}
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html` — el grupo
`productos` aparece con las dos operaciones documentadas.

## Ideas para resaltar en la demo

1. Comentar la dependencia de springdoc en el `pom.xml`, mostrar que
   Swagger deja de responder, y volverla a poner — para que quede claro
   que la anotación no "activa" nada, solo documenta lo que ya existe.
2. Mostrar el JSON crudo de `https://dummyjson.com/products/1` al lado de
   `GET /api/productos/1`, campo por campo, para que se vea el desacople.
3. Provocar el 404 (id inexistente) y el 502 (cortar la red o apuntar
   `app.dummyjson.base-url` a algo que no resuelva) y mostrar que la forma
   de la respuesta es la misma (`ProblemDetail`) en los dos casos.
