import { ref } from 'vue'
import { api, comoLista } from '@/services/api'

function limpiarQuery(query) {
  const salida = {}
  Object.entries(query || {}).forEach(([clave, valor]) => {
    if (valor === undefined || valor === null || valor === '') return
    salida[clave] = valor
  })
  return salida
}

export function useListado(ruta, filtros = () => ({})) {
  const filas = ref([])
  const cargando = ref(false)
  const error = ref('')
  const paginacion = ref({
    page: 1,
    rowsPerPage: 20,
    rowsNumber: 0,
    sortBy: null,
    descending: false
  })

  async function cargar(props) {
    const pag = props?.pagination || paginacion.value
    const max = pag.rowsPerPage > 0 ? pag.rowsPerPage : 20
    const page = pag.page > 0 ? pag.page : 1
    cargando.value = true
    error.value = ''
    try {
      const { data, total } = await api.get(ruta, {
        query: {
          first: (page - 1) * max,
          max,
          ...limpiarQuery(filtros())
        }
      })
      const lista = comoLista(data)
      filas.value = lista
      paginacion.value = {
        ...pag,
        page,
        rowsPerPage: max,
        rowsNumber: total ?? lista.length
      }
    } catch (causa) {
      filas.value = []
      error.value = causa.mensaje || 'No se pudo cargar la información.'
    } finally {
      cargando.value = false
    }
  }

  function reiniciar() {
    paginacion.value = { ...paginacion.value, page: 1 }
    return cargar()
  }

  return { filas, cargando, error, paginacion, cargar, reiniciar }
}
