import apiClient from './apiClient';

const TOKEN_KEY = 'hl7_auth_token';
const USER_KEY = 'hl7_auth_user';

export const authService = {
  /**
   * Authenticates user against POST /api/auth/login.
   *
   * @param {string} username
   * @param {string} password
   * @returns {Promise<{token: string, tokenType: string, user: object}>}
   */
  async login(username, password) {
    if (!username || !password) {
      throw new Error('Username and password are required');
    }

    const response = await apiClient.post('/api/auth/login', {
      username: username.trim(),
      password: password,
    });

    const data = response.data;
    const token = data.token;

    if (!token) {
      throw new Error('Authentication succeeded but no token was received');
    }

    // Persist token safely
    localStorage.setItem(TOKEN_KEY, token);

    // Decode JWT claims safely without exposing PHI
    let cleanRole = 'ADMIN';
    let sub = username.trim();
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(base64));
      if (payload.role) {
        cleanRole = payload.role.replace(/^ROLE_/, '');
      }
      if (payload.sub) {
        sub = payload.sub;
      }
    } catch {
      // Fallback to default role
    }

    // Save minimal non-PHI user profile
    const userProfile = {
      username: sub,
      role: cleanRole,
      name: cleanRole === 'ADMIN' ? 'System Administrator' : 'Clinical User',
    };
    localStorage.setItem(USER_KEY, JSON.stringify(userProfile));

    return {
      token,
      tokenType: data.tokenType || 'Bearer',
      user: userProfile,
    };
  },

  /**
   * Clears stored authentication state and token.
   */
  logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },

  /**
   * Retrieves the active JWT token.
   *
   * @returns {string|null}
   */
  getToken() {
    return localStorage.getItem(TOKEN_KEY);
  },

  /**
   * Retrieves the active user profile.
   *
   * @returns {object|null}
   */
  getCurrentUser() {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  },

  /**
   * Checks whether a valid session token exists.
   *
   * @returns {boolean}
   */
  isAuthenticated() {
    return Boolean(localStorage.getItem(TOKEN_KEY));
  },
};

export default authService;
