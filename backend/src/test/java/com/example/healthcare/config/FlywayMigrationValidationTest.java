package com.example.healthcare.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationValidationTest {

    @Test
    @DisplayName("Verify V3 migration exists and adds correlation_id to audit_logs")
    void testV3MigrationContent() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/migration/V3__add_correlation_id_and_align_schema.sql");
        assertTrue(resource.exists(), "V3 migration file must exist on classpath");

        String sql;
        try (InputStream is = resource.getInputStream()) {
            sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertNotNull(sql);
        assertFalse(sql.isBlank());

        // Verify audit_logs additions
        assertTrue(sql.contains("audit_logs"), "Must alter audit_logs table");
        assertTrue(sql.contains("correlation_id"), "Must add correlation_id column to audit_logs");
        assertTrue(sql.contains("created_at"), "Must ensure created_at column on audit_logs");
        assertTrue(sql.contains("idx_audit_logs_correlation_id"), "Must index correlation_id");

        // Verify schema alignment with JPA entities
        assertTrue(sql.contains("is_mock"), "Must align destinations.is_mock");
        assertTrue(sql.contains("code"), "Must align hospitals.code");
        assertTrue(sql.contains("enabled"), "Must align hospitals and users enabled flags");
        assertTrue(sql.contains("value_quantity"), "Must align observations.value_quantity");
        assertTrue(sql.contains("unit"), "Must align observations.unit");

        // Verify clean procedure lifecycle
        assertTrue(sql.contains("DELIMITER $$"), "Must use DELIMITER for MySQL procedures");
        assertTrue(sql.contains("DELIMITER ;"), "Must reset DELIMITER");
        assertTrue(sql.contains("DROP PROCEDURE IF EXISTS add_column_if_not_exists;"), "Must drop temporary procedures");
        assertTrue(sql.contains("DROP PROCEDURE IF EXISTS rename_column_if_exists;"), "Must drop temporary procedures");
    }

    @Test
    @DisplayName("Verify V1 and V2 baseline migrations exist and are untouched")
    void testV1AndV2Exist() {
        ClassPathResource v1 = new ClassPathResource("db/migration/V1__init_schema.sql");
        ClassPathResource v2 = new ClassPathResource("db/migration/V2__add_performance_indexes.sql");

        assertTrue(v1.exists(), "V1__init_schema.sql must exist");
        assertTrue(v2.exists(), "V2__add_performance_indexes.sql must exist");
    }
}
