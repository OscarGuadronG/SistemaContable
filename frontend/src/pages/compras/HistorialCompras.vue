<template>
  <div>
    <div class="row q-col-gutter-sm items-end q-mb-md">
      <div class="col-6 col-md-3">
        <q-input
          v-model="filtros.desde"
          type="date"
          outlined
          dense
          label="Desde"
        />
      </div>
      <div class="col-6 col-md-3">
        <q-input
          v-model="filtros.hasta"
          type="date"
          outlined
          dense
          label="Hasta"
        />
      </div>
      <div class="col-12 col-md-3">
        <selector-busqueda
          v-model="filtros.proveedorId"
          :opciones="proveedores"
          etiqueta="Proveedor"
        />
      </div>
      <div class="col-6 col-md-2">
        <q-select
          v-model="filtros.formaPago"
          :options="FORMAS_PAGO"
          emit-value
          map-options
          clearable
          outlined
          dense
          label="Forma de pago"
        />
      </div>
      <div class="col-auto">
        <q-btn color="primary" outline label="Buscar" @click="reiniciar" />
      </div>
    </div>
    <q-banner v-if="error" class="bg-red-1 text-negative q-mb-md" rounded>
      {{ error }}
      <template #action>
        <q-btn flat color="negative" label="Reintentar" @click="reiniciar" />
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
      <template #body-cell-total="props">
        <q-td :props="props" class="text-right monto">{{
          moneda(montoDe(props.row))
        }}</q-td>
      </template>
      <template #body-cell-formaPago="props">
        <q-td :props="props">{{ etiquetaFormaPago(props.row.formaPago) }}</q-td>
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
        <div class="full-width text-center q-pa-lg text-grey-7"
          >No hay compras en este rango.</div
        >
      </template>
    </q-table>

    <q-dialog v-model="abierto">
      <q-card style="width: min(960px, 96vw)">
        <q-card-section class="row items-center">
          <div class="text-h6"
            >Compra {{ detalle?.numeroDocumento || detalle?.id }}</div
          >
          <q-space />
          <q-btn flat round icon="close" v-close-popup />
        </q-card-section>
        <q-separator />
        <q-card-section v-if="cargandoDetalle" class="text-center">
          <q-spinner color="primary" size="32px" />
        </q-card-section>
        <q-card-section v-else-if="detalle">
          <div class="row q-col-gutter-sm q-mb-md text-body2">
            <div class="col-6 col-md-3"
              ><strong>Fecha:</strong> {{ fecha(detalle.fecha) }}</div
            >
            <div class="col-6 col-md-3"
              ><strong>Proveedor:</strong>
              {{ textoRelacion(detalle, 'proveedor', 'proveedorNombre') }}</div
            >
            <div class="col-6 col-md-3"
              ><strong>Empleado:</strong>
              {{ textoRelacion(detalle, 'empleado', 'empleadoNombre') }}</div
            >
            <div class="col-6 col-md-3"
              ><strong>Pago:</strong>
              {{ etiquetaFormaPago(detalle.formaPago) }}</div
            >
          </div>
          <div class="text-subtitle2 q-mb-xs">Líneas</div>
          <q-markup-table flat bordered dense class="tabla-app q-mb-md">
            <thead>
              <tr>
                <th class="text-left">Lote / producto</th>
                <th class="text-left">Almacén</th>
                <th class="text-right">Cantidad</th>
                <th class="text-right">Precio</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="linea in detalle.lineas || []" :key="linea.id">
                <td>{{ linea.producto || linea.numLote || linea.loteId }}</td>
                <td>{{ linea.almacen || linea.almacenId }}</td>
                <td class="text-right monto">{{ cantidad(linea.cantidad) }}</td>
                <td class="text-right monto">{{ moneda(linea.precio) }}</td>
              </tr>
            </tbody>
          </q-markup-table>
          <resumen-totales class="q-mb-md" :filas="resumenDetalle" />
          <div class="text-subtitle2 q-mb-xs">Kardex</div>
          <q-markup-table flat bordered dense class="tabla-app q-mb-md">
            <thead>
              <tr>
                <th class="text-left">Tipo</th>
                <th class="text-left">Fecha</th>
                <th class="text-left">Destino</th>
                <th class="text-left">Descripción</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="mov in kardex" :key="mov.id">
                <td>{{ mov.tipoMovimiento || mov.tipo }}</td>
                <td>{{ fecha(mov.fecha) }}</td>
                <td>{{ mov.destino || mov.destinoId || '—' }}</td>
                <td>{{ mov.descripcion || '—' }}</td>
              </tr>
              <tr v-if="kardex.length === 0">
                <td colspan="4" class="text-grey">Sin movimientos</td>
              </tr>
            </tbody>
          </q-markup-table>
          <div class="text-subtitle2 q-mb-xs">Partida</div>
          <visor-partida :partida="detalle.partida" />
        </q-card-section>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import SelectorBusqueda from '@/components/SelectorBusqueda.vue'
import ResumenTotales from '@/components/ResumenTotales.vue'
import VisorPartida from '@/components/VisorPartida.vue'
import { useListado } from '@/composables/useListado'
import { api, listarTodo } from '@/services/api'
import {
  FORMAS_PAGO,
  cantidad,
  etiquetaFormaPago,
  fecha,
  moneda,
  textoRelacion
} from '@/utils/formato'

const filtros = reactive({
  desde: '',
  hasta: '',
  proveedorId: null,
  formaPago: null
})
const proveedores = ref([])
const { filas, cargando, error, paginacion, cargar, reiniciar } = useListado(
  '/compras',
  () => ({ ...filtros })
)

const columnas = [
  { name: 'fecha', label: 'Fecha', field: 'fecha', align: 'left' },
  {
    name: 'documento',
    label: 'Documento',
    field: fila => fila.numeroDocumento || fila.id,
    align: 'left'
  },
  {
    name: 'proveedor',
    label: 'Proveedor',
    field: fila => textoRelacion(fila, 'proveedor', 'proveedorNombre'),
    align: 'left'
  },
  { name: 'formaPago', label: 'Pago', field: 'formaPago', align: 'left' },
  { name: 'total', label: 'Total', field: 'total', align: 'right' },
  { name: 'acciones', label: '', field: 'acciones', align: 'right' }
]

const abierto = ref(false)
const detalle = ref(null)
const cargandoDetalle = ref(false)

const kardex = computed(() => {
  const valor = detalle.value?.kardex
  if (!valor) return []
  return Array.isArray(valor) ? valor : [valor]
})

const resumenDetalle = computed(() => {
  const doc = detalle.value || {}
  return [
    { etiqueta: 'Subtotal', valor: moneda(doc.subtotal) },
    { etiqueta: 'Descuento', valor: moneda(doc.descuento) },
    { etiqueta: 'IVA', valor: moneda(doc.iva) },
    { etiqueta: 'Gastos', valor: moneda(doc.gastosAdicionales) },
    { etiqueta: 'Total', valor: moneda(doc.total), destacado: true }
  ]
})

function montoDe(fila) {
  return fila.total ?? fila.subtotal
}

async function ver(fila) {
  abierto.value = true
  cargandoDetalle.value = true
  detalle.value = null
  try {
    const { data } = await api.get(`/compras/${fila.id}`)
    detalle.value = data
  } catch {
    abierto.value = false
  } finally {
    cargandoDetalle.value = false
  }
}

onMounted(async () => {
  try {
    proveedores.value = await listarTodo('/proveedores')
  } catch {
    proveedores.value = []
  }
  await cargar()
})
</script>
