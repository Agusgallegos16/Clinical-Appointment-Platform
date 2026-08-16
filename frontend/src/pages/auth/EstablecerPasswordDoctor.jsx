import React, { useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Alert,
  CircularProgress,
  Container,
} from '@mui/material';
import {
  LockReset as LockResetIcon,
  CheckCircleOutline as CheckIcon,
  ErrorOutline as ErrorIcon,
} from '@mui/icons-material';
import { authService } from '../../api/authService';

const EstablecerPasswordDoctor = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [password, setPassword] = useState('');
  const [confirmarPassword, setConfirmarPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  if (!token) {
    return (
      <Container maxWidth="sm" sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', py: 4 }}>
        <Card sx={{ width: '100%', p: 3, borderRadius: 3, textAlign: 'center' }}>
          <CardContent>
            <ErrorIcon sx={{ fontSize: 72, color: 'error.main', mb: 2 }} />
            <Typography variant="h5" color="error" fontWeight={700} mb={1}>
              Enlace de Activación Inválido
            </Typography>
            <Typography variant="body1" color="text.secondary" mb={3}>
              El enlace accedido no contiene un token válido de verificación.
            </Typography>
            <Button variant="contained" onClick={() => navigate('/login')}>
              Ir a Iniciar Sesión
            </Button>
          </CardContent>
        </Card>
      </Container>
    );
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (password !== confirmarPassword) {
      setError('Las contraseñas no coinciden. Por favor verifíquelas.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      await authService.establecerPasswordDoctor({
        token,
        password,
        confirmarPassword,
      });
      setSuccess(true);
    } catch (err) {
      let msg = 'Ocurrió un error al configurar la contraseña.';
      if (err.response?.data?.mensaje) {
        msg = err.response.data.mensaje;
      } else if (err.response?.data?.message) {
        msg = err.response.data.message;
      }
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="sm" sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', py: 4 }}>
      <Card sx={{ width: '100%', p: 4, borderRadius: 3, boxShadow: '0 8px 24px rgba(0,0,0,0.08)' }}>
        <CardContent>
          {success ? (
            <Box py={2} display="flex" flexDirection="column" alignItems="center" textAlign="center" gap={2}>
              <CheckIcon sx={{ fontSize: 72, color: 'success.main' }} />
              <Typography variant="h5" color="primary" fontWeight={700}>
                ¡Cuenta Activada con Éxito!
              </Typography>
              <Typography variant="body1" color="text.secondary">
                Tu contraseña ha sido establecida correctamente. Ya podés acceder al portal médico con tu correo electrónico y tu nueva clave.
              </Typography>
              <Button
                variant="contained"
                size="large"
                color="primary"
                onClick={() => navigate('/login')}
                sx={{ mt: 2, borderRadius: 2, px: 4 }}
              >
                Ir al Inicio de Sesión
              </Button>
            </Box>
          ) : (
            <Box component="form" onSubmit={handleSubmit}>
              <Box textAlign="center" mb={3}>
                <LockResetIcon sx={{ fontSize: 56, color: 'primary.main', mb: 1 }} />
                <Typography variant="h5" fontWeight={700} color="primary">
                  Configuración de Contraseña
                </Typography>
                <Typography variant="body2" color="text.secondary" mt={0.5}>
                  Bienvenido/a al Consultorio Médico. Ingresá tu contraseña de acceso para activar tu perfil profesional.
                </Typography>
              </Box>

              {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

              <TextField
                fullWidth
                type="password"
                label="Nueva Contraseña"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                helperText="Mínimo 6 caracteres"
                sx={{ mb: 2.5 }}
              />

              <TextField
                fullWidth
                type="password"
                label="Confirmar Nueva Contraseña"
                value={confirmarPassword}
                onChange={(e) => setConfirmarPassword(e.target.value)}
                required
                sx={{ mb: 3 }}
              />

              <Button
                type="submit"
                variant="contained"
                size="large"
                fullWidth
                disabled={loading || !password || !confirmarPassword}
                sx={{ py: 1.5, borderRadius: 2, fontWeight: 700 }}
              >
                {loading ? <CircularProgress size={24} color="inherit" /> : 'Activar Cuenta y Guardar Contraseña'}
              </Button>
            </Box>
          )}
        </CardContent>
      </Card>
    </Container>
  );
};

export default EstablecerPasswordDoctor;
