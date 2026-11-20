// Matriz §2 del contrato. Administrador siempre puede.
// lectura: puede abrir la pantalla. escritura: puede crear o modificar.

export const ROLES = [
  'Ventas',
  'Bodega',
  'Contabilidad',
  'Administrador',
  'RRHH'
]

export const PERMISOS = {
  inventario: {
    lectura: ['Ventas', 'Bodega', 'Contabilidad', 'RRHH'],
    escritura: ['Bodega']
  },
  ajustes: {
    lectura: [],
    escritura: ['Bodega']
  },
  almacenes: {
    lectura: ['Ventas', 'Bodega', 'Contabilidad'],
    escritura: []
  },
  traslados: {
    lectura: ['Bodega', 'Contabilidad'],
    escritura: ['Bodega']
  },
  compras: {
    lectura: ['Bodega', 'Contabilidad'],
    escritura: ['Bodega']
  },
  devolucionesCompra: {
    lectura: ['Bodega', 'Contabilidad'],
    escritura: ['Bodega']
  },
  proveedores: {
    lectura: ['Bodega', 'Contabilidad'],
    escritura: ['Contabilidad']
  },
  ventas: {
    lectura: ['Ventas', 'Bodega', 'Contabilidad'],
    escritura: ['Ventas']
  },
  libroVentas: {
    lectura: ['Ventas', 'Contabilidad'],
    escritura: []
  },
  devolucionesVenta: {
    lectura: ['Ventas', 'Bodega', 'Contabilidad'],
    escritura: ['Ventas', 'Bodega']
  },
  clientes: {
    lectura: ['Ventas', 'Contabilidad'],
    escritura: ['Ventas']
  },
  creditosVenta: {
    lectura: ['Ventas', 'Contabilidad'],
    escritura: ['Ventas', 'Contabilidad']
  },
  creditosCompra: {
    lectura: ['Contabilidad'],
    escritura: ['Contabilidad']
  },
  personal: {
    lectura: ['Contabilidad', 'RRHH'],
    escritura: ['RRHH']
  },
  partidas: {
    lectura: ['Contabilidad'],
    escritura: ['Contabilidad']
  },
  cuentas: {
    lectura: ['Contabilidad'],
    escritura: ['Contabilidad']
  },
  periodos: {
    lectura: ['Contabilidad'],
    escritura: ['Contabilidad']
  },
  parametros: {
    lectura: ['Ventas', 'Bodega', 'Contabilidad', 'RRHH'],
    escritura: ['Contabilidad']
  },
  usuarios: {
    lectura: [],
    escritura: [],
    soloAdmin: true
  }
}

export const MENU = [
  {
    grupo: 'General',
    items: [{ titulo: 'Inicio', icono: 'home', to: '/', exacto: true }]
  },
  {
    grupo: 'Operaciones',
    items: [
      {
        titulo: 'Inventario',
        icono: 'inventory_2',
        to: '/inventario',
        permiso: 'inventario'
      },
      {
        titulo: 'Compras',
        icono: 'shopping_cart',
        to: '/compras',
        permiso: 'compras'
      },
      {
        titulo: 'Ventas',
        icono: 'point_of_sale',
        to: '/ventas',
        permiso: 'ventas'
      },
      {
        titulo: 'Créditos',
        icono: 'account_balance_wallet',
        to: '/creditos',
        cualquiera: ['creditosVenta', 'creditosCompra']
      }
    ]
  },
  {
    grupo: 'Control',
    items: [
      {
        titulo: 'Almacenes',
        icono: 'warehouse',
        to: '/almacenes',
        cualquiera: ['almacenes', 'traslados']
      },
      {
        titulo: 'Personal',
        icono: 'groups',
        to: '/personal',
        permiso: 'personal'
      }
    ]
  },
  {
    grupo: 'Contabilidad',
    items: [
      {
        titulo: 'Libro diario',
        icono: 'menu_book',
        to: '/libro-diario',
        permiso: 'partidas'
      },
      {
        titulo: 'Hoja de trabajo',
        icono: 'table_chart',
        to: '/hoja-de-trabajo',
        permiso: 'periodos'
      },
      {
        titulo: 'Catálogo de cuentas',
        icono: 'account_tree',
        to: '/catalogo-cuentas',
        permiso: 'cuentas'
      }
    ]
  },
  {
    grupo: 'Administración',
    items: [
      {
        titulo: 'Usuarios',
        icono: 'manage_accounts',
        to: '/usuarios',
        permiso: 'usuarios'
      },
      {
        titulo: 'Parámetros',
        icono: 'tune',
        to: '/parametros',
        permiso: 'parametros'
      }
    ]
  }
]

export function puede(rol, recurso, accion = 'lectura') {
  if (!rol || !recurso) return false
  if (rol === 'Administrador') return true
  const permiso = PERMISOS[recurso]
  if (!permiso || permiso.soloAdmin) return false
  if (accion === 'escritura') return permiso.escritura.includes(rol)
  return permiso.lectura.includes(rol) || permiso.escritura.includes(rol)
}

export function itemVisible(rol, item) {
  if (!item.permiso && !item.cualquiera) return true
  if (item.permiso) return puede(rol, item.permiso)
  return item.cualquiera.some(recurso => puede(rol, recurso))
}

export function menuDe(rol) {
  return MENU.map(grupo => ({
    ...grupo,
    items: grupo.items.filter(item => itemVisible(rol, item))
  })).filter(grupo => grupo.items.length > 0)
}

export function rutaPermitida(rol, meta = {}) {
  if (meta.permiso && !puede(rol, meta.permiso)) return false
  if (
    meta.cualquiera &&
    !meta.cualquiera.some(recurso => puede(rol, recurso))
  ) {
    return false
  }
  return true
}

export const COLOR_ROL = {
  Administrador: 'primary',
  Contabilidad: 'teal',
  Bodega: 'deep-orange',
  Ventas: 'indigo',
  RRHH: 'purple'
}
