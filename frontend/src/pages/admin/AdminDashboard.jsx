import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  Stack,
} from '@mui/material';
import {
  MedicalServices as MedicalIcon,
  LocalHospital as HospitalIcon,
  PersonAdd as PersonAddIcon,
  Assessment as ReportIcon,
  ManageAccounts as ManageAccountsIcon,
  ArrowForward as ArrowForwardIcon,
  Collections as GalleryIcon,
} from '@mui/icons-material';

const AdminDashboard = () => {
  const navigate = useNavigate();

  return (
    <Box
      sx={{
        minHeight: '75vh',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        width: '100%',
        py: 3,
      }}
    >
      <Box maxWidth="480px" width="100%" textAlign="center">
        {/* Banner de Saludo Unificado */}
        <Box mb={4}>
          <Typography variant="h4" fontWeight={700} color="primary" gutterBottom>
            Panel de Administración 🛡️
          </Typography>
          <Typography variant="body1" color="text.secondary">
            ¿Qué te gustaría hacer hoy?
          </Typography>
        </Box>

        {/* Botones de Acción con Estética Premium */}
        <Stack spacing={2.5}>
          <Button
            variant="contained"
            color="primary"
            size="large"
            fullWidth
            onClick={() => navigate('/admin/usuarios')}
            startIcon={<ManageAccountsIcon sx={{ fontSize: 36 }} />}
            endIcon={<ArrowForwardIcon />}
            sx={{
              py: 2.8,
              px: 3.5,
              justifyContent: 'space-between',
              fontSize: '1.15rem',
              fontWeight: 700,
              borderRadius: 3,
              boxShadow: '0 6px 20px rgba(2, 132, 199, 0.28)',
              transition: '0.2s',
              '&:hover': {
                transform: 'translateY(-2px)',
                boxShadow: '0 8px 24px rgba(2, 132, 199, 0.35)',
              },
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Buscar y Gestionar Usuarios
              </Typography>
              <Typography variant="caption" sx={{ opacity: 0.9, display: 'block', fontWeight: 400, mt: 0.3 }}>
                Administrá doctores, secretarias y pacientes registrados
              </Typography>
            </Box>
          </Button>

          <Button
            variant="contained"
            color="primary"
            size="large"
            fullWidth
            onClick={() => navigate('/admin/especialidades')}
            startIcon={<MedicalIcon sx={{ fontSize: 36 }} />}
            endIcon={<ArrowForwardIcon />}
            sx={{
              py: 2.8,
              px: 3.5,
              justifyContent: 'space-between',
              fontSize: '1.15rem',
              fontWeight: 700,
              borderRadius: 3,
              boxShadow: '0 6px 20px rgba(2, 132, 199, 0.28)',
              transition: '0.2s',
              '&:hover': {
                transform: 'translateY(-2px)',
                boxShadow: '0 8px 24px rgba(2, 132, 199, 0.35)',
              },
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Catálogo de Especialidades Médicas
              </Typography>
              <Typography variant="caption" sx={{ opacity: 0.9, display: 'block', fontWeight: 400, mt: 0.3 }}>
                Creá, edita o eliminá especialidades del consultorio
              </Typography>
            </Box>
          </Button>

          <Button
            variant="contained"
            color="primary"
            size="large"
            fullWidth
            onClick={() => navigate('/admin/doctores')}
            startIcon={<HospitalIcon sx={{ fontSize: 36 }} />}
            endIcon={<ArrowForwardIcon />}
            sx={{
              py: 2.8,
              px: 3.5,
              justifyContent: 'space-between',
              fontSize: '1.15rem',
              fontWeight: 700,
              borderRadius: 3,
              boxShadow: '0 6px 20px rgba(2, 132, 199, 0.28)',
              transition: '0.2s',
              '&:hover': {
                transform: 'translateY(-2px)',
                boxShadow: '0 8px 24px rgba(2, 132, 199, 0.35)',
              },
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Nómina Completa de Médicos
              </Typography>
              <Typography variant="caption" sx={{ opacity: 0.9, display: 'block', fontWeight: 400, mt: 0.3 }}>
                Gestioná la lista de profesionales, perfiles y especialidades
              </Typography>
            </Box>
          </Button>

          <Button
            variant="contained"
            color="primary"
            size="large"
            fullWidth
            onClick={() => navigate('/admin/usuarios/nuevo')}
            startIcon={<PersonAddIcon sx={{ fontSize: 36 }} />}
            endIcon={<ArrowForwardIcon />}
            sx={{
              py: 2.8,
              px: 3.5,
              justifyContent: 'space-between',
              fontSize: '1.15rem',
              fontWeight: 700,
              borderRadius: 3,
              boxShadow: '0 6px 20px rgba(2, 132, 199, 0.28)',
              transition: '0.2s',
              '&:hover': {
                transform: 'translateY(-2px)',
                boxShadow: '0 8px 24px rgba(2, 132, 199, 0.35)',
              },
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Registrar Usuario en el Sistema
              </Typography>
              <Typography variant="caption" sx={{ opacity: 0.9, display: 'block', fontWeight: 400, mt: 0.3 }}>
                Dar de alta un nuevo doctor, secretaria o paciente
              </Typography>
            </Box>
          </Button>

          <Button
            variant="contained"
            color="primary"
            size="large"
            fullWidth
            onClick={() => navigate('/admin/galeria')}
            startIcon={<GalleryIcon sx={{ fontSize: 36 }} />}
            endIcon={<ArrowForwardIcon />}
            sx={{
              py: 2.8,
              px: 3.5,
              justifyContent: 'space-between',
              fontSize: '1.15rem',
              fontWeight: 700,
              borderRadius: 3,
              boxShadow: '0 6px 20px rgba(2, 132, 199, 0.28)',
              transition: '0.2s',
              '&:hover': {
                transform: 'translateY(-2px)',
                boxShadow: '0 8px 24px rgba(2, 132, 199, 0.35)',
              },
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Galería de Fotos del Instituto
              </Typography>
              <Typography variant="caption" sx={{ opacity: 0.9, display: 'block', fontWeight: 400, mt: 0.3 }}>
                Subí o desvinculá fotos institucionales del consultorio
              </Typography>
            </Box>
          </Button>

          <Button
            variant="contained"
            color="primary"
            size="large"
            fullWidth
            onClick={() => navigate('/admin/reportes')}
            startIcon={<ReportIcon sx={{ fontSize: 36 }} />}
            endIcon={<ArrowForwardIcon />}
            sx={{
              py: 2.8,
              px: 3.5,
              justifyContent: 'space-between',
              fontSize: '1.15rem',
              fontWeight: 700,
              borderRadius: 3,
              boxShadow: '0 6px 20px rgba(2, 132, 199, 0.28)',
              transition: '0.2s',
              '&:hover': {
                transform: 'translateY(-2px)',
                boxShadow: '0 8px 24px rgba(2, 132, 199, 0.35)',
              },
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Ejecutar Reportes por Correo
              </Typography>
              <Typography variant="caption" sx={{ opacity: 0.9, display: 'block', fontWeight: 400, mt: 0.3 }}>
                Generá reportes automáticos y notificaciones a usuarios
              </Typography>
            </Box>
          </Button>
        </Stack>
      </Box>
    </Box>
  );
};

export default AdminDashboard;
