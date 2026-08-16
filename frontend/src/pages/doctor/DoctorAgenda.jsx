import React, { useEffect, useState } from 'react';
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
  Divider,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Avatar,
} from '@mui/material';
import {
  CheckCircle as CheckIcon,
  Cancel as CancelIcon,
  Person as PersonIcon,
  PersonOff as PersonOffIcon,
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

  const ordenarTurnosAgenda = (turnosList) => {
    return [...turnosList]
      .filter((t) => t.estado !== 'CANCELADO')
      .sort((a, b) => {
        const isAPending = a.estado === 'CONFIRMADO' || a.estado === 'PENDIENTE';
        const isBPending = b.estado === 'CONFIRMADO' || b.estado === 'PENDIENTE';

        if (isAPending && !isBPending) return -1;
        if (!isAPending && isBPending) return 1;
        return dayjs(a.fechaHora).valueOf() - dayjs(b.fechaHora).valueOf();
      });
  };

  const cargarAgenda = async (fechaSeleccionada) => {
    setLoading(true);
    setError('');
    try {
      const data = await doctorService.obtenerAgenda(entidadId, fechaSeleccionada);
      setTurnos(ordenarTurnosAgenda(data));
    } catch (err) {
      setError('Error al consultar la agenda médica.');
    } finally {
      setLoading(false);
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

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="flex-start" flexWrap="wrap" gap={2} mb={3}>
        <Box>
          <Typography variant="h4" fontWeight={700} color="primary" mb={1}>
            Mi Agenda
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Consultá la lista de pacientes y turnos confirmados para el día seleccionado.
          </Typography>
        </Box>
        <GoogleCalendarButton />
      </Box>

      <Paper sx={{ p: 2.5, mb: 4 }}>
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
          <Grid item xs={12} sm={6}>
            <Typography variant="body2" color="text.secondary">
              Mostrando turnos para el <strong>{dayjs(fecha).format('DD/MM/YYYY')}</strong> ({turnos.length} citas)
            </Typography>
          </Grid>
        </Grid>
      </Paper>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {loading ? (
        <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
      ) : turnos.length === 0 ? (
        <Alert severity="info">No poseés citas médicas para esta fecha.</Alert>
      ) : (
        <Grid container spacing={2.5}>
          {turnos.map((turno) => {
            const isPending = turno.estado === 'CONFIRMADO' || turno.estado === 'PENDIENTE';
            const borderColors = {
              CONFIRMADO: '#0284c7',
              PENDIENTE: '#0284c7',
              COMPLETADO: '#22c55e',
              AUSENTE: '#f59e0b',
              CANCELADO: '#ef4444',
            };

            return (
              <Grid item xs={12} key={turno.id}>
                <Card sx={{ borderLeft: `6px solid ${borderColors[turno.estado] || '#0284c7'}`, opacity: isPending ? 1 : 0.88 }}>
                <CardContent sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, justifyContent: 'space-between', alignItems: { sm: 'center' }, gap: 2 }}>
                  <Box>
                    <Box display="flex" alignItems="center" gap={1.5} mb={0.5}>
                      <Typography variant="h6" fontWeight={700} color="primary">
                        {dayjs(turno.fechaHora).format('HH:mm')} hs
                      </Typography>
                      <Chip
                        label={turno.estado}
                        color={getChipColor(turno.estado)}
                        size="small"
                        sx={{ fontWeight: 600 }}
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
                      🏥 <strong> Especialidad: </strong> {turno.especialidadNombre}
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
        })}
        </Grid>
      )}

      {/* Modal Emergente de Cancelación por Médico */}
      <Dialog open={cancelDialogOpen} onClose={handleCloseCancelDialog} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700} color="error.main">
          Justificación Obligatoria de Cancelación
        </DialogTitle>
        <DialogContent>
          <Typography variant="body1" mb={2}>
            Vas a cancelar la cita médica con <strong>{turnoACancelar?.pacienteNombre}</strong> programada para el{' '}
            <strong>{dayjs(turnoACancelar?.fechaHora).format('DD/MM/YYYY [a las] HH:mm')} hs</strong>.
          </Typography>
          <Alert severity="info" sx={{ mb: 2.5 }}>
            Al confirmar la cancelación, se enviará automáticamente un correo electrónico de disculpas al paciente incluyendo tu justificación.
          </Alert>

          {cancelError && <Alert severity="error" sx={{ mb: 2 }}>{cancelError}</Alert>}

          <TextField
            fullWidth
            multiline
            rows={3}
            label="Motivo / Justificación de la Cancelación *"
            placeholder="Ej: Inconvenientes personales imprevistos, fuerza mayor, congreso médico..."
            value={motivoCancelacion}
            onChange={(e) => setMotivoCancelacion(e.target.value)}
            error={Boolean(cancelError)}
          />
        </DialogContent>
        <DialogActions sx={{ p: 2.5 }}>
          <Button onClick={handleCloseCancelDialog} color="inherit" disabled={submittingCancel}>
            Volver
          </Button>
          <Button
            variant="contained"
            color="error"
            onClick={handleConfirmarCancelacionDoctor}
            disabled={submittingCancel || !motivoCancelacion.trim()}
          >
            {submittingCancel ? <CircularProgress size={24} color="inherit" /> : 'Confirmar y Notificar al Paciente'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Modal Emergente de Ficha y Estadísticas del Paciente */}
      <Dialog open={pacienteModalOpen} onClose={handleClosePacienteModal} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700} color="primary" sx={{ pb: 1 }}>
          Ficha del Paciente
        </DialogTitle>
        <DialogContent>
          {loadingStats ? (
            <Box display="flex" justifyContent="center" py={5}><CircularProgress size={44} /></Box>
          ) : statsError ? (
            <Alert severity="error">{statsError}</Alert>
          ) : pacienteStats ? (
            <Box pt={1}>
              {/* Header con Nombre, DNI, Edad y Teléfono */}
              <Paper sx={{ p: 2.5, mb: 3, bgcolor: 'background.default', borderRadius: 3 }}>
                <Box display="flex" alignItems="center" gap={2} mb={2}>
                  <Avatar sx={{ width: 56, height: 56, bgcolor: 'primary.main', fontWeight: 700, fontSize: '1.4rem' }}>
                    {pacienteStats.nombre ? pacienteStats.nombre.charAt(0).toUpperCase() : 'P'}
                  </Avatar>
                  <Box>
                    <Typography variant="h6" fontWeight={700}>
                      {pacienteStats.nombre} {pacienteStats.apellido}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      📧 {pacienteStats.email || 'Sin correo registrado'}
                    </Typography>
                  </Box>
                </Box>
                <Divider sx={{ my: 1.5 }} />
                <Grid container spacing={2}>
                  <Grid item xs={6} sm={3}>
                    <Typography variant="caption" color="text.secondary" display="block">DNI</Typography>
                    <Typography variant="subtitle2" fontWeight={700}>{pacienteStats.dni || '-'}</Typography>
                  </Grid>
                  <Grid item xs={6} sm={3}>
                    <Typography variant="caption" color="text.secondary" display="block">Nacimiento</Typography>
                    <Typography variant="subtitle2" fontWeight={700}>
                      {pacienteStats.fechaNacimiento ? dayjs(pacienteStats.fechaNacimiento).format('DD/MM/YYYY') : '-'}
                    </Typography>
                  </Grid>
                  <Grid item xs={6} sm={3}>
                    <Typography variant="caption" color="text.secondary" display="block">Edad</Typography>
                    <Typography variant="subtitle2" fontWeight={700}>
                      {pacienteStats.edad !== null && pacienteStats.edad !== undefined ? `${pacienteStats.edad} años` : 'Sin registrar'}
                    </Typography>
                  </Grid>
                  <Grid item xs={6} sm={3}>
                    <Typography variant="caption" color="text.secondary" display="block">Teléfono</Typography>
                    <Typography variant="subtitle2" fontWeight={700}>{pacienteStats.telefono || '-'}</Typography>
                  </Grid>
                </Grid>
              </Paper>

              {/* Título de Métricas */}
              <Typography variant="subtitle1" fontWeight={700} mb={2}>
                📊 Historial de Asistencia ({pacienteStats.totalTurnos} turnos en total)
              </Typography>

              {/* Barra Gráfica de Proporciones Visuales */}
              <Box mb={3}>
                <Box
                  sx={{
                    width: '100%',
                    height: 28,
                    borderRadius: 3,
                    overflow: 'hidden',
                    display: 'flex',
                    bgcolor: 'action.disabledBackground',
                    boxShadow: 'inset 0 1px 4px rgba(0,0,0,0.12)',
                  }}
                >
                  {pacienteStats.porcentajeCompletados > 0 && (
                    <Box sx={{ width: `${pacienteStats.porcentajeCompletados}%`, bgcolor: '#22c55e', transition: 'width 0.5s' }} title={`Completados: ${pacienteStats.porcentajeCompletados}% (${pacienteStats.totalCompletados})`} />
                  )}
                  {pacienteStats.porcentajeAusentes > 0 && (
                    <Box sx={{ width: `${pacienteStats.porcentajeAusentes}%`, bgcolor: '#f59e0b', transition: 'width 0.5s' }} title={`Ausentes: ${pacienteStats.porcentajeAusentes}% (${pacienteStats.totalAusentes})`} />
                  )}
                  {pacienteStats.porcentajeCancelados > 0 && (
                    <Box sx={{ width: `${pacienteStats.porcentajeCancelados}%`, bgcolor: '#ef4444', transition: 'width 0.5s' }} title={`Cancelados: ${pacienteStats.porcentajeCancelados}% (${pacienteStats.totalCancelados})`} />
                  )}
                  {pacienteStats.porcentajePendientes > 0 && (
                    <Box sx={{ width: `${pacienteStats.porcentajePendientes}%`, bgcolor: '#0284c7', transition: 'width 0.5s' }} title={`Pendientes: ${pacienteStats.porcentajePendientes}% (${pacienteStats.totalPendientes})`} />
                  )}
                </Box>
              </Box>

              {/* Leyenda de Colores con Porcentajes y Cantidades */}
              <Paper sx={{ p: 2, bgcolor: 'background.default', borderRadius: 3 }}>
                <Grid container spacing={2} justifyContent="space-around">
                  <Grid item xs={6} sm={3}>
                    <Box display="flex" alignItems="center" gap={1}>
                      <span style={{ width: 14, height: 14, borderRadius: '50%', backgroundColor: '#22c55e', display: 'inline-block', flexShrink: 0 }}></span>
                      <Box>
                        <Typography variant="body2" fontWeight={700}>
                          {pacienteStats.porcentajeCompletados}%
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          Completados ({pacienteStats.totalCompletados})
                        </Typography>
                      </Box>
                    </Box>
                  </Grid>

                  <Grid item xs={6} sm={3}>
                    <Box display="flex" alignItems="center" gap={1}>
                      <span style={{ width: 14, height: 14, borderRadius: '50%', backgroundColor: '#f59e0b', display: 'inline-block', flexShrink: 0 }}></span>
                      <Box>
                        <Typography variant="body2" fontWeight={700}>
                          {pacienteStats.porcentajeAusentes}%
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          Ausentes ({pacienteStats.totalAusentes})
                        </Typography>
                      </Box>
                    </Box>
                  </Grid>

                  <Grid item xs={6} sm={3}>
                    <Box display="flex" alignItems="center" gap={1}>
                      <span style={{ width: 14, height: 14, borderRadius: '50%', backgroundColor: '#ef4444', display: 'inline-block', flexShrink: 0 }}></span>
                      <Box>
                        <Typography variant="body2" fontWeight={700}>
                          {pacienteStats.porcentajeCancelados}%
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          Cancelados ({pacienteStats.totalCancelados})
                        </Typography>
                      </Box>
                    </Box>
                  </Grid>

                  <Grid item xs={6} sm={3}>
                    <Box display="flex" alignItems="center" gap={1}>
                      <span style={{ width: 14, height: 14, borderRadius: '50%', backgroundColor: '#0284c7', display: 'inline-block', flexShrink: 0 }}></span>
                      <Box>
                        <Typography variant="body2" fontWeight={700}>
                          {pacienteStats.porcentajePendientes}%
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          Pendientes ({pacienteStats.totalPendientes})
                        </Typography>
                      </Box>
                    </Box>
                  </Grid>
                </Grid>
              </Paper>
            </Box>
          ) : null}
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={handleClosePacienteModal} variant="contained">
            Cerrar
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DoctorAgenda;
