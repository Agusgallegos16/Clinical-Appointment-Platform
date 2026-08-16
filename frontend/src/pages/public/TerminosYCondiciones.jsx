import React from 'react';
import { Container, Paper, Typography, Box, Divider, Button, Card, CardContent } from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Gavel as GavelIcon,
  WarningAmber as WarningIcon,
  Person as PersonIcon,
  EventAvailable as EventIcon,
  Shield as ShieldIcon,
  Cloud as CloudIcon,
  VerifiedUser as VerifiedUserIcon,
  Build as BuildIcon,
  Extension as ExtensionIcon,
  Copyright as CopyrightIcon,
  Balance as BalanceIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import Footer from '../../components/Footer';
import { CLINIC_CONFIG } from '../../config/clinicConfig';

const TerminosYCondiciones = () => {
  const navigate = useNavigate();

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', bgcolor: 'background.default' }}>
      <Container maxWidth="md" sx={{ py: 5, flex: 1 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate(-1)}
          sx={{ mb: 3, fontWeight: 700 }}
          variant="outlined"
        >
          Volver
        </Button>

        <Paper elevation={0} sx={{ p: { xs: 3, md: 5 }, borderRadius: 4, border: '1px solid', borderColor: 'divider', boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}>
          <Box display="flex" alignItems="center" gap={2} mb={2}>
            <GavelIcon color="primary" sx={{ fontSize: 44 }} />
            <Box>
              <Typography variant="h4" fontWeight={800} color="primary">
                Términos y Condiciones de Uso y Política de Privacidad
              </Typography>
              <Typography variant="caption" color="text.secondary" fontWeight={600}>
                Última actualización: {CLINIC_CONFIG.lastUpdatedDate}
              </Typography>
            </Box>
          </Box>

          <Divider sx={{ my: 3 }} />

          <Typography variant="body1" paragraph color="text.primary" sx={{ lineHeight: 1.7 }}>
            Bienvenido/a a la plataforma web de gestión de turnos y atención de <strong>{CLINIC_CONFIG.name}</strong> (en adelante, la "Plataforma"). Al acceder, registrarse o utilizar nuestros servicios web, el usuario (en adelante, el "Usuario") declara haber leído, entendido y aceptado de manera plena y sin reservas los presentes Términos y Condiciones y la Política de Privacidad.
          </Typography>
          <Typography variant="body1" paragraph color="text.secondary" sx={{ fontStyle: 'italic', mb: 4 }}>
            Si el Usuario no estuviera de acuerdo con estas condiciones, deberá abstenerse de utilizar la Plataforma.
          </Typography>

          {/* 1. Naturaleza del Servicio y Cláusula de NO EMERGENCIA */}
          <Card variant="outlined" sx={{ my: 3, borderRadius: 3, bgcolor: 'background.paper', borderColor: 'error.light' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1.5} mb={1.5}>
                <WarningIcon color="error" />
                <Typography variant="h6" fontWeight={700} color="error.dark">
                  1. Naturaleza del Servicio y Cláusula de NO EMERGENCIA
                </Typography>
              </Box>
              <Typography variant="body2" paragraph color="text.secondary">
                La Plataforma tiene como único propósito facilitar la reserva, consulta, reprogramación y cancelación de turnos médicos programados con los profesionales de {CLINIC_CONFIG.name}.
              </Typography>
              <Typography variant="subtitle2" fontWeight={800} color="error.dark" gutterBottom>
                INFORMACIÓN IMPORTANTE SOBRE EMERGENCIAS MÉDICAS:
              </Typography>
              <Typography variant="body2" fontWeight={600} color="text.primary">
                La Plataforma NO es un servicio de atención médica de urgencia ni de emergencias. En caso de presentar una urgencia, emergencia médica o riesgo de vida, el Usuario debe comunicarse inmediatamente al 107 (SAME), al 911 o dirigirse sin demora a la guardia médica más cercana.
              </Typography>
            </CardContent>
          </Card>

          {/* 2. Registro, Cuentas y Capacidad Legal */}
          <Card variant="outlined" sx={{ my: 3, borderRadius: 3, bgcolor: 'background.paper' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1.5} mb={1.5}>
                <PersonIcon color="primary" />
                <Typography variant="h6" fontWeight={700}>
                  2. Registro, Cuentas y Capacidad Legal
                </Typography>
              </Box>
              <Typography variant="body2" paragraph color="text.secondary">
                • <strong>Capacidad:</strong> El Usuario afirma contar con la capacidad legal suficiente para contratar y celebrar acuerdos vinculantes conforme a las leyes de la República Argentina.
              </Typography>
              <Typography variant="body2" paragraph color="text.secondary">
                • <strong>Veracidad de la información:</strong> El Usuario se compromete a proporcionar información verdadera, exacta, actual y completa al momento de registrarse o solicitar un turno (Nombre, Apellido, DNI, Teléfono, Correo Electrónico y Obra Social/Prepaga si correspondiera).
              </Typography>
              <Typography variant="body2" color="text.secondary">
                • <strong>Seguridad de la cuenta:</strong> El Usuario es responsable de mantener la confidencialidad de sus datos de acceso (email y contraseña) y de todas las actividades que ocurran bajo su cuenta. La Plataforma se reserva el derecho de cancelar turnos o suspender cuentas que contengan datos falsos o erróneos.
              </Typography>
            </CardContent>
          </Card>

          {/* 3. Gestión de Turnos, Cancelaciones y Tolerancia */}
          <Card variant="outlined" sx={{ my: 3, borderRadius: 3, bgcolor: 'background.paper' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1.5} mb={1.5}>
                <EventIcon color="primary" />
                <Typography variant="h6" fontWeight={700}>
                  3. Gestión de Turnos, Cancelaciones y Tolerancia
                </Typography>
              </Box>
              <Typography variant="body2" paragraph color="text.secondary">
                • <strong>Cancelaciones y Reprogramaciones:</strong> El paciente podrá cancelar o reprogramar su turno a través de la Plataforma con una anticipación mínima de <strong>{CLINIC_CONFIG.cancellationNoticeHours}</strong>.
              </Typography>
              <Typography variant="body2" color="text.secondary">
                • <strong>Tolerancia:</strong> Se otorgará un margen de tolerancia de <strong>{CLINIC_CONFIG.toleranceMinutes}</strong> sobre el horario fijado. Transcurrido dicho lapso, el profesional podrá dar por cancelado el turno o reprogramarlo según la disponibilidad de la agenda, para evitar demoras con los demás pacientes.
              </Typography>
            </CardContent>
          </Card>

          {/* 4. Política de Privacidad y Protección de Datos Personales (Ley N° 25.326) */}
          <Card variant="outlined" sx={{ my: 3, borderRadius: 3, bgcolor: 'background.paper' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1.5} mb={1.5}>
                <ShieldIcon color="success" />
                <Typography variant="h6" fontWeight={700}>
                  4. Política de Privacidad y Protección de Datos Personales (Ley N° 25.326)
                </Typography>
              </Box>
              <Typography variant="body2" paragraph color="text.secondary">
                • <strong>Compromiso de Confidencialidad:</strong> La Plataforma recolecta datos de contacto e identificación con el único fin de administrar la agenda médica y el contacto directo entre {CLINIC_CONFIG.name} y el paciente.
              </Typography>
              <Typography variant="body2" paragraph color="text.secondary">
                • <strong>NO comercialización:</strong> A diferencia de otras plataformas, {CLINIC_CONFIG.name} NO vende, cede, ni comparte datos personales o estadísticos con empresas farmacéuticas, anunciantes, patrocinadores ni terceros ajenos a la prestación directa del servicio médico.
              </Typography>
              <Typography variant="body2" color="text.secondary">
                • <strong>Normativa:</strong> La Plataforma trata todos los datos personales en estricto cumplimiento de la Ley de Protección de Datos Personales N° 25.326, su Decreto Reglamentario N° 1558/2001 y sus normas complementarias.
              </Typography>
            </CardContent>
          </Card>

          {/* 5. Almacenamiento y Transferencia Internacional de Datos */}
          <Card variant="outlined" sx={{ my: 3, borderRadius: 3, bgcolor: 'background.paper' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1.5} mb={1.5}>
                <CloudIcon color="info" />
                <Typography variant="h6" fontWeight={700}>
                  5. Almacenamiento y Transferencia Internacional de Datos
                </Typography>
              </Box>
              <Typography variant="body2" paragraph color="text.secondary">
                El Usuario comprende y acepta que la Plataforma utiliza infraestructura de tecnología en la nube de proveedores internacionales líderes (tales como Supabase, Vercel o Render) para el alojamiento de sus bases de datos y servicios web.
              </Typography>
              <Typography variant="body2" paragraph color="text.secondary">
                • <strong>Consentimiento de Transferencia:</strong> En cumplimiento del Art. 12 del Decreto 1558/2001, el Usuario consiente expresamente que sus datos personales sean almacenados en servidores alojados en el exterior (incluyendo, entre otros, Estados Unidos o Brasil).
              </Typography>
              <Typography variant="body2" color="text.secondary">
                • <strong>Medidas de Seguridad:</strong> La Plataforma adopta las medidas técnicas, organizativas y de ciberseguridad exigidas por la normativa vigente para garantizar un nivel adecuado de reserva y confidencialidad de la información.
              </Typography>
            </CardContent>
          </Card>

          {/* 6. Leyenda Obligatoria de la AAIP (Resolución 14/2018) */}
          <Card variant="outlined" sx={{ my: 3, borderRadius: 3, bgcolor: 'background.paper' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1.5} mb={1.5}>
                <VerifiedUserIcon color="primary" />
                <Typography variant="h6" fontWeight={700}>
                  6. Leyenda Obligatoria de la AAIP (Resolución 14/2018)
                </Typography>
              </Box>
              <Typography variant="body2" paragraph color="text.secondary" sx={{ fontStyle: 'italic', mb: 2 }}>
                En cumplimiento de la normativa dictada por la Agencia de Acceso a la Información Pública (AAIP) de la República Argentina, se informa expresamente:
              </Typography>
              <Paper variant="outlined" sx={{ p: 2, bgcolor: 'action.hover', borderRadius: 2, mb: 2 }}>
                <Typography variant="body2" color="text.primary" sx={{ lineHeight: 1.6 }}>
                  "El titular de los datos personales tiene la facultad de ejercer el derecho de acceso a los mismos en forma gratuita a intervalos no inferiores a seis meses, salvo que se acredite un interés legítimo al efecto conforme lo establecido en el artículo 14, inciso 3 de la Ley Nº 25.326.
                </Typography>
                <Typography variant="body2" color="text.primary" sx={{ lineHeight: 1.6, mt: 1 }}>
                  Asimismo, el titular de los datos tiene el derecho de rectificar sus datos, si los mismos fueran erróneos o inexactos, y de solicitar la supresión de los mismos, salvo que una norma legal, una obligación contractual u otra razón obligara o permitiera al responsable de la base de datos conservarlos.
                </Typography>
                <Typography variant="body2" color="text.primary" sx={{ lineHeight: 1.6, mt: 1, fontWeight: 600 }}>
                  LA AGENCIA DE ACCESO A LA INFORMACIÓN PÚBLICA, en su carácter de Órgano de Control de la Ley N° 25.326, tiene la atribución de atender las denuncias y reclamos que interpongan quienes resulten afectados en sus derechos por incumplimiento de las normas vigentes en materia de protección de datos personales."
                </Typography>
              </Paper>
            </CardContent>
          </Card>

          {/* 7. Disponibilidad Técnica y Exención de Responsabilidad */}
          <Card variant="outlined" sx={{ my: 3, borderRadius: 3, bgcolor: 'background.paper' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1.5} mb={1.5}>
                <BuildIcon color="warning" />
                <Typography variant="h6" fontWeight={700}>
                  7. Disponibilidad Técnica y Exención de Responsabilidad
                </Typography>
              </Box>
              <Typography variant="body2" paragraph color="text.secondary">
                • <strong>Funcionamiento de la Red:</strong> La Plataforma no garantiza el funcionamiento ininterrumpido o totalmente libre de errores de sus servidores o conexiones.
              </Typography>
              <Typography variant="body2" paragraph color="text.secondary">
                • <strong>Interrupciones:</strong> El acceso a la Plataforma podrá verse suspendido momentáneamente por mantenimiento programado, actualizaciones de software o por fallas ajenas a nuestro control derivadas de proveedores de conectividad o infraestructura en la nube.
              </Typography>
              <Typography variant="body2" color="text.secondary">
                • <strong>Exención:</strong> {CLINIC_CONFIG.name} no será responsable por perjuicios directos o indirectos derivados de la falta temporal de disponibilidad del sitio o fallas en las conexiones de red del Usuario.
              </Typography>
            </CardContent>
          </Card>

          {/* 8. Integraciones con Servicios de Terceros */}
          <Card variant="outlined" sx={{ my: 3, borderRadius: 3, bgcolor: 'background.paper' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1.5} mb={1.5}>
                <ExtensionIcon color="primary" />
                <Typography variant="h6" fontWeight={700}>
                  8. Integraciones con Servicios de Terceros
                </Typography>
              </Box>
              <Typography variant="body2" color="text.secondary">
                La Plataforma puede integrarse con servicios externos (como Google Calendar o sistemas de notificación automatizada) para facilitar recordatorios y la sincronización de agendas. {CLINIC_CONFIG.name} no se responsabiliza por desincronizaciones o errores derivados de caídas externas en las plataformas de dichos terceros.
              </Typography>
            </CardContent>
          </Card>

          {/* 9. Propiedad Intelectual */}
          <Card variant="outlined" sx={{ my: 3, borderRadius: 3, bgcolor: 'background.paper' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1.5} mb={1.5}>
                <CopyrightIcon color="secondary" />
                <Typography variant="h6" fontWeight={700}>
                  9. Propiedad Intelectual
                </Typography>
              </Box>
              <Typography variant="body2" color="text.secondary">
                Todo el contenido de la Plataforma (código fuente, diseño de interfaz, marcas, logotipos, textos e iconografía) es de propiedad exclusiva de {CLINIC_CONFIG.name} o cuenta con las licencias correspondientes. Queda prohibida su reproducción, copia o distribución sin autorización previa y por escrito.
              </Typography>
            </CardContent>
          </Card>

          {/* 10. Modificaciones, Jurisdicción y Ley Aplicable */}
          <Card variant="outlined" sx={{ my: 3, borderRadius: 3, bgcolor: 'background.paper' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={1.5} mb={1.5}>
                <BalanceIcon color="primary" />
                <Typography variant="h6" fontWeight={700}>
                  10. Modificaciones, Jurisdicción y Ley Aplicable
                </Typography>
              </Box>
              <Typography variant="body2" paragraph color="text.secondary">
                • <strong>Modificaciones:</strong> {CLINIC_CONFIG.name} se reserva el derecho de actualizar los presentes Términos y Condiciones en cualquier momento. Los cambios serán vigentes desde su publicación en la Plataforma.
              </Typography>
              <Typography variant="body2" paragraph color="text.secondary">
                • <strong>Ley Aplicable:</strong> Los presentes Términos y Condiciones se rigen en todos sus aspectos por las leyes de la República Argentina.
              </Typography>
              <Typography variant="body2" color="text.secondary">
                • <strong>Jurisdicción:</strong> Cualquier controversia judicial será sometida a la competencia de los Tribunales Ordinarios con asiento en {CLINIC_CONFIG.jurisdictionCity}, renunciando a cualquier otro fuero o jurisdicción que pudiera corresponder.
              </Typography>
            </CardContent>
          </Card>
        </Paper>
      </Container>
      <Footer />
    </Box>
  );
};

export default TerminosYCondiciones;
