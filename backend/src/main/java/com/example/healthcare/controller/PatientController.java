package com.example.healthcare.controller;

import com.example.healthcare.dto.clinical.EncounterResponse;
import com.example.healthcare.dto.clinical.ObservationResponse;
import com.example.healthcare.dto.clinical.PatientResponse;
import com.example.healthcare.entity.EncounterEntity;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.entity.ObservationEntity;
import com.example.healthcare.entity.PatientEntity;
import com.example.healthcare.exception.ResourceNotFoundException;
import com.example.healthcare.repository.EncounterRepository;
import com.example.healthcare.repository.ObservationRepository;
import com.example.healthcare.repository.PatientRepository;
import com.example.healthcare.security.TenantSecurityService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for clinical patient, encounter, and observation data.
 * Enforces strict multi-tenant hospital data isolation on all operations.
 */
@RestController
@RequestMapping("/api")
@Transactional(readOnly = true)
public class PatientController {

    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final ObservationRepository observationRepository;
    private final TenantSecurityService tenantSecurityService;

    public PatientController(PatientRepository patientRepository,
                             EncounterRepository encounterRepository,
                             ObservationRepository observationRepository,
                             TenantSecurityService tenantSecurityService) {
        this.patientRepository = patientRepository;
        this.encounterRepository = encounterRepository;
        this.observationRepository = observationRepository;
        this.tenantSecurityService = tenantSecurityService;
    }

    /**
     * Lists patients belonging strictly to the authenticated user's hospital.
     */
    @GetMapping("/patients")
    public ResponseEntity<List<PatientResponse>> getPatients() {
        Hospital hospital = tenantSecurityService.requireCurrentHospital();
        List<PatientResponse> patients = patientRepository.findAllByHospitalIdOrderByCreatedAtDesc(hospital.getId()).stream()
                .map(this::mapToPatientResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(patients);
    }

    /**
     * Retrieves patient details by ID, rejecting cross-hospital access attempts.
     */
    @GetMapping("/patients/{id}")
    public ResponseEntity<PatientResponse> getPatientById(@PathVariable Long id) {
        PatientEntity patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));

        tenantSecurityService.validateResourceHospital(patient.getHospital());
        return ResponseEntity.ok(mapToPatientResponse(patient));
    }

    /**
     * Retrieves encounters for a patient, enforcing hospital isolation.
     */
    @GetMapping("/patients/{id}/encounters")
    public ResponseEntity<List<EncounterResponse>> getPatientEncounters(@PathVariable Long id) {
        PatientEntity patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));

        tenantSecurityService.validateResourceHospital(patient.getHospital());

        List<EncounterResponse> encounters = encounterRepository
                .findByPatientIdAndHospitalId(id, patient.getHospital().getId()).stream()
                .map(this::mapToEncounterResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(encounters);
    }

    /**
     * Retrieves observations for a patient, enforcing hospital isolation.
     */
    @GetMapping("/patients/{id}/observations")
    public ResponseEntity<List<ObservationResponse>> getPatientObservations(@PathVariable Long id) {
        PatientEntity patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));

        tenantSecurityService.validateResourceHospital(patient.getHospital());

        List<ObservationResponse> observations = observationRepository
                .findByPatientIdAndHospitalId(id, patient.getHospital().getId()).stream()
                .map(this::mapToObservationResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(observations);
    }

    /**
     * Deletes a patient by ID, enforcing tenant ownership.
     */
    @DeleteMapping("/patients/{id}")
    @Transactional
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        PatientEntity patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));

        tenantSecurityService.validateResourceHospital(patient.getHospital());
        patientRepository.delete(patient);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves an encounter by ID, rejecting cross-hospital access attempts.
     */
    @GetMapping("/encounters/{id}")
    public ResponseEntity<EncounterResponse> getEncounterById(@PathVariable Long id) {
        EncounterEntity encounter = encounterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with id: " + id));

        tenantSecurityService.validateResourceHospital(encounter.getHospital());
        return ResponseEntity.ok(mapToEncounterResponse(encounter));
    }

    /**
     * Retrieves an observation by ID, rejecting cross-hospital access attempts.
     */
    @GetMapping("/observations/{id}")
    public ResponseEntity<ObservationResponse> getObservationById(@PathVariable Long id) {
        ObservationEntity observation = observationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Observation not found with id: " + id));

        tenantSecurityService.validateResourceHospital(observation.getHospital());
        return ResponseEntity.ok(mapToObservationResponse(observation));
    }

    private PatientResponse mapToPatientResponse(PatientEntity patient) {
        Long hospitalId = patient.getHospital() != null ? patient.getHospital().getId() : null;
        String hospitalName = patient.getHospital() != null ? patient.getHospital().getName() : null;

        return new PatientResponse(
                patient.getId(),
                patient.getPatientIdentifier(),
                patient.getFamilyName(),
                patient.getGivenName(),
                patient.getBirthDate(),
                patient.getGender(),
                patient.getCreatedAt(),
                hospitalId,
                hospitalName
        );
    }

    private EncounterResponse mapToEncounterResponse(EncounterEntity encounter) {
        Long hospitalId = encounter.getHospital() != null ? encounter.getHospital().getId() : null;
        String hospitalName = encounter.getHospital() != null ? encounter.getHospital().getName() : null;

        return new EncounterResponse(
                encounter.getId(),
                encounter.getEncounterIdentifier(),
                encounter.getPatient() != null ? encounter.getPatient().getId() : null,
                encounter.getPatientClass(),
                encounter.getLocation(),
                encounter.getAdmitDateTime(),
                encounter.getDischargeDateTime(),
                encounter.getCreatedAt(),
                hospitalId,
                hospitalName
        );
    }

    private ObservationResponse mapToObservationResponse(ObservationEntity observation) {
        Long hospitalId = observation.getHospital() != null ? observation.getHospital().getId() : null;
        String hospitalName = observation.getHospital() != null ? observation.getHospital().getName() : null;

        return new ObservationResponse(
                observation.getId(),
                observation.getObservationCode(),
                observation.getValueType(),
                observation.getValueString(),
                observation.getValueQuantity(),
                observation.getUnit(),
                observation.getReferenceRange(),
                observation.getStatus(),
                observation.getPatient() != null ? observation.getPatient().getId() : null,
                observation.getEncounter() != null ? observation.getEncounter().getId() : null,
                observation.getCreatedAt(),
                hospitalId,
                hospitalName
        );
    }
}
