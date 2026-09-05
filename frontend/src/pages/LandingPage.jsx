import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  LocalHospital,
  EventAvailable,
  AccessTime,
  Phone,
  Email,
  LocationOn,
  Search,
  ArrowForward,
  Star,
  Menu as MenuIcon,
  Close as CloseIcon,
  ChevronRight,
  ChevronLeft,
  Collections,
  Person,
  Groups,
  Security,
  Biotech,
  CalendarMonth,
  CheckCircle,
  WhatsApp,
} from '@mui/icons-material';

import { CLINIC_CONFIG } from '../config/clinicConfig';
import { doctorService } from '../api/doctorService';
import { getInstitucionGallery } from '../api/storageService';
import { useAuth } from '../context/AuthContext';

import './LandingPage.css';

const LandingPage = () => {
  const navigate = useNavigate();
  const { isAuthenticated, role } = useAuth();

  // Scroll Progress and Sticky Navbar State
  const [scrollProgress, setScrollProgress] = useState(0);
  const [isScrolled, setIsScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  // Dynamic Staff State
  const [doctores, setDoctores] = useState([]);
  const [loadingDoctors, setLoadingDoctors] = useState(true);

  // Search Query Filter
  const [searchQuery, setSearchQuery] = useState('');

  // Active FAQ Accordion Index
  const [activeFaq, setActiveFaq] = useState(0);

  // Gallery State for "Nosotros" Section (Supabase Storage)
  const [gallery, setGallery] = useState([]);
  const [currentSlideIndex, setCurrentSlideIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);

  // Fetch Supabase Institution Gallery
  useEffect(() => {
    const fetchGallery = async () => {
      try {
        const supabasePhotos = await getInstitucionGallery();
        if (supabasePhotos && supabasePhotos.length > 0) {
          setGallery(
            supabasePhotos.map((item, idx) => ({
              url: item.url,
              title: `Instalaciones & Servicios — Imagen ${idx + 1}`,
              subtitle: 'Equipamiento e infraestructura de alta complejidad.',
            }))
          );
        } else {
          // Default initial photos if bucket is not populated yet
          setGallery([
            {
              url: 'https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&w=1200&q=80',
              title: 'Instalaciones de Vanguardia',
              subtitle: 'Consultorios modernos diseñados para la comodidad y seguridad de nuestros pacientes.',
            },
            {
              url: 'https://images.unsplash.com/photo-1586773860418-d37222d8fce3?auto=format&fit=crop&w=1200&q=80',
              title: 'Centro Diagnóstico Integral',
              subtitle: 'Equipamiento de alta tecnología para estudios y diagnóstico de precisión.',
            },
            {
              url: 'https://images.unsplash.com/photo-1516549655169-df83a0774514?auto=format&fit=crop&w=1200&q=80',
              title: 'Atención Personalizada y Humana',
              subtitle: 'Un equipo comprometido con el cuidado y acompañamiento de tu salud.',
            },
          ]);
        }
      } catch (err) {
        console.error('Error al obtener fotos de Supabase Storage:', err);
      }
    };

    fetchGallery();
  }, []);

  // Circular Auto-Slide Effect (10s interval)
  useEffect(() => {
    if (gallery.length <= 1 || isPaused) return;

    const interval = setInterval(() => {
      setCurrentSlideIndex((prev) => (prev + 1) % gallery.length);
    }, 10000);

    return () => clearInterval(interval);
  }, [gallery.length, isPaused]);

  const handlePrevSlide = () => {
    setCurrentSlideIndex((prev) => (prev - 1 + gallery.length) % gallery.length);
  };

  const handleNextSlide = () => {
    setCurrentSlideIndex((prev) => (prev + 1) % gallery.length);
  };

  // Scroll Progress Listener & Navbar Scroll Effect
  useEffect(() => {
    const handleScroll = () => {
      const totalHeight = document.documentElement.scrollHeight - window.innerHeight;
      const progress = totalHeight > 0 ? window.scrollY / totalHeight : 0;
      setScrollProgress(progress);

      if (window.scrollY > 40) {
        setIsScrolled(true);
      } else {
        setIsScrolled(false);
      }
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Fetch Public Doctors (Only Visible / Publicly Available ones from Backend DB)
  useEffect(() => {
    const fetchData = async () => {
      setLoadingDoctors(true);
      try {
        // Pass true for soloVisibles parameter to filter out invisible doctor profiles at backend
        const docsData = await doctorService.listarDoctores(null, true).catch(() => []);

        if (Array.isArray(docsData)) {
          // Store only real database doctors that are visible
          setDoctores(docsData.filter((doc) => doc.disponibleParaTurnos !== false));
        } else {
          setDoctores([]);
        }
      } catch (err) {
        console.error('Error fetching public doctors data:', err);
        setDoctores([]);
      } finally {
        setLoadingDoctors(false);
      }
    };

    fetchData();
  }, []);

  // Handler for dashboard navigation if already logged in
  const handleAuthRedirect = () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    if (role === 'ADMIN') navigate('/admin');
    else if (role === 'DOCTOR') navigate('/doctor');
    else if (role === 'PACIENTE') navigate('/paciente');
    else navigate('/login');
  };

  // Filter doctors based on search query (name or specialty) AND visibility
  const filteredDoctors = doctores
    .filter((doc) => doc.disponibleParaTurnos !== false)
    .filter((doc) => {
      const query = searchQuery.toLowerCase();
      const fullName = `${doc.nombre || ''} ${doc.apellido || ''}`.toLowerCase();
      const specNames = doc.especialidades?.map((e) => e.nombre).join(' ').toLowerCase() || '';

      return fullName.includes(query) || specNames.includes(query);
    });

  const faqs = [
    {
      q: '¿Cómo solicito un turno médico online por primera vez?',
      a: 'Es muy sencillo. Haz clic en "Sacar Turno", crea tu cuenta de paciente con tu DNI y correo electrónico. Una vez registrado, podrás elegir el profesional médico de preferencia y la fecha/hora que mejor se adapte a tu agenda.',
    },
    {
      q: '¿Qué documentación debo presentar el día de mi consulta?',
      a: 'El día de tu cita debes concurrir 15 minutos antes con tu DNI y el carnet físico o digital de tu Obra Social o Prepaga habilitada.',
    },
    {
      q: '¿Puedo reprogramar o cancelar una cita agendada?',
      a: 'Sí, la plataforma permite gestionar tus citas de forma 100% digital. Ingresando a "Mis Turnos" en tu panel de paciente podrás reprogramar o cancelar con hasta ' + CLINIC_CONFIG.cancellationNoticeHours + ' de anticipación.',
    },
    {
      q: '¿Cómo funciona la gestión de turnos para menores de edad?',
      a: 'En el módulo de Paciente contamos con la opción de "Gestión de Menores y Familiares". Podrás vincular a tus hijos a cargo bajo tu misma cuenta y gestionar sus citas de manera centralizada.',
    },
  ];

  return (
    <div className="lp-root">
      {/* Scroll Progress Bar */}
      <div
        className="lp-scroll-progress"
        style={{ transform: `scaleX(${scrollProgress})` }}
      />

      {/* Navigation Header */}
      <header className={`lp-header ${isScrolled ? 'scrolled' : ''}`}>
        <div className="lp-header-container">
          <Link to="/" className="lp-logo">
            <div className="lp-logo-icon">
              <LocalHospital sx={{ fontSize: 26 }} />
            </div>
            <div className="lp-logo-text">
              <span className="lp-logo-title">{CLINIC_CONFIG.name}</span>
              <span className="lp-logo-subtitle">{CLINIC_CONFIG.description}</span>
            </div>
          </Link>

          {/* Desktop Nav Links */}
          <nav className="lp-nav">
            <a href="#inicio" className="lp-nav-link">Inicio</a>
            <a href="#nosotros" className="lp-nav-link">Nosotros</a>
            <a href="#staff" className="lp-nav-link">Nómina Médica</a>
            <a href="#contacto" className="lp-nav-link">Contacto</a>
          </nav>

          {/* Header Action Buttons */}
          <div className="lp-nav-actions">
            {isAuthenticated ? (
              <button onClick={handleAuthRedirect} className="lp-btn lp-btn-primary">
                <Person sx={{ fontSize: 20 }} />
                <span>Ir a Mi Panel</span>
              </button>
            ) : (
              <>
                <Link to="/login" className="lp-btn lp-btn-primary">
                  <EventAvailable sx={{ fontSize: 20 }} />
                  <span>Sacar Turno</span>
                </Link>
              </>
            )}

            {/* Mobile Toggle Button */}
            <button
              className="lp-mobile-toggle"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-label="Abrir menú"
            >
              {mobileMenuOpen ? <CloseIcon /> : <MenuIcon />}
            </button>
          </div>
        </div>

        {/* Mobile Dropdown Menu */}
        {mobileMenuOpen && (
          <div className="lp-mobile-menu">
            <a href="#inicio" className="lp-mobile-link" onClick={() => setMobileMenuOpen(false)}>
              <span>Inicio</span>
              <ChevronRight sx={{ fontSize: 18, color: 'var(--lp-blue-600)' }} />
            </a>
            <a href="#nosotros" className="lp-mobile-link" onClick={() => setMobileMenuOpen(false)}>
              <span>Nosotros</span>
              <ChevronRight sx={{ fontSize: 18, color: 'var(--lp-blue-600)' }} />
            </a>
            <a href="#staff" className="lp-mobile-link" onClick={() => setMobileMenuOpen(false)}>
              <span>Nómina Médica</span>
              <ChevronRight sx={{ fontSize: 18, color: 'var(--lp-blue-600)' }} />
            </a>
            <a href="#contacto" className="lp-mobile-link" onClick={() => setMobileMenuOpen(false)}>
              <span>Contacto</span>
              <ChevronRight sx={{ fontSize: 18, color: 'var(--lp-blue-600)' }} />
            </a>


          </div>
        )}
      </header>

      {/* Hero Section */}
      <section id="inicio" className="lp-hero">
        <div className="lp-mesh-bg" />
        <div className="lp-hero-container">
          <div className="lp-hero-content">
            <h1 className="lp-hero-title">
              Cuidamos tu salud con <em>conocimiento, compromiso</em> y calidez humana.
            </h1>

            <p className="lp-hero-sub">
              El <strong>{CLINIC_CONFIG.name}</strong> pone a tu disposición más de 14 profesionales de excelencia, diagnósticos de precisión y reserva de turnos online en segundos.
            </p>

            <div className="lp-hero-cta">
              <Link to="/login" className="lp-btn lp-btn-primary" style={{ padding: '14px 32px', fontSize: '1.05rem' }}>
                <span>Sacar Turno Online</span>
                <ArrowForward sx={{ fontSize: 20 }} />
              </Link>

              <a href="#staff" className="lp-btn lp-btn-secondary" style={{ padding: '14px 28px', fontSize: '1.05rem' }}>
                <span>Ver Nómina de Médicos</span>
              </a>
            </div>

            <div className="lp-hero-info-pills">
              <div className="lp-info-pill">
                <CheckCircle className="lp-info-pill-icon" sx={{ fontSize: 20 }} />
                <span>Atención Presencial</span>
              </div>
              <div className="lp-info-pill">
                <CheckCircle className="lp-info-pill-icon" sx={{ fontSize: 20 }} />
                <span>Diagnóstico Personalizado</span>
              </div>
            </div>
          </div>

          {/* Hero Visual Card (Glassmorphic Mockup) */}
          <div className="lp-hero-visual">
            <div className="lp-hero-main-card">
              <div className="lp-card-floating-badge">
                <Star sx={{ fontSize: 18, color: '#f59e0b' }} />
                <span>4.8 / 5.0 (Reseñas de Pacientes)</span>
              </div>

              <div className="lp-medical-card-header">
                <div className="lp-medical-avatar">
                  <LocalHospital sx={{ fontSize: 32 }} />
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.15rem', color: 'var(--lp-blue-900)', fontWeight: 700 }}>
                    {CLINIC_CONFIG.name}
                  </h3>
                  <span style={{ fontSize: '0.85rem', color: 'var(--lp-blue-600)', fontWeight: 600 }}>
                    Centro Médico
                  </span>
                </div>
              </div>

              <p style={{ fontSize: '0.92rem', color: 'var(--lp-text-secondary)', lineHeight: 1.5, marginBottom: '20px' }}>
                Reserva de turnos ágil, recepción digital, recordatorios automáticos de turnos e historial de visitas unificado.
              </p>

              <div style={{ background: 'var(--lp-blue-50)', padding: '16px', borderRadius: '12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <AccessTime sx={{ color: 'var(--lp-blue-600)' }} />
                  <div>
                    <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--lp-blue-900)' }}>Próximos Turnos Disponibles</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--lp-text-secondary)' }}>Hoy & Mañana en todas las áreas</div>
                  </div>
                </div>
                <Link to="/login" style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--lp-blue-700)', textDecoration: 'none' }}>
                  Reservar →
                </Link>
              </div>

              <div className="lp-card-floating-badge-2">
                <Security sx={{ color: 'var(--lp-emerald-600)' }} />
                <div>
                  <div style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--lp-blue-900)' }}>Atención Garantizada</div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--lp-text-muted)' }}>Seguridad y confidencialidad médica</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Stats Counter Section */}
      <section className="lp-stats-section">
        <div className="lp-stats-grid">
          <div className="lp-stat-item">
            <span className="lp-stat-number">+15</span>
            <span className="lp-stat-label">Años de Trayectoria</span>
          </div>
          <div className="lp-stat-item">
            <span className="lp-stat-number">+10k</span>
            <span className="lp-stat-label">Pacientes Atendidos con Éxito</span>
          </div>
          <div className="lp-stat-item">
            <span className="lp-stat-number">+14</span>
            <span className="lp-stat-label">Médicos & Especialistas de la Salud</span>
          </div>
        </div>
      </section>

      {/* Nosotros Section (Circular Interactive Photo Panel) */}
      <section id="nosotros" className="lp-section">
        <div className="lp-container">
          <div className="lp-section-header">
            <h2 className="lp-section-title">Nuestras Instalaciones & Centro Médico</h2>
            <p className="lp-section-desc">
              Recorré de manera interactiva la infraestructura, tecnología y espacios de atención del {CLINIC_CONFIG.name}.
            </p>
          </div>

          {/* Interactive Circular Carousel Panel */}
          {gallery.length > 0 && (
            <div
              className="lp-carousel-container"
              onMouseEnter={() => setIsPaused(true)}
              onMouseLeave={() => setIsPaused(false)}
            >
              <div className="lp-carousel-slide">
                <img
                  src={gallery[currentSlideIndex].url}
                  alt={gallery[currentSlideIndex].title || 'Foto Institucional'}
                  className="lp-carousel-img"
                />

                <div className="lp-carousel-overlay">
                  <span className="lp-carousel-tag">
                    <Collections sx={{ fontSize: 16 }} />
                    <span>Galería Institucional · {currentSlideIndex + 1} de {gallery.length}</span>
                  </span>
                </div>

                {/* Circular Navigation Arrow Buttons */}
                {gallery.length > 1 && (
                  <>
                    <button
                      className="lp-carousel-btn lp-carousel-btn-prev"
                      onClick={handlePrevSlide}
                      aria-label="Anterior"
                    >
                      <ChevronLeft sx={{ fontSize: 28 }} />
                    </button>

                    <button
                      className="lp-carousel-btn lp-carousel-btn-next"
                      onClick={handleNextSlide}
                      aria-label="Siguiente"
                    >
                      <ChevronRight sx={{ fontSize: 28 }} />
                    </button>
                  </>
                )}

                {/* Circular Dot Indicators */}
                {gallery.length > 1 && (
                  <div className="lp-carousel-dots">
                    {gallery.map((_, idx) => (
                      <button
                        key={idx}
                        className={`lp-carousel-dot ${idx === currentSlideIndex ? 'active' : ''}`}
                        onClick={() => setCurrentSlideIndex(idx)}
                        aria-label={`Ir a foto ${idx + 1}`}
                      />
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </section>

      {/* Dynamic Staff Médico Section (GET /api/doctores) */}
      <section id="staff" className="lp-section" style={{ background: '#f8fafc' }}>
        <div className="lp-container">
          <div className="lp-section-header">
            <span className="lp-section-kicker">Nuestros Profesionales</span>
            <h2 className="lp-section-title">Nómina Médica Institucional</h2>
            <p className="lp-section-desc">
              Consultá nuestro equipo de médicos especialistas y seleccioná tu profesional de confianza para agendar tu consulta.
            </p>
          </div>

          {/* Search Filter Bar */}
          <div className="lp-staff-filter-bar" style={{ justifyContent: 'center' }}>
            <div className="lp-search-box" style={{ maxWidth: '600px', width: '100%' }}>
              <Search sx={{ color: 'var(--lp-text-muted)', fontSize: 20 }} />
              <input
                type="text"
                placeholder="Buscar profesional o especialidad médica..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="lp-search-input"
              />
            </div>
          </div>

          {/* Doctors Grid */}
          {loadingDoctors ? (
            <div style={{ textAlign: 'center', padding: '40px', color: 'var(--lp-text-muted)' }}>
              Cargando nómina de médicos de la institución...
            </div>
          ) : filteredDoctors.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px', background: '#ffffff', borderRadius: '16px', border: '1px solid var(--lp-border)' }}>
              <p style={{ color: 'var(--lp-text-secondary)', fontSize: '1rem', margin: 0 }}>
                No se encontraron profesionales que coincidan con la búsqueda.
              </p>
            </div>
          ) : (
            <div className="lp-grid-3">
              {filteredDoctors.map((doc) => {
                const nombreCompleto = `Dr. ${doc.nombre} ${doc.apellido}`;
                return (
                  <div key={doc.id} className="lp-doctor-card">
                    {doc.fotoUrl ? (
                      <img src={doc.fotoUrl} alt={nombreCompleto} className="lp-doctor-avatar" />
                    ) : (
                      <div className="lp-doctor-avatar-placeholder">
                        {doc.nombre?.[0]}
                        {doc.apellido?.[0]}
                      </div>
                    )}

                    <h3 className="lp-doctor-name">{nombreCompleto}</h3>

                    <div className="lp-doctor-badges">
                      {doc.especialidades && doc.especialidades.length > 0 ? (
                        doc.especialidades.map((esp) => (
                          <span key={esp.id || esp.nombre} className="lp-badge-spec">
                            {esp.nombre}
                          </span>
                        ))
                      ) : (
                        <span className="lp-badge-spec">Medicina General</span>
                      )}
                    </div>

                    <div className="lp-doctor-schedule">
                      <AccessTime sx={{ fontSize: 16, color: 'var(--lp-blue-600)' }} />
                      <span>Atención en Consultorios</span>
                    </div>

                    <div className="lp-doctor-card-action">
                      <Link to="/login" className="lp-btn lp-btn-outline" style={{ width: '100%', fontSize: '0.88rem' }}>
                        <span>Reservar Cita</span>
                        <ChevronRight sx={{ fontSize: 18 }} />
                      </Link>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </section>

      {/* FAQ Section */}
      <section className="lp-section">
        <div className="lp-container">
          <div className="lp-section-header">
            <span className="lp-section-kicker">Resuelve tus Dudas</span>
            <h2 className="lp-section-title">Preguntas Frecuentes</h2>
            <p className="lp-section-desc">
              Información clara y transparente para la atención de nuestros pacientes.
            </p>
          </div>

          <div className="lp-faq-list">
            {faqs.map((faq, idx) => (
              <div
                key={idx}
                className={`lp-faq-item ${activeFaq === idx ? 'active' : ''}`}
              >
                <button
                  className="lp-faq-question"
                  onClick={() => setActiveFaq(activeFaq === idx ? -1 : idx)}
                >
                  <span>{faq.q}</span>
                  <ChevronRight
                    sx={{
                      transform: activeFaq === idx ? 'rotate(90deg)' : 'rotate(0deg)',
                      transition: 'transform 0.2s ease',
                      color: 'var(--lp-blue-600)',
                    }}
                  />
                </button>
                {activeFaq === idx && (
                  <div className="lp-faq-answer">
                    <p style={{ margin: 0 }}>{faq.a}</p>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Contacto & Ubicación Section */}
      <section id="contacto" className="lp-section" style={{ background: '#f8fafc' }}>
        <div className="lp-container">
          <div className="lp-grid-2">
            <div>
              <span className="lp-section-kicker">Contacto & Ubicación</span>
              <h2 className="lp-section-title" style={{ textAlign: 'left', marginBottom: '24px' }}>
                Estamos cerca tuyo para lo que necesites
              </h2>
              <p className="lp-section-desc" style={{ textAlign: 'left', marginBottom: '32px' }}>
                Comunicate a través de nuestro número de contacto o acercate a nuestra dirección.
              </p>

              <div className="lp-contact-card">
                <div className="lp-contact-item">
                  <div className="lp-contact-icon">
                    <LocationOn />
                  </div>
                  <div>
                    <div className="lp-contact-title">Dirección</div>
                    <div className="lp-contact-desc">{CLINIC_CONFIG.address}</div>
                  </div>
                </div>

                <div className="lp-contact-item">
                  <div className="lp-contact-icon">
                    <Phone />
                  </div>
                  <div>
                    <div className="lp-contact-title">Teléfono Fijo</div>
                    <div className="lp-contact-desc">{CLINIC_CONFIG.phone}</div>
                  </div>
                </div>

                <div className="lp-contact-item">
                  <div className="lp-contact-icon" style={{ background: 'rgba(37, 211, 102, 0.1)', color: '#25D366' }}>
                    <WhatsApp />
                  </div>
                  <div>
                    <div className="lp-contact-title">WhatsApp</div>
                    <div className="lp-contact-desc">
                      <a
                        href={`https://wa.me/${CLINIC_CONFIG.whatsapp?.replace(/[^0-9]/g, '')}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        style={{ fontWeight: 600, textDecoration: 'none' }}
                      >
                        {CLINIC_CONFIG.whatsapp}
                      </a>
                    </div>
                  </div>
                </div>

                <div className="lp-contact-item" style={{ marginBottom: 0 }}>
                  <div className="lp-contact-icon">
                    <Email />
                  </div>
                  <div>
                    <div className="lp-contact-title">Correo Electrónico</div>
                    <div className="lp-contact-desc">{CLINIC_CONFIG.email}</div>
                  </div>
                </div>
              </div>
            </div>

            {/* Quick Online Action Card */}
            <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
              <div
                style={{
                  background: 'linear-gradient(135deg, var(--lp-blue-900), var(--lp-blue-800))',
                  color: '#ffffff',
                  padding: '40px',
                  borderRadius: 'var(--lp-radius-lg)',
                  boxShadow: 'var(--lp-shadow-lg)',
                  position: 'relative',
                  overflow: 'hidden',
                }}
              >
                <h3 style={{ fontSize: '1.8rem', fontWeight: 800, marginBottom: '16px', lineHeight: 1.2 }}>
                  ¿Necesitás agendar una cita médica?
                </h3>
                <p style={{ color: 'var(--lp-blue-100)', fontSize: '1.05rem', lineHeight: 1.6, marginBottom: '32px' }}>
                  Accedé al sistema de turnos online para consultar agendas en tiempo real y seleccionar profesionales.
                </p>

                <Link
                  to="/login"
                  className="lp-btn"
                  style={{
                    background: '#ffffff',
                    color: 'var(--lp-blue-900)',
                    padding: '14px 32px',
                    fontWeight: 700,
                  }}
                >
                  <EventAvailable sx={{ fontSize: 20 }} />
                  <span>Ingresar al Portal de Turnos</span>
                </Link>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="lp-footer">
        <div className="lp-container">
          <div className="lp-footer-grid">
            <div className="lp-footer-brand">
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <div className="lp-logo-icon" style={{ background: '#ffffff', color: 'var(--lp-blue-900)' }}>
                  <LocalHospital sx={{ fontSize: 26 }} />
                </div>
                <span style={{ fontSize: '1.2rem', fontWeight: 800, color: '#ffffff' }}>
                  {CLINIC_CONFIG.name}
                </span>
              </div>
              <p>
                Comprometidos con la excelencia en atención médica, diagnóstico preciso y servicios de salud para toda la comunidad.
              </p>
            </div>

            <div>
              <div className="lp-footer-title">Navegación</div>
              <ul className="lp-footer-links">
                <li><a href="#inicio" className="lp-footer-link">Inicio</a></li>
                <li><a href="#nosotros" className="lp-footer-link">Nosotros</a></li>
                <li><a href="#staff" className="lp-footer-link">Staff Médico</a></li>
              </ul>
            </div>

            <div>
              <div className="lp-footer-title">Servicios</div>
              <ul className="lp-footer-links">
                <li><Link to="/login" className="lp-footer-link">Reserva de Turnos</Link></li>
                <li><Link to="/login" className="lp-footer-link">Portal del Paciente</Link></li>
                <li><Link to="/contacto" className="lp-footer-link">Contacto Directo</Link></li>
              </ul>
            </div>

            <div>
              <div className="lp-footer-title">Legales</div>
              <ul className="lp-footer-links">
                <li><Link to="/terminos-y-condiciones" className="lp-footer-link">Términos y Condiciones</Link></li>
                <li><Link to="/contacto" className="lp-footer-link">Contacto Legal</Link></li>
              </ul>
            </div>
          </div>

          <div className="lp-footer-bottom">
            <div>
              © {new Date().getFullYear()} {CLINIC_CONFIG.name}. Todos los derechos reservados.
            </div>
            <div>
              Sistema de Gestión y Reserva de Turnos Médicos
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;
