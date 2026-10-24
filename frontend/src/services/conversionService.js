import apiClient from './apiClient';

export const conversionService = {
  /**
   * Submits raw HL7 v2 message to backend POST /api/hl7/convert.
   *
   * @param {string} hl7Message raw HL7 message string
   * @returns {Promise<object>} normalized conversion response
   */
  async convertHL7(hl7Message) {
    if (!hl7Message || !hl7Message.trim()) {
      throw new Error('HL7 message is required');
    }

    const response = await apiClient.post('/api/hl7/convert', {
      hl7Message: hl7Message.trim(),
    });

    const data = response.data;

    // Safely parse fhirOutput if returned as a JSON string
    let parsedFhir = null;
    if (data.fhirOutput) {
      if (typeof data.fhirOutput === 'object') {
        parsedFhir = data.fhirOutput;
      } else if (typeof data.fhirOutput === 'string') {
        try {
          parsedFhir = JSON.parse(data.fhirOutput);
        } catch (parseErr) {
          console.warn('FHIR JSON string could not be parsed as JSON object; keeping raw string:', parseErr);
          parsedFhir = data.fhirOutput;
        }
      }
    }

    return {
      status: data.status || 'SUCCESS',
      conversionId: data.conversionId,
      messageControlId: data.messageControlId,
      messageType: data.messageType,
      patientId: data.patientId,
      encounterId: data.encounterId,
      observationCount: data.observationCount,
      fhirBundle: parsedFhir,
      rawFhirOutput: data.fhirOutput,
      timestamp: data.timestamp || new Date().toISOString(),
    };
  },
};

export default conversionService;
