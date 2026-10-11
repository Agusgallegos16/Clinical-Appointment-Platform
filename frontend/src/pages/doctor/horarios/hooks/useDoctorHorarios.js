import { useState, useEffect, useMemo } from 'react';
import dayjs from 'dayjs';
import { useAuth } from '../../../../context/AuthContext';
import { doctorService } from '../../../../api/doctorService';
import {
  diasSemanaEnum,
  diasNombreEs,
  normalizeFecha,
  normalizeHora,
  getSpecialtyColorPalette,
  generateTimeSlots,
} from '../utils/horariosFormatters';

export const useDoctorHorarios = () => {
  const { entidadId } = useAuth();
  const todayStr = dayjs().format('YYYY-MM-DD');
  const now = dayjs();
  const currentMinutesToday = now.hour() * 60 + now.minute();

  const [doctorInfo, setDoctorInfo] = useState(null);
  const [horarios, setHorarios] = useState([]);
  const [plantillas, setPlantillas] = useState([]);
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // Pop-up modal centrado de error o advertencia
  const [errorModal, setErrorModal] = useState('');
  const [errorModalTitle, setErrorModalTitle] = useState('Atención');
  const [success, setSuccess] = useState('');

  // Navegación de semana
  const [currentWeekStart, setCurrentWeekStart] = useState(dayjs().startOf('week'));

  // WIZARD MODAL 1: AGREGAR FRANJA HORARIA MANUAL
  const [openManualWizard, setOpenManualWizard] = useState(false);
  const [activeStepManual, setActiveStepManual] = useState(0);
  const [selectedEspecialidadId, setSelectedEspecialidadId] = useState('');
  const [tipoHorario, setTipoHorario] = useState('RECURRENTE');
  const [diaSemana, setDiaSemana] = useState('LUNES');
  const [fechaPuntual, setFechaPuntual] = useState('');
  const [fechaDesde, setFechaDesde] = useState('');
  const [fechaHasta, setFechaHasta] = useState('');
  const [horaInicio, setHoraInicio] = useState('09:00');
  const [horaFin, setHoraFin] = useState('13:00');
  const [duracionTurno, setDuracionTurno] = useState(30);

  // WIZARD MODAL 2: APLICAR PLANTILLA
  const [openTemplateWizard, setOpenTemplateWizard] = useState(false);
  const [activeStepTemplate, setActiveStepTemplate] = useState(0);
  const [selectedPlantillaId, setSelectedPlantillaId] = useState('');
  const [tipoAplicacion, setTipoAplicacion] = useState('RECURRENTE');
  const [diaAplicar, setDiaAplicar] = useState('LUNES');
  const [fechaAplicar, setFechaAplicar] = useState('');
  const [fechaDesdeAplicar, setFechaDesdeAplicar] = useState('');
  const [fechaHastaAplicar, setFechaHastaAplicar] = useState('');
  const [applying, setApplying] = useState(false);

  // Modales de bloqueo / edición / eliminación
  const [selectedSlotBlock, setSelectedSlotBlock] = useState(null);
  const [blocking, setBlocking] = useState(false);
  const [editingHorario, setEditingHorario] = useState(null);
  const [editFormData, setEditFormData] = useState({
    especialidadId: '',
    diaSemana: 'LUNES',
    fecha: '',
    fechaDesde: '',
    fechaHasta: '',
    horaInicio: '09:00',
    horaFin: '13:00',
    duracionTurnoMinutos: 30,
  });
  const [showClearWeekModal, setShowClearWeekModal] = useState(false);
  const [clearingWeek, setClearingWeek] = useState(false);
  const [selectedHorarioDelete, setSelectedHorarioDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (entidadId) {
      cargarPerfilDoctor();
      cargarHorarios();
      cargarPlantillas();
      cargarSlots();
    }
  }, [entidadId, currentWeekStart]);

  const cargarPerfilDoctor = async () => {
    try {
      const doc = await doctorService.obtenerPorId(entidadId);
      setDoctorInfo(doc);
      if (doc.especialidades && doc.especialidades.length > 0) {
        setSelectedEspecialidadId(doc.especialidades[0].id);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const cargarHorarios = async () => {
    try {
      const data = await doctorService.obtenerHorarios(entidadId);
      const normalized = data.map((h) => ({
        ...h,
        fecha: normalizeFecha(h.fecha),
        fechaDesde: normalizeFecha(h.fechaDesde),
        fechaHasta: normalizeFecha(h.fechaHasta),
        horaInicio: normalizeHora(h.horaInicio),
        horaFin: normalizeHora(h.horaFin),
      }));
      setHorarios(normalized);
    } catch (err) {
      triggerErrorModal('Error al obtener la lista de horarios.', 'Error de Carga');
    } finally {
      setLoading(false);
    }
  };

  const cargarPlantillas = async () => {
    try {
      const data = await doctorService.listarPlantillas(entidadId);
      setPlantillas(data);
    } catch (err) {
      console.error(err);
    }
  };

  const cargarSlots = async () => {
    try {
      const desdeStr = currentWeekStart.format('YYYY-MM-DD');
      const hastaStr = currentWeekStart.add(6, 'day').format('YYYY-MM-DD');
      const data = await doctorService.obtenerSlots(entidadId, desdeStr, hastaStr);
      setSlots(data);
    } catch (err) {
      console.error(err);
    }
  };

  const triggerErrorModal = (msg, title = 'Conflicto de Horario') => {
    setErrorModalTitle(title);
    setErrorModal(msg);
  };

  const timeSlots = useMemo(() => generateTimeSlots(), []);

  const weekDaysWithDates = useMemo(() => {
    const todayStart = dayjs().startOf('day');

    return [0, 1, 2, 3, 4, 5, 6].map((offset) => {
      const dateObj = currentWeekStart.add(offset, 'day');
      const isPastDay = dateObj.isBefore(todayStart);
      const isTodayDay = dateObj.isSame(todayStart, 'day');

      return {
        dayNum: offset,
        enumName: diasSemanaEnum[offset],
        nameEs: diasNombreEs[offset],
        dateStr: dateObj.format('YYYY-MM-DD'),
        formattedHeader: `${diasNombreEs[offset]} ${dateObj.format('DD/MM')}`,
        isPastDay,
        isTodayDay,
      };
    });
  }, [currentWeekStart]);

  const weekSlotsByDate = useMemo(() => {
    const slotsMap = {};

    weekDaysWithDates.forEach((dayInfo) => {
      const { dateStr } = dayInfo;
      slotsMap[dateStr] = [];
    });

    slots.forEach((s) => {
      const dateStr = normalizeFecha(s.fecha);
      if (slotsMap[dateStr]) {
        const startText = normalizeHora(s.horaInicio);
        const endText = normalizeHora(s.horaFin);
        const startMinutes = dayjs(`${dateStr}T${startText}`).hour() * 60 + dayjs(`${dateStr}T${startText}`).minute();
        const endMinutes = dayjs(`${dateStr}T${endText}`).hour() * 60 + dayjs(`${dateStr}T${endText}`).minute();

        const espNombre = s.especialidad ? s.especialidad.nombre : null;
        const espId = s.especialidad ? s.especialidad.id : null;
        const colorPalette = getSpecialtyColorPalette(espId);

        slotsMap[dateStr].push({
          slotId: s.id,
          id: `slot-${s.id}`,
          originalData: s,
          dateStr,
          isPuntual: s.esPuntual,
          startText,
          endText,
          startMinutes,
          endMinutes,
          durationMinutes: s.duracionMinutos || 30,
          especialidadNombre: espNombre,
          colorPalette,
        });
      }
    });

    Object.keys(slotsMap).forEach((d) => {
      slotsMap[d].sort((a, b) => a.startMinutes - b.startMinutes);
    });

    return slotsMap;
  }, [slots, weekDaysWithDates]);

  const coveredRowsByDate = useMemo(() => {
    const map = {};
    weekDaysWithDates.forEach((d) => {
      map[d.dateStr] = new Set();
    });

    weekDaysWithDates.forEach((dayInfo) => {
      const dateStr = dayInfo.dateStr;
      const daySlots = weekSlotsByDate[dateStr] || [];
      if (daySlots.length === 0) return;

      timeSlots.forEach((timeStr) => {
        const rowStartMin = parseInt(timeStr.split(':')[0], 10) * 60 + parseInt(timeStr.split(':')[1], 10);
        const rowEndMin = rowStartMin + 30;

        const startingSlots = daySlots.filter((s) => s.startMinutes >= rowStartMin && s.startMinutes < rowEndMin);

        if (startingSlots.length > 0) {
          const maxEndMin = Math.max(...startingSlots.map((s) => s.endMinutes));

          for (let checkMin = rowStartMin + 30; checkMin < maxEndMin; checkMin += 30) {
            const hasNextStarting = daySlots.some((s) => s.startMinutes >= checkMin && s.startMinutes < checkMin + 30);
            if (!hasNextStarting) {
              map[dateStr].add(checkMin);
            } else {
              break;
            }
          }
        }
      });
    });

    return map;
  }, [weekDaysWithDates, weekSlotsByDate, timeSlots]);

  const horariosVisiblesEnSemana = useMemo(() => {
    const weekStart = currentWeekStart.startOf('day');
    const weekEnd = currentWeekStart.add(6, 'day').endOf('day');

    const diasOrden = {
      DOMINGO: 0,
      LUNES: 1,
      MARTES: 2,
      MIERCOLES: 3,
      JUEVES: 4,
      VIERNES: 5,
      SABADO: 6,
    };

    const filtrados = horarios.filter((h) => {
      if (h.fecha) {
        const f = dayjs(h.fecha);
        return (f.isAfter(weekStart) || f.isSame(weekStart, 'day')) &&
               (f.isBefore(weekEnd) || f.isSame(weekEnd, 'day'));
      } else if (h.diaSemana) {
        if (h.fechaDesde && dayjs(h.fechaDesde).isAfter(weekEnd, 'day')) return false;
        if (h.fechaHasta && dayjs(h.fechaHasta).isBefore(weekStart, 'day')) return false;
        return true;
      }
      return true;
    });

    return filtrados.sort((a, b) => {
      const diaA = a.fecha ? dayjs(a.fecha).day() : (diasOrden[a.diaSemana] ?? 0);
      const diaB = b.fecha ? dayjs(b.fecha).day() : (diasOrden[b.diaSemana] ?? 0);

      if (diaA !== diaB) {
        return diaA - diaB;
      }

      return a.horaInicio.localeCompare(b.horaInicio);
    });
  }, [horarios, currentWeekStart]);

  const totalTurnosSemana = useMemo(() => {
    return Object.values(weekSlotsByDate).reduce((acc, arr) => acc + arr.length, 0);
  }, [weekSlotsByDate]);

  const handlePrevWeek = () => setCurrentWeekStart((prev) => prev.subtract(1, 'week'));
  const handleNextWeek = () => setCurrentWeekStart((prev) => prev.add(1, 'week'));
  const handleToday = () => setCurrentWeekStart(dayjs().startOf('week'));

  // MÉTODOS DEL WIZARD MANUAL
  const handleOpenManualWizard = () => {
    setActiveStepManual(0);
    setOpenManualWizard(true);
  };

  const handleNextManual = () => {
    if (activeStepManual === 1) {
      if (tipoHorario === 'PUNTUAL' && !fechaPuntual) {
        triggerErrorModal('Por favor selecciona una fecha puntual específica.', 'Campo Requerido');
        return;
      }
    } else if (activeStepManual === 2) {
      if (!horaInicio || !horaFin) {
        triggerErrorModal('Por favor indica la hora de inicio y fin de la franja.', 'Campos Requeridos');
        return;
      }
    }
    setActiveStepManual((prev) => prev + 1);
  };

  const handleBackManual = () => {
    setActiveStepManual((prev) => prev - 1);
  };

  const handleGuardarHorarioWizard = async () => {
    setSaving(true);
    setErrorModal('');
    setSuccess('');

    try {
      const payload = {
        especialidadId: selectedEspecialidadId ? Number(selectedEspecialidadId) : null,
        horaInicio,
        horaFin,
        duracionTurnoMinutos: Number(duracionTurno),
        fechaDesde: fechaDesde || null,
        fechaHasta: fechaHasta || null,
      };

      if (tipoHorario === 'RECURRENTE') {
        payload.diaSemana = diaSemana;
      } else {
        payload.fecha = fechaPuntual;
      }

      await doctorService.agregarHorario(entidadId, payload);
      setSuccess('¡Franja horaria agregada y turnos generados exitosamente!');
      setOpenManualWizard(false);
      setFechaDesde('');
      setFechaHasta('');
      setFechaPuntual('');
      cargarHorarios();
      cargarSlots();
    } catch (err) {
      const msg = err.response?.data?.mensaje || err.response?.data?.message || 'Error al guardar la franja horaria.';
      triggerErrorModal(msg, 'Conflicto de Horario');
    } finally {
      setSaving(false);
    }
  };

  // MÉTODOS DEL WIZARD PLANTILLA
  const handleOpenTemplateWizard = () => {
    if (plantillas.length > 0 && !selectedPlantillaId) {
      setSelectedPlantillaId(plantillas[0].id);
    }
    setActiveStepTemplate(0);
    setOpenTemplateWizard(true);
  };

  const handleNextTemplate = () => {
    if (activeStepTemplate === 0 && !selectedPlantillaId) {
      triggerErrorModal('Por favor selecciona una plantilla de la lista.', 'Campo Requerido');
      return;
    }
    if (activeStepTemplate === 1) {
      if (tipoAplicacion === 'PUNTUAL' && !fechaAplicar) {
        triggerErrorModal('Por favor indica la fecha específica de aplicación.', 'Campo Requerido');
        return;
      }
    }
    setActiveStepTemplate((prev) => prev + 1);
  };

  const handleBackTemplate = () => {
    setActiveStepTemplate((prev) => prev - 1);
  };

  const handleAplicarPlantillaWizard = async () => {
    if (!selectedPlantillaId) return;
    setApplying(true);
    setErrorModal('');
    setSuccess('');

    try {
      const payload = { plantillaId: Number(selectedPlantillaId) };
      if (tipoAplicacion === 'RECURRENTE') {
        payload.diaSemana = diaAplicar;
        payload.fechaDesde = fechaDesdeAplicar || null;
        payload.fechaHasta = fechaHastaAplicar || null;
      } else {
        payload.fecha = fechaAplicar;
      }

      await doctorService.aplicarPlantilla(entidadId, payload);
      setSuccess('¡Plantilla aplicada correctamente con su período de atención!');
      setOpenTemplateWizard(false);
      setFechaDesdeAplicar('');
      setFechaHastaAplicar('');
      setFechaAplicar('');
      cargarHorarios();
      cargarSlots();
    } catch (err) {
      const msg = err.response?.data?.mensaje || err.response?.data?.message || 'Error al aplicar la plantilla.';
      triggerErrorModal(msg, 'Aplicar Plantilla');
    } finally {
      setApplying(false);
    }
  };

  const handleOpenEditModal = (h) => {
    setEditingHorario(h);
    setEditFormData({
      especialidadId: h.especialidad ? h.especialidad.id : '',
      diaSemana: h.diaSemana || 'LUNES',
      fecha: h.fecha || '',
      fechaDesde: h.fechaDesde || '',
      fechaHasta: h.fechaHasta || '',
      horaInicio: h.horaInicio || '09:00',
      horaFin: h.horaFin || '13:00',
      duracionTurnoMinutos: h.duracionTurnoMinutos || 30,
    });
  };

  const handleSaveEditHorario = async () => {
    if (!editingHorario) return;
    setSaving(true);
    setErrorModal('');
    setSuccess('');

    try {
      const payload = {
        especialidadId: editFormData.especialidadId ? Number(editFormData.especialidadId) : null,
        horaInicio: editFormData.horaInicio,
        horaFin: editFormData.horaFin,
        duracionTurnoMinutos: Number(editFormData.duracionTurnoMinutos),
        fechaDesde: editFormData.fechaDesde || null,
        fechaHasta: editFormData.fechaHasta || null,
      };

      if (editingHorario.fecha) {
        payload.fecha = editFormData.fecha;
      } else {
        payload.diaSemana = editFormData.diaSemana;
      }

      await doctorService.actualizarHorario(editingHorario.id, payload);
      setSuccess('¡Franja horaria actualizada correctamente!');
      setEditingHorario(null);
      cargarHorarios();
      cargarSlots();
    } catch (err) {
      const msg = err.response?.data?.mensaje || err.response?.data?.message || 'Error al actualizar la franja horaria.';
      triggerErrorModal(msg, 'Conflicto de Horario');
    } finally {
      setSaving(false);
    }
  };

  const handleBloquearSlotIndividual = async () => {
    if (!selectedSlotBlock) return;
    setBlocking(true);
    try {
      if (selectedSlotBlock.slotId) {
        await doctorService.eliminarSlot(selectedSlotBlock.slotId);
      }
      setSuccess(`¡Turno deshabilitado/eliminado con éxito!`);
      setSelectedSlotBlock(null);
      cargarSlots();
    } catch (err) {
      const msg = err.response?.data?.mensaje || err.response?.data?.message || 'No se pudo deshabilitar este turno individual.';
      triggerErrorModal(msg, 'Deshabilitar Turno');
    } finally {
      setBlocking(false);
    }
  };

  const handleConfirmEliminarHorario = async () => {
    if (!selectedHorarioDelete) return;
    setDeleting(true);
    try {
      await doctorService.eliminarHorario(selectedHorarioDelete.id);
      setSelectedHorarioDelete(null);
      setSuccess('Franja horaria eliminada con éxito y sus turnos asociados fueron removidos.');
      cargarHorarios();
      cargarSlots();
    } catch (err) {
      triggerErrorModal('No se pudo eliminar la franja horaria.', 'Eliminar Franja');
    } finally {
      setDeleting(false);
    }
  };

  const handleLimpiarSemanaActual = async () => {
    setClearingWeek(true);
    setErrorModal('');
    setSuccess('');
    try {
      const desdeStr = currentWeekStart.format('YYYY-MM-DD');
      const hastaStr = currentWeekStart.add(6, 'day').format('YYYY-MM-DD');

      await doctorService.limpiarHorariosSemana(entidadId, desdeStr, hastaStr);

      setSuccess(`¡Se limpiaron todos los turnos para la semana del ${currentWeekStart.format('DD/MM')} al ${currentWeekStart.add(6, 'day').format('DD/MM')}!`);
      setShowClearWeekModal(false);
      cargarHorarios();
      cargarSlots();
    } catch (err) {
      triggerErrorModal('Error al borrar los turnos de la semana visible.', 'Limpiar Semana');
    } finally {
      setClearingWeek(false);
    }
  };

  const especialidadSeleccionadaNombre = useMemo(() => {
    if (!doctorInfo?.especialidades) return 'General';
    const esp = doctorInfo.especialidades.find((e) => Number(e.id) === Number(selectedEspecialidadId));
    return esp ? esp.nombre : 'General';
  }, [doctorInfo, selectedEspecialidadId]);

  const plantillaSeleccionadaNombre = useMemo(() => {
    const p = plantillas.find((item) => Number(item.id) === Number(selectedPlantillaId));
    return p ? p.nombre : '';
  }, [plantillas, selectedPlantillaId]);

  return {
    todayStr,
    currentMinutesToday,
    doctorInfo,
    horarios,
    plantillas,
    slots,
    loading,
    saving,
    errorModal,
    setErrorModal,
    errorModalTitle,
    success,
    currentWeekStart,
    openManualWizard,
    setOpenManualWizard,
    activeStepManual,
    selectedEspecialidadId,
    setSelectedEspecialidadId,
    tipoHorario,
    setTipoHorario,
    diaSemana,
    setDiaSemana,
    fechaPuntual,
    setFechaPuntual,
    fechaDesde,
    setFechaDesde,
    fechaHasta,
    setFechaHasta,
    horaInicio,
    setHoraInicio,
    horaFin,
    setHoraFin,
    duracionTurno,
    setDuracionTurno,
    openTemplateWizard,
    setOpenTemplateWizard,
    activeStepTemplate,
    selectedPlantillaId,
    setSelectedPlantillaId,
    tipoAplicacion,
    setTipoAplicacion,
    diaAplicar,
    setDiaAplicar,
    fechaAplicar,
    setFechaAplicar,
    fechaDesdeAplicar,
    setFechaDesdeAplicar,
    fechaHastaAplicar,
    setFechaHastaAplicar,
    applying,
    selectedSlotBlock,
    setSelectedSlotBlock,
    blocking,
    editingHorario,
    setEditingHorario,
    editFormData,
    setEditFormData,
    showClearWeekModal,
    setShowClearWeekModal,
    clearingWeek,
    selectedHorarioDelete,
    setSelectedHorarioDelete,
    deleting,
    timeSlots,
    weekDaysWithDates,
    weekSlotsByDate,
    coveredRowsByDate,
    horariosVisiblesEnSemana,
    totalTurnosSemana,
    especialidadSeleccionadaNombre,
    plantillaSeleccionadaNombre,
    handlePrevWeek,
    handleNextWeek,
    handleToday,
    handleOpenManualWizard,
    handleNextManual,
    handleBackManual,
    handleGuardarHorarioWizard,
    handleOpenTemplateWizard,
    handleNextTemplate,
    handleBackTemplate,
    handleAplicarPlantillaWizard,
    handleOpenEditModal,
    handleSaveEditHorario,
    handleBloquearSlotIndividual,
    handleConfirmEliminarHorario,
    handleLimpiarSemanaActual,
  };
};
