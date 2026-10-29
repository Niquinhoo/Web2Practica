# TP3 · Cobertura y evidencia

Implementación sobre `tp3`, desde `TP2`. Fecha de ejecución: 29 de octubre de 2026.

## Comprobaciones locales

`./mvnw --batch-mode verify` pasó con **58 pruebas unitarias y 24 de integración**, sin fallos ni omisiones. Las integraciones usaron PostgreSQL 17.11 temporal, iniciado por Testcontainers; se verificó evolución e idempotencia de V1–V6 con datos previos conservados. Reportes XML en `target/surefire-reports` y `target/failsafe-reports`.

| Consigna / riesgo | Evidencia automatizada |
|---|---|
| BCrypt real, sin texto plano, USER fijo | `UsuarioServiceImplTest`, `SeguridadIT.registro_yLogin_guardanHashYNoExponenSecretos` |
| Email duplicado y carrera de registro | `SeguridadIT.registro_concurrente_unSoloUsuarioYConflicto` |
| Validación de entrada y límite BCrypt UTF-8 | `UsuarioServiceImplTest`, parametrizadas de `SeguridadIT` |
| JWT firmado, rol/id, expiración y clave mínima | `JwtServiceTest` |
| Token ausente, malformado, vencido o firma ajena → 401 | `SeguridadIT.acceder_*` |
| Dueño asignado desde identidad | `ListaServiceImplTest.crear_usuarioAutenticado_asignaPropiedad`, `SeguridadIT.lista` |
| Listas propias/ajenas y filtrado de listados | `SeguridadIT.listas_usuarioAjeno_noPuedeLeerBorrarNiMover` |
| Control en origen y destino del movimiento | `SeguridadIT.listas_usuarioAjeno_noPuedeLeerBorrarNiMover`, regresiones `ApiIntegrationIT` |
| Favoritos ajenos, alta/PUT hacia lista ajena → 403 | `SeguridadIT.favoritos_usuarioAjeno_noPuedeOperarPorIdNiUsarListaAjena` |
| ADMIN y listas TP2 sin dueño | `SeguridadIT.admin_listasAjenasYLegadas_accedeSinCambiarPropiedad` |
| CRUD, referencias y rollback real TP2 | 8 casos de `ApiIntegrationIT` y pruebas de servicios/controllers existentes |
| V1–V4 intactas, V5/V6, datos previos e idempotencia | `FlywayMigrationIT` |
| Swagger público con Bearer y rutas auth | `SeguridadIT.rutasPublicas_yOpenApi_documentanBearerSinPedirToken` |

La verificación inicial de TP2 tenía 48 unitarias verdes y dos errores de conexión en integración por el alias de zona horaria enviado por la JVM. El TP3 usa UTC en las JVM de pruebas y bases efímeras, sin dependencia de la configuración local.

## Imagen y PostgreSQL remoto

Se construyó `web2-practica:tp3` con Java 25 y usuario sin privilegios. La imagen incluye un cliente `cloudflared` opcional para conectarse a PostgreSQL por Access y Tunnel.

Se creó una base dedicada `webii_tp3` en el PostgreSQL 18.6 del equipo remoto. El conector systemd `web2-tp3-tunnel.service` quedó activo y el hostname `tp3-postgres.osadiamarketing.com` se protege con una política Service Auth limitada al token del contenedor. Una petición sin ese token recibió `403`.

La imagen ejecutada localmente se conectó a la base remota por el túnel; los logs confirmaron aplicación de las seis migraciones sobre `public`, versión v6. `/health` respondió `200` y `/api/listas` sin JWT respondió `401`.

Para repetir el recorrido HTTP real, con PowerShell 7:

```powershell
./scripts/smoke-tp3.ps1 -BaseUrl http://localhost:18080
```

El script crea dos usuarios con identificadores únicos y contraseñas aleatorias, comprueba registro/login y accesos 200/401/403, y elimina las listas/favoritos creados. Los usuarios de prueba quedan identificados por el prefijo mostrado en la salida para que un operador pueda retirarlos; no hay un endpoint público de borrado de usuarios.

## CI y publicación

- [Fallo deliberado confirmado](https://github.com/Niquinhoo/Web2Practica/actions/runs/40817485164): se alteró únicamente una aserción de rol (`ADMIN` esperado frente a `USER` real). Surefire detectó el fallo y el workflow quedó rojo. La aserción se restauró en el commit siguiente.
- [Recuperación en verde](https://github.com/Niquinhoo/Web2Practica/actions/runs/40817598752): 58 unitarias y 24 integraciones, sin fallos ni omisiones.
- [CI con API y Worker](https://github.com/Niquinhoo/Web2Practica/actions/runs/40818228909): jobs Java y Cloudflare exitosos. El Worker tiene **5 pruebas adicionales** de arranque frío, concurrencia, límite de readiness, instancia caliente y selección de instancia. Total: **87 pruebas**.
- [CI de la revisión con Swagger documentado](https://github.com/Niquinhoo/Web2Practica/actions/runs/40819278261): jobs Java y Cloudflare en verde.
- [API pública](https://web2-practica-tp3.nicotrib699.workers.dev/health) y [Swagger](https://web2-practica-tp3.nicotrib699.workers.dev/swagger-ui/index.html).
- Versión Cloudflare final: `68270b67-0e12-48d3-bc91-69297193bca7`. La evidencia HTTP pública registró health `200`, ausencia de token `401`, registro `201`, login `200`, lista propia `200`, lista/favorito ajenos `403` y limpieza de recursos `204`.
- [Resultados HTTP públicos](tp3/evidencias/http-publico.json), [imagen local contra DB remota](tp3/evidencias/http-local-con-db-remota.json), [captura health](tp3/evidencias/health-200.jpg) y [captura sin token](tp3/evidencias/listas-401.jpg) y [captura autenticada](tp3/evidencias/listas-200.jpg).
- [Resumen de logs del Worker](tp3/evidencias/runtime-cloudflare.json): 62 eventos revisados, sin errores de runtime. El archivo conserva solo conteos; no publica metadatos de visitantes ni cabeceras de autenticación.
- El catálogo público real de DummyJSON también respondió `200` (194 productos en la comprobación).

El primer intento de despliegue detectó que esta cuenta exige la política `durable-object`. Se reemplazó el helper anterior por la API nativa de Durable Objects, usando un único nombre de instancia, readiness compartido entre solicitudes y timeout para el arranque de la JVM. La aplicación publicada conservó Java/Spring y el PostgreSQL remoto. Los secretos no están incluidos en el repositorio.

## Segundo despliegue gratuito: Vercel Hobby

El 29 de octubre se publicó además la misma API Java en [Vercel](https://web2-practica-tp3.vercel.app/health), con [Swagger propio](https://web2-practica-tp3.vercel.app/swagger-ui/index.html), sin retirar Cloudflare. La [consulta de plan](tp3/evidencias/vercel/plan.json) confirmó `hobby`, activo y sin trial. PostgreSQL sigue en el equipo del usuario y se alcanza por Access/Tunnel; no se contrató una base administrada en Vercel.

La [guía reproducible](../../deployment/vercel/README.md) documenta creación del proyecto, secretos, publicación manual desde `tp3`, el timeout del primer intento y su resolución con ajuste de JVM y región `gru1`. Despliegue verificado: `dpl_BBn32Wt21tKgCn6NpCJLgRa6ythP`, fuente `9c8be6510b5b45423a982cbb025445aad021eb58`. Los [logs resumidos](tp3/evidencias/vercel/runtime.json) muestran PostgreSQL 18.6, esquema v6 y arranques de Spring de 13–15 segundos; la consulta de error/fatal para ese despliegue no devolvió registros en la ventana indicada.

Se ejecutó nuevamente el [smoke HTTP completo en Vercel](tp3/evidencias/vercel/http-publico.json): health 200, sin token 401, registro 201, login 200, lista propia 200, lista/favorito ajenos 403 y limpieza 204. Capturas reales: [health](tp3/evidencias/vercel/health-200.jpg), [401 sin JWT](tp3/evidencias/vercel/listas-401.jpg), [200 con JWT](tp3/evidencias/vercel/listas-200.jpg). El cambio no modifica código Java ni la imagen de Cloudflare.

## Cierre de pendientes de entrega

El 29 de octubre se reemplazó la captura de health de Vercel por una tomada desde Swagger, con URL, código `200`, cuerpo JSON completo y cabeceras legibles. Ambos README configuran ahora `POSTGRES_PORT=55432` antes de levantar Compose y usan ese mismo puerto en JDBC; se verificó la configuración resuelta de Compose.

Se renombraron seis clases unitarias de `Tests` a `Test` y cuarenta métodos heredados a `operación_escenario_resultado`, conservando sus cuerpos y aserciones. La convención se comprobó en los 57 métodos de prueba actuales. Los informes históricos de TP1/TP2 conservan los nombres que tenían en esas ejecuciones.

Se ejecutó `./mvnw --batch-mode clean verify`: **58 unitarias y 24 integraciones**, cero fallos, errores u omisiones. `clean` eliminó clases y reportes anteriores al renombrado; los XML nuevos suman exactamente 82 ejecuciones Java.

## Límites operativos

JWT sin revocación inmediata: cambio de rol o baja de cuenta no invalida tokens ya emitidos hasta su vencimiento. Las listas legadas sin dueño solo son accesibles para ADMIN. La disponibilidad de PostgreSQL depende de que el equipo remoto y su Tunnel estén encendidos. Cloudflare Containers conserva Workers Paid; Vercel Hobby aporta la experiencia adicional de alojamiento gratuito, con cuotas y runtime de contenedores beta. No se agregaron refresh tokens, OAuth ni un umbral opcional de JaCoCo. No se realizaron pruebas de carga.
