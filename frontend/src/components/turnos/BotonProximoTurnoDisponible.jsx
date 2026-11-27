import { Alert, Box, Button, CircularProgress } from '@mui/material';
import { EventAvailable as EventAvailableIcon } from '@mui/icons-material';

export default function BotonProximoTurnoDisponible({
  onClick,
  loading = false,
  disabled = false,
  sinDisponibilidad = false,
  error = null,
  mensajeExito = null,
  label = 'Ir al próximo turno disponible',
  loadingLabel = 'Buscando próximo turno...',
  emptyMessage = 'No hay turnos disponibles cargados próximamente para este profesional.',
  variant = 'outlined',
  color = 'secondary',
  size = 'medium',
  fullWidth = false,
  sx = {},
}) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        gap: 1,
        width: fullWidth ? '100%' : 'auto',
        ...sx,
      }}
    >
      <Button
        type="button"
        variant={variant}
        color={color}
        size={size}
        fullWidth={fullWidth}
        onClick={onClick}
        disabled={disabled || loading}
        aria-busy={loading}
        startIcon={
          loading ? (
            <CircularProgress size={18} color="inherit" />
          ) : (
            <EventAvailableIcon />
          )
        }
        sx={{
          fontWeight: 600,
          whiteSpace: 'nowrap',
        }}
      >
        {loading ? loadingLabel : label}
      </Button>

      {error && (
        <Alert severity="error" variant="outlined" sx={{ py: 0.25 }}>
          {error}
        </Alert>
      )}

      {!error && sinDisponibilidad && (
        <Alert severity="info" variant="outlined" sx={{ py: 0.25 }}>
          {emptyMessage}
        </Alert>
      )}

      {!error && !sinDisponibilidad && mensajeExito && (
        <Alert severity="success" variant="outlined" sx={{ py: 0.25 }}>
          {mensajeExito}
        </Alert>
      )}
    </Box>
  );
}
