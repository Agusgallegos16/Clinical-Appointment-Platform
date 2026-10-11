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
  Alert,
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
  MedicalServices as MedicalIcon,
  CheckCircle as CheckCircleIcon,
  ArrowForward as ArrowForwardIcon,
  ArrowBack as ArrowBackIcon,
} from '@mui/icons-material';
import { stepsManualWizard } from '../utils/horariosFormatters';

const ModalFranjaManual = ({
  open,
  onClose,
  activeStep,
  doctorInfo,
  selectedEspecialidadId,
  setSelectedEspecialidadId,
  tipoHorario,
  setTipoHorario,
  diaSemana,
  setDiaSemana,
  todayStr,
  fechaDesde,
  setFechaDesde,
  fechaHasta,
  setFechaHasta,
  fechaPuntual,
  setFechaPuntual,
  horaInicio,
  setHoraInicio,
  horaFin,
  setHoraFin,
  duracionTurno,
  setDuracionTurno,
  especialidadSeleccionadaNombre,
  saving,
  handleBack,
  handleNext,
  handleGuardar,
}) => {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ fontWeight: 700, pb: 1 }}>
        ➕ Agregar Franja De Turnos
      </DialogTitle>

      <DialogContent dividers>
        <Stepper activeStep={activeStep} alternativeLabel sx={{ mb: 3 }}>
          {stepsManualWizard.map((label) => (
            <Step key={label}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>

        {/* PASO 0: ESPECIALIDAD MÉDICA */}
        {activeStep === 0 && (
          <Box>
            <Typography variant="subtitle1" fontWeight={700} mb={1}>
              Paso 1: Selecciona la Especialidad Médica 🩺
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              Indica para cuál de tus especialidades habilitarás este horario de atención.
            </Typography>

            {doctorInfo?.especialidades && doctorInfo.especialidades.length > 0 ? (
              <Grid container spacing={1.5}>
                {doctorInfo.especialidades.map((esp) => {
                  const isSelected = Number(selectedEspecialidadId) === Number(esp.id);
                  return (
                    <Grid item xs={12} key={esp.id}>
                      <Card
                        variant="outlined"
                        onClick={() => setSelectedEspecialidadId(esp.id)}
                        sx={{
                          borderRadius: 2,
                          borderColor: isSelected ? 'primary.main' : 'divider',
                          bgcolor: isSelected ? 'primary.50' : 'background.paper',
                          boxShadow: isSelected ? '0 0 0 2px #0284c7' : 'none',
                        }}
                      >
                        <CardActionArea sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Box display="flex" alignItems="center" gap={1.5}>
                            <MedicalIcon color={isSelected ? 'primary' : 'action'} />
                            <Typography fontWeight={700} color={isSelected ? 'primary.main' : 'text.primary'}>
                              {esp.nombre}
                            </Typography>
                          </Box>
                          {isSelected && <CheckCircleIcon color="primary" />}
                        </CardActionArea>
                      </Card>
                    </Grid>
                  );
                })}
              </Grid>
            ) : (
              <Alert severity="info">Atendiendo como Medicina General</Alert>
            )}
          </Box>
        )}

        {/* PASO 1: TIPO DE CONFIGURACIÓN Y FECHA / DÍA */}
        {activeStep === 1 && (
          <Box>
            <Typography variant="subtitle1" fontWeight={700} mb={1}>
              Paso 2: ¿Cómo querés aplicar este horario? 📅
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              Elegí si es un horario que se repite todas las semanas o una fecha específica única.
            </Typography>

            <Grid container spacing={2} mb={3}>
              <Grid item xs={6}>
                <Card
                  variant="outlined"
                  onClick={() => setTipoHorario('RECURRENTE')}
                  sx={{
                    borderRadius: 2,
                    borderColor: tipoHorario === 'RECURRENTE' ? 'primary.main' : 'divider',
                    bgcolor: tipoHorario === 'RECURRENTE' ? 'primary.50' : 'background.paper',
                    boxShadow: tipoHorario === 'RECURRENTE' ? '0 0 0 2px #0284c7' : 'none',
                  }}
                >
                  <CardActionArea sx={{ p: 2, textAlign: 'center' }}>
                    <Typography fontWeight={700} color={tipoHorario === 'RECURRENTE' ? 'primary.main' : 'text.primary'}>
                      Semanal Recurrente
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Ej: Todos los Lunes
                    </Typography>
                  </CardActionArea>
                </Card>
              </Grid>

              <Grid item xs={6}>
                <Card
                  variant="outlined"
                  onClick={() => setTipoHorario('PUNTUAL')}
                  sx={{
                    borderRadius: 2,
                    borderColor: tipoHorario === 'PUNTUAL' ? 'primary.main' : 'divider',
                    bgcolor: tipoHorario === 'PUNTUAL' ? 'primary.50' : 'background.paper',
                    boxShadow: tipoHorario === 'PUNTUAL' ? '0 0 0 2px #0284c7' : 'none',
                  }}
                >
                  <CardActionArea sx={{ p: 2, textAlign: 'center' }}>
                    <Typography fontWeight={700} color={tipoHorario === 'PUNTUAL' ? 'primary.main' : 'text.primary'}>
                      Fecha Puntual
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Día único específico
                    </Typography>
                  </CardActionArea>
                </Card>
              </Grid>
            </Grid>

            {tipoHorario === 'RECURRENTE' ? (
              <>
                <FormControl fullWidth margin="normal">
                  <InputLabel>Día de la Semana</InputLabel>
                  <Select
                    value={diaSemana}
                    onChange={(e) => setDiaSemana(e.target.value)}
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

                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <TextField
                      fullWidth
                      label="Vigencia Desde"
                      type="date"
                      value={fechaDesde}
                      onChange={(e) => setFechaDesde(e.target.value)}
                      inputProps={{ min: todayStr }}
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>
                  <Grid item xs={6}>
                    <TextField
                      fullWidth
                      label="Vigencia Hasta"
                      type="date"
                      value={fechaHasta}
                      onChange={(e) => setFechaHasta(e.target.value)}
                      inputProps={{ min: fechaDesde || todayStr }}
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>
                </Grid>
              </>
            ) : (
              <TextField
                fullWidth
                margin="normal"
                label="Seleccionar Fecha Puntual"
                type="date"
                value={fechaPuntual}
                onChange={(e) => setFechaPuntual(e.target.value)}
                required
                inputProps={{ min: todayStr }}
                InputLabelProps={{ shrink: true }}
              />
            )}
          </Box>
        )}

        {/* PASO 2: HORARIOS Y DURACIÓN DE TURNO */}
        {activeStep === 2 && (
          <Box>
            <Typography variant="subtitle1" fontWeight={700} mb={1}>
              Paso 3: Definí el Rango Horario y Duración ⏰
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              Indicá la hora de inicio, hora de fin y cuánto durará cada consulta médica.
            </Typography>

            <Grid container spacing={2} mb={2}>
              <Grid item xs={6}>
                <TextField
                  fullWidth
                  label="Hora de Inicio"
                  type="time"
                  value={horaInicio}
                  onChange={(e) => setHoraInicio(e.target.value)}
                  required
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={6}>
                <TextField
                  fullWidth
                  label="Hora de Fin"
                  type="time"
                  value={horaFin}
                  onChange={(e) => setHoraFin(e.target.value)}
                  required
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
            </Grid>

            <Typography variant="subtitle2" fontWeight={600} mb={1}>
              Duración de cada turno:
            </Typography>
            <TextField
              fullWidth
              margin="dense"
              label="Duración (En Minutos)"
              type="number"
              value={duracionTurno}
              onChange={(e) => setDuracionTurno(e.target.value)}
              required
            />
          </Box>
        )}

        {/* PASO 3: RESUMEN Y CONFIRMACIÓN */}
        {activeStep === 3 && (
          <Box>
            <Typography variant="subtitle1" fontWeight={700} mb={1}>
              Paso 4: Confirma la Configuración 📋
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              Revisá que los datos ingresados sean correctos antes de habilitar tus turnos.
            </Typography>

            <Paper variant="outlined" sx={{ p: 2.5, bgcolor: 'primary.50', borderRadius: 3, mb: 2 }}>
              <Box display="flex" alignItems="center" gap={1} mb={1.5}>
                <MedicalIcon color="primary" />
                <Typography fontWeight={700} color="primary.main" variant="h6">
                  {especialidadSeleccionadaNombre}
                </Typography>
              </Box>
              <Divider sx={{ my: 1 }} />

              <Grid container spacing={1.5} sx={{ mt: 0.5 }}>
                <Grid item xs={12}>
                  <Typography variant="body2" color="text.secondary">Frecuencia:</Typography>
                  <Typography variant="subtitle2" fontWeight={700}>
                    {tipoHorario === 'RECURRENTE' ? `Todos los ${diaSemana}` : `Fecha Puntual: ${fechaPuntual}`}
                  </Typography>
                </Grid>

                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">Rango Horario:</Typography>
                  <Typography variant="subtitle2" fontWeight={700}>
                    {horaInicio} hs a {horaFin} hs
                  </Typography>
                </Grid>

                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">Duración por Turno:</Typography>
                  <Typography variant="subtitle2" fontWeight={700}>
                    {duracionTurno} minutos
                  </Typography>
                </Grid>

                {tipoHorario === 'RECURRENTE' && (fechaDesde || fechaHasta) && (
                  <Grid item xs={12}>
                    <Typography variant="body2" color="text.secondary">Vigencia:</Typography>
                    <Typography variant="subtitle2" fontWeight={700} color="primary.dark">
                      Desde: {fechaDesde || 'Inmediata'} ➔ Hasta: {fechaHasta || 'Indefinida'}
                    </Typography>
                  </Grid>
                )}
              </Grid>
            </Paper>
          </Box>
        )}
      </DialogContent>

      <DialogActions sx={{ p: 2.5, justifyContent: 'space-between' }}>
        <Button
          disabled={activeStep === 0 || saving}
          onClick={handleBack}
          startIcon={<ArrowBackIcon />}
        >
          Anterior
        </Button>

        {activeStep < stepsManualWizard.length - 1 ? (
          <Button
            variant="contained"
            onClick={handleNext}
            endIcon={<ArrowForwardIcon />}
          >
            Siguiente
          </Button>
        ) : (
          <Button
            variant="contained"
            color="primary"
            onClick={handleGuardar}
            disabled={saving}
            startIcon={saving ? <CircularProgress size={20} color="inherit" /> : <CheckCircleIcon />}
            sx={{ fontWeight: 700, px: 3 }}
          >
            Confirmar y Habilitar Turnos
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};

export default ModalFranjaManual;
