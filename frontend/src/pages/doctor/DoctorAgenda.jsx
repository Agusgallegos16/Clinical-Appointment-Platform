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

  useEffect(() => {
    if (entidadId) cargarAgenda(fecha);
  }, [entidadId, fecha]);

  const cargarAgenda = async (fechaSeleccionada) => {
    setLoading(true);
    setError('');
    try {
      const data = await doctorService.obtenerAgenda(entidadId, fechaSeleccionada);
      setTurnos(data);
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
            Mi Agenda Médica
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
        <Alert severity="info">No poseés citas médicas registradas para esta fecha.</Alert>
      ) : (
        <Grid container spacing={2.5}>
          {turnos.map((turno) => (
            <Grid item xs={12} key={turno.id}>
              <Card sx={{ borderLeft: '6px solid #0284c7' }}>
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

                    <Typography variant="subtitle1" fontWeight={600}>
                      👤 Paciente: {turno.pacienteNombre}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      🏥 Especialidad: {turno.especialidadNombre}
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
          ))}
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
    </Box>
  );
};

export default DoctorAgenda;
