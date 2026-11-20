<template>
  <q-page class="pagina q-pa-md">
    <encabezado-pagina
      titulo="Ventas"
      subtitulo="Facturación por almacén, libro, clientes y devoluciones"
      icono="point_of_sale"
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
        outside-arrows
      >
        <q-tab
          v-if="puedeRegistrar"
          name="registrar"
          icon="add_shopping_cart"
          label="Registrar"
        />
        <q-tab name="historial" icon="history" label="Historial" />
        <q-tab
          v-if="veLibro"
          name="libro"
          icon="menu_book"
          label="Libro de ventas"
        />
        <q-tab
          v-if="veClientes"
          name="clientes"
          icon="people"
          label="Clientes"
        />
        <q-tab
          v-if="veDevoluciones"
          name="devoluciones"
          icon="undo"
          label="Devoluciones"
        />
      </q-tabs>
      <q-separator />
      <q-tab-panels v-model="tab" animated>
        <q-tab-panel v-if="puedeRegistrar" name="registrar">
          <registrar-venta />
        </q-tab-panel>
        <q-tab-panel name="historial">
          <historial-ventas />
        </q-tab-panel>
        <q-tab-panel v-if="veLibro" name="libro">
          <libro-ventas />
        </q-tab-panel>
        <q-tab-panel v-if="veClientes" name="clientes">
          <clientes-panel :puede-escribir="puedeClientes" />
        </q-tab-panel>
        <q-tab-panel v-if="veDevoluciones" name="devoluciones">
          <devoluciones-venta />
        </q-tab-panel>
      </q-tab-panels>
    </q-card>
  </q-page>
</template>

<script setup>
import { computed, ref } from 'vue'
import EncabezadoPagina from '@/components/EncabezadoPagina.vue'
import RegistrarVenta from '@/pages/ventas/RegistrarVenta.vue'
import HistorialVentas from '@/pages/ventas/HistorialVentas.vue'
import LibroVentas from '@/pages/ventas/LibroVentas.vue'
import ClientesPanel from '@/pages/ventas/ClientesPanel.vue'
import DevolucionesVenta from '@/pages/ventas/DevolucionesVenta.vue'
import { usePermiso } from '@/composables/usePermiso'

const { permitido } = usePermiso()
const puedeRegistrar = computed(() => permitido('ventas', 'escritura'))
const veLibro = computed(() => permitido('libroVentas'))
const veClientes = computed(() => permitido('clientes'))
const puedeClientes = computed(() => permitido('clientes', 'escritura'))
const veDevoluciones = computed(() => permitido('devolucionesVenta'))
const tab = ref(puedeRegistrar.value ? 'registrar' : 'historial')
</script>
