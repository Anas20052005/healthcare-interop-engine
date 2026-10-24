package com.example.healthcare.repository;

import com.example.healthcare.entity.ConversionEntity;
import com.example.healthcare.entity.EncounterEntity;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.entity.ObservationEntity;
import com.example.healthcare.entity.PatientEntity;
import com.example.healthcare.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PersistenceLayerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private ObservationRepository observationRepository;

    @Autowired
    private ConversionRepository conversionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private DeliveryResultRepository deliveryResultRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @org.junit.jupiter.api.BeforeEach
    void cleanUp() {
        auditLogRepository.deleteAll();
        deliveryResultRepository.deleteAll();
        destinationRepository.deleteAll();
        conversionRepository.deleteAll();
        observationRepository.deleteAll();
        encounterRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.findAll().stream()
                .filter(u -> !"admin".equals(u.getUsername()))
                .forEach(userRepository::delete);
        hospitalRepository.deleteAll();
    }

    @Test
    @DisplayName("Should persist and find User entity")
    void testUserPersistence() {
        User user = new User("doctor_test", "hashed_password_123", "ROLE_DOCTOR");
        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<User> found = userRepository.findByUsername("doctor_test");
        assertTrue(found.isPresent());
        assertEquals("ROLE_DOCTOR", found.get().getRole());
        assertTrue(userRepository.existsByUsername("doctor_test"));
    }

    @Test
    @DisplayName("Should persist and find PatientEntity")
    void testPatientPersistence() {
        PatientEntity patient = new PatientEntity(
                "P1001",
                "Sharma",
                "Rahul",
                LocalDate.of(1995, 5, 10),
                "male"
        );
        PatientEntity saved = patientRepository.save(patient);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<PatientEntity> found = patientRepository.findByPatientIdentifier("P1001");
        assertTrue(found.isPresent());
        assertEquals("Sharma", found.get().getFamilyName());
        assertEquals("Rahul", found.get().getGivenName());

        List<PatientEntity> byFamilyName = patientRepository.findByFamilyName("Sharma");
        assertFalse(byFamilyName.isEmpty());
    }

    @Test
    @DisplayName("Should persist EncounterEntity with relationship to PatientEntity")
    void testEncounterPersistence() {
        PatientEntity patient = new PatientEntity(
                "P1002",
                "Kumar",
                "Amit",
                LocalDate.of(1988, 3, 15),
                "male"
        );
        patient = patientRepository.save(patient);

        LocalDateTime now = LocalDateTime.now();
        EncounterEntity encounter = new EncounterEntity(
                "ENC-2001",
                patient,
                "inpatient",
                "WARD1",
                now,
                null
        );
        EncounterEntity saved = encounterRepository.save(encounter);

        assertNotNull(saved.getId());
        assertEquals("ENC-2001", saved.getEncounterIdentifier());
        assertEquals(patient.getId(), saved.getPatient().getId());

        List<EncounterEntity> encounters = encounterRepository.findByPatientId(patient.getId());
        assertEquals(1, encounters.size());
    }

    @Test
    @DisplayName("Should persist ObservationEntity with relationships to Patient and Encounter")
    void testObservationPersistence() {
        PatientEntity patient = new PatientEntity(
                "P1003",
                "Verma",
                "Priya",
                LocalDate.of(1992, 8, 22),
                "female"
        );
        patient = patientRepository.save(patient);

        EncounterEntity encounter = new EncounterEntity(
                "ENC-3001",
                patient,
                "outpatient",
                "CLINIC-A",
                LocalDateTime.now(),
                null
        );
        encounter = encounterRepository.save(encounter);

        ObservationEntity observation = new ObservationEntity(
                patient,
                encounter,
                "GLUCOSE",
                "NM",
                "95",
                95.0,
                "mg/dL",
                "70-100",
                "final"
        );
        ObservationEntity saved = observationRepository.save(observation);

        assertNotNull(saved.getId());
        assertEquals("GLUCOSE", saved.getObservationCode());
        assertEquals(95.0, saved.getValueQuantity());
        assertEquals("mg/dL", saved.getUnit());

        List<ObservationEntity> patientObs = observationRepository.findByPatientId(patient.getId());
        assertEquals(1, patientObs.size());

        List<ObservationEntity> encounterObs = observationRepository.findByEncounterId(encounter.getId());
        assertEquals(1, encounterObs.size());
    }

    @Test
    @DisplayName("Should persist ConversionEntity audit record")
    void testConversionPersistence() {
        ConversionEntity conversion = new ConversionEntity(
                "MSG-9999",
                "ADT^A01",
                "SUCCESS",
                "P1001",
                null
        );
        ConversionEntity saved = conversionRepository.save(conversion);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<ConversionEntity> found = conversionRepository.findByMessageControlId("MSG-9999");
        assertTrue(found.isPresent());
        assertEquals("SUCCESS", found.get().getStatus());
    }

    @Test
    @DisplayName("Should update existing PatientEntity and maintain audit timestamps")
    void testPatientUpdate() {
        PatientEntity patient = new PatientEntity("P-UPD-01", "InitialFamily", "Given", LocalDate.of(1990, 1, 1), "male");
        PatientEntity saved = patientRepository.save(patient);
        Long id = saved.getId();

        saved.setFamilyName("UpdatedFamily");
        PatientEntity updated = patientRepository.save(saved);

        assertEquals(id, updated.getId());
        assertEquals("UpdatedFamily", updated.getFamilyName());
        assertNotNull(updated.getCreatedAt());
    }

    @Test
    @DisplayName("Should delete ConversionEntity and verify removal")
    void testConversionDeletion() {
        ConversionEntity conversion = new ConversionEntity("MSG-DEL-01", "ADT^A01", "SUCCESS", "P1001", null);
        ConversionEntity saved = conversionRepository.save(conversion);
        assertNotNull(saved.getId());

        conversionRepository.delete(saved);
        Optional<ConversionEntity> found = conversionRepository.findByMessageControlId("MSG-DEL-01");
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should persist and find Hospital entity")
    void testHospitalPersistence() {
        Hospital hospital = new Hospital(
                "St. Jude Medical Center",
                "STJUDE",
                "admin@stjude.org",
                "555-1234",
                "777 Care Blvd",
                true
        );
        Hospital saved = hospitalRepository.save(hospital);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<Hospital> found = hospitalRepository.findByCode("STJUDE");
        assertTrue(found.isPresent());
        assertEquals("St. Jude Medical Center", found.get().getName());
        assertEquals("STJUDE", found.get().getCode());
        assertTrue(hospitalRepository.existsByCode("STJUDE"));
    }

    @Test
    @DisplayName("Should persist User with relationship to Hospital entity")
    void testUserHospitalRelationship() {
        Hospital hospital = new Hospital("Metro Health", "METRO_HLTH", "info@metro.org", null, null, true);
        Hospital savedHospital = hospitalRepository.save(hospital);

        User user = new User("metro_admin", "password123", "ADMIN", savedHospital, true);
        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getId());
        assertNotNull(savedUser.getHospital());
        assertEquals(savedHospital.getId(), savedUser.getHospital().getId());

        Optional<User> foundUser = userRepository.findByUsername("metro_admin");
        assertTrue(foundUser.isPresent());
        assertNotNull(foundUser.get().getHospital());
        assertEquals("METRO_HLTH", foundUser.get().getHospital().getCode());
    }
}
