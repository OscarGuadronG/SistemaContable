<template>
  <q-page class="pagina q-pa-md">
    <encabezado-pagina
      titulo="Hoja de trabajo"
      subtitulo="Trimestre fiscal y vista mensual por fechas"
      icono="table_chart"
    >
      <template #acciones>
        <div class="row q-gutter-sm no-imprimir">
          <q-btn
            v-if="puedeAjustar"
            color="primary"
            outline
            icon="edit_note"
            label="Partida de ajuste"
            @click="ajusteAbierto = true"
          />
          <q-btn
            v-if="puedeCerrar"
            color="negative"
            outline
            icon="lock"
            label="Cerrar periodo"
            :disable="!puedeConfirmarCierre"
            @click="abrirCierre"
          >
            <q-tooltip v-if="!puedeConfirmarCierre">
              La hoja debe cuadrar en comprobación, balance ajustado y balance
              general.
            </q-tooltip>
          </q-btn>
          <q-btn
            outline
            icon="print"
            label="Imprimir"
            :disable="!hoja"
            @click="imprimir({ horizontal: true })"
          />
        </div>
      </template>
    </encabezado-pagina>

    <q-card flat bordered class="q-mb-md no-imprimir">
      <q-card-section>
        <div class="row q-col-gutter-sm items-end">
          <div class="col-12 col-sm-auto">
            <q-btn-toggle
              v-model="modo"
              no-caps
              unelevated
              toggle-color="primary"
              :options="[
                { label: 'Trimestre', value: 'periodo' },
                { label: 'Por fechas', value: 'fechas' }
              ]"
            />
          </div>
          <template v-if="modo === 'periodo'">
            <div class="col-12 col-md-6">
              <q-select
                v-model="periodoId"
                :options="opcionesPeriodo"
                emit-value
                map-options
                outlined
                dense
                label="Periodo trimestral"
                :loading="cargandoPeriodos"
              />
            </div>
          </template>
          <template v-else>
            <div class="col-6 col-sm-3">
              <q-input
                v-model="desde"
                type="date"
                outlined
                dense
                label="Desde"
              />
            </div>
            <div class="col-6 col-sm-3">
              <q-input
                v-model="hasta"
                type="date"
                outlined
                dense
                label="Hasta"
              />
            </div>
          </template>
          <div class="col-auto">
            <q-btn
              color="primary"
              outline
              label="Consultar"
              :loading="cargando"
              @click="cargarHoja"
            />
          </div>
        </div>
        <div v-if="errorPeriodos" class="text-negative q-mt-sm">{{
          errorPeriodos
        }}</div>
      </q-card-section>
    </q-card>

    <q-banner
      v-if="soloLectura"
      class="bg-blue-1 text-primary q-mb-md no-imprimir"
      rounded
    >
      Este periodo está cerrado. La hoja se muestra en solo lectura.
    </q-banner>
    <q-banner
      v-if="error"
      class="bg-red-1 text-negative q-mb-md no-imprimir"
      rounded
    >
      {{ error }}
      <template #action>
        <q-btn flat color="negative" label="Reintentar" @click="cargarHoja" />
      </template>
    </q-banner>

    <div v-if="cargando" class="row justify-center q-pa-xl">
      <q-spinner color="primary" size="36px" />
    </div>

    <q-card v-else-if="hoja" flat bordered>
      <q-tabs
        v-model="tab"
        align="left"
        active-color="primary"
        indicator-color="primary"
        dense
        no-caps
        inline-label
        mobile-arrows
        class="no-imprimir"
      >
        <q-tab name="hoja" icon="grid_on" label="Hoja completa" />
        <q-tab
          name="resultados"
          icon="trending_up"
          label="Estado de resultados"
        />
        <q-tab name="balance" icon="account_balance" label="Balance general" />
      </q-tabs>
      <q-separator class="no-imprimir" />
      <q-tab-panels v-model="tab" animated>
        <q-tab-panel name="hoja" class="zona-impresion">
          <div class="solo-imprimir q-mb-md">
            <div class="text-h6">Distribuidora Ta Bien</div>
            <div>Hoja de trabajo</div>
            <div>{{ rangoImpresion }}</div>
          </div>
          <div class="row q-gutter-sm q-mb-md items-center">
            <q-chip
              :color="hoja.cuadrada?.comprobacion ? 'positive' : 'negative'"
              text-color="white"
              dense
            >
              Comprobación:
              {{ hoja.cuadrada?.comprobacion ? 'cuadra' : 'descuadrada' }}
            </q-chip>
            <q-chip
              :color="hoja.cuadrada?.ajustado ? 'positive' : 'negative'"
              text-color="white"
              dense
            >
              Balance ajustado:
              {{ hoja.cuadrada?.ajustado ? 'cuadra' : 'descuadrada' }}
            </q-chip>
            <q-chip
              :color="hoja.cuadrada?.balance ? 'positive' : 'negative'"
              text-color="white"
              dense
            >
              Balance general:
              {{ hoja.cuadrada?.balance ? 'cuadra' : 'descuadrada' }}
            </q-chip>
            <span v-if="Number(hoja.utilidad) > 0" class="text-body2">
              Utilidad {{ moneda(hoja.utilidad) }}
            </span>
            <span v-if="Number(hoja.perdida) > 0" class="text-body2">
              Pérdida {{ moneda(hoja.perdida) }}
            </span>
          </div>
          <div class="hoja-ancha">
            <q-markup-table
              flat
              bordered
              dense
              separator="cell"
              class="tabla-app tabla-hoja"
            >
              <thead>
                <tr>
                  <th rowspan="2" class="text-left">Código</th>
                  <th rowspan="2" class="text-left col-texto">Cuenta</th>
                  <th colspan="2" class="grupo">Balance de comprobación</th>
                  <th colspan="2" class="grupo">Ajustes</th>
                  <th colspan="2" class="grupo">Balance ajustado</th>
                  <th colspan="2" class="grupo">Estado de resultados</th>
                  <th colspan="2" class="grupo">Balance general</th>
                </tr>
                <tr>
                  <th v-for="n in 10" :key="n" class="text-right">{{
                    n % 2 ? 'Debe' : 'Haber'
                  }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="fila in filas" :key="fila.cuentaId">
                  <td>{{ fila.codigo }}</td>
                  <td class="col-texto">{{ fila.nombre }}</td>
                  <td class="monto">{{
                    celda(fila, 'comprobacion', 'debe')
                  }}</td>
                  <td class="monto">{{
                    celda(fila, 'comprobacion', 'haber')
                  }}</td>
                  <td class="monto">{{ celda(fila, 'ajustes', 'debe') }}</td>
                  <td class="monto">{{ celda(fila, 'ajustes', 'haber') }}</td>
                  <td class="monto">{{ celda(fila, 'ajustado', 'debe') }}</td>
                  <td class="monto">{{ celda(fila, 'ajustado', 'haber') }}</td>
                  <td class="monto">{{ celda(fila, 'resultados', 'debe') }}</td>
                  <td class="monto">{{
                    celda(fila, 'resultados', 'haber')
                  }}</td>
                  <td class="monto">{{ celda(fila, 'balance', 'debe') }}</td>
                  <td class="monto">{{ celda(fila, 'balance', 'haber') }}</td>
                </tr>
                <tr v-if="filaResultado" class="text-weight-medium">
                  <td></td>
                  <td>{{ filaResultado.nombre }}</td>
                  <td></td><td></td><td></td><td></td><td></td><td></td>
                  <td class="monto">{{ filaResultado.resultadosDebe }}</td>
                  <td class="monto">{{ filaResultado.resultadosHaber }}</td>
                  <td class="monto">{{ filaResultado.balanceDebe }}</td>
                  <td class="monto">{{ filaResultado.balanceHaber }}</td>
                </tr>
                <tr v-if="filas.length === 0">
                  <td colspan="12" class="text-center text-grey-7"
                    >No hay movimientos en el rango.</td
                  >
                </tr>
              </tbody>
              <tfoot>
                <tr class="text-weight-bold">
                  <td colspan="2">Totales</td>
                  <td class="monto">{{ total('comprobacion', 'debe') }}</td>
                  <td class="monto">{{ total('comprobacion', 'haber') }}</td>
                  <td class="monto">{{ total('ajustes', 'debe') }}</td>
                  <td class="monto">{{ total('ajustes', 'haber') }}</td>
                  <td class="monto">{{ total('ajustado', 'debe') }}</td>
                  <td class="monto">{{ total('ajustado', 'haber') }}</td>
                  <td class="monto">{{ total('resultados', 'debe') }}</td>
                  <td class="monto">{{ total('resultados', 'haber') }}</td>
                  <td class="monto">{{ total('balance', 'debe') }}</td>
                  <td class="monto">{{ total('balance', 'haber') }}</td>
                </tr>
              </tfoot>
            </q-markup-table>
          </div>
        </q-tab-panel>

        <q-tab-panel name="resultados" class="zona-impresion">
          <div class="solo-imprimir q-mb-md">
            <div class="text-h6">Distribuidora Ta Bien</div>
            <div>Estado de resultados</div>
            <div>{{ rangoImpresion }}</div>
          </div>
          <q-markup-table flat bordered dense class="tabla-app">
            <thead>
              <tr>
                <th class="text-left">Código</th>
                <th class="text-left">Cuenta</th>
                <th class="text-right">Debe</th>
                <th class="text-right">Haber</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="linea in lineasResultados" :key="linea.clave">
                <td
                  v-if="linea.tipo === 'grupo'"
                  colspan="4"
                  class="text-weight-medium bg-grey-2"
                >
                  {{ linea.texto }}
                </td>
                <template v-else>
                  <td>{{ linea.codigo }}</td>
                  <td class="col-texto">{{ linea.nombre }}</td>
                  <td class="monto">{{ linea.debe }}</td>
                  <td class="monto">{{ linea.haber }}</td>
                </template>
              </tr>
              <tr v-if="lineasResultados.length === 0">
                <td colspan="4" class="text-grey-7"
                  >Sin saldos en este estado.</td
                >
              </tr>
            </tbody>
            <tfoot>
              <tr class="text-weight-bold">
                <td colspan="2">Totales</td>
                <td class="monto">{{ total('resultados', 'debe') }}</td>
                <td class="monto">{{ total('resultados', 'haber') }}</td>
              </tr>
            </tfoot>
          </q-markup-table>
        </q-tab-panel>

        <q-tab-panel name="balance" class="zona-impresion">
          <div class="solo-imprimir q-mb-md">
            <div class="text-h6">Distribuidora Ta Bien</div>
            <div>Balance general</div>
            <div>{{ rangoImpresion }}</div>
          </div>
          <q-markup-table flat bordered dense class="tabla-app">
            <thead>
              <tr>
                <th class="text-left">Código</th>
                <th class="text-left">Cuenta</th>
                <th class="text-right">Debe</th>
                <th class="text-right">Haber</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="linea in lineasBalance" :key="linea.clave">
                <td
                  v-if="linea.tipo === 'grupo'"
                  colspan="4"
                  class="text-weight-medium bg-grey-2"
                >
                  {{ linea.texto }}
                </td>
                <template v-else>
                  <td>{{ linea.codigo }}</td>
                  <td class="col-texto">{{ linea.nombre }}</td>
                  <td class="monto">{{ linea.debe }}</td>
                  <td class="monto">{{ linea.haber }}</td>
                </template>
              </tr>
              <tr v-if="lineasBalance.length === 0">
                <td colspan="4" class="text-grey-7"
                  >Sin saldos en este estado.</td
                >
              </tr>
            </tbody>
            <tfoot>
              <tr class="text-weight-bold">
                <td colspan="2">Totales</td>
                <td class="monto">{{ total('balance', 'debe') }}</td>
                <td class="monto">{{ total('balance', 'haber') }}</td>
              </tr>
            </tfoot>
          </q-markup-table>
        </q-tab-panel>
      </q-tab-panels>
    </q-card>

    <div v-else class="text-center text-grey-7 q-pa-xl">
      Elija un periodo o un rango de fechas para generar la hoja.
    </div>

    <formulario-partida
      v-model="ajusteAbierto"
      titulo="Partida de ajuste"
      tipo-fijo="Ajuste"
      :fecha-inicial="fechaAjuste"
      @guardada="cargarHoja"
    />

    <q-dialog v-model="cierreAbierto" persistent>
      <q-card style="width: min(480px, 94vw)">
        <q-card-section class="text-h6">Cerrar periodo</q-card-section>
        <q-card-section class="q-pt-none">
          Se genera la partida de cierre, se guarda esta hoja y se abre el
          periodo siguiente. Esta acción es definitiva.
          <q-checkbox
            v-model="aceptaCierre"
            class="q-mt-md"
            label="Entiendo que el cierre es definitivo"
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" v-close-popup :disable="cerrando" />
          <q-btn
            color="negative"
            unelevated
            label="Cerrar periodo"
            :disable="!aceptaCierre"
            :loading="cerrando"
            @click="cerrarPeriodo"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import EncabezadoPagina from '@/components/EncabezadoPagina.vue'
import FormularioPartida from '@/components/FormularioPartida.vue'
import { usePermiso } from '@/composables/usePermiso'
import { api, comoLista } from '@/services/api'
import { avisarExito, imprimir } from '@/utils/dialogo'
import { fecha, hoy, moneda } from '@/utils/formato'

const { permitido } = usePermiso()
const puedeEscribir = computed(() => permitido('periodos', 'escritura'))

const modo = ref('periodo')
const tab = ref('hoja')
const periodoId = ref(null)
const periodos = ref([])
const cargandoPeriodos = ref(false)
const errorPeriodos = ref('')
const desde = ref(`${hoy().slice(0, 7)}-01`)
const hasta = ref(hoy())
const hoja = ref(null)
const cargando = ref(false)
const error = ref('')
const ajusteAbierto = ref(false)
const cierreAbierto = ref(false)
const aceptaCierre = ref(false)
const cerrando = ref(false)

const CLASE = {
  1: 'Activo',
  2: 'Pasivo',
  3: 'Patrimonio',
  4: 'Costos y gastos',
  5: 'Ingresos'
}

const opcionesPeriodo = computed(() =>
  periodos.value.map(periodo => ({
    value: periodo.id,
    label: etiquetaPeriodo(periodo)
  }))
)

const periodoSeleccionado = computed(
  () =>
    periodos.value.find(periodo => periodo.id === periodoId.value) ||
    hoja.value?.periodo ||
    null
)

const soloLectura = computed(
  () => periodoSeleccionado.value?.estado === 'Cerrado'
)

const puedeAjustar = computed(
  () =>
    puedeEscribir.value &&
    !soloLectura.value &&
    (modo.value === 'fechas' || periodoId.value)
)

const puedeCerrar = computed(
  () =>
    puedeEscribir.value &&
    modo.value === 'periodo' &&
    periodoId.value &&
    !soloLectura.value
)

const puedeConfirmarCierre = computed(() => {
  const cuadre = hoja.value?.cuadrada
  return Boolean(cuadre?.comprobacion && cuadre?.ajustado && cuadre?.balance)
})

const fechaAjuste = computed(() => {
  const periodo = periodoSeleccionado.value
  const dia = hoy()
  if (!periodo?.fechaInicio || !periodo?.fechaFin) return dia
  if (dia >= periodo.fechaInicio && dia <= periodo.fechaFin) return dia
  return String(periodo.fechaFin).slice(0, 10)
})

const filas = computed(() =>
  [...(hoja.value?.filas || [])].sort((a, b) =>
    String(a.codigo).localeCompare(String(b.codigo), 'es', { numeric: true })
  )
)

const filaResultado = computed(() => {
  const utilidad = Number(hoja.value?.utilidad || 0)
  const perdida = Number(hoja.value?.perdida || 0)
  if (utilidad <= 0 && perdida <= 0) return null
  return {
    nombre: utilidad > 0 ? 'Utilidad del ejercicio' : 'Pérdida del ejercicio',
    resultadosDebe: utilidad > 0 ? moneda(utilidad) : '',
    resultadosHaber: perdida > 0 ? moneda(perdida) : '',
    balanceDebe: perdida > 0 ? moneda(perdida) : '',
    balanceHaber: utilidad > 0 ? moneda(utilidad) : ''
  }
})

const filasResultados = computed(() =>
  filas.value.filter(fila => fila.clase === 4 || fila.clase === 5)
)
const filasBalance = computed(() =>
  filas.value.filter(fila => fila.clase >= 1 && fila.clase <= 3)
)

const rangoImpresion = computed(() => {
  if (periodoSeleccionado.value)
    return etiquetaPeriodo(periodoSeleccionado.value)
  return `${fecha(hoja.value?.desde)} – ${fecha(hoja.value?.hasta)}`
})

function armarEstado(grupo, agrupar) {
  const origen =
    grupo === 'resultados' ? filasResultados.value : filasBalance.value
  const salida = []
  let claseAnterior = null
  origen.forEach(fila => {
    const debe = Number(fila?.[grupo]?.debe || 0)
    const haber = Number(fila?.[grupo]?.haber || 0)
    if (!debe && !haber) return
    if (agrupar && fila.clase !== claseAnterior) {
      claseAnterior = fila.clase
      salida.push({
        tipo: 'grupo',
        clave: `g-${fila.clase}`,
        texto: CLASE[fila.clase] || `Clase ${fila.clase}`
      })
    }
    salida.push({
      tipo: 'cuenta',
      clave: fila.cuentaId,
      codigo: fila.codigo,
      nombre: fila.nombre,
      debe: debe ? moneda(debe) : '',
      haber: haber ? moneda(haber) : ''
    })
  })
  const extra = filaResultado.value
  if (extra) {
    salida.push({
      tipo: 'extra',
      clave: 'resultado',
      codigo: '',
      nombre: extra.nombre,
      debe: grupo === 'resultados' ? extra.resultadosDebe : extra.balanceDebe,
      haber: grupo === 'resultados' ? extra.resultadosHaber : extra.balanceHaber
    })
  }
  return salida
}

const lineasResultados = computed(() => armarEstado('resultados', false))
const lineasBalance = computed(() => armarEstado('balance', true))

function etiquetaPeriodo(periodo) {
  if (!periodo) return 'Periodo'
  const estado = periodo.estado || ''
  return [
    periodo.anioFiscal ? `Año fiscal ${periodo.anioFiscal}` : '',
    periodo.trimestre ? `trimestre ${periodo.trimestre}` : periodo.nombre || '',
    periodo.fechaInicio
      ? `${fecha(periodo.fechaInicio)} – ${fecha(periodo.fechaFin)}`
      : '',
    estado
  ]
    .filter(Boolean)
    .join(' · ')
}

function celda(fila, grupo, lado) {
  const valor = Number(fila?.[grupo]?.[lado] || 0)
  return valor === 0 ? '' : moneda(valor)
}

function total(grupo, lado) {
  return moneda(hoja.value?.totales?.[grupo]?.[lado])
}

async function cargarPeriodos() {
  cargandoPeriodos.value = true
  errorPeriodos.value = ''
  try {
    const { data } = await api.get('/periodos')
    periodos.value = comoLista(data)
  } catch (causa) {
    periodos.value = []
    errorPeriodos.value = causa.mensaje || 'No se pudieron cargar los periodos.'
  }
  try {
    const { data } = await api.get('/periodos/actual', { silencioso: true })
    if (!periodoId.value && data?.id) periodoId.value = data.id
  } catch {
    if (!periodoId.value && periodos.value.length) {
      periodoId.value = periodos.value[periodos.value.length - 1].id
    }
  } finally {
    cargandoPeriodos.value = false
  }
}

async function cargarHoja() {
  cargando.value = true
  error.value = ''
  try {
    const query =
      modo.value === 'periodo'
        ? { periodoId: periodoId.value }
        : { desde: desde.value, hasta: hasta.value }
    if (modo.value === 'periodo' && !periodoId.value) {
      error.value = 'Seleccione un periodo.'
      hoja.value = null
      return
    }
    const { data } = await api.get('/hoja-trabajo', { query })
    hoja.value = data
    const remoto = data?.periodo
    if (remoto?.id) {
      const indice = periodos.value.findIndex(
        periodo => periodo.id === remoto.id
      )
      if (indice >= 0)
        periodos.value[indice] = { ...periodos.value[indice], ...remoto }
    }
  } catch (causa) {
    hoja.value = null
    error.value = causa.mensaje || 'No se pudo generar la hoja de trabajo.'
  } finally {
    cargando.value = false
  }
}

function abrirCierre() {
  aceptaCierre.value = false
  cierreAbierto.value = true
}

async function cerrarPeriodo() {
  if (!aceptaCierre.value || !periodoId.value) return
  cerrando.value = true
  try {
    await api.post(`/periodos/${periodoId.value}/cerrar`)
    avisarExito('Periodo cerrado.')
    cierreAbierto.value = false
    await cargarPeriodos()
    await cargarHoja()
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    cerrando.value = false
  }
}

onMounted(async () => {
  await cargarPeriodos()
  if (periodoId.value) await cargarHoja()
})
</script>

<style scoped>
.grupo {
  text-align: center;
  background: #e8eef6;
}
</style>
