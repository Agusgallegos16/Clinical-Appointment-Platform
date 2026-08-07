import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  TextField,
  Button,
  CircularProgress,
  Alert,
  Paper,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Divider,
} from '@mui/material';
import {
  Add as AddIcon,
  FolderCopy as TemplateIcon,
  PlayArrow as ApplyIcon,
  Delete as DeleteIcon,
  RemoveCircleOutline as RemoveIcon,
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { doctorService } from '../../api/doctorService';

const DoctorPlantillas = () => {
  const { entidadId } = useAuth();
  const [plantillas, setPlantillas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Formulario nueva plantilla con N franjas dinámicas
  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [detalles, setDetalles] = useState([
    { horaInicio: '08:00', horaFin: '12:00', duracionTurnoMinutos: 30 },
    { horaInicio: '14:00', horaFin: '18:00', duracionTurnoMinutos: 30 },
  ]);
  const [saving, setSaving] = useState(false);

  // Modal Aplicar Plantilla
  const [selectedPlantillaApply, setSelectedPlantillaApply] = useState(null);
  const [fechaAplicar, setFechaAplicar] = useState('');
  const [applying, setApplying] = useState(false);

  // Modal Eliminar Plantilla
  const [selectedPlantillaDelete, setSelectedPlantillaDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (entidadId) cargarPlantillas();
  }, [entidadId]);

  const cargarPlantillas = async () => {
    try {
      const data = await doctorService.listarPlantillas(entidadId);
      setPlantillas(data);
    } catch (err) {
      setError('Error al obtener la lista de plantillas.');
    } finally {
      setLoading(false);
    }
  };

  // Agregar nueva franja dinámica
  const handleAgregarFranja = () => {
    setDetalles([
      ...detalles,
      { horaInicio: '18:00', horaFin: '20:00', duracionTurnoMinutos: 30 },
    ]);
  };

  // Eliminar franja
  const handleRemoverFranja = (index) => {
    if (detalles.length <= 1) {
      setError('Una plantilla debe contener al menos una franja horaria.');
      return;
    }
    setDetalles(detalles.filter((_, idx) => idx !== index));
  };

  // Validar superposición de franjas horarias
  const validarSuperposicion = (franjas) => {
    // Ordenar por hora inicio
    const ordenadas = [...franjas].sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));

    for (let i = 0; i < ordenadas.length; i++) {
      const actual = ordenadas[i];
      if (actual.horaInicio >= actual.horaFin) {
        return `En la franja #${i + 1}, la hora de inicio (${actual.horaInicio}) debe ser menor a la hora de fin (${actual.horaFin}).`;
      }

      if (i < ordenadas.length - 1) {
        const siguiente = ordenadas[i + 1];
        if (actual.horaFin > siguiente.horaInicio) {
          return `Superposición detectada entre franjas: la franja (${actual.horaInicio} a ${actual.horaFin}) se superpone con (${siguiente.horaInicio} a ${siguiente.horaFin}).`;
        }
      }
    }
    return null;
  };

  const handleCrearPlantilla = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    // Validar superposiciones
    const errorSuperposicion = validarSuperposicion(detalles);
    if (errorSuperposicion) {
      setError(errorSuperposicion);
      return;
    }

    setSaving(true);

    try {
      await doctorService.crearPlantilla(entidadId, {
        nombre,
        descripcion,
        detalles,
      });

      setSuccess(`¡Plantilla "${nombre}" creada con éxito!`);
      setNombre('');
      setDescripcion('');
      setDetalles([
        { horaInicio: '08:00', horaFin: '12:00', duracionTurnoMinutos: 30 },
        { horaInicio: '14:00', horaFin: '18:00', duracionTurnoMinutos: 30 },
      ]);
      cargarPlantillas();
    } catch (err) {
      setError('Error al crear la plantilla.');
    } finally {
      setSaving(false);
    }
  };

  const handleAplicarPlantilla = async () => {
    if (!selectedPlantillaApply || !fechaAplicar) return;
    setApplying(true);
    setError('');
    setSuccess('');

    try {
      await doctorService.aplicarPlantilla(entidadId, {
        plantillaId: selectedPlantillaApply.id,
        fecha: fechaAplicar,
      });

      setSuccess(`¡Plantilla "${selectedPlantillaApply.nombre}" aplicada con éxito para el día ${fechaAplicar}!`);
      setSelectedPlantillaApply(null);
      setFechaAplicar('');
    } catch (err) {
      setError('Error al aplicar la plantilla a la fecha seleccionada.');
    } finally {
      setApplying(false);
    }
  };

  const handleConfirmEliminarPlantilla = async () => {
    if (!selectedPlantillaDelete) return;
    setDeleting(true);
    setError('');
    try {
      await doctorService.eliminarPlantilla(selectedPlantillaDelete.id);
      setSelectedPlantillaDelete(null);
      setSuccess('Plantilla eliminada correctamente.');
      cargarPlantillas();
    } catch (err) {
      setError('No se pudo eliminar la plantilla.');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} color="primary" mb={1}>
        Plantillas de Agenda Personalizables
      </Typography>
      <Typography variant="body1" color="text.secondary" mb={3}>
        Diseñá estructuras de jornada con múltiples franjas horarias sin superposición y aplicalas en 1 clic.
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

      <Grid container spacing={3}>
        {/* Formulario Crear Plantilla Dinámica */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" fontWeight={600} mb={2}>
              Crear Nueva Plantilla
            </Typography>

            <Box component="form" onSubmit={handleCrearPlantilla}>
              <TextField
                fullWidth
                margin="normal"
                label="Nombre de la Plantilla"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                placeholder="Ej: Jornada Completa con Prácticas"
                required
              />

              <TextField
                fullWidth
                margin="normal"
                label="Descripción"
                value={descripcion}
                onChange={(e) => setDescripcion(e.target.value)}
                placeholder="Ej: Mañana 15m, Tarde 30m, Vespertino 45m"
              />

              <Box display="flex" justifyContent="space-between" alignItems="center" mt={2} mb={1}>
                <Typography variant="subtitle2" fontWeight={600}>
                  Franjas Horarias ({detalles.length}):
                </Typography>
                <Button
                  size="small"
                  startIcon={<AddIcon />}
                  onClick={handleAgregarFranja}
                  variant="outlined"
                >
                  + Agregar Franja
                </Button>
              </Box>

              {detalles.map((d, idx) => (
                <Box key={idx} p={2} border="1px solid #cbd5e1" borderRadius={2} mb={1.5} bgcolor="background.paper">
                  <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                    <Typography variant="subtitle2" color="primary" fontWeight={700}>
                      Franja #{idx + 1}
                    </Typography>
                    {detalles.length > 1 && (
                      <IconButton size="small" color="error" onClick={() => handleRemoverFranja(idx)}>
                        <RemoveIcon />
                      </IconButton>
                    )}
                  </Box>

                  <Grid container spacing={1.5}>
                    <Grid item xs={4}>
                      <TextField
                        fullWidth
                        size="small"
                        label="Hora Inicio"
                        type="time"
                        value={d.horaInicio}
                        onChange={(e) => {
                          const updated = [...detalles];
                          updated[idx].horaInicio = e.target.value;
                          setDetalles(updated);
                        }}
                        InputLabelProps={{ shrink: true }}
                      />
                    </Grid>
                    <Grid item xs={4}>
                      <TextField
                        fullWidth
                        size="small"
                        label="Hora Fin"
                        type="time"
                        value={d.horaFin}
                        onChange={(e) => {
                          const updated = [...detalles];
                          updated[idx].horaFin = e.target.value;
                          setDetalles(updated);
                        }}
                        InputLabelProps={{ shrink: true }}
                      />
                    </Grid>
                    <Grid item xs={4}>
                      <TextField
                        fullWidth
                        size="small"
                        label="Min/Turno"
                        type="number"
                        value={d.duracionTurnoMinutos}
                        onChange={(e) => {
                          const updated = [...detalles];
                          updated[idx].duracionTurnoMinutos = Number(e.target.value);
                          setDetalles(updated);
                        }}
                      />
                    </Grid>
                  </Grid>
                </Box>
              ))}

              <Button
                type="submit"
                variant="contained"
                fullWidth
                size="large"
                startIcon={<AddIcon />}
                disabled={saving || !nombre}
                sx={{ mt: 2 }}
              >
                {saving ? <CircularProgress size={24} color="inherit" /> : 'Guardar Plantilla'}
              </Button>
            </Box>
          </Paper>
        </Grid>

        {/* Lista de Plantillas Guardadas con Opción de Eliminar */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" fontWeight={600} mb={2}>
              Plantillas Guardadas ({plantillas.length})
            </Typography>

            {loading ? (
              <CircularProgress />
            ) : plantillas.length === 0 ? (
              <Alert severity="info">No tenés plantillas creadas por el momento.</Alert>
            ) : (
              <Grid container spacing={2}>
                {plantillas.map((p) => (
                  <Grid item xs={12} key={p.id}>
                    <Card variant="outlined">
                      <CardContent>
                        <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                          <Box display="flex" alignItems="center" gap={1}>
                            <TemplateIcon color="primary" />
                            <Typography variant="subtitle1" fontWeight={700}>
                              {p.nombre}
                            </Typography>
                          </Box>
                          <Box display="flex" gap={0.5}>
                            <Button
                              variant="contained"
                              color="secondary"
                              size="small"
                              startIcon={<ApplyIcon />}
                              onClick={() => setSelectedPlantillaApply(p)}
                            >
                              Aplicar
                            </Button>
                            <IconButton
                              color="error"
                              size="small"
                              onClick={() => setSelectedPlantillaDelete(p)}
                              title="Eliminar Plantilla"
                            >
                              <DeleteIcon />
                            </IconButton>
                          </Box>
                        </Box>
                        <Typography variant="body2" color="text.secondary" mb={1.5}>
                          {p.descripcion || 'Sin descripción'}
                        </Typography>
                        <Divider sx={{ my: 1 }} />
                        <Box display="flex" flexWrap="wrap" gap={1}>
                          {p.detalles?.map((d, idx) => (
                            <Chip
                              key={idx}
                              label={`${d.horaInicio} - ${d.horaFin} (${d.duracionTurnoMinutos}m)`}
                              size="small"
                              variant="outlined"
                              color="primary"
                            />
                          ))}
                        </Box>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            )}
          </Paper>
        </Grid>
      </Grid>

      {/* Modal Aplicar Plantilla a Fecha */}
      <Dialog open={!!selectedPlantillaApply} onClose={() => setSelectedPlantillaApply(null)}>
        <DialogTitle>Aplicar Plantilla "{selectedPlantillaApply?.nombre}"</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" mb={2}>
            Seleccioná la fecha puntual en la que querés desplegar esta estructura de horarios:
          </Typography>
          <TextField
            fullWidth
            type="date"
            label="Fecha de Aplicación"
            value={fechaAplicar}
            onChange={(e) => setFechaAplicar(e.target.value)}
            InputLabelProps={{ shrink: true }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedPlantillaApply(null)}>Cancelar</Button>
          <Button onClick={handleAplicarPlantilla} variant="contained" color="secondary" disabled={applying || !fechaAplicar}>
            {applying ? <CircularProgress size={20} color="inherit" /> : 'Confirmar y Aplicar'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Modal Confirmar Eliminación Plantilla */}
      <Dialog open={!!selectedPlantillaDelete} onClose={() => setSelectedPlantillaDelete(null)}>
        <DialogTitle>¿Eliminar la plantilla "{selectedPlantillaDelete?.nombre}"?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Vas a eliminar esta plantilla de agenda. Esta acción no se puede deshacer.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedPlantillaDelete(null)}>Cancelar</Button>
          <Button onClick={handleConfirmEliminarPlantilla} color="error" variant="contained" disabled={deleting}>
            {deleting ? <CircularProgress size={20} color="inherit" /> : 'Sí, Eliminar'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DoctorPlantillas;
