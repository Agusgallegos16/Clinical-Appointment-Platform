import React, { useEffect, useState, useMemo, useRef } from 'react';
import FullCalendar from '@fullcalendar/react';
import timeGridPlugin from '@fullcalendar/timegrid';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
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
  Tabs,
  Tab,
} from '@mui/material';
import {
  Add as AddIcon,
  Schedule as ScheduleIcon,
  Delete as DeleteIcon,
  FolderCopy as TemplateIcon,
  CalendarMonth as CalendarIcon,
  Info as InfoIcon,
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { doctorService } from '../../api/doctorService';
import dayjs from 'dayjs';

const diasSemana = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'];

const dayOfWeekToNumber = {
  LUNES: 1,
  MARTES: 2,
  MIERCOLES: 3,
  JUEVES: 4,
  VIERNES: 5,
  SABADO: 6,
  DOMINGO: 0,
};

const DoctorHorarios = () => {
  const { entidadId } = useAuth();
  const calendarRef = useRef(null);

  const [horarios, setHorarios] = useState([]);
  const [plantillas, setPlantillas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Filtros
  const [filterTab, setFilterTab] = useState(0); // 0: Todos, 1: Recurrentes, 2: Puntuales

  // Rango de fechas visible en el calendario para proyectar recurrentes
  const [currentStartStr, setCurrentStartStr] = useState(dayjs().startOf('week').format('YYYY-MM-DD'));
  const [currentEndStr, setCurrentEndStr] = useState(dayjs().endOf('week').format('YYYY-MM-DD'));

  // Formulario nuevo horario manual
  const [tipoHorario, setTipoHorario] = useState('RECURRENTE');
  const [diaSemana, setDiaSemana] = useState('LUNES');
  const [fechaPuntual, setFechaPuntual] = useState('');
  const [horaInicio, setHoraInicio] = useState('09:00');
  const [horaFin, setHoraFin] = useState('13:00');
  const [duracionTurno, setDuracionTurno] = useState(30);

  // Formulario aplicar plantilla
  const [selectedPlantillaId, setSelectedPlantillaId] = useState('');
  const [tipoAplicacion, setTipoAplicacion] = useState('RECURRENTE');
  const [diaAplicar, setDiaAplicar] = useState('LUNES');
  const [fechaAplicar, setFechaAplicar] = useState('');
  const [applying, setApplying] = useState(false);

  // Modal para eliminar horario
  const [selectedHorarioDelete, setSelectedHorarioDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (entidadId) {
      cargarHorarios();
      cargarPlantillas();
    }
  }, [entidadId]);

  const cargarHorarios = async () => {
    try {
      const data = await doctorService.obtenerHorarios(entidadId);
      setHorarios(data);
    } catch (err) {
      setError('Error al obtener la lista de horarios.');
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

  // Mapear los horarios de backend a eventos de FullCalendar
  const calendarEvents = useMemo(() => {
    const events = [];

    horarios.forEach((h) => {
      if (h.fecha) {
        // Horario puntual en fecha específica
        events.push({
          id: `h-${h.id}`,
          title: `Atención Puntual (${h.duracionTurnoMinutos}m/turno)`,
          start: `${h.fecha}T${h.horaInicio}`,
          end: `${h.fecha}T${h.horaFin}`,
          backgroundColor: '#0d9488', // Teal para fecha puntual
          borderColor: '#0f766e',
          extendedProps: { originalData: h },
        });
      } else if (h.diaSemana) {
        // Horario semanal recurrente proyectado en la semana visible del calendario
        const targetDayNum = dayOfWeekToNumber[h.diaSemana];
        let curr = dayjs(currentStartStr);
        const end = dayjs(currentEndStr);

        while (curr.isBefore(end) || curr.isSame(end, 'day')) {
          if (curr.day() === targetDayNum) {
            const dateStr = curr.format('YYYY-MM-DD');
            events.push({
              id: `h-${h.id}-${dateStr}`,
              title: `Atención Semanal (${h.duracionTurnoMinutos}m/turno)`,
              start: `${dateStr}T${h.horaInicio}`,
              end: `${dateStr}T${h.horaFin}`,
              backgroundColor: '#0284c7', // Azul para recurrente
              borderColor: '#0369a1',
              extendedProps: { originalData: h },
            });
          }
          curr = curr.add(1, 'day');
        }
      }
    });

    return events;
  }, [horarios, currentStartStr, currentEndStr]);

  const handleDatesSet = (dateInfo) => {
    setCurrentStartStr(dayjs(dateInfo.start).format('YYYY-MM-DD'));
    setCurrentEndStr(dayjs(dateInfo.end).format('YYYY-MM-DD'));
  };

  const handleGuardarHorario = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setSuccess('');

    try {
      const payload = {
        horaInicio,
        horaFin,
        duracionTurnoMinutos: Number(duracionTurno),
      };

      if (tipoHorario === 'RECURRENTE') {
        payload.diaSemana = diaSemana;
      } else {
        payload.fecha = fechaPuntual;
      }

      await doctorService.agregarHorario(entidadId, payload);
      setSuccess('¡Horario de atención agregado exitosamente!');
      cargarHorarios();
    } catch (err) {
      setError(err.response?.data?.mensaje || err.response?.data?.message || 'Error al guardar el horario.');
    } finally {
      setSaving(false);
    }
  };

  const handleAplicarPlantilla = async (e) => {
    e.preventDefault();
    if (!selectedPlantillaId) return;
    setApplying(true);
    setError('');
    setSuccess('');

    try {
      const payload = { plantillaId: Number(selectedPlantillaId) };
      if (tipoAplicacion === 'RECURRENTE') {
        payload.diaSemana = diaAplicar;
      } else {
        payload.fecha = fechaAplicar;
      }

      await doctorService.aplicarPlantilla(entidadId, payload);
      setSuccess('¡Plantilla aplicada correctamente! Se han actualizado tus horarios.');
      cargarHorarios();
    } catch (err) {
      setError('Error al aplicar la plantilla.');
    } finally {
      setApplying(false);
    }
  };

  const handleConfirmEliminarHorario = async () => {
    if (!selectedHorarioDelete) return;
    setDeleting(true);
    setError('');
    try {
      await doctorService.eliminarHorario(selectedHorarioDelete.id);
      setSelectedHorarioDelete(null);
      setSuccess('Horario eliminado con éxito.');
      cargarHorarios();
    } catch (err) {
      setError('No se pudo eliminar el horario.');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} color="primary" mb={1}>
        Gestión de Horarios de Atención
      </Typography>
      <Typography variant="body1" color="text.secondary" mb={2}>
        Navegá semana a semana por tu calendario interactivo por horas y configurá tus disponibilidades de atención.
      </Typography>

      <Alert severity="info" icon={<InfoIcon />} sx={{ mb: 3 }}>
        <strong>Prioridad de Fechas Puntuales:</strong> Si configurás un horario puntual para una fecha específica (ej: 12/08), esa fecha tomará prioridad por sobre el horario semanal estándar. No podés guardar franjas que se superpongan en el mismo día.
      </Alert>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

      {/* CALENDARIO INTERACTIVO FULLCALENDAR POR HORAS Y NAVEGACIÓN SEMANAL */}
      <Paper sx={{ p: 3, mb: 4, bgcolor: 'background.paper' }}>
        <Box display="flex" alignItems="center" gap={1.5} mb={2}>
          <CalendarIcon color="primary" sx={{ fontSize: 30 }} />
          <Typography variant="h6" fontWeight={700} color="primary">
            Calendario Semanal de Atención Médica
          </Typography>
        </Box>

        <Box sx={{ '.fc': { fontFamily: 'inherit' } }}>
          <FullCalendar
            ref={calendarRef}
            plugins={[timeGridPlugin, dayGridPlugin, interactionPlugin]}
            initialView="timeGridWeek"
            headerToolbar={{
              left: 'prev,next today',
              center: 'title',
              right: 'timeGridWeek,dayGridMonth',
            }}
            buttonText={{
              today: 'Hoy',
              week: 'Semana',
              month: 'Mes',
            }}
            locale="es"
            slotMinTime="07:00:00"
            slotMaxTime="22:00:00"
            allDaySlot={false}
            events={calendarEvents}
            datesSet={handleDatesSet}
            height="520px"
            eventClick={(info) => {
              const original = info.event.extendedProps.originalData;
              if (original) setSelectedHorarioDelete(original);
            }}
          />
        </Box>
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
                  <FormControl fullWidth margin="dense">
                    <InputLabel>Día de la Semana</InputLabel>
                    <Select
                      value={diaAplicar}
                      onChange={(e) => setDiaAplicar(e.target.value)}
                      label="Día de la Semana"
                    >
                      {diasSemana.map((d) => (
                        <MenuItem key={d} value={d}>{d}</MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                ) : (
                  <TextField
                    fullWidth
                    margin="dense"
                    label="Fecha Específica"
                    type="date"
                    value={fechaAplicar}
                    onChange={(e) => setFechaAplicar(e.target.value)}
                    required
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

          {/* Opción 2: Agregar Horario Manual */}
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" fontWeight={600} mb={2}>
              Agregar Horario Manual
            </Typography>

            <Box component="form" onSubmit={handleGuardarHorario}>
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
                <FormControl fullWidth margin="normal">
                  <InputLabel>Día de la Semana</InputLabel>
                  <Select
                    value={diaSemana}
                    onChange={(e) => setDiaSemana(e.target.value)}
                    label="Día de la Semana"
                  >
                    {diasSemana.map((d) => (
                      <MenuItem key={d} value={d}>{d}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              ) : (
                <TextField
                  fullWidth
                  margin="normal"
                  label="Fecha Puntual"
                  type="date"
                  value={fechaPuntual}
                  onChange={(e) => setFechaPuntual(e.target.value)}
                  required
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
                {saving ? <CircularProgress size={24} color="inherit" /> : 'Guardar Horario Manual'}
              </Button>
            </Box>
          </Paper>
        </Grid>

        {/* Lista de Horarios Existentes */}
        <Grid item xs={12} md={7}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" fontWeight={600} mb={2}>
              Franjas Horarias Configuradas ({horarios.length})
            </Typography>

            {loading ? (
              <CircularProgress />
            ) : horarios.length === 0 ? (
              <Alert severity="info">No hay horarios configurados.</Alert>
            ) : (
              <Grid container spacing={2}>
                {horarios.map((h) => (
                  <Grid item xs={12} key={h.id}>
                    <Card variant="outlined">
                      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                        <Box display="flex" justifyContent="space-between" alignItems="center">
                          <Box display="flex" alignItems="center" gap={1}>
                            <ScheduleIcon color={h.fecha ? 'secondary' : 'primary'} />
                            <Box>
                              <Typography fontWeight={700}>
                                {h.diaSemana ? `Todos los ${h.diaSemana}` : `Fecha Puntual: ${h.fecha}`}
                              </Typography>
                              <Typography variant="caption" color="text.secondary">
                                {h.horaInicio} hs a {h.horaFin} hs ({h.duracionTurnoMinutos} min/turno)
                              </Typography>
                            </Box>
                          </Box>

                          <IconButton
                            color="error"
                            onClick={() => setSelectedHorarioDelete(h)}
                            title="Eliminar este horario"
                          >
                            <DeleteIcon />
                          </IconButton>
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

      {/* Modal Confirmar Eliminación */}
      <Dialog open={!!selectedHorarioDelete} onClose={() => setSelectedHorarioDelete(null)}>
        <DialogTitle>¿Eliminar este horario de atención?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Vas a borrar la franja horaria ({selectedHorarioDelete?.diaSemana || selectedHorarioDelete?.fecha}: {selectedHorarioDelete?.horaInicio} hs - {selectedHorarioDelete?.horaFin} hs). Esta acción eliminará los slots disponibles para esa franja.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedHorarioDelete(null)}>Cancelar</Button>
          <Button onClick={handleConfirmEliminarHorario} color="error" variant="contained" disabled={deleting}>
            {deleting ? <CircularProgress size={20} color="inherit" /> : 'Sí, Eliminar'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DoctorHorarios;
