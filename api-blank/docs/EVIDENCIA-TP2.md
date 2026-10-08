# Evidencia del TP2

Verificación realizada el **6 de octubre de 2026**, zona horaria America/Buenos_Aires.
Proyecto: `api-blank/`, rama local `tp2`, continuación del TP1.

## Entorno real

- Java 25.0.4, Spring Boot 4.1.1 y Maven Wrapper.
- PostgreSQL **17.11 real**, binarios para Windows publicados por EDB;
  conexión local `127.0.0.1:55432`, base `webii_tp2`.
- API HTTP en `http://localhost:8080`.
- Docker no estaba instalado: se verificó la alternativa local. El archivo Compose
  se entrega para usar PostgreSQL 17 en otros entornos; no se ejecutó Docker en este equipo.

## Verificación automática

```powershell
.\scripts\run-local.ps1 -Verify
```

También se ejecutó una compilación limpia con Java 25 y `TEST_DATABASE_URL` configurada:

```powershell
.\mvnw.cmd --batch-mode clean verify
```

Resultado: **BUILD SUCCESS**, **57 pruebas** (48 unitarias y 9 de integración),
sin fallos, errores ni pruebas omitidas.

- [Log de Maven](evidencia/tp2/maven-verify.txt)
- [Resultado por suite](evidencia/tp2/tests-resultados.json)

Revalidación antes de publicar, **7 de octubre de 2026**: el script local arrancó
PostgreSQL desde cero y ejecutó `verify`, con las mismas 57 pruebas sin fallos.
Se corrigió el arranque del proceso para no bloquear la terminal y se ubicó su log
fuera del directorio de datos.
[Log de la revalidación](evidencia/tp2/maven-verify-push.txt).

Las pruebas de integración usan esquemas aleatorios que se eliminan al finalizar.
El catálogo externo se simula en la suite; su cliente se prueba con un servidor HTTP local.

`FlywayMigrationIT` crea un esquema en V1, inserta un favorito sin lista, avanza a V3,
agrega una lista y un favorito clasificado y aplica V4. Confirma que el favorito previo
termina en “Sin clasificar”, conserva la nota y el producto, el otro permanece en “Regalos”,
la columna termina en NOT NULL y volver a ejecutar Flyway no aplica cambios.

`ApiIntegrationIT.falloRealTrasMoverRevierteTodaLaTransaccion` agrega una tabla temporal
con FK a la lista origen. El UPDATE de favoritos se ejecuta y luego la FK impide el DELETE
del origen. La API devuelve 409; una consulta posterior confirma que el favorito sigue en
el origen, el origen existe y el destino permanece vacío. La tabla auxiliar se elimina.

## Verificación HTTP

Se ejecutaron **25 solicitudes** contra la aplicación empaquetada y PostgreSQL real.
Cada resultado obtenido coincide con el status esperado:

| Operación | Resultado |
|---|---|
| Health | 200 |
| Crear, listar y obtener listas | 201 / 200 / 200 |
| Crear, actualizar y obtener favorito | 201 / 200 / 200 |
| Consultar favoritos de origen y destino | 200 |
| Borrar lista con favoritos | 409 |
| Nombre vacío y favorito sin lista | 400 |
| Lista inexistente | 404 |
| Mover a destino inexistente | 404 |
| Mover a la misma lista | 400 |
| Mover favoritos y borrar origen | 204 |
| Obtener origen eliminado | 404 |
| Crear y borrar lista vacía | 201 / 204 |
| Crear, borrar y buscar favorito eliminado | 201 / 204 / 404 |
| Producto real de DummyJSON y producto inexistente | 200 / 404 |

[Requests, códigos y cuerpos registrados](evidencia/tp2/http-resultados.json).
[Colección reproducible para REST Client](tp2.http).

Además, Swagger UI respondió 200 y contenía la interfaz Swagger;
OpenAPI publicó productos, favoritos y listas:
[spec obtenida por HTTP](evidencia/tp2/openapi.json).

## Persistencia después del reinicio

Se creó el favorito **1**, se actualizó y se movió a la lista **3**. Después se detuvo
el proceso Java y se inició otro proceso contra la misma base.
`GET /api/favoritos/1` devolvió 200 y los mismos cinco campos:

- [Antes del reinicio](evidencia/tp2/favorito-antes-reinicio.json)
- [Después del reinicio](evidencia/tp2/favorito-despues-reinicio.json)
- [Log del segundo arranque](evidencia/tp2/reinicio.log)
- [Consulta de flyway_schema_history](evidencia/tp2/flyway-history.txt)

El historial registra V1, V2, V3 y V4 con `success=true`. El segundo arranque validó
sus checksums, encontró el esquema en versión 4 e informó que no había migraciones pendientes.
El ejemplo persistido queda disponible para inspeccionarlo desde Swagger.

## Entregables y alcance

El [README](../README.md) contiene arranque con Docker y PostgreSQL local,
configuración, endpoints, comparación de clases con TP1, convención y checksums de Flyway,
evolución segura de filas anteriores y justificación de atomicidad/ACID.
El catálogo y el cliente HTTP del TP1 permanecen sin cambios.

La comprobación local verifica PostgreSQL, JPA, migraciones y API. El workflow de GitHub Actions
incluye un servicio PostgreSQL 17 y ejecuta `verify` al publicar cambios en `tp2`.
Los resultados remotos se consultan en la pestaña Actions del repositorio.
