<template>
  <div>
    <q-card v-if="puedeEscribir" flat bordered class="q-mb-lg">
      <q-card-section class="text-subtitle1 text-weight-medium">
        Nueva devolución a proveedor
      </q-card-section>
      <q-card-section>
        <q-banner
          v-if="errorCatalogos"
          class="bg-red-1 text-negative q-mb-md"
          rounded
        >
          {{ errorCatalogos }}
        </q-banner>
        <div class="row q-col-gutter-sm">
          <div class="col-12 col-md-6">
            <selector-busqueda
              v-model="form.compraId"
              :opciones="compras"
              etiqueta="Compra (dentro del plazo)"
              :opcion-etiqueta="etiquetaCompra"
              @update:model-value="cargarCompra"
            />
          </div>
          <div class="col-6 col-md-3">
            <q-input
              v-model="form.fecha"
              type="date"
              outlined
              dense
              label="Fecha"
            />
          </div>
          <div class="col-6 col-md-3">
            <q-input
              v-model.number="form.porcentajeCompensacion"
              type="number"
              min="0"
              max="100"
              step="0.01"
              outlined
              dense
              label="% de compensación"
            />
          </div>
          <div class="col-12">
            <q-input
              v-model="form.motivo"
              outlined
              dense
              autogrow
              label="Motivo"
            />
          </div>
        </div>
        <q-markup-table
          v-if="lineas.length"
          flat
          bordered
          dense
          class="tabla-app q-mt-md"
        >
          <thead>
            <tr>
              <th class="text-left">Lote</th>
              <th class="text-left">Almacén</th>
              <th class="text-right">Comprado</th>
              <th class="text-right">A devolver</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="linea in lineas" :key="linea.clave">
              <td>{{ linea.numLote || linea.loteId }}</td>
              <td>{{ linea.almacen || linea.almacenId }}</td>
              <td class="text-right monto">{{ cantidad(linea.comprada) }}</td>
              <td style="width: 140px">
                <q-input
                  v-model.number="linea.devolver"
                  type="number"
                  min="0"
                  :max="linea.comprada"
                  step="0.0001"
                  dense
                  outlined
                />
              </td>
            </tr>
          </tbody>
        </q-markup-table>
        <div class="text-caption text-grey-7 q-mt-sm">
          Plazo máximo: {{ diasMax }} días desde la compra. El costo de la
          devolución usa el costo de entrada de esa compra; la compensación es
          el porcentaje indicado.
        </div>
        <q-btn
          class="q-mt-md"
          color="primary"
          unelevated
          label="Registrar devolución"
          :loading="guardando"
          :disable="!puedeEnviar"
          @click="guardar"
        />
      </q-card-section>
    </q-card>

    <div class="text-subtitle1 text-weight-medium q-mb-sm">Historial</div>
    <div class="row q-col-gutter-sm items-end q-mb-md">
      <div class="col-6 col-sm-3">
        <q-input
          v-model="filtros.desde"
          type="date"
          outlined
          dense
          label="Desde"
        />
      </div>
      <div class="col-6 col-sm-3">
        <q-input
          v-model="filtros.hasta"
          type="date"
          outlined
          dense
          label="Hasta"
        />
      </div>
      <div class="col-auto">
        <q-btn color="primary" outline label="Buscar" @click="reiniciar" />
      </div>
    </div>
    <q-banner v-if="error" class="bg-red-1 text-negative q-mb-md" rounded>{{
      error
    }}</q-banner>
    <q-table
      flat
      bordered
      class="tabla-app"
      :rows="filas"
      :columns="columnas"
      row-key="id"
      v-model:pagination="paginacion"
      :loading="cargando"
      :rows-per-page-options="[10, 20, 50]"
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
    </q-table>

    <q-dialog v-model="abierto">
      <q-card style="width: min(760px, 96vw)">
        <q-card-section class="row items-center">
          <div class="text-h6">Devolución {{ detalle?.id }}</div>
          <q-space />
          <q-btn flat round icon="close" v-close-popup />
        </q-card-section>
        <q-card-section v-if="detalle">
          <div class="q-mb-sm">
            Compra {{ detalle.compraId }} · {{ fecha(detalle.fecha) }} ·
            compensación {{ detalle.porcentajeCompensacion ?? '—' }} %
          </div>
          <div class="q-mb-sm">{{ detalle.motivo }}</div>
          <q-markup-table flat bordered dense class="tabla-app q-mb-md">
            <thead>
              <tr>
                <th class="text-left">Lote</th>
                <th class="text-left">Almacén</th>
                <th class="text-right">Cantidad</th>
                <th class="text-right">Costo</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="linea in detalle.lineas || []"
                :key="linea.id || linea.loteId"
              >
                <td>{{ linea.numLote || linea.loteId }}</td>
                <td>{{ linea.almacen || linea.almacenId }}</td>
                <td class="text-right">{{ cantidad(linea.cantidad) }}</td>
                <td class="text-right">{{
                  moneda(linea.costoUnitario || linea.precio)
                }}</td>
              </tr>
            </tbody>
          </q-markup-table>
          <visor-partida :partida="detalle.partida" />
        </q-card-section>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import SelectorBusqueda from '@/components/SelectorBusqueda.vue'
import VisorPartida from '@/components/VisorPartida.vue'
import { useListado } from '@/composables/useListado'
import { usePermiso } from '@/composables/usePermiso'
import { api, listarTodo } from '@/services/api'
import { mapaParametros, numeroParametro } from '@/services/parametros'
import { avisarExito } from '@/utils/dialogo'
import {
  cantidad,
  fecha,
  hoy,
  moneda,
  sumarDias,
  textoRelacion
} from '@/utils/formato'

const { permitido, idEmpleado } = usePermiso()
const puedeEscribir = computed(() =>
  permitido('devolucionesCompra', 'escritura')
)

const diasMax = ref(30)
const compras = ref([])
const lineas = ref([])
const errorCatalogos = ref('')
const guardando = ref(false)
const form = reactive({
  compraId: null,
  fecha: hoy(),
  porcentajeCompensacion: 100,
  motivo: ''
})

const filtros = reactive({ desde: '', hasta: '' })
const { filas, cargando, error, paginacion, cargar, reiniciar } = useListado(
  '/devoluciones-compra',
  () => ({ ...filtros })
)
const columnas = [
  { name: 'fecha', label: 'Fecha', field: 'fecha', align: 'left' },
  { name: 'compraId', label: 'Compra', field: 'compraId', align: 'left' },
  {
    name: 'porcentaje',
    label: '% compensación',
    field: fila => fila.porcentajeCompensacion ?? '—',
    align: 'right'
  },
  { name: 'motivo', label: 'Motivo', field: 'motivo', align: 'left' },
  { name: 'acciones', label: '', field: 'acciones', align: 'right' }
]
const abierto = ref(false)
const detalle = ref(null)

const puedeEnviar = computed(
  () =>
    form.compraId &&
    form.motivo.trim() &&
    lineas.value.some(linea => Number(linea.devolver) > 0)
)

function etiquetaCompra(compra) {
  return `${fecha(compra.fecha)} · ${compra.numeroDocumento || compra.id} · ${textoRelacion(compra, 'proveedor', 'proveedorNombre')}`
}

async function cargarCompra() {
  lineas.value = []
  if (!form.compraId) return
  try {
    const { data } = await api.get(`/compras/${form.compraId}`)
    lineas.value = (data.lineas || []).map((linea, indice) => ({
      clave: linea.id || indice,
      loteId: linea.loteId,
      almacenId: linea.almacenId,
      numLote: linea.numLote,
      almacen: linea.almacen,
      comprada: Number(linea.cantidad) || 0,
      devolver: 0
    }))
  } catch {
    lineas.value = []
  }
}

async function guardar() {
  const seleccion = lineas.value.filter(linea => Number(linea.devolver) > 0)
  if (seleccion.some(linea => Number(linea.devolver) > linea.comprada)) {
    errorCatalogos.value =
      'La cantidad a devolver supera lo comprado en alguna línea.'
    return
  }
  guardando.value = true
  const cuerpo = {
    compraId: form.compraId,
    fecha: form.fecha,
    porcentajeCompensacion: Number(form.porcentajeCompensacion),
    motivo: form.motivo.trim(),
    lineas: seleccion.map(linea => ({
      loteId: linea.loteId,
      almacenId: linea.almacenId,
      cantidad: Number(linea.devolver)
    }))
  }
  if (idEmpleado.value) cuerpo.empleadoId = idEmpleado.value
  try {
    await api.post('/devoluciones-compra', cuerpo)
    avisarExito('Devolución registrada.')
    form.compraId = null
    form.motivo = ''
    form.porcentajeCompensacion = 100
    lineas.value = []
    await reiniciar()
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

async function ver(fila) {
  abierto.value = true
  detalle.value = null
  try {
    const { data } = await api.get(`/devoluciones-compra/${fila.id}`)
    detalle.value = data
  } catch {
    abierto.value = false
  }
}

onMounted(async () => {
  try {
    const params = await mapaParametros()
    diasMax.value = numeroParametro(params, 'devolucion_compra.dias_max', 30)
    const desde = sumarDias(hoy(), -diasMax.value)
    compras.value = await listarTodo('/compras', { desde, hasta: hoy() })
  } catch (causa) {
    errorCatalogos.value = causa.mensaje || 'No se pudieron cargar las compras.'
  }
  await cargar()
})
</script>
