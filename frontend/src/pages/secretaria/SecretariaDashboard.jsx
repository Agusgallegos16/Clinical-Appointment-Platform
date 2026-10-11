import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  Stack,
  Alert,
} from '@mui/material';
import {
  CalendarMonth as CalendarIcon,
  ArrowForward as ArrowForwardIcon,
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { pacienteService } from '../../api/pacienteService';

const SecretariaDashboard = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [nombreCompleto, setNombreCompleto] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    cargarPerfilSecretaria();
  }, []);

  const cargarPerfilSecretaria = async () => {
    try {
      const data = await pacienteService.obtenerMiPerfil();
      if (data && data.nombre && data.apellido) {
        setNombreCompleto(`${data.nombre} ${data.apellido}`);
      }
    } catch (err) {
      console.warn('Perfil detallado no encontrado para secretaria:', err);
    }
  };

  const nombreMostrar = nombreCompleto ||
    (user?.email?.toLowerCase().startsWith('secretaria') ? 'Secretaria' : user?.email?.split('@')[0]) ||
    'Secretaria';

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
        {}
        <Box mb={4} display="flex" flexDirection="column" alignItems="center">
          <Typography variant="h4" fontWeight={700} color="primary" gutterBottom>
            ¡Hola, {nombreMostrar}! 👩‍💼
          </Typography>
          <Typography variant="body1" color="text.secondary" mb={2}>
            ¿Qué te gustaría hacer hoy?
          </Typography>
        </Box>

        {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

        <Stack spacing={2.5}>
          <Button
            variant="contained"
            color="primary"
            size="large"
            fullWidth
            onClick={() => navigate('/secretaria/agenda')}
            startIcon={<CalendarIcon sx={{ fontSize: 36 }} />}
            endIcon={<ArrowForwardIcon />}
            sx={{
              py: 2.8,
              px: 3.5,
              justifyContent: 'space-between',
              fontSize: '1.15rem',
              fontWeight: 700,
              borderRadius: 3,
              boxShadow: '0 6px 20px rgba(2, 132, 199, 0.28)',
              transition: '0.2s',
              '&:hover': {
                transform: 'translateY(-2px)',
                boxShadow: '0 8px 24px rgba(2, 132, 199, 0.35)',
              },
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Revisar Agenda Médica
              </Typography>
            </Box>
          </Button>
        </Stack>
      </Box>
    </Box>
  );
};

export default SecretariaDashboard;
