import React, { useEffect, useState, useMemo } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  TextField,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  CircularProgress,
  Alert,
  Paper,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  TableContainer,
} from '@mui/material';
import {
  Add as AddIcon,
  Schedule as ScheduleIcon,
  Delete as DeleteIcon,
  DeleteSweep as DeleteSweepIcon,
  Settings as SettingsIcon,
  FolderCopy as TemplateIcon,
  Info as InfoIcon,
  ChevronLeft as ChevronLeftIcon,
  ChevronRight as ChevronRightIcon,
  Today as TodayIcon,
  Block as BlockIcon,
  Warning as WarningIcon,
  MedicalServices as MedicalIcon,
  History as HistoryIcon,
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { doctorService } from '../../api/doctorService';
import dayjs from 'dayjs';

const diasSemanaEnum = ['DOMINGO', 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO'];
const diasNombreEs = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

// Funciones de normalización defensiva para datos de bloqueos
// Maneja tanto arrays numéricos [2026,8,11] como strings ISO "2026-08-11"
const normalizeFecha = (f) => {
  if (f == null) return null;
  if (Array.isArray(f)) {
    const [y, m, d] = f;
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
  }
  return String(f);
};

const normalizeHora = (h) => {
  if (h == null) return '00:00';
  if (Array.isArray(h)) {
    const hh = String(h[0] || 0).padStart(2, '0');
    const mm = String(h[1] || 0).padStart(2, '0');
    return `${hh}:${mm}`;
  }
  return String(h).substring(0, 5);
};

// Paletas de color por especialidad
const getSpecialtyColorPalette = (espId) => {
  const palettes = [
    { bg: 'linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%)', text: '#0369a1', border: '#0284c7' }, // Azul Médico
    { bg: 'linear-gradient(135deg, #ccfbf1 0%, #99f6e4 100%)', text: '#0f766e', border: '#0d9488' }, // Teal Salud
    { bg: 'linear-gradient(135deg, #f3e8ff 0%, #e9d5ff 100%)', text: '#6b21a8', border: '#9333ea' }, // Violeta Dermatología
    { bg: 'linear-gradient(135deg, #fef3c7 0%, #fde68a 100%)', text: '#92400e', border: '#d97706' }, // Ámbar Traumatología
    { bg: 'linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%)', text: '#166534', border: '#16a34a' }, // Verde Pediatría
    { bg: 'linear-gradient(135deg, #ffe4e6 0%, #fecdd3 100%)', text: '#9f1239', border: '#e11d48' }, // Rosa
  ];
  if (!espId) return palettes[0];
  const idx = Number(espId) % palettes.length;
  return palettes[idx];
};

const generateTimeSlots = () => {
  const times = [];
  let current = dayjs().hour(7).minute(0).second(0);
  const end = dayjs().hour(22).minute(0).second(0);

  while (current.isBefore(end) || current.isSame(end)) {
    times.push(current.format('HH:mm'));
    current = current.add(30, 'minute');
  }
  return times;
};

const DoctorHorarios = () => {
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

  // Navegación de semana (Inicio de semana actual: Domingo)
  const [currentWeekStart, setCurrentWeekStart] = useState(dayjs().startOf('week'));

  // Formulario nuevo horario manual
  const [selectedEspecialidadId, setSelectedEspecialidadId] = useState('');
  const [tipoHorario, setTipoHorario] = useState('RECURRENTE');
  const [diaSemana, setDiaSemana] = useState('LUNES');
  const [fechaPuntual, setFechaPuntual] = useState('');
  const [fechaDesde, setFechaDesde] = useState('');
  const [fechaHasta, setFechaHasta] = useState('');
  const [horaInicio, setHoraInicio] = useState('09:00');
  const [horaFin, setHoraFin] = useState('13:00');
  const [duracionTurno, setDuracionTurno] = useState(30);

  // Formulario aplicar plantilla
  const [selectedPlantillaId, setSelectedPlantillaId] = useState('');
  const [tipoAplicacion, setTipoAplicacion] = useState('RECURRENTE');
  const [diaAplicar, setDiaAplicar] = useState('LUNES');
  const [fechaAplicar, setFechaAplicar] = useState('');
  const [fechaDesdeAplicar, setFechaDesdeAplicar] = useState('');
  const [fechaHastaAplicar, setFechaHastaAplicar] = useState('');
  const [applying, setApplying] = useState(false);

  // Modal para deshabilitar / bloquear slot individual
  const [selectedSlotBlock, setSelectedSlotBlock] = useState(null);
  const [blocking, setBlocking] = useState(false);

  // Modal para editar / configurar franja horaria existente ⚙️
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

  // Modal para borrar horarios de toda la semana visible
  const [showClearWeekModal, setShowClearWeekModal] = useState(false);
  const [clearingWeek, setClearingWeek] = useState(false);

  // Modal para eliminar franja horaria completa
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
      // Normalizar fechas y horas para manejar ambos formatos (array/string) de Jackson
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

  // Días de la semana visible con sus fechas reales (Domingo a Sábado) y estado de fecha pasada
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

  // Renderizar la grilla con los slots instanciados concretos directamente cargados desde la BD
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

  // Precalculo de filas absorbidas por rowSpan previa en la grilla estilo Excel
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

  // FILTRADO Y ORDENAMIENTO DINÁMICO DE LA LISTA INFERIOR
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

  const handleGuardarHorario = async (e) => {
    e.preventDefault();
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
      setSuccess('¡Horario de atención agregado y slots generados exitosamente!');
      setFechaDesde('');
      setFechaHasta('');
      cargarHorarios();
      cargarSlots();
    } catch (err) {
      const msg = err.response?.data?.mensaje || err.response?.data?.message || 'Error al guardar el horario.';
      triggerErrorModal(msg, 'Conflicto de Horario');
    } finally {
      setSaving(false);
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

  const handleAplicarPlantilla = async (e) => {
    e.preventDefault();
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
      setSuccess('¡Plantilla aplicada correctamente con su período de vigencia!');
      setFechaDesdeAplicar('');
      setFechaHastaAplicar('');
      cargarHorarios();
      cargarSlots();
    } catch (err) {
      const msg = err.response?.data?.mensaje || err.response?.data?.message || 'Error al aplicar la plantilla.';
      triggerErrorModal(msg, 'Aplicar Plantilla');
    } finally {
      setApplying(false);
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

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={2} mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} color="primary" mb={0.5}>
            Gestión de Horarios por Especialidad
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Configurá y gestioná tus días y horarios de atención médica.
          </Typography>
        </Box>

        <Chip
          icon={<ScheduleIcon />}
          label={`${totalTurnosSemana} turnos activos esta semana`}
          color="primary"
          sx={{ fontWeight: 700, fontSize: '0.95rem', py: 2, px: 1 }}
        />
      </Box>

      {/* POP-UP DIALOG MODAL DE ALERTA PROMINENTE */}
      <Dialog open={!!errorModal} onClose={() => setErrorModal('')} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ color: 'error.main', display: 'flex', alignItems: 'center', gap: 1, fontWeight: 700 }}>
          <WarningIcon color="error" /> {errorModalTitle}
        </DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ color: 'text.primary', fontWeight: 500 }}>
            {errorModal}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setErrorModal('')} variant="contained" color="error" fullWidth>
            Entendido
          </Button>
        </DialogActions>
      </Dialog>

      {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

      {/* PLANILLA MATRIZ Y LISTADO DE TURNOS POR DÍA DE LA SEMANA */}
      <Paper sx={{ p: 3, mb: 4, bgcolor: 'background.paper', borderRadius: 3, boxShadow: '0 4px 20px rgba(0,0,0,0.06)' }}>
        <Box display="flex" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={2} mb={2.5}>
          <Box display="flex" alignItems="center" gap={1}>
            <Button
              variant="outlined"
              size="small"
              startIcon={<ChevronLeftIcon />}
              onClick={handlePrevWeek}
            >
              Anterior
            </Button>
            <Button
              variant="outlined"
              size="small"
              startIcon={<TodayIcon />}
              onClick={handleToday}
            >
              Semana Actual
            </Button>
            <Button
              variant="outlined"
              size="small"
              endIcon={<ChevronRightIcon />}
              onClick={handleNextWeek}
            >
              Siguiente
            </Button>
          </Box>

          <Box display="flex" alignItems="center" gap={2}>
            <Typography variant="h6" fontWeight={700} color="primary">
              Semana del {currentWeekStart.format('DD/MM/YYYY')} al {currentWeekStart.add(6, 'day').format('DD/MM/YYYY')}
            </Typography>

            <Button
              variant="outlined"
              color="error"
              size="small"
              startIcon={<DeleteSweepIcon />}
              onClick={() => setShowClearWeekModal(true)}
              sx={{ fontWeight: 700 }}
            >
              Borrar Turnos de esta Semana
            </Button>
          </Box>
        </Box>

        {/* PLANILLA MATRIZ ESTILO EXCEL CON FILTRO GRIS EN DÍAS/HORAS PASADOS */}
        <TableContainer sx={{ maxHeight: 650, borderRadius: 2 }}>
          <Table stickyHeader size="small" border={1} borderColor="#cbd5e1" sx={{ borderCollapse: 'collapse', bgcolor: '#f8fafc' }}>
            <TableHead>
              <TableRow>
                <TableCell sx={{ bgcolor: '#0284c7', color: '#ffffff', fontWeight: 700, width: 90, zIndex: 11, textAlign: 'center' }}>
                  Hora
                </TableCell>
                {weekDaysWithDates.map((dayInfo) => (
                  <TableCell
                    key={dayInfo.dateStr}
                    align="center"
                    sx={{
                      bgcolor: dayInfo.isPastDay ? '#64748b' : '#0284c7',
                      color: '#ffffff',
                      fontWeight: 700,
                      minWidth: 155,
                      zIndex: 11,
                      opacity: dayInfo.isPastDay ? 0.85 : 1,
                    }}
                  >
                    <Typography variant="subtitle2" fontWeight={700}>
                      {dayInfo.formattedHeader} {dayInfo.isPastDay ? '(Pasado)' : ''}
                    </Typography>
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>

            <TableBody>
              {timeSlots.map((timeStr) => {
                const rowStartMin = parseInt(timeStr.split(':')[0], 10) * 60 + parseInt(timeStr.split(':')[1], 10);
                const rowEndMin = rowStartMin + 30;

                return (
                  <TableRow key={timeStr} hover style={{ height: 48 }}>
                    <TableCell sx={{ fontWeight: 700, bgcolor: '#e2e8f0', color: '#0f172a', textAlign: 'center', py: 0.5, height: 48, boxSizing: 'border-box' }}>
                      {timeStr}
                    </TableCell>

                    {weekDaysWithDates.map((dayInfo) => {
                      const dateStr = dayInfo.dateStr;
                      const daySlots = weekSlotsByDate[dateStr] || [];
                      const isPastCell = dayInfo.isPastDay || (dayInfo.isTodayDay && rowEndMin <= currentMinutesToday);

                      // Si esta celda fue absorbida por un rowSpan previo, no renderizar <TableCell>
                      if (coveredRowsByDate[dateStr]?.has(rowStartMin)) {
                        return null;
                      }

                      // Turnos que COMIENZAN dentro del rango de esta fila [rowStartMin, rowEndMin)
                      const startingSlots = daySlots.filter(
                        (s) => s.startMinutes >= rowStartMin && s.startMinutes < rowEndMin
                      );

                      if (startingSlots.length > 0) {
                        const maxEndMin = Math.max(...startingSlots.map((s) => s.endMinutes));

                        let rowsSpanned = 1;
                        for (let checkMin = rowStartMin + 30; checkMin < maxEndMin; checkMin += 30) {
                          const hasNextStarting = daySlots.some((s) => s.startMinutes >= checkMin && s.startMinutes < checkMin + 30);
                          if (!hasNextStarting) {
                            rowsSpanned++;
                          } else {
                            break;
                          }
                        }

                        const cellHeightPx = rowsSpanned * 48;

                        return (
                          <TableCell
                            key={dateStr}
                            rowSpan={rowsSpanned}
                            align="center"
                            sx={{
                              p: 0,
                              position: 'relative',
                              verticalAlign: 'top',
                              bgcolor: isPastCell ? '#e2e8f0' : '#f8fafc',
                              height: `${cellHeightPx}px`,
                              boxSizing: 'border-box',
                            }}
                          >
                            <Box sx={{ position: 'relative', width: '100%', height: `${cellHeightPx}px` }}>
                              {startingSlots.map((slot) => {
                                const palette = slot.colorPalette;
                                const isPastSlot = isPastCell;

                                // Escala Exacta de Unidad Base: 15m = 24px (1X), 30m = 48px (2X), 45m = 72px (3X), 60m = 96px (4X)
                                const topOffsetPx = Math.round(((slot.startMinutes - rowStartMin) / 15) * 24);
                                const slotHeightPx = Math.max(22, Math.round((slot.durationMinutes / 15) * 24) - 2);

                                return (
                                  <Box
                                    key={slot.id}
                                    onClick={() => setSelectedSlotBlock(slot)}
                                    sx={{
                                      position: 'absolute',
                                      top: `${topOffsetPx}px`,
                                      left: 2,
                                      right: 2,
                                      height: `${slotHeightPx}px`,
                                      display: 'flex',
                                      alignItems: 'center',
                                      justifyContent: 'center',
                                      textAlign: 'center',
                                      background: isPastSlot
                                        ? 'linear-gradient(135deg, #cbd5e1 0%, #94a3b8 100%)'
                                        : palette.bg,
                                      color: isPastSlot ? '#475569' : palette.text,
                                      border: `1.5px solid ${isPastSlot ? '#64748b' : palette.border}`,
                                      borderRadius: 1.5,
                                      py: 0.2,
                                      px: 0.8,
                                      cursor: 'pointer',
                                      opacity: isPastSlot ? 0.65 : 1,
                                      boxShadow: '0 2px 5px rgba(0,0,0,0.06)',
                                      transition: '0.15s ease-in-out',
                                      overflow: 'hidden',
                                      boxSizing: 'border-box',
                                      zIndex: 2,
                                      '&:hover': {
                                        transform: 'scale(1.02)',
                                        boxShadow: '0 4px 12px rgba(2, 132, 199, 0.3)',
                                        zIndex: 10,
                                      },
                                    }}
                                  >
                                    <Typography
                                      variant="caption"
                                      sx={{
                                        fontWeight: 800,
                                        fontSize: slot.durationMinutes <= 15 ? '0.66rem' : '0.75rem',
                                        lineHeight: 1.1,
                                        whiteSpace: 'nowrap',
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                      }}
                                    >
                                      Turno de {slot.startText} a {slot.endText}
                                      {slot.especialidadNombre ? ` — ${slot.especialidadNombre}` : ''}
                                    </Typography>
                                  </Box>
                                );
                              })}
                            </Box>
                          </TableCell>
                        );
                      }

                      return (
                        <TableCell
                          key={dateStr}
                          align="center"
                          sx={{
                            p: 0,
                            height: 48,
                            bgcolor: isPastCell ? '#f1f5f9' : '#f8fafc',
                            boxSizing: 'border-box',
                          }}
                        />
                      );
                    })}
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      <Grid container spacing={3}>
        {/* Formularios a la Izquierda */}
        <Grid item xs={12} md={5}>
          {/* Opción 1: Aplicar Plantilla Directamente */}
          {plantillas.length > 0 && (
            <Paper sx={{ p: 3, mb: 3, borderColor: 'secondary.main', borderWidth: 2 }}>
              <Box display="flex" alignItems="center" gap={1} mb={1}>
                <TemplateIcon color="secondary" />
                <Typography variant="h6" fontWeight={700} color="secondary.dark">
                  Aplicar una Plantilla Guardada
                </Typography>
              </Box>

              <Box component="form" onSubmit={handleAplicarPlantilla}>
                <FormControl fullWidth margin="dense">
                  <InputLabel>Seleccionar Plantilla</InputLabel>
                  <Select
                    value={selectedPlantillaId}
                    onChange={(e) => setSelectedPlantillaId(e.target.value)}
                    label="Seleccionar Plantilla"
                    required
                  >
                    {plantillas.map((p) => (
                      <MenuItem key={p.id} value={p.id}>{p.nombre}</MenuItem>
                    ))}
                  </Select>
                </FormControl>

                <FormControl fullWidth margin="dense">
                  <InputLabel>Aplicar A</InputLabel>
                  <Select
                    value={tipoAplicacion}
                    onChange={(e) => setTipoAplicacion(e.target.value)}
                    label="Aplicar A"
                  >
                    <MenuItem value="RECURRENTE">Día Semanal Recurrente</MenuItem>
                    <MenuItem value="PUNTUAL">Fecha Puntual Específica</MenuItem>
                  </Select>
                </FormControl>

                {tipoAplicacion === 'RECURRENTE' ? (
                  <>
                    <FormControl fullWidth margin="dense">
                      <InputLabel>Día de la Semana</InputLabel>
                      <Select
                        value={diaAplicar}
                        onChange={(e) => setDiaAplicar(e.target.value)}
                        label="Día de la Semana"
                      >
                        {['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'].map((d) => (
                          <MenuItem key={d} value={d}>{d}</MenuItem>
                        ))}
                      </Select>
                    </FormControl>

                    <Grid container spacing={2} sx={{ mt: 0.5 }}>
                      <Grid item xs={6}>
                        <TextField
                          fullWidth
                          label="Vigencia Desde (Opcional)"
                          type="date"
                          value={fechaDesdeAplicar}
                          onChange={(e) => setFechaDesdeAplicar(e.target.value)}
                          inputProps={{ min: todayStr }}
                          InputLabelProps={{ shrink: true }}
                        />
                      </Grid>
                      <Grid item xs={6}>
                        <TextField
                          fullWidth
                          label="Vigencia Hasta (Opcional)"
                          type="date"
                          value={fechaHastaAplicar}
                          onChange={(e) => setFechaHastaAplicar(e.target.value)}
                          inputProps={{ min: fechaDesdeAplicar || todayStr }}
                          InputLabelProps={{ shrink: true }}
                        />
                      </Grid>
                    </Grid>
                  </>
                ) : (
                  <TextField
                    fullWidth
                    margin="dense"
                    label="Fecha Específica"
                    type="date"
                    value={fechaAplicar}
                    onChange={(e) => setFechaAplicar(e.target.value)}
                    required
                    inputProps={{ min: todayStr }}
                    InputLabelProps={{ shrink: true }}
                  />
                )}

                <Button
                  type="submit"
                  variant="contained"
                  color="secondary"
                  fullWidth
                  disabled={applying || !selectedPlantillaId}
                  sx={{ mt: 2 }}
                >
                  {applying ? <CircularProgress size={24} color="inherit" /> : 'Aplicar Plantilla a la Agenda'}
                </Button>
              </Box>
            </Paper>
          )}

          {/* Opción 2: Agregar Horario Manual con Selección de Especialidad */}
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" fontWeight={600} mb={2}>
              Agregar Franja Horaria Manual
            </Typography>

            <Box component="form" onSubmit={handleGuardarHorario}>
              {doctorInfo?.especialidades && doctorInfo.especialidades.length > 0 && (
                <FormControl fullWidth margin="normal">
                  <InputLabel>Especialidad Médica</InputLabel>
                  <Select
                    value={selectedEspecialidadId}
                    onChange={(e) => setSelectedEspecialidadId(e.target.value)}
                    label="Especialidad Médica"
                  >
                    {doctorInfo.especialidades.map((esp) => (
                      <MenuItem key={esp.id} value={esp.id}>
                        {esp.nombre}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              )}

              <FormControl fullWidth margin="normal">
                <InputLabel>Tipo de Configuración</InputLabel>
                <Select
                  value={tipoHorario}
                  onChange={(e) => setTipoHorario(e.target.value)}
                  label="Tipo de Configuración"
                >
                  <MenuItem value="RECURRENTE">Semanal Recurrente (Ej. Todos los Lunes)</MenuItem>
                  <MenuItem value="PUNTUAL">Fecha Puntual Específica</MenuItem>
                </Select>
              </FormControl>

              {tipoHorario === 'RECURRENTE' ? (
                <>
                  <FormControl fullWidth margin="normal">
                    <InputLabel>Día de la Semana</InputLabel>
                    <Select
                      value={diaSemana}
                      onChange={(e) => setDiaSemana(e.target.value)}
                      label="Día de la Semana"
                    >
                      {['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'].map((d) => (
                        <MenuItem key={d} value={d}>{d}</MenuItem>
                      ))}
                    </Select>
                  </FormControl>

                  <Grid container spacing={2} sx={{ mt: 0.5 }}>
                    <Grid item xs={6}>
                      <TextField
                        fullWidth
                        label="Vigencia Desde (Opcional)"
                        type="date"
                        value={fechaDesde}
                        onChange={(e) => setFechaDesde(e.target.value)}
                        inputProps={{ min: todayStr }}
                        InputLabelProps={{ shrink: true }}
                      />
                    </Grid>
                    <Grid item xs={6}>
                      <TextField
                        fullWidth
                        label="Vigencia Hasta (Opcional)"
                        type="date"
                        value={fechaHasta}
                        onChange={(e) => setFechaHasta(e.target.value)}
                        inputProps={{ min: todayStr }}
                        InputLabelProps={{ shrink: true }}
                      />
                    </Grid>
                  </Grid>
                </>
              ) : (
                <TextField
                  fullWidth
                  margin="normal"
                  label="Fecha Puntual"
                  type="date"
                  value={fechaPuntual}
                  onChange={(e) => setFechaPuntual(e.target.value)}
                  required
                  inputProps={{ min: todayStr }}
                  InputLabelProps={{ shrink: true }}
                />
              )}

              <Grid container spacing={2} sx={{ mt: 0.5 }}>
                <Grid item xs={6}>
                  <TextField
                    fullWidth
                    label="Hora Inicio"
                    type="time"
                    value={horaInicio}
                    onChange={(e) => setHoraInicio(e.target.value)}
                    required
                    InputLabelProps={{ shrink: true }}
                  />
                </Grid>
                <Grid item xs={6}>
                  <TextField
                    fullWidth
                    label="Hora Fin"
                    type="time"
                    value={horaFin}
                    onChange={(e) => setHoraFin(e.target.value)}
                    required
                    InputLabelProps={{ shrink: true }}
                  />
                </Grid>
              </Grid>

              <TextField
                fullWidth
                margin="normal"
                label="Duración de Turno (Minutos)"
                type="number"
                value={duracionTurno}
                onChange={(e) => setDuracionTurno(e.target.value)}
                required
              />

              <Button
                type="submit"
                variant="contained"
                fullWidth
                size="large"
                startIcon={<AddIcon />}
                disabled={saving}
                sx={{ mt: 3 }}
              >
                {saving ? <CircularProgress size={24} color="inherit" /> : 'Guardar Franja Horaria'}
              </Button>
            </Box>
          </Paper>
        </Grid>

        {/* Lista de Horarios Existentes Filtrados y Ordenados por Día y Hora */}
        <Grid item xs={12} md={7}>
          <Paper sx={{ p: 3 }}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
              <Typography variant="h6" fontWeight={600}>
                Franjas Vigentes en esta Semana ({horariosVisiblesEnSemana.length})
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Ordenadas por día y hora • {currentWeekStart.format('DD/MM')} al {currentWeekStart.add(6, 'day').format('DD/MM')}
              </Typography>
            </Box>

            {loading ? (
              <CircularProgress />
            ) : horariosVisiblesEnSemana.length === 0 ? (
              <Alert severity="info">No hay franjas horarias vigentes configuradas para la semana visible.</Alert>
            ) : (
              <Grid container spacing={2}>
                {horariosVisiblesEnSemana.map((h) => (
                  <Grid item xs={12} key={h.id}>
                    <Card variant="outlined">
                      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                        <Box display="flex" justifyContent="space-between" alignItems="center">
                          <Box display="flex" alignItems="center" gap={1.5}>
                            <MedicalIcon color="primary" />
                            <Box>
                              <Typography fontWeight={700}>
                                {h.especialidad ? h.especialidad.nombre : 'General'} — {h.diaSemana ? `Todos los ${h.diaSemana}` : `Fecha Puntual: ${h.fecha}`}
                              </Typography>
                              <Typography variant="caption" color="text.secondary" display="block">
                                {h.horaInicio} hs a {h.horaFin} hs ({h.duracionTurnoMinutos} min/turno)
                              </Typography>
                              {(h.fechaDesde || h.fechaHasta) && (
                                <Typography variant="caption" color="primary.main" fontWeight={600}>
                                  📅 Vigencia: {h.fechaDesde || 'Inicio'} ➔ {h.fechaHasta || 'Indefinido'}
                                </Typography>
                              )}
                            </Box>
                          </Box>

                          <Box display="flex" gap={0.5}>
                            <IconButton
                              color="primary"
                              onClick={() => handleOpenEditModal(h)}
                              title="Configurar / Editar Franja ⚙️"
                            >
                              <SettingsIcon />
                            </IconButton>
                            <IconButton
                              color="error"
                              onClick={() => setSelectedHorarioDelete(h)}
                              title="Eliminar Franja Completa"
                            >
                              <DeleteIcon />
                            </IconButton>
                          </Box>
                        </Box>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            )}
          </Paper>
        </Grid>
      </Grid>

      {/* Modal Confirmar Limpieza de Semana Completa */}
      <Dialog open={showClearWeekModal} onClose={() => setShowClearWeekModal(false)}>
        <DialogTitle>¿Borrar todos los turnos de esta semana?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Se deshabilitarán y eliminarán todos los turnos configurados exclusivamente para la semana del <strong>{currentWeekStart.format('DD/MM/YYYY')}</strong> al <strong>{currentWeekStart.add(6, 'day').format('DD/MM/YYYY')}</strong>. Las demás semanas se conservarán intactas.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowClearWeekModal(false)}>Cancelar</Button>
          <Button onClick={handleLimpiarSemanaActual} color="error" variant="contained" disabled={clearingWeek} startIcon={<DeleteSweepIcon />}>
            {clearingWeek ? <CircularProgress size={20} color="inherit" /> : 'Sí, Borrar Turnos de esta Semana'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Modal Deshabilitar / Bloquear Slot Individual */}
      <Dialog open={!!selectedSlotBlock} onClose={() => setSelectedSlotBlock(null)}>
        <DialogTitle>¿Deshabilitar este turno individual?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Vas a deshabilitar el turno de <strong>{selectedSlotBlock?.especialidadNombre || 'General'}</strong> del día <strong>{dayjs(selectedSlotBlock?.dateStr).format('DD/MM/YYYY')}</strong> de <strong>{selectedSlotBlock?.startText} hs a {selectedSlotBlock?.endText} hs</strong>. Este slot no estará disponible para reserva del paciente.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedSlotBlock(null)}>Cancelar</Button>
          <Button onClick={handleBloquearSlotIndividual} color="warning" variant="contained" disabled={blocking} startIcon={<BlockIcon />}>
            {blocking ? <CircularProgress size={20} color="inherit" /> : 'Deshabilitar Turno'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Modal Configurar / Editar Franja Horaria ⚙️ */}
      <Dialog open={!!editingHorario} onClose={() => setEditingHorario(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Configurar Parámetros de la Franja Horaria ⚙️</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2}>
            {doctorInfo?.especialidades && doctorInfo.especialidades.length > 0 && (
              <Grid item xs={12}>
                <FormControl fullWidth>
                  <InputLabel>Especialidad Médica</InputLabel>
                  <Select
                    value={editFormData.especialidadId}
                    onChange={(e) => setEditFormData({ ...editFormData, especialidadId: e.target.value })}
                    label="Especialidad Médica"
                  >
                    {doctorInfo.especialidades.map((esp) => (
                      <MenuItem key={esp.id} value={esp.id}>
                        {esp.nombre}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
            )}

            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Hora Inicio"
                type="time"
                value={editFormData.horaInicio}
                onChange={(e) => setEditFormData({ ...editFormData, horaInicio: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Hora Fin"
                type="time"
                value={editFormData.horaFin}
                onChange={(e) => setEditFormData({ ...editFormData, horaFin: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Duración de Turno (Minutos)"
                type="number"
                value={editFormData.duracionTurnoMinutos}
                onChange={(e) => setEditFormData({ ...editFormData, duracionTurnoMinutos: e.target.value })}
              />
            </Grid>

            {!editingHorario?.fecha && (
              <>
                <Grid item xs={6}>
                  <TextField
                    fullWidth
                    label="Vigencia Desde (Opcional)"
                    type="date"
                    value={editFormData.fechaDesde}
                    onChange={(e) => setEditFormData({ ...editFormData, fechaDesde: e.target.value })}
                    inputProps={{ min: todayStr }}
                    InputLabelProps={{ shrink: true }}
                  />
                </Grid>
                <Grid item xs={6}>
                  <TextField
                    fullWidth
                    label="Vigencia Hasta (Opcional)"
                    type="date"
                    value={editFormData.fechaHasta}
                    onChange={(e) => setEditFormData({ ...editFormData, fechaHasta: e.target.value })}
                    inputProps={{ min: todayStr }}
                    InputLabelProps={{ shrink: true }}
                  />
                </Grid>
              </>
            )}
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditingHorario(null)}>Cancelar</Button>
          <Button onClick={handleSaveEditHorario} variant="contained" disabled={saving}>
            {saving ? <CircularProgress size={20} color="inherit" /> : 'Guardar Configuración'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Modal Confirmar Eliminación de Franja Completa */}
      <Dialog open={!!selectedHorarioDelete} onClose={() => setSelectedHorarioDelete(null)}>
        <DialogTitle>¿Eliminar esta franja horaria completa?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Vas a borrar toda la franja de <strong>{selectedHorarioDelete?.especialidad ? selectedHorarioDelete.especialidad.nombre : 'General'}</strong> ({selectedHorarioDelete?.diaSemana || selectedHorarioDelete?.fecha}: {selectedHorarioDelete?.horaInicio} hs - {selectedHorarioDelete?.horaFin} hs).
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedHorarioDelete(null)}>Cancelar</Button>
          <Button onClick={handleConfirmEliminarHorario} color="error" variant="contained" disabled={deleting}>
            {deleting ? <CircularProgress size={20} color="inherit" /> : 'Sí, Eliminar Franja'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DoctorHorarios;
