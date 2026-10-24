import axios from 'axios';

/**
 * Centralized Axios API Client
 * Configured with base URL, JWT authorization interceptor, and 401 handling.
 */
const RAW_API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const API_BASE_URL = RAW_API_BASE_URL.trim().replace(/\/+$/, '');

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

// Request Interceptor: Attach JWT Bearer token automatically
apiClient.interceptors.request.use(
  (config) => {
    // Exclude public endpoints from receiving authorization headers
    const isPublic = config.url?.includes('/api/auth/login') || config.url?.includes('/api/health');

    if (!isPublic) {
      const token = localStorage.getItem('hl7_auth_token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response Interceptor: Handle 401 Unauthorized & sanitize error responses
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    const status = error.response ? error.response.status : null;

    if (status === 401) {
      // Clear expired or invalid credentials
      localStorage.removeItem('hl7_auth_token');
      localStorage.removeItem('hl7_auth_user');

      // Dispatch custom event so React context can sync cleanly without a full page reload if desired
      window.dispatchEvent(new Event('auth:unauthorized'));

      // Redirect to login if not already on the login page
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    // Extract sanitized, user-friendly error message
    let friendlyMessage = 'An unexpected error occurred while communicating with the server.';

    if (error.response?.data?.message) {
      friendlyMessage = error.response.data.message;
    } else if (error.code === 'ERR_NETWORK') {
      friendlyMessage = 'Cannot connect to backend server. Please verify the Spring Boot service is running on port 8080.';
    } else if (error.message) {
      friendlyMessage = error.message;
    }

    // Enhance error with clean sanitized message
    error.userMessage = friendlyMessage;
    return Promise.reject(error);
  }
);

export default apiClient;
