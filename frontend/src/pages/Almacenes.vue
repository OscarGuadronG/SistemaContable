<template>
  <q-page class="pagina q-pa-md">
    <encabezado-pagina
      titulo="Almacenes"
      subtitulo="Bodegas habilitadas y traslados de inventario"
      icono="warehouse"
    />
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
        <q-tab name="lista" icon="warehouse" label="Almacenes" />
        <q-tab
          v-if="veTraslados"
          name="traslado"
          icon="swap_horiz"
          label="Trasladar"
        />
        <q-tab
          v-if="veTraslados"
          name="historial"
          icon="history"
          label="Historial"
        />
      </q-tabs>
      <q-separator />
      <q-tab-panels v-model="tab" animated>
        <q-tab-panel name="lista">
          <div class="row justify-end q-mb-md" v-if="puedeEditar">
            <q-btn
              color="primary"
              unelevated
              icon="add"
              label="Nuevo almacén"
              @click="abrir()"
            />
          </div>
          <q-banner
            v-if="errorLista"
            class="bg-red-1 text-negative q-mb-md"
            rounded
          >
            {{ errorLista }}
            <template #action
              ><q-btn
                flat
                color="negative"
                label="Reintentar"
                @click="cargarAlmacenes"
            /></template>
          </q-banner>
          <q-table
            flat
            bordered
            class="tabla-app"
            :rows="almacenes"
            :columns="columnas"
            row-key="id"
            :loading="cargandoLista"
          >
            <template #body-cell-estado="props">
              <q-td :props="props">
                <q-badge :color="props.row.estado ? 'positive' : 'grey'">
                  {{ props.row.estado ? 'Habilitado' : 'Inhabilitado' }}
                </q-badge>
              </q-td>
            </template>
            <template #body-cell-acciones="props">
              <q-td :props="props" class="q-gutter-xs">
                <template v-if="puedeEditar">
                  <q-btn
                    flat
                    dense
                    round
                    icon="edit"
                    color="primary"
                    @click="abrir(props.row)"
                  />
                  <q-btn
                    flat
                    dense
                    :label="props.row.estado ? 'Inhabilitar' : 'Habilitar'"
                    :color="props.row.estado ? 'negative' : 'positive'"
                    @click="cambiarEstado(props.row)"
                  />
                </template>
              </q-td>
            </template>
          </q-table>
        </q-tab-panel>

        <q-tab-panel v-if="veTraslados" name="traslado">
          <q-form @submit.prevent="guardarTraslado">
            <div class="row q-col-gutter-sm">
              <div class="col-12 col-md-4">
                <selector-busqueda
                  v-model="traslado.almacenOrigenId"
                  :opciones="habilitados"
                  etiqueta="Origen"
                  :rules="[requerido('Seleccione el origen')]"
                />
              </div>
              <div class="col-12 col-md-4">
                <selector-busqueda
                  v-model="traslado.almacenDestinoId"
                  :opciones="destinos"
                  etiqueta="Destino"
                  :rules="[requerido('Seleccione el destino')]"
                />
              </div>
              <div class="col-6 col-md-2">
                <q-input
                  v-model="traslado.fecha"
                  type="date"
                  outlined
                  dense
                  label="Fecha"
                />
              </div>
              <div class="col-6 col-md-2">
                <q-input
                  v-model.number="traslado.gastosAdicionales"
                  type="number"
                  min="0"
                  step="0.01"
                  outlined
                  dense
                  label="Gastos"
                />
              </div>
              <div class="col-12 col-md-4">
                <q-select
                  v-model="traslado.formaPagoGastos"
                  :options="FORMAS_PAGO_INMEDIATO"
                  emit-value
                  map-options
                  outlined
                  dense
                  label="Pago de gastos"
                />
              </div>
              <div class="col-12">
                <q-input
                  v-model="traslado.descripcion"
                  outlined
                  dense
                  label="Descripción"
                />
              </div>
            </div>
            <div class="row items-center q-mt-md q-mb-sm">
              <div class="text-subtitle2">Líneas</div>
              <q-space />
              <q-btn
                flat
                color="primary"
                icon="add"
                label="Agregar"
                @click="agregarLinea"
              />
            </div>
            <div
              v-for="(linea, indice) in traslado.lineas"
              :key="linea.clave"
              class="row q-col-gutter-sm q-mb-sm"
            >
              <div class="col-12 col-md-7">
                <selector-busqueda
                  v-model="linea.loteId"
                  :opciones="existenciasOrigen"
                  etiqueta="Lote en origen"
                  opcion-valor="loteId"
                  :opcion-etiqueta="etiquetaExistencia"
                  :rules="[requerido('Seleccione el lote')]"
                />
              </div>
              <div class="col-10 col-md-3">
                <q-input
                  v-model.number="linea.cantidad"
                  type="number"
                  min="0.0001"
                  step="0.0001"
                  outlined
                  dense
                  label="Cantidad"
                  :error="excede(linea)"
                  error-message="Supera la existencia en origen"
                />
              </div>
              <div class="col-2">
                <q-btn
                  flat
                  round
                  color="negative"
                  icon="close"
                  @click="traslado.lineas.splice(indice, 1)"
                />
              </div>
            </div>
            <q-btn
              v-if="puedeTrasladar"
              color="primary"
              unelevated
              type="submit"
              label="Registrar traslado"
              :loading="guardando"
              :disable="hayExceso"
            />
            <div v-else class="text-grey-7"
              >Su rol puede consultar traslados, no registrarlos.</div
            >
          </q-form>
        </q-tab-panel>

        <q-tab-panel v-if="veTraslados" name="historial">
          <div class="row q-col-gutter-sm items-end q-mb-md">
            <div class="col-6 col-sm-3"
              ><q-input
                v-model="filtros.desde"
                type="date"
                outlined
                dense
                label="Desde"
            /></div>
            <div class="col-6 col-sm-3"
              ><q-input
                v-model="filtros.hasta"
                type="date"
                outlined
                dense
                label="Hasta"
            /></div>
            <div class="col-12 col-sm-3">
              <selector-busqueda
                v-model="filtros.almacenId"
                :opciones="almacenes"
                etiqueta="Almacén"
              />
            </div>
            <div class="col-auto"
              ><q-btn color="primary" outline label="Buscar" @click="reiniciar"
            /></div>
          </div>
          <q-banner
            v-if="error"
            class="bg-red-1 text-negative q-mb-md"
            rounded
            >{{ error }}</q-banner
          >
          <q-table
            flat
            bordered
            class="tabla-app"
            :rows="filas"
            :columns="columnasTraslado"
            row-key="kardexId"
            v-model:pagination="paginacion"
            :loading="cargando"
            @request="cargar"
          >
            <template #body-cell-fecha="props"
              ><q-td :props="props">{{
                fecha(props.row.fecha)
              }}</q-td></template
            >
            <template #body-cell-acciones="props">
              <q-td :props="props"
                ><q-btn
                  flat
                  dense
                  color="primary"
                  label="Detalle"
                  @click="verTraslado(props.row)"
              /></q-td>
            </template>
          </q-table>
        </q-tab-panel>
      </q-tab-panels>
    </q-card>

    <q-dialog v-model="dialogo" persistent>
      <q-card style="width: min(480px, 94vw)">
        <q-form @submit.prevent="guardarAlmacen">
          <q-card-section class="text-h6">{{
            editando ? 'Editar almacén' : 'Nuevo almacén'
          }}</q-card-section>
          <q-card-section class="q-gutter-md">
            <q-input
              v-model="form.nombre"
              outlined
              dense
              label="Nombre"
              :rules="[requerido('Indique el nombre')]"
            />
            <q-input
              v-model="form.descripcion"
              outlined
              dense
              autogrow
              label="Descripción"
            />
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat label="Cancelar" v-close-popup />
            <q-btn
              color="primary"
              unelevated
              type="submit"
              label="Guardar"
              :loading="guardando"
            />
          </q-card-actions>
        </q-form>
      </q-card>
    </q-dialog>

    <q-dialog v-model="detalleAbierto">
      <q-card style="width: min(840px, 96vw)">
        <q-card-section class="row items-center">
          <div class="text-h6"
            >Traslado {{ detalle?.kardexId || detalle?.id }}</div
          >
          <q-space />
          <q-btn flat round icon="close" v-close-popup />
        </q-card-section>
        <q-card-section v-if="detalle">
          <div class="q-mb-sm">
            {{ textoRelacion(detalle, 'origen') }} →
            {{ textoRelacion(detalle, 'destino') }} · gastos
            {{ moneda(detalle.gastosAdicionales) }} · costo
            {{ moneda(detalle.costoTotal) }}
          </div>
          <q-markup-table flat bordered dense class="tabla-app q-mb-md">
            <thead>
              <tr>
                <th class="text-left">Producto</th>
                <th class="text-left">Lote</th>
                <th class="text-right">Cantidad</th>
                <th class="text-right">Costo unitario</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="linea in detalle.lineas || []"
                :key="linea.loteId || linea.lote"
              >
                <td>{{ linea.producto || '—' }}</td>
                <td>{{
                  linea.lote?.numLote || linea.lote || linea.numLote || '—'
                }}</td>
                <td class="text-right">{{ cantidad(linea.cantidad) }}</td>
                <td class="text-right">{{ moneda(linea.costoUnitario) }}</td>
              </tr>
            </tbody>
          </q-markup-table>
          <visor-partida :partida="detalle.partida" />
        </q-card-section>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import EncabezadoPagina from '@/components/EncabezadoPagina.vue'
import SelectorBusqueda from '@/components/SelectorBusqueda.vue'
import VisorPartida from '@/components/VisorPartida.vue'
import { useListado } from '@/composables/useListado'
import { usePermiso } from '@/composables/usePermiso'
import { api, listarTodo } from '@/services/api'
import { avisarExito, confirmar } from '@/utils/dialogo'
import {
  FORMAS_PAGO_INMEDIATO,
  cantidad,
  fecha,
  hoy,
  moneda,
  redondear,
  requerido,
  textoRelacion
} from '@/utils/formato'

const { permitido } = usePermiso()
const puedeEditar = computed(() => permitido('almacenes', 'escritura'))
const veTraslados = computed(() => permitido('traslados'))
const puedeTrasladar = computed(() => permitido('traslados', 'escritura'))
const tab = ref('lista')

const almacenes = ref([])
const cargandoLista = ref(false)
const errorLista = ref('')
const existencias = ref([])
const lotes = ref([])
const productos = ref([])
const dialogo = ref(false)
const editando = ref(null)
const form = reactive({ nombre: '', descripcion: '' })
const guardando = ref(false)
let consecutivo = 0
const traslado = reactive({
  almacenOrigenId: null,
  almacenDestinoId: null,
  fecha: hoy(),
  descripcion: '',
  gastosAdicionales: 0,
  formaPagoGastos: 'Contado',
  lineas: []
})
const filtros = reactive({ desde: '', hasta: '', almacenId: null })
const { filas, cargando, error, paginacion, cargar, reiniciar } = useListado(
  '/traslados',
  () => ({ ...filtros })
)
const detalle = ref(null)
const detalleAbierto = ref(false)

const columnas = [
  { name: 'nombre', label: 'Nombre', field: 'nombre', align: 'left' },
  {
    name: 'descripcion',
    label: 'Descripción',
    field: 'descripcion',
    align: 'left'
  },
  { name: 'estado', label: 'Estado', field: 'estado', align: 'left' },
  { name: 'acciones', label: '', field: 'acciones', align: 'right' }
]
const columnasTraslado = [
  { name: 'fecha', label: 'Fecha', field: 'fecha', align: 'left' },
  {
    name: 'origen',
    label: 'Origen',
    field: fila => textoRelacion(fila, 'origen'),
    align: 'left'
  },
  {
    name: 'destino',
    label: 'Destino',
    field: fila => textoRelacion(fila, 'destino'),
    align: 'left'
  },
  {
    name: 'gastos',
    label: 'Gastos',
    field: 'gastosAdicionales',
    align: 'right',
    format: moneda
  },
  {
    name: 'costo',
    label: 'Costo',
    field: 'costoTotal',
    align: 'right',
    format: moneda
  },
  { name: 'acciones', label: '', field: 'acciones', align: 'right' }
]

const habilitados = computed(() =>
  almacenes.value.filter(item => item.estado !== false)
)
const destinos = computed(() =>
  habilitados.value.filter(item => item.id !== traslado.almacenOrigenId)
)
const existenciasOrigen = computed(() =>
  existencias.value.filter(
    item =>
      item.almacenId === traslado.almacenOrigenId && Number(item.cantidad) > 0
  )
)

function etiquetaExistencia(item) {
  const lote = lotes.value.find(candidato => candidato.id === item.loteId)
  const producto = productos.value.find(
    candidato => candidato.id === lote?.productoId
  )
  return `${producto?.nombre || 'Producto'} · ${lote?.numLote || item.loteId} · ${cantidad(item.cantidad)}`
}

function stockDe(loteId) {
  return Number(
    existenciasOrigen.value.find(item => item.loteId === loteId)?.cantidad || 0
  )
}

function comprometido(loteId, excepto) {
  return traslado.lineas
    .filter(linea => linea !== excepto && linea.loteId === loteId)
    .reduce((suma, linea) => suma + (Number(linea.cantidad) || 0), 0)
}

function excede(linea) {
  if (!linea.loteId) return false
  return (
    Number(linea.cantidad || 0) + comprometido(linea.loteId, linea) >
    stockDe(linea.loteId)
  )
}

const hayExceso = computed(() => traslado.lineas.some(excede))

function agregarLinea() {
  consecutivo += 1
  traslado.lineas.push({ clave: consecutivo, loteId: null, cantidad: 1 })
}

async function cargarAlmacenes() {
  cargandoLista.value = true
  errorLista.value = ''
  try {
    almacenes.value = await listarTodo('/almacenes')
  } catch (causa) {
    errorLista.value = causa.mensaje || 'No se pudieron cargar los almacenes.'
  } finally {
    cargandoLista.value = false
  }
}

function abrir(fila) {
  editando.value = fila || null
  form.nombre = fila?.nombre || ''
  form.descripcion = fila?.descripcion || ''
  dialogo.value = true
}

async function guardarAlmacen() {
  guardando.value = true
  const cuerpo = {
    nombre: form.nombre.trim(),
    descripcion: form.descripcion?.trim() || null
  }
  try {
    if (editando.value) await api.put(`/almacenes/${editando.value.id}`, cuerpo)
    else await api.post('/almacenes', cuerpo)
    avisarExito('Almacén guardado.')
    dialogo.value = false
    await cargarAlmacenes()
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

async function cambiarEstado(fila) {
  const accion = fila.estado ? 'inhabilitar' : 'habilitar'
  const acepta = await confirmar({
    titulo: fila.estado ? 'Inhabilitar almacén' : 'Habilitar almacén',
    mensaje: `¿Desea ${accion} «${fila.nombre}»?`,
    ok: fila.estado ? 'Inhabilitar' : 'Habilitar',
    color: fila.estado ? 'negative' : 'positive'
  })
  if (!acepta) return
  try {
    await api.post(`/almacenes/${fila.id}/${accion}`)
    avisarExito('Estado actualizado.')
    await cargarAlmacenes()
  } catch {
    // El cliente HTTP ya mostró el error.
  }
}

async function guardarTraslado() {
  if (traslado.almacenOrigenId === traslado.almacenDestinoId) return
  guardando.value = true
  try {
    await api.post('/traslados', {
      almacenOrigenId: traslado.almacenOrigenId,
      almacenDestinoId: traslado.almacenDestinoId,
      fecha: traslado.fecha,
      descripcion: traslado.descripcion?.trim() || null,
      gastosAdicionales: redondear(traslado.gastosAdicionales || 0),
      formaPagoGastos: traslado.formaPagoGastos,
      lineas: traslado.lineas.map(linea => ({
        loteId: linea.loteId,
        cantidad: Number(linea.cantidad)
      }))
    })
    avisarExito('Traslado registrado.')
    traslado.lineas = []
    traslado.descripcion = ''
    traslado.gastosAdicionales = 0
    existencias.value = await listarTodo('/existencias')
    await reiniciar()
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

async function verTraslado(fila) {
  detalleAbierto.value = true
  detalle.value = null
  try {
    const { data } = await api.get(`/traslados/${fila.kardexId}`)
    detalle.value = data
  } catch {
    detalleAbierto.value = false
  }
}

onMounted(async () => {
  await cargarAlmacenes()
  if (veTraslados.value) {
    try {
      ;[existencias.value, lotes.value, productos.value] = await Promise.all([
        listarTodo('/existencias'),
        listarTodo('/lotes'),
        listarTodo('/productos')
      ])
    } catch {
      // El formulario avisa al fallar la búsqueda de lotes.
    }
    agregarLinea()
    await cargar()
  }
})
</script>
