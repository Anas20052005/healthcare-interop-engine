import apiClient from './apiClient';

/**
 * Service for managing conversion audit history.
 * Leverages the centralized Axios client with automatic Bearer JWT attachment.
 */
export const historyService = {
  /**
   * Fetches the audit history of conversions from GET /api/hl7/history.
   *
   * @returns {Promise<Array>} List of historical conversion audit records
   */
  async getHistory() {
    const response = await apiClient.get('/api/hl7/history');
    return response.data || [];
  },
};

export default historyService;
