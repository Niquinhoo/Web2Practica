# Web2Practica

Repositorio para almacenar los proyectos prácticos Maven de la materia **Web II**.

## Proyectos

| Proyecto | Descripción |
|---|---|
| [`api-blank`](./api-blank) | Proyecto base con Java 25, Spring Boot 4.1.1, Maven Wrapper y Spring Web. |

Cada proyecto Maven se guarda en su propia carpeta para mantener separada la práctica del repositorio de teoría.

## Requisitos generales

- JDK 25.
- Git.
- Conexión a Internet para descargar dependencias de Maven la primera vez.

Los proyectos incluyen Maven Wrapper, por lo que no es necesario instalar Maven globalmente.

## Ejecutar `api-blank`

Desde Windows PowerShell:

```powershell
cd api-blank
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
