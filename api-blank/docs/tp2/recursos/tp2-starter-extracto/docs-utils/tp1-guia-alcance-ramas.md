# Guía TP1 — Alcance de ramas: qué hay en `main` y qué hay en `develop`

Notas internas de la cátedra sobre cómo se repartió el contenido del TP1
entre las dos ramas. No es para repartir a los alumnos tal cual (aunque no
hay ningún problema si lo ven) — es para no perder el criterio con el que
se armó cada una.

## La idea general

- **`main`** es lo único que los alumnos ven. Tiene el setup del proyecto y
  toda la **configuración / infraestructura transversal** que no depende de
  cómo resuelva cada uno las consignas de diseño. Es "andamiaje", no
  "solución".
- **`develop`** es la rama de trabajo de la cátedra: tiene además la
  **lógica de negocio ya resuelta** (el módulo de productos completo, y a
  futuro favoritos) para mostrar en clase o usar de referencia al corregir.
  **No se pushea** — vive solo en la máquina de quien la usa. Lo que esté
  listo para que los alumnos lo vean se promueve a `main` con un PR.

El criterio para decidir qué va en cada lado fue: **si resolverlo es el
objetivo de aprendizaje de alguna consigna, va solo en `develop`. Si es
plomería que no aporta aprendizaje por sí misma, va en `main`.**

## Qué hay en `main`

| Archivo | Por qué está |
|---|---|
| `pom.xml` (deps: webmvc, validation, springdoc) | Consigna 1 pide agregarlas explícitamente |
| `DemoApplication`, `HealthController`, `PingController` | Setup base + "un endpoint de prueba responde" (consigna 1.3) |
| `config/RestClientConfig` | Es un `@Bean` de configuración, no la lógica de consumo en sí |
| `config/OpenApiConfig` | Metadata de Swagger, no la documentación puntual de cada endpoint |
| `client/dummyjson/DummyJsonProducto` y `DummyJsonProductosResponse` | Es la forma del JSON externo (dato, no lógica) — les ahorra tipear el shape para que se concentren en consumirlo |
| `exception/GlobalExceptionHandler` + `RecursoNoEncontradoException` + `ServicioExternoException` | Transversal a toda la API, no es de un recurso en particular — no tiene sentido que cada uno reinvente el mismo `@ControllerAdvice` |

## Qué hay SOLO en `develop` (todavía no en `main`)

| Archivo | Por qué no está en `main` todavía |
|---|---|
| `client/dummyjson/DummyJsonClient` | Es literalmente lo que la consigna 2.1 pide investigar (`RestClient`, manejo de `HttpClientErrorException`/timeouts) |
| `dto/producto/ProductoDTO`, `ProductoPageResponse` | Es el diseño del contrato propio — consigna 2.2, decisión de cada uno |
| `service/ProductoService(Impl)` | Lógica de negocio (mapeo externo → propio) |
| `controller/ProductoController` | Expone la API propia — consigna 2.3/2.4 |
| Todo `favoritos` (dominio, repository, DTOs, service, controller) | Ninguna parte de este recurso está armada — es 100% de diseño propio (consignas 3 a 7) |

Ver [tp1-guia-catalogo-productos.md](tp1-guia-catalogo-productos.md) para
el detalle de cómo quedó armado el módulo de productos, y
[tp1-guia-favoritos.md](tp1-guia-favoritos.md) para la guía de clase de
favoritos.

## Cómo se promueve contenido de `develop` a `main`

Cuando algo de `develop` esté listo para que los alumnos lo vean:

```
git push origin main       # si todavía no se pusheó nunca
git push origin develop    # recién ahí GitHub ofrece "Compare & pull request"
```

y se abre el PR `develop → main` desde GitHub.

**Importante:** `develop` se construyó como `main` + commits que *agregan*
archivos (nunca como una rama separada desde el mismo punto). Esto es a
propósito: si `develop` fuera una rama hermana que nunca tocó los archivos
que `main` borró, un merge futuro respetaría el borrado de `main` y no
traería nada de vuelta (git ve "un lado borró, el otro no cambió nada" →
gana el borrado). Al ser aditiva, el merge trae los archivos nuevos sin
ambigüedad. **Cualquier archivo nuevo que se sume en `develop` de acá en
más tiene que ser realmente nuevo**, no una versión modificada de algo que
ya existe en `main`, para que esto se mantenga así de limpio.
