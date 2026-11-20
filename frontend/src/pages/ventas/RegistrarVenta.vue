<template>
  <div>
    <q-banner v-if="error" class="bg-red-1 text-negative q-mb-md" rounded>
      {{ error }}
    </q-banner>
    <div class="row q-col-gutter-md">
      <div class="col-12 col-lg-8">
        <div class="row q-col-gutter-sm q-mb-md">
          <div class="col-12 col-md-6">
            <selector-busqueda
              v-model="almacenId"
              :opciones="almacenes"
              etiqueta="Almacén de la venta"
              :rules="[requerido('Seleccione el almacén')]"
              @update:model-value="cambiarAlmacen"
            />
          </div>
          <div class="col-12 col-md-6">
            <q-input
              v-model="buscar"
              outlined
              dense
              clearable
              label="Buscar producto"
            >
              <template #prepend><q-icon name="search" /></template>
            </q-input>
          </div>
        </div>
        <q-table
          flat
          bordered
          class="tabla-app"
          :rows="productosFiltrados"
          :columns="columnasPrecio"
          row-key="productoId"
          :loading="cargandoPrecios"
          :rows-per-page-options="[10, 20, 50]"
        >
          <template #body-cell-precioVenta="props">
            <q-td :props="props" class="text-right monto">{{
              moneda(props.row.precioVenta)
            }}</q-td>
          </template>
          <template #body-cell-existencia="props">
            <q-td :props="props" class="text-right monto">{{
              cantidad(disponible(props.row))
            }}</q-td>
          </template>
          <template #body-cell-agregar="props">
            <q-td :props="props">
              <q-btn
                flat
                dense
                color="primary"
                icon="add"
                label="Agregar"
                :disable="disponible(props.row) <= 0"
                @click="agregar(props.row)"
              />
            </q-td>
          </template>
          <template #no-data>
            <div class="full-width text-center q-pa-md text-grey-7">
              Elija un almacén para ver productos con existencia.
            </div>
          </template>
        </q-table>

        <div class="text-subtitle2 q-mt-md q-mb-sm">Detalle de la venta</div>
        <q-markup-table flat bordered dense class="tabla-app">
          <thead>
            <tr>
              <th class="text-left">Producto</th>
              <th class="text-right">Precio</th>
              <th class="text-right">Cantidad</th>
              <th class="text-right">Importe</th>
              <th />
            </tr>
          </thead>
          <tbody>
            <tr v-for="linea in lineas" :key="linea.productoId">
              <td>{{ linea.nombre }}</td>
              <td class="text-right monto">{{ moneda(linea.precio) }}</td>
              <td style="width: 130px">
                <q-input
                  v-model.number="linea.cantidad"
                  type="number"
                  min="0.0001"
                  :max="linea.existencia"
                  step="0.0001"
                  dense
                  outlined
                  :error="linea.cantidad > linea.existencia"
                  error-message="Supera la existencia"
                />
              </td>
              <td class="text-right monto">{{
                moneda(linea.cantidad * linea.precio)
              }}</td>
              <td>
                <q-btn
                  flat
                  round
                  dense
                  icon="close"
                  color="negative"
                  @click="quitar(linea.productoId)"
                />
              </td>
            </tr>
            <tr v-if="lineas.length === 0">
              <td colspan="5" class="text-grey"
                >Todavía no hay productos en la venta.</td
              >
            </tr>
          </tbody>
        </q-markup-table>
      </div>
      <div class="col-12 col-lg-4">
        <q-form class="q-gutter-sm" @submit.prevent="guardar">
          <selector-busqueda
            v-model="form.clienteId"
            :opciones="clientes"
            etiqueta="Cliente"
            :rules="[requerido('Seleccione el cliente')]"
          />
          <selector-busqueda
            v-model="form.empleadoId"
            :opciones="empleados"
            :opcion-etiqueta="e => e.nombreCompleto"
            etiqueta="Empleado"
          />
          <q-input
            v-model="form.fecha"
            type="date"
            outlined
            dense
            label="Fecha"
            :rules="[requerido('Indique la fecha')]"
          />
          <q-input
            v-model="form.numeroDocumento"
            outlined
            dense
            label="N.º de documento"
            :rules="[requerido('Indique el documento')]"
          />
          <q-select
            v-model="form.formaPago"
            :options="FORMAS_PAGO"
            emit-value
            map-options
            outlined
            dense
            label="Forma de pago"
          />
          <q-input
            v-if="form.formaPago === 'Cheque'"
            v-model="form.numeroCheque"
            outlined
            dense
            label="Número de cheque"
            :rules="[requerido('Indique el número de cheque')]"
          />
          <q-btn-toggle
            v-model="descuentoModo"
            spread
            no-caps
            unelevated
            toggle-color="primary"
            :options="[
              { label: 'Descuento $', value: 'monto' },
              { label: 'Descuento %', value: 'porcentaje' }
            ]"
          />
          <q-input
            v-model.number="descuentoValor"
            type="number"
            min="0"
            :max="descuentoModo === 'porcentaje' ? 100 : undefined"
            step="0.01"
            outlined
            dense
            :label="descuentoModo === 'porcentaje' ? 'Porcentaje' : 'Monto'"
          />
          <q-input
            v-model="form.descripcion"
            outlined
            dense
            autogrow
            label="Descripción"
          />
          <resumen-totales :filas="filasResumen" />
          <q-banner
            v-if="hayExceso"
            class="bg-orange-1 text-orange-10"
            rounded
            dense
          >
            Hay cantidades mayores que la existencia del almacén.
          </q-banner>
          <q-btn
            color="primary"
            unelevated
            class="full-width"
            type="submit"
            label="Registrar venta"
            :loading="guardando"
            :disable="hayExceso || lineas.length === 0"
          />
        </q-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import SelectorBusqueda from '@/components/SelectorBusqueda.vue'
import ResumenTotales from '@/components/ResumenTotales.vue'
import { usePermiso } from '@/composables/usePermiso'
import { api, listarTodo } from '@/services/api'
import { mapaParametros, numeroParametro } from '@/services/parametros'
import { totalesVenta } from '@/utils/calculos'
import { avisarExito } from '@/utils/dialogo'
import {
  FORMAS_PAGO,
  cantidad,
  hoy,
  moneda,
  redondear,
  requerido
} from '@/utils/formato'

const { idEmpleado } = usePermiso()
const almacenes = ref([])
const clientes = ref([])
const empleados = ref([])
const precios = ref([])
const lineas = ref([])
const almacenId = ref(null)
const buscar = ref('')
const cargandoPrecios = ref(false)
const guardando = ref(false)
const error = ref('')
const tasaIva = ref(0.13)
const descuentoModo = ref('monto')
const descuentoValor = ref(0)
const form = reactive({
  clienteId: null,
  empleadoId: null,
  fecha: hoy(),
  numeroDocumento: '',
  descripcion: '',
  formaPago: 'Contado',
  numeroCheque: ''
})

const columnasPrecio = [
  { name: 'nombre', label: 'Producto', field: 'nombre', align: 'left' },
  {
    name: 'unidad',
    label: 'Unidad',
    field: fila => fila.unidad?.abrev || fila.unidad || '—',
    align: 'left'
  },
  {
    name: 'existencia',
    label: 'Disponible',
    field: 'existencia',
    align: 'right'
  },
  {
    name: 'precioVenta',
    label: 'Precio',
    field: 'precioVenta',
    align: 'right'
  },
  { name: 'agregar', label: '', field: 'agregar', align: 'right' }
]

const productosFiltrados = computed(() => {
  const consulta = buscar.value.trim().toLowerCase()
  if (!consulta) return precios.value
  return precios.value.filter(item =>
    `${item.nombre} ${item.tipo?.nombre || item.tipo || ''}`
      .toLowerCase()
      .includes(consulta)
  )
})

function enCarrito(productoId) {
  return (
    lineas.value.find(linea => linea.productoId === productoId)?.cantidad || 0
  )
}

function disponible(producto) {
  return Number(producto.existencia || 0) - enCarrito(producto.productoId)
}

const descuentoMonto = computed(() => {
  const subtotal = lineas.value.reduce(
    (suma, linea) =>
      suma + Number(linea.cantidad || 0) * Number(linea.precio || 0),
    0
  )
  if (descuentoModo.value === 'porcentaje') {
    return redondear((subtotal * Number(descuentoValor.value || 0)) / 100)
  }
  return redondear(descuentoValor.value || 0)
})

const totales = computed(() =>
  totalesVenta(lineas.value, descuentoMonto.value, tasaIva.value)
)
const hayExceso = computed(() =>
  lineas.value.some(linea => Number(linea.cantidad) > Number(linea.existencia))
)
const filasResumen = computed(() => [
  { etiqueta: 'Subtotal', valor: moneda(totales.value.subtotal) },
  { etiqueta: 'Descuento', valor: moneda(totales.value.descuento) },
  {
    etiqueta: `IVA (${Math.round(tasaIva.value * 100)} %)`,
    valor: moneda(totales.value.iva)
  },
  { etiqueta: 'Total', valor: moneda(totales.value.total), destacado: true }
])

async function cambiarAlmacen() {
  if (lineas.value.length) lineas.value = []
  precios.value = []
  if (!almacenId.value) return
  cargandoPrecios.value = true
  error.value = ''
  try {
    const { data } = await api.get('/ventas/precios', {
      query: { almacenId: almacenId.value }
    })
    precios.value = Array.isArray(data) ? data : []
  } catch (causa) {
    error.value = causa.mensaje || 'No se pudieron cargar los precios.'
  } finally {
    cargandoPrecios.value = false
  }
}

function agregar(producto) {
  const actual = lineas.value.find(
    linea => linea.productoId === producto.productoId
  )
  if (actual) {
    if (actual.cantidad + 1 <= Number(producto.existencia)) actual.cantidad += 1
    return
  }
  lineas.value.push({
    productoId: producto.productoId,
    nombre: producto.nombre,
    precio: Number(producto.precioVenta) || 0,
    existencia: Number(producto.existencia) || 0,
    cantidad: 1
  })
}

function quitar(productoId) {
  lineas.value = lineas.value.filter(linea => linea.productoId !== productoId)
}

async function guardar() {
  if (hayExceso.value || !almacenId.value) return
  guardando.value = true
  const cuerpo = {
    clienteId: form.clienteId,
    almacenId: almacenId.value,
    fecha: form.fecha,
    numeroDocumento: form.numeroDocumento.trim(),
    descripcion: form.descripcion?.trim() || null,
    formaPago: form.formaPago,
    descuento: totales.value.descuento,
    lineas: lineas.value.map(linea => ({
      productoId: linea.productoId,
      cantidad: Number(linea.cantidad)
    }))
  }
  if (form.empleadoId) cuerpo.empleadoId = form.empleadoId
  if (form.formaPago === 'Cheque')
    cuerpo.numeroCheque = form.numeroCheque.trim()
  try {
    await api.post('/ventas', cuerpo)
    avisarExito('Venta registrada.')
    lineas.value = []
    form.numeroDocumento = ''
    form.descripcion = ''
    form.numeroCheque = ''
    descuentoValor.value = 0
    await cambiarAlmacen()
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

onMounted(async () => {
  try {
    const [alms, clis, empl, params] = await Promise.all([
      listarTodo('/almacenes'),
      listarTodo('/clientes'),
      listarTodo('/empleados/seleccion'),
      mapaParametros()
    ])
    almacenes.value = alms.filter(item => item.estado !== false)
    clientes.value = clis
    empleados.value = empl
    tasaIva.value = numeroParametro(params, 'iva.tasa', 0.13)
    form.empleadoId = idEmpleado.value || null
  } catch (causa) {
    error.value = causa.mensaje || 'No se pudieron cargar los catálogos.'
  }
})
</script>
