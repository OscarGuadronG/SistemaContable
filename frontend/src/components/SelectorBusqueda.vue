<template>
  <q-select
    :model-value="modelValue"
    :options="filtradas"
    :option-value="opcionValor"
    :option-label="etiquetaDe"
    emit-value
    map-options
    use-input
    input-debounce="150"
    outlined
    dense
    clearable
    behavior="menu"
    :label="etiqueta"
    :loading="cargando"
    :disable="disable"
    :rules="reglas"
    :hint="hint"
    @filter="filtrar"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template v-if="$slots.option" #option="scope">
      <slot name="option" v-bind="scope" />
    </template>
    <template #no-option>
      <q-item>
        <q-item-section class="text-grey">Sin coincidencias</q-item-section>
      </q-item>
    </template>
  </q-select>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { default: null },
  opciones: { type: Array, default: () => [] },
  opcionValor: { type: String, default: 'id' },
  opcionEtiqueta: { type: [String, Function], default: 'nombre' },
  etiqueta: { type: String, default: '' },
  cargando: { type: Boolean, default: false },
  disable: { type: Boolean, default: false },
  reglas: { type: Array, default: () => [] },
  hint: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])
const filtradas = ref([])

watch(
  () => props.opciones,
  valor => {
    filtradas.value = valor || []
  },
  { immediate: true }
)

function etiquetaDe(opcion) {
  if (!opcion) return ''
  if (typeof props.opcionEtiqueta === 'function') {
    return props.opcionEtiqueta(opcion)
  }
  return String(opcion[props.opcionEtiqueta] ?? '')
}

function filtrar(valor, update) {
  update(() => {
    const consulta = String(valor || '')
      .trim()
      .toLowerCase()
    if (!consulta) {
      filtradas.value = props.opciones
      return
    }
    filtradas.value = props.opciones.filter(opcion =>
      etiquetaDe(opcion).toLowerCase().includes(consulta)
    )
  })
}
</script>
