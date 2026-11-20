<template>
  <q-page class="pagina q-pa-md">
    <encabezado-pagina
      titulo="Libro diario"
      subtitulo="Partidas por fecha, registro manual y terceros"
      icono="menu_book"
    >
      <template #acciones>
        <q-btn
          v-if="puedeEscribir && tab === 'partidas'"
          color="primary"
          unelevated
          icon="add"
          label="Registrar partida"
          @click="nuevaPartida = true"
        />
      </template>
    </encabezado-pagina>

    <q-card flat bordered>
      <q-tabs
        v-model="tab"
        align="left"
        active-color="primary"
        indicator-color="primary"
        dense
        no-caps
        inline-label
        mobile-arrows
      >
        <q-tab name="partidas" icon="menu_book" label="Partidas" />
        <q-tab name="terceros" icon="handshake" label="Terceros" />
      </q-tabs>
      <q-separator />
      <q-tab-panels v-model="tab" animated>
        <q-tab-panel name="partidas">
          <div class="row q-col-gutter-sm items-end q-mb-md">
            <div class="col-6 col-md-2">
              <q-input
                v-model="filtros.desde"
                type="date"
                outlined
                dense
                label="Desde"
              />
            </div>
            <div class="col-6 col-md-2">
              <q-input
                v-model="filtros.hasta"
                type="date"
                outlined
                dense
                label="Hasta"
              />
            </div>
            <div class="col-6 col-md-2">
              <q-select
                v-model="filtros.tipo"
                :options="TIPOS"
                emit-value
                map-options
                clearable
                outlined
                dense
                label="Tipo"
              />
            </div>
            <div class="col-6 col-md-2">
              <q-select
                v-model="filtros.origen"
                :options="ORIGENES"
                emit-value
                map-options
                clearable
                outlined
                dense
                label="Origen"
              />
            </div>
            <div class="col-12 col-md-3">
              <selector-busqueda
                v-model="filtros.terceroId"
                :opciones="terceros"
                etiqueta="Tercero"
              />
            </div>
            <div class="col-auto">
              <q-btn
                color="primary"
                outline
                label="Buscar"
                @click="reiniciar"
              />
            </div>
          </div>

          <q-banner v-if="error" class="bg-red-1 text-negative q-mb-md" rounded>
            {{ error }}
            <template #action>
              <q-btn
                flat
                color="negative"
                label="Reintentar"
                @click="reiniciar"
              />
            </template>
          </q-banner>

          <q-table
            flat
            bordered
            class="tabla-app"
            :rows="filas"
            :columns="columnas"
            row-key="id"
            v-model:pagination="paginacion"
            :loading="cargando"
            :rows-per-page-options="[10, 20, 50, 100]"
            @request="cargar"
          >
            <template #body-cell-fecha="props">
              <q-td :props="props">{{ fecha(props.row.fecha) }}</q-td>
            </template>
            <template #body-cell-acciones="props">
              <q-td :props="props">
                <q-btn
                  flat
                  dense
                  color="primary"
                  label="Detalle"
                  @click="ver(props.row)"
                />
              </q-td>
            </template>
            <template #no-data>
              <div class="full-width text-center q-pa-lg text-grey-7">
                No hay partidas con esos filtros.
              </div>
            </template>
          </q-table>
        </q-tab-panel>

        <q-tab-panel name="terceros">
          <crud-simple
            ruta="/terceros"
            titulo="tercero"
            etiqueta-nuevo="Nuevo tercero"
            :puede-escribir="puedeEscribir"
            :columnas="columnasTercero"
            :campos="camposTercero"
            @cambio="cargarTerceros"
          >
            <template #extra="{ fila }">
              <q-btn
                flat
                dense
                color="primary"
                label="Partidas"
                @click="verTercero(fila)"
              />
            </template>
          </crud-simple>
        </q-tab-panel>
      </q-tab-panels>
    </q-card>

    <formulario-partida v-model="nuevaPartida" @guardada="reiniciar" />

    <q-dialog v-model="detalleAbierto">
      <q-card style="width: min(860px, 96vw)">
        <q-card-section class="row items-center">
          <div class="text-h6">Partida</div>
          <q-space />
          <q-btn flat round icon="close" v-close-popup />
        </q-card-section>
        <q-separator />
        <q-card-section>
          <div v-if="cargandoDetalle" class="row justify-center q-pa-lg">
            <q-spinner color="primary" size="32px" />
          </div>
          <q-banner
            v-else-if="errorDetalle"
            class="bg-red-1 text-negative"
            rounded
          >
            {{ errorDetalle }}
          </q-banner>
          <template v-else-if="detalle">
            <div class="row q-col-gutter-sm q-mb-md text-body2">
              <div class="col-auto"
                ><strong>Origen:</strong>
                {{ etiquetaOrigen(detalle.origen) }}</div
              >
              <div v-if="detalle.tercero" class="col-auto">
                <strong>Tercero:</strong>
                {{ textoRelacion(detalle, 'tercero') }}
              </div>
              <div v-for="ref in referencias" :key="ref" class="col-auto">{{
                ref
              }}</div>
            </div>
            <visor-partida :partida="detalle" />
          </template>
        </q-card-section>
      </q-card>
    </q-dialog>

    <q-dialog v-model="terceroAbierto">
      <q-card style="width: min(920px, 96vw)">
        <q-card-section class="row items-center">
          <div>
            <div class="text-h6">{{ terceroActual?.nombre }}</div>
            <div class="text-caption">Partidas del tercero</div>
          </div>
          <q-space />
          <q-btn flat round icon="close" v-close-popup />
        </q-card-section>
        <q-card-section>
          <q-banner
            v-if="errorTercero"
            class="bg-red-1 text-negative q-mb-sm"
            rounded
          >
            {{ errorTercero }}
          </q-banner>
          <q-table
            flat
            bordered
            dense
            class="tabla-app"
            :rows="partidasTercero"
            :columns="columnas"
            row-key="id"
            :loading="cargandoTercero"
            :rows-per-page-options="[10, 20, 50]"
          >
            <template #body-cell-fecha="props">
              <q-td :props="props">{{ fecha(props.row.fecha) }}</q-td>
            </template>
            <template #body-cell-acciones="props">
              <q-td :props="props">
                <q-btn
                  flat
                  dense
                  color="primary"
                  label="Detalle"
                  @click="ver(props.row)"
                />
              </q-td>
            </template>
            <template #no-data>
              <div class="full-width text-center q-pa-md text-grey-7">
                Este tercero no tiene partidas.
              </div>
            </template>
          </q-table>
        </q-card-section>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import CrudSimple from '@/components/CrudSimple.vue'
import EncabezadoPagina from '@/components/EncabezadoPagina.vue'
import FormularioPartida from '@/components/FormularioPartida.vue'
import SelectorBusqueda from '@/components/SelectorBusqueda.vue'
import VisorPartida from '@/components/VisorPartida.vue'
import { useListado } from '@/composables/useListado'
import { usePermiso } from '@/composables/usePermiso'
import { api, listarTodo } from '@/services/api'
import { fecha, moneda, requerido, textoRelacion } from '@/utils/formato'

const { permitido } = usePermiso()
const puedeEscribir = computed(() => permitido('partidas', 'escritura'))
const tab = ref('partidas')
const nuevaPartida = ref(false)
const terceros = ref([])

const TIPOS = [
  'Diario',
  'Ingreso',
  'Egreso',
  'Ajuste',
  'Cierre',
  'Apertura'
].map(valor => ({
  value: valor,
  label: valor
}))

const ORIGENES = [
  ['compra', 'Compra'],
  ['venta', 'Venta'],
  ['traslado', 'Traslado'],
  ['ajuste', 'Ajuste de inventario'],
  ['planilla', 'Planilla'],
  ['pago', 'Pago'],
  ['devolucion', 'Devolución'],
  ['manual', 'Manual'],
  ['cierre', 'Cierre'],
  ['apertura', 'Apertura']
].map(([value, label]) => ({ value, label }))

const filtros = reactive({
  desde: '',
  hasta: '',
  tipo: null,
  terceroId: null,
  origen: null
})

const { filas, cargando, error, paginacion, cargar, reiniciar } = useListado(
  '/partidas',
  () => ({ ...filtros })
)

const columnas = [
  { name: 'fecha', label: 'Fecha', field: 'fecha', align: 'left' },
  {
    name: 'concepto',
    label: 'Concepto',
    field: 'concepto',
    align: 'left',
    classes: 'col-texto',
    headerClasses: 'col-texto'
  },
  { name: 'tipo', label: 'Tipo', field: 'tipo', align: 'left' },
  {
    name: 'origen',
    label: 'Origen',
    field: fila => etiquetaOrigen(fila.origen),
    align: 'left'
  },
  {
    name: 'tercero',
    label: 'Tercero',
    field: fila => textoRelacion(fila, 'tercero'),
    align: 'left'
  },
  {
    name: 'totalCargo',
    label: 'Cargos',
    field: 'totalCargo',
    align: 'right',
    format: moneda
  },
  {
    name: 'totalAbono',
    label: 'Abonos',
    field: 'totalAbono',
    align: 'right',
    format: moneda
  },
  { name: 'acciones', label: '', field: 'acciones', align: 'right' }
]

const columnasTercero = [
  { name: 'nombre', label: 'Nombre', field: 'nombre', align: 'left' },
  {
    name: 'descripcion',
    label: 'Descripción',
    field: 'descripcion',
    align: 'left'
  }
]
const camposTercero = [
  { key: 'nombre', label: 'Nombre', reglas: [requerido('Indique el nombre')] },
  { key: 'descripcion', label: 'Descripción', tipo: 'textarea' }
]

function etiquetaOrigen(valor) {
  return ORIGENES.find(item => item.value === valor)?.label || valor || '—'
}

const detalleAbierto = ref(false)
const detalle = ref(null)
const cargandoDetalle = ref(false)
const errorDetalle = ref('')

const referencias = computed(() => {
  const fila = detalle.value
  if (!fila) return []
  const mapa = [
    ['compraId', 'Compra'],
    ['ventaId', 'Venta'],
    ['idKardex', 'Kardex'],
    ['idPlanilla', 'Planilla'],
    ['idPagoCredito', 'Pago'],
    ['idDevolucionCompra', 'Devolución de compra'],
    ['idDevolucionVenta', 'Devolución de venta'],
    ['periodoId', 'Periodo']
  ]
  return mapa
    .filter(([clave]) => fila[clave])
    .map(([clave, etiqueta]) => `${etiqueta} #${fila[clave]}`)
})

async function ver(fila) {
  detalleAbierto.value = true
  detalle.value = null
  errorDetalle.value = ''
  cargandoDetalle.value = true
  try {
    const { data } = await api.get(`/partidas/${fila.id}`)
    detalle.value = data
  } catch (causa) {
    errorDetalle.value = causa.mensaje || 'No se pudo abrir la partida.'
  } finally {
    cargandoDetalle.value = false
  }
}

const terceroAbierto = ref(false)
const terceroActual = ref(null)
const partidasTercero = ref([])
const cargandoTercero = ref(false)
const errorTercero = ref('')

async function verTercero(fila) {
  terceroActual.value = fila
  terceroAbierto.value = true
  partidasTercero.value = []
  errorTercero.value = ''
  cargandoTercero.value = true
  try {
    partidasTercero.value = await listarTodo(`/terceros/${fila.id}/partidas`)
  } catch (causa) {
    errorTercero.value = causa.mensaje || 'No se pudieron cargar las partidas.'
  } finally {
    cargandoTercero.value = false
  }
}

async function cargarTerceros() {
  try {
    terceros.value = await listarTodo('/terceros')
  } catch {
    terceros.value = []
  }
}

onMounted(() => {
  reiniciar()
  cargarTerceros()
})
</script>
