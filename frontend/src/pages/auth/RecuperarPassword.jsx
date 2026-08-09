import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  CircularProgress,
  Container,
} from '@mui/material';
import { LockReset as LockResetIcon } from '@mui/icons-material';
import { authService } from '../../api/authService';

const RecuperarPassword = () => {
  const [formData, setFormData] = useState({
    email: '',
    nuevaPassword: '',
    confirmarNuevaPassword: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (formData.nuevaPassword !== formData.confirmarNuevaPassword) {
      setError('Las contraseñas no coinciden. Por favor verifíquelas.');
      return;
    }

    setLoading(true);

    try {
      await authService.solicitarRestablecimientoPassword(formData);
      setSuccessMessage(
        '¡Solicitud procesada! Te enviamos un correo electrónico a ' +
        formData.email +
        ' con un enlace para confirmar el cambio de contraseña. Por favor revisá tu bandeja de entrada.'
      );
    } catch (err) {
      const msg =
        err.response?.data?.mensaje ||
        err.response?.data?.message ||
        (err.response?.data?.detalles
          ? Object.values(err.response.data.detalles).join('. ')
          : 'Error al procesar la solicitud. Por favor reintente más tarde.');
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="xs" sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', py: 4 }}>
      <Card sx={{ width: '100%', p: 2, borderRadius: 3, boxShadow: '0 8px 24px rgba(0,0,0,0.08)' }}>
        <CardContent>
          <Box display="flex" flexDirection="column" alignItems="center" mb={3}>
            <LockResetIcon color="primary" sx={{ fontSize: 48, mb: 1 }} />
            <Typography variant="h5" align="center" color="primary" fontWeight={700}>
              Recuperar Contraseña
            </Typography>
            <Typography variant="body2" color="text.secondary" align="center">
              Ingresá tu correo y tu nueva clave deseada
            </Typography>
          </Box>

          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          {successMessage && (
            <Alert severity="info" sx={{ mb: 3, fontWeight: 500 }}>
              {successMessage}
            </Alert>
          )}

          {!successMessage && (
            <Box component="form" onSubmit={handleSubmit}>
              <TextField
                fullWidth
                label="Correo Electrónico"
                name="email"
                type="email"
                value={formData.email}
                onChange={handleChange}
                required
                margin="normal"
              />

              <TextField
                fullWidth
                label="Nueva Contraseña"
                name="nuevaPassword"
                type="password"
                value={formData.nuevaPassword}
                onChange={handleChange}
                required
                margin="normal"
                helperText="Mínimo 6 caracteres"
              />

              <TextField
                fullWidth
                label="Confirmar Nueva Contraseña"
                name="confirmarNuevaPassword"
                type="password"
                value={formData.confirmarNuevaPassword}
                onChange={handleChange}
                required
                margin="normal"
                error={formData.confirmarNuevaPassword !== '' && formData.nuevaPassword !== formData.confirmarNuevaPassword}
                helperText={
                  formData.confirmarNuevaPassword !== '' && formData.nuevaPassword !== formData.confirmarNuevaPassword
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
                {loading ? <CircularProgress size={24} color="inherit" /> : 'Solicitar Cambio de Contraseña'}
              </Button>
            </Box>
          )}

          <Box textAlign="center" mt={2}>
            <Typography variant="body2" color="text.secondary">
              ¿Recordaste tu clave?{' '}
              <Link to="/login" style={{ color: '#0284c7', textDecoration: 'none', fontWeight: 600 }}>
                Volver al Login
              </Link>
            </Typography>
          </Box>
        </CardContent>
      </Card>
    </Container>
  );
};

export default RecuperarPassword;
