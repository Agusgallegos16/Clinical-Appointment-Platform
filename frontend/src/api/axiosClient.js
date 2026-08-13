import axios from 'axios';

// Instancia centralizada de Axios con timeout defensivo
const axiosClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Interceptor para inyectar automáticamente el Token JWT en el encabezado Authorization
axiosClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Interceptor para capturar expiraciones de sesión (401 Unauthorized) y accesos denegados (403 Forbidden)
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    if (status === 401) {
      const token = localStorage.getItem('jwt_token');
      if (token && window.location.pathname !== '/login') {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('user_data');
        window.location.href = '/login?expired=true';
      }
    } else if (status === 403) {
      console.warn('Acceso denegado a recurso restringido.');
    }

    return Promise.reject(error);
  }
);

export default axiosClient;
