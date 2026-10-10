-- Esquema de la BD contable (solo contexto; NO ejecutar). Fuente: exportación de Supabase.
-- Enums: estado_empleado{Activo,Inactivo,Suspendido} estado_planilla{Pendiente,Pagado}
--        naturaleza_cuenta{Acreedora,Deudora} tipo_movimiento{Entrada,Salida,Traslado,Ajuste}
--        tipo_partida{Diario,Ingreso,Egreso,Ajuste,Cierre}

CREATE TABLE public.departamento (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre character varying NOT NULL,
  descripcion text,
  CONSTRAINT departamento_pkey PRIMARY KEY (id)
);
CREATE TABLE public.empleado (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre character varying NOT NULL,
  apellido character varying NOT NULL,
  fech_nac date,
  id_departamento bigint NOT NULL,
  salario numeric NOT NULL DEFAULT 0 CHECK (salario >= 0::numeric),
  estado estado_empleado NOT NULL DEFAULT 'Activo',
  numero_isss character varying NOT NULL UNIQUE,
  dui character varying NOT NULL UNIQUE,
  CONSTRAINT empleado_pkey PRIMARY KEY (id),
  CONSTRAINT empleado_id_departamento_fkey FOREIGN KEY (id_departamento) REFERENCES public.departamento(id)
);
CREATE TABLE public.cuentas (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  codigo integer NOT NULL UNIQUE,
  name character varying NOT NULL,
  naturaleza naturaleza_cuenta NOT NULL,
  activo boolean NOT NULL DEFAULT true,
  imputable boolean NOT NULL DEFAULT false,
  id_padre bigint,
  CONSTRAINT cuentas_pkey PRIMARY KEY (id),
  CONSTRAINT cuentas_id_padre_fkey FOREIGN KEY (id_padre) REFERENCES public.cuentas(id)
);
CREATE TABLE public.tercero (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre character varying NOT NULL,
  descripcion character varying,
  CONSTRAINT tercero_pkey PRIMARY KEY (id)
);
CREATE TABLE public.cliente (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre character varying NOT NULL,
  descripcion character varying,
  nit character varying NOT NULL UNIQUE,
  CONSTRAINT cliente_pkey PRIMARY KEY (id)
);
CREATE TABLE public.proveedor (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre character varying NOT NULL,
  descripcion character varying,
  nit character varying NOT NULL UNIQUE,
  CONSTRAINT proveedor_pkey PRIMARY KEY (id)
);
CREATE TABLE public.unidad_medida (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre character varying NOT NULL,
  abrev character varying NOT NULL,
  CONSTRAINT unidad_medida_pkey PRIMARY KEY (id)
);
CREATE TABLE public.tipo_producto (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre character varying NOT NULL,
  id_padre bigint,
  CONSTRAINT tipo_producto_pkey PRIMARY KEY (id),
  CONSTRAINT tipo_producto_id_padre_fkey FOREIGN KEY (id_padre) REFERENCES public.tipo_producto(id)
);
CREATE TABLE public.producto (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre character varying NOT NULL,
  id_unidad_medida bigint NOT NULL,
  id_tipo bigint NOT NULL,
  descripcion text,
  CONSTRAINT producto_pkey PRIMARY KEY (id),
  CONSTRAINT producto_id_unidad_medida_fkey FOREIGN KEY (id_unidad_medida) REFERENCES public.unidad_medida(id),
  CONSTRAINT producto_id_tipo_fkey FOREIGN KEY (id_tipo) REFERENCES public.tipo_producto(id)
);
CREATE TABLE public.lote (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_producto bigint NOT NULL,
  num_lote character varying NOT NULL,
  fech_caducidad date,
  CONSTRAINT lote_pkey PRIMARY KEY (id),
  CONSTRAINT lote_id_producto_fkey FOREIGN KEY (id_producto) REFERENCES public.producto(id)
);
CREATE TABLE public.almacen (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre character varying NOT NULL,
  descripcion text,
  estado boolean NOT NULL,
  CONSTRAINT almacen_pkey PRIMARY KEY (id)
);
CREATE TABLE public.existencias (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_lote bigint NOT NULL,
  id_almacen bigint NOT NULL,
  cantidad numeric NOT NULL DEFAULT 0 CHECK (cantidad >= 0::numeric),
  CONSTRAINT existencias_pkey PRIMARY KEY (id),
  CONSTRAINT existencias_id_lote_fkey FOREIGN KEY (id_lote) REFERENCES public.lote(id),
  CONSTRAINT existencias_id_almacen_fkey FOREIGN KEY (id_almacen) REFERENCES public.almacen(id)
);
CREATE TABLE public.compra (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  fecha date NOT NULL DEFAULT CURRENT_DATE,
  id_proveedor bigint NOT NULL,
  id_empleado bigint NOT NULL,
  descripcion character varying,
  CONSTRAINT compra_pkey PRIMARY KEY (id),
  CONSTRAINT compra_id_proveedor_fkey FOREIGN KEY (id_proveedor) REFERENCES public.proveedor(id),
  CONSTRAINT compra_id_empleado_fkey FOREIGN KEY (id_empleado) REFERENCES public.empleado(id)
);
CREATE TABLE public.compra_detalle (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_compra bigint NOT NULL,
  id_lote bigint NOT NULL,
  id_almacen bigint NOT NULL,
  cantidad numeric NOT NULL CHECK (cantidad > 0::numeric),
  precio numeric NOT NULL CHECK (precio >= 0::numeric),
  CONSTRAINT compra_detalle_pkey PRIMARY KEY (id),
  CONSTRAINT compra_detalle_id_compra_fkey FOREIGN KEY (id_compra) REFERENCES public.compra(id),
  CONSTRAINT compra_detalle_id_lote_fkey FOREIGN KEY (id_lote) REFERENCES public.lote(id),
  CONSTRAINT compra_detalle_id_almacen_fkey FOREIGN KEY (id_almacen) REFERENCES public.almacen(id)
);
CREATE TABLE public.ventas (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  fecha date NOT NULL DEFAULT CURRENT_DATE,
  id_cliente bigint NOT NULL,
  id_empleado bigint NOT NULL,
  descripcion character varying,
  CONSTRAINT ventas_pkey PRIMARY KEY (id),
  CONSTRAINT ventas_id_cliente_fkey FOREIGN KEY (id_cliente) REFERENCES public.cliente(id),
  CONSTRAINT ventas_id_empleado_fkey FOREIGN KEY (id_empleado) REFERENCES public.empleado(id)
);
CREATE TABLE public.ventas_detalle (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_venta bigint NOT NULL,
  id_lote bigint NOT NULL,
  id_almacen bigint NOT NULL,
  cantidad numeric NOT NULL CHECK (cantidad > 0::numeric),
  precio numeric NOT NULL CHECK (precio >= 0::numeric),
  CONSTRAINT ventas_detalle_pkey PRIMARY KEY (id),
  CONSTRAINT ventas_detalle_id_venta_fkey FOREIGN KEY (id_venta) REFERENCES public.ventas(id),
  CONSTRAINT ventas_detalle_id_lote_fkey FOREIGN KEY (id_lote) REFERENCES public.lote(id),
  CONSTRAINT ventas_detalle_id_almacen_fkey FOREIGN KEY (id_almacen) REFERENCES public.almacen(id)
);
CREATE TABLE public.kardex (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  tip_mov tipo_movimiento NOT NULL,
  origen bigint,
  destino bigint,
  fecha timestamp with time zone NOT NULL DEFAULT now(),
  id_venta bigint,
  id_compra bigint,
  descripcion character varying,
  CONSTRAINT kardex_pkey PRIMARY KEY (id),
  CONSTRAINT kardex_origen_fkey FOREIGN KEY (origen) REFERENCES public.almacen(id),
  CONSTRAINT kardex_destino_fkey FOREIGN KEY (destino) REFERENCES public.almacen(id),
  CONSTRAINT kardex_id_venta_fkey FOREIGN KEY (id_venta) REFERENCES public.ventas(id),
  CONSTRAINT kardex_id_compra_fkey FOREIGN KEY (id_compra) REFERENCES public.compra(id)
);
CREATE TABLE public.kardex_detalle (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_kardex bigint NOT NULL,
  id_lote bigint NOT NULL,
  cantidad numeric NOT NULL CHECK (cantidad > 0::numeric),
  precio_costo numeric NOT NULL CHECK (precio_costo >= 0::numeric),
  CONSTRAINT kardex_detalle_pkey PRIMARY KEY (id),
  CONSTRAINT kardex_detalle_id_kardex_fkey FOREIGN KEY (id_kardex) REFERENCES public.kardex(id),
  CONSTRAINT kardex_detalle_id_lote_fkey FOREIGN KEY (id_lote) REFERENCES public.lote(id)
);
CREATE TABLE public.partida (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  fecha date NOT NULL DEFAULT CURRENT_DATE,
  concepto character varying NOT NULL,
  tipo tipo_partida NOT NULL,
  id_compra bigint,
  id_venta bigint,
  id_tercero bigint,
  CONSTRAINT partida_pkey PRIMARY KEY (id),
  CONSTRAINT partida_id_compra_fkey FOREIGN KEY (id_compra) REFERENCES public.compra(id),
  CONSTRAINT partida_id_venta_fkey FOREIGN KEY (id_venta) REFERENCES public.ventas(id),
  CONSTRAINT partida_id_tercero_fkey FOREIGN KEY (id_tercero) REFERENCES public.tercero(id)
);
CREATE TABLE public.partida_detalle (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_partida bigint NOT NULL,
  id_cuenta bigint NOT NULL,
  abono numeric NOT NULL DEFAULT 0 CHECK (abono >= 0::numeric),
  cargo numeric NOT NULL DEFAULT 0 CHECK (cargo >= 0::numeric),
  CONSTRAINT partida_detalle_pkey PRIMARY KEY (id),
  CONSTRAINT partida_detalle_id_partida_fkey FOREIGN KEY (id_partida) REFERENCES public.partida(id),
  CONSTRAINT partida_detalle_id_cuenta_fkey FOREIGN KEY (id_cuenta) REFERENCES public.cuentas(id)
);
CREATE TABLE public.planilla (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  fecha_inicio timestamp with time zone NOT NULL,
  fecha_fin timestamp with time zone NOT NULL,
  fecha_pago timestamp with time zone NOT NULL,
  descripcion character varying,
  estado estado_planilla NOT NULL DEFAULT 'Pendiente',
  CONSTRAINT planilla_pkey PRIMARY KEY (id)
);
CREATE TABLE public.planilla_detalle (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  planilla_id bigint NOT NULL,
  empleado_id bigint NOT NULL,
  dias smallint NOT NULL DEFAULT 30,
  horasx_diur smallint NOT NULL DEFAULT 0,
  horasx_noct smallint NOT NULL DEFAULT 0,
  sueldo_base numeric NOT NULL,
  CONSTRAINT planilla_detalle_pkey PRIMARY KEY (id),
  CONSTRAINT planilla_detalle_planilla_id_fkey FOREIGN KEY (planilla_id) REFERENCES public.planilla(id),
  CONSTRAINT planilla_detalle_empleado_id_fkey FOREIGN KEY (empleado_id) REFERENCES public.empleado(id)
);
