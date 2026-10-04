# Backend de Sistema Contable

API REST sobre Jakarta EE 10 (Open Liberty `webProfile-10.0`), JDK 21, Maven 3.9 (wrapper incluido) y PostgreSQL
(Supabase). Cubre catálogos, inventario (compras, ventas, traslados, ajustes, kardex, existencias), contabilidad
(partidas y balanza de comprobación) y planillas de pago, con contabilización automática.

> **Estado.** La capa de datos, de negocio y REST está implementada y probada con pruebas unitarias, H2 en memoria y un
> servidor JAX-RS en memoria. Se verificó que **Open Liberty arranca y despliega la aplicación y que la capa REST
> responde** (sin base de datos); **no se ha ejecutado nada contra un PostgreSQL real** (ver
> [Límites conocidos](#límites-conocidos)). Los códigos de cuenta y las tasas son **valores de referencia que debe
> verificar quien conoce el catálogo y la normativa** (ver
> [Valores a verificar](#valores-a-verificar-antes-de-usar-datos-reales)).

## Requisitos

- JDK 21.
- No hace falta instalar Maven: se usa el wrapper (`./mvnw`, o `mvnw.cmd` en Windows).
- Una base PostgreSQL con el esquema de [`../docs/esquema-bd.sql`](../docs/esquema-bd.sql) ya creado. El backend **no
  crea ni modifica tablas** (`eclipselink.ddl-generation=none`).

## Configurar la conexión a la base de datos

La conexión se define con variables de entorno (o con `bootstrap.properties`) que `server.xml` toma como variables de
Liberty. Las plantillas están en `src/main/liberty/config/`:

| Variable | Significado | Por defecto |
|---|---|---|
| `DB_HOST` | Servidor | `localhost` |
| `DB_PORT` | Puerto | `5432` |
| `DB_NAME` | Base de datos | `postgres` |
| `DB_USER` | Usuario | `postgres` |
| `DB_PASSWORD` | Contraseña | *(vacía, no hay valor por defecto)* |
| `DB_SSLMODE` | `disable`, `allow`, `prefer`, `require`, `verify-ca`, `verify-full` | `prefer` |

Pasos:

1. Copiar `src/main/liberty/config/server.env.example` como `src/main/liberty/config/server.env` (está ignorado por git)
   y completar los valores. Liberty **no** lee un archivo `.env` por sí solo; `server.env` sí. Supabase exige SSL: usar
   `DB_SSLMODE=require`.
2. Alternativa: exportar las variables en el shell antes de arrancar, o copiar `bootstrap.properties.example` como
   `bootstrap.properties` (ignorado por git; **tiene más precedencia** que las variables de entorno, no definir ambos).
3. Nunca versionar contraseñas. Los archivos `*.example` no llevan secretos.

El datasource (`jdbc/ContableDS`) lleva `stringType="unspecified"`; es imprescindible para escribir las columnas enum de
PostgreSQL (ver [`../docs/recomendaciones-bd.md`](../docs/recomendaciones-bd.md), sección 6). El driver PostgreSQL no va
dentro del WAR: lo copia `liberty-maven-plugin` al servidor.

## Ejecutar

```bash
cd backend
./mvnw liberty:dev        # compila, instala Liberty (primera vez: descarga el runtime y las características) y arranca en modo desarrollo
```

Otros objetivos útiles:

```bash
./mvnw -q -DskipTests package      # genera target/sistema-contable.war
./mvnw liberty:create liberty:install-feature   # solo instala el servidor y las características
```

El puerto HTTP es el `9080` (`server.xml`). La raíz de contexto no está fijada en `server.xml`: Liberty usa el nombre
del WAR, así que las URL son `http://localhost:9080/sistema-contable/api/...` (verificado desplegando el WAR con
`./mvnw liberty:deploy liberty:start`; `GET /sistema-contable/api/hello` respondió 200). Para detener el servidor:
`./mvnw liberty:stop`. Los ejemplos de esta guía usan la ruta relativa `/api/...`.

## Pruebas

```bash
./mvnw -q test       # pruebas unitarias y de integración en memoria
./mvnw -q verify     # lo anterior + empaquetado y reporte de cobertura (JaCoCo en target/site/jacoco)
```

Las pruebas **no necesitan base de datos ni Liberty**:

- Reglas de negocio con Mockito.
- Mapeo JPA y consultas JPQL reales con EclipseLink contra **H2 en memoria**, cargando un esquema equivalente al de
  `docs/esquema-bd.sql` (incluida la atomicidad de compra/venta/planilla con transacción y *rollback*).
- Recursos REST contra un servidor JAX-RS en memoria (Jersey, solo `test`): estados HTTP, cabeceras `Location` y
  `Total-Records`, JSON de los DTO y traductor de excepciones.

## Estructura de paquetes (`com.sistemacontable`)

| Paquete | Contenido |
|---|---|
| `entity` | Entidades JPA (una por tabla, 22) y `entity.enums` (los 5 enums de PostgreSQL) |
| `control` | DAOs `@Stateless` (`AbstractDAO` genérico + uno por entidad); solo acceso a datos, sin reglas de negocio |
| `control.service` | Servicios `@Stateless` con las reglas de negocio y la contabilización automática; `control.service.cmd` son los comandos (records) que reciben |
| `control.config` | `ConfigContable`: códigos de cuenta y tasas desde `contabilidad.properties` |
| `control.exception` | `ReglaNegocioException` (422) y `NoEncontradoException` (404) |
| `boundary` | Recursos JAX-RS, paginación, traductor de excepciones (`ManejadorExcepciones`/`ErroresHttp`) |
| `boundary.dto` | Records de entrada (`*Request`, con Bean Validation) y de salida (`*Dto`); **nunca** se exponen entidades JPA |
| `boundary.mapper` | Mapeo puro entidad a DTO |

Decisiones de diseño relevantes:

- **Los recursos solo delegan en servicios**; no hay lógica de negocio ni acceso a DAOs en ellos (hay una prueba de
  arquitectura que lo comprueba).
- Cada recurso es `@RequestScoped @Transactional`: la transacción abarca la petición para que el mapeo a DTO lea las
  relaciones perezosas dentro de ella. Los servicios siguen siendo EJB con su propia transacción (se unen a la del
  recurso). Esta combinación (CDI + JTA + EJB) **no se ha ejercitado en Liberty**.
- **Compras, ventas, planillas (pago) y ajustes son atómicos**: stock, kardex y partida se escriben en una sola
  transacción; ante cualquier fallo no queda nada a medias.
- Las partidas, compras y ventas son **inmutables** (solo se crean y se consultan): `PUT`/`DELETE` responden `405`. Una
  cuenta no se borra: se desactiva.

## Convenciones de la API

Base: `/api`. Todo en JSON (`application/json`); fechas ISO (`2026-03-01`, `2026-03-01T08:00:00Z`); importes como
números decimales; los campos nulos no se serializan.

**Paginación** (todos los listados): `?first=<desplazamiento>&max=<tamaño>`, con `first >= 0` (por defecto 0) y
`1 <= max <= 100` (por defecto 50). Son independientes (`first=50&max=50` es la segunda página de 50). El total de
registros de la colección completa viaja en la cabecera `Total-Records`. Valores fuera de rango responden `400`.

**Errores**: cuerpo `{"codigo": "...", "mensaje": "..."}` (más `violaciones` en validaciones y `referencia` en errores
internos).

| Estado | Cuándo | `codigo` |
|---|---|---|
| 400 | Parámetro, JSON o campos obligatorios inválidos | `SOLICITUD_INVALIDA`, `VALIDACION` (lista de `violaciones`), `JSON_INVALIDO` |
| 404 | El recurso (o uno referenciado) no existe, o la ruta no existe | `NO_ENCONTRADO`, `HTTP_404` |
| 405 | Método no permitido (p. ej. `PUT /partidas/1`) | `HTTP_405` |
| 409 | Violación de integridad en la base (duplicado, referencia) | `CONFLICTO_INTEGRIDAD` |
| 415 | Tipo de contenido no soportado | `HTTP_415` |
| 422 | Regla de negocio incumplida (stock insuficiente, partida descuadrada, ...) | `REGLA_NEGOCIO` |
| 500 | Error inesperado: mensaje genérico y `referencia` (el detalle y la traza solo van al log del servidor) | `ERROR_INTERNO` |

Las excepciones que el contenedor envuelve en `EJBException` se desenvuelven: un 422 o un 404 nunca llega como 500.

## Endpoints

`GET` de colección admite paginación. `POST` crea y responde `201` con cabecera `Location` y el recurso creado.

| Recurso | Métodos |
|---|---|
| `/api/hello` | `GET` (verificación) |
| `/api/departamentos`, `/api/empleados`, `/api/terceros`, `/api/clientes`, `/api/proveedores`, `/api/unidades-medida`, `/api/tipos-producto`, `/api/productos`, `/api/lotes`, `/api/almacenes` | `GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}` (204; `422` si tiene referencias) |
| `/api/cuentas` | `GET`, `GET /{id}`, `POST`, `PUT /{id}`, `POST /{id}/activar`, `POST /{id}/desactivar` (sin `DELETE`) |
| `/api/compras` | `GET`, `GET /{id}` (con líneas), `POST` (registra compra, existencias, kardex y partida; el cuerpo lleva `porcentajeContado`) |
| `/api/ventas` | `GET`, `GET /{id}` (con líneas), `POST` (registra venta, existencias, kardex, ingreso y costo de ventas; el cuerpo lleva `porcentajeContado`) |
| `/api/traslados` | `POST` (el movimiento se consulta en `/api/kardex/{id}`, indicado en `Location`) |
| `/api/ajustes` | `POST` (cantidad con signo y motivo; el movimiento se consulta en `/api/kardex/{id}`) |
| `/api/kardex` | `GET`, `GET /{id}` (con líneas); solo lectura |
| `/api/existencias` | `GET`, `GET /{id}`; solo lectura |
| `/api/partidas` | `GET`, `GET /{id}` (con líneas), `POST`; `PUT` y `DELETE` dan `405` |
| `/api/balanza?desde=AAAA-MM-DD&hasta=AAAA-MM-DD` | `GET` (ambas fechas obligatorias) |
| `/api/planillas` | `GET`, `GET /{id}`, `POST`, `PUT /{id}` |
| `/api/planillas/{id}/detalles` | `GET`, `POST` (agrega un empleado Activo) |
| `/api/planillas/{id}/detalles/generar` | `POST` (un detalle por cada empleado Activo sin detalle) |
| `/api/planillas/detalles/{detalleId}` | `GET`, `PUT`, `DELETE` (solo planillas Pendiente) |
| `/api/planillas/{id}/calculo` | `GET` (calcula sin escribir) |
| `/api/planillas/{id}/pagar` | `POST` (pasa a Pagado y registra la partida de egreso) |

**Forma de pago de compras y ventas.** No se guarda en la base de datos: el cuerpo de `POST /api/compras` y
`POST /api/ventas` lleva un `porcentajeContado` obligatorio (0 a 100, ambos incluidos; `400` si falta o está fuera de
rango). Con el total ya con IVA, `contado = redondeo(total × porcentajeContado / 100)` (HALF_UP, 2 decimales) y
`crédito = total − contado`, así la partida cuadra al centavo. El contado va a `cuenta.caja` y el crédito a
`cuenta.proveedores` (compra) o `cuenta.clientes` (venta); nunca se genera una línea en cero y una cuenta de pago solo se
exige si su línea se genera (con 100 % de contado no hace falta `cuenta.clientes`/`cuenta.proveedores`; con 0 %, no
hace falta `cuenta.caja`). Como la forma de pago no queda registrada, saldar después lo que quedó a crédito no está
soportado; hay una propuesta no aplicada en [`../docs/recomendaciones-bd.md`](../docs/recomendaciones-bd.md).

Los listados de compras, ventas, kardex y partidas **no incluyen las líneas** (se omite el campo `lineas`); `GET /{id}`
sí las incluye.

## Valores a verificar antes de usar datos reales

Todos los códigos de cuenta y todas las tasas de `src/main/resources/contabilidad.properties` son **valores de
referencia (placeholders)**. Hay que contrastarlos con el catálogo de cuentas real cargado en la base y con la
normativa vigente (IVA, ISSS, AFP, renta y recargos de horas extra) antes de usar compras, ventas o planillas reales.
Cada clave se puede sobreescribir **sin recompilar** con una propiedad de sistema (`-Dcuenta.inventario=1204`) o con una
variable de entorno (`CUENTA_INVENTARIO=1204`; mayúsculas y puntos a guion bajo).

| Clave | Valor de referencia | Uso |
|---|---|---|
| `cuenta.caja` / `cuenta.bancos` / `cuenta.clientes` | 1101 / 1102 / 1103 | Caja: parte de contado de compras y ventas; Clientes: parte a crédito de ventas; Bancos: pago de planillas |
| `cuenta.inventario` | 1104 | Compras (debe), costo de ventas (haber), ajustes |
| `cuenta.iva.credito` / `cuenta.iva.debito` | 1105 / 2102 | IVA de compras y de ventas |
| `cuenta.proveedores` | 2101 | Parte a crédito de las compras (haber) |
| `cuenta.retenciones.isss` / `.afp` / `.renta` | 2103 / 2104 / 2105 | Retenciones de la planilla |
| `cuenta.ventas` / `cuenta.costo.ventas` | 4101 / 5101 | Ingreso y costo de las ventas |
| `cuenta.sueldos` | 5201 | Gasto de sueldos de la planilla |
| `cuenta.ajuste.inventario` | 5301 | Contrapartida de los ajustes (si `ajuste.genera.partida=true`) |
| `ajuste.genera.partida` | `false` | Si un ajuste de inventario genera partida |
| `iva.tasa` | `0.13` | IVA sobre el subtotal |
| `isss.tasa.empleado` / `isss.techo` | `0.03` / `1000.00` | ISSS del empleado y techo mensual |
| `afp.tasa.empleado` | `0.0725` | AFP del empleado |
| `hora.extra.diurna.recargo` / `hora.extra.nocturna.recargo` | `1.00` / `1.25` | Recargo sobre el valor de la hora |
| `horas.mes` / `dias.mes` | `240` / `30` | Valor de la hora y prorrateo por días |
| `renta.tramo.N.desde/sobre/tasa/cuota.fija` | Tabla de 4 tramos | Retención de renta mensual |

Las cuentas deben existir en `cuentas`, estar activas y ser **imputables**; si falta alguna, la operación se rechaza con
`422` indicando la clave de configuración, sin escribir nada.

## Límites conocidos

- **PostgreSQL no se ha ejercitado.** Las pruebas usan H2 (que no tiene enums nativos de PostgreSQL). Se arrancó
  Liberty (el plugin instala `openliberty-kernel` 26.0.0.9; el motor REST es RESTEasy) **sin base de datos**
  y se comprobó: despliegue del WAR, enrutamiento, CDI + `@Transactional` + EJB hasta el acceso a datos,
  deserialización JSON-B de los records, Bean Validation (`400` con `violaciones`), `404`/`405` y el `500` genérico con
  referencia cuando no hay base. **Sin base de datos no se ejercitó**: ninguna lectura/escritura real, la serialización
  de DTO con datos, `stringType=unspecified` con los enums ni el esquema real; `liberty:dev` (modo desarrollo) tampoco.
- Los mensajes por defecto de Bean Validation salen en inglés (`must not be null`); se pueden localizar con un
  `ValidationMessages.properties` si se desea.
- La base no impone todas las reglas (por ejemplo, no hay `UNIQUE (id_lote, id_almacen)` en `existencias`); el backend
  las valida y compensa con bloqueos, pero hay propuestas de restricciones e índices en
  [`../docs/recomendaciones-bd.md`](../docs/recomendaciones-bd.md) (**no aplicadas**).
- No hay autenticación ni autorización: la API no tiene seguridad y no debe exponerse tal cual.
- Los listados no se pueden filtrar (solo paginar), salvo la balanza por rango de fechas; los de compras, ventas, kardex
  y partidas se ordenan por id ascendente.
- Cada fila de un listado puede cargar sus relaciones perezosas dentro de la transacción (consultas adicionales por
  fila); con páginas de hasta 100 filas es aceptable, pero se puede optimizar con `JOIN FETCH` si hiciera falta.
- El cálculo de planilla no incluye aportes patronales ni otros descuentos; ver el Javadoc de `PlanillaService`.
- `backend/.env.example` no se leyó ni se modificó en este trabajo; la configuración documentada aquí es la de
  `src/main/liberty/config/*.example`. Conviene revisar que ambas plantillas sean coherentes.
