<template>
  <q-page class="pagina q-pa-md">
    <encabezado-pagina
      titulo="Usuarios"
      subtitulo="Cuentas de acceso, rol y empleado vinculado"
      icono="manage_accounts"
    >
      <template #acciones>
        <q-btn
          v-if="puedeEscribir"
          color="primary"
          unelevated
          icon="person_add"
          label="Nuevo usuario"
          @click="abrirNuevo"
        />
      </template>
    </encabezado-pagina>

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
      :rows="filas"
      :columns="columnas"
      row-key="id"
      :loading="cargando"
      :rows-per-page-options="[10, 20, 50]"
    >
      <template #body-cell-rol="props">
        <q-td :props="props">
          <q-badge
            :color="COLOR_ROL[props.row.rol] || 'grey'"
            :label="props.row.rol"
          />
        </q-td>
      </template>
      <template #body-cell-activo="props">
        <q-td :props="props">
          <q-badge
            outline
            :color="props.row.activo ? 'positive' : 'negative'"
            :label="props.row.activo ? 'Activo' : 'Inactivo'"
          />
        </q-td>
      </template>
      <template #body-cell-creado="props">
        <q-td :props="props">{{ fechaHora(props.row.creado) }}</q-td>
      </template>
      <template #body-cell-acciones="props">
        <q-td :props="props">
          <q-btn
            v-if="puedeEscribir"
            flat
            dense
            color="primary"
            icon="edit"
            label="Editar"
            @click="abrirEdicion(props.row)"
          />
        </q-td>
      </template>
      <template #no-data>
        <div class="full-width text-center q-pa-lg text-grey-7"
          >No hay usuarios.</div
        >
      </template>
    </q-table>

    <q-dialog v-model="dialogo" persistent>
      <q-card style="width: min(520px, 94vw)">
        <q-card-section class="text-h6">
          {{ editando ? 'Editar usuario' : 'Nuevo usuario' }}
        </q-card-section>
        <q-form @submit.prevent="guardar">
          <q-card-section class="q-gutter-md">
            <q-input
              v-if="!editando"
              v-model="form.email"
              type="email"
              outlined
              dense
              label="Correo"
              autocomplete="off"
              :rules="[requerido('Indique el correo'), correoValido]"
            />
            <q-input
              v-else
              :model-value="form.email"
              outlined
              dense
              label="Correo"
              readonly
            />
            <q-input
              v-if="!editando"
              v-model="form.password"
              :type="verClave ? 'text' : 'password'"
              outlined
              dense
              label="Contraseña"
              autocomplete="new-password"
              hint="Mínimo 8 caracteres"
              :rules="[claveValida]"
            >
              <template #append>
                <q-icon
                  :name="verClave ? 'visibility_off' : 'visibility'"
                  class="cursor-pointer"
                  @click="verClave = !verClave"
                />
              </template>
            </q-input>
            <q-input
              v-model="form.nombre"
              outlined
              dense
              label="Nombre"
              :rules="[requerido('Indique el nombre')]"
            />
            <q-select
              v-model="form.rol"
              :options="opcionesRol"
              emit-value
              map-options
              outlined
              dense
              label="Rol"
              :rules="[requerido('Seleccione el rol')]"
            />
            <selector-busqueda
              v-model="form.idEmpleado"
              :opciones="empleados"
              etiqueta="Empleado (opcional)"
              :opcion-etiqueta="item => item.nombreCompleto"
              hint="Vincula las compras, ventas y planillas de esta persona."
            />
            <q-toggle
              v-if="editando"
              v-model="form.activo"
              label="Usuario activo"
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
  </q-page>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import EncabezadoPagina from '@/components/EncabezadoPagina.vue'
import SelectorBusqueda from '@/components/SelectorBusqueda.vue'
import { usePermiso } from '@/composables/usePermiso'
import { COLOR_ROL, ROLES } from '@/config/permisos'
import { api, comoLista, listarTodo } from '@/services/api'
import { avisarExito, confirmar } from '@/utils/dialogo'
import { fechaHora, requerido } from '@/utils/formato'

const { permitido } = usePermiso()
const puedeEscribir = computed(() => permitido('usuarios', 'escritura'))

const filas = ref([])
const empleados = ref([])
const cargando = ref(false)
const error = ref('')
const dialogo = ref(false)
const guardando = ref(false)
const editando = ref(null)
const verClave = ref(false)
const form = reactive({
  email: '',
  password: '',
  nombre: '',
  rol: 'Ventas',
  idEmpleado: null,
  activo: true
})

const opcionesRol = ROLES.map(rol => ({ value: rol, label: rol }))

const nombresEmpleado = computed(() =>
  Object.fromEntries(
    empleados.value.map(empleado => [empleado.id, empleado.nombreCompleto])
  )
)

const columnas = [
  { name: 'email', label: 'Correo', field: 'email', align: 'left' },
  { name: 'nombre', label: 'Nombre', field: 'nombre', align: 'left' },
  { name: 'rol', label: 'Rol', field: 'rol', align: 'left' },
  {
    name: 'empleado',
    label: 'Empleado',
    field: fila => nombresEmpleado.value[fila.idEmpleado] || '—',
    align: 'left'
  },
  { name: 'activo', label: 'Estado', field: 'activo', align: 'left' },
  { name: 'creado', label: 'Creado', field: 'creado', align: 'left' },
  { name: 'acciones', label: '', field: 'acciones', align: 'right' }
]

function correoValido(valor) {
  return (
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(valor || '').trim()) ||
    'Indique un correo válido.'
  )
}

function claveValida(valor) {
  return (
    String(valor || '').length >= 8 ||
    'La contraseña debe tener al menos 8 caracteres.'
  )
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const { data } = await api.get('/usuarios')
    filas.value = comoLista(data)
  } catch (causa) {
    filas.value = []
    error.value = causa.mensaje || 'No se pudieron cargar los usuarios.'
  } finally {
    cargando.value = false
  }
}

async function cargarEmpleados() {
  try {
    empleados.value = await listarTodo('/empleados/seleccion')
  } catch {
    empleados.value = []
  }
}

function abrirNuevo() {
  editando.value = null
  verClave.value = false
  form.email = ''
  form.password = ''
  form.nombre = ''
  form.rol = 'Ventas'
  form.idEmpleado = null
  form.activo = true
  dialogo.value = true
}

function abrirEdicion(fila) {
  editando.value = fila
  form.email = fila.email || ''
  form.password = ''
  form.nombre = fila.nombre || ''
  form.rol = fila.rol
  form.idEmpleado = fila.idEmpleado ?? null
  form.activo = Boolean(fila.activo)
  dialogo.value = true
}

async function guardar() {
  if (editando.value && editando.value.activo && !form.activo) {
    const acepta = await confirmar({
      titulo: 'Inactivar usuario',
      mensaje: `${form.nombre} dejará de entrar al sistema.`,
      ok: 'Inactivar',
      color: 'negative'
    })
    if (!acepta) return
  }
  guardando.value = true
  try {
    if (editando.value) {
      await api.put(`/usuarios/${editando.value.id}`, {
        rol: form.rol,
        nombre: form.nombre.trim(),
        idEmpleado: form.idEmpleado || null,
        activo: Boolean(form.activo)
      })
      avisarExito('Usuario actualizado.')
    } else {
      const cuerpo = {
        email: form.email.trim(),
        password: form.password,
        rol: form.rol,
        nombre: form.nombre.trim()
      }
      if (form.idEmpleado) cuerpo.idEmpleado = form.idEmpleado
      await api.post('/usuarios', cuerpo)
      avisarExito('Usuario creado.')
    }
    dialogo.value = false
    await cargar()
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

onMounted(() => {
  cargar()
  cargarEmpleados()
})
</script>
