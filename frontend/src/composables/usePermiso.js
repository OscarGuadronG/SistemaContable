import { storeToRefs } from 'pinia'
import { useSesionStore } from '@/stores/sesion'
import { puede } from '@/config/permisos'

export function usePermiso() {
  const sesion = useSesionStore()
  const { rol, nombre, idEmpleado, email, empleado } = storeToRefs(sesion)

  function permitido(recurso, accion = 'lectura') {
    return puede(rol.value, recurso, accion)
  }

  return { permitido, rol, nombre, idEmpleado, email, empleado, sesion }
}
