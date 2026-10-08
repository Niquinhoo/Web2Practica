# TP1 · Spring Boot, API REST y arquitectura en capas

Introducción a Spring mediante una API con controladores, servicios, DTO, validación, errores y documentación.

📽️ [Favoritos: cerrar la arquitectura en capas](https://unvime-web-2.matiendres.workers.dev/practicos/presentaciones/tp1-favoritos-presentacion/)

## Objetivos

[   Sección titulada «Objetivos» ](#objetivos)

- Configurar un proyecto Spring Boot con Maven.

- Implementar una arquitectura en capas (Controller → Service → Repository).

- Consumir un servicio web externo desde el backend.

- Diseñar una API RESTful propia, con DTOs desacoplados del modelo interno.

- Aplicar validación de datos de entrada.

- Implementar manejo uniforme de errores.

- Documentar la API con Swagger/OpenAPI.

## El dominio del práctico

[   Sección titulada «El dominio del práctico» ](#el-dominio-del-práctico)

La aplicación va a exponer dos grupos de endpoints:

1. **Catálogo de productos** (`/api/productos`) — de solo lectura. El backend consume una API pública externa y expone su propia versión, con su propio contrato JSON.

2. **Favoritos** (`/api/favoritos`) — un recurso propio, con CRUD completo, guardado en memoria (sin persistencia real).

Este combo permite practicar tanto el consumo de un servicio externo como el diseño de una API propia con todas sus operaciones.

## Forma de trabajo

[   Sección titulada «Forma de trabajo» ](#forma-de-trabajo)

Organizá el proyecto como una aplicación Spring Boot normal (Maven, un único repositorio). Incluí un `README` con las instrucciones para levantar el proyecto y probar los endpoints.

## Consignas

[   Sección titulada «Consignas» ](#consignas)

### 1. Setup del proyecto

[   Sección titulada «1. Setup del proyecto» ](#1-setup-del-proyecto)

1. Creá el proyecto con [Spring Initializr](https://start.spring.io/): Java 25, Maven, Spring Boot 4.1.x.

2. Agregá las dependencias: `spring-boot-starter-webmvc`, `spring-boot-starter-validation`, `springdoc-openapi-starter-webmvc-ui`. Lombok es opcional.

3. Verificá que el proyecto levanta y que un endpoint de prueba responde.

### 2. Catálogo de productos (consumo de un servicio externo)

[   Sección titulada «2. Catálogo de productos (consumo de un servicio externo)» ](#2-catálogo-de-productos-consumo-de-un-servicio-externo)

1. Investigá `RestClient` de Spring (o `WebClient` si preferís programación reactiva) para consumir la API pública [DummyJSON](https://dummyjson.com/products).

2. Definí un DTO propio para el producto — no expongas el JSON externo tal cual, elegí y nombrá los campos que le interesan a tu API.

3. Exponé `GET /api/productos`, que devuelva la lista de productos mapeada a tu DTO.

4. Exponé `GET /api/productos/{id}`.

5. *Opcional:* diseñá tu propio contrato de paginación hacia el cliente (reutilizando o transformando los parámetros `limit`/`skip` de DummyJSON), retomando lo visto sobre paginación en el TP0.

### 3. Favoritos: modelo de dominio y repository en memoria

[   Sección titulada «3. Favoritos: modelo de dominio y repository en memoria» ](#3-favoritos-modelo-de-dominio-y-repository-en-memoria)

1. Definí una entidad de dominio `Favorito` (por ejemplo: id, referencia al producto externo, nota personal, fecha en la que se agregó).

2. Implementá un repository en memoria (una interfaz + una implementación basada en una colección), sin JPA.

### 4. DTOs de favoritos

[   Sección titulada «4. DTOs de favoritos» ](#4-dtos-de-favoritos)

1. Definí un DTO de entrada (lo que el cliente envía al crear/actualizar un favorito) y un DTO de salida (lo que la API devuelve).

2. Mapeá entre la entidad y los DTOs — a mano o con una librería de mapeo, a elección.

### 5. Controller de favoritos — CRUD completo

[   Sección titulada «5. Controller de favoritos — CRUD completo» ](#5-controller-de-favoritos--crud-completo)

Implementá las operaciones sobre `/api/favoritos`, usando el método HTTP y el código de estado correctos en cada caso:

| Operación  | Método  | Código de éxito  |
| --- | --- | --- |
| Crear  | `POST /api/favoritos`  | `201 Created`  |
| Listar  | `GET /api/favoritos`  | `200 OK`  |
| Obtener uno  | `GET /api/favoritos/{id}`  | `200 OK`  |
| Actualizar  | `PUT /api/favoritos/{id}`  | `200 OK`  |
| Eliminar  | `DELETE /api/favoritos/{id}`  | `204 No Content`  |
### 6. Validación

[   Sección titulada «6. Validación» ](#6-validación)

1. Validá el DTO de entrada de favoritos con Bean Validation (`@NotNull`, `@NotBlank`, etc.).

2. Cuando la validación falle, la API debe responder `400 Bad Request` con el detalle de qué campo falló y por qué.

### 7. Manejo uniforme de errores

[   Sección titulada «7. Manejo uniforme de errores» ](#7-manejo-uniforme-de-errores)

1. Implementá un manejador centralizado de excepciones (`@ControllerAdvice`).

2. Contemplá al menos estos casos, con una respuesta de error consistente en toda la API:

- favorito inexistente → `404 Not Found`;

- validación fallida → `400 Bad Request`;

- falla al consumir la API externa (caída o timeout) → un código `5xx` apropiado.

### 8. Documentación

[   Sección titulada «8. Documentación» ](#8-documentación)

1. Integrá springdoc-openapi y verificá que Swagger UI muestra ambos grupos de endpoints (`productos` y `favoritos`).

2. Agregá al menos una descripción por endpoint (`@Operation`).

## Evidencia esperada

[   Sección titulada «Evidencia esperada» ](#evidencia-esperada)

- Repositorio con el proyecto funcionando.

- `README` con las instrucciones para levantarlo.

- Swagger UI navegable, mostrando todos los endpoints documentados.

- Al menos un caso de éxito y un caso de error probado por recurso (capturas o colección de Postman/Insomnia/`.http`).

[ Página anterior
TP0 · Conceptos backend  ](https://unvime-web-2.matiendres.workers.dev/practicos/tp0/)[ Siguiente página
Repaso · Parcial 1  ](https://unvime-web-2.matiendres.workers.dev/practicos/repaso-parcial-1/)
