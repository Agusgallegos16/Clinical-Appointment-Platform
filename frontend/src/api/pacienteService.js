import axiosClient from './axiosClient';

export const pacienteService = {
  obtenerPorId: async (id, options = {}) => {
    const response = await axiosClient.get(`/pacientes/${id}`, options);
    return response.data;
  },

  obtenerEstadisticas: async (id) => {
    const response = await axiosClient.get(`/pacientes/${id}/estadisticas`);
    return response.data;
  },

  obtenerMiPerfil: async () => {
    const response = await axiosClient.get('/pacientes/mi-perfil');
    return response.data;
  },

  actualizarMiPerfil: async (data) => {
    const response = await axiosClient.put('/pacientes/mi-perfil', data);
    return response.data;
  },

  buscarPorDni: async (dni) => {
    try {
      const response = await axiosClient.get(`/pacientes/buscar-por-dni/${dni}`);
      return response.data;
    } catch (error) {
      if (error.response && error.response.status === 404) {
        return null;
      }
      throw error;
    }
  },

  obtenerEstadoCompletitud: async (options = {}) => {
    const response = await axiosClient.get('/pacientes/mi-perfil/estado-completitud', options);
    return response.data;
  },

  completarPerfil: async (data, options = {}) => {
    const response = await axiosClient.patch('/pacientes/mi-perfil/completar', data, options);
    return response.data;
  },
};
