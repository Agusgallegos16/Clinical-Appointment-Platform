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

  confirmarEmail: async (token) => {
    const response = await axiosClient.get('/auth/confirmar-email', { params: { token } });
    return response.data;
  },

  solicitarRestablecimientoPassword: async (data) => {
    const response = await axiosClient.post('/auth/solicitar-restablecimiento-password', data);
    return response.data;
  },

  confirmarRestablecimientoPassword: async (token) => {
    const response = await axiosClient.get('/auth/confirmar-restablecimiento-password', { params: { token } });
    return response.data;
  },

  registroDoctor: async (doctorData) => {
    const response = await axiosClient.post('/auth/registro-doctor', doctorData);
    return response.data;
  },

  registrarDoctor: async (doctorData) => {
    const response = await axiosClient.post('/auth/registro-doctor', doctorData);
    return response.data;
  },

  establecerPasswordDoctor: async (data) => {
    const response = await axiosClient.post('/auth/establecer-password-doctor', data);
    return response.data;
  },
};
