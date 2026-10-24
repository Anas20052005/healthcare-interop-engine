import apiClient from './apiClient';

/**
 * Service handling patient demographic and clinical records retrieval.
 * Enforces strict hospital tenant boundaries at the API level.
 */
export const patientService = {
  /**
   * Lists patients scoped to the current user's hospital.
   * @returns {Promise<Array>}
   */
  async getPatients() {
    const response = await apiClient.get('/api/patients');
    return response.data;
  },

  /**
   * Retrieves full details for a single patient.
   * @param {number} id
   * @returns {Promise<Object>}
   */
  async getPatientById(id) {
    const response = await apiClient.get(`/api/patients/${id}`);
    return response.data;
  },

  /**
   * Retrieves encounters for a patient.
   * @param {number} patientId
   * @returns {Promise<Array>}
   */
  async getPatientEncounters(patientId) {
    const response = await apiClient.get(`/api/patients/${patientId}/encounters`);
    return response.data;
  },

  /**
   * Retrieves observations/results for a patient.
   * @param {number} patientId
   * @returns {Promise<Array>}
   */
  async getPatientObservations(patientId) {
    const response = await apiClient.get(`/api/patients/${patientId}/observations`);
    return response.data;
  },
};

export default patientService;
