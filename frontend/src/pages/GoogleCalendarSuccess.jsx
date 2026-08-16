import React from 'react';
import { Box, Paper, Typography, Button, Container } from '@mui/material';
import { CheckCircleOutline as CheckCircleOutlineIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const GoogleCalendarSuccess = () => {
  const navigate = useNavigate();
  const { role } = useAuth();

  const handleReturn = () => {
    if (role === 'DOCTOR') navigate('/doctor');
    else if (role === 'PACIENTE') navigate('/paciente');
    else navigate('/');
  };

  return (
    <Container maxWidth="sm" sx={{ mt: 8, mb: 4 }}>
      <Paper elevation={3} sx={{ p: 4, textAlign: 'center', borderRadius: 3 }}>
        <CheckCircleOutlineIcon sx={{ fontSize: 72, color: 'success.main', mb: 2 }} />

        <Typography variant="h4" fontWeight={700} gutterBottom>
          ¡Google Calendar Conectado!
        </Typography>

        <Typography variant="body1" color="text.secondary" paragraph sx={{ mb: 4 }}>
          Tu cuenta de Google ha sido vinculada correctamente. A partir de ahora, cada turno que agendes o recibas se sincronizará automáticamente en tu Google Calendar personal.
        </Typography>

        <Button
          variant="contained"
          size="large"
          onClick={handleReturn}
          sx={{ borderRadius: 2, px: 4, fontWeight: 600 }}
        >
          Volver a Mi Panel
        </Button>
      </Paper>
    </Container>
  );
};

export default GoogleCalendarSuccess;
