import React, { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
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
  ArrowBack as ArrowBackIcon,
  Home as HomeIcon,
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { CLINIC_CONFIG } from '../../config/clinicConfig';

import Footer from '../../components/Footer';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const { login, loading, isAuthenticated, role } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = location.state?.from?.pathname;

  React.useEffect(() => {
    if (isAuthenticated) {
      if (role === 'ADMIN') navigate('/admin', { replace: true });
      else if (role === 'DOCTOR') navigate('/doctor', { replace: true });
      else if (role === 'SECRETARIA') navigate('/secretaria', { replace: true });
      else if (role === 'PACIENTE') navigate('/paciente', { replace: true });
    }
  }, [isAuthenticated, role, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    try {
      const userData = await login({ email, password });

      // Verificar si hay una ruta previa permitida según el rol del usuario
      const isTargetAllowed = from && (
        (userData.rol === 'PACIENTE' && from.startsWith('/paciente')) ||
        (userData.rol === 'DOCTOR' && from.startsWith('/doctor')) ||
        (userData.rol === 'SECRETARIA' && from.startsWith('/secretaria')) ||
        (userData.rol === 'ADMIN' && from.startsWith('/admin'))
      );

      if (isTargetAllowed) {
        navigate(from, { replace: true });
      } else if (userData.rol === 'ADMIN') navigate('/admin');
      else if (userData.rol === 'DOCTOR') navigate('/doctor');
      else if (userData.rol === 'SECRETARIA') navigate('/secretaria');
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
      <Container maxWidth="xs" sx={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', py: 4 }}>
        
        {/* Botón para volver a la página principal / Landing Page */}
        <Box width="100%" display="flex" justifyContent="flex-start" mb={2}>
          <Button
            component={Link}
            to="/"
            startIcon={<ArrowBackIcon />}
            sx={{
              textTransform: 'none',
              fontWeight: 600,
              color: 'text.secondary',
              fontSize: '0.88rem',
              '&:hover': { color: 'primary.main', bgcolor: 'transparent' },
            }}
          >
            Volver a la página principal
          </Button>
        </Box>

        <Card sx={{ width: '100%', p: 2, borderRadius: 3, boxShadow: '0 8px 24px rgba(0,0,0,0.08)' }}>
          <CardContent>
            <Box display="flex" flexDirection="column" alignItems="center" mb={3}>
              <HospitalIcon color="primary" sx={{ fontSize: 48, mb: 1 }} />
              <Typography variant="h5" align="center" color="primary" fontWeight={700}>
                Iniciar Sesión
              </Typography>
              <Typography
                variant="caption"
                align="center"
                sx={{
                  mt: 0.8,
                  px: 1,
                  fontSize: '0.73rem',
                  color: '#94a3b8',
                  lineHeight: 1.3,
                  display: 'block',
                }}
              >
                Ingresa tus credenciales para ingresar al portal del {CLINIC_CONFIG.name}.
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
