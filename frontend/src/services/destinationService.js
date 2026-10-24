import apiClient from './apiClient';

/**
 * Service for configuring external and mock FHIR destinations
 * and dispatching converted FHIR R4 outputs.
 */
export const destinationService = {
  /**
   * Retrieves all accessible destinations (hospital-scoped and platform-wide).
   * @returns {Promise<Array>} list of destinations
   */
  async getDestinations() {
    const response = await apiClient.get('/api/destinations');
    return response.data || [];
  },

  /**
   * Retrieves a specific destination by ID.
   * @param {number|string} id destination ID
   * @returns {Promise<object>} destination details
   */
  async getDestinationById(id) {
    const response = await apiClient.get(`/api/destinations/${id}`);
    return response.data;
  },

  /**
   * Creates a new destination configuration.
   * @param {object} payload destination creation payload
   * @returns {Promise<object>} created destination
   */
  async createDestination(payload) {
    const response = await apiClient.post('/api/destinations', payload);
    return response.data;
  },

  /**
   * Updates an existing destination configuration.
   * @param {number|string} id destination ID
   * @param {object} payload destination update payload
   * @returns {Promise<object>} updated destination
   */
  async updateDestination(id, payload) {
    const response = await apiClient.put(`/api/destinations/${id}`, payload);
    return response.data;
  },

  /**
   * Toggles or updates the enabled/disabled status of a destination.
   * @param {number|string} id destination ID
   * @param {boolean} enabled status flag
   * @returns {Promise<object>} updated destination
   */
  async updateDestinationStatus(id, enabled) {
    const response = await apiClient.patch(`/api/destinations/${id}/status`, { enabled });
    return response.data;
  },

  /**
   * Deletes a destination configuration.
   * @param {number|string} id destination ID
   * @returns {Promise<void>}
   */
  async deleteDestination(id) {
    await apiClient.delete(`/api/destinations/${id}`);
  },

  /**
   * Explicitly dispatches a conversion's FHIR output to a selected destination.
   * @param {number|string} conversionId conversion ID
   * @param {number|string} destinationId destination ID
   * @returns {Promise<object>} delivery result
   */
  async sendConversion(conversionId, destinationId) {
    const response = await apiClient.post(`/api/hl7/conversions/${conversionId}/send`, {
      destinationId: Number(destinationId),
    });
    return response.data;
  },

  /**
   * Retrieves delivery attempt history for a specific conversion.
   * @param {number|string} conversionId conversion ID
   * @returns {Promise<Array>} list of delivery attempts
   */
  async getConversionDeliveries(conversionId) {
    const response = await apiClient.get(`/api/hl7/conversions/${conversionId}/deliveries`);
    return response.data || [];
  },

  /**
   * Retrieves delivery history for a specific destination.
   * @param {number|string} destinationId destination ID
   * @returns {Promise<Array>} list of delivery attempts
   */
  async getDestinationDeliveries(destinationId) {
    const response = await apiClient.get(`/api/destinations/${destinationId}/deliveries`);
    return response.data || [];
  },

  /**
   * Retrieves details of a specific delivery attempt by ID.
   * @param {number|string} deliveryId delivery result ID
   * @returns {Promise<object>} delivery attempt details
   */
  async getDeliveryById(deliveryId) {
    const response = await apiClient.get(`/api/hl7/deliveries/${deliveryId}`);
    return response.data;
  },

  /**
   * Retries an eligible failed delivery attempt.
   * @param {number|string} deliveryId delivery result ID
   * @returns {Promise<object>} updated delivery attempt result
   */
  async retryDelivery(deliveryId) {
    const response = await apiClient.post(`/api/hl7/deliveries/${deliveryId}/retry`);
    return response.data;
  },

  /**
   * Retrieves received resources stored by the local mock FHIR destination.
   * @returns {Promise<Array>} list of received resources
   */
  async getMockReceivedMessages() {
    const response = await apiClient.get('/api/mock-fhir/received');
    return response.data || [];
  },

  /**
   * Clears the local mock FHIR destination received messages buffer.
   * @returns {Promise<void>}
   */
  async clearMockReceivedMessages() {
    await apiClient.delete('/api/mock-fhir/received');
  },

  /**
   * Tests connectivity to a destination by probing its FHIR /metadata endpoint.
   * No patient data is sent. Returns success, latency, HTTP status, and server info.
   * @param {number|string} id destination ID
   * @returns {Promise<object>} { success, message, httpStatusCode, serverSoftware, serverFhirVersion, latencyMs }
   */
  async testConnection(id) {
    const response = await apiClient.get(`/api/destinations/${id}/test`);
    return response.data;
  },
};

export default destinationService;
