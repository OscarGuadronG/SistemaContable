import { redondear } from '@/utils/formato'

export function repartir(total, bases) {
  const limpios = bases.map(base => Math.max(0, Number(base) || 0))
  const suma = limpios.reduce((acumulado, base) => acumulado + base, 0)
  const monto = Number(total) || 0
  if (limpios.length === 0) return []
  if (suma <= 0 || monto === 0) return limpios.map(() => 0)
  const partes = []
  let acumulado = 0
  limpios.forEach((base, indice) => {
    if (indice === limpios.length - 1) {
      partes.push(redondear(monto - acumulado))
      return
    }
    const parte = redondear((monto * base) / suma)
    partes.push(parte)
    acumulado = redondear(acumulado + parte)
  })
  return partes
}

export function totalesCompra(lineas, descuento, gastos, tasaIva) {
  const subtotal = redondear(
    lineas.reduce(
      (acumulado, linea) =>
        acumulado + Number(linea.cantidad || 0) * Number(linea.precio || 0),
      0
    )
  )
  const descuentoMonto = redondear(Math.min(Number(descuento) || 0, subtotal))
  const base = redondear(subtotal - descuentoMonto)
  const iva = redondear(base * (Number(tasaIva) || 0))
  const gastosMonto = redondear(Number(gastos) || 0)
  const total = redondear(base + iva + gastosMonto)
  const gastosLinea = repartir(
    gastosMonto,
    lineas.map(linea => Number(linea.cantidad || 0) * Number(linea.precio || 0))
  )
  return {
    subtotal,
    descuento: descuentoMonto,
    iva,
    gastos: gastosMonto,
    total,
    gastosLinea
  }
}

export function totalesVenta(lineas, descuento, tasaIva) {
  const subtotal = redondear(
    lineas.reduce(
      (acumulado, linea) =>
        acumulado + Number(linea.cantidad || 0) * Number(linea.precio || 0),
      0
    )
  )
  const descuentoMonto = redondear(
    Math.min(Math.max(Number(descuento) || 0, 0), subtotal)
  )
  const base = redondear(subtotal - descuentoMonto)
  const iva = redondear(base * (Number(tasaIva) || 0))
  const total = redondear(base + iva)
  return { subtotal, descuento: descuentoMonto, iva, total }
}

export function diasEntre(desde, hasta) {
  if (!desde || !hasta) return NaN
  const inicio = new Date(`${String(desde).slice(0, 10)}T00:00:00`)
  const fin = new Date(`${String(hasta).slice(0, 10)}T00:00:00`)
  return Math.round((fin.getTime() - inicio.getTime()) / 86400000)
}

export function simularInteres({
  fechaDocumento,
  fechaPago,
  capital,
  tasa,
  diasExoneracion = 30
}) {
  const dias = diasEntre(fechaDocumento, fechaPago)
  if (!Number.isFinite(dias) || dias < 0) {
    return { dias: 0, meses: 0, interes: 0, exonerado: false, invalido: true }
  }
  if (dias < Number(diasExoneracion)) {
    return { dias, meses: 0, interes: 0, exonerado: true, invalido: false }
  }
  const meses = Math.ceil(dias / 30)
  const interes = redondear(Number(capital || 0) * Number(tasa || 0) * meses)
  return { dias, meses, interes, exonerado: false, invalido: false }
}
