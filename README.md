# Web2Practica

Prácticos Maven de Web II. [`api-blank`](api-blank/README.md) contiene el TP2:
API REST de productos, favoritos y listas con Java 25, Spring Boot 4.1.1,
PostgreSQL, JPA y Flyway. Continúa el TP1 en la rama `TP2`.

Desde PowerShell, con JDK 25 y Docker local disponibles:

```powershell
cd api-blank
docker compose up -d --wait
.\mvnw.cmd --batch-mode verify
.\mvnw.cmd spring-boot:run
```

En Linux/macOS usar `./mvnw`. No hace falta instalar Maven.
[Swagger](http://localhost:8080/swagger-ui.html).
Configuración, alternativa con PostgreSQL local, arquitectura y pruebas en el
[README del proyecto](api-blank/README.md).
