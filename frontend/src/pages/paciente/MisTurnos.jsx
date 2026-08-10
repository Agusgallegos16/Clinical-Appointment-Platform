import React, { useEffect, useState, useMemo } from 'react';
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
  PersonOff as PersonOffIcon,
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { turnoService } from '../../api/turnoService';
import dayjs from 'dayjs';

import { useSearchParams } from 'react-router-dom';
import { pacienteMenorService } from '../../api/pacienteMenorService';

import GoogleCalendarButton from '../../components/GoogleCalendarButton';

const MisTurnos = () => {
  const { entidadId } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  const queryPacienteId = searchParams.get('pacienteId') ? Number(searchParams.get('pacienteId')) : null;
  const queryNombre = searchParams.get('nombre');

  const [selectedPacienteId, setSelectedPacienteId] = useState(queryPacienteId || entidadId);
  const [menores, setMenores] = useState([]);

  const [turnos, setTurnos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterTab, setFilterTab] = useState(0); // 0: Turnos Activos (por defecto), 1: Todos, 2: Finalizados / Cancelados

  // Estado para modal de cancelación
  const [selectedTurnoCancel, setSelectedTurnoCancel] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    if (entidadId) {
      cargarMenoresTutor();
    }
  }, [entidadId]);

  useEffect(() => {
    const idAUsar = queryPacienteId || selectedPacienteId || entidadId;
    if (idAUsar) {
      cargarTurnos(idAUsar);
    }
  }, [selectedPacienteId, queryPacienteId, entidadId]);

  const cargarMenoresTutor = async () => {
    try {
      const data = await pacienteMenorService.listarMenores();
      setMenores(data);
    } catch (err) {
      // Opcional
    }
  };

  const cargarTurnos = async (pacienteIdTarget) => {
    setLoading(true);
    setError('');
    try {
      const data = await turnoService.obtenerPorPaciente(pacienteIdTarget);
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
    const targetId = queryPacienteId || selectedPacienteId || entidadId;
    try {
      await turnoService.cancelar(selectedTurnoCancel.id);
      setSelectedTurnoCancel(null);
      await cargarTurnos(targetId);
    } catch (err) {
      setError(err.response?.data?.mensaje || err.response?.data?.message || 'No se pudo cancelar el turno.');
    } finally {
      setCancelling(false);
    }
  };

  // Filtrado y Ordenamiento Estricto por Fecha y Hora (Próximos turnos primero)
  const turnosFiltradosYOrdenados = useMemo(() => {
    const filtrados = turnos.filter((t) => {
      if (filterTab === 0) return t.estado === 'CONFIRMADO' || t.estado === 'PENDIENTE';
      if (filterTab === 2) return t.estado === 'CANCELADO' || t.estado === 'COMPLETADO' || t.estado === 'AUSENTE';
      return true;
    });

    return filtrados.sort((a, b) => {
      const dateA = dayjs(a.fechaHora);
      const dateB = dayjs(b.fechaHora);

      // Si estamos en activos (0), los más próximos van primero (ascendente)
      if (filterTab === 0) {
        return dateA.diff(dateB);
      }
      // En historial o todos, orden descendente por fecha
      return dateB.diff(dateA);
    });
  }, [turnos, filterTab]);

  const getChipColor = (estado) => {
    switch (estado) {
      case 'CONFIRMADO':
      case 'PENDIENTE':
        return 'primary';
      case 'COMPLETADO':
        return 'success';
      case 'AUSENTE':
        return 'warning';
      case 'CANCELADO':
        return 'error';
      default:
        return 'default';
    }
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="flex-start" flexWrap="wrap" gap={2} mb={3}>
        <Box>
          <Typography variant="h4" fontWeight={700} color="primary" mb={1}>
            Mis Turnos Agendados
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Consultá tus próximas citas médicas y gestioná tus turnos activos.
          </Typography>
        </Box>
        <GoogleCalendarButton />
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {menores.length > 0 && (
        <Box mb={3}>
          <Typography variant="subtitle2" fontWeight={700} color="text.secondary" mb={1}>
            👤 Seleccionar Paciente:
          </Typography>
          <Tabs
            value={selectedPacienteId || entidadId}
            onChange={(e, newId) => {
              setSelectedPacienteId(newId);
              setSearchParams({ pacienteId: newId });
            }}
            variant="scrollable"
            scrollButtons="auto"
            sx={{ bgcolor: 'background.paper', borderRadius: 3, p: 0.5 }}
          >
            <Tab value={entidadId} label="Mis Turnos (Titular)" sx={{ fontWeight: 700 }} />
            {menores.map((m) => (
              <Tab key={m.id} value={m.id} label={`🧒 ${m.nombre} ${m.apellido}`} sx={{ fontWeight: 600 }} />
            ))}
          </Tabs>
        </Box>
      )}

      <Tabs value={filterTab} onChange={(e, val) => setFilterTab(val)} sx={{ mb: 3 }}>
        <Tab label="Turnos Activos" />
        <Tab label="Todos los Turnos" />
        <Tab label="Finalizados / Cancelados / Ausentes" />
      </Tabs>

      {loading ? (
        <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
      ) : turnosFiltradosYOrdenados.length === 0 ? (
        <Alert severity="info">No tenés turnos registrados en esta categoría.</Alert>
      ) : (
        <Grid container spacing={2.5}>
          {turnosFiltradosYOrdenados.map((turno) => (
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

                  <Typography variant="body2" color="text.secondary" mb={0.5}>
                    💬 <strong>Motivo:</strong> {turno.motivoConsulta || 'Consulta General'}
                  </Typography>

                  {turno.estado === 'CANCELADO' && turno.motivoCancelacion && (
                    <Alert severity="error" sx={{ mt: 1.5, py: 0.5, px: 1.5, fontSize: '0.85rem', fontWeight: 600 }}>
                      ❌ <strong>Motivo de Cancelación del Médico:</strong> {turno.motivoCancelacion}
                    </Alert>
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
