import apiClient from './apiClient';

/**
 * Service handling user and administrator account operations.
 */
export const userService = {
  /**
   * Creates a new user or administrator in the system.
   * Requires authenticated ADMIN credentials.
   *
   * @param {Object} userData - { username, password, role, hospitalId }
   * @returns {Promise<{id: number, username: string, role: string, enabled: boolean, message: string}>}
   */
  async createUser(userData) {
    if (!userData.username || !userData.password) {
      throw new Error('Username and password are required');
    }

    const payload = {
      username: userData.username.trim(),
      password: userData.password,
      role: userData.role || 'USER',
    };

    if (userData.hospitalId) {
      payload.hospitalId = Number(userData.hospitalId);
    }

    const response = await apiClient.post('/api/users', payload);
    return response.data;
  },

  /**
   * Retrieves all users accessible to the current administrator.
   * @returns {Promise<Array<{id: number, username: string, role: string, hospitalId: number, hospitalName: string, active: boolean, createdAt: string}>>}
   */
  async getUsers() {
    const response = await apiClient.get('/api/users');
    return response.data;
  },

  /**
   * Retrieves a single user by ID.
   * @param {number} id
   */
  async getUserById(id) {
    const response = await apiClient.get(`/api/users/${id}`);
    return response.data;
  },

  /**
   * Updates an existing user (e.g. role, active status, hospital).
   * Preserves username and existing password when password is not provided.
   *
   * @param {number} id
   * @param {Object} updateData - { role, hospitalId, clearHospital, enabled, password }
   */
  async updateUser(id, updateData) {
    const payload = {};
    if (updateData.role) payload.role = updateData.role;
    if (updateData.enabled !== undefined) payload.enabled = Boolean(updateData.enabled);
    if (updateData.hospitalId) {
      payload.hospitalId = Number(updateData.hospitalId);
    } else if (updateData.clearHospital) {
      payload.clearHospital = true;
    }
    if (updateData.password && updateData.password.trim()) {
      payload.password = updateData.password.trim();
    }
    const response = await apiClient.put(`/api/users/${id}`, payload);
    return response.data;
  },
};

export default userService;
