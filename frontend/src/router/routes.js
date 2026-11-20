const routes = [
  {
    path: '/login',
    component: () => import('@/pages/Login.vue'),
    meta: { publica: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('@/pages/Inicio.vue') },
      {
        path: 'inventario',
        component: () => import('@/pages/Inventario.vue'),
        meta: { permiso: 'inventario' }
      },
      {
        path: 'compras',
        component: () => import('@/pages/Compras.vue'),
        meta: { permiso: 'compras' }
      },
      {
        path: 'ventas',
        component: () => import('@/pages/Ventas.vue'),
        meta: { permiso: 'ventas' }
      },
      {
        path: 'creditos',
        component: () => import('@/pages/Creditos.vue'),
        meta: { cualquiera: ['creditosVenta', 'creditosCompra'] }
      },
      {
        path: 'almacenes',
        component: () => import('@/pages/Almacenes.vue'),
        meta: { cualquiera: ['almacenes', 'traslados'] }
      },
      {
        path: 'personal',
        component: () => import('@/pages/Personal.vue'),
        meta: { permiso: 'personal' }
      },
      {
        path: 'libro-diario',
        component: () => import('@/pages/LibroDiario.vue'),
        meta: { permiso: 'partidas' }
      },
      {
        path: 'hoja-de-trabajo',
        component: () => import('@/pages/HojaTrabajo.vue'),
        meta: { permiso: 'periodos' }
      },
      {
        path: 'catalogo-cuentas',
        component: () => import('@/pages/CatalogoCuentas.vue'),
        meta: { permiso: 'cuentas' }
      },
      {
        path: 'usuarios',
        component: () => import('@/pages/Usuarios.vue'),
        meta: { permiso: 'usuarios' }
      },
      {
        path: 'parametros',
        component: () => import('@/pages/Parametros.vue'),
        meta: { permiso: 'parametros' }
      },
      {
        path: 'prohibido',
        component: () => import('@/pages/Prohibido.vue')
      },
      { path: 'compra', redirect: '/compras' },
      { path: 'venta', redirect: '/ventas' },
      { path: 'almacen', redirect: '/almacenes' }
    ]
  },
  {
    path: '/:catchAll(.*)*',
    component: () => import('@/pages/ErrorNotFound.vue'),
    meta: { publica: true }
  }
]

export default routes
