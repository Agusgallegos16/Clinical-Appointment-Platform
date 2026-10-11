import React, { useState } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  Collapse,
  Alert,
  Divider,
  Stack,
} from '@mui/material';
import {
  ErrorOutline as ErrorIcon,
  Refresh as RefreshIcon,
  Home as HomeIcon,
  BugReport as BugReportIcon,
} from '@mui/icons-material';

const ErrorFallback = ({
  error,
  errorInfo,
  resetErrorBoundary,
  isGlobal = false,
  title = 'Ocurrió un error inesperado en la aplicación',
  message = 'Se produjo una interrupción en la carga o renderizado de esta sección. Podés reintentar la acción o volver al inicio.',
}) => {
  const [showDetails, setShowDetails] = useState(false);

  const handleGoHome = () => {
    window.location.href = '/';
  };

  const handleReload = () => {
    window.location.reload();
  };

  return (
    <Box
      display="flex"
      justifyContent="center"
      alignItems="center"
      minHeight={isGlobal ? '80vh' : '400px'}
      p={3}
    >
      <Paper
        elevation={3}
        sx={{
          p: { xs: 3, md: 4 },
          maxWidth: 650,
          width: '100%',
          borderRadius: 3,
          textAlign: 'center',
          border: '1px solid',
          borderColor: 'divider',
        }}
      >
        <Box
          display="inline-flex"
          p={2}
          borderRadius="50%"
          bgcolor="error.light"
          color="error.contrastText"
          mb={2}
        >
          <ErrorIcon sx={{ fontSize: 44, color: 'error.main' }} />
        </Box>

        <Typography variant="h5" fontWeight={700} gutterBottom color="text.primary">
          {title}
        </Typography>

        <Typography variant="body1" color="text.secondary" mb={3}>
          {message}
        </Typography>

        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1.5}
          justifyContent="center"
          mb={2}
        >
          {resetErrorBoundary && (
            <Button
              variant="contained"
              color="primary"
              startIcon={<RefreshIcon />}
              onClick={resetErrorBoundary}
              sx={{ fontWeight: 600 }}
            >
              Reintentar
            </Button>
          )}

          <Button
            variant="outlined"
            color="primary"
            startIcon={<HomeIcon />}
            onClick={handleGoHome}
            sx={{ fontWeight: 600 }}
          >
            Ir al Inicio
          </Button>

          <Button
            variant="text"
            color="inherit"
            onClick={handleReload}
            sx={{ fontWeight: 500 }}
          >
            Recargar Página
          </Button>
        </Stack>

        {error && (
          <Box mt={2}>
            <Button
              size="small"
              color="inherit"
              startIcon={<BugReportIcon />}
              onClick={() => setShowDetails((prev) => !prev)}
              sx={{ textTransform: 'none', color: 'text.secondary', fontSize: '0.8rem' }}
            >
              {showDetails ? 'Ocultar detalles técnicos' : 'Ver detalles técnicos'}
            </Button>

            <Collapse in={showDetails}>
              <Divider sx={{ my: 1.5 }} />
              <Alert
                severity="error"
                sx={{
                  textAlign: 'left',
                  fontFamily: 'monospace',
                  fontSize: '0.78rem',
                  maxHeight: 180,
                  overflowY: 'auto',
                  wordBreak: 'break-all',
                }}
              >
                <strong>Error:</strong> {error.toString()}
                {errorInfo?.componentStack && (
                  <pre style={{ margin: '8px 0 0 0', whiteSpace: 'pre-wrap' }}>
                    {errorInfo.componentStack}
                  </pre>
                )}
              </Alert>
            </Collapse>
          </Box>
        )}
      </Paper>
    </Box>
  );
};

export default ErrorFallback;
