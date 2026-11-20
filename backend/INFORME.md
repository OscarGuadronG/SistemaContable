# Informe de implementación del backend

Entrega en `feat/requerimientos-finales`, 20 de noviembre de 2026. Se modificaron únicamente `backend/` y `docs/esquema-bd.sql`. No se hizo commit, push, despliegue remoto ni modificación de la base real. Los cambios de `frontend/` pertenecen al trabajo paralelo y no se tocaron.

## Cobertura por requerimiento

| Requerimientos | Implementación |
|---|---|
| RF-INV-01 a 07 | Inventario agregado por producto, búsqueda por nombre/tipo, descendientes de categoría, disponibilidad y almacén; detalle por lote, cantidad y costo del almacén; CRUD existente y ajustes con partida vinculada a kardex. |
| RF-COM-01 a 09 | Compras con proveedor/empleado, empleado del perfil como respaldo, lotes existentes o nuevos en la misma transacción, gastos distribuidos por valor y redondeo residual, entradas agrupadas por almacén, existencias, modalidades Contado/Crédito/Cheque, descuento, IVA y partida cuadrada. Tasa/plazo/vencimiento se guardan en el documento. |
| RF-COM-10 a 13 | Libro de compras con filas y totales, filtros de documentos, gestión de proveedores, devoluciones con plazo, compensación, control acumulado de cantidades y stock, kardex, partida e historial. |
| RF-VEN-01 a 13 | Venta por producto y almacén; precio calculado por costo promedio y margen; descuento general; FIFO por primera entrada al almacén y desempate por id de lote; control acumulado de líneas repetidas; costo histórico por detalle; kardex, existencias, IVA y partida de Ingreso para las tres modalidades. |
| RF-VEN-14 a 18 | CRUD de clientes existente, libro de ventas, consulta del documento con kardex y partida, devoluciones con precio/costo históricos, descuento proporcional, IVA, reintegro de existencias e historial. |
| RF-ALM-01 a 14 | Gestión y habilitación/inhabilitación de almacenes con permisos; traslados entre almacenes habilitados y distintos, validación de stock, gastos distribuidos y capitalizados en el costo de destino, asiento solo por gastos, consulta de historial y detalle con partida. |
| RF-PER-01 a 17 | CRUD y filtros de personal, selección de activos, activar/inactivar, duplicados DUI/ISSS con 409, planillas existentes con valores predeterminados 30/0/0, filtros y referencia `id_planilla` al pagar. Se conserva la generación de detalles de activos, cálculo e inmutabilidad de planillas pagadas. |
| RF-LD-01 a 11 | Libro diario cronológico, detalle de cuentas e importes, filtros por rango/tipo/tercero/origen/periodo, terceros y sus partidas; creación manual y ajustes con validación de partida doble y cuentas activas/imputables. Las referencias permiten identificar compra, venta, traslado, ajuste, planilla, pago y devolución. |
| RF-HT-01 a 12 | Hoja por trimestre o rango mensual: comprobación, ajustes, ajustado, resultados y balance, con utilidad/pérdida y validación de cuadre; instantánea JSONB para periodos cerrados, asientos de Cierre y Apertura y traslado de saldos. La impresión corresponde al frontend sobre los datos de la API. |
| RF-CC-01 a 12 | Árbol con nivel, códigos, naturaleza e imputabilidad; longitudes 1/2/4/5, prefijo y nivel siguiente, naturaleza heredada, unicidad con 409, activar/desactivar y DELETE solo sin hijas ni movimientos. |
| RF-GEN-01 a 09 | Parámetros de crédito, documentos pendientes/cancelados, saldos que descuentan capital y devoluciones, historial y simulación, pago con interés simple por meses iniciados, exoneración anterior a 30 días y contabilización identificada por documento y pago. La API proporciona los datos para constancias del frontend. |
| RF-GEN-10 a 20 | Periodos trimestrales fiscales julio–junio, toda partida asociada a un periodo abierto, rechazo de Cierre/Apertura manuales, cierre en orden, reconocimiento de utilidad o pérdida, apertura del siguiente trimestre y creación de cuatro periodos del año fiscal siguiente. |
| Seguridad y operación | ES256/JWKS de Supabase, caché y renovación de claves desconocidas, validación de emisor/audiencia/expiración, perfil UUID consultado para cada autorización, `@Roles`, usuario de petición, 401/403, CORS, `/me`, administración de usuarios con HttpClient y service role solo del servidor. |
| Esquema, errores y despliegue | Esquema documental/H2 actualizado, convertidor temporal para EclipseLink, JSONB como texto con tipo inferido por PostgreSQL, mensajes de validación en español, duplicados legibles, códigos contables reales y ajustes activos. Docker multi-etapa, wrapper Maven, Liberty mínimo, contexto `/`, `PORT`, heap 300 MB y pool JDBC máximo cinco. |

## Decisiones contables y de integración

- Se conserva el precio de adquisición en compra_detalle y el costo capitalizado en kardex_detalle. El residual monetario del reparto de gastos se asigna a la última línea.
- El costo se calcula por lote y almacén, a partir de sus entradas. Si hay varias entradas de un lote en una compra/almacén, la devolución usa su costo ponderado de entrada de ese documento.
- Una devolución de cliente puede recibirse en otro almacén habilitado. La cantidad máxima se controla por lote sobre toda la venta y sobre todas sus devoluciones, para impedir rebasarla cambiando de almacén.
- El saldo de un crédito de venta se reduce por el reintegro exacto contabilizado a Clientes, incluido IVA y su redondeo por devolución. Se conserva un eventual saldo a favor; no se calcula interés sobre él ni se acepta un pago de capital que exceda el saldo.
- Los pagos usan interés simple sobre el capital abonado desde la fecha original del documento: 29 días sin interés, 30 días un mes, 45 días dos meses con los parámetros predeterminados. La tasa del documento es histórica.
- Los documentos se bloquean al pagar o devolver. Las existencias se bloquean antes de planear FIFO y se validan también las cantidades acumuladas. Los fallos revierten documentos, stock, kardex y partidas.
- Los traslados sin gastos no producen partida. Con gastos, se capitaliza únicamente el incremento; no se registra una compra o venta artificial. Si el valor base de un traslado es cero, los gastos se distribuyen por cantidad.
- Las operaciones sin importe contable no generan líneas en cero. Un periodo sin movimientos de resultados se rechaza al cerrar con 422, para evitar fabricar un asiento sin líneas o con importes ficticios. Esta decisión también está documentada en el README.
- El calendario operativo sigue los periodos persistidos y el ciclo julio–junio exigido por RF-GEN-11. Cambiar parámetros no refecha ni recalendariza los periodos existentes.
- La autorización utiliza el perfil de la BD y no los roles del JWT. Perfil no usa caché compartida de EclipseLink. El tipo de crédito se autoriza también en el servicio para que Ventas no opere compras mediante un parámetro o cuerpo JSON.
- La Admin API de Auth y JTA son dos sistemas transaccionales: se valida el empleado antes de crear Auth y se intenta compensar con DELETE si falla la escritura del perfil, que se sincroniza antes de retornar. Un error de red o del commit posterior todavía puede requerir reconciliación externa.

## Pruebas y empaquetado

Comandos ejecutados desde backend:

```text
./mvnw -q clean test             (durante la integración)
./mvnw -q test                   (verificación final)
Tests run: 888, Failures: 0, Errors: 0, Skipped: 0
./mvnw -q -DskipTests package    (exit 0)
```

WAR: `backend/target/sistema-contable.war`. Se comprobó que contiene `WEB-INF/lib/nimbus-jose-jwt-9.40.jar` y que PostgreSQL no está empaquetado dentro del WAR; el driver se copia a la biblioteca compartida del servidor.

Las pruebas incluyen Mockito, Jersey en memoria y H2 con EclipseLink: JWT con EC P-256 generado localmente; expiración/firma/perfil/rol; gastos de compra, FIFO y margen; crédito y pagos; plazos y cantidades de devolución; traslados con gastos y costos distintos por almacén; libros e inventario; asientos cuadrados; rollback ante fallos; hoja y cierre con utilidad/pérdida; apertura y creación del año fiscal siguiente; DTO de hoja tanto calculada como recuperada de JSON.

## Docker y ejecución

Se ejecutó desde la raíz:

```text
docker build -t sistema-contable-api backend/     (exit 0)
GET http://localhost:9080/api/hello               200, Hola desde Sistema Contable
GET http://localhost:9080/api/me                  401, NO_AUTENTICADO
OPTIONS /api/me, Origin http://localhost:9000     200, CORS correcto
```

También se verificó `PORT=19080` y el arranque con un límite Docker de **512 MiB**, con `/api/hello` 200 y `/api/me` 401. En reposo y sin BD, ese contenedor consumió aproximadamente **222,6 MiB / 512 MiB**; no es una medición bajo carga. El arranque observado del servidor final fue de aproximadamente 14,5 segundos. Se instalaron REST/JAX-RS, CDI, EJB Lite, Bean Validation, JDBC, JSON-B/JSON-P y persistencia, más sus dependencias; no se usa el paquete webProfile completo. El contenedor de verificación se eliminó después de las comprobaciones.

## Lo que no se pudo verificar

- No se conectó a PostgreSQL real ni se ejecutó SQL en Supabase: no se proporcionó contraseña, y no se buscó ni inventó ninguna.
- No se crearon usuarios en un proyecto Auth real ni se probó una service role key real. JWT y JWKS se verificaron con claves y servidor locales de prueba.
- H2 no sustituye la comprobación en PostgreSQL de los enum nativos, JSONB, UUID, triggers diferidos de cuadre y triggers de cuenta activa/imputable. Se probaron las reglas equivalentes del servicio y los mapeos adaptados.
- No se desplegó a una cuenta Render. Se verificaron localmente la imagen, el puerto, las características Liberty, rutas públicas/protegidas y CORS. No hay una prueba de carga con tráfico y BD reales.
- Se conserva el alcance de la planilla existente: no se añadieron aportes patronales ni descuentos ajenos al contrato.

## Archivos cambiados

`M`: modificado, `N`: nuevo, `D`: eliminado. La lista incluye código, pruebas, configuración y documentación; los artefactos de target no se versionan.

```text
N backend/.dockerignore
N backend/Dockerfile
N backend/INFORME.md
M backend/README.md
M backend/mvnw
M backend/pom.xml
M backend/src/main/java/com/sistemacontable/boundary/AjusteResource.java
M backend/src/main/java/com/sistemacontable/boundary/AlmacenResource.java
N backend/src/main/java/com/sistemacontable/boundary/AutenticacionResource.java
M backend/src/main/java/com/sistemacontable/boundary/BalanzaResource.java
M backend/src/main/java/com/sistemacontable/boundary/ClienteResource.java
M backend/src/main/java/com/sistemacontable/boundary/CompraResource.java
N backend/src/main/java/com/sistemacontable/boundary/CreditoResource.java
M backend/src/main/java/com/sistemacontable/boundary/CuentaResource.java
M backend/src/main/java/com/sistemacontable/boundary/DepartamentoResource.java
N backend/src/main/java/com/sistemacontable/boundary/DevolucionCompraResource.java
N backend/src/main/java/com/sistemacontable/boundary/DevolucionVentaResource.java
M backend/src/main/java/com/sistemacontable/boundary/EmpleadoResource.java
M backend/src/main/java/com/sistemacontable/boundary/ErroresHttp.java
M backend/src/main/java/com/sistemacontable/boundary/ExistenciasResource.java
N backend/src/main/java/com/sistemacontable/boundary/FechaParamConverter.java
N backend/src/main/java/com/sistemacontable/boundary/HojaTrabajoResource.java
N backend/src/main/java/com/sistemacontable/boundary/InventarioResource.java
M backend/src/main/java/com/sistemacontable/boundary/KardexResource.java
N backend/src/main/java/com/sistemacontable/boundary/LibroResource.java
M backend/src/main/java/com/sistemacontable/boundary/LoteResource.java
N backend/src/main/java/com/sistemacontable/boundary/ParametroResource.java
M backend/src/main/java/com/sistemacontable/boundary/PartidaResource.java
N backend/src/main/java/com/sistemacontable/boundary/PeriodoResource.java
M backend/src/main/java/com/sistemacontable/boundary/PlanillaResource.java
M backend/src/main/java/com/sistemacontable/boundary/ProductoResource.java
M backend/src/main/java/com/sistemacontable/boundary/ProveedorResource.java
M backend/src/main/java/com/sistemacontable/boundary/Respuestas.java
M backend/src/main/java/com/sistemacontable/boundary/TerceroResource.java
M backend/src/main/java/com/sistemacontable/boundary/TipoProductoResource.java
M backend/src/main/java/com/sistemacontable/boundary/TrasladoResource.java
M backend/src/main/java/com/sistemacontable/boundary/UnidadMedidaResource.java
M backend/src/main/java/com/sistemacontable/boundary/VentaResource.java
N backend/src/main/java/com/sistemacontable/boundary/dto/AutenticacionDto.java
N backend/src/main/java/com/sistemacontable/boundary/dto/CierrePeriodoDto.java
M backend/src/main/java/com/sistemacontable/boundary/dto/CompraDto.java
M backend/src/main/java/com/sistemacontable/boundary/dto/CompraRequest.java
N backend/src/main/java/com/sistemacontable/boundary/dto/CreditoDto.java
N backend/src/main/java/com/sistemacontable/boundary/dto/CuentaArbolDto.java
N backend/src/main/java/com/sistemacontable/boundary/dto/DevolucionCompraRequest.java
N backend/src/main/java/com/sistemacontable/boundary/dto/DevolucionDto.java
N backend/src/main/java/com/sistemacontable/boundary/dto/DevolucionVentaRequest.java
N backend/src/main/java/com/sistemacontable/boundary/dto/HojaTrabajoDto.java
N backend/src/main/java/com/sistemacontable/boundary/dto/InventarioDto.java
M backend/src/main/java/com/sistemacontable/boundary/dto/KardexDto.java
N backend/src/main/java/com/sistemacontable/boundary/dto/LibroComprasDto.java
N backend/src/main/java/com/sistemacontable/boundary/dto/LibroVentasDto.java
N backend/src/main/java/com/sistemacontable/boundary/dto/LineaDevolucionRequest.java
N backend/src/main/java/com/sistemacontable/boundary/dto/PagoCreditoRequest.java
N backend/src/main/java/com/sistemacontable/boundary/dto/ParametroRequest.java
M backend/src/main/java/com/sistemacontable/boundary/dto/PartidaDto.java
N backend/src/main/java/com/sistemacontable/boundary/dto/PartidaResumenDto.java
N backend/src/main/java/com/sistemacontable/boundary/dto/PeriodoDto.java
N backend/src/main/java/com/sistemacontable/boundary/dto/PrecioVentaDto.java
N backend/src/main/java/com/sistemacontable/boundary/dto/TrasladoDto.java
M backend/src/main/java/com/sistemacontable/boundary/dto/TrasladoRequest.java
M backend/src/main/java/com/sistemacontable/boundary/dto/VentaDto.java
M backend/src/main/java/com/sistemacontable/boundary/dto/VentaRequest.java
M backend/src/main/java/com/sistemacontable/boundary/mapper/ContabilidadMapper.java
N backend/src/main/java/com/sistemacontable/boundary/mapper/HojaTrabajoMapper.java
M backend/src/main/java/com/sistemacontable/boundary/mapper/InventarioMapper.java
N backend/src/main/java/com/sistemacontable/boundary/mapper/PeriodoMapper.java
N backend/src/main/java/com/sistemacontable/boundary/mapper/TrasladoMapper.java
M backend/src/main/java/com/sistemacontable/control/CompraDAO.java
N backend/src/main/java/com/sistemacontable/control/DevolucionCompraDAO.java
N backend/src/main/java/com/sistemacontable/control/DevolucionCompraDetalleDAO.java
N backend/src/main/java/com/sistemacontable/control/DevolucionVentaDAO.java
N backend/src/main/java/com/sistemacontable/control/DevolucionVentaDetalleDAO.java
M backend/src/main/java/com/sistemacontable/control/EmpleadoDAO.java
M backend/src/main/java/com/sistemacontable/control/ExistenciasDAO.java
N backend/src/main/java/com/sistemacontable/control/FinanzasDAO.java
M backend/src/main/java/com/sistemacontable/control/KardexDAO.java
N backend/src/main/java/com/sistemacontable/control/PagoCreditoDAO.java
N backend/src/main/java/com/sistemacontable/control/ParametroDAO.java
M backend/src/main/java/com/sistemacontable/control/PartidaDAO.java
M backend/src/main/java/com/sistemacontable/control/PartidaDetalleDAO.java
N backend/src/main/java/com/sistemacontable/control/PeriodoDAO.java
M backend/src/main/java/com/sistemacontable/control/PlanillaDAO.java
M backend/src/main/java/com/sistemacontable/control/ProductoDAO.java
M backend/src/main/java/com/sistemacontable/control/VentaDAO.java
N backend/src/main/java/com/sistemacontable/control/exception/ConflictoException.java
M backend/src/main/java/com/sistemacontable/control/service/AjusteService.java
M backend/src/main/java/com/sistemacontable/control/service/Asientos.java
N backend/src/main/java/com/sistemacontable/control/service/AutenticacionService.java
N backend/src/main/java/com/sistemacontable/control/service/CierrePeriodo.java
M backend/src/main/java/com/sistemacontable/control/service/ClienteService.java
M backend/src/main/java/com/sistemacontable/control/service/CompraService.java
N backend/src/main/java/com/sistemacontable/control/service/CreditoService.java
M backend/src/main/java/com/sistemacontable/control/service/CuentaService.java
N backend/src/main/java/com/sistemacontable/control/service/DevolucionService.java
M backend/src/main/java/com/sistemacontable/control/service/EmpleadoService.java
M backend/src/main/java/com/sistemacontable/control/service/ExistenciasService.java
N backend/src/main/java/com/sistemacontable/control/service/InteresCredito.java
N backend/src/main/java/com/sistemacontable/control/service/InventarioService.java
M backend/src/main/java/com/sistemacontable/control/service/KardexService.java
N backend/src/main/java/com/sistemacontable/control/service/LibroService.java
D backend/src/main/java/com/sistemacontable/control/service/PagoDocumento.java
N backend/src/main/java/com/sistemacontable/control/service/ParametroService.java
M backend/src/main/java/com/sistemacontable/control/service/PartidaService.java
N backend/src/main/java/com/sistemacontable/control/service/PeriodoService.java
M backend/src/main/java/com/sistemacontable/control/service/PlanillaService.java
M backend/src/main/java/com/sistemacontable/control/service/ProveedorService.java
M backend/src/main/java/com/sistemacontable/control/service/ReglasCuenta.java
N backend/src/main/java/com/sistemacontable/control/service/ReglasDevolucion.java
M backend/src/main/java/com/sistemacontable/control/service/ReglasKardex.java
N backend/src/main/java/com/sistemacontable/control/service/ResultadoTraslado.java
N backend/src/main/java/com/sistemacontable/control/service/ResumenPartida.java
M backend/src/main/java/com/sistemacontable/control/service/TrasladoService.java
M backend/src/main/java/com/sistemacontable/control/service/VentaService.java
M backend/src/main/java/com/sistemacontable/control/service/cmd/CompraCmd.java
N backend/src/main/java/com/sistemacontable/control/service/cmd/DevolucionCompraCmd.java
N backend/src/main/java/com/sistemacontable/control/service/cmd/DevolucionVentaCmd.java
M backend/src/main/java/com/sistemacontable/control/service/cmd/LineaCompraCmd.java
N backend/src/main/java/com/sistemacontable/control/service/cmd/LineaDevolucionCmd.java
M backend/src/main/java/com/sistemacontable/control/service/cmd/LineaVentaCmd.java
N backend/src/main/java/com/sistemacontable/control/service/cmd/NuevoLoteCmd.java
N backend/src/main/java/com/sistemacontable/control/service/cmd/PagoCreditoCmd.java
M backend/src/main/java/com/sistemacontable/control/service/cmd/TrasladoCmd.java
M backend/src/main/java/com/sistemacontable/control/service/cmd/VentaCmd.java
M backend/src/main/java/com/sistemacontable/entity/Compra.java
N backend/src/main/java/com/sistemacontable/entity/DevolucionCompra.java
N backend/src/main/java/com/sistemacontable/entity/DevolucionCompraDetalle.java
N backend/src/main/java/com/sistemacontable/entity/DevolucionVenta.java
N backend/src/main/java/com/sistemacontable/entity/DevolucionVentaDetalle.java
M backend/src/main/java/com/sistemacontable/entity/Existencias.java
N backend/src/main/java/com/sistemacontable/entity/InstantConverter.java
M backend/src/main/java/com/sistemacontable/entity/Kardex.java
N backend/src/main/java/com/sistemacontable/entity/PagoCredito.java
N backend/src/main/java/com/sistemacontable/entity/Parametro.java
M backend/src/main/java/com/sistemacontable/entity/Partida.java
N backend/src/main/java/com/sistemacontable/entity/Periodo.java
M backend/src/main/java/com/sistemacontable/entity/Venta.java
M backend/src/main/java/com/sistemacontable/entity/VentaDetalle.java
N backend/src/main/java/com/sistemacontable/entity/enums/EstadoPeriodo.java
N backend/src/main/java/com/sistemacontable/entity/enums/FormaPago.java
M backend/src/main/java/com/sistemacontable/entity/enums/TipoMovimiento.java
M backend/src/main/java/com/sistemacontable/entity/enums/TipoPartida.java
N backend/src/main/java/com/sistemacontable/security/FiltroCors.java
N backend/src/main/java/com/sistemacontable/security/FiltroSeguridad.java
N backend/src/main/java/com/sistemacontable/security/Perfil.java
N backend/src/main/java/com/sistemacontable/security/PerfilDAO.java
N backend/src/main/java/com/sistemacontable/security/PerfilNoActivoException.java
N backend/src/main/java/com/sistemacontable/security/RolApp.java
N backend/src/main/java/com/sistemacontable/security/Roles.java
N backend/src/main/java/com/sistemacontable/security/UsuarioActual.java
N backend/src/main/java/com/sistemacontable/security/VerificadorJwt.java
M backend/src/main/liberty/config/server.env.example
M backend/src/main/liberty/config/server.xml
M backend/src/main/resources/META-INF/persistence.xml
N backend/src/main/resources/ValidationMessages.properties
M backend/src/main/resources/contabilidad.properties
M backend/src/test/java/com/sistemacontable/JpaTestSupport.java
M backend/src/test/java/com/sistemacontable/boundary/ErroresHttpTest.java
M backend/src/test/java/com/sistemacontable/boundary/RecursosEstructuraTest.java
M backend/src/test/java/com/sistemacontable/boundary/RecursosRestTest.java
M backend/src/test/java/com/sistemacontable/boundary/ServidorEnMemoria.java
M backend/src/test/java/com/sistemacontable/boundary/dto/PeticionesTest.java
N backend/src/test/java/com/sistemacontable/boundary/mapper/HojaTrabajoMapperTest.java
M backend/src/test/java/com/sistemacontable/boundary/mapper/MapeadoresTest.java
M backend/src/test/java/com/sistemacontable/control/ConsultasJpqlTest.java
M backend/src/test/java/com/sistemacontable/control/EstructuraDAOsTest.java
M backend/src/test/java/com/sistemacontable/control/config/ConfigContableTest.java
M backend/src/test/java/com/sistemacontable/control/service/AjusteServiceTest.java
M backend/src/test/java/com/sistemacontable/control/service/AsientosTest.java
M backend/src/test/java/com/sistemacontable/control/service/Bd.java
M backend/src/test/java/com/sistemacontable/control/service/CatalogosH2Test.java
M backend/src/test/java/com/sistemacontable/control/service/CompraServiceTest.java
M backend/src/test/java/com/sistemacontable/control/service/ContactosServiceTest.java
M backend/src/test/java/com/sistemacontable/control/service/CuentaBalanzaH2Test.java
M backend/src/test/java/com/sistemacontable/control/service/CuentaServiceTest.java
M backend/src/test/java/com/sistemacontable/control/service/EmpleadoDepartamentoServiceTest.java
M backend/src/test/java/com/sistemacontable/control/service/EscenarioContable.java
M backend/src/test/java/com/sistemacontable/control/service/EstructuraServiciosTest.java
N backend/src/test/java/com/sistemacontable/control/service/FinanzasH2Test.java
N backend/src/test/java/com/sistemacontable/control/service/FinanzasServiceTest.java
N backend/src/test/java/com/sistemacontable/control/service/InteresCreditoTest.java
M backend/src/test/java/com/sistemacontable/control/service/InventarioH2Test.java
M backend/src/test/java/com/sistemacontable/control/service/JerarquiaYReglasCuentaTest.java
D backend/src/test/java/com/sistemacontable/control/service/PagoDocumentoTest.java
M backend/src/test/java/com/sistemacontable/control/service/PartidaServiceH2Test.java
M backend/src/test/java/com/sistemacontable/control/service/PartidaServiceTest.java
N backend/src/test/java/com/sistemacontable/control/service/PeriodoServiceH2Test.java
N backend/src/test/java/com/sistemacontable/control/service/ReglasDevolucionTest.java
M backend/src/test/java/com/sistemacontable/control/service/TrasladoServiceTest.java
M backend/src/test/java/com/sistemacontable/control/service/VentaServiceTest.java
M backend/src/test/java/com/sistemacontable/entity/MapeoEntidadesTest.java
N backend/src/test/java/com/sistemacontable/security/FiltroSeguridadTest.java
N backend/src/test/java/com/sistemacontable/security/SeguridadJwtTest.java
M backend/src/test/resources/META-INF/persistence.xml
M docs/esquema-bd.sql
```
