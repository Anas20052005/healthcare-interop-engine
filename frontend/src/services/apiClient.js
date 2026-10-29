import axios from 'axios';

/**
 * Production and development API URLs.
 * In production builds (import.meta.env.PROD), never fall back to localhost.
 */
const PROD_API_URL = 'https://healthcare-interop-engine.onrender.com';
const DEV_API_URL = 'http://localhost:8080';

const envApiBase = import.meta.env.VITE_API_BASE_URL?.trim();
const isLocalhost = (url) => Boolean(url && (url.includes('localhost') || url.includes('127.0.0.1')));

let resolvedBaseUrl;
if (import.meta.env.PROD) {
  // In production builds, strictly reject accidental localhost / 127.0.0.1 settings
  if (envApiBase && !isLocalhost(envApiBase)) {
    resolvedBaseUrl = envApiBase;
  } else {
    resolvedBaseUrl = PROD_API_URL;
  }
} else {
  resolvedBaseUrl = envApiBase || DEV_API_URL;
}

export const API_BASE_URL = resolvedBaseUrl.replace(/\/+$/, '');

// Configurable client timeout: default to 45s in prod (accommodates Render cold starts), 30s in dev
const parsedTimeout = Number(import.meta.env.VITE_API_TIMEOUT_MS);
export const API_TIMEOUT_MS = !isNaN(parsedTimeout) && parsedTimeout > 0
  ? parsedTimeout
  : (import.meta.env.PROD ? 45000 : 30000);

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: API_TIMEOUT_MS,
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

// Response Interceptor: Handle 401, server errors, cold start timeouts & network errors
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
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    // Determine specific, user-friendly error message
    let friendlyMessage = 'An unexpected error occurred while communicating with the server.';

    if (error.response?.data?.message) {
      friendlyMessage = error.response.data.message;
    } else if (status === 401) {
      friendlyMessage = 'Invalid username or password. Please verify your credentials.';
    } else if (status === 403) {
      friendlyMessage = 'Access denied. You do not have permission to access this resource.';
    } else if (status === 404) {
      friendlyMessage = 'The requested resource was not found on the server.';
    } else if (status === 502 || status === 503) {
      friendlyMessage = 'The backend service is temporarily unavailable or initializing. Please retry in a few moments.';
    } else if (status === 504) {
      friendlyMessage = 'Gateway timeout: the server took too long to respond. The backend may still be starting up. Please try again.';
    } else if (error.code === 'ECONNABORTED' || error.message?.toLowerCase().includes('timeout')) {
      friendlyMessage = 'Request timed out. The backend server is likely waking up from idle mode (cold start). Please wait a moment and try again.';
      error.isTimeout = true;
      error.isColdStart = true;
    } else if (error.code === 'ERR_NETWORK') {
      if (import.meta.env.PROD) {
        friendlyMessage = 'Cannot connect to backend server. The service may be waking up from sleep or experiencing network issues. Please check your connection and retry.';
      } else {
        friendlyMessage = `Cannot connect to backend server at ${API_BASE_URL}. Please verify the Spring Boot service is running.`;
      }
      error.isNetworkError = true;
    } else if (error.message) {
      friendlyMessage = error.message;
    }

    // Enhance error with clean sanitized message
    error.userMessage = friendlyMessage;
    return Promise.reject(error);
  }
);

export default apiClient;
