import React from 'react';
import {
  Button,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
} from '@mui/material';
import {
  Warning as WarningIcon,
  DeleteSweep as DeleteSweepIcon,
} from '@mui/icons-material';

export const ModalError = ({
  errorModal,
  errorModalTitle,
  onClose,
}) => {
  return (
    <Dialog open={!!errorModal} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ color: 'error.main', display: 'flex', alignItems: 'center', gap: 1, fontWeight: 700 }}>
        <WarningIcon color="error" /> {errorModalTitle}
      </DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ color: 'text.primary', fontWeight: 500 }}>
          {errorModal}
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} variant="contained" color="error" fullWidth>
          Entendido
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export const ModalLimpiarSemana = ({
  open,
  onClose,
  currentWeekStart,
  clearingWeek,
  handleLimpiarSemanaActual,
}) => {
  return (
    <Dialog open={open} onClose={onClose}>
      <DialogTitle>¿Borrar todos los turnos de esta semana?</DialogTitle>
      <DialogContent>
        <DialogContentText>
          Se deshabilitarán y eliminarán todos los turnos configurados exclusivamente para la semana del <strong>{currentWeekStart.format('DD/MM/YYYY')}</strong> al <strong>{currentWeekStart.add(6, 'day').format('DD/MM/YYYY')}</strong>. Las demás semanas se conservarán intactas.
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button
          onClick={handleLimpiarSemanaActual}
          color="error"
          variant="contained"
          disabled={clearingWeek}
          startIcon={<DeleteSweepIcon />}
        >
          {clearingWeek ? <CircularProgress size={20} color="inherit" /> : 'Sí, Borrar Turnos de esta Semana'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export const ModalEliminarFranja = ({
  selectedHorarioDelete,
  onClose,
  deleting,
  handleConfirmEliminarHorario,
}) => {
  return (
    <Dialog open={!!selectedHorarioDelete} onClose={onClose}>
      <DialogTitle>¿Eliminar esta franja horaria completa?</DialogTitle>
      <DialogContent>
        <DialogContentText>
          Vas a borrar toda la franja de <strong>{selectedHorarioDelete?.especialidad ? selectedHorarioDelete.especialidad.nombre : 'General'}</strong> ({selectedHorarioDelete?.diaSemana || selectedHorarioDelete?.fecha}: {selectedHorarioDelete?.horaInicio} hs - {selectedHorarioDelete?.horaFin} hs).
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button
          onClick={handleConfirmEliminarHorario}
          color="error"
          variant="contained"
          disabled={deleting}
        >
          {deleting ? <CircularProgress size={20} color="inherit" /> : 'Sí, Eliminar Franja'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
