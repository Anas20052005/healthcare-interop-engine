package com.example.healthcare.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service providing authenticated AES-256-GCM encryption and decryption for sensitive tokens.
 * Complies with HIPAA and security rules: tokens are stored encrypted at rest and never logged or exposed in plain text.
 */
@Service
public class TokenCryptoService {

    private static final Logger logger = LoggerFactory.getLogger(TokenCryptoService.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final String NON_PROD_DEV_KEY = "healthcare-dedicated-destination-token-key-non-prod-32b";

    private final byte[] primaryKeyBytes;
    private final byte[] legacyKeyBytes;
    private final boolean hasDedicatedKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @org.springframework.beans.factory.annotation.Autowired
    public TokenCryptoService(
            @Value("${app.token-encryption-key:}") String tokenEncryptionKey,
            @Value("${jwt.secret:}") String jwtSecret,
            @org.springframework.beans.factory.annotation.Autowired(required = false) Environment environment) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");

            String effectiveJwtSecret = (jwtSecret != null && !jwtSecret.isBlank())
                    ? jwtSecret.trim()
                    : "healthcare-interop-engine-secure-development-jwt-secret-key-32bytes-min";
            this.legacyKeyBytes = sha.digest(effectiveJwtSecret.getBytes(StandardCharsets.UTF_8));

            boolean isProduction = environment != null && environment.acceptsProfiles(Profiles.of("prod", "production"));

            if (tokenEncryptionKey != null && !tokenEncryptionKey.isBlank()) {
                String cleanKey = tokenEncryptionKey.trim();

                if (jwtSecret != null && !jwtSecret.isBlank() && cleanKey.equals(jwtSecret.trim())) {
                    throw new IllegalStateException("Security violation: TOKEN_ENCRYPTION_KEY must not reuse JWT_SECRET as the encryption key.");
                }

                if (isProduction && cleanKey.getBytes(StandardCharsets.UTF_8).length < 32) {
                    throw new IllegalStateException("TOKEN_ENCRYPTION_KEY must be at least 32 bytes in production.");
                }

                this.primaryKeyBytes = sha.digest(cleanKey.getBytes(StandardCharsets.UTF_8));
                this.hasDedicatedKey = true;
                logger.info("TokenCryptoService initialized with dedicated destination encryption key");
            } else {
                if (isProduction) {
                    throw new IllegalStateException("TOKEN_ENCRYPTION_KEY environment variable is required in production when destination credential encryption is enabled. Do not reuse JWT_SECRET.");
                }

                // In non-production without a dedicated key, use an isolated non-prod key distinct from jwtSecret
                this.primaryKeyBytes = sha.digest(NON_PROD_DEV_KEY.getBytes(StandardCharsets.UTF_8));
                this.hasDedicatedKey = false;
                logger.warn("TokenCryptoService initialized without dedicated key in non-production; using isolated non-prod encryption key distinct from JWT secret. Set 'app.token-encryption-key' in production.");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize TokenCryptoService", e);
        }
    }

    public TokenCryptoService(String tokenEncryptionKey, String jwtSecret) {
        this(tokenEncryptionKey, jwtSecret, null);
    }

    /**
     * Convenience single-argument constructor for legacy test suites using a single secret.
     */
    public TokenCryptoService(String secret) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            this.legacyKeyBytes = sha.digest(secret.getBytes(StandardCharsets.UTF_8));
            this.primaryKeyBytes = this.legacyKeyBytes;
            this.hasDedicatedKey = false;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize TokenCryptoService", e);
        }
    }

    /**
     * Encrypts a plaintext token string using AES-256-GCM with a randomized IV.
     *
     * @param plainToken plaintext token
     * @return Base64-encoded encrypted string containing IV + ciphertext + authentication tag
     */
    public String encrypt(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            return null;
        }
        return encryptWithKey(plainToken, primaryKeyBytes);
    }

    private String encryptWithKey(String plainToken, byte[] key) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainToken.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            logger.error("Failed to encrypt token safely", e);
            throw new IllegalStateException("Encryption failed");
        }
    }

    /**
     * Decrypts an encrypted token string back to its plaintext representation for HTTP transport.
     * Tries the primary dedicated key first. If it fails with AEAD authentication error and a dedicated key is active,
     * it falls back to the legacy key derived from JWT secret for seamless backward compatibility.
     *
     * @param encryptedToken Base64-encoded encrypted token
     * @return plaintext token
     */
    public String decrypt(String encryptedToken) {
        if (encryptedToken == null || encryptedToken.isBlank()) {
            return null;
        }
        try {
            return decryptWithKey(encryptedToken, primaryKeyBytes);
        } catch (javax.crypto.AEADBadTagException e) {
            if (hasDedicatedKey) {
                logger.debug("Decryption with primary key failed authentication tag check; attempting legacy key fallback");
                try {
                    return decryptWithKey(encryptedToken, legacyKeyBytes);
                } catch (Exception legacyException) {
                    logger.error("Failed to decrypt token with legacy key fallback as well", legacyException);
                    throw new IllegalStateException("Decryption failed");
                }
            }
            logger.error("Failed to decrypt token safely (AEAD tag mismatch)", e);
            throw new IllegalStateException("Decryption failed");
        } catch (Exception e) {
            logger.error("Failed to decrypt token safely", e);
            throw new IllegalStateException("Decryption failed");
        }
    }

    private String decryptWithKey(String encryptedToken, byte[] key) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(encryptedToken);
        if (decoded.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted token format");
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);

        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

        byte[] plainTextBytes = cipher.doFinal(cipherText);
        return new String(plainTextBytes, StandardCharsets.UTF_8);
    }

    /**
     * Checks whether an existing ciphertext requires migration to the primary key.
     * If encrypted with the legacy key and decryptable, re-encrypts using the dedicated primary key.
     * If already encrypted with the primary key or undecryptable, returns the original token safely without overwrite.
     */
    public String reencryptIfNeeded(String encryptedToken) {
        if (encryptedToken == null || encryptedToken.isBlank() || !hasDedicatedKey) {
            return encryptedToken;
        }
        // Test if primary key can decrypt it
        try {
            decryptWithKey(encryptedToken, primaryKeyBytes);
            // Already encrypted with primary key!
            return encryptedToken;
        } catch (javax.crypto.AEADBadTagException tagEx) {
            // Decrypt with legacy key and re-encrypt with primary key
            try {
                String plain = decryptWithKey(encryptedToken, legacyKeyBytes);
                logger.info("Successfully migrated legacy ciphertext to dedicated encryption key");
                return encrypt(plain);
            } catch (Exception e) {
                logger.warn("Could not decrypt legacy token for re-encryption, preserving existing ciphertext without modification");
                return encryptedToken;
            }
        } catch (Exception e) {
            logger.warn("Could not verify token encryption key state, preserving existing ciphertext");
            return encryptedToken;
        }
    }

    public boolean hasDedicatedKey() {
        return hasDedicatedKey;
    }
}
