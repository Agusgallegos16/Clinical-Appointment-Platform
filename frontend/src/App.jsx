import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ColorModeProvider, useColorMode } from './context/ColorModeContext';
import { getCustomTheme } from './theme/theme';

// Layout & Security
import MainLayout from './components/MainLayout';
import ProtectedRoute from './routes/ProtectedRoute';

// Auth Pages
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';

// Paciente Pages
import PacienteDashboard from './pages/paciente/PacienteDashboard';
import ReservarTurno from './pages/paciente/ReservarTurno';
import MisTurnos from './pages/paciente/MisTurnos';

// Doctor Pages
import DoctorDashboard from './pages/doctor/DoctorDashboard';
import DoctorAgenda from './pages/doctor/DoctorAgenda';
import DoctorHorarios from './pages/doctor/DoctorHorarios';
import DoctorPlantillas from './pages/doctor/DoctorPlantillas';

// Admin Pages
import AdminDashboard from './pages/admin/AdminDashboard';
import AdminEspecialidades from './pages/admin/AdminEspecialidades';
import AdminDoctores from './pages/admin/AdminDoctores';
import AdminNuevoDoctor from './pages/admin/AdminNuevoDoctor';
import AdminReportes from './pages/admin/AdminReportes';

import ConfirmarEmail from './pages/auth/ConfirmarEmail';
import RecuperarPassword from './pages/auth/RecuperarPassword';
import ConfirmarRestablecimiento from './pages/auth/ConfirmarRestablecimiento';
import GoogleCalendarSuccess from './pages/GoogleCalendarSuccess';

// Redirección inteligente en la raíz '/' según rol
const RootRedirect = () => {
  const { isAuthenticated, role } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (role === 'ADMIN') return <Navigate to="/admin" replace />;
  if (role === 'DOCTOR') return <Navigate to="/doctor" replace />;
  if (role === 'PACIENTE') return <Navigate to="/paciente" replace />;
  return <Navigate to="/login" replace />;
};

const AppContent = () => {
  const { mode } = useColorMode();
  const theme = getCustomTheme(mode);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter>
        <Routes>
          {/* Rutas Públicas */}
          <Route path="/login" element={<Login />} />
          <Route path="/registro" element={<Register />} />
          <Route path="/recuperar-password" element={<RecuperarPassword />} />
          <Route path="/confirmar-email" element={<ConfirmarEmail />} />
          <Route path="/confirmar-restablecimiento" element={<ConfirmarRestablecimiento />} />
          <Route path="/google-calendar/success" element={<GoogleCalendarSuccess />} />
          <Route path="/" element={<RootRedirect />} />

          {/* Rutas Protegidas dentro del MainLayout */}
          <Route element={<MainLayout />}>
            {/* Módulo Paciente */}
            <Route element={<ProtectedRoute allowedRoles={['PACIENTE']} />}>
              <Route path="/paciente" element={<PacienteDashboard />} />
              <Route path="/paciente/reservar" element={<ReservarTurno />} />
              <Route path="/paciente/turnos" element={<MisTurnos />} />
            </Route>

            {/* Módulo Doctor */}
            <Route element={<ProtectedRoute allowedRoles={['DOCTOR']} />}>
              <Route path="/doctor" element={<DoctorDashboard />} />
              <Route path="/doctor/agenda" element={<DoctorAgenda />} />
              <Route path="/doctor/horarios" element={<DoctorHorarios />} />
              <Route path="/doctor/plantillas" element={<DoctorPlantillas />} />
            </Route>

            {/* Módulo Admin */}
            <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
              <Route path="/admin" element={<AdminDashboard />} />
              <Route path="/admin/especialidades" element={<AdminEspecialidades />} />
              <Route path="/admin/doctores" element={<AdminDoctores />} />
              <Route path="/admin/doctores/nuevo" element={<AdminNuevoDoctor />} />
              <Route path="/admin/reportes" element={<AdminReportes />} />
            </Route>
          </Route>

          {/* Ruta no encontrada */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
  );
};

const App = () => (
  <ColorModeProvider>
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  </ColorModeProvider>
);

export default App;
