# Contrato de la API — Sistema Contable "Distribuidora Ta Bien"

Fuente única de verdad entre backend (Java, Open Liberty) y frontend (Quasar). Si algo de este documento choca con
`backend/README.md` o con el código existente, **manda este documento** (los requerimientos de Oscar,
`Requerimientos de App.docx`, mandan sobre todo). Lo que no se menciona aquí sigue como está en `backend/README.md`.

## 0. Arquitectura y despliegue

- **Frontend**: SPA Quasar (Vue 3), Static Site en Render. Variables públicas de build:
  `API_URL` (p. ej. `https://sistema-contable-api.onrender.com/api`), `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`.
- **Backend**: WAR en Open Liberty, Web Service Docker en Render (plan free). Escucha en `${PORT}` (Render lo inyecta;
  por defecto 9080). Base: `/api` (la raíz de contexto debe ser `/`, es decir, URL `https://host/api/...`).
- **BD**: PostgreSQL de Supabase, proyecto `xyukidsdmihmsmlpddqz`. El backend se conecta por JDBC al *session pooler*
  (`aws-0-us-west-2.pooler.supabase.com:5432`, usuario `postgres.xyukidsdmihmsmlpddqz`, SSL `require`). El rol
  `postgres` salta RLS. La API de datos de Supabase **no** expone tablas: todas tienen RLS sin políticas, salvo
  `perfil` (cada usuario lee solo su fila).
- **Auth**: Supabase Auth (email + contraseña). El frontend inicia sesión con `@supabase/supabase-js` y manda
  `Authorization: Bearer <access_token>` en cada llamada al backend.

## 1. Autenticación y roles

Roles (`public.rol_app`): `Ventas`, `Bodega`, `Contabilidad`, `Administrador`, `RRHH`.

Tabla `perfil(id uuid = auth.users.id, rol, nombre, id_empleado, activo)`. Un trigger copia `rol` y `activo` a
`auth.users.raw_app_meta_data`, así que el JWT trae `app_metadata.rol` (útil para el menú del frontend tras refrescar
sesión). **El backend autoriza con la tabla `perfil`**, no con el claim (es la fuente de verdad y refleja cambios al
instante).

Verificación del JWT en el backend (filtro JAX-RS `@Provider @Priority(AUTHENTICATION)`):
- Firma ES256 contra el JWKS `${SUPABASE_URL}/auth/v1/.well-known/jwks.json` (cachear las llaves; refrescar si llega un
  `kid` desconocido). Librería sugerida: `com.nimbusds:nimbus-jose-jwt` (empaquetada en el WAR).
- `iss` = `${SUPABASE_URL}/auth/v1`, `aud` = `authenticated`, `exp` vigente (tolerancia 60 s).
- `sub` → `perfil.id`. Sin perfil o `activo = false` → `403 SIN_PERFIL`.
- Rutas públicas: `GET /api/hello` y cualquier `OPTIONS` (preflight CORS).
- Sin token o token inválido → `401 NO_AUTENTICADO`. Rol sin permiso → `403 PROHIBIDO`.
- Anotación propia `@Roles({...})` en clase o método (la del método gana). `Administrador` siempre pasa.
- El usuario autenticado (id, email, rol, idEmpleado) queda disponible para los servicios (bean `@RequestScoped`).

CORS: filtro de respuesta que permite los orígenes de la variable `CORS_ORIGINS` (lista separada por comas; en
desarrollo `http://localhost:9000`), métodos `GET,POST,PUT,DELETE,OPTIONS`, cabeceras `Authorization,Content-Type`,
y expone `Location,Total-Records`.

### Endpoints de sesión y usuarios

| Método y ruta | Roles | Descripción |
|---|---|---|
| `GET /api/me` | todos | `{id, email, rol, nombre, idEmpleado, empleado: {id, nombre, apellido} \| null}` |
| `GET /api/usuarios` | Admin | Lista `{id, email, rol, nombre, idEmpleado, activo, creado}` (lee `auth.users` + `perfil`) |
| `POST /api/usuarios` | Admin | `{email, password, rol, nombre, idEmpleado?}` → crea el usuario con la Admin API de Supabase (`POST ${SUPABASE_URL}/auth/v1/admin/users`, `email_confirm: true`, cabeceras `apikey` y `Authorization: Bearer ${SUPABASE_SERVICE_ROLE_KEY}`) y su `perfil`. `201`. |
| `PUT /api/usuarios/{id}` | Admin | `{rol, nombre, idEmpleado, activo}` (solo `perfil`) |

## 2. Matriz de permisos

`R` = lectura (GET), `W` = escritura. Admin = todo.

| Recurso | Ventas | Bodega | Contabilidad | RRHH |
|---|---|---|---|---|
| Inventario: productos, unidades, tipos, lotes, existencias, `/inventario` | R | R W | R | R |
| Ajustes de inventario | – | W | – | – |
| Almacenes | R | R | R | – |
| Almacenes: crear/modificar/habilitar | – | – | – | – (solo Admin) |
| Traslados | – | R W | R | – |
| Compras, libro de compras | – | R W | R | – |
| Devoluciones a proveedores | – | R W | R | – |
| Proveedores | – | R | R W | – |
| Ventas, precios de venta | R W | R | R | – |
| Libro de ventas | R | – | R | – |
| Devoluciones de clientes | R W | R W | R | – |
| Clientes | R W | – | R | – |
| Créditos de ventas (cuentas por cobrar y cobros) | R W | – | R W | – |
| Créditos de compras (cuentas por pagar y pagos) | – | – | R W | – |
| Empleados, departamentos, planillas | – | – | R | R W |
| `GET /api/empleados/seleccion` (id y nombre de activos) | R | R | R | R |
| Partidas (libro diario), terceros | – | – | R W | – |
| Catálogo de cuentas | – | – | R W | – |
| Periodos, hoja de trabajo, cierre | – | – | R W | – |
| Parámetros (`/api/parametros`) | R | R | R W | R |
| Usuarios | – | – | – | – (solo Admin) |

## 3. Convenciones (se mantienen de `backend/README.md`)

JSON, fechas ISO, importes decimales con 2 decimales (`HALF_UP`), costos unitarios con 4. Paginación
`?first=&max=` (máx. 100) y cabecera `Total-Records`. Errores `{codigo, mensaje, violaciones?}`; además `401`/`403`
de la sección 1. Las violaciones de unicidad (`23505`) responden `409 CONFLICTO_INTEGRIDAD` con un mensaje legible
("Ya existe un empleado con ese DUI", etc.). Mensajes de validación en español (`ValidationMessages.properties`).

Los listados nuevos aceptan filtros por query string (todos opcionales) y se ordenan por fecha ascendente y luego id.

## 4. Parámetros de negocio (tabla `parametro`)

`GET /api/parametros` → `[{clave, valor, descripcion}]`; `PUT /api/parametros/{clave}` `{valor}` (Contabilidad, Admin).

| Clave | Por defecto | Uso |
|---|---|---|
| `credito.tasa_mensual` | 0.02 | Interés mensual de documentos al crédito (se copia al documento al registrarlo) |
| `credito.plazo_meses` | 3 | Plazo; `fecha_vencimiento = fecha + plazo` |
| `credito.dias_exoneracion` | 30 | Pagado antes de estos días → sin interés |
| `venta.margen` | 0.30 | Precio de venta = costo promedio × (1 + margen) |
| `iva.tasa` | 0.13 | IVA |
| `devolucion_compra.dias_max` | 30 | Plazo de devolución a proveedor |
| `devolucion_venta.dias_max` | 30 | Plazo de devolución de cliente |
| `ciclo.mes_inicio` | 7 | Inicio del ciclo (julio) |

Los **códigos de cuenta** siguen en `contabilidad.properties` (sobrescribibles por variable de entorno), con los
valores del catálogo real de la BD (sección 11).

## 5. Inventario

- `GET /api/inventario?buscar=&tipoId=&almacenId=&disponibilidad=con|sin|todos` → por producto:
  `{productoId, nombre, descripcion, tipo:{id,nombre}, unidad:{id,nombre,abrev}, existenciaTotal, costoPromedio,
  porAlmacen:[{almacenId, almacen, cantidad}]}`. `buscar` filtra por nombre de producto o de tipo (ILIKE).
  `tipoId` incluye los subtipos. Con `almacenId`, `existenciaTotal` es la de ese almacén (RF-INV-07).
- `GET /api/inventario/{productoId}` → lo anterior más `lotes:[{loteId, numLote, fechCaducidad, almacenId, almacen,
  cantidad, costoUnitario}]`.
- Productos/unidades/tipos/lotes: CRUD existente. El frontend crea unidad o tipo nuevos antes del producto.
- Ajustes (`POST /api/ajustes`, existente): `ajuste.genera.partida=true`; la partida lleva `id_kardex`. Faltante: Otros
  gastos (debe) / Inventario (haber); sobrante: Inventario / Otros ingresos.

**Costo de un lote en un almacén** (se usa en ventas, traslados y devoluciones): promedio ponderado de las entradas de
ese lote a ese almacén: `Σ(cantidad × precio_costo) / Σ cantidad` sobre `kardex_detalle` cuyo `kardex.destino =
almacén` (entradas de compra, traslados entrantes y devoluciones de clientes). Sin entradas → `422`.

## 6. Compras (RF-COM)

`POST /api/compras` (Bodega, Admin) — reemplaza `porcentajeContado`:

```json
{
  "proveedorId": 1, "empleadoId": 4, "fecha": "2026-11-20",
  "numeroDocumento": "CCF-0012", "descripcion": "Compra de cereal",
  "formaPago": "Contado | Credito | Cheque", "numeroCheque": "000123",
  "descuento": 10.00, "gastosAdicionales": 20.00,
  "lineas": [
    {"loteId": 2, "almacenId": 1, "cantidad": 10, "precio": 8.00},
    {"nuevoLote": {"productoId": 3, "numLote": "L-2026-11", "fechCaducidad": "2027-06-01"},
     "almacenId": 1, "cantidad": 5, "precio": 20.00}
  ]
}
```

- `empleadoId` opcional: si falta se usa `perfil.id_empleado`; si tampoco hay → `422`.
- `numeroCheque` obligatorio si `formaPago = Cheque`.
- Cada línea lleva `loteId` **o** `nuevoLote` (se crea el lote en la misma transacción; RF-COM-03).
- `compra_detalle.precio` = precio de compra (p. ej. 80). Los gastos adicionales se reparten entre líneas en
  proporción a `cantidad × precio` (la última línea absorbe el redondeo) y `kardex_detalle.precio_costo` = precio +
  gasto asignado / cantidad (p. ej. 100) (RF-COM-04/05).
- Montos: `subtotal = Σ cantidad × precio`; `iva = round((subtotal − descuento) × iva.tasa)`;
  `total = subtotal − descuento + iva + gastosAdicionales`. Se guardan en `compra`.
- Kardex `Entrada`, `origen = null`, `destino = almacén` (uno por almacén de la compra), `id_compra`.
- Existencias: suma por lote y almacén.
- Partida (`Egreso` si Contado/Cheque, `Diario` si Crédito), con `id_compra`:
  - Debe: Inventario (`subtotal + gastos`), IVA crédito fiscal (`iva`).
  - Haber: Descuentos sobre compras (`descuento`, si > 0), Caja (Contado) | Bancos (Cheque) | Proveedores (Crédito)
    por `total`.
- Crédito: se guardan `tasa_interes = credito.tasa_mensual`, `plazo_meses`, `fecha_vencimiento`.
- `GET /api/compras?desde=&hasta=&proveedorId=&formaPago=` y `GET /api/compras/{id}` (con líneas, kardex y partida).
- `GET /api/libros/compras?desde=&hasta=` (Bodega, Contabilidad, Admin) → `{filas:[{fecha, compraId,
  numeroDocumento, proveedor, nit, formaPago, subtotal, descuento, gastosAdicionales, iva, total}], totales:{...}}`.

**Devoluciones a proveedores** (RF-COM-12/13), `POST /api/devoluciones-compra`:
`{compraId, fecha, porcentajeCompensacion (0–100), motivo, empleadoId?, lineas:[{loteId, almacenId, cantidad}]}`.
- Rechaza (`422`) si pasaron más de `devolucion_compra.dias_max` días desde la compra, si la cantidad supera lo
  comprado de ese lote/almacén en esa compra menos lo ya devuelto, o si supera las existencias.
- `costo_unitario` = `precio_costo` de la entrada de esa compra; `monto_costo = Σ cantidad × costo`;
  `monto_compensado = round(monto_costo × porcentaje / 100)`.
- Kardex `Devolucion`, `origen = almacén`, `destino = null`, `id_devolucion_compra`. Existencias restan.
- Partida `Diario` con `id_devolucion_compra`: Debe Caja/Bancos/Proveedores (según la forma de pago de la compra)
  por `monto_compensado` y Otros gastos por la pérdida (`monto_costo − monto_compensado`, si > 0); Haber Inventario
  por `monto_costo`.
- `GET /api/devoluciones-compra?desde=&hasta=&compraId=` y `GET /api/devoluciones-compra/{id}`.

## 7. Ventas (RF-VEN)

`GET /api/ventas/precios?almacenId=` (Ventas, Bodega, Admin) → productos con existencia en ese almacén:
`{productoId, nombre, unidad, tipo, existencia, costoPromedio, precioVenta}` donde `costoPromedio` es el promedio
ponderado (por existencia) de sus lotes en el almacén y `precioVenta = round(costoPromedio × (1 + venta.margen))`.

`POST /api/ventas` (Ventas, Admin) — reemplaza `porcentajeContado` y el lote por línea:

```json
{
  "clienteId": 1, "empleadoId": 7, "almacenId": 1, "fecha": "2026-11-20",
  "numeroDocumento": "FAC-0101", "descripcion": "Venta mostrador",
  "formaPago": "Contado | Credito | Cheque", "numeroCheque": null,
  "descuento": 5.00,
  "lineas": [{"productoId": 3, "cantidad": 12}]
}
```

- El precio **no** viaja: lo fija el backend con `/ventas/precios` (RF-VEN-05). `descuento` es un monto general
  (0 ≤ descuento ≤ subtotal; el frontend puede pedirlo en %).
- Valida existencia del producto en el almacén (RF-VEN-07) y descuenta por **FIFO** (RF-VEN-08): lotes ordenados por la
  fecha de su primera entrada a ese almacén, luego `lote.id`. Una línea de `ventas_detalle` por lote consumido, con
  `precio` (de venta) y `costo_unitario` (costo real del lote, RF-VEN-11).
- `subtotal = Σ cantidad × precio`; `iva = round((subtotal − descuento) × iva.tasa)`;
  `total = subtotal − descuento + iva`; `costo_total = Σ cantidad × costo_unitario`. Se guardan en `ventas`.
- Kardex `Salida`, `origen = almacén`, `id_venta`, `precio_costo` = costo real del lote.
- Partida `Ingreso` con `id_venta`: Debe Caja | Bancos | Clientes (`total`), Rebajas sobre ventas (`descuento`, si > 0),
  Costo de ventas (`costo_total`); Haber Ventas (`subtotal`), IVA débito fiscal (`iva`), Inventario (`costo_total`).
- Crédito: igual que compras (`tasa_interes`, `plazo_meses`, `fecha_vencimiento`).
- `GET /api/ventas?desde=&hasta=&clienteId=&formaPago=`, `GET /api/ventas/{id}`.
- `GET /api/libros/ventas?desde=&hasta=` → `{filas:[{fecha, ventaId, numeroDocumento, cliente, nit, formaPago,
  subtotal, descuento, iva, total}], totales}`.

**Devoluciones de clientes** (RF-VEN-16/17/18), `POST /api/devoluciones-venta`:
`{ventaId, fecha, motivo, empleadoId?, lineas:[{loteId, almacenId, cantidad}]}`.
- Plazo `devolucion_venta.dias_max`; cantidad ≤ vendida de ese lote en esa venta − ya devuelta.
- `precio` y `costo_unitario` se copian de la línea de la venta. `monto = round(Σ cantidad × precio × (1 − descuento/subtotal))`,
  `iva = round(monto × iva.tasa)`, `costo = Σ cantidad × costo_unitario`.
- Kardex `Devolucion`, `destino = almacén`, `origen = null`, `id_devolucion_venta`. Existencias suman.
- Partida `Diario` con `id_devolucion_venta`: Debe Devoluciones sobre ventas (`monto`), IVA débito (`iva`), Inventario
  (`costo`); Haber Caja/Bancos/Clientes según la forma de pago de la venta (`monto + iva`), Costo de ventas (`costo`).
- `GET /api/devoluciones-venta?desde=&hasta=&ventaId=`, `GET /api/devoluciones-venta/{id}`.

## 8. Créditos y pagos (RF-GEN-01 a 09)

- `GET /api/creditos?tipo=venta|compra&estado=pendiente|cancelado|todos&terceroId=` → documentos al crédito:
  `{tipo, documentoId, fecha, numeroDocumento, contraparte:{id,nombre}, total, pagadoCapital, saldo,
  tasaInteres, plazoMeses, fechaVencimiento, vencido}`. `saldo = total − Σ pago_credito.capital − devoluciones`.
- `GET /api/creditos/{tipo}/{documentoId}?fecha=AAAA-MM-DD` → lo anterior más `pagos:[...]` y una simulación
  `{fecha, dias, meses, interes}` para pagar **todo** el saldo en esa fecha.
- `POST /api/creditos/pagos` (RF-GEN-07):
  `{"tipo": "venta", "documentoId": 1, "fecha": "2026-11-20", "capital": 500.00, "formaPago": "Contado|Cheque",
  "numeroCheque": null, "descripcion": "Abono"}`.
  - `capital ≤ saldo`. `dias = fecha − fecha del documento`. Si `dias < credito.dias_exoneracion` → `interes = 0`
    (RF-GEN-04); si no, `meses = ceil(dias / 30)` e `interes = round(capital × tasa_interes × meses)` (RF-GEN-05).
  - Guarda `pago_credito` y su partida (`Ingreso` para ventas, `Egreso` para compras) con `id_pago_credito` e
    `id_venta`/`id_compra` del documento (RF-GEN-09):
    - Venta: Debe Caja|Bancos (`capital + interes`); Haber Clientes (`capital`), Productos financieros (`interes`).
    - Compra: Debe Proveedores (`capital`), Gastos financieros (`interes`); Haber Caja|Bancos (`capital + interes`).
  - Respuesta `201` con `{pago, partida}`.

## 9. Almacenes y traslados (RF-ALM)

- Almacenes: CRUD existente; `POST /api/almacenes/{id}/habilitar` y `/inhabilitar` (Admin). `DELETE` se quita del
  frontend (se inhabilita).
- `POST /api/traslados` (Bodega, Admin):
  `{almacenOrigenId, almacenDestinoId, fecha, descripcion, gastosAdicionales, formaPagoGastos: "Contado|Cheque",
  lineas:[{loteId, cantidad}]}`.
  - Valida existencias en origen (RF-ALM-07), origen ≠ destino, ambos habilitados.
  - `precio_costo` = costo del lote en origen + gasto asignado por unidad (reparto por valor). `kardex.gastos_adicionales`.
  - Kardex `Traslado` (origen y destino). Existencias se mueven.
  - Partida `Diario` con `id_kardex`, `id_compra = id_venta = null` (RF-ALM-11), **solo si gastos > 0**: Debe
    Inventario; Haber Caja|Bancos. Sin gastos el traslado no tiene efecto contable y `partida` es `null`.
- `GET /api/traslados?desde=&hasta=&almacenId=` → `{kardexId, fecha, origen, destino, descripcion,
  gastosAdicionales, costoTotal, partidaId}`; `GET /api/traslados/{kardexId}` → más líneas `{lote, producto,
  cantidad, costoUnitario}` y la partida completa.

## 10. Personal y planillas (RF-PER)

- Empleados: CRUD existente + `POST /api/empleados/{id}/activar` y `/inactivar`. DUI e ISSS únicos → `409` con mensaje
  legible (RF-PER-03). `GET /api/empleados?estado=&departamentoId=&buscar=`.
- `GET /api/empleados/seleccion` (todos) → `[{id, nombreCompleto, departamento}]` de empleados Activos.
- Planillas: lo existente. `dias` por defecto 30, horas extra 0 (RF-PER-11/12). "Procesada" = estado `Pagado`.
  `POST /api/planillas/{id}/pagar` guarda `partida.id_planilla`.
  `GET /api/planillas?estado=&desde=&hasta=`.

## 11. Contabilidad (RF-LD, RF-CC, RF-HT, RF-GEN-10..20)

### Catálogo real (BD de Oscar) y cuentas de configuración

Niveles por longitud del código: 1 dígito = nivel 1, 2 = nivel 2, 4 = nivel 3, 5 = nivel 4. Clase 4 = costos y gastos
(deudoras), 5 = ingresos (acreedoras), 6 = liquidadora, 7 = orden.

| Clave de `contabilidad.properties` | Código |
|---|---|
| `cuenta.caja` | 1101 |
| `cuenta.bancos` | 11031 |
| `cuenta.clientes` | 1104 |
| `cuenta.inventario` | 11081 |
| `cuenta.iva.credito` | 1110 |
| `cuenta.proveedores` | 2101 |
| `cuenta.iva.debito` | 2108 |
| `cuenta.retenciones.isss` / `.afp` / `.renta` | 21061 / 21062 / 21063 |
| `cuenta.ventas` | 5101 |
| `cuenta.devoluciones.ventas` | 5102 |
| `cuenta.rebajas.ventas` | 5103 |
| `cuenta.costo.ventas` | 4101 |
| `cuenta.descuentos.compras` | 4105 |
| `cuenta.sueldos` | 41071 |
| `cuenta.otros.gastos` (ajustes, pérdida en devoluciones) | 4109 |
| `cuenta.otros.ingresos` (sobrantes) | 5203 |
| `cuenta.gastos.financieros` | 4108 |
| `cuenta.productos.financieros` | 5201 |
| `cuenta.perdidas.ganancias` | 6101 |
| `cuenta.utilidad.ejercicio` / `cuenta.perdida.ejercicio` | 3201 / 3202 |

### Catálogo de cuentas

- `GET /api/cuentas/arbol` → árbol `{id, codigo, nombre, naturaleza, activo, imputable, nivel, hijos:[...]}`.
- Crear subcuenta: el código debe empezar por el del padre y tener el largo del nivel siguiente (nivel 3 = 4 dígitos,
  nivel 4 = 5). Código único (RF-CC-11). Naturaleza por defecto la del padre.
- `DELETE /api/cuentas/{id}`: solo si no tiene subcuentas ni movimientos (RF-CC-12); si no, `422`.
- Activar/desactivar existentes. Partidas solo en cuentas activas e imputables (RF-CC-10; hay trigger en la BD).

### Libro diario

- `GET /api/partidas?desde=&hasta=&tipo=&terceroId=&origen=compra|venta|traslado|ajuste|planilla|pago|devolucion|manual|cierre|apertura&periodoId=`
  ordenadas por fecha y id; cada fila con `{id, fecha, concepto, tipo, origen, tercero, totalCargo, totalAbono}`.
  `GET /api/partidas/{id}` con líneas `{cuenta:{id,codigo,nombre}, cargo, abono}` y referencias de origen.
- `POST /api/partidas` (manual, RF-LD-04/05/07): `{fecha, concepto, tipo: Diario|Ingreso|Egreso|Ajuste, terceroId?,
  lineas:[{cuentaId, cargo, abono}]}`. Debe cuadrar (RF-LD-10). No se permiten tipos `Cierre`/`Apertura` ni fechas en
  un periodo `Cerrado` (`422`). Toda partida (manual o automática) guarda `id_periodo` según su fecha.
- Terceros: CRUD existente. `GET /api/terceros/{id}/partidas` (RF-LD-08).

### Periodos, hoja de trabajo y cierre

Periodo trimestral con ciclo que inicia en julio (tabla `periodo`; año fiscal 2026 = jul-2026 a jun-2027, ya sembrado).
Cada periodo se calcula **solo con las partidas de sus fechas**; sus saldos iniciales vienen de su partida `Apertura`
(la partida 1 "Inicialización de operaciones" del 2026-07-01 hace de apertura del primer periodo).

- `GET /api/periodos` y `GET /api/periodos/actual` (el que contiene la fecha de hoy).
- `GET /api/hoja-trabajo?periodoId=` (o `?desde=&hasta=` para una vista mensual, RF-HT-01) →
  ```
  { periodo, desde, hasta,
    filas: [{cuentaId, codigo, nombre, clase,
             comprobacion:{debe, haber},   // saldos sin partidas Ajuste ni Cierre
             ajustes:{debe, haber},        // partidas tipo Ajuste del rango
             ajustado:{debe, haber},
             resultados:{debe, haber},     // clases 4 y 5
             balance:{debe, haber}}],      // clases 1, 2 y 3
    totales: {comprobacion, ajustes, ajustado, resultados, balance},
    utilidad: n, perdida: n,
    cuadrada: {comprobacion: bool, ajustado: bool, balance: bool} }
  ```
  Solo cuentas con movimiento; saldo deudor o acreedor según el signo (no según la naturaleza). La utilidad
  (ingresos − costos y gastos) se agrega a la columna que cuadra resultados y balance. Clases 6 y 7 no entran a los
  estados. Si el periodo está `Cerrado`, devuelve la instantánea `periodo.hoja_trabajo`.
- Partidas de ajuste (RF-HT-04): `POST /api/partidas` con `tipo: "Ajuste"`.
- `POST /api/periodos/{id}/cerrar` (Contabilidad, Admin) (RF-HT-10/11, RF-GEN-16..20):
  1. Exige que los periodos anteriores estén cerrados y que la hoja cuadre (`422` si no).
  2. Partida `Cierre` en `fecha_fin`: lleva a cero las cuentas de clases 4 y 5 contra Pérdidas y ganancias, y salda
     Pérdidas y ganancias contra Utilidad o Pérdida del ejercicio.
  3. Guarda la hoja en `periodo.hoja_trabajo`, `estado = Cerrado`, `fecha_cierre`, `id_partida_cierre`.
  4. Crea la partida `Apertura` del periodo siguiente (en su `fecha_inicio`) con los saldos finales de las clases 1–3
     (y guarda `id_partida_apertura` en el periodo siguiente). Si es el 4.º trimestre, crea los 4 periodos del año
     fiscal siguiente.
  Respuesta: `{periodo, partidaCierre, partidaApertura}`.

## 12. Datos existentes

La BD ya tiene datos de demostración de Oscar (126 cuentas, 15 empleados, 2 almacenes, 1 compra, 1 venta, 2
partidas, 3 kardex). **No borrar ni recrear.** La cuenta 4105 se renombró a "Descuentos Sobre Compras- CR".
