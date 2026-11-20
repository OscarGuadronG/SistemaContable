<template>
  <q-page class="pagina q-pa-md">
    <encabezado-pagina
      titulo="Créditos"
      subtitulo="Cuentas por cobrar y por pagar, interés y constancias"
      icono="account_balance_wallet"
    />
    <q-card flat bordered>
      <q-tabs
        v-model="tab"
        align="left"
        active-color="primary"
        indicator-color="primary"
        dense
        no-caps
        inline-label
        mobile-arrows
      >
        <q-tab
          v-if="veCobrar"
          name="venta"
          icon="south_west"
          label="Por cobrar"
        />
        <q-tab
          v-if="vePagar"
          name="compra"
          icon="north_east"
          label="Por pagar"
        />
      </q-tabs>
      <q-separator />
      <q-tab-panels v-model="tab" animated>
        <q-tab-panel v-if="veCobrar" name="venta">
          <cuentas-credito tipo="venta" />
        </q-tab-panel>
        <q-tab-panel v-if="vePagar" name="compra">
          <cuentas-credito tipo="compra" />
        </q-tab-panel>
      </q-tab-panels>
    </q-card>
  </q-page>
</template>

<script setup>
import { computed, ref } from 'vue'
import EncabezadoPagina from '@/components/EncabezadoPagina.vue'
import CuentasCredito from '@/pages/creditos/CuentasCredito.vue'
import { usePermiso } from '@/composables/usePermiso'

const { permitido } = usePermiso()
const veCobrar = computed(() => permitido('creditosVenta'))
const vePagar = computed(() => permitido('creditosCompra'))
const tab = ref(veCobrar.value ? 'venta' : 'compra')
</script>
