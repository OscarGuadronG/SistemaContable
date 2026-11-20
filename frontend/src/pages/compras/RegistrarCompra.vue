<template>
  <div>
    <q-banner
      v-if="errorCatalogos"
      class="bg-red-1 text-negative q-mb-md"
      rounded
    >
      {{ errorCatalogos }}
      <template #action>
        <q-btn
          flat
          color="negative"
          label="Reintentar"
          @click="cargarCatalogos"
        />
      </template>
    </q-banner>
    <q-form @submit.prevent="guardar">
      <div class="row q-col-gutter-md">
        <div class="col-12 col-lg-8">
          <div class="row q-col-gutter-sm">
            <div class="col-12 col-sm-6">
              <selector-busqueda
                v-model="form.proveedorId"
                :opciones="proveedores"
                etiqueta="Proveedor"
                :rules="[requerido('Seleccione el proveedor')]"
              />
            </div>
            <div class="col-12 col-sm-6">
              <selector-busqueda
                v-model="form.empleadoId"
                :opciones="empleados"
                opcion-valor="id"
                :opcion-etiqueta="e => e.nombreCompleto"
                etiqueta="Empleado"
              />
            </div>
            <div class="col-6 col-sm-4">
              <q-input
                v-model="form.fecha"
                type="date"
                outlined
                dense
                label="Fecha"
                :rules="[requerido('Indique la fecha')]"
              />
            </div>
            <div class="col-6 col-sm-4">
              <q-input
                v-model="form.numeroDocumento"
                outlined
                dense
                label="N.º de documento"
                :rules="[requerido('Indique el documento')]"
              />
            </div>
            <div class="col-12 col-sm-4">
              <q-select
                v-model="form.formaPago"
                :options="FORMAS_PAGO"
                emit-value
                map-options
                outlined
                dense
                label="Forma de pago"
              />
            </div>
            <div v-if="form.formaPago === 'Cheque'" class="col-12 col-sm-4">
              <q-input
                v-model="form.numeroCheque"
                outlined
                dense
                label="Número de cheque"
                :rules="[requerido('Indique el número de cheque')]"
              />
            </div>
            <div class="col-6 col-sm-4">
              <q-input
                v-model.number="form.descuento"
                type="number"
                min="0"
                step="0.01"
                outlined
                dense
                label="Descuento ($)"
              />
            </div>
            <div class="col-6 col-sm-4">
              <q-input
                v-model.number="form.gastosAdicionales"
                type="number"
                min="0"
                step="0.01"
                outlined
                dense
                label="Gastos adicionales ($)"
              />
            </div>
            <div class="col-12">
              <q-input
                v-model="form.descripcion"
                outlined
                dense
                autogrow
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
              label="Agregar línea"
              @click="agregarLinea"
            />
          </div>

          <div
            v-for="(linea, indice) in form.lineas"
            :key="linea.clave"
            class="q-mb-md q-pa-sm rounded-borders"
            style="border: 1px solid #e4e8ef"
          >
            <div class="row q-col-gutter-sm items-start">
              <div class="col-12 col-sm-4">
                <q-btn-toggle
                  v-model="linea.modo"
                  spread
                  no-caps
                  unelevated
                  toggle-color="primary"
                  :options="[
                    { label: 'Lote existente', value: 'existente' },
                    { label: 'Lote nuevo', value: 'nuevo' }
                  ]"
                />
              </div>
              <div v-if="linea.modo === 'existente'" class="col-12 col-md-8">
                <selector-busqueda
                  v-model="linea.loteId"
                  :opciones="lotes"
                  etiqueta="Lote"
                  :opcion-etiqueta="etiquetaLote"
                  :rules="[requerido('Seleccione el lote')]"
                />
              </div>
              <template v-else>
                <div class="col-12 col-md-4">
                  <selector-busqueda
                    v-model="linea.productoId"
                    :opciones="productos"
                    etiqueta="Producto"
                    :rules="[requerido('Seleccione el producto')]"
                  />
                </div>
                <div class="col-6 col-md-2">
                  <q-input
                    v-model="linea.numLote"
                    outlined
                    dense
                    label="N.º de lote"
                    :rules="[requerido('Indique el lote')]"
                  />
                </div>
                <div class="col-6 col-md-2">
                  <q-input
                    v-model="linea.fechCaducidad"
                    type="date"
                    outlined
                    dense
                    label="Caducidad"
                  />
                </div>
              </template>
              <div class="col-12 col-sm-4">
                <selector-busqueda
                  v-model="linea.almacenId"
                  :opciones="almacenes"
                  etiqueta="Almacén"
                  :rules="[requerido('Seleccione el almacén')]"
                />
              </div>
              <div class="col-6 col-sm-2">
                <q-input
                  v-model.number="linea.cantidad"
                  type="number"
                  min="0.0001"
                  step="0.0001"
                  outlined
                  dense
                  label="Cantidad"
                  :rules="[numeroPositivo('Cantidad inválida')]"
                />
              </div>
              <div class="col-5 col-sm-2">
                <q-input
                  v-model.number="linea.precio"
                  type="number"
                  min="0"
                  step="0.01"
                  outlined
                  dense
                  label="Precio"
                  :rules="[numeroPositivo('Precio inválido', { cero: true })]"
                />
              </div>
              <div class="col-1">
                <q-btn
                  flat
                  round
                  color="negative"
                  icon="close"
                  :disable="form.lineas.length === 1"
                  @click="form.lineas.splice(indice, 1)"
                />
              </div>
              <div class="col-12 text-caption text-grey-7">
                Gasto asignado {{ moneda(totales.gastosLinea[indice] || 0) }}
                · costo unitario
                {{ moneda(costoUnitario(linea, indice)) }}
              </div>
            </div>
          </div>
        </div>
        <div class="col-12 col-lg-4">
          <div class="text-subtitle2 q-mb-sm">Vista previa</div>
          <resumen-totales :filas="filasResumen" />
          <q-banner
            v-if="descuentoInvalido"
            class="bg-orange-1 text-orange-10 q-mt-sm"
            rounded
            dense
          >
            El descuento no puede superar el subtotal.
          </q-banner>
          <q-btn
            class="full-width q-mt-md"
            color="primary"
            unelevated
            type="submit"
            label="Registrar compra"
            :loading="guardando"
            :disable="descuentoInvalido || form.lineas.length === 0"
          />
        </div>
      </div>
    </q-form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import SelectorBusqueda from '@/components/SelectorBusqueda.vue'
import ResumenTotales from '@/components/ResumenTotales.vue'
import { usePermiso } from '@/composables/usePermiso'
import { api, listarTodo } from '@/services/api'
import { mapaParametros, numeroParametro } from '@/services/parametros'
import { totalesCompra } from '@/utils/calculos'
import { avisarExito } from '@/utils/dialogo'
import {
  FORMAS_PAGO,
  hoy,
  moneda,
  numeroPositivo,
  redondear,
  requerido
} from '@/utils/formato'

const { idEmpleado } = usePermiso()
const proveedores = ref([])
const empleados = ref([])
const productos = ref([])
const lotes = ref([])
const almacenes = ref([])
const tasaIva = ref(0.13)
const errorCatalogos = ref('')
const guardando = ref(false)
let consecutivo = 0

const form = reactive({
  proveedorId: null,
  empleadoId: null,
  fecha: hoy(),
  numeroDocumento: '',
  descripcion: '',
  formaPago: 'Contado',
  numeroCheque: '',
  descuento: 0,
  gastosAdicionales: 0,
  lineas: []
})

function lineaNueva() {
  consecutivo += 1
  return {
    clave: consecutivo,
    modo: 'existente',
    loteId: null,
    productoId: null,
    numLote: '',
    fechCaducidad: '',
    almacenId: null,
    cantidad: 1,
    precio: 0
  }
}

function agregarLinea() {
  form.lineas.push(lineaNueva())
}

function etiquetaLote(lote) {
  const producto = productos.value.find(item => item.id === lote.productoId)
  return `${producto?.nombre || 'Producto'} · ${lote.numLote}`
}

const totales = computed(() =>
  totalesCompra(
    form.lineas,
    form.descuento,
    form.gastosAdicionales,
    tasaIva.value
  )
)
const descuentoInvalido = computed(
  () => Number(form.descuento) > totales.value.subtotal
)

function costoUnitario(linea, indice) {
  const cantidadLinea = Number(linea.cantidad) || 0
  if (cantidadLinea <= 0) return 0
  return redondear(
    Number(linea.precio || 0) +
      (totales.value.gastosLinea[indice] || 0) / cantidadLinea,
    4
  )
}

const filasResumen = computed(() => [
  { etiqueta: 'Subtotal', valor: moneda(totales.value.subtotal) },
  { etiqueta: 'Descuento', valor: moneda(totales.value.descuento) },
  {
    etiqueta: `IVA (${Math.round(tasaIva.value * 100)} %)`,
    valor: moneda(totales.value.iva)
  },
  { etiqueta: 'Gastos adicionales', valor: moneda(totales.value.gastos) },
  { etiqueta: 'Total', valor: moneda(totales.value.total), destacado: true }
])

async function cargarCatalogos() {
  errorCatalogos.value = ''
  try {
    const [prov, empl, prod, lots, alms, params] = await Promise.all([
      listarTodo('/proveedores'),
      listarTodo('/empleados/seleccion'),
      listarTodo('/productos'),
      listarTodo('/lotes'),
      listarTodo('/almacenes'),
      mapaParametros()
    ])
    proveedores.value = prov
    empleados.value = empl
    productos.value = prod
    lotes.value = lots
    almacenes.value = alms.filter(item => item.estado !== false)
    tasaIva.value = numeroParametro(params, 'iva.tasa', 0.13)
    if (!form.empleadoId && idEmpleado.value) form.empleadoId = idEmpleado.value
  } catch (causa) {
    errorCatalogos.value =
      causa.mensaje || 'No se pudieron cargar los catálogos.'
  }
}

function reiniciar() {
  form.proveedorId = null
  form.fecha = hoy()
  form.numeroDocumento = ''
  form.descripcion = ''
  form.formaPago = 'Contado'
  form.numeroCheque = ''
  form.descuento = 0
  form.gastosAdicionales = 0
  form.lineas = [lineaNueva()]
  form.empleadoId = idEmpleado.value || null
}

async function guardar() {
  guardando.value = true
  const cuerpo = {
    proveedorId: form.proveedorId,
    fecha: form.fecha,
    numeroDocumento: form.numeroDocumento.trim(),
    descripcion: form.descripcion?.trim() || null,
    formaPago: form.formaPago,
    descuento: redondear(form.descuento || 0),
    gastosAdicionales: redondear(form.gastosAdicionales || 0),
    lineas: form.lineas.map(linea => {
      const base = {
        almacenId: linea.almacenId,
        cantidad: Number(linea.cantidad),
        precio: redondear(linea.precio)
      }
      if (linea.modo === 'nuevo') {
        return {
          ...base,
          nuevoLote: {
            productoId: linea.productoId,
            numLote: linea.numLote.trim(),
            fechCaducidad: linea.fechCaducidad || null
          }
        }
      }
      return { ...base, loteId: linea.loteId }
    })
  }
  if (form.empleadoId) cuerpo.empleadoId = form.empleadoId
  if (form.formaPago === 'Cheque')
    cuerpo.numeroCheque = form.numeroCheque.trim()
  try {
    await api.post('/compras', cuerpo)
    avisarExito('Compra registrada.')
    reiniciar()
    lotes.value = await listarTodo('/lotes')
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

onMounted(async () => {
  agregarLinea()
  await cargarCatalogos()
})
</script>
