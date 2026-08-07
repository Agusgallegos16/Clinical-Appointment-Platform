import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  CircularProgress,
  Alert,
  Stack,
} from '@mui/material';
import {
  EventNote as EventNoteIcon,
  CalendarMonth as CalendarIcon,
  ArrowForward as ArrowForwardIcon,
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { turnoService } from '../../api/turnoService';
import dayjs from 'dayjs';

const PacienteDashboard = () => {
  const { user, entidadId } = useAuth();
  const [turnos, setTurnos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    if (entidadId) cargarTurnos();
    else setLoading(false);
  }, [entidadId]);

  const cargarTurnos = async () => {
    try {
      const data = await turnoService.obtenerPorPaciente(entidadId);
      setTurnos(data);
    } catch (err) {
      setError('No se pudieron cargar tus turnos agendados.');
    } finally {
      setLoading(false);
    }
  };

  const proximoTurno = turnos.find((t) => t.estado === 'CONFIRMADO' || t.estado === 'PENDIENTE');

  return (
    <Box maxWidth="800px" mx="auto">
      <Box mb={4} textAlign="center">
        <Typography variant="h4" fontWeight={700} color="primary" gutterBottom>
          ¡Hola, {user?.email?.split('@')[0]}! 👋
        </Typography>
        <Typography variant="body1" color="text.secondary">
          ¿Qué te gustaría hacer hoy en tu portal de salud?
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {/* Próximo Turno Agendado (si existe) */}
      {proximoTurno && (
        <Card sx={{ mb: 4, bgcolor: 'primary.light', borderColor: 'primary.main' }}>
          <CardContent>
            <Typography variant="subtitle2" color="primary.dark" fontWeight={700}>
              📌 PRÓXIMO TURNO AGENDADO
            </Typography>
            <Typography variant="h6" fontWeight={700} mt={1}>
              {proximoTurno.especialidadNombre} — Dr/a. {proximoTurno.doctorNombre}
            </Typography>
            <Typography variant="body1" fontWeight={600} color="primary.dark" mt={0.5}>
              📅 {dayjs(proximoTurno.fechaHora).format('DD/MM/YYYY')} a las {dayjs(proximoTurno.fechaHora).format('HH:mm')} hs
            </Typography>
          </CardContent>
        </Card>
      )}

      {/* Botones Grandes Vertically Stacked sin Números y con el mismo color */}
      <Stack spacing={2.5}>
        <Button
          variant="contained"
          color="primary"
          size="large"
          fullWidth
          onClick={() => navigate('/paciente/reservar')}
          startIcon={<EventNoteIcon sx={{ fontSize: 36 }} />}
          endIcon={<ArrowForwardIcon />}
          sx={{
            py: 2.5,
            px: 4,
            justifyContent: 'space-between',
            fontSize: '1.2rem',
            fontWeight: 700,
            borderRadius: 3,
          }}
        >
          <Box textAlign="left">
            <Typography variant="h6" fontWeight={700}>
              Reservar Nuevo Turno
            </Typography>
          </Box>
        </Button>

        <Button
          variant="contained"
          color="primary"
          size="large"
          fullWidth
          onClick={() => navigate('/paciente/turnos')}
          startIcon={<CalendarIcon sx={{ fontSize: 36 }} />}
          endIcon={<ArrowForwardIcon />}
          sx={{
            py: 2.5,
            px: 4,
            justifyContent: 'space-between',
            fontSize: '1.2rem',
            fontWeight: 700,
            borderRadius: 3,
          }}
        >
          <Box textAlign="left">
            <Typography variant="h6" fontWeight={700}>
              Mis Turnos Agendados ({turnos.length})
            </Typography>
          </Box>
        </Button>
      </Stack>
    </Box>
  );
};

export default PacienteDashboard;
