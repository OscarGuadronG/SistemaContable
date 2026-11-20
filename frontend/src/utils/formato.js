const monedaSv = new Intl.NumberFormat('es-SV', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
})

const cantidadSv = new Intl.NumberFormat('es-SV', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 4
})

export function redondear(valor, decimales = 2) {
  const numero = Number(valor)
  if (!Number.isFinite(numero)) return 0
  const factor = 10 ** decimales
  return Math.round((numero + Number.EPSILON) * factor) / factor
}

export function moneda(valor) {
  const numero = Number(valor)
  if (!Number.isFinite(numero)) return '—'
  return `$ ${monedaSv.format(numero)}`
}

export function cantidad(valor) {
  const numero = Number(valor)
  if (!Number.isFinite(numero)) return '—'
  return cantidadSv.format(numero)
}

export function porcentaje(valor) {
  const numero = Number(valor)
  if (!Number.isFinite(numero)) return '—'
  return `${monedaSv.format(numero * 100)} %`
}

export function fecha(valor) {
  if (!valor) return '—'
  const texto = String(valor)
  const coincidencia = texto.match(/^(\d{4})-(\d{2})-(\d{2})/)
  if (!coincidencia) return '—'
  return `${coincidencia[3]}/${coincidencia[2]}/${coincidencia[1]}`
}

export function fechaHora(valor) {
  if (!valor) return '—'
  const dia = fecha(valor)
  const hora = String(valor).match(/T(\d{2}:\d{2})/)
  if (dia === '—') return '—'
  return hora ? `${dia} ${hora[1]}` : dia
}

export function hoy() {
  const fechaActual = new Date()
  const mes = String(fechaActual.getMonth() + 1).padStart(2, '0')
  const dia = String(fechaActual.getDate()).padStart(2, '0')
  return `${fechaActual.getFullYear()}-${mes}-${dia}`
}

export function sumarDias(iso, dias) {
  const base = new Date(`${String(iso).slice(0, 10)}T00:00:00`)
  base.setDate(base.getDate() + dias)
  const mes = String(base.getMonth() + 1).padStart(2, '0')
  const dia = String(base.getDate()).padStart(2, '0')
  return `${base.getFullYear()}-${mes}-${dia}`
}

export function aInstante(valor) {
  if (!valor) return null
  const texto = String(valor)
  if (texto.includes('T')) return texto
  return `${texto}T00:00:00-06:00`
}

export function textoPersona(valor) {
  if (valor == null || valor === '') return '—'
  if (typeof valor === 'string' || typeof valor === 'number')
    return String(valor)
  return (
    valor.nombreCompleto ||
    [valor.nombre, valor.apellido].filter(Boolean).join(' ') ||
    valor.descripcion ||
    '—'
  )
}

export function textoRelacion(fila, ...claves) {
  for (const clave of claves) {
    const valor = fila?.[clave]
    if (valor == null || valor === '') continue
    const texto = textoPersona(valor)
    if (texto !== '—') return texto
  }
  return '—'
}

export function leer(objeto, ...claves) {
  if (!objeto) return undefined
  for (const clave of claves) {
    const valor = objeto[clave]
    if (valor !== undefined && valor !== null && valor !== '') return valor
  }
  return undefined
}

export const FORMAS_PAGO = [
  { value: 'Contado', label: 'Contado' },
  { value: 'Credito', label: 'Crédito' },
  { value: 'Cheque', label: 'Cheque' }
]

export const FORMAS_PAGO_INMEDIATO = [
  { value: 'Contado', label: 'Contado' },
  { value: 'Cheque', label: 'Cheque' }
]

export function etiquetaFormaPago(valor) {
  return FORMAS_PAGO.find(item => item.value === valor)?.label || valor || '—'
}

export function requerido(mensaje) {
  return valor =>
    (valor !== null && valor !== undefined && String(valor).trim() !== '') ||
    mensaje
}

export function numeroPositivo(mensaje, { cero = false } = {}) {
  return valor => {
    const numero = Number(valor)
    if (!Number.isFinite(numero)) return mensaje
    if (cero ? numero < 0 : numero <= 0) return mensaje
    return true
  }
}
