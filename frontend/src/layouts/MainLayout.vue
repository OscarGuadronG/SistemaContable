<template>
  <q-layout view="hHh lpR lFf">
    <q-header elevated class="bg-primary text-white">
      <q-toolbar class="q-py-sm">
        <q-btn
          dense
          flat
          round
          icon="menu"
          aria-label="Abrir menú"
          @click="alternarMenu"
        />
        <q-toolbar-title class="row items-center no-wrap q-ml-sm">
          <q-img
            src="../assets/taBien.png"
            alt="Distribuidora Ta Bien"
            class="marca-logo q-mr-md"
            style="height: 42px; width: 86px"
            fit="contain"
          />
          <div class="column">
            <div class="text-subtitle1 text-weight-bold ellipsis">
              Distribuidora Ta Bien
            </div>
            <div class="text-caption text-blue-2 gt-xs">
              Sistema de gestión contable
            </div>
          </div>
        </q-toolbar-title>
        <div class="gt-sm text-caption q-mr-md">
          <q-icon name="calendar_today" class="q-mr-xs" />
          {{ fechaHoy }}
        </div>
        <q-btn flat no-caps aria-label="Menú del usuario">
          <q-avatar size="34px" color="white" text-color="primary">
            {{ inicial }}
          </q-avatar>
          <div class="column items-start q-ml-sm gt-xs">
            <span
              class="text-weight-bold text-body2 ellipsis"
              style="max-width: 180px"
            >
              {{ nombre || 'Usuario' }}
            </span>
            <span class="text-caption text-blue-2">{{ rol || 'Sin rol' }}</span>
          </div>
          <q-icon name="keyboard_arrow_down" class="q-ml-xs" />
          <q-menu>
            <q-list style="min-width: 220px">
              <q-item>
                <q-item-section>
                  <q-item-label class="text-weight-medium">{{
                    nombre
                  }}</q-item-label>
                  <q-item-label caption>{{ email }}</q-item-label>
                </q-item-section>
              </q-item>
              <q-separator />
              <q-item v-close-popup clickable @click="salir">
                <q-item-section avatar>
                  <q-icon name="logout" color="negative" />
                </q-item-section>
                <q-item-section class="text-negative"
                  >Cerrar sesión</q-item-section
                >
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
      </q-toolbar>
    </q-header>

    <q-drawer
      v-model="menuAbierto"
      show-if-above
      bordered
      :width="260"
      side="left"
    >
      <q-scroll-area class="fit">
        <div class="q-pa-md text-center">
          <q-img
            src="../assets/taBien.png"
            alt=""
            style="height: 64px; max-width: 160px"
            fit="contain"
          />
          <div class="text-subtitle1 text-weight-bold q-mt-sm"
            >Sistema contable</div
          >
          <div class="text-caption text-grey-7">Administración y finanzas</div>
        </div>
        <q-separator />
        <q-banner
          v-if="avisoBackend"
          dense
          class="bg-orange-1 text-orange-10 q-ma-sm"
          rounded
        >
          {{ avisoBackend }}
        </q-banner>
        <q-list padding>
          <template v-for="grupo in menu" :key="grupo.grupo">
            <q-item-label header class="text-weight-bold text-grey-7">
              {{ grupo.grupo.toUpperCase() }}
            </q-item-label>
            <q-item
              v-for="item in grupo.items"
              :key="item.to"
              v-ripple
              clickable
              :to="item.to"
              :exact="item.exacto"
              active-class="bg-blue-1 text-primary item-activo"
            >
              <q-item-section avatar>
                <q-icon :name="item.icono" />
              </q-item-section>
              <q-item-section>{{ item.titulo }}</q-item-section>
            </q-item>
            <q-separator spaced />
          </template>
        </q-list>
      </q-scroll-area>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useSesionStore } from '@/stores/sesion'
import { menuDe } from '@/config/permisos'

const router = useRouter()
const sesion = useSesionStore()
const { nombre, rol, email, avisoBackend } = storeToRefs(sesion)
const menuAbierto = ref(false)
const fechaHoy = new Date().toLocaleDateString('es-SV', {
  day: '2-digit',
  month: 'long',
  year: 'numeric'
})

const menu = computed(() => menuDe(rol.value))
const inicial = computed(() =>
  (nombre.value || 'U').trim().charAt(0).toUpperCase()
)

function alternarMenu() {
  menuAbierto.value = !menuAbierto.value
}

async function salir() {
  await sesion.cerrar()
  await router.push('/login')
}
</script>
