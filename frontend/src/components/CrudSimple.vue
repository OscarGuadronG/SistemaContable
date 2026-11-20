<template>
  <div>
    <div class="row q-col-gutter-sm items-end q-mb-md">
      <div class="col-12 col-sm-6 col-md-4">
        <q-input
          v-model="busqueda"
          outlined
          dense
          clearable
          label="Buscar"
          debounce="200"
        >
          <template #prepend><q-icon name="search" /></template>
        </q-input>
      </div>
      <q-space />
      <div class="col-auto" v-if="puedeEscribir">
        <q-btn
          color="primary"
          unelevated
          icon="add"
          :label="etiquetaNuevo"
          @click="abrirNuevo"
        />
      </div>
    </div>

    <q-banner v-if="error" class="bg-red-1 text-negative q-mb-md" rounded>
      {{ error }}
      <template #action>
        <q-btn flat color="negative" label="Reintentar" @click="cargar" />
      </template>
    </q-banner>

    <q-table
      flat
      bordered
      class="tabla-app"
      :rows="filtradas"
      :columns="columnasVisibles"
      row-key="id"
      :loading="cargando"
      :rows-per-page-options="[10, 20, 50]"
      no-data-label="No hay registros"
    >
      <template #body-cell-acciones="props">
        <q-td :props="props" class="q-gutter-xs">
          <slot name="extra" :fila="props.row" />
          <q-btn
            v-if="puedeEscribir"
            flat
            dense
            round
            icon="edit"
            color="primary"
            @click="abrirEdicion(props.row)"
          >
            <q-tooltip>Editar</q-tooltip>
          </q-btn>
          <q-btn
            v-if="puedeEscribir && puedeEliminar"
            flat
            dense
            round
            icon="delete"
            color="negative"
            @click="eliminar(props.row)"
          >
            <q-tooltip>Eliminar</q-tooltip>
          </q-btn>
        </q-td>
      </template>
    </q-table>

    <q-dialog v-model="dialogo" persistent>
      <q-card style="width: min(520px, 94vw)">
        <q-card-section class="text-h6">{{ tituloDialogo }}</q-card-section>
        <q-form @submit.prevent="guardar">
          <q-card-section class="q-gutter-md">
            <q-input
              v-for="campo in campos"
              :key="campo.key"
              v-model="form[campo.key]"
              outlined
              dense
              :label="campo.label"
              :type="campo.tipo || 'text'"
              :mask="campo.mask"
              :hint="campo.hint"
              :autogrow="campo.tipo === 'textarea'"
              :rules="campo.reglas || []"
            />
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat label="Cancelar" v-close-popup :disable="guardando" />
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api, listarTodo } from '@/services/api'
import { avisarExito, confirmar } from '@/utils/dialogo'

const props = defineProps({
  ruta: { type: String, required: true },
  titulo: { type: String, required: true },
  campos: { type: Array, required: true },
  columnas: { type: Array, required: true },
  puedeEscribir: { type: Boolean, default: false },
  puedeEliminar: { type: Boolean, default: true },
  etiquetaNuevo: { type: String, default: 'Nuevo' }
})

const emit = defineEmits(['cambio'])

const filas = ref([])
const cargando = ref(false)
const error = ref('')
const busqueda = ref('')
const dialogo = ref(false)
const guardando = ref(false)
const editandoId = ref(null)
const form = reactive({})

const columnasVisibles = computed(() => [
  ...props.columnas,
  { name: 'acciones', label: '', field: 'acciones', align: 'right' }
])

const filtradas = computed(() => {
  const consulta = String(busqueda.value || '')
    .trim()
    .toLowerCase()
  if (!consulta) return filas.value
  return filas.value.filter(fila =>
    props.columnas.some(columna =>
      String(fila[columna.field] ?? '')
        .toLowerCase()
        .includes(consulta)
    )
  )
})

const tituloDialogo = computed(() =>
  editandoId.value ? `Editar ${props.titulo}` : `Nuevo ${props.titulo}`
)

function vaciar() {
  props.campos.forEach(campo => {
    form[campo.key] = ''
  })
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    filas.value = await listarTodo(props.ruta)
  } catch (causa) {
    filas.value = []
    error.value = causa.mensaje || 'No se pudo cargar el catálogo.'
  } finally {
    cargando.value = false
  }
}

function abrirNuevo() {
  editandoId.value = null
  vaciar()
  dialogo.value = true
}

function abrirEdicion(fila) {
  editandoId.value = fila.id
  props.campos.forEach(campo => {
    form[campo.key] = fila[campo.key] ?? ''
  })
  dialogo.value = true
}

async function guardar() {
  guardando.value = true
  const cuerpo = {}
  props.campos.forEach(campo => {
    const valor = form[campo.key]
    cuerpo[campo.key] = typeof valor === 'string' ? valor.trim() : valor
  })
  try {
    if (editandoId.value) {
      await api.put(`${props.ruta}/${editandoId.value}`, cuerpo)
      avisarExito('Registro actualizado.')
    } else {
      await api.post(props.ruta, cuerpo)
      avisarExito('Registro creado.')
    }
    dialogo.value = false
    emit('cambio')
    await cargar()
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

async function eliminar(fila) {
  const acepta = await confirmar({
    titulo: 'Eliminar registro',
    mensaje: `¿Eliminar «${fila.nombre || fila.id}»? Esta acción no se puede deshacer.`,
    ok: 'Eliminar',
    color: 'negative'
  })
  if (!acepta) return
  try {
    await api.eliminar(`${props.ruta}/${fila.id}`)
    avisarExito('Registro eliminado.')
    emit('cambio')
    await cargar()
  } catch {
    // El cliente HTTP ya mostró el error.
  }
}

onMounted(cargar)

defineExpose({ cargar })
</script>
