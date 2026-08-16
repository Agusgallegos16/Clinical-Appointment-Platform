import React, { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  AppBar,
  Box,
  CssBaseline,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  Avatar,
  Chip,
  Button,
  Tooltip,
  useTheme,
} from '@mui/material';
import {
  Menu as MenuIcon,
  Dashboard as DashboardIcon,
  CalendarMonth as CalendarIcon,
  EventNote as EventNoteIcon,
  Schedule as ScheduleIcon,
  FolderCopy as TemplateIcon,
  LocalHospital as HospitalIcon,
  MedicalServices as MedicalIcon,
  PersonAdd as PersonAddIcon,
  Assessment as ReportIcon,
  ManageAccounts as ManageAccountsIcon,
  LightMode as LightModeIcon,
  DarkMode as DarkModeIcon,
  Logout as LogoutIcon,
  ChildCare as ChildIcon,
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import { useColorMode } from '../context/ColorModeContext';
import Footer from './Footer';
import { CLINIC_CONFIG } from '../config/clinicConfig';

const drawerWidth = 260;

const MainLayout = () => {
  const [mobileOpen, setMobileOpen] = useState(false);
  const { user, role, logout } = useAuth();
  const { toggleColorMode, mode } = useColorMode();
  const navigate = useNavigate();
  const location = useLocation();
  const theme = useTheme();

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  // Obtener items del menú según el rol autenticado
  const getMenuItems = () => {
    switch (role) {
      case 'PACIENTE':
        return [
          { text: 'Inicio', icon: <DashboardIcon />, path: '/paciente' },
          { text: 'Reservar Turno', icon: <EventNoteIcon />, path: '/paciente/reservar' },
          { text: 'Mis Turnos', icon: <CalendarIcon />, path: '/paciente/turnos' },
          { text: 'Gestionar menores a cargo', icon: <ChildIcon />, path: '/paciente/menores' },
        ];
      case 'DOCTOR':
        return [
          { text: 'Inicio', icon: <DashboardIcon />, path: '/doctor' },
          { text: 'Mi Agenda', icon: <CalendarIcon />, path: '/doctor/agenda' },
          { text: 'Mis Horarios', icon: <ScheduleIcon />, path: '/doctor/horarios' },
          { text: 'Mis Plantillas', icon: <TemplateIcon />, path: '/doctor/plantillas' },
        ];
      case 'ADMIN':
        return [
          { text: 'Inicio', icon: <DashboardIcon />, path: '/admin' },
          { text: 'Gestionar Usuarios', icon: <ManageAccountsIcon />, path: '/admin/usuarios' },
          { text: 'Especialidades', icon: <MedicalIcon />, path: '/admin/especialidades' },
          { text: 'Doctores', icon: <HospitalIcon />, path: '/admin/doctores' },
          { text: 'Registrar Doctor', icon: <PersonAddIcon />, path: '/admin/doctores/nuevo' },
          { text: 'Reportes y Notificaciones', icon: <ReportIcon />, path: '/admin/reportes' },
        ];
      default:
        return [];
    }
  };

  const menuItems = getMenuItems();

  const drawerContent = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Toolbar sx={{ px: 2, display: 'flex', alignItems: 'center', gap: 1.5 }}>
        <HospitalIcon color="primary" sx={{ fontSize: 32 }} />
        <Box>
          <Typography variant="h6" color="primary" noWrap sx={{ fontWeight: 700, lineHeight: 1.2, maxWidth: 180 }}>
            {CLINIC_CONFIG.name}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Gestión de Turnos
          </Typography>
        </Box>
      </Toolbar>
      <Divider />
      <List sx={{ flexGrow: 1, px: 1.5, py: 2 }}>
        {menuItems.map((item) => {
          const isSelected = location.pathname === item.path;
          return (
            <ListItem key={item.text} disablePadding sx={{ mb: 0.5 }}>
              <ListItemButton
                onClick={() => {
                  navigate(item.path);
                  if (mobileOpen) setMobileOpen(false);
                }}
                selected={isSelected}
                sx={{
                  borderRadius: 2,
                  '&.Mui-selected': {
                    backgroundColor: theme.palette.primary.main,
                    color: '#ffffff',
                    '& .MuiListItemIcon-root': { color: '#ffffff' },
                    '&:hover': {
                      backgroundColor: theme.palette.primary.dark,
                    },
                  },
                }}
              >
                <ListItemIcon sx={{ minWidth: 40, color: isSelected ? '#ffffff' : 'text.secondary' }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText primary={item.text} primaryTypographyProps={{ fontWeight: isSelected ? 600 : 500 }} />
              </ListItemButton>
            </ListItem>
          );
        })}
      </List>
      <Divider />
      {/* Perfil inferior en el menú lateral */}
      <Box sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 1.5 }}>
        <Avatar sx={{ bgcolor: theme.palette.primary.main, fontWeight: 600 }}>
          {user?.email?.charAt(0).toUpperCase()}
        </Avatar>
        <Box sx={{ overflow: 'hidden', flexGrow: 1 }}>
          <Typography variant="subtitle2" noWrap sx={{ fontWeight: 600 }}>
            {user?.email}
          </Typography>
          <Chip label={role} size="small" color="primary" variant="outlined" sx={{ height: 20, fontSize: '0.7rem' }} />
        </Box>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <CssBaseline />
      
      {/* AppBar flotante (Solo visible en dispositivos móviles) */}
      <AppBar
        position="fixed"
        elevation={0}
        sx={{
          display: { xs: 'flex', sm: 'none' },
          backgroundColor: theme.palette.background.paper,
          color: theme.palette.text.primary,
          borderBottom: `1px solid ${theme.palette.divider}`,
        }}
      >
        <Toolbar display="flex" justifyContent="space-between">
          <IconButton
            color="inherit"
            edge="start"
            onClick={handleDrawerToggle}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" fontWeight={700} color="primary" noWrap sx={{ maxWidth: 200 }}>
            {CLINIC_CONFIG.name}
          </Typography>
        </Toolbar>
      </AppBar>

      {/* Drawer Navegación Lateral */}
      <Box component="nav" sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', sm: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
        >
          {drawerContent}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', sm: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth, borderRight: `1px solid ${theme.palette.divider}` },
          }}
          open
        >
          {drawerContent}
        </Drawer>
      </Box>

      {/* Contenido Principal */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { sm: `calc(100% - ${drawerWidth}px)` },
          minHeight: '100vh',
          backgroundColor: theme.palette.background.default,
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Toolbar sx={{ display: { xs: 'block', sm: 'none' } }} />

        {/* Barra Transparente / Invisible en Esquina Superior Derecha */}
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            gap: 1.5,
            mb: 1,
            backgroundColor: 'transparent',
          }}
        >
          {/* Toggle Modo Oscuro / Claro */}
          <Tooltip title={mode === 'dark' ? 'Modo Claro' : 'Modo Oscuro'}>
            <IconButton onClick={toggleColorMode} color="inherit">
              {mode === 'dark' ? <LightModeIcon sx={{ color: '#fba919' }} /> : <DarkModeIcon />}
            </IconButton>
          </Tooltip>

          {/* Botón Logout */}
          <Button
            variant="outlined"
            color="error"
            size="small"
            startIcon={<LogoutIcon />}
            onClick={handleLogout}
            sx={{ borderRadius: 2 }}
          >
            Salir
          </Button>
        </Box>

        <Box sx={{ flexGrow: 1 }}>
          <Outlet />
        </Box>
        <Footer />
      </Box>
    </Box>
  );
};

export default MainLayout;
