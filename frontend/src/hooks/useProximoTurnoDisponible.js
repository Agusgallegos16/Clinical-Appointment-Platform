import { useCallback, useEffect, useRef, useState } from 'react';
import { isCancel } from '../api/axiosClient';
import { doctorService } from '../api/doctorService';

export function useProximoTurnoDisponible({
  doctorId,
  especialidadId = null,
  desdeFecha = null,
  onFound,
} = {}) {
  const [loading, setLoading] = useState(false);
  const [sinDisponibilidad, setSinDisponibilidad] = useState(false);
  const [error, setError] = useState(null);
  const [ultimoTurnoEncontrado, setUltimoTurnoEncontrado] = useState(null);

  const abortControllerRef = useRef(null);
  const onFoundRef = useRef(onFound);
  onFoundRef.current = onFound;

  const cancelarSolicitudEnCurso = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
  }, []);

  const limpiarEstado = useCallback(() => {
    cancelarSolicitudEnCurso();
    setLoading(false);
    setSinDisponibilidad(false);
    setError(null);
    setUltimoTurnoEncontrado(null);
  }, [cancelarSolicitudEnCurso]);

  useEffect(() => {
    limpiarEstado();
    return () => {
      cancelarSolicitudEnCurso();
    };
  }, [doctorId, especialidadId, limpiarEstado, cancelarSolicitudEnCurso]);

  const buscarProximoTurno = useCallback(async (overrides = {}) => {
    const targetDoctorId = overrides.doctorId ?? doctorId;
    const targetEspecialidadId =
      overrides.especialidadId !== undefined ? overrides.especialidadId : especialidadId;
    const targetDesdeFecha =
      overrides.desdeFecha !== undefined ? overrides.desdeFecha : desdeFecha;

    if (!targetDoctorId) {
      return null;
    }

    cancelarSolicitudEnCurso();
    const controller = new AbortController();
    abortControllerRef.current = controller;

    setLoading(true);
    setSinDisponibilidad(false);
    setError(null);

    try {
      const proximo = await doctorService.obtenerProximoTurnoDisponible(targetDoctorId, {
        especialidadId: targetEspecialidadId,
        desdeFecha: targetDesdeFecha,
        signal: controller.signal,
      });

      if (controller.signal.aborted) {
        return null;
      }

      if (!proximo) {
        setUltimoTurnoEncontrado(null);
        setSinDisponibilidad(true);
        return null;
      }

      setUltimoTurnoEncontrado(proximo);
      if (typeof onFoundRef.current === 'function') {
        await onFoundRef.current(proximo);
      }
      return proximo;
    } catch (err) {
      if (isCancel(err) || controller.signal.aborted) {
        return null;
      }
      const message =
        err.response?.data?.message ||
        'No se pudo consultar el próximo turno disponible. Intente nuevamente.';
      setError(message);
      return null;
    } finally {
      if (abortControllerRef.current === controller) {
        abortControllerRef.current = null;
        setLoading(false);
      }
    }
  }, [doctorId, especialidadId, desdeFecha, cancelarSolicitudEnCurso]);

  return {
    buscarProximoTurno,
    loading,
    sinDisponibilidad,
    error,
    ultimoTurnoEncontrado,
    limpiarEstado,
  };
}
