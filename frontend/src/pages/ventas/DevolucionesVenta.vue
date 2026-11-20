<template>
  <div>
    <q-card v-if="puedeEscribir" flat bordered class="q-mb-lg">
      <q-card-section class="text-subtitle1 text-weight-medium">
        Nueva devolución de cliente
      </q-card-section>
      <q-card-section>
        <div class="row q-col-gutter-sm">
          <div class="col-12 col-md-6">
            <selector-busqueda
              v-model="form.ventaId"
              :opciones="ventas"
              etiqueta="Venta (dentro del plazo)"
              :opcion-etiqueta="etiquetaVenta"
              @update:model-value="cargarVenta"
            />
          </div>
          <div class="col-12 col-md-3">
            <q-input
              v-model="form.fecha"
              type="date"
              outlined
              dense
              label="Fecha"
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
        <q-banner
          v-if="aviso"
          class="bg-orange-1 text-orange-10 q-mt-sm"
          rounded
          dense
          >{{ aviso }}</q-banner
        >
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
              <th class="text-right">Vendido</th>
              <th class="text-right">A devolver</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="linea in lineas" :key="linea.clave">
              <td>{{ linea.numLote || linea.loteId }}</td>
              <td>{{ linea.almacen || linea.almacenId }}</td>
              <td class="text-right">{{ cantidad(linea.vendida) }}</td>
              <td style="width: 140px">
                <q-input
                  v-model.number="linea.devolver"
                  type="number"
                  min="0"
                  :max="linea.vendida"
                  step="0.0001"
                  dense
                  outlined
                />
              </td>
            </tr>
          </tbody>
        </q-markup-table>
        <div class="text-caption text-grey-7 q-mt-sm"
          >Plazo máximo: {{ diasMax }} días.</div
        >
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
      <div class="col-auto"
        ><q-btn color="primary" outline label="Buscar" @click="reiniciar"
      /></div>
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
      <template #body-cell-fecha="props"
        ><q-td :props="props">{{ fecha(props.row.fecha) }}</q-td></template
      >
      <template #body-cell-acciones="props">
        <q-td :props="props"
          ><q-btn
            flat
            dense
            color="primary"
            label="Detalle"
            @click="ver(props.row)"
        /></q-td>
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
          <div class="q-mb-sm"
            >Venta {{ detalle.ventaId }} · {{ fecha(detalle.fecha) }}</div
          >
          <div class="q-mb-sm">{{ detalle.motivo }}</div>
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
import { cantidad, fecha, hoy, sumarDias, textoRelacion } from '@/utils/formato'

const { permitido, idEmpleado } = usePermiso()
const puedeEscribir = computed(() =>
  permitido('devolucionesVenta', 'escritura')
)
const diasMax = ref(30)
const ventas = ref([])
const lineas = ref([])
const aviso = ref('')
const guardando = ref(false)
const form = reactive({ ventaId: null, fecha: hoy(), motivo: '' })
const filtros = reactive({ desde: '', hasta: '' })
const { filas, cargando, error, paginacion, cargar, reiniciar } = useListado(
  '/devoluciones-venta',
  () => ({ ...filtros })
)
const columnas = [
  { name: 'fecha', label: 'Fecha', field: 'fecha', align: 'left' },
  { name: 'ventaId', label: 'Venta', field: 'ventaId', align: 'left' },
  { name: 'motivo', label: 'Motivo', field: 'motivo', align: 'left' },
  { name: 'acciones', label: '', field: 'acciones', align: 'right' }
]
const abierto = ref(false)
const detalle = ref(null)
const puedeEnviar = computed(
  () =>
    form.ventaId &&
    form.motivo.trim() &&
    lineas.value.some(linea => Number(linea.devolver) > 0)
)

function etiquetaVenta(venta) {
  return `${fecha(venta.fecha)} · ${venta.numeroDocumento || venta.id} · ${textoRelacion(venta, 'cliente', 'clienteNombre')}`
}

async function cargarVenta() {
  lineas.value = []
  aviso.value = ''
  if (!form.ventaId) return
  try {
    const { data } = await api.get(`/ventas/${form.ventaId}`)
    lineas.value = (data.lineas || []).map((linea, indice) => ({
      clave: linea.id || indice,
      loteId: linea.loteId,
      almacenId: linea.almacenId || data.almacenId,
      numLote: linea.numLote || linea.lote?.numLote,
      almacen: linea.almacen,
      vendida: Number(linea.cantidad) || 0,
      devolver: 0
    }))
  } catch (causa) {
    aviso.value = causa.mensaje || 'No se pudo leer la venta.'
  }
}

async function guardar() {
  if (lineas.value.some(linea => Number(linea.devolver) > linea.vendida)) {
    aviso.value = 'La cantidad a devolver supera lo vendido.'
    return
  }
  guardando.value = true
  const cuerpo = {
    ventaId: form.ventaId,
    fecha: form.fecha,
    motivo: form.motivo.trim(),
    lineas: lineas.value
      .filter(linea => Number(linea.devolver) > 0)
      .map(linea => ({
        loteId: linea.loteId,
        almacenId: linea.almacenId,
        cantidad: Number(linea.devolver)
      }))
  }
  if (idEmpleado.value) cuerpo.empleadoId = idEmpleado.value
  try {
    await api.post('/devoluciones-venta', cuerpo)
    avisarExito('Devolución registrada.')
    form.ventaId = null
    form.motivo = ''
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
  try {
    const { data } = await api.get(`/devoluciones-venta/${fila.id}`)
    detalle.value = data
  } catch {
    abierto.value = false
  }
}

onMounted(async () => {
  try {
    const params = await mapaParametros()
    diasMax.value = numeroParametro(params, 'devolucion_venta.dias_max', 30)
    ventas.value = await listarTodo('/ventas', {
      desde: sumarDias(hoy(), -diasMax.value),
      hasta: hoy()
    })
  } catch (causa) {
    aviso.value = causa.mensaje || 'No se pudieron cargar las ventas.'
  }
  await cargar()
})
</script>
