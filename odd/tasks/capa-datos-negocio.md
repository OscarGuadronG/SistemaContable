# capa-datos-negocio

**Objetivo:** capa de datos y negocio del backend (`backend/`, Jakarta EE 10, JDK 21, Maven 3.9.16, Open Liberty, PostgreSQL/Supabase) para el esquema contable: entidades JPA, DAOs, servicios con reglas de negocio, recursos REST, configuración y pruebas.

**Contexto:** el repo `OscarGuadronG/SistemaContable` solo tenía el esqueleto (`pom.xml`, `RestApplication`, `HelloResource`). Se toma como referencia el proyecto de inventario (`C:\Users\jeffh\Downloads\Inventario_prn335-main`): entidades JPA a mano, `InventarioDefaultDataAccess<T>` como DAO genérico, EJB `@Stateless`, `persistence.xml` con `jta-data-source`, `server.xml` de Liberty, pruebas con Mockito. **No se arrastran sus errores de lógica** (ver sección "Errores de inventario que NO se repiten").

**Rama:** `feat/capa-datos-negocio` (local; sin push). Commits convencionales, sin atribución a IA (regla del usuario).
**TDD:** estricto en servicios y lógica pura (RED → GREEN → REFACTOR, JUnit 5 + Mockito). Runner: `./mvnw test` desde `backend/`.
**Autorización de alcance:** solo `backend/`, `odd/`, `docs/` y `.gitignore`/`.env.example` del repo SistemaContable.

## Decisiones

- **Paquetes** (base `com.sistemacontable`): `entity`, `control` (DAOs), `control.service`, `control.config`, `boundary` (REST; ya existe `HelloResource`), `boundary.dto`, `boundary.mapper`.
- **Idioma:** identificadores = nombres del esquema (español); comentarios y mensajes al usuario en español neutro.
- **IDs:** todas las PK son `bigint GENERATED ALWAYS AS IDENTITY` → `Long` + `@GeneratedValue(IDENTITY)`; nunca se inserta el id.
- **Tipos:** `numeric` → `BigDecimal` (sin `precision/scale` inventados), `date` → `LocalDate`, `timestamptz` → `OffsetDateTime`, `smallint` → `Short`.
- **Enums de Postgres → enums Java con `@Enumerated(STRING)`** (EclipseLink). Valores exactos: `estado_empleado{Activo,Inactivo,Suspendido}`, `estado_planilla{Pendiente,Pagado}`, `naturaleza_cuenta{Acreedora,Deudora}`, `tipo_movimiento{Entrada,Salida,Traslado,Ajuste}`, `tipo_partida{Diario,Ingreso,Egreso,Ajuste,Cierre}`. El datasource lleva `stringtype=unspecified` para que pgjdbc acepte String → enum.
- **Campo `cuentas.name`** se mapea como `nombre` (`@Column(name="name")`).
- **Auto-referencias** (`cuentas.id_padre`, `tipo_producto.id_padre`): validar ciclos en servicio.
- **Unidad de persistencia:** `contablePU` (JTA, `jdbc/ContableDS`). `eclipselink.ddl-generation=none`; el esquema vive en la BD.
- **Excepciones:** `ReglaNegocioException` (`@ApplicationException(rollback=true)`) para reglas de negocio; `NoEncontradoException` para 404. Un `ExceptionMapper` las traduce: negocio→422, argumento inválido→400, no encontrado→404, resto→500 genérico (sin ocultar 4xx como "Cannot access db").
- **REST con DTOs (records)**, nunca entidades JPA directas (evita LAZY/ciclos en JSON-B).
- **Contabilización automática** (elegida por el usuario): compra/venta/planilla generan kardex + existencias + partida cuadrada en **una sola transacción JTA**. Códigos de cuenta y tasas en `contabilidad.properties` (classpath), sobreescribibles por variables de entorno; nada hardcodeado en código.

## Errores de inventario que NO se repiten

1. DAO genérico lanzaba `IllegalAccessException` para argumentos nulos → usar `IllegalArgumentException`.
2. `findRange` ordenaba por `rt.get("id")` literal → ordenar por el atributo `@Id` vía metamodelo.
3. `count()` devolvía `int` y `-1` si no había `em` → `long`, y fallar con excepción.
4. REST validaba `max > first` (rompe paginación: first=50,max=50 → 422) → validar `first>=0` y `1<=max<=100`.
5. Todo error del REST → 500 "Cannot access db" → mapper de excepciones por tipo.
6. `findLikeConsulta` tragaba excepciones y devolvía lista vacía; consultaba campos inexistentes → propagar errores, probar las JPQL.
7. `calcularTotalCompra` devolvía `null` sin filas → `COALESCE`/`BigDecimal.ZERO`.
8. Cada DAO duplicaba `em` package-private y había `@Inject` sin usar → `em` privado, sin dependencias muertas.
9. MDBs `ReceptorCompra`/`DespachoVenta` compitiendo por la misma cola y solo haciendo `println`; `Notificador` sin cerrar la conexión JMS si falla → **no se usa JMS en esta fase**; eventos internos con CDI si hiciera falta.
10. `Kardex` sin lógica: stock/kardex/precio no se actualizaban → servicios atómicos (ver abajo).
11. Importes `precision=8,scale=2` (tope 999 999.99) → sin recorte artificial; `BigDecimal.compareTo` (nunca `equals`) para comparar dinero.
12. Borrado sin comprobar referencias → servicios validan dependencias y devuelven `ReglaNegocioException` en lugar de reventar con FK.

## Reglas de negocio (servicios)

- **PartidaService:** ≥2 líneas; cada línea con exactamente uno de `cargo`/`abono` > 0; cuenta `activo` e `imputable`; Σcargo == Σabono (`compareTo`); una partida **no se edita ni se borra** (solo crear/consultar); `id_compra`/`id_venta` no pueden ir ambos.
- **CuentaService:** `codigo` único; el código de un hijo empieza por el del padre; hijo hereda `naturaleza` del padre; cuenta con hijos no puede ser `imputable`; sin ciclos; no desactivar con hijos activos ni con movimientos.
- **ExistenciasDAO:** no hay UNIQUE(`id_lote`,`id_almacen`) en la BD → buscar con `PESSIMISTIC_WRITE` antes de actualizar, y documentar el UNIQUE recomendado en `docs/recomendaciones-bd.md` (no se aplica).
- **CompraService.registrar:** proveedor/empleado existen; ≥1 detalle; cantidad>0, precio≥0; almacén `estado=true`; por detalle: suma a `existencias`; crea `kardex` (`Entrada`, `destino=almacén`, `id_compra`) + `kardex_detalle` (`precio_costo=precio`); crea partida `Diario` (Inventario + IVA crédito fiscal al debe / Proveedores al haber).
- **VentaService.registrar:** cliente/empleado existen; el detalle indica lote+almacén; stock suficiente (si no → `ReglaNegocioException`, sin dejar nada a medias); resta existencias; `kardex` `Salida` (`origen`) con `precio_costo` = costo promedio ponderado del lote (Σcant×precio / Σcant de `compra_detalle`); partida `Ingreso` con ingreso (Caja o Clientes al debe / Ventas + IVA débito al haber) **y** costo de ventas (Costo de ventas al debe / Inventario al haber).
- **TrasladoService / AjusteService:** `Traslado` (origen≠destino, stock suficiente, sin partida) y `Ajuste` (cantidad con signo explícito, motivo obligatorio; partida `Ajuste` opcional por config).
- **PlanillaService:** solo empleados `Activo`; un detalle por empleado y planilla; `dias` 0..31; horas ≥0; `fecha_fin ≥ fecha_inicio`, `fecha_pago ≥ fecha_fin`; `sueldo_base` = salario del empleado al calcular (congelado); planilla `Pagado` inmutable; pagar = `Pendiente→Pagado` + partida `Egreso`. Cálculo de deducciones en clase pura `CalculoPlanilla` con tasas ISSS/AFP/renta y recargos de horas extra **configurables** (los valores por defecto son una referencia y deben verificarse contra la normativa vigente antes de usarlos).
- **BalanzaService:** balanza de comprobación por rango de fechas (Σcargo, Σabono, saldo según naturaleza, agregando hijas por jerarquía).

## Tareas

- [x] **T1 — Andamiaje:** `pom.xml` (JPA/Validation vía jakartaee-api provided; JUnit 5, Mockito, JaCoCo, Liberty plugin, driver PostgreSQL), `persistence.xml`, `server.xml` (datasource con variables de entorno, `stringtype=unspecified`), `.env.example`, `.gitignore`, `contabilidad.properties`, excepciones. Wrapper Maven 3.9.16 ya instalado.
- [x] **T2 — Entidades (22 tablas) + 5 enums** con `equals/hashCode` por id, validaciones Bean Validation y pruebas de mapeo básicas.
- [x] **T3 — DAOs:** `AbstractDAO<T,ID>` + un DAO `@Stateless` por entidad con las consultas específicas necesarias (con pruebas Mockito).
- [x] **T4 — Servicios** de la sección anterior, **TDD estricto**. (5 commits `feat(service)`, ver evidencia)
- [ ] **T5 — REST:** DTOs, mappers, recursos, `ExceptionMapper`, paginación correcta.
- [ ] **T6 — Cierre:** `./mvnw verify` verde, README del backend con arranque (`./mvnw liberty:dev`), `docs/recomendaciones-bd.md`.

## Criterios de aceptación

- `./mvnw -q verify` verde desde `backend/` (JDK 21, Maven 3.9.16).
- Ninguna regla de negocio en recursos REST; ningún DAO con lógica de negocio.
- Compra/venta/planilla atómicas: ante un fallo no queda kardex, existencias ni partida a medias.
- Cada tarea cierra con al menos un commit convencional; los IDs de commit se anotan aquí.

## Progreso / evidencia

- **T1** (`0ead05f`): pom (JUnit 5, Mockito 5.19 como javaagent, JaCoCo 0.8.12, Surefire 3.2.5, liberty-maven-plugin 3.10.3 con `copyDependencies` del driver; EclipseLink 4.0.4 + H2 2.2.224 solo `test`), `persistence.xml` (`contablePU` JTA y `consolePU`), `server.xml` (datasource con variables `db.*`, `stringType=unspecified`), `bootstrap.properties.example`, `server.env.example`, `.gitignore`, `contabilidad.properties` + `ConfigContable`, excepciones. `./mvnw -q test`: 9 pruebas OK.
- **T2** (`fb56a82`): 22 entidades + `EntidadBase` (`@MappedSuperclass` con id/equals/hashCode) + 5 enums. Pruebas: estructura/reflexion (55) y `MapeoJpaTest` (EclipseLink + H2 con el esquema real de `docs/esquema-bd.sql`, columnas verificadas).
- **T3** (`44b76f0`): `AbstractDAO<T,ID>` + 22 DAOs `@Stateless`. Pruebas Mockito por finder, estructura de DAOs y `ConsultasJpqlTest` (JPQL real contra H2).
- **Verificacion final de T3**: `./mvnw -q test` = 194 pruebas, 0 fallos, 0 errores; `./mvnw -q -DskipTests package` genera `target/sistema-contable.war` (sin driver ni H2/EclipseLink dentro).
- **Desviaciones/limitaciones**: ver informe final de la tarea (EntidadBase, `backend/.env.example` bloqueado por permisos, H2 en vez de PostgreSQL en pruebas de JPQL, `liberty:dev` no ejecutado).
- **T4** (paquete `com.sistemacontable.control.service`, comandos `record` en `...service.cmd`; 20 servicios `@Stateless @LocalBean`, errores de validación siempre `ReglaNegocioException`/`NoEncontradoException`). `./mvnw -q test` verde tras cada commit y `./mvnw -q -DskipTests package` genera el WAR:
  - `74bf81e` PartidaService + `Dinero` (único punto de redondeo HALF_UP) + `ReglasPartida` pura. 234 pruebas.
  - `66ae167` CuentaService + BalanzaService (`CuentaDAO.findTodas`, `Jerarquia`, `ReglasCuenta`, `CalculoBalanza`). 283 pruebas.
  - `a88c1f9` ExistenciasService, KardexService, CompraService, VentaService, TrasladoService, AjusteService (`Asientos`, `TotalesDocumento`, `ReglasKardex`, `ClaveStock`). 372 pruebas. Atomicidad probada con H2 + transacción explícita con rollback ante cualquier excepción (`InventarioH2Test`): ante falta de stock en la 2.ª línea, cuenta configurada inexistente, almacén deshabilitado o fallo simulado a mitad de la escritura no queda kardex, existencias ni partida.
  - `7c7f8f6` CalculoPlanilla (+ `ParametrosPlanilla`) y PlanillaService (`PlanillaDAO.buscarParaActualizar`, `ConfigContable.tiene`). 426 pruebas; valores esperados calculados a mano en los nombres/comentarios de las pruebas.
  - `1355b45` Empleado/Departamento/Cliente/Proveedor/Tercero/UnidadMedida/TipoProducto/Producto/Lote/Almacén (`AbstractDAO.existePorReferencia`, validada contra el metamodelo), `EstructuraServiciosTest`, `ReferenciasH2Test`. 570 pruebas en total, 0 fallos.
  - Decisiones (documentadas en el Javadoc de cada servicio): ver informe final de la tarea. Las cuentas y tasas de `contabilidad.properties` siguen siendo **valores de referencia a verificar** (catálogo real y normativa ISSS/AFP/renta/horas extra).
  - Nota de proceso: el ciclo RED→GREEN se verificó con ejecución previa en todos los grupos salvo en los puros de inventario del tercer commit (`ReglasKardexTest`, `AsientosTest`, `ExistenciasServiceTest`, `KardexServiceTest`), que se escribieron antes que su código pero cuyo fallo previo (compilación) no se ejecutó por separado.
