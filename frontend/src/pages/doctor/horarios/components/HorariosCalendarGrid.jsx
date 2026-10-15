import React from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Button,
  Paper,
  IconButton,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  TableContainer,
  CircularProgress,
  Alert,
} from '@mui/material';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  DeleteSweep as DeleteSweepIcon,
  Settings as SettingsIcon,
  FolderCopy as TemplateIcon,
  ChevronLeft as ChevronLeftIcon,
  ChevronRight as ChevronRightIcon,
  Today as TodayIcon,
  MedicalServices as MedicalIcon,
} from '@mui/icons-material';
import { calcularFechaInicioVigencia, calcularFechaHastaVigencia } from '../utils/horariosFormatters';

const HorariosCalendarGrid = ({
  currentWeekStart,
  handlePrevWeek,
  handleToday,
  handleNextWeek,
  setShowClearWeekModal,
  weekDaysWithDates,
  timeSlots,
  weekSlotsByDate,
  coveredRowsByDate,
  currentMinutesToday,
  setSelectedSlotBlock,
  handleOpenManualWizard,
  plantillas,
  handleOpenTemplateWizard,
  horariosVisiblesEnSemana,
  loading,
  handleOpenEditModal,
  setSelectedHorarioDelete,
}) => {
  return (
    <>
      {/* PLANILLA MATRIZ Y LISTADO DE TURNOS POR DÍA DE LA SEMANA */}
      <Paper sx={{ p: 3, mb: 4, bgcolor: 'background.paper', borderRadius: 3, boxShadow: '0 4px 20px rgba(0,0,0,0.06)' }}>
        <Box display="flex" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={2} mb={2.5}>
          <Box display="flex" alignItems="center" gap={1}>
            <Button
              variant="outlined"
              size="small"
              startIcon={<ChevronLeftIcon />}
              onClick={handlePrevWeek}
            >
              Anterior
            </Button>
            <Button
              variant="outlined"
              size="small"
              startIcon={<TodayIcon />}
              onClick={handleToday}
            >
              Semana Actual
            </Button>
            <Button
              variant="outlined"
              size="small"
              endIcon={<ChevronRightIcon />}
              onClick={handleNextWeek}
            >
              Siguiente
            </Button>
          </Box>

          <Box display="flex" alignItems="center" gap={2}>
            <Typography variant="h6" fontWeight={700} color="primary">
              Semana del {currentWeekStart.format('DD/MM/YYYY')} al {currentWeekStart.add(6, 'day').format('DD/MM/YYYY')}
            </Typography>

            <Button
              variant="outlined"
              color="error"
              size="small"
              startIcon={<DeleteSweepIcon />}
              onClick={() => setShowClearWeekModal(true)}
              sx={{ fontWeight: 700 }}
            >
              Borrar Turnos de esta Semana
            </Button>
          </Box>
        </Box>

        {/* PLANILLA MATRIZ ESTILO EXCEL CON FILTRO GRIS EN DÍAS/HORAS PASADOS */}
        <TableContainer sx={{ maxHeight: 650, borderRadius: 2 }}>
          <Table stickyHeader size="small" border={1} borderColor="#cbd5e1" sx={{ borderCollapse: 'collapse', bgcolor: '#f8fafc' }}>
            <TableHead>
              <TableRow>
                <TableCell sx={{ bgcolor: '#0284c7', color: '#ffffff', fontWeight: 700, width: 90, zIndex: 11, textAlign: 'center' }}>
                  Hora
                </TableCell>
                {weekDaysWithDates.map((dayInfo) => (
                  <TableCell
                    key={dayInfo.dateStr}
                    align="center"
                    sx={{
                      bgcolor: dayInfo.isPastDay ? '#64748b' : '#0284c7',
                      color: '#ffffff',
                      fontWeight: 700,
                      minWidth: 155,
                      zIndex: 11,
                      opacity: dayInfo.isPastDay ? 0.85 : 1,
                    }}
                  >
                    <Typography variant="subtitle2" fontWeight={700}>
                      {dayInfo.formattedHeader} {dayInfo.isPastDay ? '(Pasado)' : ''}
                    </Typography>
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>

            <TableBody>
              {timeSlots.map((timeStr) => {
                const rowStartMin = parseInt(timeStr.split(':')[0], 10) * 60 + parseInt(timeStr.split(':')[1], 10);
                const rowEndMin = rowStartMin + 30;

                return (
                  <TableRow key={timeStr} hover style={{ height: 48 }}>
                    <TableCell sx={{ fontWeight: 700, bgcolor: '#e2e8f0', color: '#0f172a', textAlign: 'center', py: 0.5, height: 48, boxSizing: 'border-box' }}>
                      {timeStr}
                    </TableCell>

                    {weekDaysWithDates.map((dayInfo) => {
                      const dateStr = dayInfo.dateStr;
                      const daySlots = weekSlotsByDate[dateStr] || [];
                      const isPastCell = dayInfo.isPastDay || (dayInfo.isTodayDay && rowEndMin <= currentMinutesToday);

                      if (coveredRowsByDate[dateStr]?.has(rowStartMin)) {
                        return null;
                      }

                      const startingSlots = daySlots.filter(
                        (s) => s.startMinutes >= rowStartMin && s.startMinutes < rowEndMin
                      );

                      if (startingSlots.length > 0) {
                        const maxEndMin = Math.max(...startingSlots.map((s) => s.endMinutes));

                        let rowsSpanned = 1;
                        for (let checkMin = rowStartMin + 30; checkMin < maxEndMin; checkMin += 30) {
                          const hasNextStarting = daySlots.some((s) => s.startMinutes >= checkMin && s.startMinutes < checkMin + 30);
                          if (!hasNextStarting) {
                            rowsSpanned++;
                          } else {
                            break;
                          }
                        }

                        const cellHeightPx = rowsSpanned * 48;

                        return (
                          <TableCell
                            key={dateStr}
                            rowSpan={rowsSpanned}
                            align="center"
                            sx={{
                              p: 0,
                              position: 'relative',
                              verticalAlign: 'top',
                              bgcolor: isPastCell ? '#e2e8f0' : '#f8fafc',
                              height: `${cellHeightPx}px`,
                              boxSizing: 'border-box',
                            }}
                          >
                            <Box sx={{ position: 'relative', width: '100%', height: `${cellHeightPx}px` }}>
                              {startingSlots.map((slot) => {
                                const palette = slot.colorPalette;
                                const isPastSlot = isPastCell;
                                const topOffsetPx = Math.round(((slot.startMinutes - rowStartMin) / 15) * 24);
                                const slotHeightPx = Math.max(22, Math.round((slot.durationMinutes / 15) * 24) - 2);

                                return (
                                  <Box
                                    key={slot.id}
                                    onClick={() => setSelectedSlotBlock(slot)}
                                    sx={{
                                      position: 'absolute',
                                      top: `${topOffsetPx}px`,
                                      left: 2,
                                      right: 2,
                                      height: `${slotHeightPx}px`,
                                      display: 'flex',
                                      alignItems: 'center',
                                      justifyContent: 'center',
                                      textAlign: 'center',
                                      background: isPastSlot
                                        ? 'linear-gradient(135deg, #cbd5e1 0%, #94a3b8 100%)'
                                        : palette.bg,
                                      color: isPastSlot ? '#475569' : palette.text,
                                      border: `1.5px solid ${isPastSlot ? '#64748b' : palette.border}`,
                                      borderRadius: 1.5,
                                      py: 0.2,
                                      px: 0.8,
                                      cursor: 'pointer',
                                      opacity: isPastSlot ? 0.65 : 1,
                                      boxShadow: '0 2px 5px rgba(0,0,0,0.06)',
                                      transition: '0.15s ease-in-out',
                                      overflow: 'hidden',
                                      boxSizing: 'border-box',
                                      zIndex: 2,
                                      '&:hover': {
                                        transform: 'scale(1.02)',
                                        boxShadow: '0 4px 12px rgba(2, 132, 199, 0.3)',
                                        zIndex: 10,
                                      },
                                    }}
                                  >
                                    <Typography
                                      variant="caption"
                                      sx={{
                                        fontWeight: 800,
                                        fontSize: slot.durationMinutes <= 15 ? '0.66rem' : '0.75rem',
                                        lineHeight: 1.1,
                                        whiteSpace: 'nowrap',
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                      }}
                                    >
                                      Turno de {slot.startText} a {slot.endText}
                                      {slot.especialidadNombre ? ` — ${slot.especialidadNombre}` : ''}
                                    </Typography>
                                  </Box>
                                );
                              })}
                            </Box>
                          </TableCell>
                        );
                      }

                      return (
                        <TableCell
                          key={dateStr}
                          align="center"
                          sx={{
                            p: 0,
                            height: 48,
                            bgcolor: isPastCell ? '#f1f5f9' : '#f8fafc',
                            boxSizing: 'border-box',
                          }}
                        />
                      );
                    })}
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* PARTE INFERIOR: BOTONES GRANDES (IZQUIERDA) | FRANJAS CONFIGURADAS (DERECHA) */}
      <Grid container spacing={3} alignItems="flex-start">
        {/* COLUMNA IZQUIERDA: BOTONES DE ACCIÓN */}
        <Grid item xs={12} md={5}>
          <Paper sx={{ p: 3, borderRadius: 3, boxShadow: '0 2px 12px rgba(0,0,0,0.06)' }}>
            <Box display="flex" flexDirection="column" gap={2}>
              {/* Botón Grande 1: Agregar Franja Horaria Manual */}
              <Button
                variant="contained"
                color="primary"
                size="large"
                fullWidth
                startIcon={<AddIcon sx={{ fontSize: 28 }} />}
                onClick={handleOpenManualWizard}
                sx={{
                  py: 1.8,
                  px: 2.5,
                  borderRadius: 2.5,
                  fontWeight: 700,
                  fontSize: '0.95rem',
                  justifyContent: 'flex-start',
                  textAlign: 'left',
                  boxShadow: '0 2px 6px rgba(0,0,0,0.08)',
                  '&:hover': {
                    boxShadow: '0 4px 12px rgba(0,0,0,0.12)',
                  },
                }}
              >
                <Box textTransform="none">
                  <Typography variant="subtitle1" fontWeight={700} lineHeight={1.2}>
                    Agregar Franja De Turnos
                  </Typography>
                  <Typography variant="caption" sx={{ opacity: 0.95 }}>
                    Configurá un día, rango horario y especialidad manualmente.
                  </Typography>
                </Box>
              </Button>

              {/* Botón Grande 2: Aplicar Plantilla */}
              {plantillas.length > 0 && (
                <Button
                  variant="contained"
                  color="secondary"
                  size="large"
                  fullWidth
                  startIcon={<TemplateIcon sx={{ fontSize: 28 }} />}
                  onClick={handleOpenTemplateWizard}
                  sx={{
                    py: 1.8,
                    px: 2.5,
                    borderRadius: 2.5,
                    fontWeight: 700,
                    fontSize: '0.95rem',
                    justifyContent: 'flex-start',
                    textAlign: 'left',
                    boxShadow: '0 2px 6px rgba(0,0,0,0.08)',
                    '&:hover': {
                      boxShadow: '0 4px 12px rgba(0,0,0,0.12)',
                    },
                  }}
                >
                  <Box textTransform="none">
                    <Typography variant="subtitle1" fontWeight={700} lineHeight={1.2}>
                      Aplicar una Plantilla
                    </Typography>
                    <Typography variant="caption" sx={{ opacity: 0.95 }}>
                      Reutilizá un modelo de horario guardado previamente
                    </Typography>
                  </Box>
                </Button>
              )}
            </Box>
          </Paper>
        </Grid>

        {/* COLUMNA DERECHA: FRANJAS HORARIAS VIGENTES ESTA SEMANA */}
        <Grid item xs={12} md={7}>
          <Paper sx={{ p: 3, borderRadius: 3, boxShadow: '0 4px 20px rgba(0,0,0,0.06)', height: '100%' }}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={2.5}>
              <Box display="flex" alignItems="center" gap={1}>
                <MedicalIcon color="primary" />
                <Typography variant="h6" fontWeight={700}>
                  Franjas Vigentes esta Semana ({horariosVisiblesEnSemana.length})
                </Typography>
              </Box>
              <Typography variant="caption" color="text.secondary" fontWeight={500}>
                {currentWeekStart.format('DD/MM')} al {currentWeekStart.add(6, 'day').format('DD/MM')}
              </Typography>
            </Box>

            {loading ? (
              <Box display="flex" justifyContent="center" py={4}>
                <CircularProgress />
              </Box>
            ) : horariosVisiblesEnSemana.length === 0 ? (
              <Alert severity="info" sx={{ borderRadius: 2 }}>
                No tienes franjas horarias configuradas para la semana actual. Haz clic en el botón de la izquierda <strong>"Agregar Franja De Turnos"</strong> o <strong>"Aplicar una Plantilla"</strong> para habilitar tus turnos.
              </Alert>
            ) : (
              <Grid container spacing={2}>
                {horariosVisiblesEnSemana.map((h) => (
                  <Grid item xs={12} key={h.id}>
                    <Card variant="outlined" sx={{ borderRadius: 2.5, transition: '0.2s', '&:hover': { borderColor: 'primary.main', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' } }}>
                      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                        <Box display="flex" justifyContent="space-between" alignItems="center">
                          <Box display="flex" alignItems="center" gap={1.5}>
                            <MedicalIcon color="primary" />
                            <Box>
                              <Typography fontWeight={700}>
                                {h.especialidad ? h.especialidad.nombre : 'General'} — {h.diaSemana ? `Todos los ${h.diaSemana}` : `Fecha Puntual: ${h.fecha}`}
                              </Typography>
                              <Typography variant="caption" color="text.secondary" display="block">
                                {h.horaInicio} hs a {h.horaFin} hs ({h.duracionTurnoMinutos} min/turno)
                              </Typography>
                              {(h.diaSemana || h.fechaDesde || h.fechaHasta) && (
                                <Typography variant="caption" color="primary.main" fontWeight={600} display="block">
                                  📅 Vigencia: {calcularFechaInicioVigencia(h.fechaDesde, h.diaSemana)} ➔ {calcularFechaHastaVigencia(h.fechaDesde, h.fechaHasta, h.diaSemana)}
                                </Typography>
                              )}
                            </Box>
                          </Box>

                          <Box display="flex" gap={0.5}>
                            <IconButton
                              size="small"
                              color="primary"
                              onClick={() => handleOpenEditModal(h)}
                              title="Editar Franja ⚙️"
                            >
                              <SettingsIcon fontSize="small" />
                            </IconButton>
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => setSelectedHorarioDelete(h)}
                              title="Eliminar Franja"
                            >
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Box>
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
    </>
  );
};

export default HorariosCalendarGrid;
