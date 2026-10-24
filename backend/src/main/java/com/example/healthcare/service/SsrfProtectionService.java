package com.example.healthcare.service;

import com.example.healthcare.exception.InvalidDestinationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Service protecting against Server-Side Request Forgery (SSRF) and unsafe destination URLs.
 * Rejects localhost and private-network IP ranges for user-configured external destinations,
 * while permitting the explicitly designated internal mock FHIR destination.
 */
@Service
public class SsrfProtectionService {

    private static final Logger logger = LoggerFactory.getLogger(SsrfProtectionService.class);

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * Validates a destination URL against SSRF rules.
     *
     * @param rawUrl URL to validate
     * @param isMock whether the destination is designated as the internal mock server
     * @throws InvalidDestinationException if the URL violates security policies
     */
    public void validateDestinationUrl(String rawUrl, boolean isMock) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new InvalidDestinationException("Destination base URL cannot be empty");
        }

        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (Exception e) {
            throw new InvalidDestinationException("Invalid URL syntax: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new InvalidDestinationException("Destination URL must use HTTP or HTTPS scheme");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidDestinationException("Destination URL must include a valid host");
        }

        int port = uri.getPort();
        if (port == -1) {
            port = scheme.equalsIgnoreCase("https") ? 443 : 80;
        }

        String path = uri.getPath() != null ? uri.getPath() : "";

        boolean isInternalMockEndpoint = isMockEndpoint(host, port, path);

        if (isMock || isInternalMockEndpoint) {
            if (!isInternalMockEndpoint) {
                logger.warn("Mock destination URL '{}' does not target the designated mock FHIR endpoint", rawUrl);
                throw new InvalidDestinationException("Mock destination must target the designated internal mock endpoint (/api/mock-fhir)");
            }
            // Explicitly designated internal mock destination is permitted!
            return;
        }

        // External destination: block loopback and private networks
        if (isLocalOrPrivateHost(host)) {
            logger.warn("SSRF attempt blocked: destination host '{}' is a restricted private/local address", host);
            throw new InvalidDestinationException("Destination URL points to a restricted local or private network address");
        }
    }

    public boolean isMockEndpoint(String host, int port, String path) {
        boolean isLocalHost = host.equalsIgnoreCase("localhost")
                || host.equals("127.0.0.1")
                || host.equals("::1");
        boolean isMatchingPort = (port == serverPort || port == 8080);
        boolean isMockPath = path.startsWith("/api/mock-fhir");
        return isLocalHost && isMatchingPort && isMockPath;
    }

    private boolean isLocalOrPrivateHost(String host) {
        String lower = host.toLowerCase().trim();
        if (lower.equals("localhost") || lower.endsWith(".localhost") || lower.equals("0.0.0.0") || lower.equals("[::]") || lower.equals("::")) {
            return true;
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isRestrictedAddress(address)) {
                    return true;
                }
            }
        } catch (UnknownHostException e) {
            // If DNS cannot resolve, perform static string pattern defense
            if (lower.startsWith("127.") || lower.startsWith("10.")
                    || lower.startsWith("192.168.") || lower.startsWith("172.")
                    || lower.startsWith("169.254.") || lower.startsWith("fc")
                    || lower.startsWith("fd") || lower.startsWith("fe80")) {
                return true;
            }
        }

        return false;
    }

    private boolean isRestrictedAddress(InetAddress address) {
        if (address.isLoopbackAddress() || address.isSiteLocalAddress()
                || address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            // 127.0.0.0/8, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 169.254.0.0/16, 100.64.0.0/10 (CGNAT), 0.0.0.0/8
            if (b0 == 0 || b0 == 127 || b0 == 10 || (b0 == 172 && (b1 >= 16 && b1 <= 31))
                    || (b0 == 192 && b1 == 168) || (b0 == 169 && b1 == 254)
                    || (b0 == 100 && (b1 >= 64 && b1 <= 127))) {
                return true;
            }
        } else if (bytes.length == 16) {
            // IPv6 Unique Local Address fc00::/7 (fc00:: - fdff::)
            int b0 = bytes[0] & 0xFF;
            if ((b0 & 0xFE) == 0xFC) {
                return true;
            }
            // IPv4-mapped IPv6 addresses ::ffff:0:0/96
            boolean isMapped = true;
            for (int i = 0; i < 10; i++) {
                if (bytes[i] != 0) {
                    isMapped = false;
                    break;
                }
            }
            if (isMapped && (bytes[10] & 0xFF) == 0xFF && (bytes[11] & 0xFF) == 0xFF) {
                int mb0 = bytes[12] & 0xFF;
                int mb1 = bytes[13] & 0xFF;
                if (mb0 == 0 || mb0 == 127 || mb0 == 10 || (mb0 == 172 && (mb1 >= 16 && mb1 <= 31))
                        || (mb0 == 192 && mb1 == 168) || (mb0 == 169 && mb1 == 254)) {
                    return true;
                }
            }
        }
        return false;
    }
}
