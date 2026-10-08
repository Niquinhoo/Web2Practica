# Web2Practica

Repositorio para almacenar los proyectos prácticos Maven de la materia **Web II**.

## Proyectos

| Proyecto | Descripción |
|---|---|
| [`api-blank`](./api-blank) | TP2: productos, favoritos y listas con PostgreSQL, JPA y Flyway; Java 25 y Spring Boot 4.1.1. Evolución del TP1 en la rama `TP2`. |

Cada proyecto Maven se guarda en su propia carpeta para mantener separada la práctica del repositorio de teoría.

## Requisitos generales

- JDK 25.
- Git.
- PostgreSQL 17 (Docker Compose o instalación local).
- Conexión a Internet para descargar dependencias de Maven la primera vez.

Los proyectos incluyen Maven Wrapper, por lo que no es necesario instalar Maven globalmente.

## Ejecutar `api-blank`

Desde Windows PowerShell:

```powershell
cd api-blank
docker compose up -d --wait
.\mvnw.cmd test
```

Para iniciar la aplicación:

```powershell
.\mvnw.cmd spring-boot:run
```

Desde Linux o macOS:

```bash
cd api-blank
./mvnw test
./mvnw spring-boot:run
```

La aplicación queda disponible en `http://localhost:8080`.

En este equipo, sin Docker: `api-blank/scripts/run-local.ps1` configura la base local y arranca
la aplicación. Para la verificación completa, ejecutarlo con `-Verify`.
Consultar el [README del proyecto](api-blank/README.md) para conexión, migraciones, Swagger
y los casos de prueba del TP2.
