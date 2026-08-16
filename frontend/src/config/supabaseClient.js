import { createClient } from '@supabase/supabase-js';

/**
 * Recordatorio de Configuración de Entorno:
 * Asegúrate de agregar las siguientes variables en tu archivo `..env.local` en el frontend:
 * 
 * VITE_SUPABASE_URL=https://<TU-PROYECTO-ID>.supabase.co
 * VITE_SUPABASE_ANON_KEY=<TU-ANON-KEY-PUBLICA>
 */

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || 'https://placeholder.supabase.co';
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || 'placeholder-anon-key';

export const supabase = createClient(supabaseUrl, supabaseAnonKey);
