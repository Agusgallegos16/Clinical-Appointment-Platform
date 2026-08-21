/**
 * Configuración Parametrizada del Instituto / Consultorio Médico
 * Permite personalizar los datos institucionales, legales y de contacto mediante variables de entorno en el Frontend (Vite)
 */
export const CLINIC_CONFIG = {
  name: import.meta.env.VITE_CLINIC_NAME || "Instituto Médico Consultorios",
  email: import.meta.env.VITE_CLINIC_EMAIL || "contacto@consultorio.com",
  legalEmail: import.meta.env.VITE_CLINIC_LEGAL_EMAIL || "legales@consultorio.com",
  phone: import.meta.env.VITE_CLINIC_PHONE || "+54 11 1234-5678",
  whatsapp: import.meta.env.VITE_CLINIC_WHATSAPP || "+54 9 11 1234-5678",
  address: import.meta.env.VITE_CLINIC_ADDRESS || "Av. Principal 1234, CABA",
  cancellationNoticeHours: import.meta.env.VITE_CANCELLATION_HOURS || "24 horas",
  toleranceMinutes: import.meta.env.VITE_TOLERANCE_MINUTES || "15 minutos",
  jurisdictionCity: import.meta.env.VITE_JURISDICTION_CITY || "Ciudad Autónoma de Buenos Aires",
  lastUpdatedDate: import.meta.env.VITE_LAST_UPDATED_DATE || "Agosto 2026",
  description : import.meta.env.VITE_ACTIVITY_DESCRIPTION || "Excelencia y Cuidado Médico"
};
