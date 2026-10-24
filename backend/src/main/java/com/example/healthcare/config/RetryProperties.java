package com.example.healthcare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for bounded FHIR delivery retries.
 */
@Component
@ConfigurationProperties(prefix = "app.retry")
public class RetryProperties {

    /**
     * Whether the automatic retry mechanism is enabled.
     */
    private boolean enabled = true;

    /**
     * Maximum number of attempts allowed for a delivery (initial attempt + retries).
     */
    private int maxAttempts = 3;

    /**
     * Initial backoff interval in milliseconds before the first retry.
     */
    private long initialIntervalMs = 2000L;

    /**
     * Exponential multiplier applied to backoff interval for each subsequent attempt.
     */
    private double multiplier = 2.0;

    /**
     * Maximum permitted backoff interval in milliseconds (upper bound cap).
     */
    private long maxIntervalMs = 60000L;

    /**
     * Randomized jitter factor (e.g. 0.2 = +/- 20%) to prevent synchronized retry storms.
     */
    private double jitter = 0.2;

    /**
     * Polling frequency in milliseconds for the scheduled background retry processor.
     */
    private long pollIntervalMs = 5000L;

    public RetryProperties() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getInitialIntervalMs() {
        return initialIntervalMs;
    }

    public void setInitialIntervalMs(long initialIntervalMs) {
        this.initialIntervalMs = initialIntervalMs;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public long getMaxIntervalMs() {
        return maxIntervalMs;
    }

    public void setMaxIntervalMs(long maxIntervalMs) {
        this.maxIntervalMs = maxIntervalMs;
    }

    public double getJitter() {
        return jitter;
    }

    public void setJitter(double jitter) {
        this.jitter = jitter;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }
}
