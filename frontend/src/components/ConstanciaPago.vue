<template>
  <q-card class="zona-impresion" style="width: min(640px, 92vw)">
    <q-card-section class="row items-center no-imprimir">
      <div class="text-h6">Constancia de pago</div>
      <q-space />
      <q-btn flat round icon="close" v-close-popup />
    </q-card-section>
    <q-separator class="no-imprimir" />
    <q-card-section>
      <div class="text-h6 text-weight-bold">Distribuidora Ta Bien</div>
      <div class="text-subtitle2 text-grey-8 q-mb-md">
        Constancia de abono a documento al crédito
      </div>
      <div class="row q-col-gutter-sm">
        <div class="col-6"><strong>Tipo:</strong> {{ tipoEtiqueta }}</div>
        <div class="col-6">
          <strong>Documento:</strong>
          {{ datos.numeroDocumento || datos.documentoId }}
        </div>
        <div class="col-6">
          <strong>Contraparte:</strong> {{ datos.contraparte }}
        </div>
        <div class="col-6"
          ><strong>Fecha de pago:</strong> {{ fecha(datos.fecha) }}</div
        >
        <div class="col-6"
          ><strong>Forma:</strong> {{ etiquetaFormaPago(datos.formaPago) }}</div
        >
        <div v-if="datos.numeroCheque" class="col-6">
          <strong>Cheque:</strong> {{ datos.numeroCheque }}
        </div>
        <div class="col-12" v-if="datos.descripcion">
          <strong>Descripción:</strong> {{ datos.descripcion }}
        </div>
      </div>
      <q-markup-table flat bordered dense class="q-mt-md tabla-app">
        <tbody>
          <tr>
            <td>Capital</td>
            <td class="text-right monto">{{ moneda(datos.capital) }}</td>
          </tr>
          <tr>
            <td>Interés</td>
            <td class="text-right monto">{{ moneda(datos.interes) }}</td>
          </tr>
          <tr class="text-weight-bold">
            <td>Total recibido</td>
            <td class="text-right monto">{{ moneda(total) }}</td>
          </tr>
        </tbody>
      </q-markup-table>
      <div v-if="datos.partida" class="q-mt-md">
        <div class="text-subtitle2 q-mb-xs">Partida generada</div>
        <visor-partida :partida="datos.partida" />
      </div>
      <div class="text-caption text-grey-7 q-mt-lg">
        Emitida el {{ fechaHora(new Date().toISOString()) }}
        <span v-if="datos.usuario"> · {{ datos.usuario }}</span>
      </div>
    </q-card-section>
    <q-card-actions align="right" class="no-imprimir">
      <q-btn flat label="Cerrar" v-close-popup />
      <q-btn
        color="primary"
        unelevated
        icon="print"
        label="Imprimir"
        @click="imprimir({ dialogo: true })"
      />
    </q-card-actions>
  </q-card>
</template>

<script setup>
import { computed } from 'vue'
import VisorPartida from '@/components/VisorPartida.vue'
import {
  etiquetaFormaPago,
  fecha,
  fechaHora,
  moneda,
  redondear
} from '@/utils/formato'
import { imprimir } from '@/utils/dialogo'

const props = defineProps({
  datos: { type: Object, required: true }
})

const tipoEtiqueta = computed(() =>
  props.datos.tipo === 'compra' ? 'Cuenta por pagar' : 'Cuenta por cobrar'
)

const total = computed(() =>
  redondear(Number(props.datos.capital || 0) + Number(props.datos.interes || 0))
)
</script>
