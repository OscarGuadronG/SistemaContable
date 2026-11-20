**Requerimientos de App**

**Roles (Usuario y contraseña usando Supabase Auth)**

- Ventas

- Bodega

- Contabilidad

- Administrador

- RRHH

**Pantalla Inventario (tablas: producto, unidad_medida, tipo_producto,
existencias, lote)**

- \- RF-INV-01: Mostrar el inventario general de productos registrados.

- \- RF-INV-02: Mostrar información básica de cada producto.

- \- RF-INV-03: Permitir buscar productos por nombre o categoría.

- \- RF-INV-04: Permitir filtrar productos según su categoría y
  disponibilidad.

- \- RF-INV-05: Permitir registrar nuevos productos (Bodega).

Al registrar nuevo productos debe ser posible crear unidad_medida y
tipo_producto de ser necesario.

- \- RF-INV-06: Permitir actualizar las existencias mediante ajustes
  autorizados (Bodega).

- \- RF-INV-07: Permitir consultar inventario por almacen.

- **Restricciones de acceso**

- \- Todos los roles pueden consultar el inventario.

- \- Bodega puede registrar y administrar productos.

- \- Ventas y Contabilidad tienen acceso de consulta.

- \- Administrador tiene acceso completo.

**Pantalla Compra (tablas: compra, compra_detalle, Kardex,
Kardex_detalle, partida)**

- RF-COM-01: Registrar compras asociadas a un proveedor y empleado.

- RF-COM-02: Agregar productos, lotes, cantidades, precios de
  adquisición y almacenes al detalle de compra.

- RF-COM-03: Permitir registrar nuevos lotes durante el proceso de
  compra cuando corresponda.

Nota: en compra_detalle se almacena el precio de la compra ej. \$80

- RF-COM-04: Registrar los gastos adicionales asociados a la adquisición
  en kardex_detalle.

Nota: en Kardex_detalle se almacena el precio de la compra + gastos
sobre compra (fletes y acarreos), ej. \$100 (\$20 en fletes y acarreos)

- RF-COM-05: Distribuir los gastos adicionales de adquisición entre los
  productos para calcular su costo de incorporación al inventario.

<!-- -->

- RF-COM-06: Generar movimientos de entrada en Kardex por la recepción
  de mercancía.

El destino es un almacen, el origen es null para indicar una entrada,
tipo Entrada

- RF-COM-07: Actualizar las existencias de los lotes en sus respectivos
  almacenes.

- RF-COM-08: Generar automáticamente la partida contable
  correspondiente.

- RF-COM-09: Permitir registrar compras al contado, crédito y cheque.

Si hubo descuento sobre compra se registra en la partida de descuentos
sobre compra

- RF-COM-10: Consultar el Libro de Compras.

Se arma con los datos de cada compra en un periodo

- RF-COM-11: Gestionar proveedores.

- RF-COM-12: Gestionar devoluciones a proveedores.

Solo se admiten devoluciones con un tiempo máximo de 1 mes con un
porcentaje de compensación variable.

- RF-COM-13: Consultar el historial de devoluciones.

- **Restricciones de acceso**

- Bodega: registro de compras, recepción de mercancía y devoluciones.

- Contabilidad: consulta del Libro de Compras, control financiero y
  gestión de proveedores.

- Administrador: acceso general y autorización de operaciones
  restringidas.

- Ventas: sin acceso a la gestión de compras.

**Pantalla Ventas (tablas: venta, venta_detalle, Kardex, Kardex_detalle,
partida)**

- RF-VEN-01: Permitir registrar nuevas ventas asociadas a un cliente y
  empleado.

- RF-VEN-02: Permitir seleccionar el almacén desde el cual se realizará
  la venta.

- RF-VEN-03: Mostrar los productos y existencias disponibles en el
  almacén seleccionado.

- RF-VEN-04: Permitir agregar productos y cantidades al detalle de
  venta.

- RF-VEN-05: Determinar el precio de venta según el costo promedio y el
  margen o recargo de ganancia configurado.

- RF-VEN-06: Permitir aplicar descuentos generales.

- RF-VEN-07: Validar que las cantidades solicitadas no superen las
  existencias disponibles.

- RF-VEN-08: Aplicar FIFO del almacen para determinar los lotes de los
  cuales se descontará la mercancía.

- RF-VEN-09: Generar automáticamente los movimientos de salida en
  Kardex.

- RF-VEN-10: Actualizar las existencias de los lotes correspondientes.

- RF-VEN-11: Calcular el costo de venta según los costos reales de los
  lotes utilizados.

- RF-VEN-12: Generar las partidas contables correspondientes,
  contemplando IVA y modalidad de pago.

- RF-VEN-13: Permitir registrar ventas al contado, crédito y cheque.

- RF-VEN-14: Permitir consultar y gestionar el perfil de los clientes.

- RF-VEN-15: Permitir consultar el Libro de Ventas.

- RF-VEN-16: Permitir registrar devoluciones de ventas dentro del plazo
  establecido.

- RF-VEN-17: Generar los movimientos de inventario y ajustes contables
  correspondientes a las devoluciones.

- RF-VEN-18: Permitir consultar el historial de ventas y devoluciones.

- **Restricciones de acceso**

- Ventas: registrar ventas, gestionar clientes y aplicar descuentos
  autorizados.

- Bodega: consultar existencias y gestionar devoluciones con permisos.

- Contabilidad: consultar el Libro de Ventas y supervisar los registros
  contables.

- Administrador: acceso completo y autorización de operaciones
  restringidas.

> **Almacenes**
>
> RF-ALM-01: Consultar los almacenes registrados.

- \- RF-ALM-02: Registrar nuevos almacenes indicando nombre y
  descripción.

- \- RF-ALM-03: Modificar la información de los almacenes registrados.

- \- RF-ALM-04: Habilitar e inhabilitar almacenes.

- \- RF-ALM-05: Permitir seleccionar un almacén de origen y uno de
  destino para realizar traslados de inventario.

- \- RF-ALM-06: Agregar productos, lotes y cantidades al detalle del
  traslado.

- \- RF-ALM-07: Validar que las cantidades a trasladar no superen las
  existencias disponibles en el almacén de origen.

- \- RF-ALM-08: Registrar los costos adicionales asociados al traslado
  de inventario, como fletes y acarreos.

- Nota: En kardex_detalle se almacena el costo del inventario trasladado
  más los costos adicionales del traslado.

- \- RF-ALM-09: Generar automáticamente los movimientos de salida y
  entrada en Kardex por el traslado de inventario.

- El origen corresponde al almacén del que sale el inventario y el
  destino al almacén que lo recibe. El tipo de movimiento es Traslado.

- \- RF-ALM-10: Actualizar las existencias de los lotes en sus
  respectivos almacenes.

- \- RF-ALM-11: Generar automáticamente la partida contable
  correspondiente al traslado.

- Nota: Como el traslado no está asociado a una compra ni a una venta,
  las referencias id_compra e id_venta de la partida quedan en NULL.

- \- RF-ALM-12: Consultar el historial de traslados entre almacenes.

- \- RF-ALM-13: Consultar el detalle de cada traslado, incluyendo
  almacén de origen, almacén de destino, productos, lotes, cantidades y
  costos.

- \- RF-ALM-14: Consultar las partidas contables generadas por los
  traslados.

- \- **Restricciones de acceso**

- \- Bodega: consulta de almacenes y registro de traslados de
  inventario.

- \- Contabilidad: consulta de traslados y sus respectivas partidas
  contables.

- \- Administrador: gestión de almacenes, habilitación e inhabilitación
  y acceso general a las operaciones.

- \- Ventas: consulta de almacenes y existencias, sin acceso a la
  gestión de almacenes ni al registro de traslados.

Personal  
- RF-PER-01: Consultar el listado de empleados registrados.

\- RF-PER-02: Registrar nuevos empleados solicitando nombres, apellidos,
fecha de nacimiento, departamento, salario, número de ISSS y DUI.

\- RF-PER-03: Validar que el DUI y el número de ISSS no estén
registrados previamente.

\- RF-PER-04: Modificar la información de los empleados registrados.

\- RF-PER-05: Gestionar el estado de los empleados, permitiendo activar
o inactivar al personal.

\- RF-PER-06: Consultar la información de cada empleado, incluyendo
departamento, salario y estado laboral.

\- RF-PER-07: Generar la planilla de pagos al final de cada mes.

\- RF-PER-08: Registrar el período de la planilla mediante fecha de
inicio, fecha de finalización y fecha de pago.

\- RF-PER-09: Agregar automáticamente a los empleados activos a la
planilla correspondiente.

\- RF-PER-10: Calcular el sueldo base de cada empleado según su salario
registrado y los días correspondientes al período de pago.

\- RF-PER-11: Permitir modificar los días laborados de cada empleado,
tomando como valor predeterminado 30 días.

\- RF-PER-12: Permitir registrar horas extras diurnas y nocturnas,
estableciendo 0 horas como valor predeterminado para ambas.

\- RF-PER-13: Permitir modificar los días laborados y las horas extras
antes de generar la planilla definitiva.

\- RF-PER-14: Registrar el detalle de pago por empleado, incluyendo días
laborados, horas extras diurnas, horas extras nocturnas y sueldo base.

\- RF-PER-15: Gestionar el estado de la planilla, permitiendo
identificar si se encuentra pendiente o procesada, según los estados
definidos en el sistema.

\- RF-PER-16: Consultar el historial de planillas generadas y sus
respectivos detalles por empleado.

\- RF-PER-17: Generar los asientos contables usando la tabla de partida.

\- **Restricciones de acceso**

- RRHH: gestión de empleados, modificación de salarios, generación y
  consulta de planillas.

- Contabilidad: consulta de planillas y control de los registros
  contables relacionados con los pagos.

- Administrador: acceso general y autorización de operaciones
  restringidas.

- Ventas: sin acceso a la gestión de personal ni a las planillas.

- Bodega: sin acceso a la gestión de personal ni a las planillas.

\- RF-LD-01: Consultar todas las partidas contables generadas
automáticamente por las diferentes operaciones del sistema, incluyendo
compras, ventas, traslados de inventario y planillas.

\- RF-LD-02: Consultar las partidas contables ordenadas cronológicamente
por fecha.

\- RF-LD-03: Visualizar el detalle de cada partida, incluyendo fecha,
concepto, tipo de partida, cuentas contables, cargos y abonos.

\- RF-LD-04: Permitir registrar manualmente partidas contables que no
estén asociadas directamente a las operaciones principales de compra y
venta.

\- RF-LD-05: Permitir registrar operaciones contables relacionadas con
deudas, cobros y otros movimientos financieros externos a las
actividades principales de la empresa.

\- RF-LD-06: Gestionar terceros, permitiendo registrar entidades
externas con las que la empresa mantiene operaciones financieras.

\- RF-LD-07: Asociar las partidas contables manuales con un tercero
cuando corresponda, utilizando el campo id_tercero de la tabla partida.

\- RF-LD-08: Permitir consultar las partidas contables asociadas a un
tercero específico.

\- RF-LD-09: Registrar el detalle de las partidas indicando las cuentas
contables correspondientes y sus respectivos cargos y abonos.

\- RF-LD-10: Validar que toda partida contable se encuentre cuadrada, es
decir, que el total de cargos sea igual al total de abonos.

\- RF-LD-11: Consultar el historial de partidas contables, permitiendo
filtrar por fechas, tipo de partida y tercero.

\- **Restricciones de acceso**

- Contabilidad: registrar, consultar y gestionar partidas contables
  manuales, así como gestionar terceros.

- Administrador: acceso general y autorización de operaciones
  restringidas.

- RRHH: sin acceso a la gestión del Libro Diario.

- Ventas: sin acceso a la gestión del Libro Diario.

- Bodega: sin acceso a la gestión del Libro Diario.

**Pantalla de Hoja de Trabajo  
**- RF-HT-01: Generar la hoja de trabajo al finalizar cada mes, después
del registro de las planillas.

\- RF-HT-02: Obtener los movimientos y saldos de las cuentas contables a
partir de las partidas registradas en el Libro Diario durante el período
correspondiente.

\- RF-HT-03: Generar el balance de comprobación, mostrando los saldos
deudores y acreedores de cada cuenta contable.

\- RF-HT-04: Permitir registrar partidas de ajuste adicionales para
realizar las correcciones y ajustes contables correspondientes al cierre
del período.

\- RF-HT-05: Incorporar las partidas de ajuste al balance de
comprobación para determinar los saldos ajustados de las cuentas.

\- RF-HT-06: Generar el balance ajustado, mostrando los saldos deudores
y acreedores después de aplicar los ajustes contables.

\- RF-HT-07: Generar la hoja de resultados, clasificando las cuentas
correspondientes para determinar la utilidad o pérdida del período.

\- RF-HT-08: Generar el balance general a partir de los saldos ajustados
de las cuentas de activo, pasivo y patrimonio.

\- RF-HT-09: Permitir consultar la hoja de trabajo completa, incluyendo
balance de comprobación, ajustes, balance ajustado, hoja de resultados y
balance general.

\- RF-HT-10: Utilizar el balance general generado como base para
establecer los saldos iniciales del siguiente período contable.

\- RF-HT-11: Validar que los balances y las columnas correspondientes se
encuentren cuadrados antes de finalizar el proceso de cierre.

\- RF-HT-12: Permitir consultar e imprimir la hoja de trabajo de
períodos anteriores.

\- **Restricciones de acceso**

- Contabilidad: generar la hoja de trabajo, registrar partidas de
  ajuste, realizar el cierre mensual y consultar períodos anteriores.

- Administrador: acceso general y autorización de operaciones
  restringidas.

- RRHH: sin acceso a la gestión de la hoja de trabajo.

- Ventas: sin acceso a la gestión de la hoja de trabajo.

- Bodega: sin acceso a la gestión de la hoja de trabajo.

**Pantalla de Catálogo de Cuentas**

\- RF-CC-01: Consultar todas las cuentas contables registradas en el
sistema.

\- RF-CC-02: Mostrar el catálogo de cuentas en forma de árbol,
organizando las cuentas según su jerarquía y relación con la cuenta
padre.

\- RF-CC-03: Visualizar el código, nombre, naturaleza y estado de cada
cuenta contable.

\- RF-CC-04: Permitir registrar nuevas cuentas contables dentro del
catálogo.

\- RF-CC-05: Permitir crear subcuentas de nivel 3 y 4, asociándolas a
una cuenta padre existente.

\- RF-CC-06: Generar la jerarquía de las cuentas contables de acuerdo
con su relación padre-hijo.

\- RF-CC-07: Permitir modificar el nombre y la información de las
cuentas contables registradas.

\- RF-CC-08: Permitir habilitar e inhabilitar cuentas contables.

\- RF-CC-09: Permitir identificar las cuentas imputables, es decir,
aquellas que pueden utilizarse directamente en las partidas contables.

\- RF-CC-10: Impedir el registro de movimientos contables en cuentas
inhabilitadas o no imputables.

\- RF-CC-11: Validar que el código de cada cuenta contable sea único.

\- RF-CC-12: Impedir la eliminación de cuentas que tengan subcuentas o
movimientos contables asociados.

\- **Restricciones de acceso**

- Contabilidad: consultar, crear y modificar cuentas contables, así como
  gestionar subcuentas y su imputabilidad.

- Administrador: acceso general y autorización de operaciones
  restringidas.

- RRHH: sin acceso a la gestión del catálogo de cuentas.

- Ventas: sin acceso a la gestión del catálogo de cuentas.

- Bodega: sin acceso a la gestión del catálogo de cuentas.

**Reglas Generales**

**1. Gestión de operaciones al crédito**

\- RF-GEN-01: Permitir registrar operaciones de compra y venta bajo la
modalidad de crédito.

\- RF-GEN-02: Aplicar una tasa de interés base del 2% mensual a los
documentos registrados al crédito, el porcentaje es modificable.

\- RF-GEN-03: Establecer un plazo base de 3 meses para las operaciones
al crédito.

\- RF-GEN-04: Exonerar el cobro de intereses cuando el documento al
crédito sea cancelado antes de cumplir el primer mes.

\- RF-GEN-05: Calcular los intereses correspondientes según el tiempo
transcurrido desde la fecha de registro de la operación al crédito.

\- RF-GEN-06: Permitir que el frontend gestione las constancias y
documentos relacionados con las operaciones al crédito.

\- RF-GEN-07: Recibir desde el frontend un JSON con la información
necesaria para registrar contablemente el pago de un documento al
crédito.

\- RF-GEN-08: Generar la partida contable correspondiente al momento de
recibir el pago, considerando el capital pendiente y los intereses
aplicables.

\- RF-GEN-09: Identificar mediante las partidas contables las
operaciones realizadas al crédito y sus respectivos pagos.

**2. Ciclo contable**

\- RF-GEN-10: Establecer un período contable trimestral.

\- RF-GEN-11: Iniciar el ciclo contable en el mes de julio, organizando
los períodos de la siguiente manera:

- Primer trimestre: julio, agosto y septiembre.

- Segundo trimestre: octubre, noviembre y diciembre.

- Tercer trimestre: enero, febrero y marzo.

- Cuarto trimestre: abril, mayo y junio.

\- RF-GEN-12: Permitir registrar las operaciones de compra, venta y
movimientos financieros de terceros durante el período contable.

\- RF-GEN-13: Generar las partidas contables correspondientes a las
operaciones de compra y venta, actualizando los saldos del balance
general.

\- RF-GEN-14: Registrar las operaciones financieras relacionadas con
terceros mediante las partidas contables correspondientes.

\- RF-GEN-15: Generar el registro de pago de planillas al finalizar cada
mes.

\- RF-GEN-16: Al finalizar cada trimestre, generar el balance de
comprobación a partir de las partidas registradas durante el período.

\- RF-GEN-17: Permitir registrar las partidas de ajuste correspondientes
al cierre del período contable.

\- RF-GEN-18: Generar los estados de resultados y el balance general
después de aplicar los ajustes contables.

\- RF-GEN-19: Utilizar los saldos finales del balance general como
saldos iniciales del siguiente período contable.

\- RF-GEN-20: Repetir el ciclo contable trimestralmente, conservando el
historial de los períodos anteriores.
