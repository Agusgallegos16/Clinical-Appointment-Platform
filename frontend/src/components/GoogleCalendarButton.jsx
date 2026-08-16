import React, { useState, useEffect } from 'react';
import { Box, Button, Chip, CircularProgress, Alert, Tooltip } from '@mui/material';
import {
  CheckCircle as CheckCircleIcon,
  LinkOff as LinkOffIcon,
  EventAvailable as EventAvailableIcon,
} from '@mui/icons-material';
import { googleCalendarService } from '../api/googleCalendarService';

const GoogleCalendarButton = () => {
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState(null);

  const checkStatus = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await googleCalendarService.obtenerEstado();
      if (res && typeof res.connected === 'boolean') {
        setConnected(res.connected);
      }
    } catch (err) {
      console.warn('Google Calendar status check skipped/failed:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkStatus();
  }, []);

  const handleConnect = async () => {
    setActionLoading(true);
    setError(null);
    try {
      const res = await googleCalendarService.obtenerUrlAuth();
      if (res && res.url) {
        window.location.href = res.url;
      } else {
        setError('No se pudo obtener la URL de Google Calendar.');
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Error al conectar con Google. Verifique la configuración.';
      setError(msg);
    } finally {
      setActionLoading(false);
    }
  };

  const handleDisconnect = async () => {
    setActionLoading(true);
    setError(null);
    try {
      await googleCalendarService.desconectar();
      setConnected(false);
    } catch (err) {
      setError('No se pudo desconectar Google Calendar.');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <Box display="flex" alignItems="center" gap={1}>
        <CircularProgress size={18} />
      </Box>
    );
  }

  return (
    <Box display="flex" flexDirection="column" gap={1} alignItems="flex-start">
      {error && <Alert severity="error" sx={{ py: 0.5, px: 1, fontSize: '0.82rem' }}>{error}</Alert>}

      {connected ? (
        <Box display="flex" alignItems="center" gap={1.5} flexWrap="wrap">
          <Chip
            icon={<CheckCircleIcon />}
            label="Google Calendar Vinculado"
            color="success"
            variant="outlined"
            sx={{ fontWeight: 600, py: 0.5 }}
          />
          <Tooltip title="Desconectar tu cuenta de Google Calendar">
            <Button
              size="small"
              color="error"
              variant="text"
              startIcon={<LinkOffIcon />}
              onClick={handleDisconnect}
              disabled={actionLoading}
            >
              Desconectar
            </Button>
          </Tooltip>
        </Box>
      ) : (
        <Button
          variant="contained"
          color="primary"
          startIcon={actionLoading ? <CircularProgress size={18} color="inherit" /> : <EventAvailableIcon />}
          onClick={handleConnect}
          disabled={actionLoading}
          sx={{
            textTransform: 'none',
            fontWeight: 600,
            borderRadius: 2,
            background: 'linear-gradient(45deg, #4285F4 30%, #34A853 90%)',
            boxShadow: '0 3px 5px 2px rgba(66, 133, 244, .3)',
            color: 'white',
            '&:hover': {
              background: 'linear-gradient(45deg, #3367D6 30%, #2E7D32 90%)',
            }
          }}
        >
          {actionLoading ? 'Conectando...' : '🔗 Vincular con mi Google Calendar'}
        </Button>
      )}
    </Box>
  );
};

export default GoogleCalendarButton;
