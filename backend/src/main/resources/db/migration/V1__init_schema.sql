-- V1__init_schema.sql
-- Baseline schema creation for Healthcare Interoperability Engine
-- Uses IF NOT EXISTS to guarantee safe onboarding on both fresh and existing databases.

CREATE TABLE IF NOT EXISTS hospitals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    identifier VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    hospital_id BIGINT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_users_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS destinations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    fhir_version VARCHAR(20) NOT NULL DEFAULT 'R4',
    auth_type VARCHAR(30) NOT NULL,
    auth_token VARCHAR(500) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    mock BOOLEAN NOT NULL DEFAULT FALSE,
    hospital_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_destinations_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS conversions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_control_id VARCHAR(100) NULL,
    message_type VARCHAR(50) NULL,
    status VARCHAR(50) NOT NULL,
    patient_identifier VARCHAR(100) NULL,
    encounter_identifier VARCHAR(100) NULL,
    observation_count INT NULL,
    error_message TEXT NULL,
    hospital_id BIGINT NULL,
    fhir_payload LONGTEXT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_conversions_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS delivery_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversion_id BIGINT NOT NULL,
    destination_id BIGINT NOT NULL,
    destination_name VARCHAR(150) NULL,
    attempt_timestamp DATETIME NOT NULL,
    status VARCHAR(30) NOT NULL,
    http_status_code INT NULL,
    error_message TEXT NULL,
    response_summary VARCHAR(500) NULL,
    attempt_count INT NOT NULL DEFAULT 1,
    max_attempts INT NOT NULL DEFAULT 3,
    next_retry_at DATETIME NULL,
    hospital_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_delivery_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS patients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_identifier VARCHAR(100) NOT NULL,
    family_name VARCHAR(100) NULL,
    given_name VARCHAR(100) NULL,
    birth_date DATE NULL,
    gender VARCHAR(20) NULL,
    address_line VARCHAR(200) NULL,
    city VARCHAR(100) NULL,
    state VARCHAR(50) NULL,
    postal_code VARCHAR(20) NULL,
    phone_number VARCHAR(50) NULL,
    hospital_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_patients_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS encounters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    encounter_identifier VARCHAR(100) NULL,
    patient_id BIGINT NOT NULL,
    patient_class VARCHAR(50) NULL,
    location VARCHAR(100) NULL,
    admit_date_time DATETIME NULL,
    discharge_date_time DATETIME NULL,
    attending_doctor VARCHAR(150) NULL,
    hospital_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_encounters_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT fk_encounters_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS observations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    encounter_id BIGINT NULL,
    observation_code VARCHAR(100) NULL,
    value_type VARCHAR(20) NULL,
    value_numeric DOUBLE NULL,
    value_string VARCHAR(255) NULL,
    units VARCHAR(50) NULL,
    reference_range VARCHAR(100) NULL,
    abnormal_flags VARCHAR(50) NULL,
    status VARCHAR(50) NULL,
    observation_date_time DATETIME NULL,
    hospital_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_observations_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT fk_observations_encounter FOREIGN KEY (encounter_id) REFERENCES encounters (id) ON DELETE SET NULL,
    CONSTRAINT fk_observations_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NULL,
    username VARCHAR(100) NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(100) NULL,
    outcome VARCHAR(20) NOT NULL,
    details VARCHAR(500) NULL,
    hospital_id BIGINT NULL,
    timestamp DATETIME NOT NULL,
    CONSTRAINT fk_audit_logs_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
