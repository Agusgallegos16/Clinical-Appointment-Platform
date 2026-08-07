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
};
