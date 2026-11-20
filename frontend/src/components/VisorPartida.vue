<template>
  <div>
    <div v-if="!partida" class="text-grey-7">
      Esta operación no generó partida contable.
    </div>
    <template v-else>
      <div class="row q-col-gutter-sm q-mb-sm text-body2">
        <div class="col-auto">
          <strong>Fecha:</strong> {{ fecha(partida.fecha) }}
        </div>
        <div v-if="partida.tipo" class="col-auto">
          <strong>Tipo:</strong> {{ partida.tipo }}
        </div>
        <div v-if="partida.concepto" class="col-12 col-texto">
          <strong>Concepto:</strong> {{ partida.concepto }}
        </div>
      </div>
      <q-markup-table
        flat
        bordered
        dense
        separator="horizontal"
        class="tabla-app"
      >
        <thead>
          <tr>
            <th class="text-left">Cuenta</th>
            <th class="text-right">Cargo</th>
            <th class="text-right">Abono</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(linea, indice) in lineas" :key="linea.id || indice">
            <td class="col-texto">{{ cuentaDe(linea) }}</td>
            <td class="text-right monto">{{ moneda(linea.cargo) }}</td>
            <td class="text-right monto">{{ moneda(linea.abono) }}</td>
          </tr>
          <tr v-if="lineas.length === 0">
            <td colspan="3" class="text-grey">Sin líneas</td>
          </tr>
        </tbody>
        <tfoot>
          <tr class="text-weight-bold">
            <td>Totales</td>
            <td class="text-right monto">{{ moneda(totalCargo) }}</td>
            <td class="text-right monto">{{ moneda(totalAbono) }}</td>
          </tr>
        </tfoot>
      </q-markup-table>
      <q-banner
        v-if="lineas.length > 0 && totalCargo !== totalAbono"
        class="bg-orange-1 text-orange-10 q-mt-sm"
        rounded
        dense
      >
        La partida no cuadra.
      </q-banner>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { fecha, moneda, redondear, textoRelacion } from '@/utils/formato'

const props = defineProps({
  partida: { type: Object, default: null }
})

const lineas = computed(() => props.partida?.lineas || [])

const totalCargo = computed(() =>
  redondear(
    lineas.value.reduce(
      (acumulado, linea) => acumulado + Number(linea.cargo || 0),
      0
    )
  )
)

const totalAbono = computed(() =>
  redondear(
    lineas.value.reduce(
      (acumulado, linea) => acumulado + Number(linea.abono || 0),
      0
    )
  )
)

function cuentaDe(linea) {
  if (linea.cuenta) {
    const codigo = linea.cuenta.codigo ?? ''
    const nombre = linea.cuenta.nombre ?? ''
    return [codigo, nombre].filter(Boolean).join(' — ') || '—'
  }
  return textoRelacion(linea, 'cuentaNombre', 'cuentaId')
}
</script>
