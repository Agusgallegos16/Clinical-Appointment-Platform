import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Box, CircularProgress, Typography, Container, Paper, Alert } from '@mui/material';
import { useAuth } from '../../context/AuthContext';

const OAuth2RedirectHandler = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { loginWithToken } = useAuth();
  const [errorMessage, setErrorMessage] = useState(null);

  useEffect(() => {
    const token = searchParams.get('token');
    const error = searchParams.get('error');

    if (error) {
      navigate(`/login?error=${encodeURIComponent(error)}`, { replace: true });
      return;
    }

    if (token) {
      const processToken = async () => {
        try {
          const userData = await loginWithToken(token);

          if (userData?.rol === 'ADMIN') {
            navigate('/admin', { replace: true });
          } else if (userData?.rol === 'DOCTOR') {
            navigate('/doctor', { replace: true });
          } else if (userData?.rol === 'SECRETARIA') {
            navigate('/secretaria', { replace: true });
          } else if (userData?.rol === 'PACIENTE') {
            navigate('/paciente/turnos', { replace: true });
          } else {
            navigate('/', { replace: true });
          }
        } catch (err) {
          console.error('Error al sincronizar sesión de Google OAuth2:', err);
          setErrorMessage('No se pudo establecer la sesión. Redirigiendo al login...');
          setTimeout(() => {
            navigate('/login?error=oauth_sync_error', { replace: true });
          }, 2000);
        }
      };

      processToken();
    } else {
      navigate('/login?error=missing_token', { replace: true });
    }
  }, [searchParams, loginWithToken, navigate]);

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
      }}
    >
      <Container maxWidth="xs">
        <Paper
          elevation={3}
          sx={{
            p: 4,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            borderRadius: 3,
            textAlign: 'center',
          }}
        >
          {errorMessage ? (
            <Alert severity="error" sx={{ width: '100%' }}>
              {errorMessage}
            </Alert>
          ) : (
            <>
              <CircularProgress size={50} color="primary" sx={{ mb: 3 }} />
              <Typography variant="h6" fontWeight={600} gutterBottom>
                Iniciando sesión con Google
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Verificando credenciales y sincronizando perfil...
              </Typography>
            </>
          )}
        </Paper>
      </Container>
    </Box>
  );
};

export default OAuth2RedirectHandler;
