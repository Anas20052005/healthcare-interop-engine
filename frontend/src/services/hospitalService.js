import apiClient from './apiClient';

/**
 * Service handling hospital tenant operations.
 * Communicates with backend /api/hospitals REST endpoints via centralized Axios client.
 */
export const hospitalService = {
  /**
   * Retrieves all registered hospitals.
   *
   * @returns {Promise<Array>} List of hospital response objects
   */
  async getHospitals() {
    const response = await apiClient.get('/api/hospitals');
    return response.data || [];
  },

  /**
   * Retrieves details for a specific hospital by ID.
   *
   * @param {number|string} id - Hospital ID
   * @returns {Promise<Object>} Hospital details
   */
  async getHospitalById(id) {
    const response = await apiClient.get(`/api/hospitals/${id}`);
    return response.data;
  },

  /**
   * Creates a new hospital tenant.
   * Requires ADMIN credentials.
   *
   * @param {Object} hospitalData - { name, code, email, phone, address, enabled }
   * @returns {Promise<Object>} Created hospital
   */
  async createHospital(hospitalData) {
    if (!hospitalData.name || !hospitalData.code) {
      throw new Error('Hospital name and unique code are required');
    }

    const payload = {
      name: hospitalData.name.trim(),
      code: hospitalData.code.trim().toUpperCase(),
      email: hospitalData.email ? hospitalData.email.trim() : null,
      phone: hospitalData.phone ? hospitalData.phone.trim() : null,
      address: hospitalData.address ? hospitalData.address.trim() : null,
      enabled: hospitalData.enabled !== undefined ? Boolean(hospitalData.enabled) : true,
    };

    const response = await apiClient.post('/api/hospitals', payload);
    return response.data;
  },

  /**
   * Updates an existing hospital tenant.
   * Requires ADMIN credentials.
   *
   * @param {number|string} id - Hospital ID
   * @param {Object} hospitalData - { name, code, email, phone, address, enabled }
   * @returns {Promise<Object>} Updated hospital
   */
  async updateHospital(id, hospitalData) {
    if (!hospitalData.name || !hospitalData.code) {
      throw new Error('Hospital name and unique code are required');
    }

    const payload = {
      name: hospitalData.name.trim(),
      code: hospitalData.code.trim().toUpperCase(),
      email: hospitalData.email ? hospitalData.email.trim() : null,
      phone: hospitalData.phone ? hospitalData.phone.trim() : null,
      address: hospitalData.address ? hospitalData.address.trim() : null,
      enabled: hospitalData.enabled !== undefined ? Boolean(hospitalData.enabled) : undefined,
    };

    const response = await apiClient.put(`/api/hospitals/${id}`, payload);
    return response.data;
  },
};

export default hospitalService;
