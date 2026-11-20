import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { supabase } from '@/boot/supabase'
import { api } from '@/services/api'

export const useSesionStore = defineStore('sesion', () => {
  const access = ref(null)
  const perfil = ref(null)
  const listo = ref(false)
  const mensaje = ref('')
  const avisoBackend = ref('')
  let suscripcion = null

  const autenticado = computed(() => Boolean(access.value))
  const rol = computed(
    () => perfil.value?.rol || access.value?.user?.app_metadata?.rol || null
  )
  const nombre = computed(
    () =>
      perfil.value?.nombre ||
      access.value?.user?.user_metadata?.nombre ||
      access.value?.user?.email ||
      ''
  )
  const email = computed(
    () => perfil.value?.email || access.value?.user?.email || ''
  )
  const idEmpleado = computed(() => {
    if (
      perfil.value &&
      Object.prototype.hasOwnProperty.call(perfil.value, 'idEmpleado')
    ) {
      return perfil.value.idEmpleado
    }
    return null
  })
  const empleado = computed(() => perfil.value?.empleado || null)

  function escuchar() {
    if (suscripcion) return
    const { data } = supabase.auth.onAuthStateChange((evento, nueva) => {
      if (evento === 'SIGNED_OUT') {
        access.value = null
        perfil.value = null
        return
      }
      if (evento === 'TOKEN_REFRESHED' || evento === 'SIGNED_IN') {
        access.value = nueva
      }
    })
    suscripcion = data.subscription
  }

  async function cerrar() {
    try {
      await supabase.auth.signOut()
    } catch {
      // Se descarta la sesión local aunque el servidor de auth no responda.
    }
    access.value = null
    perfil.value = null
    avisoBackend.value = ''
  }

  async function cargarPerfil() {
    avisoBackend.value = ''
    try {
      const { data } = await api.get('/me', { silencioso: true })
      perfil.value = data
      mensaje.value = ''
    } catch (error) {
      if (error.estado === 401 || error.estado === 403) {
        mensaje.value =
          error.mensaje ||
          'Su usuario no tiene un perfil activo. Contacte al administrador.'
        await cerrar()
        return
      }
      perfil.value = null
      avisoBackend.value =
        'No se pudo confirmar el perfil con el servidor. El menú usa el rol guardado en la sesión.'
    }
  }

  async function restaurar() {
    escuchar()
    const { data } = await supabase.auth.getSession()
    access.value = data.session
    if (data.session) await cargarPerfil()
    listo.value = true
  }

  async function entrar(emailIngresado, password) {
    mensaje.value = ''
    const { data, error } = await supabase.auth.signInWithPassword({
      email: emailIngresado.trim(),
      password
    })
    if (error) throw error
    access.value = data.session
    await cargarPerfil()
  }

  return {
    access,
    perfil,
    listo,
    mensaje,
    avisoBackend,
    autenticado,
    rol,
    nombre,
    email,
    idEmpleado,
    empleado,
    restaurar,
    entrar,
    cerrar,
    cargarPerfil
  }
})
