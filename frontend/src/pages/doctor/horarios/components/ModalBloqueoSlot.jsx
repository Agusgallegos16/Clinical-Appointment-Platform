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
import { Block as BlockIcon } from '@mui/icons-material';
import dayjs from 'dayjs';

const ModalBloqueoSlot = ({
  selectedSlotBlock,
  onClose,
  blocking,
  handleBloquearSlotIndividual,
}) => {
  return (
    <Dialog open={!!selectedSlotBlock} onClose={onClose}>
      <DialogTitle>¿Deshabilitar este turno individual?</DialogTitle>
      <DialogContent>
        <DialogContentText>
          Vas a deshabilitar el turno de <strong>{selectedSlotBlock?.especialidadNombre || 'General'}</strong> del día <strong>{dayjs(selectedSlotBlock?.dateStr).format('DD/MM/YYYY')}</strong> de <strong>{selectedSlotBlock?.startText} hs a {selectedSlotBlock?.endText} hs</strong>. Este slot no estará disponible para reserva del paciente.
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button
          onClick={handleBloquearSlotIndividual}
          color="warning"
          variant="contained"
          disabled={blocking}
          startIcon={<BlockIcon />}
        >
          {blocking ? <CircularProgress size={20} color="inherit" /> : 'Deshabilitar Turno'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ModalBloqueoSlot;
