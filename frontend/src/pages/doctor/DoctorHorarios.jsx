import React from 'react';
import { Box } from '@mui/material';
import { useDoctorHorarios } from './horarios/hooks/useDoctorHorarios';
import HorariosHeaderToolbar from './horarios/components/HorariosHeaderToolbar';
import HorariosCalendarGrid from './horarios/components/HorariosCalendarGrid';
import ModalFranjaManual from './horarios/components/ModalFranjaManual';
import ModalAplicarPlantilla from './horarios/components/ModalAplicarPlantilla';
import ModalBloqueoSlot from './horarios/components/ModalBloqueoSlot';
import ModalEditarHorario from './horarios/components/ModalEditarHorario';
import {
  ModalError,
  ModalLimpiarSemana,
  ModalEliminarFranja,
} from './horarios/components/ModalConfirmacionAccion';

const DoctorHorarios = () => {
  const h = useDoctorHorarios();

  return (
    <Box>
      {/* CABECERA PRINCIPAL Y ALERTAS */}
      <HorariosHeaderToolbar
        totalTurnosSemana={h.totalTurnosSemana}
        success={h.success}
      />

      {/* PLANILLA MATRIZ CALENDARIO Y LISTADO DE FRANJAS */}
      <HorariosCalendarGrid
        currentWeekStart={h.currentWeekStart}
        handlePrevWeek={h.handlePrevWeek}
        handleToday={h.handleToday}
        handleNextWeek={h.handleNextWeek}
        setShowClearWeekModal={h.setShowClearWeekModal}
        weekDaysWithDates={h.weekDaysWithDates}
        timeSlots={h.timeSlots}
        weekSlotsByDate={h.weekSlotsByDate}
        coveredRowsByDate={h.coveredRowsByDate}
        currentMinutesToday={h.currentMinutesToday}
        setSelectedSlotBlock={h.setSelectedSlotBlock}
        handleOpenManualWizard={h.handleOpenManualWizard}
        plantillas={h.plantillas}
        handleOpenTemplateWizard={h.handleOpenTemplateWizard}
        horariosVisiblesEnSemana={h.horariosVisiblesEnSemana}
        loading={h.loading}
        handleOpenEditModal={h.handleOpenEditModal}
        setSelectedHorarioDelete={h.setSelectedHorarioDelete}
      />

      {/* WIZARD MODAL: AGREGAR FRANJA HORARIA MANUAL */}
      <ModalFranjaManual
        open={h.openManualWizard}
        onClose={() => h.setOpenManualWizard(false)}
        activeStep={h.activeStepManual}
        doctorInfo={h.doctorInfo}
        selectedEspecialidadId={h.selectedEspecialidadId}
        setSelectedEspecialidadId={h.setSelectedEspecialidadId}
        tipoHorario={h.tipoHorario}
        setTipoHorario={h.setTipoHorario}
        diaSemana={h.diaSemana}
        setDiaSemana={h.setDiaSemana}
        todayStr={h.todayStr}
        fechaDesde={h.fechaDesde}
        setFechaDesde={h.setFechaDesde}
        fechaHasta={h.fechaHasta}
        setFechaHasta={h.setFechaHasta}
        fechaPuntual={h.fechaPuntual}
        setFechaPuntual={h.setFechaPuntual}
        horaInicio={h.horaInicio}
        setHoraInicio={h.setHoraInicio}
        horaFin={h.horaFin}
        setHoraFin={h.setHoraFin}
        duracionTurno={h.duracionTurno}
        setDuracionTurno={h.setDuracionTurno}
        especialidadSeleccionadaNombre={h.especialidadSeleccionadaNombre}
        saving={h.saving}
        handleBack={h.handleBackManual}
        handleNext={h.handleNextManual}
        handleGuardar={h.handleGuardarHorarioWizard}
      />

      {/* WIZARD MODAL: APLICAR PLANTILLA */}
      <ModalAplicarPlantilla
        open={h.openTemplateWizard}
        onClose={() => h.setOpenTemplateWizard(false)}
        activeStep={h.activeStepTemplate}
        plantillas={h.plantillas}
        selectedPlantillaId={h.selectedPlantillaId}
        setSelectedPlantillaId={h.setSelectedPlantillaId}
        tipoAplicacion={h.tipoAplicacion}
        setTipoAplicacion={h.setTipoAplicacion}
        diaAplicar={h.diaAplicar}
        setDiaAplicar={h.setDiaAplicar}
        todayStr={h.todayStr}
        fechaDesdeAplicar={h.fechaDesdeAplicar}
        setFechaDesdeAplicar={h.setFechaDesdeAplicar}
        fechaHastaAplicar={h.fechaHastaAplicar}
        setFechaHastaAplicar={h.setFechaHastaAplicar}
        fechaAplicar={h.fechaAplicar}
        setFechaAplicar={h.setFechaAplicar}
        plantillaSeleccionadaNombre={h.plantillaSeleccionadaNombre}
        applying={h.applying}
        handleBack={h.handleBackTemplate}
        handleNext={h.handleNextTemplate}
        handleAplicar={h.handleAplicarPlantillaWizard}
      />

      {/* MODAL BLOQUEAR / DESHABILITAR SLOT INDIVIDUAL */}
      <ModalBloqueoSlot
        selectedSlotBlock={h.selectedSlotBlock}
        onClose={() => h.setSelectedSlotBlock(null)}
        blocking={h.blocking}
        handleBloquearSlotIndividual={h.handleBloquearSlotIndividual}
      />

      {/* MODAL CONFIGURAR / EDITAR FRANJA HORARIA */}
      <ModalEditarHorario
        editingHorario={h.editingHorario}
        onClose={() => h.setEditingHorario(null)}
        doctorInfo={h.doctorInfo}
        editFormData={h.editFormData}
        setEditFormData={h.setEditFormData}
        todayStr={h.todayStr}
        saving={h.saving}
        handleSaveEditHorario={h.handleSaveEditHorario}
      />

      {/* MODALES DE CONFIRMACIÓN Y ALERTA */}
      <ModalError
        errorModal={h.errorModal}
        errorModalTitle={h.errorModalTitle}
        onClose={() => h.setErrorModal('')}
      />

      <ModalLimpiarSemana
        open={h.showClearWeekModal}
        onClose={() => h.setShowClearWeekModal(false)}
        currentWeekStart={h.currentWeekStart}
        clearingWeek={h.clearingWeek}
        handleLimpiarSemanaActual={h.handleLimpiarSemanaActual}
      />

      <ModalEliminarFranja
        selectedHorarioDelete={h.selectedHorarioDelete}
        onClose={() => h.setSelectedHorarioDelete(null)}
        deleting={h.deleting}
        handleConfirmEliminarHorario={h.handleConfirmEliminarHorario}
      />
    </Box>
  );
};

export default DoctorHorarios;
