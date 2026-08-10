import axiosClient from './axiosClient';

export const pacienteMenorService = {
  registrarMenor: async (menorData) => {
    const response = await axiosClient.post('/pacientes/menores', menorData);
    return response.data;
  },

  listarMenores: async () => {
    const response = await axiosClient.get('/pacientes/menores');
    return response.data;
  },

  desvincularMenor: async (menorId) => {
    const response = await axiosClient.delete(`/pacientes/menores/${menorId}`);
    return response.data;
  },
};
