import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Box, CircularProgress, Typography, Container, Paper, Alert } from '@mui/material';
import { useAuth } from '../../context/AuthContext';
import { authService } from '../../api/authService';

const OAuth2RedirectHandler = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { loginWithToken } = useAuth();
  const [errorMessage, setErrorMessage] = useState(null);
  const handledCodeRef = useRef(null);
  const isMountedRef = useRef(true);
  const redirectTimerRef = useRef(null);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
      if (redirectTimerRef.current) {
        clearTimeout(redirectTimerRef.current);
        redirectTimerRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    const code = searchParams.get('code');
    const error = searchParams.get('error');

    if (error) {
      navigate(`/login?error=${encodeURIComponent(error)}`, { replace: true });
      return;
    }

    if (!code) {
      navigate('/login?error=missing_code', { replace: true });
      return;
    }

    if (handledCodeRef.current === code) {
      return;
    }
    handledCodeRef.current = code;

    const processCode = async () => {
      try {
        const exchangeData = await authService.intercambiarCodigoOAuth2(code);
        const userData = await loginWithToken(exchangeData.token);
        if (!isMountedRef.current) return;

        if (userData?.rol === 'ADMIN') {
          navigate('/admin', { replace: true });
        } else if (userData?.rol === 'DOCTOR') {
          navigate('/doctor', { replace: true });
        } else if (userData?.rol === 'SECRETARIA') {
          navigate('/secretaria', { replace: true });
        } else if (userData?.rol === 'PACIENTE') {
          navigate('/paciente', { replace: true });
        } else {
          navigate('/', { replace: true });
        }
      } catch (err) {
        if (!isMountedRef.current) return;
        console.error('Error al sincronizar sesión de Google OAuth2:', err);
        setErrorMessage('No se pudo establecer la sesión. Redirigiendo al login...');
        redirectTimerRef.current = setTimeout(() => {
          navigate('/login?error=oauth_sync_error', { replace: true });
        }, 2000);
      }
    };

    processCode();
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
