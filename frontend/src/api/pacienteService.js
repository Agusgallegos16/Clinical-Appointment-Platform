import axiosClient from './axiosClient';

export const pacienteService = {
  obtenerPorId: async (id) => {
    const response = await axiosClient.get(`/pacientes/${id}`);
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
};
