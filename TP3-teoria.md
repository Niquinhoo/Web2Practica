# TP3 · Apuntes nuevos de Unidad 3

**Fuentes:** relevadas el 28 de octubre de 2026.

- [Resumen de Unidad 3](https://unvime-web-2.matiendres.workers.dev/teoria/03-seguridad-infraestructura/)
- [1. Autenticación y autorización](https://unvime-web-2.matiendres.workers.dev/teoria/unidad-3/01-autenticacion-autorizacion/)
- [2. Pruebas e integración continua](https://unvime-web-2.matiendres.workers.dev/teoria/unidad-3/02-pruebas-integracion-continua/)
- [3. Despliegue automático y diagnóstico](https://unvime-web-2.matiendres.workers.dev/teoria/unidad-3/03-despliegue-automatico/)
- [Presentación: autenticación, autorización y pruebas](https://unvime-web-2.matiendres.workers.dev/practicos/presentaciones/tp3-seguridad-presentacion/)
- [Presentación: pruebas, cobertura, CI y despliegue](https://unvime-web-2.matiendres.workers.dev/practicos/presentaciones/tp3-ci-cd-presentacion/)

## 1. Identidad, credenciales y JWT

El TP3 cambia el dominio de `Lista → Favorito` a `Usuario → Lista → Favorito`. El usuario tiene rol `USER` o `ADMIN`. `/api/productos/**` continúa siendo público; el resto de listas y favoritos requiere token.

### Autenticación y autorización

- **Autenticación:** quién hace la solicitud. Se valida el token al inicio del request. Si falta o no sirve en una ruta protegida, corresponde `401`.
- **Autorización:** qué puede hacer esa identidad sobre el recurso. Se evalúa cuando ya se conoce la lista; ser autenticado no da acceso a las listas de otra persona. Si existe pero no pertenece al usuario y no es `ADMIN`, la consigna fija `403`.
- El filtro JWT identifica; no debe contener las reglas de propiedad. La autorización de listas es una regla de negocio en el Service, así que también protege llamadas que no pasan por ese Controller.
- `ADMIN` es una regla basada en rol (RBAC); comparar `lista.usuarioId` con el principal es una regla basada en atributos (ABAC). Comprobar solo el rol no basta para determinar quién es dueño.

### Contraseñas y registro

- Guardá un hash lento, con sal y costo configurable; nunca la contraseña. No uses SHA-256 aplicado directamente como hash de contraseña.
- Spring `PasswordEncoder` permite elegir BCrypt (ejemplo del apunte) o un algoritmo de contraseña moderno; el apunte menciona Argon2id como preferido por OWASP para sistemas nuevos.
- El DTO de registro recibe email y contraseña, no `rol`; el Service asigna `USER` y la respuesta no expone el hash.
- Validá entrada (`@Email`, longitud mínima de contraseña) y responde el mismo `401` si el email no existe o la clave es incorrecta, para no revelar cuentas.
- `existsByEmail` mejora el mensaje, pero no resuelve carreras: la restricción `UNIQUE` de la base es quien garantiza la unicidad. Email duplicado es `409`.

### Token y configuración

- El JWT tiene `header.payload.signature`. El payload se codifica, no se cifra; no agregues contraseñas ni otros datos secretos.
- El ejemplo incluye `sub` (id), `rol`, `iat` y `exp`. La firma HMAC demuestra integridad ante quien no conoce la clave, pero cualquiera con el secreto puede emitir tokens válidos.
- Cada request `Authorization: Bearer ...` debe validar firma y vencimiento y dejar el usuario/rol en el contexto de seguridad.
- El diseño es stateless: sin consulta a la base por request ni revocación inmediata. Cambio de rol, deshabilitar el usuario o borrar el token del cliente no invalida tokens ya emitidos; el efecto se conserva hasta `exp`. Usá expiración corta, HTTPS y nunca registres tokens en logs. Refresh/revocación queda fuera del TP.
- Los secretos se cargan desde variables de entorno. El valor por defecto es para desarrollo; producción no debe arrancar con el secreto de ejemplo.

### Rutas, CSRF y propiedad

- Rutas públicas indicadas por el apunte: `/api/auth/**`, `/api/productos/**`, `/health`, `/ping`, Swagger UI y `/v3/api-docs/**`; proteger el resto con `anyRequest().authenticated()`.
- `SessionCreationPolicy.STATELESS` evita sesión del servidor. Desactivar CSRF se justifica porque el bearer token viaja en un header que el cliente agrega explícitamente; no basta con decir “la aplicación es stateless”. Si el token viaja en una cookie, hay que volver a evaluar CSRF.
- Un alta de lista toma el dueño del principal autenticado, jamás del body. `GET /api/listas` filtra por dueño; `ADMIN` puede ver todas.
- Centralizá la comprobación de propiedad en un método del Service y usala para leer, borrar, leer favoritos de una lista y mover favoritos. En el movimiento se validan tanto origen como destino.
- V6 agrega `usuario_id` sin definir por sí sola qué hacer con listas previas. El ejemplo teórico la deja nullable: quedan sin dueño y solo un `ADMIN` puede acceder. Si se elige asignarlas a alguien, esa política y el backfill deben quedar explícitos en una migración.

### Matriz de acceso y hueco de alcance

| Operación sobre listas | Sin token | Dueño | Otro usuario | ADMIN |
|---|---:|---:|---:|---:|
| Listar | 401 | Solo las suyas | Solo las suyas | Todas |
| Crear | 401 | A su nombre | A su nombre | A su nombre |
| Leer/borrar una lista | 401 | Permitido | 403 | Permitido |
| Leer sus favoritos | 401 | Permitido | 403 | Permitido |
| Mover favoritos | 401 | Si ambas listas son suyas | 403 | Permitido |

El apunte detecta un hueco importante: con autenticación solamente en `/api/favoritos/**`, cualquier usuario autenticado que conozca el ID podría operar sobre el favorito de otra persona. La consigna no enumera las rutas de favoritos en su control de propiedad, pero el objetivo del dominio dice que los favoritos pertenecen a las listas. Para cerrar ese hueco, al operar sobre un favorito puntual hay que resolver su lista y comprobar dueño/rol; agregá tests de acceso ajeno. También cubrí ese contrato en Swagger y README.

## 2. Pruebas automatizadas

- Las pruebas deben afirmar comportamiento observable y cubrir caminos negativos de seguridad, no solo getters o el camino feliz.
- Una **unitaria** prueba una regla aislada con colaboradores simulados; puede usar Mockito. En `UsuarioServiceImplTest`, el `PasswordEncoder` debe ser real para comprobar que el hash no es texto plano y que corresponde a la clave.
- Una **de integración** verifica piezas conectadas. El ejemplo usa `@SpringBootTest`, `MockMvc` para recorrer los filtros HTTP sin abrir puerto y Testcontainers con PostgreSQL real. Flyway aplica V1–V6 sobre una base limpia.
- No uses una DB en memoria para simular PostgreSQL. Testcontainers necesita Docker en ejecución tanto localmente como en CI.
- Surefire ejecuta `*Test` con `./mvnw test`; Failsafe ejecuta `*IT` durante `./mvnw verify`. El sufijo importa: una integración que Maven no descubre puede dejar el build verde falsamente.
- Convención de métodos: `operación_escenario_resultado`, por ejemplo `obtener_listaAjena_lanzaAccesoDenegado`.
- Spring Boot 4 separa `@AutoConfigureMockMvc` en `spring-boot-starter-webmvc-test`; revisar la dependencia de test si la anotación no compila. Testcontainers 2.x usa `testcontainers-junit-jupiter` y `testcontainers-postgresql`; `PostgreSQLContainer` ya no es genérica.
- Prueba explícitamente el filtro y la configuración de seguridad además de las operaciones de negocio. La matriz ayuda a encontrar combinaciones no cubiertas; el apunte señala en particular administrador, destino ajeno y favorito de otro usuario.

## 3. CI

- CI compila y ejecuta comprobaciones reproducibles en un runner limpio ante push y pull request; el verde cubre solo lo que las pruebas realmente verifican.
- GitHub Actions necesita checkout, JDK 25 y Maven Wrapper. `./mvnw --batch-mode verify` ejecuta unitarias e integración si Failsafe está configurado.
- No dependas de un PostgreSQL configurado a mano en el runner: cada test Spring obtiene su DB con Testcontainers. En runners Ubuntu Docker ya está disponible.
- Si falta permiso de ejecución en `mvnw`, registrar el bit con `git update-index --chmod=+x mvnw`.
- Comprobá que la prueba de autorización cae al romperla deliberadamente y vuelve a verde al arreglarla. Si no aparece en la salida de Maven, el nombre/patrón del plugin puede estar mal.
- CI no despliega por sí solo y no valida que la regla de seguridad esté bien planteada. No subas secretos en workflow ni repositorio.

## 4. Despliegue y diagnóstico

- En producción la app, la base y la configuración son servicios separados. La plataforma define URL pública, HTTPS y puerto; la base ya no es localhost.
- Configuración por ambiente con `${VARIABLE:valorPorDefecto}`. Variables citadas: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRETO`, `JWT_VIGENCIA_MINUTOS` y `PORT`. Convierte la URL `postgres://...` del proveedor al formato JDBC `jdbc:postgresql://host:5432/base`; agrega `?sslmode=require` cuando corresponda. No guardes secretos en Git ni en logs.
- Flyway corre al arrancar sobre la base remota. Si falla una migración, la app debe detenerse; las migraciones no se revierten al bajar la versión del código y no se editan tras aplicarlas.
- Puede desplegar la plataforma conectada al repo con `autoDeployTrigger: checksPass`, o un job de CI con `needs: verificar` y un deploy hook secreto. El pull request se prueba, pero no despliega.
- En Docker, la presentación muestra una imagen multi-stage con JDK 25 para compilar, JRE para ejecutar y un usuario sin privilegios. El servicio debe escuchar en `PORT`.
- `/health` debe ser público, simple y no revelar configuración ni datos internos. Verificá `GET /health → 200`, `/api/listas` sin token `→ 401`, y `/api/listas` con token válido `→ 200`.
- Diagnóstico: fallo antes de arrancar → build/Java/tests/permisos; cae al arrancar → variable o conexión; falla en migración → SQL/version editada; 502/503 → puerto; 401 inesperado → secreto distinto o token vencido; 403 inesperado → usuario/propiedad/rol del token. Mirá el primer error en logs y nunca imprimas claves o tokens.
- El plan gratuito sirve como demostración, no como almacenamiento fiable. La consigna solo exige conservar evidencia del despliegue exitoso.

## 5. Contenido adicional de las presentaciones

La presentación de seguridad recorre el ciclo registro → login → bearer → filtro → rutas públicas/protegidas → regla de propiedad, y compara los tres casos en vivo (`401`, dueño `200`, usuario ajeno `403`). También muestra las dos pruebas unitarias/de integración y cómo usar el botón `Authorize` de Swagger.

La presentación de CI/despliegue agrega una explicación de JaCoCo y reporte de cobertura. El umbral de líneas (ejemplo `60 %`) se presenta como **opcional**; no es una exigencia de la consigna TP3. También muestra el workflow, cómo leer un fallo, el Dockerfile, `render.yaml`, crear el Blueprint, conectar `DB_URL`, revisar los logs, probar Swagger y confirmar que Render no publica cuando CI está rojo.

## Referencias citadas por el material

- [Spring Security](https://docs.spring.io/spring-security/reference/)
- [Spring Security: CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)
- [RFC 7519 · JSON Web Token](https://www.rfc-editor.org/rfc/rfc7519)
- [Mockito](https://site.mockito.org/)
- [Testcontainers](https://testcontainers.com/)
- [GitHub Actions para Java con Maven](https://docs.github.com/actions/automating-builds-and-tests/building-and-testing-java-with-maven)
- [The Twelve-Factor App · configuración](https://12factor.net/config)
- [Spring Boot · configuración externalizada](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [PostgreSQL JDBC · SSL](https://jdbc.postgresql.org/documentation/ssl/)
