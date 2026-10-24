package com.example.healthcare.repository;

import com.example.healthcare.entity.EncounterEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link EncounterEntity} persistence and queries.
 */
@Repository
public interface EncounterRepository extends JpaRepository<EncounterEntity, Long> {

    List<EncounterEntity> findByPatientId(Long patientId);

    Optional<EncounterEntity> findByEncounterIdentifier(String encounterIdentifier);

    Optional<EncounterEntity> findByIdAndHospitalId(Long id, Long hospitalId);

    List<EncounterEntity> findAllByHospitalIdOrderByCreatedAtDesc(Long hospitalId);

    List<EncounterEntity> findByPatientIdAndHospitalId(Long patientId, Long hospitalId);

    long countByHospitalId(Long hospitalId);
}
