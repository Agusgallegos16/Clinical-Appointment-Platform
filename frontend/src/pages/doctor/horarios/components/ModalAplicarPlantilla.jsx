import React from 'react';
import {
  Box,
  Typography,
  Card,
  Grid,
  TextField,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  CircularProgress,
  Paper,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Stepper,
  Step,
  StepLabel,
  CardActionArea,
  Divider,
} from '@mui/material';
import {
  FolderCopy as TemplateIcon,
  CheckCircle as CheckCircleIcon,
  ArrowForward as ArrowForwardIcon,
  ArrowBack as ArrowBackIcon,
} from '@mui/icons-material';
import { stepsTemplateWizard } from '../utils/horariosFormatters';

const ModalAplicarPlantilla = ({
  open,
  onClose,
  activeStep,
  plantillas,
  selectedPlantillaId,
  setSelectedPlantillaId,
  tipoAplicacion,
  setTipoAplicacion,
  diaAplicar,
  setDiaAplicar,
  todayStr,
  fechaDesdeAplicar,
  setFechaDesdeAplicar,
  fechaHastaAplicar,
  setFechaHastaAplicar,
  fechaAplicar,
  setFechaAplicar,
  plantillaSeleccionadaNombre,
  applying,
  handleBack,
  handleNext,
  handleAplicar,
}) => {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ fontWeight: 700, pb: 1 }}>
        🟣 Aplicar Plantilla de Horario
      </DialogTitle>

      <DialogContent dividers>
        <Stepper activeStep={activeStep} alternativeLabel sx={{ mb: 3 }}>
          {stepsTemplateWizard.map((label) => (
            <Step key={label}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>

        {/* PASO 0: SELECCIONAR PLANTILLA */}
        {activeStep === 0 && (
          <Box>
            <Typography variant="subtitle1" fontWeight={700} mb={1}>
              Paso 1: Selecciona una Plantilla Guardada 📂
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              Elegí la plantilla de turnos previamente configurada que deseas aplicar.
            </Typography>

            <Grid container spacing={1.5}>
              {plantillas.map((p) => {
                const isSelected = Number(selectedPlantillaId) === Number(p.id);
                return (
                  <Grid item xs={12} key={p.id}>
                    <Card
                      variant="outlined"
                      onClick={() => setSelectedPlantillaId(p.id)}
                      sx={{
                        borderRadius: 2,
                        borderColor: isSelected ? 'secondary.main' : 'divider',
                        bgcolor: isSelected ? 'secondary.50' : 'background.paper',
                      }}
                    >
                      <CardActionArea sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Box display="flex" alignItems="center" gap={1.5}>
                          <TemplateIcon color={isSelected ? 'secondary' : 'action'} />
                          <Box>
                            <Typography fontWeight={700} color={isSelected ? 'secondary.dark' : 'text.primary'}>
                              {p.nombre}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              {p.especialidad ? p.especialidad.nombre : 'Medicina General'}
                            </Typography>
                          </Box>
                        </Box>
                        {isSelected && <CheckCircleIcon color="secondary" />}
                      </CardActionArea>
                    </Card>
                  </Grid>
                );
              })}
            </Grid>
          </Box>
        )}

        {/* PASO 1: DÍA Y VIGENCIA DE APLICACIÓN */}
        {activeStep === 1 && (
          <Box>
            <Typography variant="subtitle1" fontWeight={700} mb={1}>
              Paso 2: ¿Cuándo querés aplicar esta plantilla? 📅
            </Typography>

            <Grid container spacing={2} mb={3}>
              <Grid item xs={6}>
                <Card
                  variant="outlined"
                  onClick={() => setTipoAplicacion('RECURRENTE')}
                  sx={{
                    borderRadius: 2,
                    borderColor: tipoAplicacion === 'RECURRENTE' ? 'secondary.main' : 'divider',
                    bgcolor: tipoAplicacion === 'RECURRENTE' ? 'secondary.50' : 'background.paper',
                  }}
                >
                  <CardActionArea sx={{ p: 2, textAlign: 'center' }}>
                    <Typography fontWeight={700} color={tipoAplicacion === 'RECURRENTE' ? 'secondary.dark' : 'text.primary'}>
                      Semanal Recurrente
                    </Typography>
                  </CardActionArea>
                </Card>
              </Grid>

              <Grid item xs={6}>
                <Card
                  variant="outlined"
                  onClick={() => setTipoAplicacion('PUNTUAL')}
                  sx={{
                    borderRadius: 2,
                    borderColor: tipoAplicacion === 'PUNTUAL' ? 'secondary.main' : 'divider',
                    bgcolor: tipoAplicacion === 'PUNTUAL' ? 'secondary.50' : 'background.paper',
                  }}
                >
                  <CardActionArea sx={{ p: 2, textAlign: 'center' }}>
                    <Typography fontWeight={700} color={tipoAplicacion === 'PUNTUAL' ? 'secondary.dark' : 'text.primary'}>
                      Fecha Puntual
                    </Typography>
                  </CardActionArea>
                </Card>
              </Grid>
            </Grid>

            {tipoAplicacion === 'RECURRENTE' ? (
              <>
                <FormControl fullWidth margin="normal">
                  <InputLabel>Día de la Semana</InputLabel>
                  <Select
                    value={diaAplicar}
                    onChange={(e) => setDiaAplicar(e.target.value)}
                    label="Día de la Semana"
                  >
                    {['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'].map((d) => (
                      <MenuItem key={d} value={d}>{d}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <Typography variant="caption" color="text.secondary" display="block" mt={1} mb={0.5}>
                  Período de Vigencia (Opcional - Por defecto se configuran 9 semanas)
                </Typography>
                <Grid container spacing={2} sx={{ mt: 0.5 }}>
                  <Grid item xs={6}>
                    <TextField
                      fullWidth
                      label="Vigencia Desde"
                      type="date"
                      value={fechaDesdeAplicar}
                      onChange={(e) => setFechaDesdeAplicar(e.target.value)}
                      inputProps={{ min: todayStr }}
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>
                  <Grid item xs={6}>
                    <TextField
                      fullWidth
                      label="Vigencia Hasta"
                      type="date"
                      value={fechaHastaAplicar}
                      onChange={(e) => setFechaHastaAplicar(e.target.value)}
                      inputProps={{ min: fechaDesdeAplicar || todayStr }}
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>
                </Grid>
              </>
            ) : (
              <TextField
                fullWidth
                margin="normal"
                label="Fecha Puntual"
                type="date"
                value={fechaAplicar}
                onChange={(e) => setFechaAplicar(e.target.value)}
                required
                inputProps={{ min: todayStr }}
                InputLabelProps={{ shrink: true }}
              />
            )}
          </Box>
        )}

        {/* PASO 2: RESUMEN Y CONFIRMACIÓN */}
        {activeStep === 2 && (
          <Box>
            <Typography variant="subtitle1" fontWeight={700} mb={1}>
              Paso 3: Confirmación de Aplicación 📋
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              Revisá los datos de la plantilla antes de aplicar los turnos.
            </Typography>

            <Paper variant="outlined" sx={{ p: 2.5, bgcolor: 'secondary.50', borderRadius: 3, mb: 2 }}>
              <Box display="flex" alignItems="center" gap={1} mb={1.5}>
                <TemplateIcon color="secondary" />
                <Typography fontWeight={700} color="secondary.dark" variant="h6">
                  Plantilla: {plantillaSeleccionadaNombre}
                </Typography>
              </Box>
              <Divider sx={{ my: 1 }} />

              <Typography variant="body2" color="text.secondary" mt={1}>Frecuencia / Aplica A:</Typography>
              <Typography variant="subtitle2" fontWeight={700}>
                {tipoAplicacion === 'RECURRENTE' ? `Todos los ${diaAplicar}` : `Fecha Puntual: ${fechaAplicar}`}
              </Typography>

              {tipoAplicacion === 'RECURRENTE' && (fechaDesdeAplicar || fechaHastaAplicar) && (
                <>
                  <Typography variant="body2" color="text.secondary" mt={1}>Período de Vigencia:</Typography>
                  <Typography variant="subtitle2" fontWeight={700} color="secondary.dark">
                    Desde: {fechaDesdeAplicar || 'Inmediata'} ➔ Hasta: {fechaHastaAplicar || 'Indefinida'}
                  </Typography>
                </>
              )}
            </Paper>
          </Box>
        )}
      </DialogContent>

      <DialogActions sx={{ p: 2.5, justifyContent: 'space-between' }}>
        <Button
          disabled={activeStep === 0 || applying}
          onClick={handleBack}
          startIcon={<ArrowBackIcon />}
        >
          Anterior
        </Button>

        {activeStep < stepsTemplateWizard.length - 1 ? (
          <Button
            variant="contained"
            color="secondary"
            onClick={handleNext}
            endIcon={<ArrowForwardIcon />}
          >
            Siguiente
          </Button>
        ) : (
          <Button
            variant="contained"
            color="secondary"
            onClick={handleAplicar}
            disabled={applying}
            startIcon={applying ? <CircularProgress size={20} color="inherit" /> : <CheckCircleIcon />}
            sx={{ fontWeight: 700, px: 3 }}
          >
            Confirmar y Aplicar Plantilla
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};

export default ModalAplicarPlantilla;
