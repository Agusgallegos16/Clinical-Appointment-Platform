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
    <Box maxWidth="800px" mx="auto">
      <Box mb={4} textAlign="center">
        <Typography variant="h4" fontWeight={700} color="primary" gutterBottom>
          Panel de Administración General 🛡️
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Seleccioná la opción de gestión administrativa que querés ejecutar:
        </Typography>
      </Box>

      {/* Botones Grandes Vertically Stacked sin Números y con el mismo color */}
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
            py: 2.5,
            px: 4,
            justifyContent: 'space-between',
            fontSize: '1.2rem',
            fontWeight: 700,
            borderRadius: 3,
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
            py: 2.5,
            px: 4,
            justifyContent: 'space-between',
            fontSize: '1.2rem',
            fontWeight: 700,
            borderRadius: 3,
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
            py: 2.5,
            px: 4,
            justifyContent: 'space-between',
            fontSize: '1.2rem',
            fontWeight: 700,
            borderRadius: 3,
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
            py: 2.5,
            px: 4,
            justifyContent: 'space-between',
            fontSize: '1.2rem',
            fontWeight: 700,
            borderRadius: 3,
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
  );
};

export default AdminDashboard;
