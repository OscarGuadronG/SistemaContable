<template>
  <div class="flex flex-center bg-grey-2" style="min-height: 100vh">
    <q-card flat bordered style="width: min(440px, 94vw)">
      <div class="bg-primary" style="height: 6px" />
      <q-card-section class="text-center q-pt-lg">
        <q-img
          src="../assets/taBien.png"
          alt="Distribuidora Ta Bien"
          style="height: 84px; max-width: 220px"
          fit="contain"
        />
        <div class="text-h5 text-weight-bold q-mt-md">Sistema contable</div>
        <div class="text-body2 text-grey-7">Distribuidora Ta Bien</div>
      </q-card-section>
      <q-card-section>
        <q-banner v-if="error" class="bg-red-1 text-negative q-mb-md" rounded>
          {{ error }}
        </q-banner>
        <q-form class="q-gutter-md" @submit.prevent="enviar">
          <q-input
            v-model="email"
            type="email"
            autocomplete="username"
            outlined
            label="Correo"
            :rules="[requerido('Ingrese su correo'), correoValido]"
          >
            <template #prepend><q-icon name="mail" /></template>
          </q-input>
          <q-input
            v-model="password"
            :type="verClave ? 'text' : 'password'"
            autocomplete="current-password"
            outlined
            label="Contraseña"
            :rules="[requerido('Ingrese su contraseña')]"
          >
            <template #prepend><q-icon name="lock" /></template>
            <template #append>
              <q-icon
                :name="verClave ? 'visibility_off' : 'visibility'"
                class="cursor-pointer"
                @click="verClave = !verClave"
              />
            </template>
          </q-input>
          <q-btn
            type="submit"
            color="primary"
            unelevated
            class="full-width"
            label="Iniciar sesión"
            :loading="entrando"
          />
        </q-form>
      </q-card-section>
    </q-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSesionStore } from '@/stores/sesion'
import { destinoSeguro, mensajeAuth } from '@/utils/dialogo'
import { requerido } from '@/utils/formato'

const route = useRoute()
const router = useRouter()
const sesion = useSesionStore()

const email = ref('')
const password = ref('')
const verClave = ref(false)
const entrando = ref(false)
const error = ref(sesion.mensaje || '')

function correoValido(valor) {
  return /.+@.+\..+/.test(valor || '') || 'Ingrese un correo válido'
}

async function enviar() {
  error.value = ''
  entrando.value = true
  try {
    await sesion.entrar(email.value, password.value)
    if (!sesion.autenticado) {
      error.value = sesion.mensaje || 'No se pudo iniciar la sesión.'
      return
    }
    await router.replace(destinoSeguro(route.query.redirect))
  } catch (causa) {
    error.value = mensajeAuth(causa)
  } finally {
    entrando.value = false
  }
}
</script>
