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
  InputAdornment,
  IconButton,
} from '@mui/material';
import {
  Email as EmailIcon,
  Lock as LockIcon,
  Visibility,
  VisibilityOff,
  LocalHospital as HospitalIcon,
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { CLINIC_CONFIG } from '../../config/clinicConfig';

import Footer from '../../components/Footer';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const { login, loading } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    try {
      const userData = await login({ email, password });

      // Redirigir según el rol del usuario
      if (userData.rol === 'ADMIN') navigate('/admin');
      else if (userData.rol === 'DOCTOR') navigate('/doctor');
      else if (userData.rol === 'PACIENTE') navigate('/paciente');
      else navigate('/');
    } catch (err) {
      setError(
        err.response?.data?.mensaje ||
        err.response?.data?.message ||
        'Error al iniciar sesión. Verifique sus credenciales (email y contraseña).'
      );
    }
  };

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', bgcolor: 'background.default' }}>
      <Container maxWidth="xs" sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', py: 4 }}>
        <Card sx={{ width: '100%', p: 2, borderRadius: 3, boxShadow: '0 8px 24px rgba(0,0,0,0.08)' }}>
          <CardContent>
            <Box display="flex" flexDirection="column" alignItems="center" mb={3}>
              <HospitalIcon color="primary" sx={{ fontSize: 48, mb: 1 }} />
              <Typography variant="h5" align="center" color="primary" fontWeight={700}>
                {CLINIC_CONFIG.name}
              </Typography>
              <Typography variant="body2" color="text.secondary" align="center" sx={{ mt: 1, px: 1, fontWeight: 500, lineHeight: 1.4 }}>
                Sistema de gestión y reserva de turnos médicos online para pacientes y profesionales del Instituto Doctor Oscar Bustos.
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
                label="Correo Electrónico"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                margin="normal"
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <EmailIcon color="action" />
                    </InputAdornment>
                  ),
                }}
              />

              <TextField
                fullWidth
                label="Contraseña"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                margin="normal"
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <LockIcon color="action" />
                    </InputAdornment>
                  ),
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton onClick={() => setShowPassword(!showPassword)} edge="end">
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />

              <Button
                type="submit"
                fullWidth
                variant="contained"
                size="large"
                disabled={loading}
                sx={{ mt: 3, mb: 2, py: 1.2 }}
              >
                {loading ? <CircularProgress size={24} color="inherit" /> : 'Iniciar Sesión'}
              </Button>

              <Box textAlign="left" mt={2}>
                <Typography variant="body2" color="text.secondary">
                  ¿Olvidaste tu contraseña?{' '}
                  <Link to="/recuperar-password" style={{ color: '#0284c7', textDecoration: 'none', fontWeight: 600 }}>
                    Reestablecerla aquí
                  </Link>
                </Typography>
              </Box>

              <Box textAlign="left" mt={2}>
                <Typography variant="body2" color="text.secondary">
                  ¿No tenés una cuenta?{' '}
                  <Link to="/registro" style={{ color: '#0284c7', textDecoration: 'none', fontWeight: 600 }}>
                    Registrate como Paciente
                  </Link>
                </Typography>
              </Box>
            </Box>
          </CardContent>
        </Card>
      </Container>
      <Footer />
    </Box>
  );
};

export default Login;
