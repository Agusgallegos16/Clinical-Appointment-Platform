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
  const [doctorInfo, setDoctorInfo] = useState(null);
  const [turnosHoy, setTurnosHoy] = useState([]);
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
      const hoyStr = dayjs().format('YYYY-MM-DD');
      const [docData, turnosData] = await Promise.all([
        doctorService.obtenerPorId(entidadId).catch(() => null),
        doctorService.obtenerAgenda(entidadId, hoyStr).catch(() => []),
      ]);
      if (docData) setDoctorInfo(docData);
      if (turnosData) setTurnosHoy(turnosData);
    } catch (err) {
      setError('No se pudo cargar la agenda del día.');
    } finally {
      setLoading(false);
    }
  };

  const nombreDoctor = doctorInfo
    ? `${doctorInfo.nombre} ${doctorInfo.apellido}`
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
            Portal Médico — Dr/a. {nombreDoctor}
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Selecciona que quieres hacer hoy:
          </Typography>
        </Box>

        {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

        {/* Resumen Diario */}
        <Card sx={{ mb: 4, bgcolor: 'primary.light', borderColor: 'primary.main', borderRadius: 3 }}>
          <CardContent sx={{ py: 2 }}>
            <Typography variant="subtitle2" color="primary.dark" fontWeight={700}>
              📊 FECHA: {dayjs().format('DD/MM/YYYY')}
            </Typography>
            <Typography variant="h6" fontWeight={700} color="primary.dark" mt={0.5}>
              {loading ? <CircularProgress size={20} /> : `${turnosHoy.length} Pacientes Agendados para Hoy`}
            </Typography>
          </CardContent>
        </Card>

        {/* Botones Centrados, más altos y de ancho contenido */}
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
                Ver Mis Plantillas de Agenda
              </Typography>
            </Box>
          </Button>
        </Stack>
      </Box>
    </Box>
  );
};

export default DoctorDashboard;
