const routes = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('@/pages/Inicio.vue') },
      { path: 'compra', component: () => import('@/pages/Compras.vue') },
      { path: 'venta', component: () => import('@/pages/Ventas.vue')},
      { path: 'almacen', component: () => import ('@/pages/Almacenes.vue')},
      { path: 'inventario', component: () => import ('@/pages/Inventario.vue')},
      { path: 'personal', component: () => import ('@/pages/Personal.vue')},
      { path: 'libro-diario', component: () => import ('@/pages/LibroDiario.vue')},
      { path: 'hoja-de-trabajo', component: () => import ('@/pages/HojaTrabajo.vue')},
      { path: 'catalogo-cuentas', component: () => import ('@/pages/CatalogoCuentas.vue')},
    ]
  },
  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('@/pages/ErrorNotFound.vue')
  }
]

export default routes
