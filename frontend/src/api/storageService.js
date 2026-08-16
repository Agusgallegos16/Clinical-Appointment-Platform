import { supabase } from '../config/supabaseClient';

/**
 * Subir foto de perfil de un médico al bucket 'fotos-doctores' en Supabase Storage
 * @param {File} file - Archivo de imagen seleccionado
 * @param {string} [doctorId] - ID opcional del doctor para nombrar el archivo
 * @returns {Promise<string|null>} - Retorna la URL pública de la imagen o null si falla
 */
export const uploadDoctorAvatar = async (file, doctorId = 'temp') => {
  if (!file) {
    console.warn('⚠️ uploadDoctorAvatar: No se proporcionó ningún archivo.');
    return null;
  }

  const fileExt = file.name.split('.').pop() || 'jpg';
  const cleanFileName = `doctor_${doctorId}_${Date.now()}.${fileExt}`;
  const filePath = `avatars/${cleanFileName}`;

  console.log(`📤 Iniciando subida a Supabase Storage (Bucket: fotos-doctores, Ruta: ${filePath})...`);
  console.log('📄 Detalles del archivo:', { name: file.name, size: file.size, type: file.type });

  try {
    // 1. Subir la foto al bucket 'fotos-doctores'
    const { data, error } = await supabase.storage
      .from('fotos-doctores')
      .upload(filePath, file, {
        cacheControl: '3600',
        upsert: true,
      });

    if (error) {
      console.error('❌ Error devuelto por Supabase Storage:', error);
      console.error('💡 Verifica que el bucket "fotos-doctores" exista en Supabase, sea público y tenga políticas RLS para INSERT/SELECT.');
      return null;
    }

    console.log('✅ Archivo subido con éxito a Supabase. Path:', data.path);

    // 2. Obtener la URL pública resultante
    const { data: publicUrlData } = supabase.storage
      .from('fotos-doctores')
      .getPublicUrl(data.path);

    const publicUrl = publicUrlData?.publicUrl;
    console.log('🔗 URL pública obtenida de Supabase Storage:', publicUrl);
    return publicUrl;
  } catch (err) {
    console.error('💥 Excepción durante la subida a Supabase Storage:', err);
    return null;
  }
};
