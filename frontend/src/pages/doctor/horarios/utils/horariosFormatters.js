import dayjs from 'dayjs';

export const diasSemanaEnum = ['DOMINGO', 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO'];
export const diasNombreEs = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

// Funciones de normalización defensiva para datos de bloqueos
export const normalizeFecha = (f) => {
  if (f == null) return null;
  if (Array.isArray(f)) {
    const [y, m, d] = f;
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
  }
  return String(f);
};

export const normalizeHora = (h) => {
  if (h == null) return '00:00';
  if (Array.isArray(h)) {
    const hh = String(h[0] || 0).padStart(2, '0');
    const mm = String(h[1] || 0).padStart(2, '0');
    return `${hh}:${mm}`;
  }
  return String(h).substring(0, 5);
};

// Paletas de color por especialidad
export const getSpecialtyColorPalette = (espId) => {
  const palettes = [
    { bg: 'linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%)', text: '#0369a1', border: '#0284c7' }, // Azul Médico
    { bg: 'linear-gradient(135deg, #ccfbf1 0%, #99f6e4 100%)', text: '#0f766e', border: '#0d9488' }, // Teal Salud
    { bg: 'linear-gradient(135deg, #f3e8ff 0%, #e9d5ff 100%)', text: '#6b21a8', border: '#9333ea' }, // Violeta Dermatología
    { bg: 'linear-gradient(135deg, #fef3c7 0%, #fde68a 100%)', text: '#92400e', border: '#d97706' }, // Ámbar Traumatología
    { bg: 'linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%)', text: '#166534', border: '#16a34a' }, // Verde Pediatría
    { bg: 'linear-gradient(135deg, #ffe4e6 0%, #fecdd3 100%)', text: '#9f1239', border: '#e11d48' }, // Rosa
  ];
  if (!espId) return palettes[0];
  const idx = Number(espId) % palettes.length;
  return palettes[idx];
};

export const generateTimeSlots = () => {
  const times = [];
  let current = dayjs().hour(7).minute(0).second(0);
  const end = dayjs().hour(22).minute(0).second(0);

  while (current.isBefore(end) || current.isSame(end)) {
    times.push(current.format('HH:mm'));
    current = current.add(30, 'minute');
  }
  return times;
};

export const stepsManualWizard = [
  'Especialidad Médica',
  'Configuración & Día',
  'Horarios & Duración',
  'Resumen & Confirmación',
];

export const stepsTemplateWizard = [
  'Seleccionar Plantilla',
  'Día & Vigencia',
  'Resumen & Confirmación',
];
