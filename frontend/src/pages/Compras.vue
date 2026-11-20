<template>
  <q-page class="pagina q-pa-md">
    <encabezado-pagina
      titulo="Compras"
      subtitulo="Recepción de mercancía, libro, proveedores y devoluciones"
      icono="shopping_cart"
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
        <q-tab name="libro" icon="menu_book" label="Libro de compras" />
        <q-tab name="proveedores" icon="local_shipping" label="Proveedores" />
        <q-tab name="devoluciones" icon="undo" label="Devoluciones" />
      </q-tabs>
      <q-separator />
      <q-tab-panels v-model="tab" animated>
        <q-tab-panel v-if="puedeRegistrar" name="registrar">
          <registrar-compra />
        </q-tab-panel>
        <q-tab-panel name="historial">
          <historial-compras />
        </q-tab-panel>
        <q-tab-panel name="libro">
          <libro-compras />
        </q-tab-panel>
        <q-tab-panel name="proveedores">
          <crud-simple
            ruta="/proveedores"
            titulo="proveedor"
            etiqueta-nuevo="Nuevo proveedor"
            :puede-escribir="puedeProveedores"
            :columnas="columnasProveedor"
            :campos="camposProveedor"
          />
        </q-tab-panel>
        <q-tab-panel name="devoluciones">
          <devoluciones-compra />
        </q-tab-panel>
      </q-tab-panels>
    </q-card>
  </q-page>
</template>

<script setup>
import { computed, ref } from 'vue'
import EncabezadoPagina from '@/components/EncabezadoPagina.vue'
import CrudSimple from '@/components/CrudSimple.vue'
import RegistrarCompra from '@/pages/compras/RegistrarCompra.vue'
import HistorialCompras from '@/pages/compras/HistorialCompras.vue'
import LibroCompras from '@/pages/compras/LibroCompras.vue'
import DevolucionesCompra from '@/pages/compras/DevolucionesCompra.vue'
import { usePermiso } from '@/composables/usePermiso'
import { requerido } from '@/utils/formato'

const { permitido } = usePermiso()
const puedeRegistrar = computed(() => permitido('compras', 'escritura'))
const puedeProveedores = computed(() => permitido('proveedores', 'escritura'))
const tab = ref(puedeRegistrar.value ? 'registrar' : 'historial')

const columnasProveedor = [
  { name: 'nombre', label: 'Nombre', field: 'nombre', align: 'left' },
  { name: 'nit', label: 'NIT', field: 'nit', align: 'left' },
  {
    name: 'descripcion',
    label: 'Descripción',
    field: 'descripcion',
    align: 'left'
  }
]
const camposProveedor = [
  { key: 'nombre', label: 'Nombre', reglas: [requerido('Indique el nombre')] },
  { key: 'nit', label: 'NIT', reglas: [requerido('Indique el NIT')] },
  { key: 'descripcion', label: 'Descripción', tipo: 'textarea' }
]
</script>
