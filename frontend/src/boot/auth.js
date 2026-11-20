import { defineBoot } from '#q-app'
import { fijarManejador401 } from '@/services/api'
import { useSesionStore } from '@/stores/sesion'
import { rutaPermitida } from '@/config/permisos'

export default defineBoot(async ({ router }) => {
  const sesion = useSesionStore()

  fijarManejador401(async () => {
    await sesion.cerrar()
    const actual = router.currentRoute.value
    if (actual.path !== '/login') {
      await router.replace({
        path: '/login',
        query: { redirect: actual.fullPath }
      })
    }
  })

  await sesion.restaurar()

  router.beforeEach(to => {
    if (!sesion.listo) return true
    if (to.meta.publica) {
      if (to.path === '/login' && sesion.autenticado) return '/'
      return true
    }
    if (!sesion.autenticado) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    if (!rutaPermitida(sesion.rol, to.meta)) {
      return { path: '/prohibido' }
    }
    return true
  })
})
