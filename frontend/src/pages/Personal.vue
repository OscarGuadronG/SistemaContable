<template>
  <q-page class="pagina q-pa-md">
    <encabezado-pagina
      titulo="Personal"
      subtitulo="Empleados, departamentos y planillas de pago"
      icono="groups"
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
        <q-tab name="empleados" icon="badge" label="Empleados" />
        <q-tab name="departamentos" icon="domain" label="Departamentos" />
        <q-tab name="planillas" icon="payments" label="Planillas" />
      </q-tabs>
      <q-separator />
      <q-tab-panels v-model="tab" animated>
        <q-tab-panel name="empleados">
          <empleados-panel :puede-escribir="puedeEscribir" />
        </q-tab-panel>
        <q-tab-panel name="departamentos">
          <crud-simple
            ruta="/departamentos"
            titulo="departamento"
            etiqueta-nuevo="Nuevo departamento"
            :puede-escribir="puedeEscribir"
            :columnas="columnasDepartamento"
            :campos="camposDepartamento"
          />
        </q-tab-panel>
        <q-tab-panel name="planillas">
          <planillas-panel :puede-escribir="puedeEscribir" />
        </q-tab-panel>
      </q-tab-panels>
    </q-card>
  </q-page>
</template>

<script setup>
import { computed, ref } from 'vue'
import EncabezadoPagina from '@/components/EncabezadoPagina.vue'
import CrudSimple from '@/components/CrudSimple.vue'
import EmpleadosPanel from '@/pages/personal/EmpleadosPanel.vue'
import PlanillasPanel from '@/pages/personal/PlanillasPanel.vue'
import { usePermiso } from '@/composables/usePermiso'
import { requerido } from '@/utils/formato'

const { permitido } = usePermiso()
const puedeEscribir = computed(() => permitido('personal', 'escritura'))
const tab = ref('empleados')
const columnasDepartamento = [
  { name: 'nombre', label: 'Nombre', field: 'nombre', align: 'left' },
  {
    name: 'descripcion',
    label: 'Descripción',
    field: 'descripcion',
    align: 'left'
  }
]
const camposDepartamento = [
  { key: 'nombre', label: 'Nombre', reglas: [requerido('Indique el nombre')] },
  { key: 'descripcion', label: 'Descripción', tipo: 'textarea' }
]
</script>
