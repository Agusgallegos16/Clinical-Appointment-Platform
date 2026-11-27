import axiosClient from './axiosClient';

export const turnoService = {
  reservar: async (turnoData) => {
    const response = await axiosClient.post('/turnos', turnoData);
    return response.data;
  },

  obtenerPorPaciente: async (pacienteId, options = {}) => {
    const response = await axiosClient.get(`/turnos/paciente/${pacienteId}`, options);
    return response.data;
  },

  cancelar: async (turnoId, motivo = null) => {
    const payload = motivo ? { motivo } : {};
    const response = await axiosClient.put(`/turnos/${turnoId}/cancelar`, payload);
    return response.data;
  },

  cancelarPorDoctor: async (turnoId, motivo) => {
    const response = await axiosClient.put(
      `/turnos/${turnoId}/cancelar-doctor`,
      { motivo },
      { params: { motivo } }
    );
    return response.data;
  },

  cambiarEstado: async (turnoId, nuevoEstado) => {
    const response = await axiosClient.put(`/turnos/${turnoId}/estado`, null, {
      params: { nuevoEstado },
    });
    return response.data;
  },
};
