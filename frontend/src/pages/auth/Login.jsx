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
  Divider,
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

const GoogleIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" style={{ marginRight: 8, display: 'block' }}>
    <path
      fill="#4285F4"
      d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.66-5.17 3.66-9.17z"
    />
    <path
      fill="#34A853"
      d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.34 24 12 24z"
    />
    <path
      fill="#FBBC05"
      d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.18 0 9.99 0 12s.45 3.82 1.25 5.42l4.03-3.15z"
    />
    <path
      fill="#EA4335"
      d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.34 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z"
    />
  </svg>
);

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

  React.useEffect(() => {
    const params = new URLSearchParams(location.search);
    const oauthError = params.get('error');
    if (oauthError) {
      if (oauthError === 'account_blocked') {
        setError('Tu cuenta se encuentra bloqueada por el administrador. Contacta con soporte.');
      } else if (oauthError === 'access_denied') {
        setError('Has cancelado el inicio de sesión con Google.');
      } else if (oauthError === 'oauth_failed') {
        setError('Error al autenticar con Google. Por favor, intenta nuevamente.');
      } else if (oauthError === 'oauth_sync_error') {
        setError('No se pudo sincronizar la sesión de Google con el servidor.');
      } else {
        setError('Ocurrió un error al procesar el inicio de sesión con Google.');
      }
    }
  }, [location.search]);

  const handleGoogleLogin = () => {
    const envApiUrl = import.meta.env.VITE_API_URL;
    let oauthUrl = '/oauth2/authorization/google';
    if (envApiUrl && envApiUrl.trim()) {
      const cleanUrl = envApiUrl.trim().endsWith('/') ? envApiUrl.trim().slice(0, -1) : envApiUrl.trim();
      const rootUrl = cleanUrl.endsWith('/api') ? cleanUrl.slice(0, -4) : cleanUrl;
      oauthUrl = `${rootUrl}/oauth2/authorization/google`;
    }
    window.location.href = oauthUrl;
  };

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
                sx={{ mt: 3, mb: 1, py: 1.2 }}
              >
                {loading ? <CircularProgress size={24} color="inherit" /> : 'Iniciar Sesión'}
              </Button>

              <Divider sx={{ my: 2 }}>
                <Typography variant="caption" color="text.secondary" sx={{ px: 1, textTransform: 'uppercase', letterSpacing: 0.5, fontWeight: 500 }}>
                  O
                </Typography>
              </Divider>

              <Button
                fullWidth
                variant="outlined"
                size="large"
                onClick={handleGoogleLogin}
                startIcon={<GoogleIcon />}
                sx={{
                  py: 1.1,
                  borderColor: '#dadce0',
                  color: '#3c4043',
                  backgroundColor: '#ffffff',
                  textTransform: 'none',
                  fontSize: '0.92rem',
                  fontWeight: 600,
                  boxShadow: '0 1px 2px 0 rgba(60,64,67,0.15)',
                  '&:hover': {
                    borderColor: '#c6c9cc',
                    backgroundColor: '#f8f9fa',
                    boxShadow: '0 1px 3px 0 rgba(60,64,67,0.25)',
                  },
                }}
              >
                Continuar con Google
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
