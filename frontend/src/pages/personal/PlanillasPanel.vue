<template>
  <div>
    <div class="row q-col-gutter-sm items-end q-mb-md">
      <div class="col-6 col-sm-3"
        ><q-input
          v-model="filtros.desde"
          type="date"
          outlined
          dense
          label="Desde"
      /></div>
      <div class="col-6 col-sm-3"
        ><q-input
          v-model="filtros.hasta"
          type="date"
          outlined
          dense
          label="Hasta"
      /></div>
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
      <div class="col-auto"
        ><q-btn color="primary" outline label="Buscar" @click="reiniciar"
      /></div>
      <q-space />
      <div v-if="puedeEscribir" class="col-auto">
        <q-btn
          color="primary"
          unelevated
          icon="add"
          label="Nueva planilla"
          @click="nuevaAbierta = true"
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
      @request="cargar"
    >
      <template #body-cell-periodo="props">
        <q-td :props="props"
          >{{ fecha(props.row.fechaInicio) }} –
          {{ fecha(props.row.fechaFin) }}</q-td
        >
      </template>
      <template #body-cell-fechaPago="props"
        ><q-td :props="props">{{ fecha(props.row.fechaPago) }}</q-td></template
      >
      <template #body-cell-estado="props">
        <q-td :props="props">
          <q-badge
            :color="props.row.estado === 'Pagado' ? 'positive' : 'warning'"
          >
            {{ props.row.estado === 'Pagado' ? 'Procesada' : 'Pendiente' }}
          </q-badge>
        </q-td>
      </template>
      <template #body-cell-acciones="props">
        <q-td :props="props"
          ><q-btn
            flat
            dense
            color="primary"
            label="Detalle"
            @click="abrir(props.row)"
        /></q-td>
      </template>
    </q-table>

    <q-dialog v-model="nuevaAbierta" persistent>
      <q-card style="width: min(480px, 94vw)">
        <q-form @submit.prevent="crear">
          <q-card-section class="text-h6"
            >Nuevo periodo de planilla</q-card-section
          >
          <q-card-section class="q-gutter-md">
            <q-input
              v-model="nueva.fechaInicio"
              type="date"
              outlined
              dense
              label="Inicio"
              :rules="[requerido('Indique el inicio')]"
            />
            <q-input
              v-model="nueva.fechaFin"
              type="date"
              outlined
              dense
              label="Fin"
              :rules="[requerido('Indique el fin')]"
            />
            <q-input
              v-model="nueva.fechaPago"
              type="date"
              outlined
              dense
              label="Fecha de pago"
              :rules="[requerido('Indique la fecha de pago')]"
            />
            <q-input
              v-model="nueva.descripcion"
              outlined
              dense
              label="Descripción"
            />
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat label="Cancelar" v-close-popup />
            <q-btn
              color="primary"
              unelevated
              type="submit"
              label="Crear y generar"
              :loading="guardando"
            />
          </q-card-actions>
        </q-form>
      </q-card>
    </q-dialog>

    <q-dialog v-model="detalleAbierto" persistent>
      <q-card style="width: min(1100px, 98vw)">
        <q-card-section class="row items-center">
          <div>
            <div class="text-h6">Planilla {{ planilla?.id }}</div>
            <div class="text-caption">
              {{ fecha(planilla?.fechaInicio) }} –
              {{ fecha(planilla?.fechaFin) }} · pago
              {{ fecha(planilla?.fechaPago) }}
            </div>
          </div>
          <q-space />
          <q-btn
            v-if="pendiente && puedeEscribir"
            outline
            color="primary"
            label="Generar activos"
            @click="generar"
          />
          <q-btn flat round icon="close" v-close-popup />
        </q-card-section>
        <q-card-section>
          <q-markup-table flat bordered dense class="tabla-app">
            <thead>
              <tr>
                <th class="text-left">Empleado</th>
                <th class="text-right">Días</th>
                <th class="text-right">H. extra diurnas</th>
                <th class="text-right">H. extra nocturnas</th>
                <th class="text-right">Sueldo base</th>
                <th v-if="pendiente && puedeEscribir" />
              </tr>
            </thead>
            <tbody>
              <tr v-for="detalle in detalles" :key="detalle.id">
                <td>{{ nombreEmpleado(detalle.empleadoId) }}</td>
                <td class="text-right" style="width: 110px">
                  <q-input
                    v-if="pendiente && puedeEscribir"
                    v-model.number="detalle.dias"
                    type="number"
                    dense
                    outlined
                  />
                  <span v-else>{{ detalle.dias }}</span>
                </td>
                <td class="text-right" style="width: 130px">
                  <q-input
                    v-if="pendiente && puedeEscribir"
                    v-model.number="detalle.horasxDiur"
                    type="number"
                    dense
                    outlined
                  />
                  <span v-else>{{ detalle.horasxDiur }}</span>
                </td>
                <td class="text-right" style="width: 140px">
                  <q-input
                    v-if="pendiente && puedeEscribir"
                    v-model.number="detalle.horasxNoct"
                    type="number"
                    dense
                    outlined
                  />
                  <span v-else>{{ detalle.horasxNoct }}</span>
                </td>
                <td class="text-right monto">{{
                  moneda(detalle.sueldoBase)
                }}</td>
                <td v-if="pendiente && puedeEscribir">
                  <q-btn
                    flat
                    dense
                    color="primary"
                    label="Guardar"
                    @click="guardarDetalle(detalle)"
                  />
                </td>
              </tr>
              <tr v-if="detalles.length === 0">
                <td colspan="6" class="text-grey"
                  >Sin empleados. Genere el detalle de los activos.</td
                >
              </tr>
            </tbody>
          </q-markup-table>

          <div class="row q-gutter-sm q-mt-md">
            <q-btn
              outline
              color="primary"
              label="Ver cálculo"
              @click="verCalculo"
            />
            <q-btn
              v-if="pendiente && puedeEscribir"
              color="negative"
              unelevated
              label="Pagar planilla"
              @click="pagar"
            />
          </div>

          <div v-if="calculo" class="q-mt-md">
            <div class="text-subtitle2 q-mb-sm">Cálculo</div>
            <q-markup-table flat bordered dense class="tabla-app">
              <thead>
                <tr>
                  <th class="text-left">Empleado</th>
                  <th class="text-right">Devengado</th>
                  <th class="text-right">ISSS</th>
                  <th class="text-right">AFP</th>
                  <th class="text-right">Renta</th>
                  <th class="text-right">Líquido</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="linea in calculo.lineas || []"
                  :key="linea.detalle?.id"
                >
                  <td>{{ nombreEmpleado(linea.detalle?.empleadoId) }}</td>
                  <td class="text-right">{{
                    moneda(linea.calculo?.devengado)
                  }}</td>
                  <td class="text-right">{{ moneda(linea.calculo?.isss) }}</td>
                  <td class="text-right">{{ moneda(linea.calculo?.afp) }}</td>
                  <td class="text-right">{{ moneda(linea.calculo?.renta) }}</td>
                  <td class="text-right">{{
                    moneda(linea.calculo?.liquido)
                  }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr class="text-weight-bold">
                  <td>Totales</td>
                  <td class="text-right">{{
                    moneda(calculo.totalDevengado)
                  }}</td>
                  <td class="text-right">{{ moneda(calculo.totalIsss) }}</td>
                  <td class="text-right">{{ moneda(calculo.totalAfp) }}</td>
                  <td class="text-right">{{ moneda(calculo.totalRenta) }}</td>
                  <td class="text-right">{{ moneda(calculo.totalLiquido) }}</td>
                </tr>
              </tfoot>
            </q-markup-table>
            <div
              v-if="planilla?.estado === 'Pagado'"
              class="text-caption text-grey-7 q-mt-sm"
            >
              Planilla procesada. La partida de egreso queda ligada al pago.
            </div>
          </div>
        </q-card-section>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useListado } from '@/composables/useListado'
import { api, listarTodo } from '@/services/api'
import { avisarExito, confirmar } from '@/utils/dialogo'
import {
  aInstante,
  fecha,
  hoy,
  moneda,
  requerido,
  sumarDias
} from '@/utils/formato'

defineProps({
  puedeEscribir: { type: Boolean, default: false }
})

const filtros = reactive({ desde: '', hasta: '', estado: null })
const estados = [
  { value: 'Pendiente', label: 'Pendiente' },
  { value: 'Pagado', label: 'Procesada' }
]
const { filas, cargando, error, paginacion, cargar, reiniciar } = useListado(
  '/planillas',
  () => ({ ...filtros })
)
const columnas = [
  { name: 'periodo', label: 'Periodo', field: 'fechaInicio', align: 'left' },
  { name: 'fechaPago', label: 'Pago', field: 'fechaPago', align: 'left' },
  {
    name: 'descripcion',
    label: 'Descripción',
    field: 'descripcion',
    align: 'left'
  },
  { name: 'estado', label: 'Estado', field: 'estado', align: 'left' },
  { name: 'acciones', label: '', field: 'acciones', align: 'right' }
]
const empleados = ref([])
const nuevaAbierta = ref(false)
const guardando = ref(false)
const nueva = reactive({
  fechaInicio: sumarDias(hoy(), -29),
  fechaFin: hoy(),
  fechaPago: hoy(),
  descripcion: ''
})
const detalleAbierto = ref(false)
const planilla = ref(null)
const detalles = ref([])
const calculo = ref(null)
const pendiente = computed(() => planilla.value?.estado === 'Pendiente')

function nombreEmpleado(id) {
  return (
    empleados.value.find(item => item.id === id)?.nombreCompleto ||
    `Empleado ${id}`
  )
}

async function crear() {
  guardando.value = true
  try {
    const { data } = await api.post('/planillas', {
      fechaInicio: aInstante(nueva.fechaInicio),
      fechaFin: aInstante(nueva.fechaFin),
      fechaPago: aInstante(nueva.fechaPago),
      descripcion: nueva.descripcion?.trim() || null
    })
    await api.post(`/planillas/${data.id}/detalles/generar`)
    avisarExito('Planilla creada con los empleados activos.')
    nuevaAbierta.value = false
    await reiniciar()
    await abrir(data)
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

async function abrir(fila) {
  detalleAbierto.value = true
  planilla.value = fila
  calculo.value = null
  try {
    const { data } = await api.get(`/planillas/${fila.id}/detalles`)
    detalles.value = Array.isArray(data) ? data : []
  } catch {
    detalles.value = []
  }
}

async function generar() {
  try {
    await api.post(`/planillas/${planilla.value.id}/detalles/generar`)
    avisarExito('Se agregaron los empleados activos que faltaban.')
    await abrir(planilla.value)
  } catch {
    // El cliente HTTP ya mostró el error.
  }
}

async function guardarDetalle(detalle) {
  try {
    await api.put(`/planillas/detalles/${detalle.id}`, {
      dias: Number(detalle.dias),
      horasxDiur: Number(detalle.horasxDiur),
      horasxNoct: Number(detalle.horasxNoct)
    })
    avisarExito('Detalle actualizado.')
    await abrir(planilla.value)
  } catch {
    // El cliente HTTP ya mostró el error.
  }
}

async function verCalculo() {
  try {
    const { data } = await api.get(`/planillas/${planilla.value.id}/calculo`)
    calculo.value = data
  } catch {
    calculo.value = null
  }
}

async function pagar() {
  const acepta = await confirmar({
    titulo: 'Pagar planilla',
    mensaje:
      'La planilla pasará a procesada y se generará la partida de egreso. No se podrá modificar.',
    ok: 'Pagar',
    color: 'negative'
  })
  if (!acepta) return
  try {
    const { data } = await api.post(`/planillas/${planilla.value.id}/pagar`)
    planilla.value = data?.planilla || { ...planilla.value, estado: 'Pagado' }
    avisarExito('Planilla pagada.')
    await verCalculo()
    await reiniciar()
  } catch {
    // El cliente HTTP ya mostró el error.
  }
}

onMounted(async () => {
  try {
    empleados.value = await listarTodo('/empleados/seleccion')
  } catch {
    empleados.value = []
  }
  await cargar()
})
</script>
