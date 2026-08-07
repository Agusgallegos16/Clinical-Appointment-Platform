import axiosClient from './axiosClient';

export const turnoService = {
  reservar: async (turnoData) => {
    const response = await axiosClient.post('/turnos', turnoData);
    return response.data;
  },

  obtenerPorPaciente: async (pacienteId) => {
    const response = await axiosClient.get(`/turnos/paciente/${pacienteId}`);
    return response.data;
  },

  cancelar: async (turnoId) => {
    const response = await axiosClient.put(`/turnos/${turnoId}/cancelar`);
    return response.data;
  },

  cambiarEstado: async (turnoId, nuevoEstado) => {
    const response = await axiosClient.put(`/turnos/${turnoId}/estado?nuevoEstado=${nuevoEstado}`);
    return response.data;
  },
};
