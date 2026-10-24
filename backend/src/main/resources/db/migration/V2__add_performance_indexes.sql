-- V2__add_performance_indexes.sql
-- Composite performance indexes for query optimization, retry polling, and tenant-scoped lookups

DROP PROCEDURE IF EXISTS create_index_if_not_exists;

DELIMITER $$
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

-- Audit logs indexes (if not already created by entity)
CALL create_index_if_not_exists('audit_logs', 'idx_audit_logs_hospital_id', 'hospital_id');
CALL create_index_if_not_exists('audit_logs', 'idx_audit_logs_timestamp', 'timestamp');
CALL create_index_if_not_exists('audit_logs', 'idx_audit_logs_action', 'action');
CALL create_index_if_not_exists('audit_logs', 'idx_audit_logs_outcome', 'outcome');
CALL create_index_if_not_exists('audit_logs', 'idx_audit_logs_resource_type', 'resource_type');

-- Delivery results indexes for bounded retry polling and hospital delivery history
CALL create_index_if_not_exists('delivery_results', 'idx_deliv_status_retry', 'status, next_retry_at');
CALL create_index_if_not_exists('delivery_results', 'idx_deliv_hospital_status', 'hospital_id, status');
CALL create_index_if_not_exists('delivery_results', 'idx_deliv_hospital_attempt', 'hospital_id, attempt_timestamp');
CALL create_index_if_not_exists('delivery_results', 'idx_deliv_conversion_id', 'conversion_id');

-- Conversion history indexes
CALL create_index_if_not_exists('conversions', 'idx_conv_hosp_created', 'hospital_id, created_at');
CALL create_index_if_not_exists('conversions', 'idx_conv_hosp_status', 'hospital_id, status');
CALL create_index_if_not_exists('conversions', 'idx_conv_msg_ctrl', 'message_control_id');

-- Clinical data indexes for fast patient demographic and observation lookup
CALL create_index_if_not_exists('patients', 'idx_patient_ident_hosp', 'patient_identifier, hospital_id');
CALL create_index_if_not_exists('patients', 'idx_patient_hosp_created', 'hospital_id, created_at');

CALL create_index_if_not_exists('encounters', 'idx_enc_patient_hosp', 'patient_id, hospital_id');
CALL create_index_if_not_exists('encounters', 'idx_enc_ident_hosp', 'encounter_identifier, hospital_id');
CALL create_index_if_not_exists('encounters', 'idx_enc_hosp_created', 'hospital_id, created_at');

CALL create_index_if_not_exists('observations', 'idx_obs_patient_hosp', 'patient_id, hospital_id');
CALL create_index_if_not_exists('observations', 'idx_obs_encounter_hosp', 'encounter_id, hospital_id');
CALL create_index_if_not_exists('observations', 'idx_obs_hosp_created', 'hospital_id, created_at');

DROP PROCEDURE IF EXISTS create_index_if_not_exists;
