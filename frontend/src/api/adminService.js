import axiosClient from './axiosClient';

export const adminService = {
  ejecutarResumenDiario: async (fechaStr = '') => {
    const url = fechaStr ? `/doctores/ejecutar-resumen-diario?fecha=${fechaStr}` : '/doctores/ejecutar-resumen-diario';
    const response = await axiosClient.post(url);
    return response.data;
  },

  ejecutarResumenSemanal: async (desdeStr = '', hastaStr = '') => {
    let url = '/doctores/ejecutar-resumen-semanal';
    if (desdeStr && hastaStr) {
      url += `?desde=${desdeStr}&hasta=${hastaStr}`;
    }
    const response = await axiosClient.post(url);
    return response.data;
  },

  buscarUsuarios: async (email = '') => {
    const response = await axiosClient.get('/admin/usuarios', { params: { email } });
    return response.data;
  },

  cambiarEstadoBloqueo: async (id, bloquear) => {
    const response = await axiosClient.patch(`/admin/usuarios/${id}/bloquear`, null, { params: { bloquear } });
    return response.data;
  },

  eliminarUsuario: async (id) => {
    const response = await axiosClient.delete(`/admin/usuarios/${id}`);
    return response.data;
  },
};
