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
} from '@mui/material';
import {
  CheckCircle as CheckIcon,
  Cancel as CancelIcon,
  Person as PersonIcon,
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { doctorService } from '../../api/doctorService';
import { turnoService } from '../../api/turnoService';
import dayjs from 'dayjs';

const DoctorAgenda = () => {
  const { entidadId } = useAuth();
  const [fecha, setFecha] = useState(dayjs().format('YYYY-MM-DD'));
  const [turnos, setTurnos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [updatingId, setUpdatingId] = useState(null);

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

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} color="primary" mb={1}>
        Mi Agenda Médica
      </Typography>
      <Typography variant="body1" color="text.secondary" mb={3}>
        Consultá la lista de pacientes y turnos confirmados para el día seleccionado.
      </Typography>

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
                        color={turno.estado === 'COMPLETADO' ? 'success' : 'primary'}
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
                  </Box>

                  {turno.estado !== 'COMPLETADO' && turno.estado !== 'CANCELADO' && (
                    <Box display="flex" gap={1}>
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
                        color="error"
                        size="small"
                        startIcon={<CancelIcon />}
                        disabled={updatingId === turno.id}
                        onClick={() => handleCambiarEstado(turno.id, 'CANCELADO')}
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
    </Box>
  );
};

export default DoctorAgenda;
