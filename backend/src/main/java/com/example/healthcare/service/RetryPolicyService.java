package com.example.healthcare.service;

import com.example.healthcare.config.RetryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Service encapsulating transient error classification, Retry-After header parsing,
 * and exponential backoff with jitter calculation for Priority 9.
 */
@Service
public class RetryPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(RetryPolicyService.class);

    private final RetryProperties retryProperties;
    private final Random random = new Random();

    public RetryPolicyService(RetryProperties retryProperties) {
        this.retryProperties = retryProperties;
    }

    public boolean isRetryEnabled() {
        return retryProperties.isEnabled();
    }

    public int getMaxAttempts() {
        return retryProperties.getMaxAttempts();
    }

    /**
     * Determines whether an error is transient and eligible for automatic bounded retry.
     * Connection failures, network errors, timeouts, HTTP 429, and transient 5xx responses (500, 502, 503, 504) are transient.
     * Permanent 4xx responses (400, 401, 403, 404, 405, 422) and payload validation failures are NOT eligible for automatic retry.
     * HTTP 401/403 are treated as authorization/configuration failures requiring human/administrative attention.
     *
     * @param httpStatusCode HTTP status code from remote response, if available
     * @param throwable      Exception thrown during dispatch, if available
     * @return true if transient and retryable, false if permanent
     */
    public boolean isTransientError(Integer httpStatusCode, Throwable throwable) {
        if (throwable != null) {
            if (throwable instanceof ResourceAccessException) {
                return true; // Connection refused, host unreachable, network partition
            }
            if (throwable.getCause() instanceof SocketTimeoutException
                    || throwable.getCause() instanceof java.net.ConnectException
                    || throwable.getCause() instanceof java.net.UnknownHostException) {
                return true;
            }
        }

        if (httpStatusCode != null) {
            if (httpStatusCode == 429) {
                return true; // Too Many Requests / Rate limited
            }
            if (httpStatusCode == 500 || httpStatusCode == 502 || httpStatusCode == 503 || httpStatusCode == 504) {
                return true; // Transient server errors & gateway timeouts
            }
            // 401 & 403 are authorization/token errors that will never succeed without configuration updates
            // 400, 404, 405, 422 are permanent bad request/resource errors
            return false;
        }

        return false;
    }

    /**
     * Calculates the next retry timestamp based on Retry-After header or exponential backoff with jitter.
     *
     * @param currentAttempt  number of attempts completed so far (1-indexed)
     * @param responseHeaders HTTP response headers from remote destination, if present
     * @return LocalDateTime for next scheduled attempt
     */
    public LocalDateTime calculateNextRetryTime(int currentAttempt, HttpHeaders responseHeaders) {
        long delayMs = calculateDelayMs(currentAttempt, responseHeaders);
        return LocalDateTime.now().plusNanos(delayMs * 1_000_000L);
    }

    /**
     * Calculates backoff delay in milliseconds.
     */
    public long calculateDelayMs(int currentAttempt, HttpHeaders responseHeaders) {
        // Step 1: Check for Retry-After header
        if (responseHeaders != null) {
            String retryAfter = responseHeaders.getFirst(HttpHeaders.RETRY_AFTER);
            if (retryAfter != null && !retryAfter.isBlank()) {
                try {
                    // Try parsing as integer seconds
                    long seconds = Long.parseLong(retryAfter.trim());
                    if (seconds > 0) {
                        return Math.min(seconds * 1000L, retryProperties.getMaxIntervalMs());
                    }
                } catch (NumberFormatException nfe) {
                    // Try parsing as HTTP-date (RFC 1123)
                    try {
                        Instant instant = DateTimeFormatter.RFC_1123_DATE_TIME.parse(retryAfter.trim(), Instant::from);
                        long diffMs = instant.toEpochMilli() - System.currentTimeMillis();
                        if (diffMs > 0) {
                            return Math.min(diffMs, retryProperties.getMaxIntervalMs());
                        }
                    } catch (Exception ignored) {
                        logger.debug("Failed to parse Retry-After header: {}", retryAfter);
                    }
                }
            }
        }

        // Step 2: Exponential backoff = initialInterval * (multiplier ^ (attempt - 1))
        double expFactor = Math.pow(retryProperties.getMultiplier(), Math.max(0, currentAttempt - 1));
        double rawDelay = retryProperties.getInitialIntervalMs() * expFactor;
        double cappedDelay = Math.min(rawDelay, retryProperties.getMaxIntervalMs());

        // Step 3: Add jitter (+/- jitter fraction) to avoid thundering herds / synchronized retry storms
        double jitterPercent = retryProperties.getJitter();
        double jitterMultiplier = 1.0 + (random.nextDouble() * 2.0 - 1.0) * jitterPercent;
        long finalDelay = Math.round(cappedDelay * jitterMultiplier);

        return Math.max(200L, Math.min(finalDelay, retryProperties.getMaxIntervalMs()));
    }
}
