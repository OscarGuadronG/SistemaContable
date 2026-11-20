import { createClient } from '@supabase/supabase-js'
import { defineBoot } from '#q-app'

const url =
  import.meta.env.SUPABASE_URL || 'https://xyukidsdmihmsmlpddqz.supabase.co'
const clave =
  import.meta.env.SUPABASE_PUBLISHABLE_KEY ||
  'sb_publishable_sReom9tHSFYaC22i9kURHw_pi-I_JM6'

export const supabase = createClient(url, clave, {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: false
  }
})

export default defineBoot(() => {
  // El cliente se crea al importar el módulo. El boot solo lo registra.
})
