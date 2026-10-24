package com.example.healthcare.dto.common;

/**
 * Health response reporting overall service health and database availability
 * without leaking sensitive infrastructure details, passwords, or connection strings.
 */
public class HealthResponse {

    private String status; // "UP" or "DOWN"
    private String database; // "UP" or "DOWN"
    private String engine; // "UP" or "DEGRADED"

    public HealthResponse() {
    }

    public HealthResponse(String status) {
        this(status, status, status);
    }

    public HealthResponse(String status, String database, String engine) {
        this.status = status;
        this.database = database;
        this.engine = engine;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }
}
