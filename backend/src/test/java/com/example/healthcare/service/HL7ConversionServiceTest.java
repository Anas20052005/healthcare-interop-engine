package com.example.healthcare.service;

import com.example.healthcare.dto.fhir.FhirMappingResult;
import com.example.healthcare.dto.hl7.ConversionHistoryResponse;
import com.example.healthcare.dto.hl7.HL7ConvertRequest;
import com.example.healthcare.dto.hl7.HL7ConvertResponse;
import com.example.healthcare.dto.hl7.ParsedEncounter;
import com.example.healthcare.dto.hl7.ParsedHL7Data;
import com.example.healthcare.dto.hl7.ParsedObservation;
import com.example.healthcare.dto.hl7.ParsedPatient;
import com.example.healthcare.entity.ConversionEntity;
import com.example.healthcare.entity.EncounterEntity;
import com.example.healthcare.entity.Hospital;
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
import com.example.healthcare.security.TenantSecurityService;
import com.example.healthcare.validator.HL7Validator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HL7ConversionServiceTest {

    @Mock
    private HL7Parser hl7Parser;

    @Mock
    private HL7Validator hl7Validator;

    @Mock
    private HL7ToFhirMapper hl7ToFhirMapper;

    @Mock
    private PersistenceMapper persistenceMapper;

    @Mock
    private FhirSerializationService fhirSerializationService;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private ObservationRepository observationRepository;

    @Mock
    private ConversionRepository conversionRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    @InjectMocks
    private DefaultHL7ConversionService conversionService;

    private Hospital testHospital;
    private ParsedHL7Data sampleParsedData;
    private FhirMappingResult sampleFhirResult;
    private PatientEntity samplePatientEntity;
    private EncounterEntity sampleEncounterEntity;
    private ObservationEntity sampleObservationEntity;
    private ConversionEntity sampleConversionEntity;

    @BeforeEach
    void setUp() {
        testHospital = new Hospital("General Hospital", "GENHOSP");
        testHospital.setId(1L);

        lenient().when(tenantSecurityService.requireCurrentHospital()).thenReturn(testHospital);
        lenient().when(tenantSecurityService.isPlatformAdmin()).thenReturn(false);

        ParsedPatient patient = new ParsedPatient("P1001", "Sharma", "Rahul", "19950510", "M");
        ParsedEncounter encounter = new ParsedEncounter("I", "WARD-101", "202610080900", null);
        ParsedObservation obs = new ParsedObservation("NM", "GLUCOSE^Blood Glucose", "105", "mg/dL", "70-110", "N");

        sampleParsedData = new ParsedHL7Data();
        sampleParsedData.setMessageType("ADT^A01");
        sampleParsedData.setMessageControlId("MSG0001");
        sampleParsedData.setVersion("2.5");
        sampleParsedData.setPatient(patient);
        sampleParsedData.setEncounter(encounter);
        sampleParsedData.setObservations(List.of(obs));

        Patient fhirPatient = new Patient();
        fhirPatient.setId("P1001");
        Encounter fhirEncounter = new Encounter();
        fhirEncounter.setId("ENC-P1001");
        Observation fhirObs = new Observation();
        fhirObs.setId("OBS-P1001-1");

        sampleFhirResult = new FhirMappingResult(fhirPatient, fhirEncounter, List.of(fhirObs));

        samplePatientEntity = new PatientEntity("P1001", "Sharma", "Rahul", LocalDate.of(1995, 5, 10), "male", testHospital);
        samplePatientEntity.setId(10L);

        sampleEncounterEntity = new EncounterEntity("ENC-P1001", samplePatientEntity, "IMP", "WARD-101", null, null, testHospital);
        sampleEncounterEntity.setId(20L);

        sampleObservationEntity = new ObservationEntity(samplePatientEntity, sampleEncounterEntity, "GLUCOSE", "NM", null, 105.0, "mg/dL", "70-110", "final", testHospital);
        sampleObservationEntity.setId(30L);

        sampleConversionEntity = new ConversionEntity("MSG0001", "ADT^A01", "SUCCESS", "P1001", null, testHospital);
        sampleConversionEntity.setId(100L);
    }

    @Test
    @DisplayName("Should execute complete conversion pipeline successfully with hospital scoping")
    void shouldExecuteCompleteConversionSuccessfully() {
        HL7ConvertRequest request = new HL7ConvertRequest("MSH|^~\\&|HIS|...");

        when(hl7Parser.parse(any())).thenReturn(sampleParsedData);
        when(hl7ToFhirMapper.map(sampleParsedData)).thenReturn(sampleFhirResult);
        when(fhirSerializationService.serializeToJson(any(Bundle.class))).thenReturn("{\"resourceType\":\"Bundle\"}");

        when(patientRepository.findByPatientIdentifierAndHospitalId("P1001", 1L)).thenReturn(Optional.empty());
        when(persistenceMapper.mapToPatientEntity(eq(sampleFhirResult.getPatient()), any(), eq(testHospital))).thenReturn(samplePatientEntity);
        when(patientRepository.save(samplePatientEntity)).thenReturn(samplePatientEntity);

        when(persistenceMapper.mapToEncounterEntity(eq(sampleFhirResult.getEncounter()), eq(samplePatientEntity), eq(testHospital))).thenReturn(sampleEncounterEntity);
        when(encounterRepository.save(sampleEncounterEntity)).thenReturn(sampleEncounterEntity);

        when(persistenceMapper.mapToObservationEntity(any(Observation.class), eq(samplePatientEntity), eq(sampleEncounterEntity), eq(testHospital))).thenReturn(sampleObservationEntity);
        when(observationRepository.save(sampleObservationEntity)).thenReturn(sampleObservationEntity);

        when(persistenceMapper.createConversionEntity("MSG0001", "ADT^A01", "SUCCESS", "P1001", null, testHospital)).thenReturn(sampleConversionEntity);
        when(conversionRepository.save(sampleConversionEntity)).thenReturn(sampleConversionEntity);

        HL7ConvertResponse response = conversionService.convert(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(100L, response.getConversionId());
        assertEquals("MSG0001", response.getMessageControlId());
        assertEquals("ADT^A01", response.getMessageType());
        assertEquals("P1001", response.getPatientId());
        assertEquals("ENC-P1001", response.getEncounterId());
        assertEquals(1, response.getObservationCount());
        assertNotNull(response.getFhirOutput());

        verify(hl7Parser).parse(request.getHl7Message());
        verify(hl7Validator).validate(sampleParsedData);
        verify(hl7ToFhirMapper).map(sampleParsedData);
        verify(patientRepository).save(samplePatientEntity);
        verify(encounterRepository).save(sampleEncounterEntity);
        verify(observationRepository).save(sampleObservationEntity);
        verify(conversionRepository).save(sampleConversionEntity);
    }

    @Test
    @DisplayName("Should reuse and update existing patient when patientIdentifier already exists in MySQL for that hospital")
    void shouldReuseExistingPatient() {
        HL7ConvertRequest request = new HL7ConvertRequest("MSH|^~\\&|HIS|...");

        PatientEntity existingPatient = new PatientEntity("P1001", "OldName", "Rahul", LocalDate.of(1995, 5, 10), "male", testHospital);
        existingPatient.setId(5L);

        when(hl7Parser.parse(any())).thenReturn(sampleParsedData);
        when(hl7ToFhirMapper.map(sampleParsedData)).thenReturn(sampleFhirResult);
        when(fhirSerializationService.serializeToJson(any(Bundle.class))).thenReturn("{\"resourceType\":\"Bundle\"}");

        when(patientRepository.findByPatientIdentifierAndHospitalId("P1001", 1L)).thenReturn(Optional.of(existingPatient));
        when(persistenceMapper.mapToPatientEntity(sampleFhirResult.getPatient(), existingPatient, testHospital)).thenReturn(existingPatient);
        when(patientRepository.save(existingPatient)).thenReturn(existingPatient);

        when(persistenceMapper.mapToEncounterEntity(eq(sampleFhirResult.getEncounter()), eq(existingPatient), eq(testHospital))).thenReturn(sampleEncounterEntity);
        when(encounterRepository.save(sampleEncounterEntity)).thenReturn(sampleEncounterEntity);

        when(persistenceMapper.mapToObservationEntity(any(Observation.class), eq(existingPatient), eq(sampleEncounterEntity), eq(testHospital))).thenReturn(sampleObservationEntity);
        when(observationRepository.save(sampleObservationEntity)).thenReturn(sampleObservationEntity);

        when(persistenceMapper.createConversionEntity("MSG0001", "ADT^A01", "SUCCESS", "P1001", null, testHospital)).thenReturn(sampleConversionEntity);
        when(conversionRepository.save(sampleConversionEntity)).thenReturn(sampleConversionEntity);

        HL7ConvertResponse response = conversionService.convert(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        verify(persistenceMapper).mapToPatientEntity(sampleFhirResult.getPatient(), existingPatient, testHospital);
        verify(patientRepository).save(existingPatient);
    }

    @Test
    @DisplayName("Should throw InvalidHL7Exception when request is null or hl7Message is empty")
    void shouldThrowWhenRequestIsNullOrEmpty() {
        assertThrows(InvalidHL7Exception.class, () -> conversionService.convert(null));
        assertThrows(InvalidHL7Exception.class, () -> conversionService.convert(new HL7ConvertRequest(null)));
        assertThrows(InvalidHL7Exception.class, () -> conversionService.convert(new HL7ConvertRequest("   ")));
    }

    @Test
    @DisplayName("Should propagate exception when validation fails")
    void shouldPropagateExceptionWhenValidationFails() {
        HL7ConvertRequest request = new HL7ConvertRequest("MSH|^~\\&|HIS|...");

        when(hl7Parser.parse(any())).thenReturn(sampleParsedData);
        doThrow(new InvalidHL7Exception("Missing required PID segment")).when(hl7Validator).validate(sampleParsedData);

        assertThrows(InvalidHL7Exception.class, () -> conversionService.convert(request));

        verify(hl7ToFhirMapper, never()).map(any());
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should persist multiple observations correctly")
    void shouldPersistMultipleObservationsCorrectly() {
        HL7ConvertRequest request = new HL7ConvertRequest("MSH|^~\\&|HIS|...");

        Observation obs1 = new Observation();
        obs1.setId("OBS-1");
        Observation obs2 = new Observation();
        obs2.setId("OBS-2");
        FhirMappingResult multiObsResult = new FhirMappingResult(
                sampleFhirResult.getPatient(),
                sampleFhirResult.getEncounter(),
                List.of(obs1, obs2)
        );

        when(hl7Parser.parse(any())).thenReturn(sampleParsedData);
        when(hl7ToFhirMapper.map(sampleParsedData)).thenReturn(multiObsResult);
        when(fhirSerializationService.serializeToJson(any(Bundle.class))).thenReturn("{\"resourceType\":\"Bundle\"}");

        when(patientRepository.findByPatientIdentifierAndHospitalId("P1001", 1L)).thenReturn(Optional.empty());
        when(persistenceMapper.mapToPatientEntity(eq(multiObsResult.getPatient()), any(), eq(testHospital))).thenReturn(samplePatientEntity);
        when(patientRepository.save(samplePatientEntity)).thenReturn(samplePatientEntity);

        when(persistenceMapper.mapToEncounterEntity(eq(multiObsResult.getEncounter()), eq(samplePatientEntity), eq(testHospital))).thenReturn(sampleEncounterEntity);
        when(encounterRepository.save(sampleEncounterEntity)).thenReturn(sampleEncounterEntity);

        ObservationEntity obsEntity1 = new ObservationEntity(samplePatientEntity, sampleEncounterEntity, "OBS1", "NM", null, 10.0, "mg", null, "final", testHospital);
        ObservationEntity obsEntity2 = new ObservationEntity(samplePatientEntity, sampleEncounterEntity, "OBS2", "ST", "Normal", null, null, null, "final", testHospital);

        when(persistenceMapper.mapToObservationEntity(eq(obs1), eq(samplePatientEntity), eq(sampleEncounterEntity), eq(testHospital))).thenReturn(obsEntity1);
        when(persistenceMapper.mapToObservationEntity(eq(obs2), eq(samplePatientEntity), eq(sampleEncounterEntity), eq(testHospital))).thenReturn(obsEntity2);
        when(observationRepository.save(obsEntity1)).thenReturn(obsEntity1);
        when(observationRepository.save(obsEntity2)).thenReturn(obsEntity2);

        when(persistenceMapper.createConversionEntity(any(), any(), any(), any(), any(), eq(testHospital))).thenReturn(sampleConversionEntity);
        when(conversionRepository.save(sampleConversionEntity)).thenReturn(sampleConversionEntity);

        HL7ConvertResponse response = conversionService.convert(request);

        assertNotNull(response);
        assertEquals(2, response.getObservationCount());
        verify(observationRepository).save(obsEntity1);
        verify(observationRepository).save(obsEntity2);
    }

    @Test
    @DisplayName("Should propagate exception and stop pipeline when mapping fails")
    void shouldPropagateExceptionWhenMappingFails() {
        HL7ConvertRequest request = new HL7ConvertRequest("MSH|^~\\&|HIS|...");

        when(hl7Parser.parse(any())).thenReturn(sampleParsedData);
        doThrow(new com.example.healthcare.exception.HL7MappingException("Mapping failed"))
                .when(hl7ToFhirMapper).map(sampleParsedData);

        assertThrows(com.example.healthcare.exception.HL7MappingException.class, () -> conversionService.convert(request));

        verify(patientRepository, never()).save(any());
        verify(encounterRepository, never()).save(any());
        verify(observationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should propagate exception when database persistence fails")
    void shouldPropagateExceptionWhenDatabasePersistenceFails() {
        HL7ConvertRequest request = new HL7ConvertRequest("MSH|^~\\&|HIS|...");

        when(hl7Parser.parse(any())).thenReturn(sampleParsedData);
        when(hl7ToFhirMapper.map(sampleParsedData)).thenReturn(sampleFhirResult);
        when(fhirSerializationService.serializeToJson(any(Bundle.class))).thenReturn("{\"resourceType\":\"Bundle\"}");

        when(patientRepository.findByPatientIdentifierAndHospitalId("P1001", 1L)).thenReturn(Optional.empty());
        when(persistenceMapper.mapToPatientEntity(eq(sampleFhirResult.getPatient()), any(), eq(testHospital))).thenReturn(samplePatientEntity);
        when(patientRepository.save(samplePatientEntity)).thenThrow(new org.springframework.dao.DataRetrievalFailureException("DB connection dropped"));

        assertThrows(org.springframework.dao.DataRetrievalFailureException.class, () -> conversionService.convert(request));

        verify(encounterRepository, never()).save(any());
        verify(observationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve conversion history strictly scoped to hospital")
    void shouldRetrieveConversionHistoryCorrectly() {
        ConversionEntity entity1 = new ConversionEntity("MSG001", "ADT^A01", "SUCCESS", "P1001", null, testHospital);
        entity1.setId(1L);
        entity1.setEncounterIdentifier("ENC001");
        entity1.setObservationCount(2);

        ConversionEntity entity2 = new ConversionEntity("MSG002", "ADT^A01", "FAILED", "P1002", "Validation failed", testHospital);
        entity2.setId(2L);

        when(conversionRepository.findAllByHospitalIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(entity1, entity2));

        List<ConversionHistoryResponse> history = conversionService.getConversionHistory();

        assertNotNull(history);
        assertEquals(2, history.size());

        ConversionHistoryResponse item1 = history.get(0);
        assertEquals(1L, item1.getConversionId());
        assertEquals("MSG001", item1.getMessageControlId());
        assertEquals("ADT^A01", item1.getMessageType());
        assertEquals("P1001", item1.getPatientId());
        assertEquals("ENC001", item1.getEncounterId());
        assertEquals(2, item1.getObservationCount());
        assertEquals("SUCCESS", item1.getStatus());

        ConversionHistoryResponse item2 = history.get(1);
        assertEquals(2L, item2.getConversionId());
        assertEquals("FAILED", item2.getStatus());
        assertEquals("Validation failed", item2.getErrorMessage());
        assertEquals(0, item2.getObservationCount());
    }

    @Test
    @DisplayName("Should return empty list when no conversion history exists for hospital")
    void shouldReturnEmptyHistoryWhenNoneExist() {
        when(conversionRepository.findAllByHospitalIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        List<ConversionHistoryResponse> history = conversionService.getConversionHistory();

        assertNotNull(history);
        assertTrue(history.isEmpty());
    }
}
