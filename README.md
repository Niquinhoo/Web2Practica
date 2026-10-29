# Web2Practica · TP3

Proyecto Maven de Web II: Java 25, Spring Boot 4, PostgreSQL y Flyway, evolucionado desde `TP2` en la rama `tp3`.

- [Consigna TP3](TP3-consigna.md), [teoría](TP3-teoria.md) y [plan de trabajo](TP3-paso-a-paso.md).
- [Ejecutar API, autenticación, pruebas y variables](api-blank/README.md).
- [Cobertura y evidencia de TP3](api-blank/docs/EVIDENCIA-TP3.md).
- [Despliegue Cloudflare Containers y PostgreSQL por Tunnel](deployment/cloudflare/README.md).
- [PostgreSQL local con Docker](api-blank/docs/tp3/postgresql-docker.md).

Desde `api-blank`, con JDK 25 y Docker Desktop iniciado:

```powershell
docker compose up -d --wait postgres
$env:DATABASE_URL = 'jdbc:postgresql://127.0.0.1:55432/webii_tp2'
.\mvnw.cmd spring-boot:run
```

El puerto `55432` corresponde al `.env` local documentado; el Compose usa `5432` si no se configura. La API responde en `http://localhost:8080`; Swagger está en `/swagger-ui/index.html`.

```powershell
.\mvnw.cmd --batch-mode verify
```

Las integraciones levantan un PostgreSQL temporal propio con Testcontainers; no usan la base de desarrollo. Las ramas `main`, `TP1` y `TP2` conservan sus estados anteriores.
