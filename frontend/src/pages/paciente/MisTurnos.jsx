import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Chip,
  Button,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Tabs,
  Tab,
} from '@mui/material';
import {
  Cancel as CancelIcon,
  Event as EventIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { turnoService } from '../../api/turnoService';
import dayjs from 'dayjs';

const MisTurnos = () => {
  const { entidadId } = useAuth();
  const [turnos, setTurnos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterTab, setFilterTab] = useState(0); // 0: Todos, 1: Activos, 2: Cancelados/Completados

  // Estado para modal de cancelación
  const [selectedTurnoCancel, setSelectedTurnoCancel] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    if (entidadId) cargarTurnos();
  }, [entidadId]);

  const cargarTurnos = async () => {
    try {
      const data = await turnoService.obtenerPorPaciente(entidadId);
      setTurnos(data);
    } catch (err) {
      setError('Error al consultar la lista de turnos.');
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmCancelar = async () => {
    if (!selectedTurnoCancel) return;
    setCancelling(true);
    try {
      await turnoService.cancelar(selectedTurnoCancel.id);
      setSelectedTurnoCancel(null);
      cargarTurnos();
    } catch (err) {
      setError('No se pudo cancelar el turno.');
    } finally {
      setCancelling(false);
    }
  };

  const turnosFiltrados = turnos.filter((t) => {
    if (filterTab === 1) return t.estado === 'CONFIRMADO' || t.estado === 'PENDIENTE';
    if (filterTab === 2) return t.estado === 'CANCELADO' || t.estado === 'COMPLETADO';
    return true;
  });

  const getChipColor = (estado) => {
    switch (estado) {
      case 'CONFIRMADO':
      case 'PENDIENTE':
        return 'primary';
      case 'COMPLETADO':
        return 'success';
      case 'CANCELADO':
        return 'error';
      default:
        return 'default';
    }
  };

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} color="primary" mb={1}>
        Mis Turnos Agendados
      </Typography>
      <Typography variant="body1" color="text.secondary" mb={3}>
        Consultá el historial completo de tus citas médicas y gestioná tus turnos activos.
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      <Tabs value={filterTab} onChange={(e, val) => setFilterTab(val)} sx={{ mb: 3 }}>
        <Tab label="Todos los Turnos" />
        <Tab label="Activos" />
        <Tab label="Finalizados / Cancelados" />
      </Tabs>

      {loading ? (
        <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
      ) : turnosFiltrados.length === 0 ? (
        <Alert severity="info">No tenés turnos registrados en esta categoría.</Alert>
      ) : (
        <Grid container spacing={2.5}>
          {turnosFiltrados.map((turno) => (
            <Grid item xs={12} md={6} key={turno.id}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                <CardContent>
                  <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={1.5}>
                    <Typography variant="subtitle1" fontWeight={700} color="primary">
                      {turno.especialidadNombre}
                    </Typography>
                    <Chip label={turno.estado} color={getChipColor(turno.estado)} size="small" sx={{ fontWeight: 600 }} />
                  </Box>

                  <Typography variant="body1" fontWeight={600} mb={0.5}>
                    👨‍⚕️ Dr/a. {turno.doctorNombre}
                  </Typography>

                  <Typography variant="body2" color="text.secondary" mb={1}>
                    📅 {dayjs(turno.fechaHora).format('DD/MM/YYYY')} a las {dayjs(turno.fechaHora).format('HH:mm')} hs
                  </Typography>

                  <Typography variant="body2" color="text.secondary">
                    💬 <strong>Motivo:</strong> {turno.motivoConsulta || 'Consulta General'}
                  </Typography>
                </CardContent>

                {(turno.estado === 'CONFIRMADO' || turno.estado === 'PENDIENTE') && (
                  <Box px={2} pb={2}>
                    <Button
                      variant="outlined"
                      color="error"
                      size="small"
                      startIcon={<CancelIcon />}
                      onClick={() => setSelectedTurnoCancel(turno)}
                    >
                      Cancelar Turno
                    </Button>
                  </Box>
                )}
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Modal Confirmación Cancelación */}
      <Dialog open={!!selectedTurnoCancel} onClose={() => setSelectedTurnoCancel(null)}>
        <DialogTitle>¿Confirmar cancelación de turno?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Estás a punto de cancelar la cita de {selectedTurnoCancel?.especialidadNombre} con el/la Dr/a. {selectedTurnoCancel?.doctorNombre} para el {dayjs(selectedTurnoCancel?.fechaHora).format('DD/MM/YYYY HH:mm')} hs. Esta acción no se puede deshacer.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedTurnoCancel(null)}>Volver</Button>
          <Button onClick={handleConfirmCancelar} color="error" variant="contained" disabled={cancelling}>
            {cancelling ? <CircularProgress size={20} color="inherit" /> : 'Sí, Cancelar'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default MisTurnos;
