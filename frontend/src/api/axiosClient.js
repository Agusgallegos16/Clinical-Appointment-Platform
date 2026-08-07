import axios from 'axios';

// Instancia centralizada de Axios
const axiosClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
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

// Interceptor para capturar expiraciones de sesión (401 Unauthorized)
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Solo redirigir si no estamos ya en login y si hay un token previo expirado
      const token = localStorage.getItem('jwt_token');
      if (token && window.location.pathname !== '/login') {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('user_data');
        window.location.href = '/login?expired=true';
      }
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
