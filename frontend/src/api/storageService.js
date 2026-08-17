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

  try {
    const { data, error } = await supabase.storage
      .from('fotos-doctores')
      .upload(filePath, file, {
        cacheControl: '3600',
        upsert: true,
      });

    if (error) {
      console.error('❌ Error devuelto por Supabase Storage:', error);
      return null;
    }

    const { data: publicUrlData } = supabase.storage
      .from('fotos-doctores')
      .getPublicUrl(data.path);

    return publicUrlData?.publicUrl || null;
  } catch (err) {
    console.error('💥 Excepción durante la subida a Supabase Storage:', err);
    return null;
  }
};

/**
 * Obtener la lista de imágenes públicas de la galería del Instituto desde Supabase Storage (Bucket: 'fotos-institucion')
 * @returns {Promise<Array<{name: string, url: string, path: string}>>}
 */
export const getInstitucionGallery = async () => {
  try {
    const { data, error } = await supabase.storage
      .from('fotos-institucion')
      .list('', {
        limit: 100,
        sortBy: { column: 'created_at', order: 'desc' },
      });

    if (error || !data) {
      console.warn('⚠️ No se pudo listar imágenes de "fotos-institucion" en Supabase:', error);
      return [];
    }

    // Filtrar archivos válidos (que no sean carpetas o nulos)
    const validFiles = data.filter((item) => item.name && !item.name.startsWith('.'));

    const galleryList = validFiles.map((file) => {
      const { data: publicUrlData } = supabase.storage
        .from('fotos-institucion')
        .getPublicUrl(file.name);

      return {
        name: file.name,
        path: file.name,
        url: publicUrlData?.publicUrl || '',
      };
    });

    return galleryList;
  } catch (err) {
    console.error('💥 Error al obtener galería de Supabase Storage:', err);
    return [];
  }
};

/**
 * Subir una nueva foto para la galería de la institución (Admin)
 * @param {File} file - Archivo de imagen seleccionado
 * @returns {Promise<{success: boolean, url?: string, errorMsg?: string}>}
 */
export const uploadInstitucionImage = async (file) => {
  if (!file) return { success: false, errorMsg: 'No se seleccionó ningún archivo de imagen.' };

  const fileExt = file.name.split('.').pop() || 'jpg';
  const fileName = `galeria_${Date.now()}_${Math.random().toString(36).substring(2, 7)}.${fileExt}`;

  try {
    const { data, error } = await supabase.storage
      .from('fotos-institucion')
      .upload(fileName, file, {
        cacheControl: '3600',
        upsert: true,
      });

    if (error) {
      console.error('❌ Error al subir imagen de galería:', error);
      const isBucketError = error.message?.toLowerCase().includes('bucket') || error.statusCode === '404' || error.error === 'Bucket not found';
      const userFriendlyMsg = isBucketError
        ? 'El bucket "fotos-institucion" no está configurado en Supabase Storage. Verificá que exista y tenga políticas públicas.'
        : (error.message || 'Error en el servidor de almacenamiento Supabase.');
      
      return { success: false, errorMsg: userFriendlyMsg };
    }

    const { data: publicUrlData } = supabase.storage
      .from('fotos-institucion')
      .getPublicUrl(data.path);

    return { success: true, url: publicUrlData?.publicUrl || null };
  } catch (err) {
    console.error('💥 Excepción al subir imagen de galería a Supabase:', err);
    return { success: false, errorMsg: 'Ocurrió una falla de conexión al intentar subir la imagen.' };
  }
};

/**
 * Eliminar una foto de la galería de la institución (Admin)
 * @param {string} fileName - Nombre o ruta del archivo en el bucket 'fotos-institucion'
 * @returns {Promise<boolean>}
 */
export const deleteInstitucionImage = async (fileName) => {
  if (!fileName) return false;

  try {
    const { error } = await supabase.storage
      .from('fotos-institucion')
      .remove([fileName]);

    if (error) {
      console.error('❌ Error al eliminar imagen de galería en Supabase:', error);
      return false;
    }

    return true;
  } catch (err) {
    console.error('💥 Excepción al eliminar imagen de galería:', err);
    return false;
  }
};
