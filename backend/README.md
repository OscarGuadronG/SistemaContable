# Backend de Sistema Contable

Java 21, Jakarta EE 10, Open Liberty, EclipseLink y PostgreSQL (Supabase). La API usa raíz de contexto `/` y base `/api`. El contrato vigente está en [contrato-api.md](../docs/contrato-api.md); los requerimientos están en [requerimientos.md](../docs/requerimientos.md).

## Ejecución y pruebas

```bash
cd backend
./mvnw -q test
./mvnw -q -DskipTests package
./mvnw liberty:dev
```

El WAR se genera en `target/sistema-contable.war`. Maven Wrapper está incluido. Las pruebas usan Mockito, Jersey en memoria y EclipseLink con H2; no necesitan credenciales ni acceso a Supabase. `./mvnw -q verify` también genera cobertura JaCoCo en `target/site/jacoco`.

El esquema documental se mantiene en `docs/esquema-bd.sql`. La aplicación usa `eclipselink.ddl-generation=none`: **no ejecuta migraciones ni crea/recrea datos**. H2 adapta tipos enum, UUID y JSONB; no reproduce los triggers diferidos de PostgreSQL.

## Variables de entorno

| Variable | Predeterminado | Uso |
|---|---|---|
| `PORT` | `9080` | Puerto HTTP, también lo suministra Render |
| `DB_HOST` | `localhost` | Host PostgreSQL |
| `DB_PORT` | `5432` | Puerto PostgreSQL |
| `DB_NAME` | `postgres` | Base de datos |
| `DB_USER` | `postgres` | Usuario JDBC |
| `DB_PASSWORD` | vacío | Contraseña; suministrarla como secreto |
| `DB_SSLMODE` | `prefer` | En Supabase utilizar `require` |
| `SUPABASE_URL` | `https://xyukidsdmihmsmlpddqz.supabase.co` | Emisor y JWKS de Auth |
| `SUPABASE_SERVICE_ROLE_KEY` | sin valor | Solo servidor; necesaria para administrar usuarios |
| `CORS_ORIGINS` | `http://localhost:9000` | Orígenes permitidos, separados por comas |

Para el proyecto indicado, el session pooler es `aws-0-us-west-2.pooler.supabase.com:5432`, base `postgres`, usuario `postgres.xyukidsdmihmsmlpddqz`, con SSL `require`. No hay contraseñas versionadas ni se intenta deducirlas. El datasource conserva `stringType=unspecified` para enum y JSONB y limita su pool a cinco conexiones.

En desarrollo pueden copiarse `src/main/liberty/config/server.env.example` a `server.env` o `bootstrap.properties.example` a `bootstrap.properties`, ambos ignorados por git. Liberty lee esos archivos, no un `.env` arbitrario. Evitar configurar una misma variable con distintos valores en varias fuentes.

## Seguridad y errores

Excepto `GET /api/hello` y `OPTIONS`, cada petición necesita `Authorization: Bearer <access_token>` de Supabase Auth. Se verifica ES256, JWKS remoto con caché y renovación de claves desconocidas, emisor, audiencia `authenticated` y expiración con tolerancia de 60 segundos. El perfil se lee de la BD para autorizar cada solicitud; el rol del JWT no decide permisos. La entidad de perfil no usa caché compartida.

`@Roles` permite declarar permisos por clase y método; el método tiene precedencia y Administrador tiene acceso a los permisos configurados. Los endpoints sin permisos configurados se rechazan. La matriz completa está en el contrato §2. Para créditos, el servicio además controla el tipo de documento: Ventas solo opera créditos de ventas, Contabilidad y Administrador pueden operar ambos.

Las respuestas de error tienen `{codigo,mensaje,violaciones?}`. Los estados son `400` para datos/JSON inválidos, `401 NO_AUTENTICADO`, `403 SIN_PERFIL` o `PROHIBIDO`, `404`, `409 CONFLICTO_INTEGRIDAD`, `422 REGLA_NEGOCIO` y `500` con referencia para el log. DUI, ISSS, NIT y código de cuenta duplicados tienen mensajes legibles y respuesta 409. Bean Validation está localizado al español.

CORS admite únicamente los orígenes configurados, cabeceras `Authorization,Content-Type`, métodos `GET,POST,PUT,DELETE,OPTIONS` y expone `Location,Total-Records`.

## Endpoints

Los listados paginados usan `first=0&max=50`, máximo 100, y `Total-Records`. Las fechas son ISO y los listados de documentos se ordenan por fecha e id. Los métodos de creación devuelven 201 y `Location`. Los cuerpos exactos y permisos de cada ruta están en el contrato.

| Ruta bajo `/api` | Operaciones |
|---|---|
| `/hello` | GET público de salud |
| `/me` | GET identidad, perfil y empleado vinculado |
| `/usuarios`, `/usuarios/{uuid}` | GET/POST y PUT, solo Administrador; integración con Admin API de Supabase |
| `/parametros`, `/parametros/{clave}` | GET y PUT de `{valor}` |
| `/productos`, `/unidades-medida`, `/tipos-producto`, `/lotes` | CRUD de catálogos de inventario |
| `/inventario`, `/inventario/{productoId}` | GET agregado por producto y detalle de lotes/almacenes; filtros buscar, tipoId, almacenId, disponibilidad |
| `/existencias`, `/kardex` | GET colección y detalle |
| `/ajustes` | POST ajuste con cantidad con signo y motivo |
| `/compras`, `/compras/{id}` | POST, GET listado filtrado y detalle |
| `/ventas/precios?almacenId=`, `/ventas`, `/ventas/{id}` | GET precios, POST venta, GET listado filtrado y detalle |
| `/libros/compras`, `/libros/ventas` | GET con desde/hasta, filas y totales |
| `/devoluciones-compra`, `/devoluciones-compra/{id}` | POST y GET; filtros desde/hasta/compraId |
| `/devoluciones-venta`, `/devoluciones-venta/{id}` | POST y GET; filtros desde/hasta/ventaId |
| `/creditos?tipo=venta\|compra` | GET; filtros estado y terceroId |
| `/creditos/{tipo}/{documentoId}?fecha=` | GET saldo, pagos y simulación de interés |
| `/creditos/pagos` | POST capital, fecha y forma de pago; devuelve pago y partida |
| `/almacenes`, `/almacenes/{id}` | CRUD; POST `/{id}/habilitar` y `/{id}/inhabilitar` |
| `/traslados`, `/traslados/{kardexId}` | POST/GET; gastos adicionales y partida cuando corresponde |
| `/empleados`, `/departamentos` | CRUD; filtros empleados estado/departamentoId/buscar |
| `/empleados/seleccion` | GET empleados activos para selección |
| `/empleados/{id}/activar`, `/empleados/{id}/inactivar` | POST cambio de estado |
| `/planillas`, `/planillas/{id}` | GET/POST/PUT; filtros estado/desde/hasta |
| `/planillas/{id}/detalles`, `/planillas/{id}/detalles/generar` | GET/POST detalles, generación de empleados activos |
| `/planillas/detalles/{detalleId}` | GET/PUT/DELETE mientras esté Pendiente |
| `/planillas/{id}/calculo`, `/planillas/{id}/pagar` | GET cálculo y POST pago con partida vinculada |
| `/clientes`, `/proveedores`, `/terceros` | CRUD; GET `/terceros/{id}/partidas` |
| `/cuentas`, `/cuentas/arbol`, `/cuentas/{id}` | GET árbol/lista/detalle, POST/PUT/DELETE seguro; POST activar/desactivar |
| `/partidas`, `/partidas/{id}` | GET libro diario filtrado/detalle y POST manual; inmutables |
| `/balanza?desde=&hasta=` | GET balanza |
| `/periodos`, `/periodos/actual` | GET trimestres fiscales |
| `/hoja-trabajo?periodoId=` o `?desde=&hasta=` | GET hoja trimestral o vista por rango |
| `/periodos/{id}/cerrar` | POST cierre, instantánea y apertura siguiente |

## Reglas contables e inventario

Los recursos delegan en servicios EJB `@Stateless`; los DAOs concentran persistencia. Los DTO y comandos son records. Las peticiones usan JTA y los errores de negocio provocan rollback. Stock, documentos, kardex y partidas se escriben en una sola transacción. `Dinero` concentra HALF_UP: importes a dos decimales y costos unitarios a cuatro.

Compras y ventas guardan `formaPago` (`Contado`, `Credito`, `Cheque`), documento, descuento y totales. Cheque exige número en compras, ventas y pagos. Empleado omitido se resuelve con el perfil; sin vínculo se rechaza con 422. La compra permite crear lotes dentro de su transacción. Los gastos de adquisición se distribuyen por valor de línea y la última absorbe la diferencia de redondeo; el precio de compra queda separado del costo capitalizado en kardex.

El costo de lote por almacén se obtiene de sus entradas a ese almacén. Ventas calcula el precio con el margen, consume FIFO por primera entrada e id de lote, guarda el costo histórico por detalle y reconoce ingreso e inventario/costo en una partida de Ingreso. Los traslados incorporan gastos al costo de destino y generan una partida únicamente por esos gastos. Los ajustes generan partidas y vinculan su kardex: faltantes contra Otros gastos y sobrantes contra Otros ingresos.

Las devoluciones controlan fecha, plazo, cantidades acumuladas y stock cuando sale mercadería. En compras se recupera el costo de la entrada del documento; si el mismo lote tiene varias entradas en la compra/almacén, se usa su promedio ponderado. En ventas se conserva precio y costo históricos, se prorratea el descuento general y se revierte IVA. Un cliente puede devolver a otro almacén habilitado: el límite se controla sobre todo lo vendido de ese lote, independientemente del almacén receptor.

Los créditos copian tasa y plazo al documento. El interés simple se calcula sobre el capital abonado desde la fecha del documento, con meses iniciados de 30 días: a los 29 días se exonera con configuración predeterminada, a los 30 se cobra un mes y a los 45 se cobran dos. No se capitaliza interés. Los saldos restan capital y compensaciones/devoluciones; para ventas se toma de su asiento el reintegro exacto a Clientes, incluido IVA y redondeo por devolución. Las cabeceras se bloquean al pagar/devolver para evitar exceder cantidades o saldos concurrentemente.

Las cuentas usan niveles por longitud 1, 2, 4 y 5; las hijas heredan naturaleza, prefijo y longitud siguiente. Solo se eliminan cuentas sin hijas ni movimientos. La configuración de cuentas coincide con §11 del contrato y admite sobreescritura por propiedad Java o variable de entorno: `cuenta.inventario` → `CUENTA_INVENTARIO`. Las tasas de negocio modificables se obtienen de `parametro`.

Toda partida resuelve y guarda su periodo según la fecha y rechaza periodos cerrados. El calendario fiscal inicia en julio. La hoja separa comprobación, ajustes, saldos ajustados, resultados y balance, y conserva el signo efectivo del saldo. El cierre liquida clases 4/5 contra Pérdidas y ganancias, reconoce utilidad/pérdida y abre el siguiente periodo con clases 1–3. La hoja cerrada se conserva en JSONB. No se permite cerrar antes que los periodos anteriores. Un periodo sin movimientos de resultados se rechaza con 422 para evitar crear un asiento sin líneas; no se inventan importes.

## Docker y Render

Desde la raíz del repositorio:

```bash
docker build -t sistema-contable-api backend/
docker run --rm --name sistema-contable-api -p 9080:9080 \
  -e PORT=9080 -e DB_HOST -e DB_PORT -e DB_NAME -e DB_USER -e DB_PASSWORD \
  -e DB_SSLMODE=require -e SUPABASE_URL -e SUPABASE_SERVICE_ROLE_KEY \
  -e CORS_ORIGINS=http://localhost:9000 sistema-contable-api
curl -i http://localhost:9080/api/hello
curl -i http://localhost:9080/api/me
```

El Dockerfile compila con el wrapper, instala las features necesarias sobre Open Liberty kernel slim Java 21 OpenJ9, copia el WAR y el driver JDBC fuera del WAR y ejecuta la configuración del runtime. El heap está limitado a 300 MB y la caché de clases compartidas usa `/tmp`. La clave de servicio y la contraseña se suministran al ejecutar; nunca se copian dentro de la imagen.

En Render crear un Web Service desde Docker, indicar `backend/Dockerfile` y contexto `backend/` (o configurar Root Directory `backend` y Dockerfile `Dockerfile`). Cargar las variables de la tabla, `DB_SSLMODE=require` y el origen HTTPS real del frontend en `CORS_ORIGINS`. Render inyecta `PORT`. Configurar health check `/api/hello`. El contexto raíz evita prefijos adicionales antes de `/api`.

## Límites de verificación

No se ha probado contra PostgreSQL real ni contra cuentas reales de Auth: no se dispone de la contraseña ni de una service role key, y no se buscaron. H2 comprueba consultas, relaciones, reglas, asientos y rollback; los enum/JSONB, triggers diferidos y permisos efectivos de la BD requieren una prueba adicional en el entorno autorizado.

Una transacción JTA no incluye la Admin API remota. Se valida el empleado antes de crear usuarios y se intenta compensar con DELETE si falla la persistencia del perfil; un fallo de red o del commit posterior puede requerir reconciliar un usuario huérfano en Auth. El backend nunca expone la service role key.

La planilla conserva el cálculo existente de retenciones y horas extra; no incluye aportes patronales ni descuentos adicionales. El límite de memoria de Render requiere observar el consumo del proceso completo bajo carga; el heap no representa la totalidad del RSS.

El resultado exacto de pruebas y despliegue de esta entrega se registra en [INFORME.md](INFORME.md).
