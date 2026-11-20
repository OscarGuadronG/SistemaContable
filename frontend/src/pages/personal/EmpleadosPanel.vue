<template>
  <div>
    <div class="row q-col-gutter-sm items-end q-mb-md">
      <div class="col-12 col-sm-4">
        <q-input
          v-model="filtros.buscar"
          outlined
          dense
          clearable
          label="Buscar"
          @keyup.enter="reiniciar"
        />
      </div>
      <div class="col-6 col-sm-3">
        <q-select
          v-model="filtros.estado"
          :options="estados"
          emit-value
          map-options
          clearable
          outlined
          dense
          label="Estado"
        />
      </div>
      <div class="col-6 col-sm-3">
        <selector-busqueda
          v-model="filtros.departamentoId"
          :opciones="departamentos"
          etiqueta="Departamento"
        />
      </div>
      <div class="col-auto"
        ><q-btn color="primary" outline label="Buscar" @click="reiniciar"
      /></div>
      <q-space />
      <div v-if="puedeEscribir" class="col-auto">
        <q-btn
          color="primary"
          unelevated
          icon="add"
          label="Nuevo empleado"
          @click="abrir()"
        />
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
      <template #body-cell-fechNac="props"
        ><q-td :props="props">{{ fecha(props.row.fechNac) }}</q-td></template
      >
      <template #body-cell-salario="props"
        ><q-td :props="props" class="text-right monto">{{
          moneda(props.row.salario)
        }}</q-td></template
      >
      <template #body-cell-estado="props">
        <q-td :props="props">
          <q-badge
            :color="props.row.estado === 'Activo' ? 'positive' : 'grey'"
            >{{ props.row.estado }}</q-badge
          >
        </q-td>
      </template>
      <template #body-cell-acciones="props">
        <q-td :props="props" class="q-gutter-xs">
          <q-btn flat dense label="Ver" @click="ver(props.row)" />
          <template v-if="puedeEscribir">
            <q-btn
              flat
              dense
              color="primary"
              label="Editar"
              @click="abrir(props.row)"
            />
            <q-btn
              flat
              dense
              :color="props.row.estado === 'Activo' ? 'negative' : 'positive'"
              :label="props.row.estado === 'Activo' ? 'Inactivar' : 'Activar'"
              @click="cambiarEstado(props.row)"
            />
          </template>
        </q-td>
      </template>
    </q-table>

    <q-dialog v-model="dialogo" persistent>
      <q-card style="width: min(640px, 96vw)">
        <q-form @submit.prevent="guardar">
          <q-card-section class="text-h6">{{
            editando ? 'Editar empleado' : 'Nuevo empleado'
          }}</q-card-section>
          <q-card-section class="row q-col-gutter-sm">
            <div class="col-12 col-sm-6">
              <q-input
                v-model="form.nombre"
                outlined
                dense
                label="Nombres"
                :rules="[requerido('Indique los nombres')]"
              />
            </div>
            <div class="col-12 col-sm-6">
              <q-input
                v-model="form.apellido"
                outlined
                dense
                label="Apellidos"
                :rules="[requerido('Indique los apellidos')]"
              />
            </div>
            <div class="col-12 col-sm-6">
              <q-input
                v-model="form.fechNac"
                type="date"
                outlined
                dense
                label="Fecha de nacimiento"
                :rules="[requerido('Indique la fecha')]"
              />
            </div>
            <div class="col-12 col-sm-6">
              <selector-busqueda
                v-model="form.departamentoId"
                :opciones="departamentos"
                etiqueta="Departamento"
                :rules="[requerido('Seleccione el departamento')]"
              />
            </div>
            <div class="col-12 col-sm-4">
              <q-input
                v-model.number="form.salario"
                type="number"
                min="0.01"
                step="0.01"
                outlined
                dense
                label="Salario"
                :rules="[numeroPositivo('Indique el salario')]"
              />
            </div>
            <div class="col-12 col-sm-4">
              <q-input
                v-model="form.numeroIsss"
                outlined
                dense
                label="ISSS"
                hint="Número de afiliación"
                :rules="[requerido('Indique el ISSS')]"
              />
            </div>
            <div class="col-12 col-sm-4">
              <q-input
                v-model="form.dui"
                outlined
                dense
                label="DUI"
                mask="########-#"
                fill-mask
                :rules="[duiValido]"
              />
            </div>
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat label="Cancelar" v-close-popup />
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

    <q-dialog v-model="fichaAbierta">
      <q-card style="width: min(480px, 94vw)">
        <q-card-section class="text-h6"
          >{{ ficha?.nombre }} {{ ficha?.apellido }}</q-card-section
        >
        <q-card-section v-if="ficha" class="q-gutter-xs">
          <div
            ><strong>Departamento:</strong>
            {{ nombreDepartamento(ficha.departamentoId) }}</div
          >
          <div><strong>Nacimiento:</strong> {{ fecha(ficha.fechNac) }}</div>
          <div><strong>Salario:</strong> {{ moneda(ficha.salario) }}</div>
          <div><strong>ISSS:</strong> {{ ficha.numeroIsss }}</div>
          <div><strong>DUI:</strong> {{ ficha.dui }}</div>
          <div><strong>Estado:</strong> {{ ficha.estado }}</div>
        </q-card-section>
        <q-card-actions align="right"
          ><q-btn flat label="Cerrar" v-close-popup
        /></q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import SelectorBusqueda from '@/components/SelectorBusqueda.vue'
import { useListado } from '@/composables/useListado'
import { api, listarTodo } from '@/services/api'
import { avisarExito, confirmar } from '@/utils/dialogo'
import {
  fecha,
  moneda,
  numeroPositivo,
  redondear,
  requerido
} from '@/utils/formato'

defineProps({
  puedeEscribir: { type: Boolean, default: false }
})

const departamentos = ref([])
const filtros = reactive({ buscar: '', estado: null, departamentoId: null })
const estados = [
  { value: 'Activo', label: 'Activo' },
  { value: 'Inactivo', label: 'Inactivo' },
  { value: 'Suspendido', label: 'Suspendido' }
]
const { filas, cargando, error, paginacion, cargar, reiniciar } = useListado(
  '/empleados',
  () => ({ ...filtros })
)
const columnas = [
  { name: 'nombre', label: 'Nombres', field: 'nombre', align: 'left' },
  { name: 'apellido', label: 'Apellidos', field: 'apellido', align: 'left' },
  {
    name: 'departamento',
    label: 'Departamento',
    field: fila => nombreDepartamento(fila.departamentoId),
    align: 'left'
  },
  { name: 'fechNac', label: 'Nacimiento', field: 'fechNac', align: 'left' },
  { name: 'salario', label: 'Salario', field: 'salario', align: 'right' },
  { name: 'estado', label: 'Estado', field: 'estado', align: 'left' },
  { name: 'acciones', label: '', field: 'acciones', align: 'right' }
]
const dialogo = ref(false)
const editando = ref(null)
const guardando = ref(false)
const ficha = ref(null)
const fichaAbierta = ref(false)
const form = reactive({
  nombre: '',
  apellido: '',
  fechNac: '',
  departamentoId: null,
  salario: 0,
  numeroIsss: '',
  dui: ''
})

function nombreDepartamento(id) {
  return departamentos.value.find(item => item.id === id)?.nombre || '—'
}

function duiValido(valor) {
  return /^\d{8}-\d$/.test(valor || '') || 'Use el formato 00000000-0'
}

function ver(fila) {
  ficha.value = fila
  fichaAbierta.value = true
}

function abrir(fila) {
  editando.value = fila || null
  form.nombre = fila?.nombre || ''
  form.apellido = fila?.apellido || ''
  form.fechNac = fila?.fechNac || ''
  form.departamentoId = fila?.departamentoId || null
  form.salario = fila?.salario || 0
  form.numeroIsss = fila?.numeroIsss || ''
  form.dui = fila?.dui || ''
  dialogo.value = true
}

async function guardar() {
  guardando.value = true
  const cuerpo = {
    nombre: form.nombre.trim(),
    apellido: form.apellido.trim(),
    fechNac: form.fechNac,
    departamentoId: form.departamentoId,
    salario: redondear(form.salario),
    numeroIsss: form.numeroIsss.trim(),
    dui: form.dui
  }
  try {
    if (editando.value) await api.put(`/empleados/${editando.value.id}`, cuerpo)
    else await api.post('/empleados', cuerpo)
    avisarExito('Empleado guardado.')
    dialogo.value = false
    await reiniciar()
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

async function cambiarEstado(fila) {
  const activar = fila.estado !== 'Activo'
  const acepta = await confirmar({
    titulo: activar ? 'Activar empleado' : 'Inactivar empleado',
    mensaje: `¿Desea ${activar ? 'activar' : 'inactivar'} a ${fila.nombre} ${fila.apellido}?`,
    ok: activar ? 'Activar' : 'Inactivar',
    color: activar ? 'positive' : 'negative'
  })
  if (!acepta) return
  try {
    await api.post(`/empleados/${fila.id}/${activar ? 'activar' : 'inactivar'}`)
    avisarExito('Estado actualizado.')
    await reiniciar()
  } catch {
    // El cliente HTTP ya mostró el error.
  }
}

onMounted(async () => {
  try {
    departamentos.value = await listarTodo('/departamentos')
  } catch {
    departamentos.value = []
  }
  await cargar()
})
</script>
