package com.example.healthcare.mapper;

import com.example.healthcare.entity.ConversionEntity;
import com.example.healthcare.entity.EncounterEntity;
import com.example.healthcare.entity.ObservationEntity;
import com.example.healthcare.entity.PatientEntity;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Maps HAPI FHIR R4 resources into JPA persistence entities.
 * Enforces strict separation between FHIR domain models and database entities.
 */
@Component
public class PersistenceMapper {

    /**
     * Maps a FHIR Patient resource to a PatientEntity.
     * If an existing entity is provided, its demographic fields are updated.
     *
     * @param fhirPatient FHIR Patient resource
     * @param existing    Existing PatientEntity or null for new creation
     * @return populated PatientEntity
     */
    public PatientEntity mapToPatientEntity(Patient fhirPatient, PatientEntity existing) {
        return mapToPatientEntity(fhirPatient, existing, null);
    }

    public PatientEntity mapToPatientEntity(Patient fhirPatient, PatientEntity existing, com.example.healthcare.entity.Hospital hospital) {
        if (fhirPatient == null) {
            return null;
        }

        PatientEntity patient = existing != null ? existing : new PatientEntity();
        if (hospital != null) {
            patient.setHospital(hospital);
        }

        if (fhirPatient.hasIdentifier()) {
            patient.setPatientIdentifier(fhirPatient.getIdentifierFirstRep().getValue());
        }

        if (fhirPatient.hasName()) {
            patient.setFamilyName(fhirPatient.getNameFirstRep().getFamily());
            patient.setGivenName(fhirPatient.getNameFirstRep().getGivenAsSingleString());
        }

        if (fhirPatient.hasBirthDate()) {
            LocalDate birthDate = fhirPatient.getBirthDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            patient.setBirthDate(birthDate);
        }

        if (fhirPatient.hasGender()) {
            patient.setGender(fhirPatient.getGender().toCode());
        }

        return patient;
    }

    /**
     * Maps a FHIR Encounter resource to an EncounterEntity associated with the patient.
     *
     * @param fhirEncounter FHIR Encounter resource
     * @param patient       Persisted patient entity
     * @return populated EncounterEntity
     */
    public EncounterEntity mapToEncounterEntity(Encounter fhirEncounter, PatientEntity patient) {
        return mapToEncounterEntity(fhirEncounter, patient, null);
    }

    public EncounterEntity mapToEncounterEntity(Encounter fhirEncounter, PatientEntity patient, com.example.healthcare.entity.Hospital hospital) {
        if (fhirEncounter == null) {
            return null;
        }

        EncounterEntity encounter = new EncounterEntity();
        encounter.setPatient(patient);
        if (hospital != null) {
            encounter.setHospital(hospital);
        } else if (patient != null && patient.getHospital() != null) {
            encounter.setHospital(patient.getHospital());
        }

        if (fhirEncounter.hasIdentifier()) {
            encounter.setEncounterIdentifier(fhirEncounter.getIdentifierFirstRep().getValue());
        } else if (fhirEncounter.hasIdElement() && fhirEncounter.getIdElement().hasIdPart()) {
            encounter.setEncounterIdentifier(fhirEncounter.getIdElement().getIdPart());
        }

        if (fhirEncounter.hasClass_()) {
            encounter.setPatientClass(fhirEncounter.getClass_().getCode());
        }

        if (fhirEncounter.hasLocation() && fhirEncounter.getLocationFirstRep().hasLocation()) {
            encounter.setLocation(fhirEncounter.getLocationFirstRep().getLocation().getDisplay());
        }

        if (fhirEncounter.hasPeriod()) {
            if (fhirEncounter.getPeriod().hasStart()) {
                encounter.setAdmitDateTime(
                        LocalDateTime.ofInstant(fhirEncounter.getPeriod().getStart().toInstant(), ZoneId.systemDefault())
                );
            }
            if (fhirEncounter.getPeriod().hasEnd()) {
                encounter.setDischargeDateTime(
                        LocalDateTime.ofInstant(fhirEncounter.getPeriod().getEnd().toInstant(), ZoneId.systemDefault())
                );
            }
        }

        return encounter;
    }

    /**
     * Maps a FHIR Observation resource to an ObservationEntity associated with the patient and optional encounter.
     *
     * @param fhirObservation FHIR Observation resource
     * @param patient         Persisted patient entity
     * @param encounter       Persisted encounter entity or null
     * @return populated ObservationEntity
     */
    public ObservationEntity mapToObservationEntity(Observation fhirObservation, PatientEntity patient, EncounterEntity encounter) {
        return mapToObservationEntity(fhirObservation, patient, encounter, null);
    }

    public ObservationEntity mapToObservationEntity(Observation fhirObservation, PatientEntity patient, EncounterEntity encounter, com.example.healthcare.entity.Hospital hospital) {
        if (fhirObservation == null) {
            return null;
        }

        ObservationEntity observation = new ObservationEntity();
        observation.setPatient(patient);
        observation.setEncounter(encounter);
        if (hospital != null) {
            observation.setHospital(hospital);
        } else if (patient != null && patient.getHospital() != null) {
            observation.setHospital(patient.getHospital());
        }

        if (fhirObservation.hasCode() && !fhirObservation.getCode().getCoding().isEmpty()) {
            observation.setObservationCode(fhirObservation.getCode().getCodingFirstRep().getCode());
        }

        if (fhirObservation.hasValueQuantity()) {
            observation.setValueType("NM");
            if (fhirObservation.getValueQuantity().getValue() != null) {
                observation.setValueQuantity(fhirObservation.getValueQuantity().getValue().doubleValue());
            }
            observation.setUnit(fhirObservation.getValueQuantity().getUnit());
        } else if (fhirObservation.hasValueStringType()) {
            observation.setValueType("ST");
            observation.setValueString(fhirObservation.getValueStringType().getValue());
        }

        if (fhirObservation.hasReferenceRange() && !fhirObservation.getReferenceRange().isEmpty()) {
            observation.setReferenceRange(fhirObservation.getReferenceRangeFirstRep().getText());
        }

        if (fhirObservation.hasStatus()) {
            observation.setStatus(fhirObservation.getStatus().toCode());
        }

        return observation;
    }

    /**
     * Creates an audit ConversionEntity tracking the outcome of an HL7 conversion.
     *
     * @param messageControlId HL7 message control ID
     * @param messageType      HL7 message type (e.g. ADT^A01)
     * @param status           Status of conversion (e.g. SUCCESS, FAILED)
     * @param patientId        Patient identifier
     * @param errorMessage     Error message if conversion failed, or null
     * @return populated ConversionEntity
     */
    public ConversionEntity createConversionEntity(String messageControlId, String messageType,
                                                  String status, String patientId, String errorMessage) {
        return new ConversionEntity(messageControlId, messageType, status, patientId, errorMessage);
    }

    public ConversionEntity createConversionEntity(String messageControlId, String messageType,
                                                  String status, String patientId, String errorMessage,
                                                  com.example.healthcare.entity.Hospital hospital) {
        return new ConversionEntity(messageControlId, messageType, status, patientId, errorMessage, hospital);
    }
}
