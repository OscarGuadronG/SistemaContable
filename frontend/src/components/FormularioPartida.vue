<template>
  <q-dialog
    :model-value="modelValue"
    persistent
    @update:model-value="emit('update:modelValue', $event)"
  >
    <q-card style="width: min(980px, 96vw)">
      <q-card-section class="row items-center">
        <div class="text-h6">{{ titulo }}</div>
        <q-space />
        <q-btn flat round icon="close" v-close-popup :disable="guardando" />
      </q-card-section>
      <q-separator />
      <q-form @submit.prevent="guardar">
        <q-card-section class="q-gutter-md">
          <div class="row q-col-gutter-md">
            <div class="col-12 col-sm-4">
              <q-input
                v-model="form.fecha"
                type="date"
                outlined
                dense
                label="Fecha"
                :rules="[requerido('Indique la fecha')]"
              />
            </div>
            <div class="col-12 col-sm-4">
              <q-select
                v-model="form.tipo"
                :options="tipos"
                emit-value
                map-options
                outlined
                dense
                label="Tipo"
                :disable="Boolean(tipoFijo)"
                :rules="[requerido('Seleccione el tipo')]"
              />
            </div>
            <div class="col-12 col-sm-4">
              <selector-busqueda
                v-model="form.terceroId"
                :opciones="terceros"
                etiqueta="Tercero (opcional)"
                :opcion-etiqueta="item => item.nombre"
              />
            </div>
            <div class="col-12">
              <q-input
                v-model="form.concepto"
                outlined
                dense
                autogrow
                label="Concepto"
                :rules="[requerido('Escriba el concepto')]"
              />
            </div>
          </div>

          <div class="row items-center q-mb-sm">
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

          <q-banner
            v-if="errorCuentas"
            class="bg-red-1 text-negative"
            rounded
            dense
          >
            {{ errorCuentas }}
          </q-banner>

          <div
            v-for="(linea, indice) in form.lineas"
            :key="linea.clave"
            class="row q-col-gutter-sm items-start q-mb-sm"
          >
            <div class="col-12 col-md-6">
              <selector-busqueda
                v-model="linea.cuentaId"
                :opciones="cuentas"
                etiqueta="Cuenta imputable"
                opcion-valor="id"
                :opcion-etiqueta="etiquetaCuenta"
                :rules="[requerido('Seleccione la cuenta')]"
              />
            </div>
            <div class="col-6 col-md-2">
              <q-input
                v-model.number="linea.cargo"
                type="number"
                min="0"
                step="0.01"
                outlined
                dense
                label="Cargo"
                @update:model-value="alEscribir(linea, 'cargo')"
              />
            </div>
            <div class="col-5 col-md-2">
              <q-input
                v-model.number="linea.abono"
                type="number"
                min="0"
                step="0.01"
                outlined
                dense
                label="Abono"
                @update:model-value="alEscribir(linea, 'abono')"
              />
            </div>
            <div class="col-1">
              <q-btn
                flat
                round
                color="negative"
                icon="close"
                :disable="form.lineas.length <= 2"
                @click="form.lineas.splice(indice, 1)"
              />
            </div>
          </div>

          <div class="row justify-end q-gutter-md text-body1">
            <div
              >Cargos: <strong>{{ moneda(totalCargo) }}</strong></div
            >
            <div
              >Abonos: <strong>{{ moneda(totalAbono) }}</strong></div
            >
          </div>
          <q-banner v-if="!cuadra" class="bg-orange-1 text-orange-10" rounded>
            La partida no cuadra. El total de cargos debe ser igual al de abonos
            y mayor que cero. No se puede guardar.
          </q-banner>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" v-close-popup :disable="guardando" />
          <q-btn
            color="primary"
            unelevated
            type="submit"
            label="Guardar partida"
            :disable="!cuadra"
            :loading="guardando"
          />
        </q-card-actions>
      </q-form>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import SelectorBusqueda from '@/components/SelectorBusqueda.vue'
import { Notify } from 'quasar'
import { api, listarTodo } from '@/services/api'
import { avisarExito } from '@/utils/dialogo'
import { hoy, moneda, redondear, requerido } from '@/utils/formato'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  titulo: { type: String, default: 'Partida manual' },
  tipoFijo: { type: String, default: '' },
  fechaInicial: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'guardada'])

const tipos = [
  { value: 'Diario', label: 'Diario' },
  { value: 'Ingreso', label: 'Ingreso' },
  { value: 'Egreso', label: 'Egreso' },
  { value: 'Ajuste', label: 'Ajuste' }
]

const cuentas = ref([])
const terceros = ref([])
const errorCuentas = ref('')
const guardando = ref(false)
let consecutivo = 0

const form = reactive(vacio())

function vacio() {
  return {
    fecha: hoy(),
    concepto: '',
    tipo: props.tipoFijo || 'Diario',
    terceroId: null,
    lineas: [lineaNueva(), lineaNueva()]
  }
}

function lineaNueva() {
  consecutivo += 1
  return { clave: consecutivo, cuentaId: null, cargo: 0, abono: 0 }
}

function agregarLinea() {
  form.lineas.push(lineaNueva())
}

function alEscribir(linea, lado) {
  const valor = Number(linea[lado]) || 0
  if (valor > 0) {
    if (lado === 'cargo') linea.abono = 0
    else linea.cargo = 0
  }
}

function etiquetaCuenta(cuenta) {
  return `${cuenta.codigo} — ${cuenta.nombre}`
}

function aplanar(nodos, salida = []) {
  ;(nodos || []).forEach(nodo => {
    salida.push(nodo)
    aplanar(nodo.hijos || nodo.children, salida)
  })
  return salida
}

async function cargarCatalogos() {
  errorCuentas.value = ''
  try {
    let lista = []
    try {
      lista = await listarTodo('/cuentas')
    } catch {
      lista = []
    }
    if (lista.length === 0) {
      const { data } = await api.get('/cuentas/arbol')
      const raiz = Array.isArray(data) ? data : data ? [data] : []
      lista = aplanar(raiz)
    }
    cuentas.value = lista.filter(
      cuenta => cuenta.imputable && cuenta.activo !== false
    )
  } catch (causa) {
    cuentas.value = []
    errorCuentas.value = causa.mensaje || 'No se pudieron cargar las cuentas.'
  }
  try {
    terceros.value = await listarTodo('/terceros')
  } catch {
    terceros.value = []
  }
}

const totalCargo = computed(() =>
  redondear(
    form.lineas.reduce((suma, linea) => suma + (Number(linea.cargo) || 0), 0)
  )
)
const totalAbono = computed(() =>
  redondear(
    form.lineas.reduce((suma, linea) => suma + (Number(linea.abono) || 0), 0)
  )
)
const cuadra = computed(
  () => totalCargo.value > 0 && totalCargo.value === totalAbono.value
)

watch(
  () => props.modelValue,
  async abierto => {
    if (!abierto) return
    Object.assign(form, vacio())
    form.lineas = [lineaNueva(), lineaNueva()]
    form.tipo = props.tipoFijo || 'Diario'
    form.fecha = props.fechaInicial || hoy()
    await cargarCatalogos()
  }
)

async function guardar() {
  if (!cuadra.value) return
  const invalida = form.lineas.some(linea => {
    const cargo = Number(linea.cargo) || 0
    const abono = Number(linea.abono) || 0
    return (
      !linea.cuentaId || (cargo <= 0 && abono <= 0) || (cargo > 0 && abono > 0)
    )
  })
  if (invalida) {
    Notify.create({
      type: 'warning',
      message:
        'Cada línea debe tener una cuenta y un solo importe, cargo o abono.'
    })
    return
  }
  guardando.value = true
  const cuerpo = {
    fecha: form.fecha,
    concepto: form.concepto.trim(),
    tipo: form.tipo,
    lineas: form.lineas.map(linea => ({
      cuentaId: linea.cuentaId,
      cargo: redondear(linea.cargo || 0),
      abono: redondear(linea.abono || 0)
    }))
  }
  if (form.terceroId) cuerpo.terceroId = form.terceroId
  try {
    const { data } = await api.post('/partidas', cuerpo)
    avisarExito('Partida registrada.')
    emit('guardada', data)
    emit('update:modelValue', false)
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}
</script>
