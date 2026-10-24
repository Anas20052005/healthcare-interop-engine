package com.example.healthcare.mapper;

import com.example.healthcare.entity.ConversionEntity;
import com.example.healthcare.entity.EncounterEntity;
import com.example.healthcare.entity.ObservationEntity;
import com.example.healthcare.entity.PatientEntity;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PersistenceMapperTest {

    private PersistenceMapper persistenceMapper;

    @BeforeEach
    void setUp() {
        persistenceMapper = new PersistenceMapper();
    }

    @Test
    @DisplayName("Should map FHIR Patient to new PatientEntity")
    void shouldMapFhirPatientToNewPatientEntity() {
        Patient fhirPatient = new Patient();
        fhirPatient.addIdentifier().setValue("P1001");
        fhirPatient.addName().setFamily("Sharma").addGiven("Rahul");
        fhirPatient.setBirthDateElement(new DateType("1995-05-10"));
        fhirPatient.setGender(Enumerations.AdministrativeGender.MALE);

        PatientEntity entity = persistenceMapper.mapToPatientEntity(fhirPatient, null);

        assertNotNull(entity);
        assertEquals("P1001", entity.getPatientIdentifier());
        assertEquals("Sharma", entity.getFamilyName());
        assertEquals("Rahul", entity.getGivenName());
        assertEquals(LocalDate.of(1995, 5, 10), entity.getBirthDate());
        assertEquals("male", entity.getGender());
    }

    @Test
    @DisplayName("Should update existing PatientEntity from FHIR Patient")
    void shouldUpdateExistingPatientEntity() {
        PatientEntity existing = new PatientEntity("P1001", "OldFamily", "OldGiven", LocalDate.of(1990, 1, 1), "female");
        existing.setId(42L);

        Patient fhirPatient = new Patient();
        fhirPatient.addIdentifier().setValue("P1001");
        fhirPatient.addName().setFamily("Sharma").addGiven("Rahul");
        fhirPatient.setBirthDateElement(new DateType("1995-05-10"));
        fhirPatient.setGender(Enumerations.AdministrativeGender.MALE);

        PatientEntity entity = persistenceMapper.mapToPatientEntity(fhirPatient, existing);

        assertEquals(42L, entity.getId());
        assertEquals("Sharma", entity.getFamilyName());
        assertEquals("Rahul", entity.getGivenName());
        assertEquals(LocalDate.of(1995, 5, 10), entity.getBirthDate());
        assertEquals("male", entity.getGender());
    }

    @Test
    @DisplayName("Should map FHIR Encounter to EncounterEntity")
    void shouldMapFhirEncounterToEncounterEntity() {
        PatientEntity patientEntity = new PatientEntity("P1001", "Sharma", "Rahul", LocalDate.of(1995, 5, 10), "male");

        Encounter fhirEncounter = new Encounter();
        fhirEncounter.setId("ENC-P1001");
        fhirEncounter.addIdentifier().setValue("ENC-P1001");
        fhirEncounter.setClass_(new org.hl7.fhir.r4.model.Coding("http://terminology.hl7.org/CodeSystem/v3-ActCode", "IMP", "inpatient"));
        fhirEncounter.addLocation().setLocation(new Reference().setDisplay("WARD-101"));

        Period period = new Period();
        period.setStart(new Date());
        fhirEncounter.setPeriod(period);

        EncounterEntity entity = persistenceMapper.mapToEncounterEntity(fhirEncounter, patientEntity);

        assertNotNull(entity);
        assertEquals("ENC-P1001", entity.getEncounterIdentifier());
        assertEquals("IMP", entity.getPatientClass());
        assertEquals("WARD-101", entity.getLocation());
        assertEquals(patientEntity, entity.getPatient());
        assertNotNull(entity.getAdmitDateTime());
    }

    @Test
    @DisplayName("Should map FHIR Observation with Quantity value to ObservationEntity")
    void shouldMapQuantityObservation() {
        PatientEntity patientEntity = new PatientEntity("P1001", "Sharma", "Rahul", LocalDate.of(1995, 5, 10), "male");
        EncounterEntity encounterEntity = new EncounterEntity("ENC-P1001", patientEntity, "IMP", "WARD-101", null, null);

        Observation obs = new Observation();
        CodeableConcept code = new CodeableConcept();
        code.addCoding().setCode("GLUCOSE").setDisplay("Blood Glucose");
        obs.setCode(code);

        Quantity qty = new Quantity();
        qty.setValue(105.0);
        qty.setUnit("mg/dL");
        obs.setValue(qty);

        obs.addReferenceRange().setText("70-110");
        obs.setStatus(Observation.ObservationStatus.FINAL);

        ObservationEntity entity = persistenceMapper.mapToObservationEntity(obs, patientEntity, encounterEntity);

        assertNotNull(entity);
        assertEquals(patientEntity, entity.getPatient());
        assertEquals(encounterEntity, entity.getEncounter());
        assertEquals("GLUCOSE", entity.getObservationCode());
        assertEquals("NM", entity.getValueType());
        assertEquals(105.0, entity.getValueQuantity());
        assertEquals("mg/dL", entity.getUnit());
        assertNull(entity.getValueString());
        assertEquals("70-110", entity.getReferenceRange());
        assertEquals("final", entity.getStatus());
    }

    @Test
    @DisplayName("Should map FHIR Observation with String value to ObservationEntity")
    void shouldMapStringObservation() {
        PatientEntity patientEntity = new PatientEntity("P1001", "Sharma", "Rahul", LocalDate.of(1995, 5, 10), "male");

        Observation obs = new Observation();
        CodeableConcept code = new CodeableConcept();
        code.addCoding().setCode("COMMENT").setDisplay("Clinical Comment");
        obs.setCode(code);

        obs.setValue(new StringType("Patient stable"));
        obs.setStatus(Observation.ObservationStatus.FINAL);

        ObservationEntity entity = persistenceMapper.mapToObservationEntity(obs, patientEntity, null);

        assertNotNull(entity);
        assertEquals("COMMENT", entity.getObservationCode());
        assertEquals("ST", entity.getValueType());
        assertEquals("Patient stable", entity.getValueString());
        assertNull(entity.getValueQuantity());
        assertNull(entity.getUnit());
        assertEquals("final", entity.getStatus());
    }

    @Test
    @DisplayName("Should create ConversionEntity correctly")
    void shouldCreateConversionEntity() {
        ConversionEntity entity = persistenceMapper.createConversionEntity(
                "MSG0001", "ADT^A01", "SUCCESS", "P1001", null
        );

        assertNotNull(entity);
        assertEquals("MSG0001", entity.getMessageControlId());
        assertEquals("ADT^A01", entity.getMessageType());
        assertEquals("SUCCESS", entity.getStatus());
        assertEquals("P1001", entity.getPatientIdentifier());
        assertNull(entity.getErrorMessage());
    }
}
