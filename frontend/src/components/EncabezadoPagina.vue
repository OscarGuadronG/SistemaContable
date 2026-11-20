<template>
  <div class="row items-center q-col-gutter-md q-mb-md">
    <div class="col">
      <div class="row items-center no-wrap">
        <q-avatar
          v-if="icono"
          rounded
          color="blue-1"
          text-color="primary"
          :icon="icono"
          size="42px"
          class="q-mr-md"
        />
        <div>
          <div class="text-h5 text-weight-bold text-grey-10">{{ titulo }}</div>
          <div v-if="subtitulo" class="text-body2 text-grey-7">
            {{ subtitulo }}
          </div>
        </div>
      </div>
    </div>
    <div class="col-auto">
      <slot name="acciones" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  titulo: { type: String, required: true },
  subtitulo: { type: String, default: '' },
  icono: { type: String, default: '' }
})
</script>
