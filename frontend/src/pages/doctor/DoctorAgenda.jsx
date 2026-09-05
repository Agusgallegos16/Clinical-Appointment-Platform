import React, { useEffect, useState, useMemo } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  TextField,
  Chip,
  Button,
  CircularProgress,
  Alert,
  Paper,
  Tabs,
  Tab,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Avatar,
} from '@mui/material';
import {
  CheckCircle as CheckIcon,
  Cancel as CancelIcon,
  PersonOff as PersonOffIcon,
  Block as BlockIcon,
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { doctorService } from '../../api/doctorService';
import { turnoService } from '../../api/turnoService';
import { pacienteService } from '../../api/pacienteService';
import dayjs from 'dayjs';

import GoogleCalendarButton from '../../components/GoogleCalendarButton';

const DoctorAgenda = () => {
  const { entidadId } = useAuth();
  const [fecha, setFecha] = useState(dayjs().format('YYYY-MM-DD'));
  const [turnos, setTurnos] = useState([]);
  const [slotsDisponibles, setSlotsDisponibles] = useState([]);
  const [tabFiltro, setTabFiltro] = useState(0); // 0: Todos, 1: Reservados, 2: Libres

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [updatingId, setUpdatingId] = useState(null);

  // Estado para modal de cancelación con justificación obligatoria
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false);
  const [turnoACancelar, setTurnoACancelar] = useState(null);
  const [motivoCancelacion, setMotivoCancelacion] = useState('');
  const [cancelError, setCancelError] = useState('');
  const [submittingCancel, setSubmittingCancel] = useState(false);

  // Estado para modal de ficha y estadísticas del paciente
  const [pacienteModalOpen, setPacienteModalOpen] = useState(false);
  const [pacienteStats, setPacienteStats] = useState(null);
  const [loadingStats, setLoadingStats] = useState(false);
  const [statsError, setStatsError] = useState('');

  useEffect(() => {
    if (entidadId) cargarAgenda(fecha);
  }, [entidadId, fecha]);

  const cargarAgenda = async (fechaSeleccionada) => {
    setLoading(true);
    setError('');
    try {
      const [dataTurnos, dataSlots] = await Promise.all([
        doctorService.obtenerAgenda(entidadId, fechaSeleccionada).catch(() => []),
        doctorService.obtenerDisponibilidad(entidadId, fechaSeleccionada).catch(() => []),
      ]);
      setTurnos(dataTurnos || []);
      setSlotsDisponibles(dataSlots || []);
    } catch (err) {
      setError('Error al consultar la agenda médica.');
    } finally {
      setLoading(false);
    }
  };

  const handleDeshabilitarSlot = async (slotId) => {
    if (!slotId) return;
    setUpdatingId(`slot-${slotId}`);
    try {
      await doctorService.eliminarSlot(slotId);
      cargarAgenda(fecha);
    } catch (err) {
      console.error('Error al deshabilitar slot:', err);
      setError('No se pudo deshabilitar el horario libre.');
    } finally {
      setUpdatingId(null);
    }
  };

  const handleCambiarEstado = async (turnoId, nuevoEstado) => {
    setUpdatingId(turnoId);
    try {
      await turnoService.cambiarEstado(turnoId, nuevoEstado);
      cargarAgenda(fecha);
    } catch (err) {
      setError('No se pudo actualizar el estado del turno.');
    } finally {
      setUpdatingId(null);
    }
  };

  const handleOpenCancelDialog = (turno) => {
    setTurnoACancelar(turno);
    setMotivoCancelacion('');
    setCancelError('');
    setCancelDialogOpen(true);
  };

  const handleCloseCancelDialog = () => {
    if (submittingCancel) return;
    setCancelDialogOpen(false);
    setTurnoACancelar(null);
    setMotivoCancelacion('');
    setCancelError('');
  };

  const handleConfirmarCancelacionDoctor = async () => {
    if (!motivoCancelacion.trim()) {
      setCancelError('La justificación de cancelación es obligatoria.');
      return;
    }

    setSubmittingCancel(true);
    setCancelError('');
    try {
      await turnoService.cancelarPorDoctor(turnoACancelar.id, motivoCancelacion.trim());
      handleCloseCancelDialog();
      cargarAgenda(fecha);
    } catch (err) {
      setCancelError(err.response?.data?.mensaje || 'Error al procesar la cancelación.');
    } finally {
      setSubmittingCancel(false);
    }
  };

  const handleOpenPacienteModal = async (pacienteId) => {
    setPacienteModalOpen(true);
    setLoadingStats(true);
    setStatsError('');
    setPacienteStats(null);
    try {
      const data = await pacienteService.obtenerEstadisticas(pacienteId);
      setPacienteStats(data);
    } catch (err) {
      setStatsError('No se pudo cargar la información y estadísticas del paciente.');
    } finally {
      setLoadingStats(false);
    }
  };

  const handleClosePacienteModal = () => {
    setPacienteModalOpen(false);
    setPacienteStats(null);
  };

  const getChipColor = (estado) => {
    switch (estado) {
      case 'COMPLETADO': return 'success';
      case 'AUSENTE': return 'warning';
      case 'CANCELADO': return 'error';
      default: return 'primary';
    }
  };

  const turnosReservadosActivos = useMemo(() => {
    return Array.isArray(turnos) ? turnos.filter((t) => t && t.estado !== 'CANCELADO') : [];
  }, [turnos]);

  const slotsLibresActivos = useMemo(() => {
    return Array.isArray(slotsDisponibles) ? slotsDisponibles.filter((s) => s && s.disponible !== false) : [];
  }, [slotsDisponibles]);

  // Construir lista de todos los items (Reservados + Libres) ordenada por hora
  const listaTodos = useMemo(() => {
    const lista = [];

    // Turnos reservados
    if (Array.isArray(turnos)) {
      turnos.forEach((t) => {
        if (t && t.estado !== 'CANCELADO') {
          lista.push({
            tipo: 'RESERVADO',
            hora: t.fechaHora ? dayjs(t.fechaHora).format('HH:mm') : '00:00',
            fechaHora: t.fechaHora || fecha,
            datos: t,
          });
        }
      });
    }

    // Slots libres disponibles
    if (Array.isArray(slotsDisponibles)) {
      slotsDisponibles.forEach((s) => {
        if (!s || s.disponible === false) return;
        const horaRaw = s.hora;
        let horaStr = '00:00';
        if (typeof horaRaw === 'string') {
          horaStr = horaRaw;
        } else if (Array.isArray(horaRaw)) {
          const h = String(horaRaw[0]).padStart(2, '0');
          const m = String(horaRaw[1]).padStart(2, '0');
          horaStr = `${h}:${m}`;
        } else if (horaRaw) {
          horaStr = String(horaRaw);
        }
        const horaCorta = horaStr.substring(0, 5);
        const fullIsoFechaHora = `${fecha}T${horaStr.length === 5 ? horaStr + ':00' : horaStr}`;

        lista.push({
          tipo: 'HORARIO LIBRE',
          hora: horaCorta,
          fechaHora: fullIsoFechaHora,
          datos: s,
        });
      });
    }

    // Ordenar cronológicamente
    lista.sort((a, b) => dayjs(a.fechaHora).valueOf() - dayjs(b.fechaHora).valueOf());
    return lista;
  }, [turnos, slotsDisponibles, fecha]);

  // Filtrar según la pestaña activa
  const listaCombinada = useMemo(() => {
    if (tabFiltro === 1) return listaTodos.filter((i) => i.tipo === 'RESERVADO');
    if (tabFiltro === 2) return listaTodos.filter((i) => i.tipo === 'HORARIO LIBRE');
    return listaTodos;
  }, [listaTodos, tabFiltro]);

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="flex-start" flexWrap="wrap" gap={2} mb={3}>
        <Box>
          <Typography variant="h4" fontWeight={700} color="primary" mb={1}>
            Mi Agenda
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Consultá la lista completa de turnos para el día.
          </Typography>
        </Box>
        <GoogleCalendarButton />
      </Box>

      <Paper sx={{ p: 2.5, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={6} md={4}>
            <TextField
              fullWidth
              label="Fecha de la Agenda"
              type="date"
              value={fecha}
              onChange={(e) => setFecha(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={8}>
            <Typography variant="body2" color="text.secondary">
              Mostrando agenda para el <strong>{dayjs(fecha).format('DD/MM/YYYY')}</strong>: {turnosReservadosActivos.length} reservados, {slotsLibresActivos.length} libres.
            </Typography>
          </Grid>
        </Grid>
      </Paper>

      {/* Pestañas de Filtrado */}
      <Paper sx={{ mb: 3, p: 0.5, borderRadius: 2 }}>
        <Tabs
          value={tabFiltro}
          onChange={(e, val) => setTabFiltro(val)}
          indicatorColor="primary"
          textColor="primary"
          variant="fullWidth"
        >
          <Tab label={`Todos (${listaTodos.length})`} />
          <Tab label={`Reservados (${turnosReservadosActivos.length})`} />
          <Tab label={`Libres (${slotsLibresActivos.length})`} />
        </Tabs>
      </Paper>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {loading ? (
        <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
      ) : listaCombinada.length === 0 ? (
        <Alert severity="info">No poseés citas médicas ni horarios libres configurados para esta fecha.</Alert>
      ) : (
        <Grid container spacing={2.5}>
          {listaCombinada.map((item, index) => {
            const esReservado = item.tipo === 'RESERVADO';
            const turno = esReservado && item.datos ? item.datos : {};
            const slot = !esReservado && item.datos ? item.datos : {};

            if (esReservado) {
              const isPending = turno.estado === 'CONFIRMADO' || turno.estado === 'PENDIENTE';
              const borderColors = {
                CONFIRMADO: '#0284c7',
                PENDIENTE: '#0284c7',
                COMPLETADO: '#22c55e',
                AUSENTE: '#f59e0b',
                CANCELADO: '#ef4444',
              };

              return (
                <Grid item xs={12} key={`turno-${turno.id}`}>
                  <Card sx={{ borderLeft: `6px solid ${borderColors[turno.estado] || '#0284c7'}`, opacity: isPending ? 1 : 0.88 }}>
                    <CardContent sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, justifyContent: 'space-between', alignItems: { sm: 'center' }, gap: 2 }}>
                      <Box>
                        <Box display="flex" alignItems="center" gap={1.5} mb={0.5}>
                          <Typography variant="h6" fontWeight={700} color="primary">
                            {item.hora} hs
                          </Typography>
                          <Chip
                            label={`RESERVADO: ${turno.estado || 'CONFIRMADO'}`}
                            color={getChipColor(turno.estado)}
                            size="small"
                            sx={{ fontWeight: 700 }}
                          />
                        </Box>

                        <Typography
                          variant="subtitle1"
                          fontWeight={700}
                          onClick={() => handleOpenPacienteModal(turno.pacienteId)}
                          sx={{
                            cursor: 'pointer',
                            color: 'primary.main',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 0.6,
                            transition: '0.2s',
                            '&:hover': { textDecoration: 'underline', color: 'primary.dark' },
                          }}
                          title="Ver ficha del paciente e historial de asistencias"
                        >
                          👤 Paciente: {turno.pacienteNombre}
                        </Typography>

                        <Typography variant="body2" color="text.secondary">
                          🏥 <strong>Especialidad: </strong> {turno.especialidadNombre || 'Consulta General'}
                        </Typography>
                        <Typography variant="body2" color="text.secondary" mt={0.5}>
                          💳 <strong>Cobertura Indicada:</strong> {turno.obraSocial || (turno.tieneObraSocial ? 'Obra Social' : 'Particular / Sin Obra Social')}
                        </Typography>
                        <Typography variant="body2" color="text.secondary" mt={0.5}>
                          💬 <strong>Motivo:</strong> {turno.motivoConsulta || 'Sin especificar'}
                        </Typography>

                        {turno.estado === 'CANCELADO' && turno.motivoCancelacion && (
                          <Typography variant="body2" color="error" mt={0.5} fontWeight={600}>
                            ❌ <strong>Justificación de Cancelación:</strong> {turno.motivoCancelacion}
                          </Typography>
                        )}

                        {turno.googleEventId && (
                          <Chip
                            label="📅 Sincronizado en Google Calendar"
                            size="small"
                            color="info"
                            variant="outlined"
                            sx={{ mt: 1, fontWeight: 500 }}
                          />
                        )}
                      </Box>

                      {turno.estado !== 'COMPLETADO' && turno.estado !== 'CANCELADO' && turno.estado !== 'AUSENTE' && (
                        <Box display="flex" gap={1} flexWrap="wrap">
                          <Button
                            variant="contained"
                            color="success"
                            size="small"
                            startIcon={<CheckIcon />}
                            disabled={updatingId === turno.id}
                            onClick={() => handleCambiarEstado(turno.id, 'COMPLETADO')}
                          >
                            Completar
                          </Button>

                          <Button
                            variant="outlined"
                            color="warning"
                            size="small"
                            startIcon={<PersonOffIcon />}
                            disabled={updatingId === turno.id}
                            onClick={() => handleCambiarEstado(turno.id, 'AUSENTE')}
                          >
                            Marcar Ausente
                          </Button>

                          <Button
                            variant="outlined"
                            color="error"
                            size="small"
                            startIcon={<CancelIcon />}
                            disabled={updatingId === turno.id}
                            onClick={() => handleOpenCancelDialog(turno)}
                          >
                            Cancelar
                          </Button>
                        </Box>
                      )}
                    </CardContent>
                  </Card>
                </Grid>
              );
            }

            // Renderizar Tarjeta de Horario Libre
            return (
              <Grid item xs={12} key={`slot-${index}`}>
                <Card sx={{ borderLeft: '6px solid #22c55e', borderRadius: 2 }}>
                  <CardContent sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, justifyContent: 'space-between', alignItems: { sm: 'center' }, gap: 2 }}>
                    <Box>
                      <Box display="flex" alignItems="center" gap={1.5} mb={0.5}>
                        <Typography variant="h6" fontWeight={700} color="primary">
                          {item.hora} hs
                        </Typography>
                        <Chip
                          label="HORARIO LIBRE"
                          color="success"
                          size="small"
                          sx={{ fontWeight: 700 }}
                        />
                      </Box>
                      <Typography variant="body2" fontWeight={600} color="text.primary">
                        🏥 Especialidad: {slot.especialidadNombre || 'Consulta General (Cualquier especialidad)'}
                      </Typography>
                    </Box>

                    {slot?.id && (
                      <Button
                        variant="outlined"
                        color="error"
                        size="small"
                        startIcon={<BlockIcon />}
                        disabled={updatingId === `slot-${slot.id}`}
                        onClick={() => handleDeshabilitarSlot(slot.id)}
                      >
                        {updatingId === `slot-${slot.id}` ? <CircularProgress size={20} color="inherit" /> : 'Deshabilitar Turno'}
                      </Button>
                    )}
                  </CardContent>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      )}

      {/* Modal Justificación de Cancelación Obligatoria */}
      <Dialog open={cancelDialogOpen} onClose={handleCloseCancelDialog} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ color: 'error.main', fontWeight: 700 }}>
          Cancelar Cita Médica
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" mb={2}>
            Por favor ingresá el motivo de la cancelación. Esta justificación será enviada por correo al paciente.
          </Typography>
          {cancelError && <Alert severity="error" sx={{ mb: 2 }}>{cancelError}</Alert>}
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Motivo de Cancelación"
            value={motivoCancelacion}
            onChange={(e) => setMotivoCancelacion(e.target.value)}
            required
            placeholder="Ej: Ausencia imprevista del profesional..."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseCancelDialog} color="inherit" disabled={submittingCancel}>
            Volver
          </Button>
          <Button
            variant="contained"
            color="error"
            onClick={handleConfirmarCancelacionDoctor}
            disabled={submittingCancel || !motivoCancelacion.trim()}
          >
            {submittingCancel ? <CircularProgress size={20} color="inherit" /> : 'Confirmar Cancelación'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Modal Ficha y Estadísticas del Paciente */}
      <Dialog open={pacienteModalOpen} onClose={handleClosePacienteModal} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 700, pb: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
          <Avatar sx={{ bgcolor: 'primary.main', width: 32, height: 32 }}>👤</Avatar>
          Ficha e Historial del Paciente
        </DialogTitle>
        <DialogContent dividers>
          {loadingStats ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : statsError ? (
            <Alert severity="error">{statsError}</Alert>
          ) : pacienteStats ? (
            <Box display="flex" flexDirection="column" gap={2}>
              <Box p={2} bgcolor="action.hover" borderRadius={2} border="1px solid" borderColor="divider">
                <Typography variant="h6" fontWeight={700} color="primary">
                  {pacienteStats.nombre} {pacienteStats.apellido}
                </Typography>
                <Typography variant="body2" color="text.secondary" mt={0.5}>
                  🪪 <strong>DNI:</strong> {pacienteStats.dni || 'Sin especificar'} | 📞 <strong>Teléfono:</strong> {pacienteStats.telefono || 'Sin registrar'}
                </Typography>
                <Typography variant="body2" color="text.secondary" mt={0.5}>
                  📧 <strong>Email:</strong> {pacienteStats.email || 'Sin cuenta web'}
                </Typography>
                {pacienteStats.fechaNacimiento && (
                  <Typography variant="body2" color="text.secondary" mt={0.5}>
                    🎂 <strong>Nacimiento:</strong> {dayjs(pacienteStats.fechaNacimiento).format('DD/MM/YYYY')} {pacienteStats.edad ? `(${pacienteStats.edad} años)` : ''}
                  </Typography>
                )}
              </Box>

              <Typography variant="subtitle2" fontWeight={700} color="text.primary">
                📊 Distribución de Asistencia ({pacienteStats.totalTurnos} Citas Totales):
              </Typography>

              {/* Barra Multicolor de Distribución */}
              <Box sx={{ width: '100%', height: 24, borderRadius: 12, bgcolor: 'action.hover', overflow: 'hidden', display: 'flex', my: 1.5, boxShadow: 'inset 0 1px 2px rgba(0,0,0,0.08)' }}>
                <Box sx={{ width: `${pacienteStats.porcentajeCompletados || 0}%`, bgcolor: '#22c55e', transition: '0.3s' }} title={`Completados: ${pacienteStats.porcentajeCompletados || 0}%`} />
                <Box sx={{ width: `${pacienteStats.porcentajeAusentes || 0}%`, bgcolor: '#f59e0b', transition: '0.3s' }} title={`Ausentes: ${pacienteStats.porcentajeAusentes || 0}%`} />
                <Box sx={{ width: `${pacienteStats.porcentajeCancelados || 0}%`, bgcolor: '#ef4444', transition: '0.3s' }} title={`Cancelados: ${pacienteStats.porcentajeCancelados || 0}%`} />
                <Box sx={{ width: `${pacienteStats.porcentajePendientes || 0}%`, bgcolor: '#0284c7', transition: '0.3s' }} title={`Pendientes: ${pacienteStats.porcentajePendientes || 0}%`} />
              </Box>

              {/* Leyendas con Puntos Indicadores */}
              <Grid container spacing={1.5} mt={0.5}>
                <Grid item xs={6} sm={3}>
                  <Box display="flex" alignItems="center" gap={1.2}>
                    <span style={{ width: 18, height: 18, borderRadius: '50%', backgroundColor: '#22c55e', display: 'inline-block', flexShrink: 0, boxShadow: '0 1px 3px rgba(0,0,0,0.15)' }}></span>
                    <Box>
                      <Typography variant="body2" fontWeight={700}>
                        {pacienteStats.porcentajeCompletados || 0}%
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Completadas ({pacienteStats.totalCompletados || 0})
                      </Typography>
                    </Box>
                  </Box>
                </Grid>
                <Grid item xs={6} sm={3}>
                  <Box display="flex" alignItems="center" gap={1.2}>
                    <span style={{ width: 18, height: 18, borderRadius: '50%', backgroundColor: '#f59e0b', display: 'inline-block', flexShrink: 0, boxShadow: '0 1px 3px rgba(0,0,0,0.15)' }}></span>
                    <Box>
                      <Typography variant="body2" fontWeight={700}>
                        {pacienteStats.porcentajeAusentes || 0}%
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Ausencias ({pacienteStats.totalAusentes || 0})
                      </Typography>
                    </Box>
                  </Box>
                </Grid>
                <Grid item xs={6} sm={3}>
                  <Box display="flex" alignItems="center" gap={1.2}>
                    <span style={{ width: 18, height: 18, borderRadius: '50%', backgroundColor: '#ef4444', display: 'inline-block', flexShrink: 0, boxShadow: '0 1px 3px rgba(0,0,0,0.15)' }}></span>
                    <Box>
                      <Typography variant="body2" fontWeight={700}>
                        {pacienteStats.porcentajeCancelados || 0}%
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Cancelados ({pacienteStats.totalCancelados || 0})
                      </Typography>
                    </Box>
                  </Box>
                </Grid>
                <Grid item xs={6} sm={3}>
                  <Box display="flex" alignItems="center" gap={1.2}>
                    <span style={{ width: 18, height: 18, borderRadius: '50%', backgroundColor: '#0284c7', display: 'inline-block', flexShrink: 0, boxShadow: '0 1px 3px rgba(0,0,0,0.15)' }}></span>
                    <Box>
                      <Typography variant="body2" fontWeight={700}>
                        {pacienteStats.porcentajePendientes || 0}%
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Pendientes ({pacienteStats.totalPendientes || 0})
                      </Typography>
                    </Box>
                  </Box>
                </Grid>
              </Grid>
            </Box>
          ) : null}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClosePacienteModal} color="primary" variant="contained">
            Cerrar Ficha
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DoctorAgenda;
