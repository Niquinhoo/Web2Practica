# TP3 · Paso a paso para `Web2Practica`

Esta secuencia traduce la [consigna del TP3](./TP3-consigna.md) y los [apuntes nuevos de Unidad 3](./TP3-teoria.md) al estado actual de este repositorio.

Para usar la base local ya levantada y consultar las comprobaciones realizadas, ver [PostgreSQL con Docker](./api-blank/docs/tp3/postgresql-docker.md).

El texto siguiente conserva el plan inicial. El estado implementado y las verificaciones están en [EVIDENCIA-TP3](api-blank/docs/EVIDENCIA-TP3.md); el despliegue elegido usa [Cloudflare Containers y Tunnel](deployment/cloudflare/README.md).

## Estado inicial encontrado

- La rama local `tp3` ya fue creada desde `TP2`; la página de la materia todavía anunciaba `tp3-starter` como próximo, no disponible al relevarla.
- El proyecto a evolucionar es `api-blank/`: Java 25, Spring Boot 4.1.1, JPA/PostgreSQL/Flyway y migraciones V1–V4 existentes.
- `pom.xml` ya incluye `spring-boot-starter-test` y Failsafe. Todavía no incluye Spring Security, JJWT ni Testcontainers.
- Las pruebas de integración actuales (`ApiIntegrationIT`, `FlywayMigrationIT`) usan una instancia PostgreSQL externa mediante `PostgresTestDatabase`; el workflow actual inicia su propio servicio PostgreSQL.
- El workflow está en `.github/workflows/maven.yml`; no cubre la rama `tp3` y depende de ese servicio PostgreSQL.
- La configuración actual usa `DATABASE_URL`, `POSTGRES_USER` y `POSTGRES_PASSWORD`; los scripts locales y Docker Compose deben seguir funcionando cuando se migren esos nombres.
- El service actual se llama `ListaService`, mientras que la consigna y la guía teórica nombran `ListaServiceImpl` y su prueba `ListaServiceImplTest`.
- `HealthController` y Swagger ya existen; aún no hay `Dockerfile` ni Blueprint de Render.

Había además archivos sin seguimiento (`.codex-audit/`, `TP2-consigna.md`, `TP2-paso-a-paso.md`). Se conservaron. No están incluidos en un commit de esta rama.

## 0. Fijar el punto de partida

- [x] Crear `tp3` desde la rama local `TP2`.
- [ ] Confirmar `git status --short --branch` y el último commit de TP2 antes de tocar código.
- [ ] Consultar si ya se publicó `tp3-starter`; si aparece, comparar su commit con esta rama antes de continuar. La rama local actual parte del TP2 trabajado en este repo.
- [ ] Correr y registrar la verificación base en `api-blank` solo cuando empiece la implementación. No se ejecutó como parte de este relevamiento.

## 1. Dependencias y configuración

1. En `api-blank/pom.xml`, agregar Spring Security, una implementación JWT (JJWT u otra elegida), y Testcontainers para JUnit y PostgreSQL. `spring-boot-starter-test` y Failsafe ya existen; no duplicarlos.
2. Con Spring Boot 4, agregar `spring-boot-starter-webmvc-test` en scope de test si hace falta `@AutoConfigureMockMvc`.
3. Mantener las dependencias gestionadas por el parent Spring Boot cuando sea posible; comprobar compatibilidad de la versión elegida con Java 25 y Boot 4.
4. En `application.properties`, externalizar URL/usuario/contraseña de DB y secreto JWT como `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRETO`; agregar vencimiento del token y `server.port=${PORT:8080}`. Dejar valores de desarrollo locales, nunca un secreto de ejemplo para producción.
5. Actualizar `api-blank/docker-compose.yml`, `api-blank/scripts/run-local.ps1` y README para que usen los nombres acordados. No dejar dos convenciones de variables contradictorias.
6. Si el runner de GitHub informa `Permission denied` con `mvnw`, guardar el bit ejecutable en Git (`git update-index --chmod=+x api-blank/mvnw`, ajustando el path desde la raíz).

**Listo cuando:** el proyecto conserva su configuración local y puede recibir conexión, secreto y puerto desde el ambiente.

## 2. Crear usuario y registro con V5

1. Agregar `Usuario` (`id`, `email`, `passwordHash`, `rol`) y `Rol` (`USER`, `ADMIN`) al dominio; crear `UsuarioRepository` como puerto.
2. Agregar `UsuarioEntity`, `UsuarioJpaRepository` y `UsuarioRepositoryAdapter` en persistencia, con email único.
3. Crear únicamente `V5__create_usuarios.sql`; no editar V1–V4.
4. Implementar DTOs y `POST /api/auth/registro`. Validar email y longitud mínima de contraseña; el request no acepta rol. Asignar `USER`, guardar `PasswordEncoder.encode(...)`, y nunca incluir el hash en el response.
5. Convertir duplicado de email a `409 Conflict`. La restricción única de PostgreSQL debe seguir protegiendo la carrera entre dos registros simultáneos; `existsByEmail` por sí solo no alcanza.

**Unit test requerido:** capturar el `Usuario` enviado al repository y demostrar que su `passwordHash` no coincide con el texto plano y que `PasswordEncoder.matches` lo valida.

## 3. Login, emisión JWT y filtros

1. Crear `POST /api/auth/login` con respuesta uniforme `401` cuando email/contraseña no coincide; no revelar cuál de los dos datos falló.
2. Implementar un servicio JWT con `sub` = id, `rol`, `iat` y `exp`. Validar firma y vencimiento. El payload no cifra datos.
3. Crear filtro bearer que lea `Authorization`, establezca el principal y el rol en `SecurityContextHolder` si el token sirve; si no, limpie el contexto y deje que la configuración decida si la ruta requiere identidad.
4. Configurar Spring Security sin sesión y con lista pública explícita: `/api/auth/**`, `/api/productos/**`, `/health`, `/ping`, Swagger UI y `/v3/api-docs/**`. Proteger todo lo demás por defecto.
5. Configurar `401` para identidad ausente/no válida y `403` para identidad válida sin permiso. Revisar CSRF según el transporte real del token; el apunte lo desactiva porque el bearer va en un header que el cliente agrega, no por el mero hecho de usar stateless.

**Resultado esperado:** login válido entrega un token; endpoints protegidos sin token, vencido o mal firmado devuelven `401`; `/api/productos` sigue público.

## 4. Propiedad de listas con V6

1. Agregar `usuarioId` al dominio `Lista`, a `ListaEntity` y al mapeo del adapter. Mantener el dominio independiente de entidades JPA.
2. Crear `V6__add_usuario_id_a_listas.sql` con FK a `usuarios`. No modificar migraciones anteriores.
3. Elegir explícitamente qué hacer con las listas existentes: la guía teórica deja `usuario_id` nullable, con acceso de `ADMIN` a las listas sin dueño. Si se decide asignarlas, diseñar el backfill antes de imponer `NOT NULL`.
4. Introducir un `ContextoSeguridad` pequeño para que los services lean id y rol sin acoplar la regla de negocio a Spring Security.
5. Al crear lista, asignar el id autenticado desde el servidor; al listar, filtrar por dueño salvo `ADMIN`.
6. Concentrar en una sola comprobación del Service el control de propiedad para obtener, borrar, favoritos de lista y mover favoritos. En el movimiento validar las dos listas.
7. Mantener `404` para inexistente y devolver `403` para lista existente ajena, según el contrato de la consigna.
8. La clase actual es `ListaService`; decidir si renombrarla a `ListaServiceImpl` para cumplir literalmente la nomenclatura solicitada, actualizando imports y pruebas afectados.

**Unit test requerido:** `ListaServiceImplTest` confirma que crear lista usa el usuario autenticado. Agregar también prueba de lista ajena (`403`) y de `ADMIN`.

### Cerrar el hueco de favoritos que señala la teoría

La consigna exige autenticación en `/api/favoritos/**`; la teoría advierte que eso, por sí solo, deja a cualquier usuario autenticado operar sobre favoritos ajenos por ID. Para cumplir el objetivo del dominio de que los favoritos heredan la propiedad de su lista:

- `POST /api/favoritos`: comprobar que el usuario puede usar la lista de destino.
- `GET /api/favoritos`: devolver favoritos de listas del usuario; `ADMIN` puede ver todos.
- `GET`, `PUT` y `DELETE /api/favoritos/{id}`: resolver su lista y aplicar la misma regla dueño/ADMIN.
- Probar dueño, otro usuario, `ADMIN` y falta de token.

La consigna no especifica el status para favorito de otra persona; usar `403` mantiene el contrato de listas y debe quedar documentado.

## 5. Pruebas unitarias y de integración con Testcontainers

1. Renombrar/agregar clases unitarias con sufijo `Test` y las de integración con `IT`; ubicarlas en el mismo paquete del código bajo `src/test/java`.
2. Añadir una prueba unitaria para hash en `UsuarioServiceImplTest` y otra para la propiedad al crear lista en `ListaServiceImplTest`.
3. Migrar `ApiIntegrationIT` y `FlywayMigrationIT` para que cada clase/fixture use `PostgreSQLContainer` de Testcontainers en vez de `PostgresTestDatabase` con host externo. No mantener a la vez un servicio de DB redundante para CI.
4. Con `@SpringBootTest`, `@AutoConfigureMockMvc` y MockMvc, verificar en una base limpia que Flyway aplica V1–V6.
5. Cubrir como mínimo: registro, login válido `200` + token, login inválido `401`, lista propia `200`, lista ajena `403`, endpoint protegido sin token `401`, y `ADMIN` accediendo a lista ajena `200`.
6. Agregar los casos de propiedad de favoritos y de movimiento con destino ajeno para cerrar la matriz indicada en la teoría.
7. Nombrar métodos `operación_escenario_resultado`. Verificar en la salida Maven que `*Test` corre con Surefire y `*IT` con Failsafe.
8. Mantener la prueba de Flyway que cubre datos TP2 existentes: confirmar que V5/V6 no pierden datos y que las decisiones sobre listas antiguas se aplican como se documentó.

**Comandos de aceptación local (cuando se implemente):**

```powershell
cd api-blank
.\mvnw.cmd --batch-mode test
.\mvnw.cmd --batch-mode verify
```

Las pruebas de integración requieren Docker por Testcontainers. `verify` es el comando que debe mostrar las clases `*IT` y ejecutar Failsafe.

## 6. CI en GitHub Actions

1. Adaptar el workflow existente `.github/workflows/maven.yml` al contrato pedido; renombrarlo a `ci.yml` en lugar de crear un segundo pipeline duplicado.
2. Disparar en push y pull request; incluir `tp3` en los pushes mientras se trabaja en esta rama y mantener la política de PR del repositorio.
3. Usar checkout, JDK 25 y cache Maven. Ejecutar `./mvnw --batch-mode verify` desde `api-blank`.
4. Quitar el servicio PostgreSQL del workflow cuando todas las pruebas de integración arranquen su DB con Testcontainers.
5. En una comprobación controlada, romper una aserción de autorización y confirmar rojo; repararla y confirmar verde. No dejar el fallo deliberado en la rama.
6. Copiar al README la URL de una ejecución exitosa y describir dónde abrir el paso fallido.

## 7. Swagger y documentación del proyecto

1. Agregar esquema HTTP Bearer (`bearerAuth`) en configuración OpenAPI y marcar endpoints protegidos con `@SecurityRequirement`; dejar `/api/auth` documentado y el catálogo como público.
2. Revisar las descripciones de favoritos/listas para explicar autenticación y 401/403.
3. Actualizar `api-blank/README.md` y `Web2Practica/README.md` con variables, comandos, Docker requerido por Testcontainers, lectura de CI y pasos de despliegue.
4. Documentar que la regla de propiedad vive en Service y por qué; agregar ejemplos HTTP de registro, login, usuario, admin, 401 y 403.
5. Mantener V5 y V6 append-only. Las migraciones ya aplicadas son inmutables.

## 8. Desplegar y comprobar

1. Crear un `Dockerfile` multi-stage para Java 25: compilar el JAR, ejecutar con JRE y usuario sin privilegios.
2. Añadir `render.yaml` con servicio web y PostgreSQL; este repositorio guarda `pom.xml` dentro de `api-blank`, así que adaptar `rootDir`/`dockerfilePath` al contexto anidado en vez de copiar literalmente rutas pensadas para un proyecto en la raíz.
3. Configurar `DB_URL` en formato JDBC y el host/base de Render; credenciales por separado; generar `JWT_SECRETO` en la plataforma y pasar el puerto asignado como `PORT`.
4. Configurar el despliegue para esperar a CI (`checksPass` o un deploy hook condicionado a CI verde y a push de la rama de despliegue). No poner secretos en YAML ni en Git.
5. Verificar en logs que arranca Spring, Flyway aplica V1–V6 y la app queda activa.
6. Guardar evidencia: URL pública, `/health → 200`, endpoint protegido sin token `→ 401`, con token válido `→ 200`, y logs de migración/arranque. No hace falta dejar activo el plan gratuito después de obtenerla.

## 9. Entrega final

- [ ] Migraciones V5/V6 nuevas, sin editar V1–V4.
- [ ] Registro, login, hashing, JWT y autorización por lista funcionando.
- [ ] Favoritos protegidos por la propiedad de sus listas, o la decisión de alcance documentada.
- [ ] Pruebas `*Test` y `*IT` ejecutadas por los plugins correctos con Testcontainers.
- [ ] `./mvnw verify` verde localmente y en GitHub Actions; evidencia del fallo deliberado y recuperación.
- [ ] README y Swagger actualizados.
- [ ] Despliegue verificable, con URL, requests/capturas y logs.
- [ ] Revisión final de `git status --short --branch`; conservar los archivos existentes sin seguimiento.

## Cosas que quedan fuera

No agregar refresh tokens, revocación, OAuth/OIDC, recuperación de contraseña, límite de intentos, observabilidad externa ni una cuota JaCoCo: el material los menciona como límites o extensiones, y la cobertura JaCoCo aparece como opcional en la presentación.
