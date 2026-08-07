import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  Stack,
  CircularProgress,
  Alert,
} from '@mui/material';
import {
  CalendarMonth as CalendarIcon,
  Schedule as ScheduleIcon,
  FolderCopy as TemplateIcon,
  ArrowForward as ArrowForwardIcon,
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { doctorService } from '../../api/doctorService';
import dayjs from 'dayjs';

const DoctorDashboard = () => {
  const { user, entidadId } = useAuth();
  const [turnosHoy, setTurnosHoy] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    if (entidadId) cargarAgendaHoy();
  }, [entidadId]);

  const cargarAgendaHoy = async () => {
    try {
      const hoyStr = dayjs().format('YYYY-MM-DD');
      const data = await doctorService.obtenerAgenda(entidadId, hoyStr);
      setTurnosHoy(data);
    } catch (err) {
      setError('No se pudo cargar la agenda del día.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box maxWidth="800px" mx="auto">
      <Box mb={4} textAlign="center">
        <Typography variant="h4" fontWeight={700} color="primary" gutterBottom>
          Portal Médico — Dr/a. {user?.email?.split('@')[0]} 🩺
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Seleccioná la opción que querés gestionar en tu consultorio:
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {/* Resumen Diario */}
      <Card sx={{ mb: 4, bgcolor: 'primary.light', borderColor: 'primary.main' }}>
        <CardContent sx={{ py: 2 }}>
          <Typography variant="subtitle2" color="primary.dark" fontWeight={700}>
            📊 HOY: {dayjs().format('DD/MM/YYYY')}
          </Typography>
          <Typography variant="h6" fontWeight={700} color="primary.dark" mt={0.5}>
            {loading ? <CircularProgress size={20} /> : `${turnosHoy.length} Pacientes Agendados para Hoy`}
          </Typography>
        </CardContent>
      </Card>

      {/* Botones Grandes Vertically Stacked sin Números y con el mismo color */}
      <Stack spacing={2.5}>
        <Button
          variant="contained"
          color="primary"
          size="large"
          fullWidth
          onClick={() => navigate('/doctor/agenda')}
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
              Ver y Gestionar Mi Agenda del Día
            </Typography>
          </Box>
        </Button>

        <Button
          variant="contained"
          color="primary"
          size="large"
          fullWidth
          onClick={() => navigate('/doctor/horarios')}
          startIcon={<ScheduleIcon sx={{ fontSize: 36 }} />}
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
              Configurar Mis Horarios de Atención
            </Typography>
          </Box>
        </Button>

        <Button
          variant="contained"
          color="primary"
          size="large"
          fullWidth
          onClick={() => navigate('/doctor/plantillas')}
          startIcon={<TemplateIcon sx={{ fontSize: 36 }} />}
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
              Mis Plantillas de Agenda Reutilizables
            </Typography>
          </Box>
        </Button>
      </Stack>
    </Box>
  );
};

export default DoctorDashboard;
