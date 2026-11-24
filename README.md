# Web2Practica

Repositorio para los proyectos prácticos de la materia **Web II**.

## Proyectos

| Proyecto | Descripción |
|---|---|
| [`api-blank`](./api-blank) | Proyecto base con Java 25, Spring Boot 4.1.1, Maven Wrapper y Spring Web. |
| [`TS7`](./TS7) | TP4 con Node.js, TypeScript, WebSocket y Server-Sent Events. |

Los proyectos tienen instrucciones propias para ejecutarlos.

## Requisitos de `api-blank`

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
