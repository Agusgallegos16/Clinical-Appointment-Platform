import dayjs from 'dayjs';

export const normalizeFechaISO = (fechaRaw) => {
  if (fechaRaw == null) return null;
  if (Array.isArray(fechaRaw)) {
    const [year, month, day] = fechaRaw;
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  }
  const str = String(fechaRaw).trim();
  return str.length >= 10 ? str.substring(0, 10) : str;
};

export const formatLocalTimeToHHmm = (horaRaw) => {
  if (horaRaw == null) return '00:00';
  if (Array.isArray(horaRaw)) {
    const hh = String(horaRaw[0] ?? 0).padStart(2, '0');
    const mm = String(horaRaw[1] ?? 0).padStart(2, '0');
    return `${hh}:${mm}`;
  }
  const str = String(horaRaw).trim();
  return str.length >= 5 ? str.substring(0, 5) : str;
};

export const buildFechaHoraISO = (fechaStr, horaHHmm) => {
  if (!fechaStr || !horaHHmm) return null;
  return `${fechaStr}T${horaHHmm}:00`;
};

export const isPastSlot = (fechaStr, horaHHmm, now = dayjs()) => {
  if (!fechaStr || !horaHHmm) return false;
  const hoyStr = now.format('YYYY-MM-DD');
  if (fechaStr < hoyStr) return true;
  if (fechaStr > hoyStr) return false;
  return horaHHmm <= now.format('HH:mm');
};

export const formatFechaHoraResumen = (fechaStr, horaHHmm) => {
  if (!fechaStr || !horaHHmm) return '';
  const fechaFormateada = dayjs(fechaStr).isValid()
    ? dayjs(fechaStr).format('DD/MM/YYYY')
    : fechaStr;
  return `${fechaFormateada} a las ${horaHHmm} hs`;
};

export const normalizarProximoTurno = (dto) => {
  if (!dto) return null;
  const fecha = normalizeFechaISO(dto.fecha);
  const hora = formatLocalTimeToHHmm(dto.hora);
  return {
    slotId: dto.slotId ?? dto.id ?? null,
    fecha,
    hora,
    fechaHoraISO: buildFechaHoraISO(fecha, hora),
    especialidadId: dto.especialidadId ?? null,
    especialidadNombre: dto.especialidadNombre ?? null,
  };
};

export const normalizarSlotDisponibilidad = (slot, fechaStr, now = dayjs()) => {
  const hora = formatLocalTimeToHHmm(slot?.hora);
  return {
    id: slot?.id ?? null,
    hora,
    fechaHoraISO: buildFechaHoraISO(fechaStr, hora),
    especialidadId: slot?.especialidadId ?? null,
    especialidadNombre: slot?.especialidadNombre ?? null,
    disponible: slot?.disponible ?? true,
    pasado: isPastSlot(fechaStr, hora, now),
  };
};
