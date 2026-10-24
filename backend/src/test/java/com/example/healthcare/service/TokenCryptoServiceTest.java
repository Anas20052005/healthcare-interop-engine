package com.example.healthcare.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenCryptoServiceTest {

    private static final String JWT_SECRET = "test-jwt-secret-key-that-is-at-least-32-characters-long";
    private static final String DEDICATED_KEY = "dedicated-encryption-key-for-destinations-32chars";

    @Test
    @DisplayName("Encrypt and decrypt with single secret (backward compatibility)")
    void testSingleSecretEncryptionDecryption() {
        TokenCryptoService service = new TokenCryptoService(JWT_SECRET);
        assertFalse(service.hasDedicatedKey());

        String plaintext = "my-secret-bearer-token-12345";
        String ciphertext = service.encrypt(plaintext);

        assertNotNull(ciphertext);
        assertNotEquals(plaintext, ciphertext);

        String decrypted = service.decrypt(ciphertext);
        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("Encrypt and decrypt with dedicated encryption key")
    void testDedicatedKeyEncryptionDecryption() {
        TokenCryptoService service = new TokenCryptoService(DEDICATED_KEY, JWT_SECRET);
        assertTrue(service.hasDedicatedKey());

        String plaintext = "my-new-destination-token-xyz";
        String ciphertext = service.encrypt(plaintext);

        assertNotNull(ciphertext);
        assertNotEquals(plaintext, ciphertext);

        String decrypted = service.decrypt(ciphertext);
        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("Decrypt legacy ciphertext encrypted with JWT secret when dedicated key is active")
    void testLegacyCiphertextFallbackDecryption() {
        // Encrypt with legacy service (only JWT secret)
        TokenCryptoService legacyService = new TokenCryptoService(JWT_SECRET);
        String plaintext = "legacy-bearer-token-from-existing-db";
        String legacyCiphertext = legacyService.encrypt(plaintext);

        // Modern service with dedicated key configured
        TokenCryptoService modernService = new TokenCryptoService(DEDICATED_KEY, JWT_SECRET);
        assertTrue(modernService.hasDedicatedKey());

        // Should transparently decrypt legacy ciphertext via fallback!
        String decrypted = modernService.decrypt(legacyCiphertext);
        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("Re-encrypt migrates legacy ciphertext to primary key without corrupting valid data")
    void testReencryptionMigration() {
        TokenCryptoService legacyService = new TokenCryptoService(JWT_SECRET);
        String plaintext = "token-to-be-migrated";
        String legacyCiphertext = legacyService.encrypt(plaintext);

        TokenCryptoService modernService = new TokenCryptoService(DEDICATED_KEY, JWT_SECRET);
        String reencryptedCiphertext = modernService.reencryptIfNeeded(legacyCiphertext);

        assertNotNull(reencryptedCiphertext);
        assertNotEquals(legacyCiphertext, reencryptedCiphertext);

        // Legacy service CANNOT decrypt new ciphertext (ensures key separation!)
        assertThrows(IllegalStateException.class, () -> legacyService.decrypt(reencryptedCiphertext));

        // Modern service can decrypt new ciphertext with primary key
        String decrypted = modernService.decrypt(reencryptedCiphertext);
        assertEquals(plaintext, decrypted);

        // Running reencrypt again on already-migrated token should be idempotent
        String idempotent = modernService.reencryptIfNeeded(reencryptedCiphertext);
        assertEquals(reencryptedCiphertext, idempotent);
    }

    @Test
    @DisplayName("Null or blank input handling")
    void testNullOrBlankHandling() {
        TokenCryptoService service = new TokenCryptoService(DEDICATED_KEY, JWT_SECRET);
        assertNull(service.encrypt(null));
        assertNull(service.encrypt(""));
        assertNull(service.encrypt("   "));

        assertNull(service.decrypt(null));
        assertNull(service.decrypt(""));
        assertNull(service.decrypt("   "));

        assertNull(service.reencryptIfNeeded(null));
        assertEquals("", service.reencryptIfNeeded(""));
    }

    @Test
    @DisplayName("Rejects TOKEN_ENCRYPTION_KEY when it reuses JWT_SECRET")
    void testProhibitsReusingJwtSecretAsEncryptionKey() {
        String sharedKey = "shared-secret-that-should-never-be-reused-between-jwt-and-tokens";
        assertThrows(IllegalStateException.class, () ->
                new TokenCryptoService(sharedKey, sharedKey));
    }

    @Test
    @DisplayName("Production profile: requires TOKEN_ENCRYPTION_KEY and rejects null or blank")
    void testProductionRequiresTokenEncryptionKey() {
        org.springframework.core.env.Environment env = org.mockito.Mockito.mock(org.springframework.core.env.Environment.class);
        org.mockito.Mockito.when(env.acceptsProfiles(org.mockito.ArgumentMatchers.any(org.springframework.core.env.Profiles.class))).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
                new TokenCryptoService(null, JWT_SECRET, env));
        assertThrows(IllegalStateException.class, () ->
                new TokenCryptoService("", JWT_SECRET, env));
        assertThrows(IllegalStateException.class, () ->
                new TokenCryptoService("   ", JWT_SECRET, env));
    }

    @Test
    @DisplayName("Production profile: rejects TOKEN_ENCRYPTION_KEY shorter than 32 bytes")
    void testProductionRejectsShortKey() {
        org.springframework.core.env.Environment env = org.mockito.Mockito.mock(org.springframework.core.env.Environment.class);
        org.mockito.Mockito.when(env.acceptsProfiles(org.mockito.ArgumentMatchers.any(org.springframework.core.env.Profiles.class))).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
                new TokenCryptoService("short-key-under-32-characters!", JWT_SECRET, env));
    }
}
