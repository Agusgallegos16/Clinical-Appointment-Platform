import React, { useEffect, useState } from 'react';
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
} from '@mui/material';
import {
  CheckCircleOutline as CheckIcon,
  ErrorOutline as ErrorIcon,
} from '@mui/icons-material';
import { authService } from '../../api/authService';

const ConfirmarRestablecimiento = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!token) {
      setError('El enlace de confirmación no contiene un token válido.');
      setLoading(false);
      return;
    }

    const confirmar = async () => {
      try {
        await authService.confirmarRestablecimientoPassword(token);
        setSuccess(true);
        setTimeout(() => {
          navigate('/login');
        }, 4000);
      } catch (err) {
        setError(
          err.response?.data?.mensaje ||
          err.response?.data?.message ||
          'El enlace de confirmación es inválido o ha expirado. Por favor solicite nuevamente el cambio de contraseña.'
        );
      } finally {
        setLoading(false);
      }
    };

    confirmar();
  }, [token, navigate]);

  return (
    <Container maxWidth="sm" sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', py: 4 }}>
      <Card sx={{ width: '100%', p: 3, borderRadius: 3, boxShadow: '0 8px 24px rgba(0,0,0,0.08)', textAlign: 'center' }}>
        <CardContent>
          {loading ? (
            <Box py={4} display="flex" flexDirection="column" alignItems="center" gap={2}>
              <CircularProgress size={48} />
              <Typography variant="h6" color="text.secondary">
                Confirmando cambio de contraseña...
              </Typography>
            </Box>
          ) : success ? (
            <Box py={2} display="flex" flexDirection="column" alignItems="center" gap={2}>
              <CheckIcon sx={{ fontSize: 72, color: 'success.main' }} />
              <Typography variant="h5" color="primary" fontWeight={700}>
                Contraseña Cambiada con Éxito
              </Typography>
              <Typography variant="body1" color="text.secondary" paragraph>
                Tu contraseña ha sido actualizada correctamente en la base de datos. Serás redirigido al inicio de sesión en unos segundos...
              </Typography>
              <Button
                variant="contained"
                color="primary"
                onClick={() => navigate('/login')}
                sx={{ mt: 2, borderRadius: 2, px: 4 }}
              >
                Ir a Iniciar Sesión
              </Button>
            </Box>
          ) : (
            <Box py={2} display="flex" flexDirection="column" alignItems="center" gap={2}>
              <ErrorIcon sx={{ fontSize: 72, color: 'error.main' }} />
              <Typography variant="h5" color="error" fontWeight={700}>
                No se pudo cambiar la contraseña
              </Typography>
              <Alert severity="error" sx={{ width: '100%', mt: 1 }}>
                {error}
              </Alert>
              <Button
                variant="outlined"
                color="primary"
                component={Link}
                to="/recuperar-password"
                sx={{ mt: 2, borderRadius: 2 }}
              >
                Volver a Solicitar Cambio
              </Button>
            </Box>
          )}
        </CardContent>
      </Card>
    </Container>
  );
};

export default ConfirmarRestablecimiento;
