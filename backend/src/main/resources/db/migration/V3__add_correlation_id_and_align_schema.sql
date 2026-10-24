-- V3__add_correlation_id_and_align_schema.sql
-- Safely add missing columns and align table schemas with JPA entities for MySQL 8.4
-- Idempotent column additions and renames using information_schema checks

DROP PROCEDURE IF EXISTS add_column_if_not_exists;
DROP PROCEDURE IF EXISTS rename_column_if_exists;
DROP PROCEDURE IF EXISTS modify_column_if_exists;
DROP PROCEDURE IF EXISTS create_index_if_not_exists;

DELIMITER $$

CREATE PROCEDURE add_column_if_not_exists(
    IN tbl_name VARCHAR(64),
    IN col_name VARCHAR(64),
    IN col_definition VARCHAR(255)
)
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(1) INTO col_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = tbl_name
      AND column_name = col_name;

    IF col_count = 0 THEN
        SET @sql_stmt = CONCAT('ALTER TABLE `', tbl_name, '` ADD COLUMN `', col_name, '` ', col_definition);
        PREPARE stmt FROM @sql_stmt;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

CREATE PROCEDURE rename_column_if_exists(
    IN tbl_name VARCHAR(64),
    IN old_col_name VARCHAR(64),
    IN new_col_name VARCHAR(64),
    IN fallback_definition VARCHAR(255)
)
BEGIN
    DECLARE old_count INT DEFAULT 0;
    DECLARE new_count INT DEFAULT 0;

    SELECT COUNT(1) INTO old_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = tbl_name
      AND column_name = old_col_name;

    SELECT COUNT(1) INTO new_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = tbl_name
      AND column_name = new_col_name;

    -- If legacy column exists and new column does not, rename in-place to preserve all data
    IF old_count > 0 AND new_count = 0 THEN
        SET @sql_stmt = CONCAT('ALTER TABLE `', tbl_name, '` RENAME COLUMN `', old_col_name, '` TO `', new_col_name, '`');
        PREPARE stmt FROM @sql_stmt;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    -- If neither exists, add the new column using the fallback definition
    ELSEIF old_count = 0 AND new_count = 0 THEN
        SET @sql_stmt = CONCAT('ALTER TABLE `', tbl_name, '` ADD COLUMN `', new_col_name, '` ', fallback_definition);
        PREPARE stmt FROM @sql_stmt;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

CREATE PROCEDURE modify_column_if_exists(
    IN tbl_name VARCHAR(64),
    IN col_name VARCHAR(64),
    IN new_definition VARCHAR(255)
)
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(1) INTO col_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = tbl_name
      AND column_name = col_name;

    IF col_count > 0 THEN
        SET @sql_stmt = CONCAT('ALTER TABLE `', tbl_name, '` MODIFY COLUMN `', col_name, '` ', new_definition);
        PREPARE stmt FROM @sql_stmt;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

CREATE PROCEDURE create_index_if_not_exists(
    IN tbl_name VARCHAR(64),
    IN idx_name VARCHAR(64),
    IN idx_cols VARCHAR(255)
)
BEGIN
    DECLARE idx_count INT DEFAULT 0;
    SELECT COUNT(1) INTO idx_count
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = tbl_name
      AND index_name = idx_name;

    IF idx_count = 0 THEN
        SET @sql_stmt = CONCAT('CREATE INDEX `', idx_name, '` ON `', tbl_name, '` (', idx_cols, ')');
        PREPARE stmt FROM @sql_stmt;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

-- 1. audit_logs: add correlation_id, created_at, expand details column, and add correlation_id index
CALL add_column_if_not_exists('audit_logs', 'correlation_id', 'VARCHAR(100) NULL');
CALL add_column_if_not_exists('audit_logs', 'created_at', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL modify_column_if_exists('audit_logs', 'details', 'TEXT NULL');
CALL create_index_if_not_exists('audit_logs', 'idx_audit_logs_correlation_id', 'correlation_id');

-- Backfill created_at from historical timestamp if created_at was just added
UPDATE audit_logs SET created_at = timestamp WHERE timestamp IS NOT NULL AND (created_at IS NULL OR created_at > timestamp);

-- 2. destinations: ensure is_mock exists (rename from mock if present) and expand auth_token
CALL rename_column_if_exists('destinations', 'mock', 'is_mock', 'BOOLEAN NOT NULL DEFAULT FALSE');
CALL modify_column_if_exists('destinations', 'auth_token', 'VARCHAR(1000) NULL');

-- 3. hospitals: align code (from identifier), enabled (from active), and add missing fields
CALL rename_column_if_exists('hospitals', 'identifier', 'code', 'VARCHAR(50) NOT NULL');
CALL rename_column_if_exists('hospitals', 'active', 'enabled', 'BOOLEAN NOT NULL DEFAULT TRUE');
CALL add_column_if_not_exists('hospitals', 'email', 'VARCHAR(100) NULL');
CALL add_column_if_not_exists('hospitals', 'phone', 'VARCHAR(50) NULL');
CALL add_column_if_not_exists('hospitals', 'address', 'VARCHAR(255) NULL');
CALL add_column_if_not_exists('hospitals', 'updated_at', 'DATETIME NULL');

-- 4. observations: align value_quantity (from value_numeric) and unit (from units)
CALL rename_column_if_exists('observations', 'value_numeric', 'value_quantity', 'DOUBLE NULL');
CALL rename_column_if_exists('observations', 'units', 'unit', 'VARCHAR(50) NULL');

-- 5. users: align enabled (from active)
CALL rename_column_if_exists('users', 'active', 'enabled', 'BOOLEAN NOT NULL DEFAULT TRUE');

-- Clean up temporary helper procedures
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
DROP PROCEDURE IF EXISTS rename_column_if_exists;
DROP PROCEDURE IF EXISTS modify_column_if_exists;
DROP PROCEDURE IF EXISTS create_index_if_not_exists;
