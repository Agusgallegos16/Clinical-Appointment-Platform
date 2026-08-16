import axiosClient from './axiosClient';

export const googleCalendarService = {
  obtenerUrlAuth: async () => {
    const response = await axiosClient.get('/google-calendar/auth-url');
    return response.data;
  },
  obtenerEstado: async () => {
    const response = await axiosClient.get('/google-calendar/status');
    return response.data;
  },
  desconectar: async () => {
    const response = await axiosClient.post('/google-calendar/disconnect');
    return response.data;
  },
};
