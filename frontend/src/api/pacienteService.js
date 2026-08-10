import axiosClient from './axiosClient';

export const pacienteService = {
  obtenerPorId: async (id) => {
    const response = await axiosClient.get(`/pacientes/${id}`);
    return response.data;
  },
};
