import axiosClient from './axiosClient';

export const doctorService = {
  listarDoctores: async (especialidadId = null, soloVisibles = false) => {
    let url = '/doctores';
    const params = new URLSearchParams();
    if (especialidadId) params.append('especialidadId', especialidadId);
    if (soloVisibles) params.append('soloVisibles', 'true');
    const queryString = params.toString();
    if (queryString) url += `?${queryString}`;

    const response = await axiosClient.get(url);
    return response.data;
  },

  cambiarDisponibilidadTurnos: async (id, disponible) => {
    const response = await axiosClient.patch(`/doctores/${id}/disponibilidad-turnos?disponible=${disponible}`);
    return response.data;
  },

  configurarAdvertenciaBloqueante: async (id, activa, mensaje) => {
    const response = await axiosClient.patch(`/doctores/${id}/advertencia-bloqueante`, null, {
      params: { activa, mensaje },
    });
    return response.data;
  },

  configurarAdvertenciaInformativa: async (id, activa, mensaje) => {
    const response = await axiosClient.patch(`/doctores/${id}/advertencia-informativa`, null, {
      params: { activa, mensaje },
    });
    return response.data;
  },

  registrarDoctor: async (doctorData) => {
    const response = await axiosClient.post('/auth/registro-doctor', doctorData);
    return response.data;
  },

  obtenerPorId: async (id) => {
    const response = await axiosClient.get(`/doctores/${id}`);
    return response.data;
  },

  actualizarDoctor: async (id, doctorData) => {
    const response = await axiosClient.put(`/doctores/${id}`, doctorData);
    return response.data;
  },

  eliminarDoctor: async (id) => {
    const response = await axiosClient.delete(`/doctores/${id}`);
    return response.data;
  },

  obtenerDisponibilidad: async (id, fechaStr, especialidadId = null) => {
    let url = `/doctores/${id}/disponibilidad?fecha=${fechaStr}`;
    if (especialidadId) {
      url += `&especialidadId=${especialidadId}`;
    }
    const response = await axiosClient.get(url);
    return response.data;
  },

  obtenerAgenda: async (id, fechaStr) => {
    const response = await axiosClient.get(`/doctores/${id}/agenda?fecha=${fechaStr}`);
    return response.data;
  },

  agregarHorario: async (id, horarioData) => {
    const response = await axiosClient.post(`/doctores/${id}/horarios`, horarioData);
    return response.data;
  },

  actualizarHorario: async (horarioId, horarioData) => {
    const response = await axiosClient.put(`/doctores/horarios/${horarioId}`, horarioData);
    return response.data;
  },

  obtenerHorarios: async (id) => {
    const response = await axiosClient.get(`/doctores/${id}/horarios`);
    return response.data;
  },

  eliminarHorario: async (horarioId) => {
    const response = await axiosClient.delete(`/doctores/horarios/${horarioId}`);
    return response.data;
  },

  limpiarHorariosSemana: async (id, desdeStr, hastaStr) => {
    const response = await axiosClient.delete(`/doctores/${id}/horarios/semana?desde=${desdeStr}&hasta=${hastaStr}`);
    return response.data;
  },

  obtenerSlots: async (doctorId, desdeStr = null, hastaStr = null) => {
    let url = `/doctores/${doctorId}/slots`;
    if (desdeStr && hastaStr) {
      url += `?desde=${desdeStr}&hasta=${hastaStr}`;
    }
    const response = await axiosClient.get(url);
    return response.data;
  },

  eliminarSlot: async (slotId) => {
    const response = await axiosClient.delete(`/doctores/slots/${slotId}`);
    return response.data;
  },

  crearPlantilla: async (id, plantillaData) => {
    const response = await axiosClient.post(`/doctores/${id}/plantillas`, plantillaData);
    return response.data;
  },

  actualizarPlantilla: async (plantillaId, plantillaData) => {
    const response = await axiosClient.put(`/doctores/plantillas/${plantillaId}`, plantillaData);
    return response.data;
  },

  listarPlantillas: async (id) => {
    const response = await axiosClient.get(`/doctores/${id}/plantillas`);
    return response.data;
  },

  eliminarPlantilla: async (plantillaId) => {
    const response = await axiosClient.delete(`/doctores/plantillas/${plantillaId}`);
    return response.data;
  },

  aplicarPlantilla: async (id, aplicarData) => {
    const response = await axiosClient.post(`/doctores/${id}/aplicar-plantilla`, aplicarData);
    return response.data;
  },
};
