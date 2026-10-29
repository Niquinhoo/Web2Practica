# TP3 · Resolución paso a paso

La resolución está en `Web2Practica/tp3`, basada en `TP2`. [Consigna](TP3-consigna.md), [teoría](TP3-teoria.md) y [evidencia de ejecución](api-blank/docs/EVIDENCIA-TP3.md).

## 1. Conservar TP2 y revisar la base

Se conservaron productos, favoritos, listas y sus transacciones. V1–V4 no se editaron. La verificación inicial ejecutó 48 unitarias en verde y detectó un error de zona horaria en las integraciones que dependían de PostgreSQL externo. Las pruebas actuales usan UTC y una base nueva de Testcontainers.

`main`, `TP1` y `TP2` conservan sus commits anteriores. Los materiales TP2 y `.codex-audit` que ya estaban sin seguimiento no se agregaron a los commits de TP3.

## 2. Dependencias y configuración

Se agregó Spring Security y sus módulos Resource Server/JOSE. Su biblioteca Nimbus firma y valida JWT; se aprovecha el filtro Bearer de Spring en lugar de duplicar su implementación. Failsafe y `spring-boot-starter-test` ya existían. Se incorporaron los módulos PostgreSQL/JUnit de Testcontainers y `spring-security-test`.

La configuración conserva `DATABASE_URL`, `POSTGRES_USER` y `POSTGRES_PASSWORD`. El perfil `prod` exige `JWT_SECRETO` explícito; `PORT` configura el servidor y `JWT_VIGENCIA_MINUTOS` la expiración. [Variables y ejecución](api-blank/README.md).

## 3. Usuario, registro y migración V5

`Usuario` contiene id, email, hash y rol. El puerto `UsuarioRepository` tiene adapter JPA, como el resto del proyecto. V5 crea `usuarios` con email único y rol limitado a `USER`/`ADMIN`.

`UsuarioServiceImpl` normaliza el email, aplica BCrypt y asigna siempre `USER`. La entrada HTTP se valida y el límite de 72 bytes UTF-8 evita exceder BCrypt. Los responses excluyen contraseña y hash. La restricción única de PostgreSQL resuelve también registros simultáneos; duplicados responden `409`.

## 4. Login y filtro Bearer

`POST /api/auth/login` verifica BCrypt y devuelve JWT con issuer, subject=id, rol y vencimiento. Un email inexistente y una clave incorrecta producen el mismo `401`. Se verifica un hash de descarte también para el email inexistente.

`JwtService` emite HS256; el decoder verifica firma, issuer, tiempos, id positivo y rol permitido. Spring Security configura sesión stateless, rutas públicas y errores JSON `401`/`403`. CSRF se desactiva para el bearer enviado explícitamente en Authorization; no hay cookies de autenticación.

## 5. Dueño de listas y migración V6

V6 agrega `listas.usuario_id` y su índice. Las listas previas quedan con dueño nulo, sin borrar datos; solo ADMIN accede a ellas.

`ListaServiceImpl` obtiene el dueño desde `ContextoSeguridad`, nunca desde el body. `buscarAutorizada` concentra la comprobación de propiedad y la excepción de ADMIN. Se usa antes de leer, borrar, consultar favoritos y moverlos. En un movimiento se comprueban ambas listas antes de escribir.

## 6. Favoritos y transacciones

`FavoritoService` reutiliza la misma comprobación de lista para lectura, alta, PUT y borrado. PUT exige permiso tanto sobre la lista original como sobre el destino. Los listados se filtran por usuario en SQL, con acceso completo para ADMIN.

Las pruebas de TP2 conservan el caso que fuerza una FK tras mover favoritos y demuestra rollback real de toda la transacción. Una lista con favoritos sigue respondiendo `409` al intentar borrarla.

## 7. Pruebas locales

Con Docker iniciado y JDK 25, dentro de `api-blank`:

```powershell
.\mvnw.cmd --batch-mode test
.\mvnw.cmd --batch-mode verify
```

Surefire ejecuta 58 unitarias/regresiones y Failsafe 24 integraciones. Testcontainers inicia PostgreSQL real con puerto dinámico; cada clase de integración usa un esquema aislado y Flyway aplica sus migraciones. No se usa la base de desarrollo ni se omiten casos cuando Docker falla.

Dentro de `deployment/cloudflare`, con Node 24.22 o superior:

```sh
npm ci
npm run check
npm test
```

Las cinco pruebas del Worker verifican inicio frío, concurrencia, readiness agotado, instancia caliente y enrutamiento a una sola instancia. [Matriz completa](api-blank/docs/EVIDENCIA-TP3.md).

## 8. CI y documentación HTTP

`.github/workflows/ci.yml` comprueba Java/PostgreSQL y el Worker. Se alteró deliberadamente una aserción USER → ADMIN, se verificó la corrida roja y se restauró inmediatamente. Los enlaces a ambas corridas y a las revisiones en verde están en el informe de evidencia.

Swagger publica Bearer, registro/login, códigos 200/201/401/403 y las rutas protegidas. Las reglas viven en servicios; los controllers conservan su responsabilidad HTTP.

## 9. PostgreSQL remoto y Tunnel

Por elección del usuario se usa PostgreSQL del equipo remoto y Cloudflare Containers. Se crearon una base y un rol dedicados `webii_tp3`. El servicio systemd `web2-tp3-tunnel` mantiene una conexión saliente hacia Cloudflare, sin abrir un puerto PostgreSQL en el router.

Access protege `tp3-postgres.osadiamarketing.com` con un token de servicio. El cliente `cloudflared` dentro de la imagen crea un listener TCP local al que conecta JDBC. Una petición al hostname sin token Access devuelve `403`.

## 10. Cloudflare Containers y evidencia pública

El Dockerfile construye el JAR con JDK 25 y ejecuta con JRE 25 y usuario sin privilegios. La configuración `cf` publica una clase Durable Object nativa con política `durable-object`, instancia `basic` y un único nombre `tp3`. La comprobación de readiness admite el arranque de Spring/Flyway; un minuto sin actividad permite detener la instancia y reducir consumo.

Tras CI en verde se ejecuta `cf deploy` con el archivo privado de secretos. PostgreSQL mantiene los datos aunque la JVM duerma. [Guía de despliegue, secretos y operación](deployment/cloudflare/README.md).

Se probaron health, registro, login, accesos propios y ajenos desde la URL pública. El smoke HTTP puede repetirse con PowerShell 7:

```powershell
./api-blank/scripts/smoke-tp3.ps1 -BaseUrl https://web2-practica-tp3.nicotrib699.workers.dev
```

Las capturas, resultados y enlaces están en [EVIDENCIA-TP3](api-blank/docs/EVIDENCIA-TP3.md). No se agregaron refresh tokens, OAuth/OIDC, revocación inmediata ni el umbral opcional de JaCoCo.
