import React, { useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Alert,
  CircularProgress,
  Container,
  Button,
  TextField,
} from '@mui/material';
import {
  CheckCircleOutline as CheckIcon,
  ErrorOutline as ErrorIcon,
  LockReset as LockResetIcon,
} from '@mui/icons-material';
import { authService } from '../../api/authService';

const ConfirmarRestablecimiento = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [nuevaPassword, setNuevaPassword] = useState('');
  const [confirmarNuevaPassword, setConfirmarNuevaPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (nuevaPassword.length < 12) {
      setError('La contraseña debe tener al menos 12 caracteres.');
      return;
    }

    if (nuevaPassword !== confirmarNuevaPassword) {
      setError('Las contraseñas no coinciden. Por favor verifíquelas.');
      return;
    }

    setLoading(true);
    try {
      await authService.confirmarRestablecimientoPassword({
        token,
        nuevaPassword,
        confirmarNuevaPassword,
      });
      setSuccess(true);
      setTimeout(() => {
        navigate('/login');
      }, 4000);
    } catch (err) {
      setError(
        err.response?.data?.mensaje ||
          err.response?.data?.message ||
          (err.response?.data?.detalles
            ? Object.values(err.response.data.detalles).join('. ')
            : 'El enlace de confirmación es inválido o ha expirado. Por favor solicite nuevamente el cambio de contraseña.')
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="xs" sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', py: 4 }}>
      <Card sx={{ width: '100%', p: 2.5, borderRadius: 3, boxShadow: '0 8px 24px rgba(0,0,0,0.08)' }}>
        <CardContent>
          {!token ? (
            <Box py={2} display="flex" flexDirection="column" alignItems="center" gap={2} textAlign="center">
              <ErrorIcon sx={{ fontSize: 64, color: 'error.main' }} />
              <Typography variant="h5" color="error" fontWeight={700}>
                Enlace inválido
              </Typography>
              <Alert severity="error" sx={{ width: '100%' }}>
                El enlace de confirmación no contiene un token válido.
              </Alert>
              <Button
                variant="outlined"
                color="primary"
                component={Link}
                to="/recuperar-password"
                sx={{ mt: 1, borderRadius: 2 }}
              >
                Volver a Solicitar Cambio
              </Button>
            </Box>
          ) : success ? (
            <Box py={2} display="flex" flexDirection="column" alignItems="center" gap={2} textAlign="center">
              <CheckIcon sx={{ fontSize: 72, color: 'success.main' }} />
              <Typography variant="h5" color="primary" fontWeight={700}>
                Contraseña Cambiada con Éxito
              </Typography>
              <Typography variant="body1" color="text.secondary" paragraph>
                Tu contraseña ha sido actualizada correctamente. Serás redirigido al inicio de sesión en unos segundos...
              </Typography>
              <Button
                variant="contained"
                color="primary"
                onClick={() => navigate('/login')}
                sx={{ mt: 1, borderRadius: 2, px: 4 }}
              >
                Ir a Iniciar Sesión
              </Button>
            </Box>
          ) : (
            <Box>
              <Box display="flex" flexDirection="column" alignItems="center" mb={3}>
                <LockResetIcon color="primary" sx={{ fontSize: 48, mb: 1 }} />
                <Typography variant="h5" align="center" color="primary" fontWeight={700}>
                  Definir Nueva Contraseña
                </Typography>
                <Typography variant="body2" color="text.secondary" align="center">
                  Ingresá tu nueva contraseña para completar el restablecimiento
                </Typography>
              </Box>

              {error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {error}
                </Alert>
              )}

              <Box component="form" onSubmit={handleSubmit}>
                <TextField
                  fullWidth
                  label="Nueva Contraseña"
                  name="nuevaPassword"
                  type="password"
                  value={nuevaPassword}
                  onChange={(e) => setNuevaPassword(e.target.value)}
                  required
                  margin="normal"
                  helperText="Mínimo 12 caracteres"
                />

                <TextField
                  fullWidth
                  label="Confirmar Nueva Contraseña"
                  name="confirmarNuevaPassword"
                  type="password"
                  value={confirmarNuevaPassword}
                  onChange={(e) => setConfirmarNuevaPassword(e.target.value)}
                  required
                  margin="normal"
                  error={confirmarNuevaPassword !== '' && nuevaPassword !== confirmarNuevaPassword}
                  helperText={
                    confirmarNuevaPassword !== '' && nuevaPassword !== confirmarNuevaPassword
                      ? 'Las contraseñas no coinciden'
                      : ''
                  }
                />

                <Button
                  type="submit"
                  fullWidth
                  variant="contained"
                  size="large"
                  disabled={loading}
                  sx={{ mt: 3, mb: 2, py: 1.2 }}
                >
                  {loading ? <CircularProgress size={24} color="inherit" /> : 'Confirmar Nueva Contraseña'}
                </Button>
              </Box>

              <Box textAlign="center" mt={1}>
                <Button component={Link} to="/recuperar-password" size="small">
                  Solicitar un nuevo enlace
                </Button>
              </Box>
            </Box>
          )}
        </CardContent>
      </Card>
    </Container>
  );
};

export default ConfirmarRestablecimiento;
