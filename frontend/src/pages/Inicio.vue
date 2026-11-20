<template>
  <q-page class="pagina q-pa-md">
    <div class="row items-center q-col-gutter-md q-mb-lg">
      <div class="col">
        <div class="text-h4 text-weight-bold"
          >Hola, {{ nombre || 'bienvenido' }}</div
        >
        <div class="text-body1 text-grey-7">
          Distribuidora Ta Bien · sistema de gestión contable
        </div>
      </div>
      <div class="col-auto">
        <q-chip
          v-if="rol"
          :color="COLOR_ROL[rol] || 'primary'"
          text-color="white"
        >
          {{ rol }}
        </q-chip>
      </div>
    </div>

    <div class="row q-col-gutter-md q-mb-lg">
      <div class="col-12 col-md-6" v-if="vePeriodo">
        <q-card flat bordered>
          <q-card-section>
            <div class="text-subtitle2 text-grey-7"
              >Periodo contable actual</div
            >
            <div v-if="cargandoPeriodo" class="q-mt-sm">
              <q-spinner color="primary" size="24px" />
            </div>
            <div v-else-if="errorPeriodo" class="text-negative q-mt-sm">
              {{ errorPeriodo }}
            </div>
            <div v-else class="text-h6 q-mt-xs">{{ etiquetaPeriodo }}</div>
            <div
              v-if="periodo && !errorPeriodo"
              class="text-caption text-grey-7"
            >
              Ciclo que inicia en julio · trimestres fiscales
            </div>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-12 col-md-6">
        <q-card flat bordered>
          <q-card-section>
            <div class="text-subtitle2 text-grey-7">Parámetros vigentes</div>
            <div v-if="errorParametros" class="text-negative q-mt-sm">
              {{ errorParametros }}
            </div>
            <div v-else class="row q-col-gutter-sm q-mt-xs">
              <div class="col-4">
                <div class="text-caption text-grey-7">IVA</div>
                <div class="text-subtitle1">{{ textoIva }}</div>
              </div>
              <div class="col-4">
                <div class="text-caption text-grey-7">Interés mensual</div>
                <div class="text-subtitle1">{{ textoTasa }}</div>
              </div>
              <div class="col-4">
                <div class="text-caption text-grey-7">Margen de venta</div>
                <div class="text-subtitle1">{{ textoMargen }}</div>
              </div>
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>

    <div class="text-subtitle1 text-weight-medium q-mb-sm">Accesos</div>
    <div class="row q-col-gutter-md">
      <div
        v-for="item in accesos"
        :key="item.to"
        class="col-12 col-sm-6 col-md-4 col-lg-3"
      >
        <q-card
          flat
          bordered
          class="cursor-pointer team-card"
          @click="ir(item.to)"
        >
          <q-card-section class="row items-center no-wrap">
            <q-avatar
              rounded
              color="blue-1"
              text-color="primary"
              :icon="item.icono"
            />
            <div class="q-ml-md text-subtitle1 text-weight-medium">
              {{ item.titulo }}
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useSesionStore } from '@/stores/sesion'
import { COLOR_ROL, menuDe, puede } from '@/config/permisos'
import { api } from '@/services/api'
import { mapaParametros, numeroParametro } from '@/services/parametros'
import { fecha, porcentaje } from '@/utils/formato'

const router = useRouter()
const sesion = useSesionStore()
const { nombre, rol } = storeToRefs(sesion)

const periodo = ref(null)
const cargandoPeriodo = ref(false)
const errorPeriodo = ref('')
const parametros = ref(null)
const errorParametros = ref('')

const vePeriodo = computed(() => puede(rol.value, 'periodos'))
const accesos = computed(() =>
  menuDe(rol.value)
    .flatMap(grupo => grupo.items)
    .filter(item => item.to !== '/')
)

const etiquetaPeriodo = computed(() => {
  const actual = periodo.value
  if (!actual) return 'Sin periodo para la fecha de hoy'
  if (actual.nombre) return actual.nombre
  const inicio = fecha(actual.fechaInicio || actual.desde)
  const fin = fecha(actual.fechaFin || actual.hasta)
  const estado = actual.estado ? ` · ${actual.estado}` : ''
  return `${inicio} – ${fin}${estado}`
})

const textoIva = computed(() =>
  porcentaje(numeroParametro(parametros.value, 'iva.tasa', 0.13))
)
const textoTasa = computed(() =>
  porcentaje(numeroParametro(parametros.value, 'credito.tasa_mensual', 0.02))
)
const textoMargen = computed(() =>
  porcentaje(numeroParametro(parametros.value, 'venta.margen', 0.3))
)

function ir(ruta) {
  router.push(ruta)
}

onMounted(async () => {
  if (vePeriodo.value) {
    cargandoPeriodo.value = true
    try {
      const { data } = await api.get('/periodos/actual', { silencioso: true })
      periodo.value = data
    } catch (causa) {
      errorPeriodo.value = causa.mensaje || 'No se pudo consultar el periodo.'
    } finally {
      cargandoPeriodo.value = false
    }
  }
  try {
    parametros.value = await mapaParametros()
  } catch (causa) {
    errorParametros.value =
      causa.mensaje || 'No se pudieron leer los parámetros.'
  }
})
</script>
