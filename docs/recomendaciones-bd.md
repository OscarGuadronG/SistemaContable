# Recomendaciones para la base de datos

Este documento **propone** cambios al esquema de `docs/esquema-bd.sql`. **Nada de esto se ha aplicado** a ninguna base
de datos: el backend se desarrolló y se probó solo contra H2 en memoria (ver `backend/README.md`). Cada propuesta
indica qué problema resuelve, qué hace hoy el backend para mitigarlo y el SQL sugerido. Antes de ejecutar cualquier
sentencia sobre datos reales, se recomienda revisarla con quien administra la base, probarla en una copia y comprobar
que los datos existentes la cumplen (se incluyen consultas de verificación).

Resumen por prioridad:

| Prioridad | Propuesta |
|---|---|
| Alta | `UNIQUE (id_lote, id_almacen)` en `existencias` |
| Alta | `CHECK` de un solo importe por línea en `partida_detalle` |
| Media | `UNIQUE (planilla_id, empleado_id)` en `planilla_detalle` |
| Media | Índices sobre las columnas de clave foránea que usan las consultas |
| Media | Otras restricciones de coherencia (`partida`, `kardex`, `planilla`, `cuentas`) |
| Informativa | Los enums de PostgreSQL requieren `stringtype=unspecified` en el driver |
| Informativa | Exposición de `public` en Supabase (RLS) |

---

## 1. `UNIQUE (id_lote, id_almacen)` en `existencias` (alta)

**Problema.** `existencias` guarda el stock de un lote en un almacén, pero el esquema no impide dos filas para el mismo
par. Si ocurriera, el stock quedaría repartido y la lógica de "buscar la fila y actualizarla" sería ambigua.

**Qué hace hoy el backend.** `ExistenciasDAO.findForUpdate` busca la fila con `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`)
antes de actualizarla, de modo que dos movimientos simultáneos sobre una fila **existente** se serializan. Ese bloqueo
**no protege la primera inserción**: si dos transacciones intentan crear a la vez la fila de un par que aún no existe,
las dos pueden insertar. La restricción única es la única defensa completa.

**Verificación previa** (debe devolver cero filas):

```sql
SELECT id_lote, id_almacen, count(*), sum(cantidad)
FROM public.existencias
GROUP BY id_lote, id_almacen
HAVING count(*) > 1;
```

**Propuesta (no aplicada):**

```sql
ALTER TABLE public.existencias
  ADD CONSTRAINT existencias_lote_almacen_key UNIQUE (id_lote, id_almacen);
```

Con la restricción, la segunda inserción simultánea falla con SQLSTATE `23505` y la API responde `409`
(`CONFLICTO_INTEGRIDAD`) en lugar de duplicar el stock; el cliente puede reintentar. Además, el índice único cubre las
búsquedas por `id_lote` (primera columna).

## 2. `CHECK` de un solo importe por línea en `partida_detalle` (alta)

**Problema.** Cada línea de una partida debe tener **exactamente uno** de `cargo` o `abono` mayor que cero. El esquema
solo exige que cada uno sea `>= 0`, así que admite líneas con ambos en cero o con ambos positivos.

**Qué hace hoy el backend.** `PartidaService` valida esa regla (`ReglasPartida`) antes de escribir, y es el único camino
para crear partidas. Un `INSERT` directo en la base la saltaría.

**Verificación previa** (debe devolver cero filas):

```sql
SELECT id FROM public.partida_detalle
WHERE NOT ((cargo > 0 AND abono = 0) OR (cargo = 0 AND abono > 0));
```

**Propuesta (no aplicada):**

```sql
ALTER TABLE public.partida_detalle
  ADD CONSTRAINT partida_detalle_un_solo_importe
  CHECK ((cargo > 0 AND abono = 0) OR (cargo = 0 AND abono > 0));
```

## 3. `UNIQUE (planilla_id, empleado_id)` en `planilla_detalle` (media)

**Problema.** Un empleado debe tener un solo detalle por planilla; si hubiera dos, se le pagaría dos veces.

**Qué hace hoy el backend.** `PlanillaService` consulta `existePorPlanillaYEmpleado` y bloquea la planilla
(`FOR UPDATE`) antes de agregar detalles, por lo que las operaciones sobre una misma planilla se serializan.

**Verificación previa** (cero filas):

```sql
SELECT planilla_id, empleado_id, count(*)
FROM public.planilla_detalle
GROUP BY planilla_id, empleado_id
HAVING count(*) > 1;
```

**Propuesta (no aplicada):**

```sql
ALTER TABLE public.planilla_detalle
  ADD CONSTRAINT planilla_detalle_planilla_empleado_key UNIQUE (planilla_id, empleado_id);
```

## 4. Índices sobre claves foráneas (media)

PostgreSQL **no** crea índices automáticamente para las columnas de clave foránea. Estas son las columnas que usan las
consultas del backend (búsqueda por documento, por lote, por almacén, por rango de fechas, comprobación de referencias
antes de borrar y recorrido de jerarquías). Con pocos datos no hace falta nada; conviene crearlos cuando las tablas
crezcan. Se omiten los que ya cubre una restricción única propuesta arriba.

**Propuesta (no aplicada):**

```sql
-- Detalle de documentos y movimientos
CREATE INDEX IF NOT EXISTS ix_compra_detalle_id_compra   ON public.compra_detalle (id_compra);
CREATE INDEX IF NOT EXISTS ix_compra_detalle_id_lote     ON public.compra_detalle (id_lote);
CREATE INDEX IF NOT EXISTS ix_compra_detalle_id_almacen  ON public.compra_detalle (id_almacen);
CREATE INDEX IF NOT EXISTS ix_ventas_detalle_id_venta    ON public.ventas_detalle (id_venta);
CREATE INDEX IF NOT EXISTS ix_ventas_detalle_id_lote     ON public.ventas_detalle (id_lote);
CREATE INDEX IF NOT EXISTS ix_ventas_detalle_id_almacen  ON public.ventas_detalle (id_almacen);
CREATE INDEX IF NOT EXISTS ix_kardex_detalle_id_kardex   ON public.kardex_detalle (id_kardex);
CREATE INDEX IF NOT EXISTS ix_kardex_detalle_id_lote     ON public.kardex_detalle (id_lote);

-- Cabeceras: relaciones y fechas
CREATE INDEX IF NOT EXISTS ix_kardex_origen              ON public.kardex (origen);
CREATE INDEX IF NOT EXISTS ix_kardex_destino             ON public.kardex (destino);
CREATE INDEX IF NOT EXISTS ix_kardex_id_compra           ON public.kardex (id_compra);
CREATE INDEX IF NOT EXISTS ix_kardex_id_venta            ON public.kardex (id_venta);
CREATE INDEX IF NOT EXISTS ix_kardex_fecha               ON public.kardex (fecha);
CREATE INDEX IF NOT EXISTS ix_compra_id_proveedor        ON public.compra (id_proveedor);
CREATE INDEX IF NOT EXISTS ix_compra_id_empleado         ON public.compra (id_empleado);
CREATE INDEX IF NOT EXISTS ix_ventas_id_cliente          ON public.ventas (id_cliente);
CREATE INDEX IF NOT EXISTS ix_ventas_id_empleado         ON public.ventas (id_empleado);

-- Contabilidad (la balanza agrupa partida_detalle por cuenta y filtra partidas por fecha)
CREATE INDEX IF NOT EXISTS ix_partida_fecha              ON public.partida (fecha);
CREATE INDEX IF NOT EXISTS ix_partida_id_compra          ON public.partida (id_compra);
CREATE INDEX IF NOT EXISTS ix_partida_id_venta           ON public.partida (id_venta);
CREATE INDEX IF NOT EXISTS ix_partida_id_tercero         ON public.partida (id_tercero);
CREATE INDEX IF NOT EXISTS ix_partida_detalle_id_partida ON public.partida_detalle (id_partida);
CREATE INDEX IF NOT EXISTS ix_partida_detalle_id_cuenta  ON public.partida_detalle (id_cuenta);

-- Catálogos y jerarquías
CREATE INDEX IF NOT EXISTS ix_existencias_id_almacen     ON public.existencias (id_almacen);
CREATE INDEX IF NOT EXISTS ix_lote_id_producto           ON public.lote (id_producto);
CREATE INDEX IF NOT EXISTS ix_producto_id_tipo           ON public.producto (id_tipo);
CREATE INDEX IF NOT EXISTS ix_producto_id_unidad_medida  ON public.producto (id_unidad_medida);
CREATE INDEX IF NOT EXISTS ix_tipo_producto_id_padre     ON public.tipo_producto (id_padre);
CREATE INDEX IF NOT EXISTS ix_cuentas_id_padre           ON public.cuentas (id_padre);
CREATE INDEX IF NOT EXISTS ix_empleado_id_departamento   ON public.empleado (id_departamento);
CREATE INDEX IF NOT EXISTS ix_planilla_detalle_empleado  ON public.planilla_detalle (empleado_id);
```

## 5. Otras restricciones de coherencia (media)

Ninguna es imprescindible porque el backend ya valida estas reglas; sirven como red de seguridad ante escrituras que no
pasen por él.

```sql
-- Una partida se liga a una compra o a una venta, nunca a ambas.
ALTER TABLE public.partida
  ADD CONSTRAINT partida_compra_o_venta CHECK (id_compra IS NULL OR id_venta IS NULL);

-- Un movimiento de kardex debe tener los almacenes que su tipo exige.
ALTER TABLE public.kardex
  ADD CONSTRAINT kardex_almacenes_segun_tipo CHECK (
       (tip_mov = 'Entrada'  AND destino IS NOT NULL AND origen IS NULL)
    OR (tip_mov = 'Salida'   AND origen  IS NOT NULL AND destino IS NULL)
    OR (tip_mov = 'Traslado' AND origen  IS NOT NULL AND destino IS NOT NULL AND origen <> destino)
    OR (tip_mov = 'Ajuste'   AND (origen IS NULL) <> (destino IS NULL))
  );

-- Fechas coherentes de la planilla y límites del detalle.
ALTER TABLE public.planilla
  ADD CONSTRAINT planilla_fechas_ordenadas CHECK (fecha_inicio <= fecha_fin AND fecha_fin <= fecha_pago);
ALTER TABLE public.planilla_detalle
  ADD CONSTRAINT planilla_detalle_rangos CHECK (dias BETWEEN 0 AND 31 AND horasx_diur >= 0 AND horasx_noct >= 0);

-- Una cuenta no puede ser su propio padre.
ALTER TABLE public.cuentas
  ADD CONSTRAINT cuentas_padre_distinto CHECK (id_padre IS NULL OR id_padre <> id);
```

Notas:

- El `CHECK` de `kardex` reproduce las invariantes de `ReglasKardex` del backend: un ajuste lleva exactamente un almacén
  (`destino` si entra stock, `origen` si sale). **Verificar los movimientos ya existentes** antes de aplicarlo
  (`SELECT id FROM kardex WHERE NOT (...)` con la misma condición). Los ciclos largos en las jerarquías (`cuentas`, `tipo_producto`) no se pueden impedir con un
  `CHECK`; los valida el servicio.
- Si el negocio no admite dos lotes con el mismo número para un producto, `UNIQUE (id_producto, num_lote)` en `lote`.
- Las partidas contables son un libro: opcionalmente se puede impedir `UPDATE` y `DELETE` sobre `partida` y
  `partida_detalle` con un trigger que lance una excepción (hoy el backend solo ofrece crear y consultar).

## 6. Enums de PostgreSQL y `stringtype=unspecified` (informativa)

El esquema usa tipos `ENUM` nativos (`estado_empleado`, `estado_planilla`, `naturaleza_cuenta`, `tipo_movimiento`,
`tipo_partida`). El driver JDBC de PostgreSQL envía por defecto los `String` como `varchar`, y PostgreSQL **no convierte
implícitamente** `varchar` a un enum: un `INSERT` con una columna enum falla con *"column ... is of type ... but
expression is of type character varying"*. Las entidades JPA mapean los enums con `@Enumerated(STRING)`, así que el
datasource debe enviar los textos sin tipo para que PostgreSQL los resuelva:

- En Liberty (`server.xml`, ya incluido): `<properties.postgresql ... stringType="unspecified"/>`.
- En una URL JDBC: `jdbc:postgresql://host:5432/db?stringtype=unspecified`.
- En las propiedades de persistencia de EclipseLink, si se usara un datasource propio: la misma propiedad del driver.

Alternativa **no recomendada**: crear casts implícitos `varchar -> enum` en la base (`CREATE CAST ... WITH INOUT AS
IMPLICIT`); tienen efecto global y pueden ocultar errores de tipo.

Las pruebas del backend usan H2, que no tiene enums nativos de PostgreSQL: **este punto no se ha ejercitado contra un
PostgreSQL real** y debe comprobarse en la primera ejecución con la base verdadera (insertar un empleado y una partida
basta).

## 7. Exposición de `public` en Supabase (informativa)

Supabase publica el esquema `public` mediante su API de datos (PostgREST). El backend se conecta por JDBC con un rol de
base de datos, no por esa API. Si la API de datos no se va a utilizar, conviene **activar RLS en cada tabla** (sin
políticas, deniega todo a los roles `anon` y `authenticated`) para que las tablas contables no queden accesibles con la
clave pública del proyecto:

```sql
ALTER TABLE public.<tabla> ENABLE ROW LEVEL SECURITY;  -- repetir para cada tabla
```

El rol con el que se conecta el backend debe poder saltarse RLS (por ejemplo `postgres`) o tener políticas propias;
conviene confirmarlo antes de activarlo.

## 8. Tipos numéricos (informativa)

Los importes y cantidades son `numeric` sin precisión. El backend redondea los importes a 2 decimales (`Dinero`, una
sola función de redondeo `HALF_UP`) y el costo unitario a 4. Si se quiere que la base lo refuerce, una opción es
`numeric(18,4)` para cantidades y costos y `numeric(18,2)` para importes de partidas; cambiar el tipo de columnas con
datos exige una migración planificada y no es necesario para el funcionamiento actual.
