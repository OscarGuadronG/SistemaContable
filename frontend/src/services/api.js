import { Notify } from 'quasar'
import { supabase } from '@/boot/supabase'

export class ErrorApi extends Error {
  constructor({ mensaje, codigo, violaciones, estado }) {
    super(mensaje || 'Error')
    this.name = 'ErrorApi'
    this.mensaje = mensaje || 'Ocurrió un error inesperado'
    this.codigo = codigo || ''
    this.violaciones = Array.isArray(violaciones) ? violaciones : []
    this.estado = estado || 0
  }
}

let alPerderSesion = async () => {}

export function fijarManejador401(fn) {
  alPerderSesion = fn
}

let avisosLentos = 0
let cerrarAviso = null

function vigilarDemora() {
  let contado = false
  const id = setTimeout(() => {
    contado = true
    avisosLentos += 1
    if (avisosLentos === 1) {
      cerrarAviso = Notify.create({
        group: 'despertar',
        type: 'ongoing',
        message: 'Despertando el servidor…',
        caption: 'La primera llamada puede tardar cerca de un minuto.',
        spinner: true,
        timeout: 0,
        position: 'bottom'
      })
    }
  }, 5000)
  return () => {
    clearTimeout(id)
    if (!contado) return
    avisosLentos = Math.max(0, avisosLentos - 1)
    if (avisosLentos === 0 && cerrarAviso) {
      cerrarAviso()
      cerrarAviso = null
    }
  }
}

function notificarError(error) {
  const detalle = error.violaciones
    .map(item => [item.campo, item.mensaje].filter(Boolean).join(': '))
    .filter(Boolean)
    .join('\n')
  Notify.create({
    type: 'negative',
    message: error.mensaje,
    caption: detalle || undefined,
    multiLine: Boolean(detalle),
    timeout: detalle ? 9000 : 5000,
    actions: [{ icon: 'close', color: 'white', round: true }]
  })
}

export function comoLista(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.filas)) return data.filas
  return []
}

function urlDe(path, query) {
  const base = String(
    import.meta.env.API_URL || 'http://localhost:9080/api'
  ).replace(/\/$/, '')
  const ruta = path.startsWith('/') ? path : `/${path}`
  const url = new URL(`${base}${ruta}`)
  Object.entries(query || {}).forEach(([clave, valor]) => {
    if (valor === undefined || valor === null || valor === '') return
    url.searchParams.set(clave, String(valor))
  })
  return url
}

async function leerCuerpo(respuesta) {
  const texto = await respuesta.text()
  if (!texto) return null
  try {
    return JSON.parse(texto)
  } catch {
    return { mensaje: texto.slice(0, 240) }
  }
}

export async function solicitar(path, opciones = {}) {
  const terminar = vigilarDemora()
  try {
    const { data: sesionAuth } = await supabase.auth.getSession()
    const token = sesionAuth.session?.access_token
    const headers = { Accept: 'application/json' }
    if (token) headers.Authorization = `Bearer ${token}`
    let cuerpo
    if (opciones.body !== undefined) {
      headers['Content-Type'] = 'application/json'
      cuerpo = JSON.stringify(opciones.body)
    }
    let respuesta
    try {
      respuesta = await fetch(urlDe(path, opciones.query), {
        method: opciones.method || 'GET',
        headers,
        body: cuerpo
      })
    } catch {
      const error = new ErrorApi({
        mensaje:
          'No se pudo conectar con el servidor. Si está en reposo, espere un momento e intente de nuevo.',
        codigo: 'RED',
        estado: 0
      })
      if (!opciones.silencioso) notificarError(error)
      throw error
    }

    const totalCrudo = respuesta.headers.get('Total-Records')
    const total = totalCrudo == null ? null : Number(totalCrudo)

    if (respuesta.status === 401) {
      const cuerpoError = await leerCuerpo(respuesta)
      const error = new ErrorApi({
        mensaje:
          cuerpoError?.mensaje || 'La sesión expiró. Inicie sesión de nuevo.',
        codigo: cuerpoError?.codigo || 'NO_AUTENTICADO',
        violaciones: cuerpoError?.violaciones,
        estado: 401
      })
      await alPerderSesion()
      if (!opciones.silencioso) notificarError(error)
      throw error
    }

    if (!respuesta.ok) {
      const cuerpoError = await leerCuerpo(respuesta)
      const error = new ErrorApi({
        mensaje:
          cuerpoError?.mensaje ||
          `No se pudo completar la operación (${respuesta.status}).`,
        codigo: cuerpoError?.codigo || '',
        violaciones: cuerpoError?.violaciones,
        estado: respuesta.status
      })
      if (!opciones.silencioso) notificarError(error)
      throw error
    }

    const data = respuesta.status === 204 ? null : await leerCuerpo(respuesta)
    return { data, total: Number.isFinite(total) ? total : null }
  } finally {
    terminar()
  }
}

export const api = {
  get: (path, opciones = {}) => solicitar(path, { ...opciones, method: 'GET' }),
  post: (path, body, opciones = {}) =>
    solicitar(path, { ...opciones, method: 'POST', body }),
  put: (path, body, opciones = {}) =>
    solicitar(path, { ...opciones, method: 'PUT', body }),
  eliminar: (path, opciones = {}) =>
    solicitar(path, { ...opciones, method: 'DELETE' })
}

export async function listarTodo(ruta, query = {}) {
  const tamano = 100
  const todos = []
  let first = 0
  let total = Infinity
  while (todos.length < total && first < 2000) {
    const { data, total: reporte } = await api.get(ruta, {
      query: { ...query, first, max: tamano }
    })
    const filas = comoLista(data)
    if (reporte != null) total = reporte
    todos.push(...filas)
    if (filas.length < tamano) break
    first += tamano
    if (reporte == null && filas.length === 0) break
  }
  return todos
}
