# Guía TP1 — Armar Favoritos en clase (con código verificado)

**Esto es material de preparación, no algo para pegar en el repo del TP.**
Todo el código de acá se escribió y se probó end-to-end (compiló, se
corrió, se le pegó con `curl`) para tener la certeza de que funciona antes
de mostrarlo — pero después se sacó de `src/`. El proyecto real
(`develop`) sigue sin nada de favoritos, a propósito: la idea es tipearlo
en vivo en clase, usando esto como referencia propia, no como algo para
que los alumnos copien.

El patrón es el mismo que ya está resuelto en `productos` — ver
[tp1-guia-catalogo-productos.md](tp1-guia-catalogo-productos.md) si hace
falta repasarlo. Infraestructura que **ya existe** y no hay que volver a
armar: `GlobalExceptionHandler`, `RecursoNoEncontradoException`,
`ServicioExternoException`.

---

## Paso 0 — Encuadrar el recurso (para plantear antes de tipear)

- ¿Qué es un "favorito" acá? Una referencia a un producto externo + una
  nota personal — no una copia del producto.
- ¿Por qué NO guardar nombre/precio/imagen del producto adentro de
  `Favorito`? Porque esos datos ya viven en otro lado (`ProductoService`)
  y duplicarlos los deja desactualizados si el producto cambia. Solo se
  guarda `productoId`.

## Paso 1 — Dominio (`domain/Favorito`)

**Qué hacemos:** el modelo de dominio, igual de simple que `Tarea` del
práctico anterior — un `record` inmutable.

```java
package com.example.demo.domain;

import java.time.LocalDateTime;

public record Favorito(
        Long id,
        Long productoId,
        String nota,
        LocalDateTime fechaAgregado
) {
}
```

## Paso 2 — Repository en memoria (`repository/FavoritoRepository`)

**Qué hacemos:** la interfaz primero (el contrato que va a usar el
service), después la implementación con un `Map`.

```java
package com.example.demo.repository;

import com.example.demo.domain.Favorito;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository {
    List<Favorito> findAll();
    Optional<Favorito> findById(Long id);
    Favorito save(Favorito favorito);
    void deleteById(Long id);
    boolean existsById(Long id);
}
```

```java
package com.example.demo.repository;

import com.example.demo.domain.Favorito;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryFavoritoRepository implements FavoritoRepository {

    private final Map<Long, Favorito> datos = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public List<Favorito> findAll() {
        return new ArrayList<>(datos.values());
    }

    @Override
    public Optional<Favorito> findById(Long id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public Favorito save(Favorito favorito) {
        Long id = favorito.id() != null ? favorito.id() : nextId.getAndIncrement();
        Favorito guardado = new Favorito(id, favorito.productoId(), favorito.nota(), favorito.fechaAgregado());
        datos.put(id, guardado);
        return guardado;
    }

    @Override
    public void deleteById(Long id) {
        datos.remove(id);
    }

    @Override
    public boolean existsById(Long id) {
        return datos.containsKey(id);
    }
}
```

**Por qué `ConcurrentHashMap` y `AtomicLong`:** Tomcat atiende requests en
paralelo, en threads distintos. Un `HashMap` normal puede corromperse con
escrituras concurrentes; un contador `long++` común puede generar el mismo
id dos veces si dos requests llegan al mismo tiempo.

**Pregunta para instalar el gancho al TP2:** *¿qué pasa con estos favoritos
si reinicio la aplicación?* → se pierden, porque viven en memoria del
proceso. Ahí entra JPA + base de datos real.

## Paso 3 — DTOs (`dto/favorito/`)

**Qué hacemos:** un DTO de entrada con las anotaciones de validación, y
uno de salida sin ellas (no tiene sentido validar lo que la API devuelve).

```java
package com.example.demo.dto.favorito;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FavoritoRequest(
        @NotNull(message = "productoId es obligatorio")
        Long productoId,

        @NotBlank(message = "nota no puede estar vacía")
        String nota
) {
}
```

```java
package com.example.demo.dto.favorito;

import java.time.LocalDateTime;

public record FavoritoResponse(
        Long id,
        Long productoId,
        String nota,
        LocalDateTime fechaAgregado
) {
}
```

`@NotNull` vs `@NotBlank`: `productoId` es un `Long` — puede venir `null`,
pero no tiene un "vacío" (no aplica `@NotBlank` a un número). `nota` es
`String` — `@NotBlank` cubre `null`, `""` y `"   "` en un solo chequeo.

## Paso 4 — Service (`service/FavoritoService` + `FavoritoServiceImpl`)

**Qué hacemos:** la interfaz declara las 5 operaciones del CRUD en
términos de DTOs (nunca expone `Favorito` hacia afuera); la
implementación hace el mapeo entidad ↔ DTO y decide cuándo lanzar
`RecursoNoEncontradoException`.

```java
package com.example.demo.service;

import com.example.demo.dto.favorito.FavoritoRequest;
import com.example.demo.dto.favorito.FavoritoResponse;

import java.util.List;

public interface FavoritoService {
    List<FavoritoResponse> listar();
    FavoritoResponse obtener(Long id);
    FavoritoResponse crear(FavoritoRequest request);
    FavoritoResponse actualizar(Long id, FavoritoRequest request);
    void eliminar(Long id);
}
```

```java
package com.example.demo.service;

import com.example.demo.domain.Favorito;
import com.example.demo.dto.favorito.FavoritoRequest;
import com.example.demo.dto.favorito.FavoritoResponse;
import com.example.demo.exception.RecursoNoEncontradoException;
import com.example.demo.repository.FavoritoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FavoritoServiceImpl implements FavoritoService {

    private final FavoritoRepository repository;

    public FavoritoServiceImpl(FavoritoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<FavoritoResponse> listar() {
        return repository.findAll().stream()
                .map(this::aResponse)
                .toList();
    }

    @Override
    public FavoritoResponse obtener(Long id) {
        return aResponse(buscarOFallar(id));
    }

    @Override
    public FavoritoResponse crear(FavoritoRequest request) {
        Favorito nuevo = new Favorito(null, request.productoId(), request.nota(), LocalDateTime.now());
        return aResponse(repository.save(nuevo));
    }

    @Override
    public FavoritoResponse actualizar(Long id, FavoritoRequest request) {
        Favorito existente = buscarOFallar(id);
        Favorito actualizado = new Favorito(
                existente.id(),
                request.productoId(),
                request.nota(),
                existente.fechaAgregado()   // no se pisa la fecha original al actualizar
        );
        return aResponse(repository.save(actualizado));
    }

    @Override
    public void eliminar(Long id) {
        buscarOFallar(id);   // si no existe, tira 404 antes de intentar borrar
        repository.deleteById(id);
    }

    private Favorito buscarOFallar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el favorito con id " + id));
    }

    private FavoritoResponse aResponse(Favorito favorito) {
        return new FavoritoResponse(favorito.id(), favorito.productoId(), favorito.nota(), favorito.fechaAgregado());
    }
}
```

**Punto para señalar en clase:** `buscarOFallar` es un solo lugar para la
regla "si no existe, 404" — la usan `obtener`, `actualizar` y `eliminar`
por igual. Sin este método privado, esa misma línea de
`orElseThrow(...)` se repetiría tres veces.

## Paso 5 — Controller (`controller/FavoritoController`)

**Qué hacemos:** los 5 endpoints, con el código de estado correcto en
cada uno según la tabla de la consigna, y `@Valid` en los dos que reciben
body.

```java
package com.example.demo.controller;

import com.example.demo.dto.favorito.FavoritoRequest;
import com.example.demo.dto.favorito.FavoritoResponse;
import com.example.demo.service.FavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/favoritos")
@Tag(name = "favoritos", description = "Favoritos del usuario sobre el catálogo de productos (CRUD en memoria)")
public class FavoritoController {

    private final FavoritoService service;

    public FavoritoController(FavoritoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar favoritos")
    public List<FavoritoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un favorito por id", description = "Responde 404 si no existe.")
    public FavoritoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear un favorito")
    public ResponseEntity<FavoritoResponse> crear(@Valid @RequestBody FavoritoRequest request) {
        FavoritoResponse creado = service.crear(request);
        return ResponseEntity.created(URI.create("/api/favoritos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un favorito", description = "Responde 404 si no existe.")
    public FavoritoResponse actualizar(@PathVariable Long id, @Valid @RequestBody FavoritoRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un favorito", description = "Responde 404 si no existe.")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Por qué `crear` devuelve `ResponseEntity<FavoritoResponse>` pero
`obtener`/`actualizar` devuelven `FavoritoResponse` directo:** cuando hace
falta controlar algo más que el body (acá, el header `Location` con la
URL del recurso creado y el status `201`), se necesita `ResponseEntity`.
Cuando el status es siempre el mismo (`200` por defecto), devolver el DTO
directo es más simple y Spring arma el `200 OK` solo.

---

## Cómo se probó (resultados reales)

```
POST /api/favoritos
Body: {"productoId": 1, "nota": "Regalo para mamá"}
```
```
HTTP/1.1 201
Location: /api/favoritos/1
{"id":1,"productoId":1,"nota":"Regalo para mamá","fechaAgregado":"2026-09-02T19:17:30.389..."}
```

```
GET /api/favoritos
```
```json
[{"id":1,"productoId":1,"nota":"Regalo para mama", ...}, {"id":2, ...}]
```

```
PUT /api/favoritos/1
Body: {"productoId": 1, "nota": "Nota actualizada"}
```
```
HTTP/1.1 200
{"id":1,"productoId":1,"nota":"Nota actualizada","fechaAgregado":"2026-09-02T19:17:20.71..."}
```
(la fecha original se conserva — confirma que `buscarOFallar` +
`existente.fechaAgregado()` funciona)

```
DELETE /api/favoritos/1
```
```
HTTP/1.1 204 No Content
```

```
GET /api/favoritos/1     (después de borrarlo)
```
```
HTTP/1.1 404
{"detail":"No existe el favorito con id 1","status":404,"title":"Not Found"}
```

```
POST /api/favoritos
Body: {"productoId": null, "nota": ""}
```
```
HTTP/1.1 400
{
  "detail": "Uno o más campos no son válidos",
  "status": 400,
  "title": "Error de validación",
  "errores": {
    "productoId": "productoId es obligatorio",
    "nota": "nota no puede estar vacía"
  }
}
```

```
PUT /api/favoritos/999          (id inexistente)
DELETE /api/favoritos/999       (id inexistente)
```
```
HTTP/1.1 404   (los dos, mismo ProblemDetail que el GET)
```

Y en `/v3/api-docs`, el array de `tags` pasó de `["productos"]` a
`["productos", "favoritos"]` apenas se agregó el `@Tag` del controller —
sin reiniciar nada más que la app.

## Errores comunes a anticipar (para no perder tiempo en vivo)

- Poner `@Valid` en el DTO en vez de en el parámetro `@RequestBody` del
  controller — ahí no dispara ninguna validación.
- Devolver `200` en el `POST` en vez de `201` — hay que usar
  `ResponseEntity.created(...)`, no alcanza con anotar el método.
- Pisar `fechaAgregado` con `LocalDateTime.now()` al actualizar en vez de
  conservar `existente.fechaAgregado()` — el favorito "cambia de fecha de
  creación" cada vez que se edita, que no es lo esperado.
- Devolver `Favorito` (entidad) en vez de `FavoritoResponse` desde el
  controller — expone el modelo interno igual que se explicó que NO había
  que hacer con `productos`.
- Un detalle de encoding si se prueba con `curl` en Windows: si el body
  tiene tildes y se pasa como string literal en la consola, a veces se
  corrompe el UTF-8 y Jackson tira un error de parseo que parece un bug de
  la app pero es del terminal. Si pasa, probar mandando el JSON desde un
  archivo (`--data-binary @archivo.json`) o directo desde Postman/Insomnia.
