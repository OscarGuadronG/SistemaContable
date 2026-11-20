<template>
  <q-page class="pagina q-pa-md">
    <encabezado-pagina
      titulo="Parámetros"
      subtitulo="Tasas, plazos, margen e IVA usados por el sistema"
      icono="tune"
    />

    <q-banner
      v-if="!puedeEscribir"
      class="bg-blue-1 text-primary q-mb-md"
      rounded
    >
      Su rol puede consultar estos valores. La modificación corresponde a
      Contabilidad.
    </q-banner>

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
      row-key="clave"
      :loading="cargando"
      hide-pagination
      :rows-per-page-options="[0]"
    >
      <template #body-cell-valor="props">
        <q-td :props="props" class="monto">{{ presentar(props.row) }}</q-td>
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
            @click="abrir(props.row)"
          />
        </q-td>
      </template>
      <template #no-data>
        <div class="full-width text-center q-pa-lg text-grey-7"
          >No hay parámetros.</div
        >
      </template>
    </q-table>

    <q-dialog v-model="dialogo" persistent>
      <q-card style="width: min(480px, 94vw)">
        <q-card-section>
          <div class="text-h6">Editar parámetro</div>
          <div class="text-caption text-grey-7">{{ form.clave }}</div>
        </q-card-section>
        <q-form @submit.prevent="guardar">
          <q-card-section class="q-gutter-md">
            <div class="text-body2">{{ form.descripcion }}</div>
            <q-input
              v-model="form.valor"
              outlined
              dense
              label="Valor"
              :hint="pista(form.clave)"
              :rules="[requerido('Indique el valor'), valorValido]"
            />
            <div v-if="vistaPorcentaje" class="text-body2">
              Equivale a {{ vistaPorcentaje }}.
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
import { computed, onMounted, reactive, ref } from 'vue'
import EncabezadoPagina from '@/components/EncabezadoPagina.vue'
import { usePermiso } from '@/composables/usePermiso'
import { api, comoLista } from '@/services/api'
import { invalidarParametros } from '@/services/parametros'
import { avisarExito } from '@/utils/dialogo'
import { porcentaje, requerido } from '@/utils/formato'

const PISTAS = {
  'credito.tasa_mensual': 'Escriba el decimal. 0.02 = 2 % mensual.',
  'credito.plazo_meses':
    'Meses que se suman a la fecha del documento para el vencimiento.',
  'credito.dias_exoneracion':
    'Si el pago ocurre antes de estos días, el interés es cero.',
  'venta.margen': 'Escriba el decimal. 0.30 = 30 % sobre el costo promedio.',
  'iva.tasa': 'Escriba el decimal. 0.13 = 13 %.',
  'devolucion_compra.dias_max':
    'Días máximos para devolver una compra al proveedor.',
  'devolucion_venta.dias_max':
    'Días máximos para aceptar una devolución de cliente.',
  'ciclo.mes_inicio': 'Mes en que inicia el año fiscal. 7 = julio.'
}

const { permitido } = usePermiso()
const puedeEscribir = computed(() => permitido('parametros', 'escritura'))
const filas = ref([])
const cargando = ref(false)
const error = ref('')
const dialogo = ref(false)
const guardando = ref(false)
const form = reactive({ clave: '', valor: '', descripcion: '' })

const columnas = [
  { name: 'clave', label: 'Clave', field: 'clave', align: 'left' },
  {
    name: 'descripcion',
    label: 'Descripción',
    field: 'descripcion',
    align: 'left',
    classes: 'col-texto',
    headerClasses: 'col-texto'
  },
  { name: 'valor', label: 'Valor', field: 'valor', align: 'right' },
  { name: 'acciones', label: '', field: 'acciones', align: 'right' }
]

const vistaPorcentaje = computed(() => {
  if (!esTasa(form.clave)) return ''
  const numero = Number(String(form.valor).trim())
  return Number.isFinite(numero) ? porcentaje(numero) : ''
})

function esTasa(clave) {
  return /tasa|margen/.test(String(clave || ''))
}

function pista(clave) {
  return PISTAS[clave] || 'El valor se guarda tal como lo escriba.'
}

function presentar(fila) {
  const numero = Number(fila.valor)
  if (esTasa(fila.clave) && Number.isFinite(numero)) {
    return `${fila.valor} (${porcentaje(numero)})`
  }
  return fila.valor
}

function valorValido(valor) {
  const texto = String(valor ?? '').trim()
  if (!/tasa|margen|plazo|dias|mes/.test(form.clave)) return true
  const numero = Number(texto)
  if (!Number.isFinite(numero) || numero < 0)
    return 'Indique un número mayor o igual que cero.'
  return true
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const { data } = await api.get('/parametros')
    filas.value = comoLista(data)
  } catch (causa) {
    filas.value = []
    error.value = causa.mensaje || 'No se pudieron cargar los parámetros.'
  } finally {
    cargando.value = false
  }
}

function abrir(fila) {
  form.clave = fila.clave
  form.valor = fila.valor ?? ''
  form.descripcion = fila.descripcion || ''
  dialogo.value = true
}

async function guardar() {
  guardando.value = true
  try {
    await api.put(`/parametros/${encodeURIComponent(form.clave)}`, {
      valor: String(form.valor).trim()
    })
    invalidarParametros()
    avisarExito('Parámetro actualizado.')
    dialogo.value = false
    await cargar()
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

onMounted(cargar)
</script>
