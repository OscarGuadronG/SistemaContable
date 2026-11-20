<template>
  <div>
    <div class="row q-col-gutter-sm items-end q-mb-md">
      <div class="col-12 col-sm-4">
        <q-select
          v-model="estado"
          :options="estados"
          emit-value
          map-options
          outlined
          dense
          label="Estado"
        />
      </div>
      <div class="col-auto">
        <q-btn color="primary" outline label="Actualizar" @click="reiniciar" />
      </div>
    </div>
    <q-banner v-if="error" class="bg-red-1 text-negative q-mb-md" rounded>{{
      error
    }}</q-banner>
    <q-table
      flat
      bordered
      class="tabla-app"
      :rows="filasVista"
      :columns="columnas"
      row-key="_clave"
      v-model:pagination="paginacion"
      :loading="cargando"
      :rows-per-page-options="[10, 20, 50]"
      @request="alPedir"
    >
      <template #body-cell-fecha="props">
        <q-td :props="props">{{ fecha(props.row.fecha) }}</q-td>
      </template>
      <template #body-cell-fechaVencimiento="props">
        <q-td :props="props">{{ fecha(props.row.fechaVencimiento) }}</q-td>
      </template>
      <template #body-cell-saldo="props">
        <q-td :props="props" class="text-right monto">{{
          moneda(props.row.saldo)
        }}</q-td>
      </template>
      <template #body-cell-estado="props">
        <q-td :props="props">
          <q-badge
            :color="
              props.row.vencido
                ? 'negative'
                : props.row.saldo <= 0
                  ? 'positive'
                  : 'warning'
            "
          >
            {{
              props.row.vencido
                ? 'Vencido'
                : props.row.saldo <= 0
                  ? 'Cancelado'
                  : 'Pendiente'
            }}
          </q-badge>
        </q-td>
      </template>
      <template #body-cell-acciones="props">
        <q-td :props="props">
          <q-btn
            flat
            dense
            color="primary"
            label="Detalle"
            @click="abrir(props.row)"
          />
        </q-td>
      </template>
    </q-table>

    <q-dialog v-model="abierto" persistent>
      <q-card style="width: min(860px, 96vw)">
        <q-card-section class="row items-center">
          <div>
            <div class="text-h6">{{
              detalle?.numeroDocumento || detalle?.documentoId
            }}</div>
            <div class="text-caption">
              {{ textoPersona(detalle?.contraparte) }} · vence
              {{ fecha(detalle?.fechaVencimiento) }}
            </div>
          </div>
          <q-space />
          <q-btn flat round icon="close" v-close-popup />
        </q-card-section>
        <q-separator />
        <q-card-section v-if="cargandoDetalle" class="text-center">
          <q-spinner color="primary" size="28px" />
        </q-card-section>
        <q-card-section v-else-if="detalle">
          <div class="row q-col-gutter-sm q-mb-md">
            <div class="col-4"
              ><div class="text-caption text-grey-7">Total</div
              >{{ moneda(detalle.total) }}</div
            >
            <div class="col-4"
              ><div class="text-caption text-grey-7">Pagado</div
              >{{ moneda(detalle.pagadoCapital) }}</div
            >
            <div class="col-4"
              ><div class="text-caption text-grey-7">Saldo</div
              ><strong>{{ moneda(detalle.saldo) }}</strong></div
            >
          </div>
          <div class="text-subtitle2 q-mb-xs">Pagos</div>
          <q-markup-table flat bordered dense class="tabla-app q-mb-md">
            <thead>
              <tr>
                <th class="text-left">Fecha</th>
                <th class="text-right">Capital</th>
                <th class="text-right">Interés</th>
                <th class="text-left">Forma</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="pago in detalle.pagos || []" :key="pago.id">
                <td>{{ fecha(pago.fecha) }}</td>
                <td class="text-right">{{ moneda(pago.capital) }}</td>
                <td class="text-right">{{ moneda(pago.interes) }}</td>
                <td>{{ etiquetaFormaPago(pago.formaPago) }}</td>
              </tr>
              <tr v-if="!(detalle.pagos || []).length">
                <td colspan="4" class="text-grey">Sin pagos</td>
              </tr>
            </tbody>
          </q-markup-table>

          <div class="text-subtitle2 q-mb-xs"
            >Simulación si cancela el saldo</div
          >
          <div class="row q-col-gutter-sm items-end q-mb-md">
            <div class="col-12 col-sm-4">
              <q-input
                v-model="fechaSimulacion"
                type="date"
                outlined
                dense
                label="Fecha de pago"
                @update:model-value="simular"
              />
            </div>
            <div class="col">
              <span v-if="detalle.simulacion">
                {{ detalle.simulacion.dias }} días ·
                {{ detalle.simulacion.meses }} meses · interés
                {{ moneda(detalle.simulacion.interes) }}
              </span>
            </div>
          </div>

          <q-form
            v-if="puedePagar && Number(detalle.saldo) > 0"
            class="q-gutter-sm"
            @submit.prevent="confirmarPago"
          >
            <div class="text-subtitle2">Registrar pago</div>
            <div class="row q-col-gutter-sm">
              <div class="col-6 col-sm-3">
                <q-input
                  v-model.number="pago.capital"
                  type="number"
                  min="0.01"
                  step="0.01"
                  outlined
                  dense
                  label="Capital"
                  :rules="[capitalValido]"
                />
              </div>
              <div class="col-6 col-sm-3">
                <q-input
                  v-model="pago.fecha"
                  type="date"
                  outlined
                  dense
                  label="Fecha"
                  :rules="[requerido('Indique la fecha')]"
                />
              </div>
              <div class="col-6 col-sm-3">
                <q-select
                  v-model="pago.formaPago"
                  :options="FORMAS_PAGO_INMEDIATO"
                  emit-value
                  map-options
                  outlined
                  dense
                  label="Forma"
                />
              </div>
              <div v-if="pago.formaPago === 'Cheque'" class="col-6 col-sm-3">
                <q-input
                  v-model="pago.numeroCheque"
                  outlined
                  dense
                  label="N.º de cheque"
                  :rules="[requerido('Indique el cheque')]"
                />
              </div>
              <div class="col-12">
                <q-input
                  v-model="pago.descripcion"
                  outlined
                  dense
                  label="Descripción"
                />
              </div>
            </div>
            <q-banner class="bg-blue-1 text-primary" rounded>
              <div v-if="interes.invalido"
                >La fecha de pago no puede ser anterior al documento.</div
              >
              <div v-else-if="interes.exonerado">
                {{ interes.dias }} días: dentro de la exoneración ({{
                  diasExoneracion
                }}
                días). Interés {{ moneda(0) }}. Total a entregar
                {{ moneda(pago.capital) }}.
              </div>
              <div v-else>
                {{ interes.dias }} días → {{ interes.meses }} mes(es) × tasa
                {{ porcentaje(tasa) }}. Interés {{ moneda(interes.interes) }}.
                Total a entregar {{ moneda(totalPago) }}.
              </div>
            </q-banner>
            <q-btn
              color="primary"
              unelevated
              type="submit"
              label="Confirmar pago"
              :loading="guardando"
              :disable="interes.invalido"
            />
          </q-form>
        </q-card-section>
      </q-card>
    </q-dialog>

    <q-dialog v-model="constanciaAbierta">
      <constancia-pago v-if="constancia" :datos="constancia" />
    </q-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import ConstanciaPago from '@/components/ConstanciaPago.vue'
import { useListado } from '@/composables/useListado'
import { usePermiso } from '@/composables/usePermiso'
import { api } from '@/services/api'
import { mapaParametros, numeroParametro } from '@/services/parametros'
import { simularInteres } from '@/utils/calculos'
import { avisarExito, confirmar } from '@/utils/dialogo'
import {
  FORMAS_PAGO_INMEDIATO,
  etiquetaFormaPago,
  fecha,
  hoy,
  moneda,
  porcentaje,
  redondear,
  requerido,
  textoPersona
} from '@/utils/formato'

const props = defineProps({
  tipo: { type: String, required: true }
})

const { permitido, nombre } = usePermiso()
const recurso = computed(() =>
  props.tipo === 'compra' ? 'creditosCompra' : 'creditosVenta'
)
const puedePagar = computed(() => permitido(recurso.value, 'escritura'))

const estado = ref('pendiente')
const estados = [
  { value: 'pendiente', label: 'Pendientes' },
  { value: 'cancelado', label: 'Cancelados' },
  { value: 'todos', label: 'Todos' }
]
const { filas, cargando, error, paginacion, cargar, reiniciar } = useListado(
  '/creditos',
  () => ({
    tipo: props.tipo,
    estado: estado.value
  })
)
const filasVista = computed(() =>
  filas.value.map(fila => ({
    ...fila,
    _clave: `${fila.tipo || props.tipo}-${fila.documentoId}`
  }))
)

const columnas = [
  { name: 'fecha', label: 'Fecha', field: 'fecha', align: 'left' },
  {
    name: 'numeroDocumento',
    label: 'Documento',
    field: 'numeroDocumento',
    align: 'left'
  },
  {
    name: 'contraparte',
    label: props.tipo === 'compra' ? 'Proveedor' : 'Cliente',
    field: fila => textoPersona(fila.contraparte),
    align: 'left'
  },
  {
    name: 'total',
    label: 'Total',
    field: 'total',
    align: 'right',
    format: moneda
  },
  { name: 'saldo', label: 'Saldo', field: 'saldo', align: 'right' },
  {
    name: 'fechaVencimiento',
    label: 'Vencimiento',
    field: 'fechaVencimiento',
    align: 'left'
  },
  { name: 'estado', label: 'Estado', field: 'vencido', align: 'left' },
  { name: 'acciones', label: '', field: 'acciones', align: 'right' }
]

const abierto = ref(false)
const detalle = ref(null)
const cargandoDetalle = ref(false)
const fechaSimulacion = ref(hoy())
const diasExoneracion = ref(30)
const tasaParametro = ref(0.02)
const guardando = ref(false)
const pago = reactive({
  capital: 0,
  fecha: hoy(),
  formaPago: 'Contado',
  numeroCheque: '',
  descripcion: ''
})
const constanciaAbierta = ref(false)
const constancia = ref(null)

const tasa = computed(() =>
  Number(detalle.value?.tasaInteres ?? tasaParametro.value)
)
const interes = computed(() =>
  simularInteres({
    fechaDocumento: detalle.value?.fecha,
    fechaPago: pago.fecha,
    capital: pago.capital,
    tasa: tasa.value,
    diasExoneracion: diasExoneracion.value
  })
)
const totalPago = computed(() =>
  redondear(Number(pago.capital || 0) + interes.value.interes)
)

function capitalValido(valor) {
  const numero = Number(valor)
  if (!Number.isFinite(numero) || numero <= 0) return 'Indique el capital'
  if (detalle.value && numero > Number(detalle.value.saldo))
    return 'El capital supera el saldo'
  return true
}

function alPedir(propsTabla) {
  return cargar(propsTabla)
}

async function simular() {
  if (!detalle.value) return
  try {
    const { data } = await api.get(
      `/creditos/${props.tipo}/${detalle.value.documentoId}`,
      {
        query: { fecha: fechaSimulacion.value },
        silencioso: true
      }
    )
    detalle.value = data
  } catch {
    // Se conserva el detalle ya cargado.
  }
}

async function abrir(fila) {
  abierto.value = true
  cargandoDetalle.value = true
  detalle.value = null
  fechaSimulacion.value = hoy()
  pago.capital = Number(fila.saldo) || 0
  pago.fecha = hoy()
  pago.formaPago = 'Contado'
  pago.numeroCheque = ''
  pago.descripcion = ''
  try {
    const { data } = await api.get(
      `/creditos/${props.tipo}/${fila.documentoId}`,
      {
        query: { fecha: fechaSimulacion.value }
      }
    )
    detalle.value = data
    pago.capital = Number(data.saldo) || 0
  } catch {
    abierto.value = false
  } finally {
    cargandoDetalle.value = false
  }
}

async function confirmarPago() {
  const acepta = await confirmar({
    titulo: 'Confirmar pago',
    mensaje: `Se registrará un capital de ${moneda(pago.capital)} más interés de ${moneda(interes.value.interes)}. Total ${moneda(totalPago.value)}.`,
    ok: 'Registrar pago',
    color: 'primary'
  })
  if (!acepta) return
  guardando.value = true
  const cuerpo = {
    tipo: props.tipo,
    documentoId: detalle.value.documentoId,
    fecha: pago.fecha,
    capital: redondear(pago.capital),
    formaPago: pago.formaPago,
    descripcion: pago.descripcion?.trim() || null
  }
  if (pago.formaPago === 'Cheque')
    cuerpo.numeroCheque = pago.numeroCheque.trim()
  try {
    const { data } = await api.post('/creditos/pagos', cuerpo)
    avisarExito('Pago registrado.')
    constancia.value = {
      tipo: props.tipo,
      documentoId: detalle.value.documentoId,
      numeroDocumento: detalle.value.numeroDocumento,
      contraparte: textoPersona(detalle.value.contraparte),
      fecha: pago.fecha,
      formaPago: pago.formaPago,
      numeroCheque: cuerpo.numeroCheque,
      descripcion: cuerpo.descripcion,
      capital: cuerpo.capital,
      interes: data?.pago?.interes ?? interes.value.interes,
      partida: data?.partida,
      usuario: nombre.value
    }
    constanciaAbierta.value = true
    abierto.value = false
    await reiniciar()
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

onMounted(async () => {
  try {
    const mapa = await mapaParametros()
    diasExoneracion.value = numeroParametro(
      mapa,
      'credito.dias_exoneracion',
      30
    )
    tasaParametro.value = numeroParametro(mapa, 'credito.tasa_mensual', 0.02)
  } catch {
    // Se usan los valores por defecto del contrato.
  }
  await cargar()
})
</script>
