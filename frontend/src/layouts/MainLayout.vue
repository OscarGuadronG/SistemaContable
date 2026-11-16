<template>
  <q-layout view="hHh lpR lFf">
<q-header elevated class="bg-primary text-white">
  <q-toolbar class="q-py-sm">
    <q-btn dense flat round icon="menu" @click="toggleLeftDrawer" aria-label="Abrir menú"/>
    <q-toolbar-title class="row items-center no-wrap q-ml-sm">
      <q-img src="../assets/taBien.png" alt="Distribuidora Ta Bien" style="height: 42px; width: auto; object-fit: contain;"
        class="q-mr-md"/>
      <div class="column">
        <div class="text-subtitle1 text-weight-bold">
          Distribuidora Ta Bien
        </div>
        <div class="text-caption text-blue-2">
          Sistema de Gestión Empresarial
        </div>
      </div>
    </q-toolbar-title>
    <div class="gt-xs text-caption q-mr-lg">
      <q-icon name="calendar_today" class="q-mr-xs" />
      {{ new Date().toLocaleDateString('es-SV') }}
    </div>
    <q-btn flat no-caps>
      <q-avatar size="34px" color="white" text-color="primary">
        <q-icon name="person" />
      </q-avatar>
      <div class="column items-start q-ml-sm gt-xs">
        <span class="text-weight-bold text-body2">Usuario</span>
        <span class="text-caption text-blue-2">Administrador</span>
      </div>
      <q-icon name="keyboard_arrow_down" class="q-ml-xs" />
      <q-menu>
        <q-list style="min-width: 180px">
          <q-item clickable v-close-popup>
            <q-item-section avatar>
              <q-icon name="account_circle" />
            </q-item-section>
            <q-item-section>Mi perfil</q-item-section>
          </q-item>
          <q-item clickable v-close-popup>
            <q-item-section avatar>
              <q-icon name="settings" />
            </q-item-section>
            <q-item-section>Configuración</q-item-section>
          </q-item>
          <q-separator />
          <q-item clickable v-close-popup>
            <q-item-section avatar>
              <q-icon name="logout" color="negative" />
            </q-item-section>
            <q-item-section class="text-negative">
              Cerrar sesión
            </q-item-section>
          </q-item>
        </q-list>
      </q-menu>
    </q-btn>
  </q-toolbar>
</q-header>

    <q-drawer show-if-above v-model="leftDrawerOpen" side="left" bordered :width="260">
  <q-scroll-area class="fit">
    <div class="q-pa-md text-center">
      <q-icon name="account_balance" size="48px" color="primary" />
      <div class="text-h6 text-weight-bold">
        Sistema Contable
      </div>
      <div class="text-caption text-grey-7">
        Administración y Finanzas
      </div>
    </div>
    <q-separator />
    <q-list padding>
      <q-item-label header class="text-weight-bold text-grey-8">
        GENERAL
      </q-item-label>
      <q-item clickable v-ripple to="/" exact>
        <q-item-section avatar>
          <q-icon name="dashboard" />
        </q-item-section>
        <q-item-section>
          Inicio
        </q-item-section>
      </q-item>
      <q-separator spaced />
      <q-item-label header class="text-weight-bold text-grey-8">
        OPERACIONES
      </q-item-label>
      <q-item clickable v-ripple to="/compra">
        <q-item-section avatar>
          <q-icon name="shopping_cart" />
        </q-item-section>
        <q-item-section>
          Compras
        </q-item-section>
      </q-item>
      <q-item clickable v-ripple to="/venta">
        <q-item-section avatar>
          <q-icon name="point_of_sale" />
        </q-item-section>
        <q-item-section>
          Ventas
        </q-item-section>
      </q-item>
      <q-separator spaced />
      <q-item-label header class="text-weight-bold text-grey-8">
        CONTROL
      </q-item-label>
      <q-item clickable v-ripple to="/almacen">
        <q-item-section avatar>
          <q-icon name="warehouse" />
        </q-item-section>
        <q-item-section>
          Almacenes
        </q-item-section>
      </q-item>
      <q-item clickable v-ripple to="/inventario">
        <q-item-section avatar>
          <q-icon name="inventory_2" />
        </q-item-section>
        <q-item-section>
          Inventario
        </q-item-section>
      </q-item>
      <q-item clickable v-ripple to="/personal">
        <q-item-section avatar>
          <q-icon name="groups" />
        </q-item-section>
        <q-item-section>
          Personal
        </q-item-section>
      </q-item>
      <q-separator spaced />
      <q-item-label header class="text-weight-bold text-grey-8">
        CONTABILIDAD
      </q-item-label>
      <q-item clickable v-ripple to="/libro-diario">
        <q-item-section avatar>
          <q-icon name="menu_book" />
        </q-item-section>
        <q-item-section>
          Libro Diario
        </q-item-section>
      </q-item>
      <q-item clickable v-ripple to="/hoja-de-trabajo">
        <q-item-section avatar>
          <q-icon name="table_chart" />
        </q-item-section>
        <q-item-section>
          Hoja de Trabajo
        </q-item-section>
      </q-item>
      <q-item clickable v-ripple to="/catalogo-cuentas">
        <q-item-section avatar>
          <q-icon name="account_tree" />
        </q-item-section>
        <q-item-section>
          Catálogo de Cuentas
        </q-item-section>
      </q-item>
    </q-list>
  </q-scroll-area>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>

  </q-layout>
</template>

<script setup>
import { ref } from 'vue'

const leftDrawerOpen = ref(false)
function toggleLeftDrawer () {
  leftDrawerOpen.value = !leftDrawerOpen.value
}
</script>
