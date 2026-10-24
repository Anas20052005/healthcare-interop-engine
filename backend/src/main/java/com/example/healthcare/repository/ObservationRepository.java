package com.example.healthcare.repository;

import com.example.healthcare.entity.ObservationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link ObservationEntity} persistence and queries.
 */
@Repository
public interface ObservationRepository extends JpaRepository<ObservationEntity, Long> {

    List<ObservationEntity> findByPatientId(Long patientId);

    List<ObservationEntity> findByEncounterId(Long encounterId);

    Optional<ObservationEntity> findByIdAndHospitalId(Long id, Long hospitalId);

    List<ObservationEntity> findAllByHospitalIdOrderByCreatedAtDesc(Long hospitalId);

    List<ObservationEntity> findByPatientIdAndHospitalId(Long patientId, Long hospitalId);

    List<ObservationEntity> findByEncounterIdAndHospitalId(Long encounterId, Long hospitalId);

    long countByHospitalId(Long hospitalId);
}
