import { api } from '@/services/api'

let cache = null

export function invalidarParametros() {
  cache = null
}

export async function mapaParametros(forzar = false) {
  if (cache && !forzar) return cache
  const { data } = await api.get('/parametros')
  const lista = Array.isArray(data) ? data : []
  cache = Object.fromEntries(lista.map(item => [item.clave, item]))
  return cache
}

export function numeroParametro(mapa, clave, defecto) {
  const valor = Number(mapa?.[clave]?.valor)
  return Number.isFinite(valor) ? valor : defecto
}
