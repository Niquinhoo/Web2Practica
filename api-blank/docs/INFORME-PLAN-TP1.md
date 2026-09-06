# Informe previo de implementación · TP1

## Estado del informe

**Actualización del 6 de septiembre de 2026:** implementación autorizada por el usuario y realizada en la rama `TP1`, partiendo del proyecto base. Este informe se conserva como planificación histórica; el contrato definitivo está en el README y los resultados en `EVIDENCIA-TP1.md`. Se adoptaron las decisiones base de la sección 19, con `Instant`, reloj inyectable y pruebas adicionales del cliente contra un servidor HTTP local. El repositorio separa crear/actualizar/eliminar para resolver las modificaciones atómicamente. Las referencias siguientes a trabajo pendiente describen el estado previo a esta implementación.

Este documento reúne el análisis de la consigna del TP1 y del material teórico disponible. Su objetivo es servir como guía de revisión antes de implementar.

En esta etapa el proyecto no implementa todavía el TP1. Solo contiene el proyecto base de Spring Boot, el endpoint `/health` y sus pruebas iniciales. La implementación y el push a GitHub quedan pendientes de aprobación de este informe.

## 1. Fuentes y alcance

Se revisaron los siguientes materiales del workspace:

- `Web2Mach/docs/practicos/01-api-rest.md` — consigna del TP1.
- `Web2Mach/docs/teoria/01-backend-http-apis.md` — fundamentos de HTTP, REST, contratos, validación y errores.
- `Web2Mach/docs/teoria/unidad-1/01-poo-solid.md` — POO, encapsulación, abstracción, polimorfismo y SOLID.
- `Web2Mach/docs/teoria/unidad-1/02-spring-boot.md` — Spring Boot, arquitectura Controller-Service-Repository, DTO, validación e inyección de dependencias.
- `Web2Mach/docs/teoria/unidad-1/03-swagger-openapi.md` — documentación con springdoc, Swagger UI y OpenAPI.
- `Web2Practica/api-blank` — proyecto base que se continuará para el TP1.

El informe se limita al alcance de la consigna: Spring Boot, API REST, consumo de DummyJSON, favoritos en memoria, validación, errores y documentación. JPA, PostgreSQL, Flyway, seguridad y despliegue quedan fuera de este trabajo.

## 2. Objetivo del TP1

El trabajo busca construir una API con dos tipos de frontera:

1. Un catálogo de productos de solo lectura, obtenido desde un servicio externo.
2. Un recurso propio de favoritos, con CRUD completo y almacenamiento en memoria.

El trabajo también exige:

- configurar Spring Boot con Maven;
- separar responsabilidades en capas;
- definir DTOs propios;
- validar los datos de entrada;
- devolver códigos HTTP correctos;
- centralizar el manejo de errores;
- documentar la API con Swagger/OpenAPI;
- incluir README y evidencia de pruebas.

La consigna no exige específicamente utilizar Mockito. Los mocks son una técnica de pruebas que ayuda a aislar dependencias; no sustituyen la implementación real que debe funcionar en la aplicación.

## 3. Estado inicial del proyecto

El proyecto se encuentra en:

```text
C:\Users\Nicolás\Desktop\Facultad\Web-2\Web2Practica\api-blank
```

Actualmente contiene:

- Java 25 configurado en Maven.
- Spring Boot 4.1.1.
- `spring-boot-starter-webmvc`.
- `spring-boot-starter-test`.
- El endpoint `GET /health`.
- Una prueba de contexto de Spring.
- Una prueba de `HealthController` con `MockMvc`.

La ejecución inicial de `.\mvnw.cmd test` termina correctamente con `BUILD SUCCESS` y dos pruebas ejecutadas.

Para completar el setup del TP1 hay que agregar:

```text
spring-boot-starter-validation
springdoc-openapi-starter-webmvc-ui
```

La guía de Swagger de la cátedra indica usar la línea 3.x de springdoc con Spring Boot 4.1 y muestra la versión 3.1.0.

La dependencia de pruebas existente debe conservarse, porque aporta JUnit, Mockito y las herramientas de Spring Test necesarias para las pruebas.

## 4. Arquitectura propuesta

La aplicación debería seguir este recorrido:

```text
Productos:
Cliente HTTP
    ↓
ProductoController
    ↓
ProductoService
    ↓
ProductoClient, interfaz
    ↓
DummyJsonProductoClient, implementación con RestClient o WebClient
    ↓
DummyJSON

Favoritos:
Cliente HTTP
    ↓
FavoritoController
    ↓
FavoritoService
    ↓
FavoritoRepository, interfaz
    ↓
FavoritoRepositoryMemoria
    ↓
Colección en memoria
```

### Responsabilidad de cada capa

#### Controller

El Controller es la frontera HTTP. Debe encargarse de:

- definir rutas y métodos HTTP;
- leer `@PathVariable`, `@RequestParam` y `@RequestBody`;
- aplicar `@Valid` a los DTOs de entrada;
- devolver la respuesta HTTP apropiada.

No debería contener consultas a DummyJSON, operaciones sobre el `Map`, reglas de negocio ni transformaciones complejas.

#### Service

El Service representa los casos de uso y las reglas de negocio. Debe:

- coordinar las operaciones;
- verificar si un recurso existe;
- decidir qué hacer cuando no existe;
- crear o actualizar favoritos;
- generar la fecha de alta;
- transformar modelos internos o externos a DTOs públicos;
- traducir errores técnicos a excepciones propias.

Debe recibir sus dependencias por constructor.

#### Repository

El Repository de favoritos oculta dónde y cómo se almacenan los datos. En el TP1 la implementación será en memoria. Más adelante podría reemplazarse por una implementación con base de datos sin modificar el Controller ni las reglas principales del Service.

#### Cliente o gateway externo

Los productos no se almacenan en la aplicación: se consultan en DummyJSON. Por eso resulta más claro tener una interfaz `ProductoClient` o `ProductoGateway` que abstraiga la integración externa.

El Service debe depender de la interfaz, no directamente de `RestClient` ni de la URL de DummyJSON. La clase `DummyJsonProductoClient` será el adaptador concreto.

Esta separación aplica el principio de inversión de dependencias y permite reemplazar el servicio externo durante las pruebas.

## 5. Estructura de paquetes sugerida

La clase principal está en `ar.edu.unvime.apiblank`, por lo que las nuevas clases deben estar en ese paquete o debajo de él para que Spring las detecte.

Una estructura posible es:

```text
src/main/java/ar/edu/unvime/apiblank/
├── ApiBlankApplication.java
├── config/
│   ├── OpenApiConfig.java
│   └── RestClientConfig.java          (si resulta necesario)
├── producto/
│   ├── ProductoResponse.java
│   ├── ProductoExterno.java
│   ├── DummyJsonProductosResponse.java
│   ├── ProductoClient.java
│   ├── DummyJsonProductoClient.java
│   ├── ProductoService.java
│   └── ProductoController.java
├── favorito/
│   ├── Favorito.java
│   ├── FavoritoRequest.java
│   ├── FavoritoResponse.java
│   ├── FavoritoRepository.java
│   ├── FavoritoRepositoryMemoria.java
│   ├── FavoritoService.java
│   └── FavoritoController.java
└── error/
    ├── ApiError.java
    ├── RecursoNoEncontradoException.java
    ├── ServicioExternoException.java
    └── ApiExceptionHandler.java
```

Los nombres son orientativos. Lo importante es conservar las responsabilidades y las dependencias, no crear interfaces artificiales para cada clase.

## 6. Contrato de productos

Los únicos endpoints obligatorios de productos son:

| Operación | Método y ruta | Éxito esperado |
|---|---|---:|
| Listar | `GET /api/productos` | `200 OK` |
| Obtener uno | `GET /api/productos/{id}` | `200 OK` |

Los productos son de solo lectura en este TP. No se deben agregar `POST`, `PUT` ni `DELETE` para productos como parte de la implementación básica.

### DTO externo y DTO propio

La respuesta de DummyJSON debe deserializarse en clases internas, por ejemplo `ProductoExterno` y `DummyJsonProductosResponse`. Esas clases no deben salir por el Controller.

La API debe exponer un DTO propio, por ejemplo `ProductoResponse`, con los campos que realmente interese publicar. Puede mapear nombres externos a nombres propios y omitir campos que no sean parte del contrato.

Ejemplo conceptual:

```text
Campo externo: title
Campo público propio: nombre
```

El contrato público no debe quedar atado innecesariamente al JSON de DummyJSON.

### Paginación

La paginación mediante `limit` y `skip` es opcional en el TP1. La recomendación es implementar primero el listado básico y agregar paginación únicamente si se puede definir un contrato propio y documentarlo claramente.

Si se agrega, no conviene devolver el envoltorio externo sin cambios. Se debería decidir si la API devuelve una lista propia o una respuesta propia con elementos, límite, desplazamiento y total.

### Configuración externa

La URL base debería estar en configuración, por ejemplo:

```properties
dummyjson.base-url=https://dummyjson.com
```

También conviene definir timeouts razonables. Un timeout o una respuesta no exitosa de DummyJSON no debe terminar en un error genérico sin explicación ni devolver el JSON externo al cliente.

## 7. Modelo y contrato de favoritos

El modelo de dominio puede tener:

```text
id: Long
productoId: Long
nota: String
fechaAgregado: Instant o OffsetDateTime
```

La fecha debería ser generada por el backend y no recibirse desde el cliente. El `productoId` puede ser suficiente como referencia; no es obligatorio guardar la respuesta completa del producto externo.

El DTO de entrada representa lo que el cliente puede enviar. Una propuesta mínima sería:

```text
productoId: obligatorio y positivo
nota: opcional o obligatoria según la decisión del equipo, con un límite de longitud
```

El DTO de salida representa lo que la API devuelve e incluye los datos que el cliente necesita, sin exponer directamente la estructura interna.

Es posible usar un mismo `FavoritoRequest` para POST y PUT si ambos representan el mismo conjunto completo de datos. En ese caso, `PUT` debe reemplazar los datos del favorito indicado por la ruta y no crear un favorito si el ID no existe.

## 8. Contrato CRUD de favoritos

| Operación | Método y ruta | Código de éxito |
|---|---|---:|
| Crear | `POST /api/favoritos` | `201 Created` |
| Listar | `GET /api/favoritos` | `200 OK` |
| Obtener uno | `GET /api/favoritos/{id}` | `200 OK` |
| Actualizar | `PUT /api/favoritos/{id}` | `200 OK` |
| Eliminar | `DELETE /api/favoritos/{id}` | `204 No Content` |

Consideraciones:

- `POST` crea un recurso nuevo y no debería aceptar el ID como autoridad del cliente.
- `PUT` actualiza el recurso indicado por el ID de la ruta.
- `DELETE` debe responder sin cuerpo.
- Si el favorito no existe, obtenerlo, actualizarlo o eliminarlo debe producir `404 Not Found`.
- No se debe transformar un `PUT` sobre un ID inexistente en una creación silenciosa.
- Los recursos deben expresarse como sustantivos plurales; no usar rutas como `/crearFavorito` o `/obtenerProductos`.

## 9. Repository en memoria

El contrato debería ser pequeño y específico:

```java
public interface FavoritoRepository {
    List<Favorito> buscarTodos();
    Optional<Favorito> buscarPorId(Long id);
    Favorito guardar(Favorito favorito);
    void eliminar(Long id);
}
```

La implementación puede utilizar `ConcurrentHashMap<Long, Favorito>` y `AtomicLong` para generar IDs.

Hay que tener en cuenta que los beans de Spring normalmente son singleton. Por lo tanto, la colección será compartida por distintas solicitudes. La implementación debe:

- evitar estructuras no seguras para acceso concurrente;
- no devolver directamente una colección interna modificable;
- generar IDs únicos;
- evitar estado `static` innecesario;
- cuidar el aislamiento entre pruebas.

Si el orden de los favoritos forma parte del contrato, no se debe depender del orden accidental de `ConcurrentHashMap`; se debe ordenar explícitamente o usar una estructura que exprese ese requisito.

Los datos se perderán al reiniciar la aplicación. Eso es correcto para el TP1 porque todavía no hay persistencia real.

## 10. Validación

La validación debe realizarse en el DTO de entrada y activarse en el Controller:

```java
@Valid @RequestBody FavoritoRequest request
```

Anotaciones posibles:

- `@NotNull` para `productoId`;
- `@Positive` para exigir un ID mayor que cero;
- `@NotBlank` si la nota es obligatoria;
- `@Size` para limitar la longitud de la nota.

La validación de formato y datos de entrada pertenece a la frontera HTTP. Las reglas de negocio que no sean simples restricciones de formato deben permanecer en el Service o en el dominio.

`@Valid` no produce ningún efecto si el DTO no tiene restricciones de Bean Validation.

## 11. Manejo uniforme de errores

Se propone un formato común:

```java
public record ApiError(
        int status,
        String mensaje,
        Map<String, String> campos
) {}
```

El manejador central debe utilizar `@RestControllerAdvice`.

Casos mínimos:

| Caso | Respuesta |
|---|---:|
| Favorito inexistente | `404 Not Found` |
| Validación fallida | `400 Bad Request` |
| DummyJSON caído o con timeout | `502 Bad Gateway` o `503 Service Unavailable` |

`502 Bad Gateway` es una opción natural cuando la aplicación no puede obtener una respuesta válida de un servicio externo. `503 Service Unavailable` también es válido si se quiere expresar indisponibilidad temporal. Hay que elegir una política y documentarla.

Ejemplo de error de validación:

```json
{
  "status": 400,
  "mensaje": "Hay datos inválidos",
  "campos": {
    "productoId": "debe ser mayor que 0",
    "nota": "no debe estar vacía"
  }
}
```

Además de los casos obligatorios, conviene contemplar JSON mal formado, tipos incompatibles, IDs con formato inválido y un error inesperado general. Esto evita que algunos endpoints devuelvan el formato propio y otros el error predeterminado de Spring.

No conviene envolver todo en un `catch (Exception)` dentro de cada Controller. El manejo debe estar centralizado.

## 12. Qué mockear y qué no mockear

### Regla principal

En la aplicación ejecutándose normalmente:

- DummyJSON se consume de verdad.
- `FavoritoRepositoryMemoria` se utiliza de verdad.
- Los DTOs y modelos son objetos reales.
- El mock no se incorpora como reemplazo permanente de la funcionalidad.

Los mocks se utilizan en los tests para controlar colaboradores y evitar dependencias lentas, inestables o externas.

### Tabla de pruebas

| Prueba | Clase que se prueba de verdad | Colaborador simulado |
|---|---|---|
| Service de favoritos | `FavoritoService` | `FavoritoRepository` |
| Repository de favoritos | `FavoritoRepositoryMemoria` | Ninguno |
| Controller de favoritos | Controller y MVC | `FavoritoService` |
| Service de productos | `ProductoService` | `ProductoClient` |
| Controller de productos | Controller y MVC | `ProductoService` |
| Cliente de DummyJSON | `DummyJsonProductoClient` y `RestClient` | Servidor HTTP local simulado |
| Contexto completo | Aplicación Spring | Cliente externo falso o configuración de prueba |

### Service de favoritos

Se debe mockear `FavoritoRepository` para devolver de forma controlada:

- un favorito encontrado;
- un resultado vacío;
- un favorito guardado;
- una respuesta para verificar que `guardar` o `eliminar` fueron invocados.

No se debe mockear el `FavoritoService`, porque es la clase bajo prueba.

### Repository de favoritos

No se debe mockear nada al probar `FavoritoRepositoryMemoria`. Es necesario probar la colección real y su comportamiento.

### Service de productos

Se debe mockear `ProductoClient`. El Service real debe probar:

- la transformación del modelo externo al DTO propio;
- que no filtra directamente el JSON externo;
- el tratamiento de un producto inexistente;
- el tratamiento de una falla o timeout del cliente externo.

### Controllers

Se puede usar `MockMvc` con el Controller real y el Service mockeado. Así se comprueban las rutas, métodos HTTP, cuerpos JSON, validación y códigos de estado sin ejecutar la lógica interna ni llamar a Internet.

### Cliente HTTP externo

Para probar que el adaptador construye correctamente la URL, envía los parámetros y deserializa la respuesta, es preferible un servidor HTTP de prueba como MockWebServer o WireMock.

Mockear directamente el `ProductoClient` sirve para probar el Service, pero no prueba el comportamiento de `RestClient`. Son niveles diferentes.

### Pruebas completas

En una prueba de contexto o de integración:

- el repository de favoritos puede ser el real en memoria;
- el cliente de productos debe sustituirse por un fake, un mock o un servidor HTTP local;
- no conviene depender de Internet para que `mvn test` pase.

No se deben mockear DTOs, entidades, `Map`, `ConcurrentHashMap` ni la clase que se está probando.

## 13. Casos de prueba mínimos

### Productos

- `GET /api/productos` devuelve `200` y una lista con el DTO propio.
- `GET /api/productos/{id}` devuelve `200` para un producto existente.
- Un producto inexistente produce `404` si se define esa traducción para el error externo.
- Un timeout o una caída de DummyJSON produce el `5xx` elegido.
- El Service mapea campos externos a nombres públicos propios.
- Ninguna respuesta pública contiene campos innecesarios del proveedor.

### Favoritos

- Crear un favorito válido devuelve `201`.
- Crear un favorito inválido devuelve `400` y detalla los campos.
- Listar favoritos devuelve `200`.
- Obtener un favorito existente devuelve `200`.
- Obtener un favorito inexistente devuelve `404`.
- Actualizar un favorito existente devuelve `200`.
- Actualizar un ID inexistente devuelve `404`.
- Eliminar un favorito existente devuelve `204` sin cuerpo.
- El repository genera IDs y mantiene correctamente los datos.

La evidencia del TP1 pide por lo menos un caso exitoso y uno de error probado para cada recurso. Conviene automatizar esos casos y además conservar una colección Postman, Insomnia o un archivo `.http`.

## 14. Principios teóricos aplicados

### Responsabilidad única

- Controller: HTTP.
- Service: casos de uso y negocio.
- Repository: almacenamiento.
- Cliente externo: comunicación con DummyJSON.
- DTO: contrato de entrada o salida.
- Advice: traducción uniforme de excepciones a HTTP.

Cada parte tiene una razón de cambio principal.

### Inversión de dependencias

El Service debe depender de `FavoritoRepository` y `ProductoClient`, no de `ConcurrentHashMap`, `RestClient` o una URL concreta.

La inyección por constructor hace explícitas las dependencias y permite usar implementaciones reales, fakes o mocks.

### Encapsulación

La colección del repository debe ser privada. No se debe permitir que un Controller modifique directamente el estado interno ni que una lista devuelta exponga la colección original.

### Abstracción y polimorfismo

`ProductoClient` y `FavoritoRepository` expresan qué operaciones necesita el Service sin imponer cómo se implementan.

En producción pueden usarse `DummyJsonProductoClient` y `FavoritoRepositoryMemoria`; en las pruebas pueden ocupar ese lugar un mock, un fake o un servidor HTTP local.

### Liskov e interfaces pequeñas

Las implementaciones reales y de prueba deben respetar el mismo contrato. Si un repository promete `Optional.empty()` cuando no encuentra un favorito, un fake o mock utilizado en tests debe mantener esa expectativa.

No hace falta crear interfaces artificiales para clases que no tengan una frontera reemplazable.

## 15. OpenAPI y Swagger

La dependencia de springdoc debe habilitar:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
http://localhost:8080/v3/api-docs.yaml
```

Se recomienda agregar:

- `@Tag` para agrupar Productos y Favoritos;
- `@Operation` en cada endpoint;
- `@Schema` en DTOs con descripciones y ejemplos;
- `@ApiResponses` para respuestas alternativas y errores.

Springdoc puede inferir rutas, parámetros y tipos a partir de los mappings y de las firmas Java. Las anotaciones deben agregarse principalmente donde Java no expresa la intención humana: reglas, ejemplos, propósito y errores.

No hace falta crear HTML manual, agregar `@EnableSwagger` ni usar Springfox.

La documentación tiene que coincidir con el comportamiento real. No se debe documentar `200` si el endpoint devuelve `201`, ni un `404` si la aplicación termina respondiendo `500`.

Swagger UI ejecuta la API real al utilizar “Try it out”. No reemplaza las pruebas automatizadas y las operaciones sobre favoritos modifican la colección en memoria.

## 16. Orden recomendado de implementación

1. Confirmar que Java 25 y `.\mvnw.cmd test` funcionan.
2. Agregar Validation y springdoc al `pom.xml`.
3. Mantener temporalmente `/health` como smoke test.
4. Definir rutas, DTOs, ejemplos JSON y códigos HTTP antes de escribir toda la lógica.
5. Implementar `Favorito`, `FavoritoRepository` y `FavoritoRepositoryMemoria`.
6. Implementar `FavoritoService` con inyección por constructor.
7. Implementar `FavoritoController` con validación y respuestas correctas.
8. Implementar `ApiError`, excepciones propias y `@RestControllerAdvice`.
9. Probar favoritos con repository real en pruebas del repository y repository mockeado en pruebas del Service.
10. Crear la interfaz `ProductoClient`.
11. Crear los DTOs externos y el DTO público de productos.
12. Implementar el cliente real de DummyJSON con `RestClient` o `WebClient`.
13. Implementar `ProductoService` y los dos endpoints de lectura.
14. Agregar pruebas del Service de productos usando un mock de `ProductoClient`.
15. Agregar pruebas del adaptador HTTP con un servidor local simulado, si se incluye ese nivel de cobertura.
16. Agregar `@Tag`, `@Operation`, `@Schema` y `@ApiResponses`.
17. Crear o actualizar `README.md` con instalación, ejecución, rutas, ejemplos y Swagger.
18. Crear evidencia mediante tests, capturas, Postman, Insomnia o un archivo `.http`.
19. Ejecutar nuevamente todas las pruebas y revisar que no haya dependencias de Internet en `mvn test`.

Implementar primero favoritos permite validar la arquitectura y el manejo de errores sin depender de un servicio externo. Luego se incorpora DummyJSON detrás de la interfaz.

## 17. Evidencia y README

El README final debería incluir:

- requisitos: JDK 25;
- comandos para ejecutar pruebas;
- comando para iniciar Spring Boot;
- URL base de la API;
- endpoints de productos;
- endpoints de favoritos;
- ejemplos de requests y responses;
- formato de errores;
- URL de Swagger UI;
- aclaración de que favoritos se almacenan en memoria y se pierden al reiniciar;
- aclaración de que productos se consultan desde DummyJSON.

Como evidencia mínima conviene guardar:

- un caso exitoso de productos;
- un error de productos, preferentemente producto inexistente o falla externa simulada;
- un caso exitoso de favoritos;
- un error de favoritos por validación o recurso inexistente;
- captura o exportación de Swagger UI;
- resultado de `mvn test`.

## 18. Checklist de revisión antes de implementar

### Setup

- [ ] El proyecto continúa siendo Maven con Spring Boot 4.1.x.
- [ ] Se utiliza Java 25.
- [ ] Están WebMVC, Validation, springdoc y las dependencias de pruebas.
- [ ] El proyecto inicia y las pruebas base siguen pasando.

### Arquitectura

- [ ] Los Controllers solo manejan HTTP.
- [ ] Los Services contienen los casos de uso.
- [ ] Favoritos usa un Repository en memoria real.
- [ ] Productos usa un cliente o gateway externo abstraído por una interfaz.
- [ ] Las dependencias se reciben por constructor.
- [ ] Las clases de aplicación están debajo de `ar.edu.unvime.apiblank`.

### Productos

- [ ] Existen solamente los dos GET obligatorios.
- [ ] Se consume DummyJSON desde el backend.
- [ ] El JSON externo no se expone directamente.
- [ ] Se usa un DTO público propio.
- [ ] Se manejan producto inexistente y falla externa.

### Favoritos

- [ ] Existe el CRUD completo.
- [ ] No se usa JPA ni base de datos.
- [ ] El repository genera IDs y mantiene el estado en memoria.
- [ ] Se separan request, response y modelo de dominio.
- [ ] La fecha de alta la genera el backend.
- [ ] Un `PUT` no crea si el ID no existe.

### HTTP y errores

- [ ] POST devuelve `201`.
- [ ] DELETE devuelve `204` sin cuerpo.
- [ ] Validación inválida devuelve `400`.
- [ ] Recurso inexistente devuelve `404`.
- [ ] Falla externa devuelve un `5xx` documentado.
- [ ] El formato de error es consistente.

### Pruebas

- [ ] El Service de favoritos se prueba con repository mockeado.
- [ ] El repository en memoria se prueba sin mockearlo.
- [ ] El Service de productos se prueba con `ProductoClient` mockeado.
- [ ] Los Controllers se prueban con el Service mockeado.
- [ ] Los tests no dependen de Internet.
- [ ] Hay éxito y error para cada recurso.

### Documentación

- [ ] Swagger UI abre.
- [ ] `/v3/api-docs` devuelve OpenAPI.
- [ ] Hay tags para ambos recursos.
- [ ] Cada endpoint tiene `@Operation`.
- [ ] Los DTOs tienen ejemplos comprensibles.
- [ ] Los códigos documentados coinciden con los reales.
- [ ] El README permite levantar y probar el proyecto.

## 19. Decisiones pendientes para revisar

Antes de implementar conviene confirmar estas decisiones del equipo:

1. Qué campos exactos tendrá `ProductoResponse`.
2. Si la nota de un favorito será obligatoria u opcional.
3. Si se devolverá `502` o `503` ante una falla de DummyJSON.
4. Si se implementará la paginación opcional.
5. Si se validará contra DummyJSON que el `productoId` exista al crear un favorito.
6. Si se utilizará solo Mockito para los Services o también un servidor HTTP local para probar el adaptador externo.
7. Si la fecha se manejará con `Instant` u `OffsetDateTime`.

La propuesta base es: DTO público pequeño y propio, `nota` opcional con límite de longitud, `502` para error del proveedor externo, sin paginación al principio, favoritos guardando solo `productoId` y Service de productos probado con `ProductoClient` mockeado.

## 20. Resultado esperado después de la aprobación

Una vez revisado y aprobado este informe, el siguiente trabajo será:

1. implementar el TP1 según estas decisiones;
2. ejecutar y corregir las pruebas;
3. revisar manualmente Swagger y los endpoints;
4. actualizar el README y preparar la evidencia;
5. hacer commit y push al repositorio remoto autorizado.
