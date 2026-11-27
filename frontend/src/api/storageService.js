import axiosClient from './axiosClient';
import { supabase } from '../config/supabaseClient';

const ALLOWED_RASTER_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;

export const isAllowedRasterImage = (file) => {
  if (!file) return false;
  const lowerName = (file.name || '').toLowerCase();
  if (lowerName.endsWith('.svg') || lowerName.endsWith('.svgz')) {
    return false;
  }
  return ALLOWED_RASTER_MIME_TYPES.includes(file.type);
};

/**
 * Upload a doctor's profile avatar through the authenticated Spring Boot storage proxy.
 * @param {File} file - Selected raster image file (JPEG, PNG, or WEBP)
 * @param {string} [doctorId] - Optional doctor ID hint (used for admin uploads)
 * @returns {Promise<string|null>} - Public URL of the uploaded image or null on failure
 */
export const uploadDoctorAvatar = async (file, doctorId = 'temp') => {
  if (!file) {
    console.warn('uploadDoctorAvatar: No file provided.');
    return null;
  }

  if (!isAllowedRasterImage(file)) {
    console.error('uploadDoctorAvatar: Unsupported image format. Only JPG, PNG, and WEBP are allowed.');
    return null;
  }

  if (file.size > MAX_IMAGE_SIZE_BYTES) {
    console.error('uploadDoctorAvatar: Image exceeds 5 MB limit.');
    return null;
  }

  const formData = new FormData();
  formData.append('file', file);
  if (doctorId) {
    formData.append('doctorId', String(doctorId));
  }

  try {
    const response = await axiosClient.post('/storage/doctores/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data?.url || null;
  } catch (err) {
    console.error('Error uploading doctor avatar through backend proxy:', err);
    return null;
  }
};

/**
 * List public images of the institution gallery from Supabase Storage (read-only).
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
      console.warn('Could not list images from "fotos-institucion" in Supabase:', error);
      return [];
    }

    const validFiles = data.filter((item) => item.name && !item.name.startsWith('.'));

    return validFiles.map((file) => {
      const { data: publicUrlData } = supabase.storage
        .from('fotos-institucion')
        .getPublicUrl(file.name);

      return {
        name: file.name,
        path: file.name,
        url: publicUrlData?.publicUrl || '',
      };
    });
  } catch (err) {
    console.error('Error fetching institution gallery:', err);
    return [];
  }
};

/**
 * Upload a new institution gallery image through the authenticated ADMIN backend endpoint.
 * @param {File} file - Selected raster image file (JPEG, PNG, or WEBP)
 * @returns {Promise<{success: boolean, url?: string, errorMsg?: string}>}
 */
export const uploadInstitucionImage = async (file) => {
  if (!file) return { success: false, errorMsg: 'No se seleccionó ningún archivo de imagen.' };

  if (!isAllowedRasterImage(file)) {
    return {
      success: false,
      errorMsg: 'Formato no permitido. Solo se aceptan imágenes JPG, PNG o WEBP (SVG no está permitido).',
    };
  }

  if (file.size > MAX_IMAGE_SIZE_BYTES) {
    return {
      success: false,
      errorMsg: 'La imagen supera el tamaño máximo permitido de 5 MB.',
    };
  }

  const formData = new FormData();
  formData.append('file', file);

  try {
    const response = await axiosClient.post('/storage/institucion', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return { success: true, url: response.data?.url || null };
  } catch (err) {
    console.error('Error uploading institution gallery image:', err);
    const serverMsg =
      err.response?.data?.mensaje ||
      err.response?.data?.message ||
      'Error al subir la imagen al servidor de almacenamiento.';
    return { success: false, errorMsg: serverMsg };
  }
};

/**
 * Delete an institution gallery image through the authenticated ADMIN backend endpoint.
 * @param {string} fileName - Object name inside the 'fotos-institucion' bucket
 * @returns {Promise<boolean>}
 */
export const deleteInstitucionImage = async (fileName) => {
  if (!fileName) return false;

  try {
    await axiosClient.delete(`/storage/institucion/${encodeURIComponent(fileName)}`);
    return true;
  } catch (err) {
    console.error('Error deleting institution gallery image:', err);
    return false;
  }
};
