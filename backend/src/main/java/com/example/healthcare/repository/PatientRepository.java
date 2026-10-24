package com.example.healthcare.repository;

import com.example.healthcare.entity.PatientEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link PatientEntity} persistence and queries.
 */
@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, Long> {

    Optional<PatientEntity> findByPatientIdentifier(String patientIdentifier);

    Optional<PatientEntity> findByPatientIdentifierAndHospitalId(String patientIdentifier, Long hospitalId);

    Optional<PatientEntity> findByIdAndHospitalId(Long id, Long hospitalId);

    List<PatientEntity> findAllByHospitalIdOrderByCreatedAtDesc(Long hospitalId);

    long countByHospitalId(Long hospitalId);

    List<PatientEntity> findByFamilyName(String familyName);
}
