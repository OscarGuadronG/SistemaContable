<template>
  <q-page class="pagina q-pa-md">
    <encabezado-pagina
      titulo="Catálogo de cuentas"
      subtitulo="Árbol contable, códigos y cuentas imputables"
      icono="account_tree"
    >
      <template #acciones>
        <q-btn
          v-if="puedeEscribir"
          color="primary"
          unelevated
          icon="add"
          label="Nueva cuenta"
          @click="abrirCrear(null)"
        />
      </template>
    </encabezado-pagina>

    <div class="row q-col-gutter-md">
      <div class="col-12 col-md-8">
        <q-input
          v-model="busqueda"
          outlined
          dense
          clearable
          label="Buscar por código o nombre"
          class="q-mb-md"
          debounce="200"
        >
          <template #prepend><q-icon name="search" /></template>
        </q-input>

        <q-banner v-if="error" class="bg-red-1 text-negative q-mb-md" rounded>
          {{ error }}
          <template #action>
            <q-btn flat color="negative" label="Reintentar" @click="cargar" />
          </template>
        </q-banner>

        <q-card
          flat
          bordered
          class="arbol-cuenta relative-position"
          style="min-height: 240px"
        >
          <q-inner-loading :showing="cargando" color="primary" />
          <q-card-section
            v-if="!cargando && nodosVisibles.length === 0"
            class="text-grey-7 text-center"
          >
            {{
              busqueda
                ? 'Ninguna cuenta coincide con la búsqueda.'
                : 'El catálogo está vacío.'
            }}
          </q-card-section>
          <q-card-section v-else>
            <q-tree
              :nodes="nodosVisibles"
              node-key="id"
              children-key="hijos"
              v-model:selected="seleccionadaId"
              v-model:expanded="expandidos"
              selected-color="primary"
              no-connectors
            >
              <template #default-header="prop">
                <div class="row items-center q-gutter-xs">
                  <span class="text-weight-medium">{{ prop.node.codigo }}</span>
                  <span>{{ prop.node.nombre }}</span>
                  <q-badge
                    :color="
                      prop.node.naturaleza === 'Acreedora' ? 'teal' : 'primary'
                    "
                    :label="prop.node.naturaleza || '—'"
                  />
                  <q-badge
                    outline
                    :color="
                      prop.node.activo === false ? 'negative' : 'positive'
                    "
                    :label="prop.node.activo === false ? 'Inactiva' : 'Activa'"
                  />
                  <q-badge
                    v-if="prop.node.imputable"
                    outline
                    color="grey-8"
                    label="Imputable"
                  />
                </div>
              </template>
            </q-tree>
          </q-card-section>
        </q-card>
      </div>

      <div class="col-12 col-md-4">
        <q-card flat bordered>
          <q-card-section>
            <div class="text-subtitle1 text-weight-medium q-mb-sm"
              >Cuenta seleccionada</div
            >
            <div v-if="!seleccionada" class="text-grey-7">
              Seleccione una cuenta del árbol para verla o modificarla.
            </div>
            <template v-else>
              <div class="text-h6">{{ seleccionada.codigo }}</div>
              <div class="q-mb-sm">{{ seleccionada.nombre }}</div>
              <div class="text-body2 text-grey-8">
                Nivel {{ seleccionada.nivel || nivelDe(seleccionada.codigo) }} ·
                {{ seleccionada.naturaleza || 'Sin naturaleza' }}
              </div>
              <div class="text-body2 text-grey-8 q-mb-md">
                {{ seleccionada.imputable ? 'Imputable' : 'De agrupación' }} ·
                {{ seleccionada.activo === false ? 'Inactiva' : 'Activa' }}
              </div>
              <div v-if="puedeEscribir" class="column q-gutter-sm">
                <q-btn
                  outline
                  color="primary"
                  icon="account_tree"
                  label="Crear subcuenta"
                  :disable="!puedeSubcuenta"
                  @click="abrirCrear(seleccionada)"
                />
                <div v-if="!puedeSubcuenta" class="text-caption text-grey-7">
                  El nivel 4 (5 dígitos) no admite subcuentas.
                </div>
                <q-btn
                  outline
                  color="primary"
                  icon="edit"
                  label="Editar"
                  @click="abrirEdicion"
                />
                <q-btn
                  outline
                  :color="
                    seleccionada.activo === false ? 'positive' : 'warning'
                  "
                  :icon="
                    seleccionada.activo === false ? 'check_circle' : 'block'
                  "
                  :label="
                    seleccionada.activo === false ? 'Habilitar' : 'Inhabilitar'
                  "
                  @click="cambiarEstado"
                />
                <q-btn
                  outline
                  color="negative"
                  icon="delete"
                  label="Eliminar"
                  :disable="tieneHijos"
                  @click="eliminar"
                />
                <div v-if="tieneHijos" class="text-caption text-grey-7">
                  Una cuenta con subcuentas no se puede eliminar.
                </div>
              </div>
            </template>
          </q-card-section>
        </q-card>
      </div>
    </div>

    <q-dialog v-model="dialogo" persistent>
      <q-card style="width: min(520px, 94vw)">
        <q-card-section class="text-h6">{{ tituloDialogo }}</q-card-section>
        <q-form @submit.prevent="guardar">
          <q-card-section class="q-gutter-md">
            <q-input
              v-model="form.codigo"
              outlined
              dense
              :label="`Código (${largoEsperado} dígitos)`"
              :hint="pistaCodigo"
              :rules="[codigoValido]"
            />
            <q-input
              v-model="form.nombre"
              outlined
              dense
              label="Nombre"
              :rules="[requerido('Indique el nombre')]"
            />
            <q-select
              v-model="form.naturaleza"
              :options="['Deudora', 'Acreedora']"
              outlined
              dense
              label="Naturaleza"
              :rules="[requerido('Seleccione la naturaleza')]"
            />
            <q-toggle v-model="form.imputable" label="Cuenta imputable" />
            <div class="text-caption text-grey-7">
              Solo las cuentas imputables activas reciben movimientos.
            </div>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import EncabezadoPagina from '@/components/EncabezadoPagina.vue'
import { usePermiso } from '@/composables/usePermiso'
import { api } from '@/services/api'
import { avisarExito, confirmar } from '@/utils/dialogo'
import { requerido } from '@/utils/formato'

const LONGITUD_SIGUIENTE = { 0: 1, 1: 2, 2: 4, 4: 5 }
const NIVEL_POR_LARGO = { 1: 1, 2: 2, 4: 3, 5: 4 }

const { permitido } = usePermiso()
const puedeEscribir = computed(() => permitido('cuentas', 'escritura'))

const arbol = ref([])
const cargando = ref(false)
const error = ref('')
const busqueda = ref('')
const seleccionadaId = ref(null)
const expandidos = ref([])
const dialogo = ref(false)
const guardando = ref(false)
const modo = ref('crear')
const padre = ref(null)
const editando = ref(null)
const form = reactive({
  codigo: '',
  nombre: '',
  naturaleza: 'Deudora',
  imputable: false
})

const nodosVisibles = computed(() =>
  filtrar(
    arbol.value,
    String(busqueda.value || '')
      .trim()
      .toLowerCase()
  )
)

const seleccionada = computed(
  () => buscar(seleccionadaId.value, arbol.value)?.nodo || null
)

const tieneHijos = computed(() => (seleccionada.value?.hijos || []).length > 0)

const puedeSubcuenta = computed(
  () => longitudSiguiente(seleccionada.value?.codigo) != null
)

const largoEsperado = computed(() => {
  if (modo.value === 'editar')
    return String(editando.value?.codigo ?? '').length || 1
  return longitudSiguiente(padre.value?.codigo) || 1
})

const pistaCodigo = computed(() => {
  if (!padre.value) return 'Cuenta de primer nivel: 1 dígito.'
  return `Debe empezar por ${padre.value.codigo}.`
})

const tituloDialogo = computed(() => {
  if (modo.value === 'editar')
    return `Editar ${editando.value?.codigo || 'cuenta'}`
  if (padre.value) return `Subcuenta de ${padre.value.codigo}`
  return 'Nueva cuenta'
})

function nivelDe(codigo) {
  return NIVEL_POR_LARGO[String(codigo ?? '').length] || '—'
}

function longitudSiguiente(codigo) {
  const largo = codigo == null || codigo === '' ? 0 : String(codigo).length
  return LONGITUD_SIGUIENTE[largo] ?? null
}

function normalizar(nodos) {
  return (nodos || []).map(nodo => ({
    ...nodo,
    hijos: normalizar(nodo.hijos || [])
  }))
}

function filtrar(nodos, consulta) {
  if (!consulta) return nodos
  const salida = []
  nodos.forEach(nodo => {
    const hijos = filtrar(nodo.hijos || [], consulta)
    const texto = `${nodo.codigo} ${nodo.nombre}`.toLowerCase()
    if (texto.includes(consulta) || hijos.length)
      salida.push({ ...nodo, hijos })
  })
  return salida
}

function buscar(id, nodos, padreNodo = null) {
  if (id == null) return null
  for (const nodo of nodos) {
    if (nodo.id === id) return { nodo, padre: padreNodo }
    const hallado = buscar(id, nodo.hijos || [], nodo)
    if (hallado) return hallado
  }
  return null
}

function idsRama(nodos, acumulado = []) {
  nodos.forEach(nodo => {
    if ((nodo.hijos || []).length) {
      acumulado.push(nodo.id)
      idsRama(nodo.hijos, acumulado)
    }
  })
  return acumulado
}

watch(nodosVisibles, nodos => {
  expandidos.value = idsRama(nodos)
})

function codigoValido(valor) {
  const texto = String(valor ?? '').trim()
  if (!/^\d+$/.test(texto)) return 'Use solo dígitos, sin espacios.'
  if (texto.length !== largoEsperado.value) {
    return `El código debe tener ${largoEsperado.value} dígitos.`
  }
  if (padre.value && !texto.startsWith(String(padre.value.codigo))) {
    return `El código debe empezar por ${padre.value.codigo}.`
  }
  return true
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const { data } = await api.get('/cuentas/arbol')
    const raiz = Array.isArray(data) ? data : data ? [data] : []
    arbol.value = normalizar(raiz)
  } catch (causa) {
    arbol.value = []
    error.value = causa.mensaje || 'No se pudo cargar el catálogo.'
  } finally {
    cargando.value = false
  }
}

function abrirCrear(cuentaPadre) {
  modo.value = 'crear'
  editando.value = null
  padre.value = cuentaPadre
  const largo = longitudSiguiente(cuentaPadre?.codigo) || 1
  form.codigo = cuentaPadre ? String(cuentaPadre.codigo) : ''
  form.nombre = ''
  form.naturaleza = cuentaPadre?.naturaleza || 'Deudora'
  form.imputable = largo >= 5
  dialogo.value = true
}

function abrirEdicion() {
  const hallado = buscar(seleccionadaId.value, arbol.value)
  if (!hallado) return
  modo.value = 'editar'
  editando.value = hallado.nodo
  padre.value = hallado.padre
  form.codigo = String(hallado.nodo.codigo ?? '')
  form.nombre = hallado.nodo.nombre || ''
  form.naturaleza = hallado.nodo.naturaleza || 'Deudora'
  form.imputable = Boolean(hallado.nodo.imputable)
  dialogo.value = true
}

async function guardar() {
  guardando.value = true
  const cuerpo = {
    codigo: Number(String(form.codigo).trim()),
    nombre: form.nombre.trim(),
    naturaleza: form.naturaleza,
    imputable: Boolean(form.imputable),
    padreId: padre.value?.id ?? null
  }
  try {
    if (modo.value === 'editar' && editando.value) {
      await api.put(`/cuentas/${editando.value.id}`, cuerpo)
      avisarExito('Cuenta actualizada.')
      seleccionadaId.value = editando.value.id
    } else {
      const { data } = await api.post('/cuentas', cuerpo)
      avisarExito('Cuenta creada.')
      if (data?.id) seleccionadaId.value = data.id
    }
    dialogo.value = false
    await cargar()
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

async function cambiarEstado() {
  const cuenta = seleccionada.value
  if (!cuenta) return
  const activar = cuenta.activo === false
  const acepta = await confirmar({
    titulo: activar ? 'Habilitar cuenta' : 'Inhabilitar cuenta',
    mensaje: activar
      ? `¿Habilitar la cuenta ${cuenta.codigo} ${cuenta.nombre}?`
      : `¿Inhabilitar la cuenta ${cuenta.codigo} ${cuenta.nombre}? Dejará de recibir movimientos.`,
    ok: activar ? 'Habilitar' : 'Inhabilitar',
    color: activar ? 'positive' : 'warning'
  })
  if (!acepta) return
  try {
    await api.post(
      `/cuentas/${cuenta.id}/${activar ? 'activar' : 'desactivar'}`
    )
    avisarExito(activar ? 'Cuenta habilitada.' : 'Cuenta inhabilitada.')
    await cargar()
  } catch {
    // El cliente HTTP ya mostró el error.
  }
}

async function eliminar() {
  const cuenta = seleccionada.value
  if (!cuenta || tieneHijos.value) return
  const acepta = await confirmar({
    titulo: 'Eliminar cuenta',
    mensaje: `¿Eliminar ${cuenta.codigo} ${cuenta.nombre}? Solo es posible si no tiene subcuentas ni movimientos.`,
    ok: 'Eliminar',
    color: 'negative'
  })
  if (!acepta) return
  try {
    await api.eliminar(`/cuentas/${cuenta.id}`)
    avisarExito('Cuenta eliminada.')
    seleccionadaId.value = null
    await cargar()
  } catch {
    // El backend rechaza la cuenta si ya tiene movimientos.
  }
}

onMounted(cargar)
</script>
