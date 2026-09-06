# TP1 · API REST de productos y favoritos

Implementado en la rama `TP1` a partir del proyecto virgen `api-blank`.
Java 25, Spring Boot 4.1.1, Maven, Spring WebMVC, Bean Validation y springdoc 3.1.0.

## Ejecutar

Desde esta carpeta, en PowerShell:

```powershell
.\mvnw.cmd --version
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

En Linux/macOS: `./mvnw test` y `./mvnw spring-boot:run`.
La primera ejecución necesita Internet para descargar dependencias.
Maven debe informar **Java 25**. Puede usar otro JDK aunque `java -version` muestre 25.
En este equipo se puede seleccionar así:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.4'
```

Si Windows muestra `Unable to establish loopback connection` / `Invalid argument: connect`
y la carpeta temporal del usuario tiene acentos, usar una carpeta ASCII para los sockets
internos del JDK. Este ajuste afecta solamente esta terminal y sus procesos hijos:

```powershell
$tp1Temp = Join-Path $env:PUBLIC 'codex-tp1-temp'
New-Item -ItemType Directory -Force -Path $tp1Temp | Out-Null
$env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Djdk.net.unixdomain.tmpdir=$tp1Temp".Trim()
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Para empaquetar y ejecutar:

```powershell
.\mvnw.cmd --batch-mode verify
java -jar target/api-blank-0.0.1-SNAPSHOT.jar
```

API: <http://localhost:8080>. Detener con Ctrl+C.

## Endpoints

| Método | Ruta | Éxito | Descripción |
|---|---|---|---|
| GET | `/health` | 200 | Comprobación de arranque |
| GET | `/api/productos` | 200 | Catálogo completo con DTO propio |
| GET | `/api/productos/{id}` | 200 | Producto por ID |
| POST | `/api/favoritos` | 201 | Crear; header Location con URL del favorito |
| GET | `/api/favoritos` | 200 | Listar ordenados por ID |
| GET | `/api/favoritos/{id}` | 200 | Obtener favorito |
| PUT | `/api/favoritos/{id}` | 200 | Reemplazar producto y nota |
| DELETE | `/api/favoritos/{id}` | 204 | Eliminar; sin cuerpo |

## Productos

El backend consume DummyJSON con RestClient. Solicita `/products?limit=0` para obtener
todo el catálogo; la API propia no implementa paginación. El listado es un array de DTOs:

```json
{
  "id": 1,
  "nombre": "Essence Mascara Lash Princess",
  "descripcion": "Descripción provista por DummyJSON",
  "precio": 9.99,
  "categoria": "beauty"
}
```

Los valores dependen del proveedor. No hay operaciones de escritura sobre productos.

## Favoritos

POST y PUT aceptan:

```json
{"productoId": 1, "nota": "Comprar para regalar"}
```

`productoId` es obligatorio y positivo. `nota` es opcional, admite null o cadena vacía
y tiene un máximo de 500 caracteres. Respuesta de ejemplo:

```json
{
  "id": 1,
  "productoId": 1,
  "nota": "Comprar para regalar",
  "fechaAgregado": "2026-09-06T12:00:00Z"
}
```

- El backend genera ID y fecha UTC; no forman parte del DTO de entrada.
- PUT reemplaza producto y nota. Omitir la nota la deja en null. Conserva ID y fecha.
- Obtener, actualizar o eliminar un ID inexistente devuelve 404. PUT nunca lo crea.
- Se guarda una referencia positiva al producto sin verificarla contra DummyJSON.
- Se permiten varios favoritos sobre el mismo producto.
- Los datos están en memoria y **se pierden al reiniciar**. No hay JPA ni base de datos.
- Las modificaciones son atómicas. La lista es una copia inmutable ordenada; no se
  promete una instantánea transaccional de escrituras concurrentes.

Ejemplo PowerShell:

```powershell
$nuevo = Invoke-RestMethod http://localhost:8080/api/favoritos -Method Post -ContentType 'application/json' -Body '{"productoId":1,"nota":"Comprar para regalar"}'
Invoke-RestMethod "http://localhost:8080/api/favoritos/$($nuevo.id)"
Invoke-RestMethod "http://localhost:8080/api/favoritos/$($nuevo.id)" -Method Put -ContentType 'application/json' -Body '{"productoId":2}'
Invoke-RestMethod "http://localhost:8080/api/favoritos/$($nuevo.id)" -Method Delete
```

## Errores

El manejador central devuelve siempre status, mensaje y campos:

```json
{
  "status": 400,
  "mensaje": "Hay datos inválidos",
  "campos": {"productoId": "debe ser mayor que 0"}
}
```

| Estado | Situación |
|---|---|
| 400 | Validación, JSON mal formado o ID de tipo incorrecto |
| 404 | Favorito/producto inexistente o ruta inexistente |
| 405 | Método HTTP no permitido |
| 415 | Content-Type no soportado |
| 500 | Error inesperado, sin detalles internos |
| 502 | Timeout, falla HTTP o contenido inválido de DummyJSON |

campos es un objeto vacío si el error no corresponde a validación de campos.
Los detalles técnicos del proveedor se registran en el servidor, sin reenviarlos al cliente.

## Configuración

En `src/main/resources/application.properties`:

| Propiedad | Predeterminado |
|---|---|
| `dummyjson.base-url` | https://dummyjson.com o variable DUMMYJSON_BASE_URL |
| `dummyjson.connect-timeout` | 3s |
| `dummyjson.read-timeout` | 5s |

Los timeouts se pueden reemplazar con propiedades de Spring.
Los endpoints de productos necesitan Internet durante el uso normal; favoritos funciona
independientemente del proveedor.

## Arquitectura

```text
producto/  Controller → Service → ProductoClient → DummyJsonProductoClient → DummyJSON
favorito/  Controller → Service → FavoritoRepository → FavoritoRepositoryMemoria
error/     ApiError, excepciones propias y ApiExceptionHandler
config/    OpenAPI, reloj UTC y RestClient con timeouts
```

Inyección por constructor y tipos separados para DTOs públicos, dominio y DTOs externos.
El repositorio usa ConcurrentHashMap y AtomicLong. Clock permite probar la fecha generada.

## Swagger, pruebas y evidencia

- [Swagger UI](http://localhost:8080/swagger-ui.html)
- [OpenAPI JSON](http://localhost:8080/v3/api-docs)
- [OpenAPI YAML](http://localhost:8080/v3/api-docs.yaml)
- [Requests manuales](docs/tp1.http): ejecutar en orden con REST Client de VS Code;
  reutiliza el ID obtenido mediante una solicitud nombrada.
- [Evidencia de verificación](docs/EVIDENCIA-TP1.md)
- [Informe previo](docs/INFORME-PLAN-TP1.md)

Swagger permite ejecutar la API con Try it out. Las operaciones modifican los favoritos reales.
La suite prueba servicios con Mockito, controladores con MockMvc, repositorio real, cliente
HTTP con servidor local del JDK (incluido timeout), integración de Spring, CRUD y OpenAPI.
Los tests no realizan consultas a Internet.

Mockito usa mock-maker-subclass: se simulan interfaces y servicios no finales, sin
instrumentar DTOs ni auto-adjuntar un agente a Java 25.
GitHub Actions ejecuta verify en pushes a main y TP1, y en PRs a main.

## Referencias

- [RestClient de Spring](https://docs.spring.io/spring-framework/reference/7.1/integration/rest-clients.html)
- [DummyJSON Products](https://dummyjson.com/docs/products)
- [springdoc: instalación](https://springdoc.org/getting-started.html)
