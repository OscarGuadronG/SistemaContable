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
          v-model="filtros.clienteId"
          :opciones="clientes"
          etiqueta="Cliente"
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
          label="Pago"
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
      :rows-per-page-options="[10, 20, 50, 100]"
      @request="cargar"
    >
      <template #body-cell-fecha="props">
        <q-td :props="props">{{ fecha(props.row.fecha) }}</q-td>
      </template>
      <template #body-cell-total="props">
        <q-td :props="props" class="text-right monto">{{
          moneda(props.row.total)
        }}</q-td>
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
      <q-card style="width: min(960px, 96vw)">
        <q-card-section class="row items-center">
          <div class="text-h6"
            >Venta {{ detalle?.numeroDocumento || detalle?.id }}</div
          >
          <q-space />
          <q-btn flat round icon="close" v-close-popup />
        </q-card-section>
        <q-card-section v-if="detalle">
          <div class="row q-col-gutter-sm q-mb-md">
            <div class="col-6 col-md-3"
              ><strong>Fecha:</strong> {{ fecha(detalle.fecha) }}</div
            >
            <div class="col-6 col-md-3"
              ><strong>Cliente:</strong>
              {{ textoRelacion(detalle, 'cliente', 'clienteNombre') }}</div
            >
            <div class="col-6 col-md-3"
              ><strong>Pago:</strong>
              {{ etiquetaFormaPago(detalle.formaPago) }}</div
            >
            <div class="col-6 col-md-3"
              ><strong>Costo:</strong> {{ moneda(detalle.costoTotal) }}</div
            >
          </div>
          <div class="text-subtitle2 q-mb-xs">Lotes usados (FIFO)</div>
          <q-markup-table flat bordered dense class="tabla-app q-mb-md">
            <thead>
              <tr>
                <th class="text-left">Producto</th>
                <th class="text-left">Lote</th>
                <th class="text-left">Almacén</th>
                <th class="text-right">Cantidad</th>
                <th class="text-right">Precio</th>
                <th class="text-right">Costo unitario</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="linea in detalle.lineas || []"
                :key="linea.id || linea.loteId"
              >
                <td>{{ linea.producto || linea.productoNombre || '—' }}</td>
                <td>{{
                  linea.numLote || linea.lote?.numLote || linea.loteId
                }}</td>
                <td>{{ linea.almacen || linea.almacenId }}</td>
                <td class="text-right monto">{{ cantidad(linea.cantidad) }}</td>
                <td class="text-right monto">{{ moneda(linea.precio) }}</td>
                <td class="text-right monto">{{
                  moneda(linea.costoUnitario)
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
import { onMounted, reactive, ref } from 'vue'
import SelectorBusqueda from '@/components/SelectorBusqueda.vue'
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
  clienteId: null,
  formaPago: null
})
const clientes = ref([])
const { filas, cargando, error, paginacion, cargar, reiniciar } = useListado(
  '/ventas',
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
    name: 'cliente',
    label: 'Cliente',
    field: fila => textoRelacion(fila, 'cliente', 'clienteNombre'),
    align: 'left'
  },
  {
    name: 'formaPago',
    label: 'Pago',
    field: fila => etiquetaFormaPago(fila.formaPago),
    align: 'left'
  },
  { name: 'total', label: 'Total', field: 'total', align: 'right' },
  { name: 'acciones', label: '', field: 'acciones', align: 'right' }
]
const abierto = ref(false)
const detalle = ref(null)

async function ver(fila) {
  abierto.value = true
  detalle.value = null
  try {
    const { data } = await api.get(`/ventas/${fila.id}`)
    detalle.value = data
  } catch {
    abierto.value = false
  }
}

onMounted(async () => {
  try {
    clientes.value = await listarTodo('/clientes')
  } catch {
    clientes.value = []
  }
  await cargar()
})
</script>
