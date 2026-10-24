package com.example.healthcare.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for issuing, parsing, and validating JWT authentication tokens.
 * Complies with HIPAA/privacy regulations: embeds ONLY minimal metadata
 * (subject/username, role, issued-at, expiration) and NEVER contains PHI.
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    public static final String INSECURE_DEV_DEFAULT_SECRET = "healthcare-interop-engine-secure-development-jwt-secret-key-32bytes-min";

    private final String secret;
    private final long expiration;

    @org.springframework.beans.factory.annotation.Autowired
    public JwtService(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration:86400000}") long expiration,
            @org.springframework.beans.factory.annotation.Autowired(required = false) Environment environment) {
        this.expiration = expiration;

        boolean isProduction = environment != null && environment.acceptsProfiles(Profiles.of("prod", "production"));

        if (isProduction) {
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException("JWT_SECRET environment variable is required in production profiles.");
            }
            if (INSECURE_DEV_DEFAULT_SECRET.equals(secret.trim())) {
                throw new IllegalStateException("Production profile cannot use the insecure development JWT secret.");
            }
            if (secret.trim().getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("JWT_SECRET must be at least 32 bytes (256 bits) in production.");
            }
            this.secret = secret.trim();
        } else {
            if (secret != null && !secret.isBlank()) {
                this.secret = secret.trim();
            } else {
                logger.warn("JWT_SECRET is not configured. Falling back to development secret for non-production.");
                this.secret = INSECURE_DEV_DEFAULT_SECRET;
            }
        }
    }

    public JwtService(String secret, long expiration) {
        this(secret, expiration, null);
    }

    /**
     * Generates a signed JWT token for the authenticated user.
     *
     * @param userDetails authenticated user details
     * @return signed compact JWT string
     */
    public String generateToken(UserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");
        return generateToken(userDetails.getUsername(), role);
    }

    /**
     * Generates a signed JWT token with minimal claims (username, role, timestamps).
     *
     * @param username user principal
     * @param role     user authority role
     * @return signed compact JWT string
     */
    public String generateToken(String username, String role) {
        return generateToken(username, role, null);
    }

    /**
     * Generates a signed JWT token with claims (username, role, optional hospitalId, timestamps).
     *
     * @param username   user principal
     * @param role       user authority role
     * @param hospitalId hospital ID or null for platform administrators
     * @return signed compact JWT string
     */
    public String generateToken(String username, String role, Long hospitalId) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", role);
        if (hospitalId != null) {
            extraClaims.put("hospitalId", hospitalId);
        }

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts username (subject claim) from the token.
     *
     * @param token JWT token
     * @return subject username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts role claim from the token.
     *
     * @param token JWT token
     * @return role string
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extracts optional hospitalId claim from the token.
     *
     * @param token JWT token
     * @return hospitalId Long, or null if unassigned
     */
    public Long extractHospitalId(String token) {
        return extractClaim(token, claims -> {
            Object val = claims.get("hospitalId");
            if (val instanceof Number) {
                return ((Number) val).longValue();
            }
            return null;
        });
    }

    /**
     * Extracts token expiration timestamp.
     *
     * @param token JWT token
     * @return expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts an arbitrary claim using the provided resolver function.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Validates whether a token belongs to the given user and has not expired.
     *
     * @param token       JWT token
     * @param userDetails user details to match against
     * @return true if valid, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates that the token signature is valid and not expired without loading UserDetails.
     *
     * @param token JWT token
     * @return true if token is structurally and cryptographically valid
     */
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        Date exp = extractExpiration(token);
        return exp.before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
