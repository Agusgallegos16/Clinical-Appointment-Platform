import React from 'react';
import {
  Grid,
  TextField,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';

const ModalEditarHorario = ({
  editingHorario,
  onClose,
  doctorInfo,
  editFormData,
  setEditFormData,
  todayStr,
  saving,
  handleSaveEditHorario,
}) => {
  return (
    <Dialog open={!!editingHorario} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Configurar Parámetros de la Franja Horaria ⚙️</DialogTitle>
      <DialogContent dividers>
        <Grid container spacing={2}>
          {doctorInfo?.especialidades && doctorInfo.especialidades.length > 0 && (
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel>Especialidad Médica</InputLabel>
                <Select
                  value={editFormData.especialidadId}
                  onChange={(e) => setEditFormData({ ...editFormData, especialidadId: e.target.value })}
                  label="Especialidad Médica"
                >
                  {doctorInfo.especialidades.map((esp) => (
                    <MenuItem key={esp.id} value={esp.id}>
                      {esp.nombre}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
          )}

          <Grid item xs={6}>
            <TextField
              fullWidth
              label="Hora Inicio"
              type="time"
              value={editFormData.horaInicio}
              onChange={(e) => setEditFormData({ ...editFormData, horaInicio: e.target.value })}
              InputLabelProps={{ shrink: true }}
            />
          </Grid>
          <Grid item xs={6}>
            <TextField
              fullWidth
              label="Hora Fin"
              type="time"
              value={editFormData.horaFin}
              onChange={(e) => setEditFormData({ ...editFormData, horaFin: e.target.value })}
              InputLabelProps={{ shrink: true }}
            />
          </Grid>

          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Duración de Turno (Minutos)"
              type="number"
              value={editFormData.duracionTurnoMinutos}
              onChange={(e) => setEditFormData({ ...editFormData, duracionTurnoMinutos: e.target.value })}
            />
          </Grid>

          {!editingHorario?.fecha && (
            <>
              <Grid item xs={6}>
                <TextField
                  fullWidth
                  label="Vigencia Desde (Opcional)"
                  type="date"
                  value={editFormData.fechaDesde}
                  onChange={(e) => setEditFormData({ ...editFormData, fechaDesde: e.target.value })}
                  inputProps={{ min: todayStr }}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={6}>
                <TextField
                  fullWidth
                  label="Vigencia Hasta (Opcional)"
                  type="date"
                  value={editFormData.fechaHasta}
                  onChange={(e) => setEditFormData({ ...editFormData, fechaHasta: e.target.value })}
                  inputProps={{ min: todayStr }}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
            </>
          )}
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button onClick={handleSaveEditHorario} variant="contained" disabled={saving}>
          {saving ? <CircularProgress size={20} color="inherit" /> : 'Guardar Configuración'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ModalEditarHorario;
