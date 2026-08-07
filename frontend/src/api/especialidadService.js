import axiosClient from './axiosClient';

export const especialidadService = {
  listarTodas: async () => {
    const response = await axiosClient.get('/especialidades');
    return response.data;
  },

  crear: async (especialidadData) => {
    const response = await axiosClient.post('/especialidades', especialidadData);
    return response.data;
  },

  eliminar: async (id) => {
    const response = await axiosClient.delete(`/especialidades/${id}`);
    return response.data;
  },
};
