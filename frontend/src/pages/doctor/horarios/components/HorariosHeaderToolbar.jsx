import React from 'react';
import { Box, Typography, Chip, Alert } from '@mui/material';
import { Schedule as ScheduleIcon } from '@mui/icons-material';

const HorariosHeaderToolbar = ({ totalTurnosSemana, success }) => {
  return (
    <>
      <Box display="flex" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={2} mb={3}>
        <Box>
          <Typography variant="h4" fontWeight={700} color="primary" mb={0.5}>
            Gestión de Horarios por Especialidad
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Configurá tus días y horarios de atención médica.
          </Typography>
        </Box>

        <Chip
          icon={<ScheduleIcon />}
          label={`${totalTurnosSemana} turnos activos esta semana`}
          color="primary"
          sx={{ fontWeight: 700, fontSize: '0.95rem', py: 2, px: 1 }}
        />
      </Box>

      {success && (
        <Alert severity="success" sx={{ mb: 3 }}>
          {success}
        </Alert>
      )}
    </>
  );
};

export default HorariosHeaderToolbar;
