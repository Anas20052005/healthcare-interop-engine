package com.example.healthcare.service;

import com.example.healthcare.dto.fhir.FhirMappingResult;
import com.example.healthcare.dto.hl7.ConversionHistoryResponse;
import com.example.healthcare.dto.hl7.HL7ConvertRequest;
import com.example.healthcare.dto.hl7.HL7ConvertResponse;
import com.example.healthcare.dto.hl7.ParsedHL7Data;
import com.example.healthcare.entity.ConversionEntity;
import com.example.healthcare.entity.EncounterEntity;
import com.example.healthcare.entity.ObservationEntity;
import com.example.healthcare.entity.PatientEntity;
import com.example.healthcare.exception.InvalidHL7Exception;
import com.example.healthcare.mapper.HL7ToFhirMapper;
import com.example.healthcare.mapper.PersistenceMapper;
import com.example.healthcare.parser.HL7Parser;
import com.example.healthcare.repository.ConversionRepository;
import com.example.healthcare.repository.EncounterRepository;
import com.example.healthcare.repository.ObservationRepository;
import com.example.healthcare.repository.PatientRepository;
import com.example.healthcare.validator.HL7Validator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrator service coordinating HL7 parsing, validation, FHIR R4 mapping,
 * database persistence, and response creation.
 */
@Service
public class DefaultHL7ConversionService implements HL7ConversionService, HL7Service {

    private static final Logger logger = LoggerFactory.getLogger(DefaultHL7ConversionService.class);

    private final HL7Parser hl7Parser;
    private final HL7Validator hl7Validator;
    private final HL7ToFhirMapper hl7ToFhirMapper;
    private final PersistenceMapper persistenceMapper;
    private final FhirSerializationService fhirSerializationService;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final ObservationRepository observationRepository;
    private final ConversionRepository conversionRepository;
    private final com.example.healthcare.security.TenantSecurityService tenantSecurityService;
    private final AuditService auditService;

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultHL7ConversionService(HL7Parser hl7Parser,
                                      HL7Validator hl7Validator,
                                      HL7ToFhirMapper hl7ToFhirMapper,
                                      PersistenceMapper persistenceMapper,
                                      FhirSerializationService fhirSerializationService,
                                      PatientRepository patientRepository,
                                      EncounterRepository encounterRepository,
                                      ObservationRepository observationRepository,
                                      ConversionRepository conversionRepository,
                                      com.example.healthcare.security.TenantSecurityService tenantSecurityService,
                                      AuditService auditService) {
        this.hl7Parser = hl7Parser;
        this.hl7Validator = hl7Validator;
        this.hl7ToFhirMapper = hl7ToFhirMapper;
        this.persistenceMapper = persistenceMapper;
        this.fhirSerializationService = fhirSerializationService;
        this.patientRepository = patientRepository;
        this.encounterRepository = encounterRepository;
        this.observationRepository = observationRepository;
        this.conversionRepository = conversionRepository;
        this.tenantSecurityService = tenantSecurityService;
        this.auditService = auditService;
    }

    public DefaultHL7ConversionService(HL7Parser hl7Parser,
                                      HL7Validator hl7Validator,
                                      HL7ToFhirMapper hl7ToFhirMapper,
                                      PersistenceMapper persistenceMapper,
                                      FhirSerializationService fhirSerializationService,
                                      PatientRepository patientRepository,
                                      EncounterRepository encounterRepository,
                                      ObservationRepository observationRepository,
                                      ConversionRepository conversionRepository,
                                      com.example.healthcare.security.TenantSecurityService tenantSecurityService) {
        this(hl7Parser, hl7Validator, hl7ToFhirMapper, persistenceMapper, fhirSerializationService,
                patientRepository, encounterRepository, observationRepository, conversionRepository,
                tenantSecurityService, null);
    }

    @Override
    @Transactional
    public HL7ConvertResponse convert(HL7ConvertRequest request) {
        if (request == null || request.getHl7Message() == null || request.getHl7Message().trim().isEmpty()) {
            throw new InvalidHL7Exception("HL7 message must not be empty or null");
        }

        // Enforce trusted hospital identity directly from database authentication
        com.example.healthcare.entity.Hospital hospital = tenantSecurityService.requireCurrentHospital();
        logger.info("HL7 conversion started for hospital '{}' (ID: {})", hospital.getName(), hospital.getId());

        // 1. Parsing
        ParsedHL7Data parsedData = hl7Parser.parse(request.getHl7Message());
        logger.info("HL7 parsed successfully. Control ID: {}, Message Type: {}",
                parsedData.getMessageControlId(), parsedData.getMessageType());

        // 2. Validation
        hl7Validator.validate(parsedData);
        logger.info("HL7 validation successful. Control ID: {}", parsedData.getMessageControlId());

        // 3. FHIR Mapping
        FhirMappingResult fhirResult = hl7ToFhirMapper.map(parsedData);
        logger.info("FHIR mapping successful. Control ID: {}", parsedData.getMessageControlId());

        // 4. Construct FHIR Bundle (Collection)
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        if (fhirResult.getPatient() != null) {
            bundle.addEntry().setResource(fhirResult.getPatient());
        }
        if (fhirResult.getEncounter() != null) {
            bundle.addEntry().setResource(fhirResult.getEncounter());
        }
        if (fhirResult.getObservations() != null) {
            for (Observation obs : fhirResult.getObservations()) {
                bundle.addEntry().setResource(obs);
            }
        }
        String serializedFhir = fhirSerializationService.serializeToJson(bundle);

        // 5. Persistence strictly isolated to authenticated hospital
        String patientIdentifier = (parsedData.getPatient() != null)
                ? parsedData.getPatient().getPatientId()
                : null;

        // Persist Patient (scoped strictly to hospital)
        PatientEntity existingPatient = null;
        if (patientIdentifier != null && !patientIdentifier.isBlank()) {
            existingPatient = patientRepository.findByPatientIdentifierAndHospitalId(patientIdentifier, hospital.getId()).orElse(null);
        }
        PatientEntity patientEntity = persistenceMapper.mapToPatientEntity(fhirResult.getPatient(), existingPatient, hospital);
        PatientEntity savedPatient = patientRepository.save(patientEntity);

        // Persist Encounter (scoped strictly to hospital)
        EncounterEntity savedEncounter = null;
        if (fhirResult.getEncounter() != null) {
            EncounterEntity encounterEntity = persistenceMapper.mapToEncounterEntity(fhirResult.getEncounter(), savedPatient, hospital);
            savedEncounter = encounterRepository.save(encounterEntity);
        }

        // Persist Observations (scoped strictly to hospital)
        List<ObservationEntity> savedObservations = new ArrayList<>();
        if (fhirResult.getObservations() != null && !fhirResult.getObservations().isEmpty()) {
            for (Observation fhirObs : fhirResult.getObservations()) {
                ObservationEntity obsEntity = persistenceMapper.mapToObservationEntity(fhirObs, savedPatient, savedEncounter, hospital);
                savedObservations.add(observationRepository.save(obsEntity));
            }
        }

        // Persist Conversion audit record with hospital association
        ConversionEntity conversionEntity = persistenceMapper.createConversionEntity(
                parsedData.getMessageControlId(),
                parsedData.getMessageType(),
                "SUCCESS",
                patientIdentifier,
                null,
                hospital
        );
        if (savedEncounter != null) {
            conversionEntity.setEncounterIdentifier(savedEncounter.getEncounterIdentifier());
        }
        conversionEntity.setObservationCount(savedObservations.size());
        conversionEntity.setFhirPayload(serializedFhir);
        ConversionEntity savedConversion = conversionRepository.save(conversionEntity);
        logger.info("Persistence successful. Conversion ID: {}, Hospital: {}", savedConversion.getId(), hospital.getName());

        if (auditService != null) {
            auditService.log(
                    "HL7_CONVERSION_SUCCESS",
                    "CONVERSION",
                    String.valueOf(savedConversion.getId()),
                    "SUCCESS",
                    "Converted HL7 message (" + parsedData.getMessageType() + ", ControlId: " + parsedData.getMessageControlId() + ", ObsCount: " + savedObservations.size() + ")",
                    hospital
            );
        }

        // 6. Build Response DTO
        HL7ConvertResponse response = new HL7ConvertResponse();
        response.setStatus("SUCCESS");
        response.setConversionId(savedConversion.getId());
        response.setMessageControlId(parsedData.getMessageControlId());
        response.setMessageType(parsedData.getMessageType());
        response.setPatientId(patientIdentifier);
        if (savedEncounter != null) {
            response.setEncounterId(savedEncounter.getEncounterIdentifier());
        }
        response.setObservationCount(savedObservations.size());
        response.setFhirOutput(serializedFhir);
        response.setTimestamp(LocalDateTime.now());

        logger.info("HL7 conversion completed. Conversion ID: {}", savedConversion.getId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversionHistoryResponse> getConversionHistory() {
        List<ConversionEntity> entities;
        if (tenantSecurityService.isPlatformAdmin()) {
            logger.info("Platform admin retrieving platform-wide conversion audit history");
            entities = conversionRepository.findAllByOrderByCreatedAtDesc();
        } else {
            com.example.healthcare.entity.Hospital hospital = tenantSecurityService.requireCurrentHospital();
            logger.info("Retrieving conversion audit history for hospital '{}' (ID: {})", hospital.getName(), hospital.getId());
            entities = conversionRepository.findAllByHospitalIdOrderByCreatedAtDesc(hospital.getId());
        }

        List<ConversionHistoryResponse> history = new ArrayList<>(entities.size());
        for (ConversionEntity entity : entities) {
            history.add(mapToHistoryResponse(entity));
        }
        return history;
    }

    @Override
    @Transactional(readOnly = true)
    public ConversionHistoryResponse getConversionById(Long id) {
        ConversionEntity entity = conversionRepository.findById(id)
                .orElseThrow(() -> new com.example.healthcare.exception.ResourceNotFoundException("Conversion record not found with id: " + id));

        // Enforce tenant authorization check
        tenantSecurityService.validateResourceHospital(entity.getHospital());

        return mapToHistoryResponse(entity);
    }

    private ConversionHistoryResponse mapToHistoryResponse(ConversionEntity entity) {
        Long hospitalId = entity.getHospital() != null ? entity.getHospital().getId() : null;
        String hospitalName = entity.getHospital() != null ? entity.getHospital().getName() : null;

        ConversionHistoryResponse response = new ConversionHistoryResponse(
                entity.getId(),
                entity.getMessageControlId(),
                entity.getMessageType(),
                entity.getPatientIdentifier(),
                entity.getEncounterIdentifier(),
                entity.getObservationCount() != null ? entity.getObservationCount() : 0,
                entity.getStatus(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                hospitalId,
                hospitalName
        );
        response.setFhirOutput(entity.getFhirPayload());
        return response;
    }
}
