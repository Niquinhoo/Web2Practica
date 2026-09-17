# Evidencia TP2

Verificado el 16/09/2026, en la rama `TP2`, con Java 25.0.2, Spring Boot 4.1.1
y PostgreSQL 17 real en `127.0.0.1:55432`. No se usó H2.

## Resultados

- [Maven verify](evidencia/tp2/maven-verify.txt): **47 pruebas, 0 fallos, 0 errores, 0 omitidas**; JAR generado.
- [Historial Flyway](evidencia/tp2/migraciones.txt): V1, V2, V3 y V4 aplicadas en `public`, todas exitosas.
- [Reinicio](evidencia/tp2/reinicio.json): se creó un favorito, se detuvo el proceso Java
  10120 y se arrancó otro (25712); GET devolvió exactamente los mismos datos.
- [HTTP real](evidencia/tp2/http-resultados.json): lista ocupada 409, nombre inválido 400,
  favorito sin lista 400, movimiento a destino inexistente 404, actualización 200,
  movimiento correcto 204, origen eliminado 404, favorito en destino 200 y borrados 204.
- [Swagger](evidencia/tp2/swagger.json): UI respondió 200; OpenAPI publica Productos,
  Favoritos y Listas, incluidas las rutas de consulta y movimiento de favoritos.

Las peticiones de evidencia usaron el JAR en el puerto 18080. La aplicación de
prueba se detuvo al terminar; PostgreSQL local sigue disponible en 55432.
Los registros HTTP de prueba se eliminaron; permanece la lista inicial “Sin clasificar”.

## Comprobaciones automáticas relevantes

`Tp2IntegrationTests` verifica creación de listas con 201 y Location, listados,
validaciones, 404, 409, movimiento de varios favoritos, conservación del contenido
previo del destino y rechazo de origen=destino. También prueba:

1. **Rollback real:** un trigger temporal hace fallar el DELETE después del UPDATE.
   Sin una transacción exterior de test, comprueba que el favorito siga en origen
   y que la lista origen continúe existiendo. El trigger se elimina al finalizar.
2. **Evolución con datos:** aplica V1 en un esquema nuevo, inserta un favorito sin lista,
   aplica V2/V3 e inserta otro clasificado; V4 asigna únicamente el primero a
   “Sin clasificar”, conserva el segundo e impide nuevos valores nulos.
3. **Idempotencia:** una segunda ejecución de Flyway no aplica migraciones nuevas.

Las pruebas existentes cubren el CRUD completo, errores, productos y documentación
OpenAPI. Los tests usan `tp2_test` y un esquema aleatorio para la migración histórica,
sin modificar datos de aplicación en `public`.

## Reproducir

Seguir el [README](../README.md) para levantar la base y ejecutar:

```powershell
.\mvnw.cmd --batch-mode verify
.\mvnw.cmd spring-boot:run
```

Ejecutar [tp2.http](tp2.http) en orden. Incluye la pausa para reiniciar la aplicación
y comprobar persistencia. Los cambios funcionales y la documentación quedan en el
árbol de trabajo; no se realizó commit ni push.
