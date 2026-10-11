import axiosClient from './axiosClient';

export const especialidadService = {
  listarTodas: async (options = {}) => {
    const response = await axiosClient.get('/especialidades', options);
    return response.data;
  },

  obtenerTodas: async (options = {}) => {
    const response = await axiosClient.get('/especialidades', options);
    return response.data;
  },

  crear: async (especialidadData) => {
    const response = await axiosClient.post('/especialidades', especialidadData);
    return response.data;
  },

  actualizar: async (id, especialidadData) => {
    const response = await axiosClient.put(`/especialidades/${id}`, especialidadData);
    return response.data;
  },

  eliminar: async (id) => {
    const response = await axiosClient.delete(`/especialidades/${id}`);
    return response.data;
  },
};
