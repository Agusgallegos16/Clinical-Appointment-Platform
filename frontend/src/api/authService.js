import axiosClient from './axiosClient';

export const authService = {
  login: async (credentials) => {
    const response = await axiosClient.post('/auth/login', credentials);
    return response.data;
  },

  registroPaciente: async (pacienteData) => {
    const response = await axiosClient.post('/auth/registro-paciente', pacienteData);
    return response.data;
  },

  registroDoctor: async (doctorData) => {
    const response = await axiosClient.post('/auth/registro-doctor', doctorData);
    return response.data;
  },
};
