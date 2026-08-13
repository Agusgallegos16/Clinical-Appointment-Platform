import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
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
  Grid,
} from '@mui/material';
import { LocalHospital as HospitalIcon } from '@mui/icons-material';
import { authService } from '../../api/authService';
import dayjs from 'dayjs';
import Footer from '../../components/Footer';

const Register = () => {
  const [formData, setFormData] = useState({
    nombre: '',
    apellido: '',
    email: '',
    password: '',
    confirmarPassword: '',
    dni: '',
    telefono: '',
    fechaNacimiento: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!formData.fechaNacimiento) {
      setError('Por favor seleccioná tu fecha de nacimiento.');
      return;
    }

    if (formData.password !== formData.confirmarPassword) {
      setError('Las contraseñas no coinciden. Por favor verifíquelas.');
      return;
    }

    setLoading(true);

    try {
      await authService.registroPaciente({
        ...formData,
        dni: Number(formData.dni),
      });

      setSuccessMessage(
        '¡Registro iniciado exitosamente! Te enviamos un correo con el enlace de confirmación a ' +
        formData.email +
        '. Por favor revisá tu bandeja de entrada para activar tu cuenta antes de iniciar sesión.'
      );
    } catch (err) {
      setError(
        err.response?.data?.mensaje ||
        err.response?.data?.message ||
        'Error al registrar el usuario. Verifique si el email o DNI ya están registrados.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', bgcolor: 'background.default' }}>
      <Container maxWidth="sm" sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', py: 4 }}>
      <Card sx={{ width: '100%', p: 2, borderRadius: 3, boxShadow: '0 8px 24px rgba(0,0,0,0.08)' }}>
        <CardContent>
          <Box display="flex" flexDirection="column" alignItems="center" mb={3}>
            <HospitalIcon color="primary" sx={{ fontSize: 48, mb: 1 }} />
            <Typography variant="h5" align="center" color="primary" fontWeight={700}>
              Registro de Paciente
            </Typography>
            <Typography variant="body2" color="text.secondary" align="center">
              Crea tu cuenta para agendar tus turnos médicos
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
              <Grid container spacing={2}>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Nombre"
                    name="nombre"
                    value={formData.nombre}
                    onChange={handleChange}
                    required
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Apellido"
                    name="apellido"
                    value={formData.apellido}
                    onChange={handleChange}
                    required
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="DNI / Identificación"
                    name="dni"
                    type="number"
                    value={formData.dni}
                    onChange={handleChange}
                    required
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Teléfono"
                    name="telefono"
                    value={formData.telefono}
                    onChange={handleChange}
                    required
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Fecha de Nacimiento *"
                    name="fechaNacimiento"
                    type="date"
                    value={formData.fechaNacimiento}
                    onChange={handleChange}
                    InputLabelProps={{ shrink: true }}
                    inputProps={{ max: dayjs().format('YYYY-MM-DD') }}
                    required
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Correo Electrónico"
                    name="email"
                    type="email"
                    value={formData.email}
                    onChange={handleChange}
                    required
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Contraseña"
                    name="password"
                    type="password"
                    value={formData.password}
                    onChange={handleChange}
                    required
                    helperText="Mínimo 6 caracteres"
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Confirmar Contraseña"
                    name="confirmarPassword"
                    type="password"
                    value={formData.confirmarPassword}
                    onChange={handleChange}
                    required
                    error={formData.confirmarPassword !== '' && formData.password !== formData.confirmarPassword}
                    helperText={
                      formData.confirmarPassword !== '' && formData.password !== formData.confirmarPassword
                        ? 'Las contraseñas no coinciden'
                        : ''
                    }
                  />
                </Grid>
              </Grid>

              <Button
                type="submit"
                fullWidth
                variant="contained"
                size="large"
                disabled={loading}
                sx={{ mt: 3, mb: 2, py: 1.2 }}
              >
                {loading ? <CircularProgress size={24} color="inherit" /> : 'Crear Cuenta'}
              </Button>
            </Box>
          )}

            <Box textAlign="center" mt={2}>
              <Typography variant="body2" color="text.secondary">
                ¿Ya tenés una cuenta?{' '}
                <Link to="/login" style={{ color: '#0284c7', textDecoration: 'none', fontWeight: 600 }}>
                  Iniciá Sesión
                </Link>
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Container>
      <Footer />
    </Box>
  );
};

export default Register;
