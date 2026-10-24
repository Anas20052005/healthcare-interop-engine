package com.example.healthcare.dto.destination;

/**
 * Response DTO for the connection test action on a FHIR destination.
 * Reports whether the server is reachable and FHIR-compliant without
 * sending real patient data.
 */
public class DestinationTestResponse {

    /** Whether the test was broadly successful (HTTP 2xx or FHIR response received). */
    private boolean success;

    /** Human-readable status message shown in the UI. */
    private String message;

    /** HTTP status code returned by the remote server, if any. */
    private Integer httpStatusCode;

    /** FHIR CapabilityStatement software name, if the server returned one. */
    private String serverSoftware;

    /** FHIR CapabilityStatement FHIR version, if returned. */
    private String serverFhirVersion;

    /** How long the probe took in milliseconds. */
    private long latencyMs;

    public DestinationTestResponse() {
    }

    public DestinationTestResponse(boolean success, String message, Integer httpStatusCode,
                                   String serverSoftware, String serverFhirVersion, long latencyMs) {
        this.success = success;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
        this.serverSoftware = serverSoftware;
        this.serverFhirVersion = serverFhirVersion;
        this.latencyMs = latencyMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getServerSoftware() {
        return serverSoftware;
    }

    public void setServerSoftware(String serverSoftware) {
        this.serverSoftware = serverSoftware;
    }

    public String getServerFhirVersion() {
        return serverFhirVersion;
    }

    public void setServerFhirVersion(String serverFhirVersion) {
        this.serverFhirVersion = serverFhirVersion;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }
}
