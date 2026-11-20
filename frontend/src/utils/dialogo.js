import { Dialog, Notify } from 'quasar'

export function avisarExito(mensaje) {
  Notify.create({ type: 'positive', message: mensaje, timeout: 3000 })
}

export function confirmar({
  titulo,
  mensaje,
  ok = 'Confirmar',
  color = 'primary'
}) {
  return new Promise(resolve => {
    Dialog.create({
      title: titulo,
      message: mensaje,
      cancel: { label: 'Cancelar', flat: true, color: 'grey-8', noCaps: true },
      ok: { label: ok, color, unelevated: true, noCaps: true },
      persistent: true
    })
      .onOk(() => resolve(true))
      .onCancel(() => resolve(false))
  })
}

export function imprimir({ dialogo = false, horizontal = false } = {}) {
  document.body.classList.toggle('print-dialogo', dialogo)
  document.body.classList.toggle('print-horizontal', horizontal)
  const limpiar = () => {
    document.body.classList.remove('print-dialogo', 'print-horizontal')
    window.removeEventListener('afterprint', limpiar)
  }
  window.addEventListener('afterprint', limpiar)
  window.print()
}

export function mensajeAuth(error) {
  const codigo = error?.code || ''
  const texto = error?.message || ''
  const mapa = {
    invalid_credentials: 'Correo o contraseña incorrectos.',
    email_not_confirmed: 'El correo todavía no está confirmado.',
    over_request_rate_limit:
      'Demasiados intentos. Espere un momento e intente de nuevo.',
    user_banned: 'Este usuario está bloqueado. Contacte al administrador.',
    'Invalid login credentials': 'Correo o contraseña incorrectos.'
  }
  if (mapa[codigo]) return mapa[codigo]
  if (mapa[texto]) return mapa[texto]
  if (/fetch|network|Failed to fetch/i.test(texto)) {
    return 'No hay conexión con el servicio de autenticación.'
  }
  return 'No se pudo iniciar sesión. Verifique sus datos e intente de nuevo.'
}

export function destinoSeguro(valor) {
  if (typeof valor !== 'string') return '/'
  if (!valor.startsWith('/') || valor.startsWith('//')) return '/'
  return valor
}
