<template>
  <div class="zona-impresion">
    <div class="solo-imprimir q-mb-md">
      <div class="text-h6">Distribuidora Ta Bien</div>
      <div
        >Libro de compras · {{ fecha(filtros.desde) }} –
        {{ fecha(filtros.hasta) }}</div
      >
    </div>
    <div class="row q-col-gutter-sm items-end q-mb-md no-imprimir">
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
        <q-btn
          color="primary"
          outline
          label="Consultar"
          :loading="cargando"
          @click="cargar"
        />
      </div>
      <div class="col-auto">
        <q-btn
          outline
          icon="print"
          label="Imprimir"
          @click="imprimir({ horizontal: true })"
        />
      </div>
    </div>
    <q-banner
      v-if="error"
      class="bg-red-1 text-negative q-mb-md no-imprimir"
      rounded
    >
      {{ error }}
    </q-banner>
    <q-table
      flat
      bordered
      class="tabla-app"
      :rows="filas"
      :columns="columnas"
      row-key="compraId"
      :loading="cargando"
      hide-pagination
      :rows-per-page-options="[0]"
    >
      <template #body-cell-fecha="props">
        <q-td :props="props">{{ fecha(props.row.fecha) }}</q-td>
      </template>
      <template #bottom-row>
        <q-tr class="text-weight-bold">
          <q-td colspan="4">Totales</q-td>
          <q-td class="text-right monto">{{ moneda(totales.subtotal) }}</q-td>
          <q-td class="text-right monto">{{ moneda(totales.descuento) }}</q-td>
          <q-td class="text-right monto">{{
            moneda(totales.gastosAdicionales)
          }}</q-td>
          <q-td class="text-right monto">{{ moneda(totales.iva) }}</q-td>
          <q-td class="text-right monto">{{ moneda(totales.total) }}</q-td>
        </q-tr>
      </template>
      <template #no-data>
        <div class="full-width text-center q-pa-lg text-grey-7"
          >No hay compras en el periodo.</div
        >
      </template>
    </q-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '@/services/api'
import { imprimir } from '@/utils/dialogo'
import { fecha, hoy, moneda, sumarDias } from '@/utils/formato'

const filtros = reactive({ desde: sumarDias(hoy(), -30), hasta: hoy() })
const filas = ref([])
const totales = ref({})
const cargando = ref(false)
const error = ref('')

const columnas = [
  { name: 'fecha', label: 'Fecha', field: 'fecha', align: 'left' },
  {
    name: 'numeroDocumento',
    label: 'Documento',
    field: 'numeroDocumento',
    align: 'left'
  },
  { name: 'proveedor', label: 'Proveedor', field: 'proveedor', align: 'left' },
  { name: 'formaPago', label: 'Pago', field: 'formaPago', align: 'left' },
  {
    name: 'subtotal',
    label: 'Subtotal',
    field: 'subtotal',
    align: 'right',
    format: moneda
  },
  {
    name: 'descuento',
    label: 'Descuento',
    field: 'descuento',
    align: 'right',
    format: moneda
  },
  {
    name: 'gastosAdicionales',
    label: 'Gastos',
    field: 'gastosAdicionales',
    align: 'right',
    format: moneda
  },
  { name: 'iva', label: 'IVA', field: 'iva', align: 'right', format: moneda },
  {
    name: 'total',
    label: 'Total',
    field: 'total',
    align: 'right',
    format: moneda
  }
]

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const { data } = await api.get('/libros/compras', {
      query: { desde: filtros.desde, hasta: filtros.hasta }
    })
    filas.value = data?.filas || []
    totales.value = data?.totales || {}
  } catch (causa) {
    filas.value = []
    totales.value = {}
    error.value = causa.mensaje || 'No se pudo armar el libro de compras.'
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)
</script>
