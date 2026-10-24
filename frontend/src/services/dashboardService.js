import apiClient from './apiClient';

/**
 * Service for fetching real-time dashboard analytics.
 * Strictly scoped to the authenticated user's hospital tenant.
 */
export const dashboardService = {
  /**
   * Fetches tenant-scoped dashboard metrics from GET /api/dashboard/stats.
   *
   * @returns {Promise<Object>} Dashboard statistics response
   */
  async getStats() {
    const response = await apiClient.get('/api/dashboard/stats');
    return response.data;
  },

  /**
   * Probes backend and database operational health from GET /api/health.
   *
   * @returns {Promise<Object>} Engine health response { status, database, engine }
   */
  async getHealth() {
    const response = await apiClient.get('/api/health');
    return response.data;
  },
};

export default dashboardService;
