# TP3 · Seguridad, pruebas e integración continua

**Fuente:** <https://unvime-web-2.matiendres.workers.dev/practicos/tp3/> · **Relevado:** 28 de octubre de 2026
**Descripción:** autenticación, autorización, pruebas automatizadas, integración continua y una experiencia básica de despliegue.

Material enlazado por la consigna:

- [TP3: autenticación, autorización y pruebas](https://unvime-web-2.matiendres.workers.dev/practicos/presentaciones/tp3-seguridad-presentacion/)
- [TP3: pruebas, cobertura, CI y despliegue](https://unvime-web-2.matiendres.workers.dev/practicos/presentaciones/tp3-ci-cd-presentacion/)
- [Unidad 3 · Seguridad, calidad e infraestructura](https://unvime-web-2.matiendres.workers.dev/teoria/03-seguridad-infraestructura/)

## Objetivos

- Implementar autenticación con usuario y contraseña y emitir un token para las solicitudes siguientes.
- Aplicar autorización por rol y propiedad: cada usuario opera sobre sus listas.
- Comprobar accesos permitidos y denegados con pruebas automatizadas, además de probar la lógica de negocio.
- Ejecutar las pruebas automáticamente en cada push, incluidas las migraciones sobre una base limpia.
- Desplegar el servicio con configuración por variables de entorno y un endpoint de estado verificable.

## Punto de partida y dominio

El práctico continúa el proyecto del TP2, sobre el mismo repositorio Maven, con los mismos paquetes. La rama `tp3-starter` se anunciaba como próxima; al momento del relevamiento no estaba disponible. El starter anunciado parte de TP2 sin dependencias de seguridad ni testing agregadas, por lo que incorporarlas forma parte de la consigna.

El catálogo público `/api/productos` permanece como espejo de solo lectura. Se agrega `Usuario` (rol `USER` o `ADMIN`) y cada `Lista` pasa a pertenecer a un usuario; los `Favorito` siguen perteneciendo a una lista. Un usuario común opera sobre sus listas y un `ADMIN` puede ver las listas de cualquiera.

## Forma de trabajo

Seguí trabajando sobre el mismo repositorio Maven del TP2. Actualizá el `README` con cómo ejecutar las pruebas, cómo se ejecuta el pipeline de CI y los pasos que usaste para desplegar.

## Consignas

### 1. Dependencias y configuración

- Agregá `spring-boot-starter-security`.
- Agregá una biblioteca JWT, por ejemplo JJWT, para emitir y validar tokens.
- Conservá `spring-boot-starter-test` y agregá Testcontainers: `testcontainers`, `testcontainers-junit-jupiter` y `testcontainers-postgresql`.
- Pasá a variables de entorno los datos sensibles fijados en `application.properties`: conexión y credenciales de la base, además del secreto JWT. Usá `${VARIABLE:valorPorDefecto}` para que el proyecto siga funcionando localmente sin configuración adicional.
- Agregá `maven-failsafe-plugin`. Surefire ejecuta las clases `*Test`; Failsafe ejecuta las pruebas `*IT` con `./mvnw verify`.

### 2. Usuario, persistencia y registro

- Definí el dominio `Usuario` (`id`, `email`, `passwordHash`, `rol`) y el puerto `UsuarioRepository`, siguiendo el patrón de `FavoritoRepository` y `ListaRepository`.
- Implementá `UsuarioEntity`, `UsuarioJpaRepository` y `UsuarioRepositoryAdapter`.
- Creá `V5__create_usuarios.sql` con `usuarios(id, email UNIQUE, password_hash, rol)`.
- Implementá `POST /api/auth/registro`: recibe email y contraseña, guarda únicamente el hash producido por `PasswordEncoder` de Spring Security y asigna `USER` por defecto. El rol no lo elige el cliente.
- Respondé `409 Conflict` cuando el email ya existe.

### 3. Autenticación, login y JWT

- Implementá `POST /api/auth/login`: verifica email y contraseña contra el hash y devuelve un JWT firmado que incluye el id y el rol. Credenciales inválidas responden `401 Unauthorized`.
- Configurá un filtro que lea `Authorization: Bearer <token>`, valide firma y vencimiento y deje la identidad disponible durante la solicitud.
- `/api/auth/**` y `/api/productos/**` quedan públicos. `/api/favoritos/**` y `/api/listas/**` exigen token válido; sin token o con token inválido/vencido responden `401 Unauthorized`.

### 4. Autorización por propiedad y rol

- Agregá `usuarioId` al dominio `Lista` y a `ListaEntity`, con relación `@ManyToOne` a `UsuarioEntity`, como el patrón `Favorito → Lista` del TP2.
- Creá `V6__add_usuario_id_a_listas.sql`.
- Al crear una lista, el dueño sale de la identidad autenticada; nunca se acepta desde el body.
- Para `GET`/`DELETE /api/listas/{id}`, `GET /api/listas/{id}/favoritos` y `POST /api/listas/{origenId}/mover-favoritos`, comprobá propiedad. La excepción es `ADMIN`. En el movimiento, comprobá tanto origen como destino.
- Una lista existente que pertenece a otra persona responde `403 Forbidden`, no `404`.
- `GET /api/listas` devuelve las listas del usuario autenticado, o todas para `ADMIN`.
- En el `README`, explicá dónde vive la regla de autorización y por qué la ubicás allí en vez de en el Controller.

### 5. Pruebas

**Unitarias:**

- Una prueba de `ListaServiceImpl` confirma que la lista nueva queda asociada al usuario autenticado.
- Una prueba de `UsuarioServiceImpl` confirma que nunca se guarda la contraseña en texto plano.

**Integración:** usá Testcontainers con PostgreSQL real y Flyway aplicando las migraciones contra una base limpia en cada ejecución. Cubrí:

- login válido → `200` con token;
- login inválido → `401`;
- acceso a lista propia → `200`;
- acceso a lista ajena → `403`;
- `/api/listas` sin token → `401`;
- `ADMIN` sobre lista ajena → `200`.

Las clases unitarias terminan en `Test` (por ejemplo `ListaServiceImplTest`); las de integración, en `IT` (por ejemplo `ListaControllerIT`). Van en el mismo paquete que el código bajo prueba, pero bajo `src/test/java`. Los métodos describen `operación_escenario_resultado`, por ejemplo `obtener_listaAjena_lanzaAccesoDenegado`. El sufijo determina qué plugin Maven ejecuta cada clase.

### 6. Integración continua

- Agregá o adaptá un workflow de GitHub Actions para que en cada push compile y ejecute `./mvnw verify`.
- `verify` debe ejecutar las pruebas unitarias (`*Test`, Surefire) y las de integración (`*IT`, Failsafe), incluidas las migraciones con Testcontainers.
- Demostrá que el workflow falla al romper una prueba deliberadamente y vuelve a pasar al repararla.
- El runner no tiene tu PostgreSQL local: toda prueba que arranca Spring usa su propio PostgreSQL mediante Testcontainers. Si `mvnw` no tiene permiso de ejecución, registralo con `git update-index --chmod=+x mvnw`.

### 7. Despliegue

- Desplegá la aplicación en un proveedor gratuito con PostgreSQL, por ejemplo Render.
- Configurá mediante variables de entorno la conexión remota y `JWT_SECRETO`.
- Confirmá que corrieron las migraciones remotas, que `GET /health` responde en la URL pública y que los logs muestran un arranque sin errores.
- No es necesario mantener el servicio activo una vez obtenida la evidencia.

### 8. Documentación

- Actualizá Swagger: documentá `/api/auth` y marcá que favoritos y listas requieren autenticación.
- Actualizá el `README` con cómo correr las pruebas, cómo interpretar CI y los pasos de despliegue.

## Evidencia esperada

- Proyecto del TP2 evolucionado con autenticación y autorización.
- Migraciones V5 y V6 nuevas, sin modificar una migración ya aplicada.
- Pruebas unitarias y de integración con Testcontainers en verde localmente y en CI.
- Enlace a una corrida exitosa de GitHub Actions.
- URL pública, captura de `GET /health`, y de un endpoint protegido respondiendo `401` sin token y `200` con token válido.
- `README` que justifique dónde se implementó la autorización por propiedad.
