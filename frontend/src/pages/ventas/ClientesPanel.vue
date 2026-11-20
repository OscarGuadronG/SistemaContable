<template>
  <div>
    <crud-simple
      ref="crud"
      ruta="/clientes"
      titulo="cliente"
      etiqueta-nuevo="Nuevo cliente"
      :puede-escribir="puedeEscribir"
      :columnas="columnas"
      :campos="campos"
    >
      <template #extra="{ fila }">
        <q-btn flat dense color="primary" label="Perfil" @click="ver(fila)" />
      </template>
    </crud-simple>

    <q-dialog v-model="abierto">
      <q-card style="width: min(860px, 96vw)">
        <q-card-section class="row items-center">
          <div>
            <div class="text-h6">{{ cliente?.nombre }}</div>
            <div class="text-caption">NIT {{ cliente?.nit || '—' }}</div>
          </div>
          <q-space />
          <q-btn flat round icon="close" v-close-popup />
        </q-card-section>
        <q-card-section>
          <div class="text-body2 q-mb-md">{{
            cliente?.descripcion || 'Sin descripción'
          }}</div>
          <div class="text-subtitle2 q-mb-sm">Ventas del cliente</div>
          <q-banner
            v-if="error"
            class="bg-red-1 text-negative q-mb-sm"
            rounded
            >{{ error }}</q-banner
          >
          <q-table
            flat
            bordered
            dense
            class="tabla-app"
            :rows="ventas"
            :columns="columnasVentas"
            row-key="id"
            :loading="cargando"
            :rows-per-page-options="[10, 20]"
          >
            <template #body-cell-fecha="props">
              <q-td :props="props">{{ fecha(props.row.fecha) }}</q-td>
            </template>
            <template #body-cell-total="props">
              <q-td :props="props" class="text-right">{{
                moneda(props.row.total)
              }}</q-td>
            </template>
          </q-table>
        </q-card-section>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import CrudSimple from '@/components/CrudSimple.vue'
import { listarTodo } from '@/services/api'
import { etiquetaFormaPago, fecha, moneda, requerido } from '@/utils/formato'

defineProps({
  puedeEscribir: { type: Boolean, default: false }
})

const columnas = [
  { name: 'nombre', label: 'Nombre', field: 'nombre', align: 'left' },
  { name: 'nit', label: 'NIT', field: 'nit', align: 'left' },
  {
    name: 'descripcion',
    label: 'Descripción',
    field: 'descripcion',
    align: 'left'
  }
]
const campos = [
  { key: 'nombre', label: 'Nombre', reglas: [requerido('Indique el nombre')] },
  { key: 'nit', label: 'NIT', reglas: [requerido('Indique el NIT')] },
  { key: 'descripcion', label: 'Descripción', tipo: 'textarea' }
]
const columnasVentas = [
  { name: 'fecha', label: 'Fecha', field: 'fecha', align: 'left' },
  {
    name: 'documento',
    label: 'Documento',
    field: fila => fila.numeroDocumento || fila.id,
    align: 'left'
  },
  {
    name: 'forma',
    label: 'Pago',
    field: fila => etiquetaFormaPago(fila.formaPago),
    align: 'left'
  },
  { name: 'total', label: 'Total', field: 'total', align: 'right' }
]
const abierto = ref(false)
const cliente = ref(null)
const ventas = ref([])
const cargando = ref(false)
const error = ref('')

async function ver(fila) {
  cliente.value = fila
  abierto.value = true
  cargando.value = true
  error.value = ''
  ventas.value = []
  try {
    ventas.value = await listarTodo('/ventas', { clienteId: fila.id })
  } catch (causa) {
    error.value = causa.mensaje || 'No se pudieron cargar las ventas.'
  } finally {
    cargando.value = false
  }
}
</script>
