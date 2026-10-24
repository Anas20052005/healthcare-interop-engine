import apiClient from './apiClient';

/**
 * Service for managing audit log retrieval.
 * Leverages the centralized Axios client with automatic Bearer JWT attachment.
 */
export const auditService = {
  /**
   * Fetches paginated, filtered audit logs from GET /api/audit-logs.
   *
   * @param {Object} params Filter and pagination parameters
   * @returns {Promise<Object>} Page response containing content, page, size, totalElements, totalPages
   */
  async getAuditLogs(params = {}) {
    const queryParams = new URLSearchParams();

    if (params.page !== undefined && params.page !== null) {
      queryParams.append('page', params.page);
    }
    if (params.size !== undefined && params.size !== null) {
      queryParams.append('size', params.size);
    }
    if (params.action && params.action !== 'ALL') {
      queryParams.append('action', params.action);
    }
    if (params.outcome && params.outcome !== 'ALL') {
      queryParams.append('outcome', params.outcome);
    }
    if (params.resourceType && params.resourceType !== 'ALL') {
      queryParams.append('resourceType', params.resourceType);
    }
    if (params.startDate) {
      queryParams.append('startDate', params.startDate);
    }
    if (params.endDate) {
      queryParams.append('endDate', params.endDate);
    }
    if (params.search) {
      queryParams.append('search', params.search);
    }
    if (params.hospitalId) {
      queryParams.append('hospitalId', params.hospitalId);
    }

    const queryString = queryParams.toString();
    const url = queryString ? `/api/audit-logs?${queryString}` : '/api/audit-logs';
    const response = await apiClient.get(url);
    return response.data || { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
  },

  /**
   * Fetches a single audit log entry by ID.
   *
   * @param {number|string} id The audit log record ID
   * @returns {Promise<Object>} The audit log response object
   */
  async getAuditLogById(id) {
    const response = await apiClient.get(`/api/audit-logs/${id}`);
    return response.data;
  },
};

export default auditService;
