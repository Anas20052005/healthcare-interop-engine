package com.example.healthcare.service;

import com.example.healthcare.config.RetryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryPolicyServiceTest {

    private RetryProperties retryProperties;
    private RetryPolicyService retryPolicyService;

    @BeforeEach
    void setUp() {
        retryProperties = new RetryProperties();
        retryProperties.setEnabled(true);
        retryProperties.setMaxAttempts(3);
        retryProperties.setInitialIntervalMs(1000L);
        retryProperties.setMultiplier(2.0);
        retryProperties.setMaxIntervalMs(10000L);
        retryProperties.setJitter(0.1);
        retryProperties.setPollIntervalMs(5000L);

        retryPolicyService = new RetryPolicyService(retryProperties);
    }

    @Test
    @DisplayName("Transient error detection: network errors and timeouts are transient")
    void testTransientNetworkErrors() {
        assertTrue(retryPolicyService.isTransientError(null, new ResourceAccessException("Connection refused")));
        assertTrue(retryPolicyService.isTransientError(null, new ResourceAccessException("timeout", new SocketTimeoutException())));
        assertTrue(retryPolicyService.isTransientError(null, new RuntimeException("wrapped", new ConnectException())));
    }

    @Test
    @DisplayName("Transient error detection: HTTP 429 and transient 5xx responses are transient")
    void testTransientHttpStatusCodes() {
        assertTrue(retryPolicyService.isTransientError(429, null)); // Too Many Requests
        assertTrue(retryPolicyService.isTransientError(500, null)); // Internal Server Error
        assertTrue(retryPolicyService.isTransientError(502, null)); // Bad Gateway
        assertTrue(retryPolicyService.isTransientError(503, null)); // Service Unavailable
        assertTrue(retryPolicyService.isTransientError(504, null)); // Gateway Timeout
    }

    @Test
    @DisplayName("Permanent error detection: client 4xx errors and auth errors are NOT transient")
    void testPermanentHttpStatusCodes() {
        assertFalse(retryPolicyService.isTransientError(400, null)); // Bad Request
        assertFalse(retryPolicyService.isTransientError(401, null)); // Unauthorized (requires token update)
        assertFalse(retryPolicyService.isTransientError(403, null)); // Forbidden (requires privilege update)
        assertFalse(retryPolicyService.isTransientError(404, null)); // Not Found
        assertFalse(retryPolicyService.isTransientError(405, null)); // Method Not Allowed
        assertFalse(retryPolicyService.isTransientError(422, null)); // Unprocessable Entity
    }

    @Test
    @DisplayName("Retry-After header parsing: numeric seconds header is respected")
    void testRetryAfterHeaderNumericSeconds() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "5");

        long delay = retryPolicyService.calculateDelayMs(1, headers);
        assertEquals(5000L, delay);
    }

    @Test
    @DisplayName("Retry-After header parsing: capped at maxIntervalMs")
    void testRetryAfterHeaderCappedAtMaxInterval() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "3600"); // 1 hour

        long delay = retryPolicyService.calculateDelayMs(1, headers);
        assertEquals(10000L, delay); // capped at maxIntervalMs = 10000
    }

    @Test
    @DisplayName("Exponential backoff calculation grows with attempt count and applies upper bound")
    void testExponentialBackoffProgression() {
        // Attempt 1: ~1000ms (+/- 10% jitter = [900, 1100])
        long delay1 = retryPolicyService.calculateDelayMs(1, null);
        assertTrue(delay1 >= 900L && delay1 <= 1100L, "Delay 1 should be around 1000ms: " + delay1);

        // Attempt 2: ~2000ms (+/- 10% jitter = [1800, 2200])
        long delay2 = retryPolicyService.calculateDelayMs(2, null);
        assertTrue(delay2 >= 1800L && delay2 <= 2200L, "Delay 2 should be around 2000ms: " + delay2);

        // Attempt 3: ~4000ms (+/- 10% jitter = [3600, 4400])
        long delay3 = retryPolicyService.calculateDelayMs(3, null);
        assertTrue(delay3 >= 3600L && delay3 <= 4400L, "Delay 3 should be around 4000ms: " + delay3);

        // High attempt: should be capped at maxIntervalMs = 10000ms
        long delay10 = retryPolicyService.calculateDelayMs(10, null);
        assertTrue(delay10 <= 10000L, "Delay should be capped at maxIntervalMs: " + delay10);
    }

    @Test
    @DisplayName("calculateNextRetryTime returns future timestamp")
    void testCalculateNextRetryTime() {
        LocalDateTime next = retryPolicyService.calculateNextRetryTime(1, null);
        assertNotNull(next);
        assertTrue(next.isAfter(LocalDateTime.now().minusSeconds(1)));
    }
}
