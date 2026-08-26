# api-blank

Proyecto base de **Web II** con Java 25, Spring Boot 4.1.1, Maven Wrapper y Spring Web.

## Requisitos

- JDK 25.
- Conexión a Internet para que Maven descargue sus dependencias la primera vez.

No es necesario instalar Maven globalmente: el proyecto incluye `mvnw` y `mvnw.cmd`.

## Ejecutar las pruebas

En Windows PowerShell:

```powershell
.\mvnw.cmd test
```

En Linux o macOS:

```bash
./mvnw test
```

## Ejecutar la aplicación

En Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

En Linux o macOS:

```bash
./mvnw spring-boot:run
```

La aplicación queda disponible en `http://localhost:8080`.

## Estructura inicial

- `src/main/java`: código de la aplicación.
- `src/main/resources`: configuración.
- `src/test/java`: pruebas.
- `.mvn/`, `mvnw` y `mvnw.cmd`: Maven Wrapper.
- `.github/workflows/maven.yml`: verificación automática en GitHub Actions.
