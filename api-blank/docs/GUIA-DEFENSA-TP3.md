# TP3: demostración en Swagger y guía para defender la implementación

Informe preparado el 29 de octubre de 2026. Describe la implementación de la rama `tp3` de Web2Practica. Los ejemplos siguientes son instrucciones para una demostración nueva, no registros de nuevas pruebas ejecutadas.

## 1. Qué exige la consigna sobre el alojamiento

La [consigna relevada y guardada en el repositorio](../../TP3-consigna.md), sección 7, pide: «Desplegá la aplicación en un proveedor gratuito con PostgreSQL, por ejemplo Render».

Por lo tanto, **sí hay una condición explícita de gratuidad**. Render es un ejemplo; no exige una cuenta de Render en particular. [Fuente original](https://unvime-web-2.matiendres.workers.dev/practicos/tp3/). La página original no pudo consultarse de nuevo con el navegador de búsqueda al preparar este informe; la cita procede de la copia guardada durante el relevamiento.

Se eligió Cloudflare Containers después de que el usuario indicara que ya tenía Workers Paid. La API Java está en ese producto y PostgreSQL en el equipo remoto del usuario. **Tener una suscripción paga previamente no convierte esta solución en un proveedor gratuito**. El funcionamiento técnico está demostrado, pero este punto requiere aceptación de la cátedra o una experiencia adicional de despliegue en un proveedor gratuito que cumpla la condición. No se realizó esa experiencia adicional. Tampoco se afirma que el despliegue actual sea gratuito o no genere consumo adicional.

La consigna permite apagar el despliegue después de obtener evidencia. No se modificó el alojamiento al redactar este informe.

## 2. Preparar la demostración

1. Asegurarse de que el equipo que aloja PostgreSQL esté encendido, conectado y con PostgreSQL y `web2-tp3-tunnel.service` activos.
2. Abrir [Swagger público](https://web2-practica-tp3.nicotrib699.workers.dev/swagger-ui/index.html).
3. Esperar el arranque si el contenedor estaba dormido. No confundir una demora inicial con un error de permisos.
4. En cada operación: abrirla, pulsar **Try it out**, completar los campos y pulsar **Execute**.
5. Leer **Server response → Code y Response body**. La tabla de respuestas documentadas y `Example Value` no demuestran una ejecución real.
6. Si Swagger ya tiene autorización, abrir **Authorize → Logout → Close** para empezar sin token.

Usaremos dos cuentas descartables: `defensa-a-20261029@example.com` y `defensa-b-20261029@example.com`. Si ya existen, cambiar el sufijo en todos los pasos. Las contraseñas de ejemplo son solo para estas cuentas de demostración; no reutilizar una contraseña personal.

Anotar los valores reales que devuelva la API: `ID_USUARIO_A`, `ID_USUARIO_B`, `ID_LISTA_A` e `ID_LISTA_B`. No asumir que serán 1 o 2. En los campos numéricos de Swagger hay que pegar el número real, no esas etiquetas.

## 3. Probar las rutas públicas y el bloqueo sin token

1. Ejecutar `GET /health`: debe devolver `200` y JSON con `status: ok`.
2. Ejecutar `GET /api/productos`: es público; depende también de la disponibilidad del catálogo externo.
3. Ejecutar `GET /api/listas` sin autorización: debe devolver `401`.

**Para defenderlo:** Swagger es la interfaz de prueba. Spring Security protege el servidor incluso si el cliente usa curl u otra herramienta. El `401` indica que no existe una identidad válida para esa solicitud.

## 4. Registrar al usuario A

Abrir `POST /api/auth/registro` y ejecutar:

```json
{
  "email": "defensa-a-20261029@example.com",
  "password": "Defensa-A-2026!"
}
```

Debe devolver `201` con `id`, `email` y `rol: USER`. Anotar `ID_USUARIO_A`. No debe devolver la contraseña ni su hash.

Repetir exactamente el registro: debe devolver `409`, porque el email ya existe. Probar aparte una contraseña de menos de ocho caracteres: debe devolver `400`.

**Para defenderlo:** el servicio normaliza el email, valida la entrada y usa `PasswordEncoder` con BCrypt. Guarda un hash con sal, no una contraseña recuperable. La restricción UNIQUE de PostgreSQL evita duplicados incluso con solicitudes concurrentes. El cliente no elige el rol: el registro siempre crea USER. BCrypt tiene un límite de 72 bytes UTF-8 que también se controla en el servicio.

## 5. Login incorrecto y correcto

1. Abrir `POST /api/auth/login`.
2. Usar el email de A y una contraseña incorrecta de al menos ocho caracteres, por ejemplo `Incorrecta-2026!`: debe devolver `401`. Una clave demasiado corta activa la validación de entrada y puede devolver `400`, por eso no sirve para demostrar credenciales incorrectas.
3. Ejecutar nuevamente con el JSON correcto del registro: debe devolver `200`.
4. Copiar únicamente el valor de `token`, sin comillas. La respuesta también incluye `tokenType: Bearer` y `expiresIn` en segundos; la vigencia configurada por defecto es 30 minutos.
5. Pulsar **Authorize**, pegar el token en `bearerAuth`, pulsar **Authorize/Apply credentials** y después **Close**. Pegar solo el token: Swagger agrega `Bearer` automáticamente.
6. Ejecutar `GET /api/listas`: debe devolver `200`; para una cuenta recién creada será `[]`.

**Para defenderlo:** BCrypt compara la contraseña con el hash. Si coincide, `JwtService` firma un JWT HS256 que contiene el ID en `sub`, el rol, emisor y tiempos. Un JWT está firmado, no cifrado: sus claims no son un lugar para contraseñas. Spring valida firma, emisor, vencimiento e identidad en cada petición y completa el contexto de seguridad. No se mantiene una sesión de autenticación en el servidor.

No pegar tokens reales en sitios externos ni incluirlos completos en capturas de evidencia. Swagger muestra el Authorization dentro de su ejemplo Curl.

## 6. Crear y consultar una lista propia

Con el token de A activo, ejecutar `POST /api/listas`:

```json
{
  "nombre": "Lista de A para defensa"
}
```

Debe devolver `201`. Guardar el `id` como `ID_LISTA_A` y comprobar que `usuarioId` coincide con `ID_USUARIO_A`. El body de creación contiene solo el nombre: el dueño se obtiene del token.

1. Ejecutar `GET /api/listas/{id}` con `ID_LISTA_A`: `200`.
2. Ejecutar `GET /api/listas`: `200`, con la lista de A.
3. Ejecutar `GET /api/listas/{id}/favoritos` con ese ID: `200`, inicialmente `[]`.

**Para defenderlo:** aceptar un dueño enviado por el cliente permitiría crear recursos a nombre de otra persona. `ListaServiceImpl` usa el ID autenticado para asignarlo.

## 7. Registrar B y probar el aislamiento entre usuarios

1. Registrar B con `POST /api/auth/registro`:

```json
{
  "email": "defensa-b-20261029@example.com",
  "password": "Defensa-B-2026!"
}
```

2. Hacer login con B y copiar su token.
3. Abrir **Authorize → Logout**, ingresar el token de B y cerrar. Verificar que no se esté reutilizando el token de A.
4. Ejecutar `GET /api/listas/{id}` con `ID_LISTA_A`: debe devolver `403`.
5. Ejecutar `GET /api/listas/{id}/favoritos` con `ID_LISTA_A`: `403`.
6. Ejecutar `DELETE /api/listas/{id}` con `ID_LISTA_A`: `403`; la lista debe conservarse.
7. Ejecutar `GET /api/listas`: `200`, sin la lista de A. Para B recién creado será `[]`.
8. Crear una lista con nombre `Lista de B para defensa` y guardar `ID_LISTA_B`. Su `usuarioId` debe ser el de B.
9. Consultar `ID_LISTA_B` con B: `200`.

**Para defenderlo:** B está autenticado, pero no es dueño del recurso. Eso produce `403`. Un ID inexistente produce `404`. El listado filtra en SQL para no devolver recursos ajenos.

## 8. Verificar ambos extremos de un movimiento

Con B autorizado, abrir `POST /api/listas/{origenId}/mover-favoritos`.

1. Poner `ID_LISTA_B` como origen y, en el body, el número real de `ID_LISTA_A` como `destinoId`: debe devolver `403` porque el destino es ajeno.
2. Poner `ID_LISTA_A` como origen y el número de `ID_LISTA_B` como destino: debe devolver `403` porque el origen es ajeno.
3. Confirmar que ambas listas siguen existiendo, consultándolas con sus respectivos dueños.

El body tiene esta forma; reemplazar 123 por el ID real:

```json
{"destinoId": 123}
```

Para demostrar el camino permitido, crear dos listas descartables con el mismo usuario y mover una hacia la otra. Debe responder `204`; el origen se elimina aunque esté vacío. No usar para ese ensayo una lista que se necesite conservar.

**Para defenderlo:** se autorizan origen y destino antes de escribir. La operación mueve los favoritos y elimina el origen en una sola transacción. Ante un error que provoca rollback, las escrituras no quedan parcialmente aplicadas. Las pruebas de integración también verifican el rollback real con PostgreSQL.

## 9. Demostrar ADMIN

El registro público no permite crear administradores. Para una demostración manual, un operador autorizado prepara una tercera cuenta descartable mediante el registro y cambia su rol desde la base. Esta es una instrucción de preparación; no se ejecutó al redactar el informe:

```sql
UPDATE usuarios
SET rol = 'ADMIN'
WHERE email = 'defensa-admin-20261029@example.com';
```

1. Confirmar que se modificó exactamente la cuenta prevista en la base de la práctica.
2. Hacer login nuevamente con esa cuenta: el token anterior conserva el rol anterior.
3. Sustituir el token en Authorize.
4. Consultar `ID_LISTA_A` e `ID_LISTA_B`: ambas deben devolver `200`.
5. `GET /api/listas` debe incluir listas de otros usuarios; puede incluir listas legadas de TP2 sin dueño.

Si no se prepara esa cuenta, presentar la prueba automatizada `SeguridadIT.admin_listasAjenasYLegadas_accedeSinCambiarPropiedad` y aclarar que el caso ADMIN no se demostró manualmente. No fingir que el registro genera ADMIN ni modificar un JWT para cambiar el rol: alterar el contenido invalida la firma.

## 10. Token inválido, cierre y limpieza

1. En Authorize reemplazar el token por `token-invalido`: `GET /api/listas` debe devolver `401`.
2. Hacer Logout: la misma petición debe seguir respondiendo `401`.
3. Para limpiar, hacer login con cada dueño y borrar únicamente sus listas vacías de demostración: `204`. Una lista con favoritos devuelve `409` hasta retirar esos favoritos.
4. Logout en Swagger al terminar. Esto deja de enviar el token desde la interfaz; no lo revoca en el servidor. Los usuarios de demostración permanecen, porque no hay endpoint público para borrarlos.

## 11. Cómo se construyó y cómo defender las decisiones

| Cambio | Motivo que conviene explicar |
|---|---|
| Continuar desde TP2 en `tp3` | Conservar el dominio y sus transacciones; `main`, TP1 y TP2 no se modificaron. |
| Dominio Usuario, puerto y adapter JPA | Mantener las capas del proyecto: HTTP, negocio y persistencia. |
| V5 crea usuarios; V6 agrega dueño a listas | Evolucionar sin editar V1–V4 ni borrar datos previos. Las listas legadas quedan solo para ADMIN. |
| Spring Resource Server + Nimbus JOSE/JWT | Reutilizar firma, decoder y filtro Bearer de una biblioteca existente en vez de escribir un filtro propio. |
| Autorización en `ListaServiceImpl.buscarAutorizada` | Compartir la misma regla con favoritos y proteger llamadas al servicio que no pasen por un Controller. |
| BCrypt y secreto por entorno | No guardar contraseñas en texto plano ni credenciales de producción en Git. |
| Testcontainers + Flyway | Probar con PostgreSQL real y base temporal, sin depender de la DB del desarrollador. |
| Surefire y Failsafe | Separar unitarias e integraciones; `verify` ejecuta ambas. |
| GitHub Actions | Ejecutar las comprobaciones en cada push y conservar reportes; se demostró una prueba roja deliberada y su reparación. |
| Contenedor Java + PostgreSQL externo | La base conserva datos cuando el contenedor se detiene o reemplaza. |

Recorrido de una petición protegida: **Swagger → HTTPS/Worker → Spring Security valida JWT → Controller valida la entrada → Service comprueba propiedad → Repository/JPA → PostgreSQL**. La conexión de la aplicación a la base usa un cliente cloudflared y Access; el conector del equipo remoto mantiene el Tunnel saliente.

Hay dos credenciales distintas: el **JWT del usuario** controla el acceso a listas; el **token de servicio de Access** permite al contenedor alcanzar PostgreSQL. El usuario de Swagger no recibe el token de Access ni la contraseña de la DB.

## 12. Evidencia existente y límites

La verificación anterior registró **58 unitarias, 24 integraciones y 5 pruebas del Worker: 87 en total**, con [CI final exitoso](https://github.com/Niquinhoo/Web2Practica/actions/runs/40820858514). El [informe de evidencia](EVIDENCIA-TP3.md) incluye capturas y resultados HTTP públicos. Este documento no agrega una nueva ejecución de esas pruebas.

No hay refresh tokens ni revocación inmediata de JWT; una modificación de rol no invalida los tokens ya emitidos. La disponibilidad depende del equipo remoto y del Tunnel; el token de Access se emitió con 30 días de vigencia y requiere renovación. El alojamiento elegido usa Workers Paid y queda pendiente la aceptación académica de esa diferencia con el requisito gratuito.
