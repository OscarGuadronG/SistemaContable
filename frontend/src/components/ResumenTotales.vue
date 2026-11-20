<template>
  <q-list bordered separator class="rounded-borders bg-white">
    <q-item v-for="fila in filas" :key="fila.etiqueta">
      <q-item-section>
        <q-item-label :class="fila.destacado ? 'text-weight-bold' : ''">
          {{ fila.etiqueta }}
        </q-item-label>
        <q-item-label v-if="fila.ayuda" caption>{{ fila.ayuda }}</q-item-label>
      </q-item-section>
      <q-item-section side>
        <span
          class="monto"
          :class="fila.destacado ? 'text-weight-bold text-primary' : ''"
        >
          {{ fila.valor }}
        </span>
      </q-item-section>
    </q-item>
  </q-list>
</template>

<script setup>
defineProps({
  filas: { type: Array, default: () => [] }
})
</script>
