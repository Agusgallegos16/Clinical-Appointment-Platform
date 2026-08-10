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
  ArrowForward as ArrowForwardIcon,
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
        <Box mb={4}>
          <Typography variant="h4" fontWeight={700} color="primary" gutterBottom>
            Panel de Administración General 🛡️
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Seleccioná la opción de gestión administrativa que querés ejecutar:
          </Typography>
        </Box>

        {/* Botones Centrados, más altos y de ancho contenido */}
        <Stack spacing={2.5}>
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
              boxShadow: '0 4px 14px rgba(2, 132, 199, 0.25)',
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Catálogo de Especialidades Médicas
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
              boxShadow: '0 4px 14px rgba(2, 132, 199, 0.25)',
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Nómina Completa de Médicos
              </Typography>
            </Box>
          </Button>

          <Button
            variant="contained"
            color="primary"
            size="large"
            fullWidth
            onClick={() => navigate('/admin/doctores/nuevo')}
            startIcon={<PersonAddIcon sx={{ fontSize: 36 }} />}
            endIcon={<ArrowForwardIcon />}
            sx={{
              py: 2.8,
              px: 3.5,
              justifyContent: 'space-between',
              fontSize: '1.15rem',
              fontWeight: 700,
              borderRadius: 3,
              boxShadow: '0 4px 14px rgba(2, 132, 199, 0.25)',
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Registrar Nuevo Doctor en el Sistema
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
              boxShadow: '0 4px 14px rgba(2, 132, 199, 0.25)',
            }}
          >
            <Box textAlign="left">
              <Typography variant="h6" fontWeight={700}>
                Ejecutar Reportes por Correo Electrónico
              </Typography>
            </Box>
          </Button>
        </Stack>
      </Box>
    </Box>
  );
};

export default AdminDashboard;
