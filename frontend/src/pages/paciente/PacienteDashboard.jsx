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
  ChildCare as ChildCareIcon,
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { turnoService } from '../../api/turnoService';
import { pacienteService } from '../../api/pacienteService';
import dayjs from 'dayjs';

const PacienteDashboard = () => {
  const { user, entidadId } = useAuth();
  const [pacienteInfo, setPacienteInfo] = useState(null);
  const [turnos, setTurnos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    if (entidadId) {
      cargarDatos();
    } else {
      setLoading(false);
    }
  }, [entidadId]);

  const cargarDatos = async () => {
    setLoading(true);
    try {
      const [pacienteData, turnosData] = await Promise.all([
        pacienteService.obtenerPorId(entidadId).catch(() => null),
        turnoService.obtenerPorPaciente(entidadId).catch(() => []),
      ]);
      if (pacienteData) setPacienteInfo(pacienteData);
      if (turnosData) setTurnos(turnosData);
    } catch (err) {
      setError('No se pudieron cargar tus datos.');
    } finally {
      setLoading(false);
    }
  };

  const proximoTurno = turnos.find((t) => t.estado === 'CONFIRMADO' || t.estado === 'PENDIENTE');

  const nombreMostrar = pacienteInfo
    ? `${pacienteInfo.nombre} ${pacienteInfo.apellido}`
    : user?.email?.split('@')[0] || '';

  return (
    <Box
      sx={{
        minHeight: '75vh',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        width: '100%',
        py: 3,
      }}
    >
      <Box maxWidth="480px" width="100%" textAlign="center">
        <Box mb={4}>
          <Typography variant="h4" fontWeight={700} color="primary" gutterBottom>
            ¡Hola, {nombreMostrar}!
          </Typography>
          <Typography variant="body1" color="text.secondary">
            ¿Qué te gustaría hacer hoy?
          </Typography>
        </Box>

        {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

        {/* Próximo Turno Agendado (si existe) */}
        {proximoTurno && (
          <Card sx={{ mb: 4, bgcolor: 'primary.light', borderColor: 'primary.main', borderRadius: 3 }}>
            <CardContent>
              <Typography variant="subtitle2" color="primary.dark" fontWeight={700}>
                📌 PRÓXIMO TURNO AGENDADO
              </Typography>
              <Typography variant="h6" color="primary.dark" fontWeight={700} mt={1}>
                {proximoTurno.especialidadNombre} — Dr/a. {proximoTurno.doctorNombre}
              </Typography>
              <Typography variant="body1" fontWeight={600} color="primary.dark" mt={0.5}>
                {dayjs(proximoTurno.fechaHora).format('DD/MM/YYYY')} a las {dayjs(proximoTurno.fechaHora).format('HH:mm')} hs
              </Typography>
            </CardContent>
          </Card>
        )}

        {/* Botones Centrados, más altos y de ancho contenido */}
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
              py: 2.8,
              px: 3.5,
              justifyContent: 'space-between',
              fontSize: '1.15rem',
              fontWeight: 700,
              borderRadius: 3,
              boxShadow: '0 4px 14px rgba(2, 132, 199, 0.25)',
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
              py: 2.8,
              px: 3.5,
              justifyContent: 'space-between',
              fontSize: '1.15rem',
              fontWeight: 700,
              borderRadius: 3,
              boxShadow: '0 4px 14px rgba(2, 132, 199, 0.25)',
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Ver Mis Turnos
              </Typography>
            </Box>
          </Button>

          <Button
            variant="outlined"
            color="primary"
            size="large"
            fullWidth
            onClick={() => navigate('/paciente/menores')}
            startIcon={<ChildCareIcon sx={{ fontSize: 36 }} />}
            endIcon={<ArrowForwardIcon />}
            sx={{
              py: 2.8,
              px: 3.5,
              justifyContent: 'space-between',
              fontSize: '1.15rem',
              fontWeight: 700,
              borderRadius: 3,
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Gestionar Menores a Cargo
              </Typography>
            </Box>
          </Button>
        </Stack>
      </Box>
    </Box>
  );
};

export default PacienteDashboard;
