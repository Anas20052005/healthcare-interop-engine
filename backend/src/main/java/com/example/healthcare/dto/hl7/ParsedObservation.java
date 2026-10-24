package com.example.healthcare.dto.hl7;

/**
 * Structured model representing observation/result information extracted from HL7 OBX segment.
 * Preserves raw extracted values without premature clinical validation or FHIR mapping.
 */
public class ParsedObservation {

    private String valueType;
    private String observationIdentifier;
    private String observationValue;
    private String units;
    private String referenceRange;
    private String status;

    public ParsedObservation() {
    }

    public ParsedObservation(String valueType, String observationIdentifier, String observationValue, String units, String referenceRange, String status) {
        this.valueType = valueType;
        this.observationIdentifier = observationIdentifier;
        this.observationValue = observationValue;
        this.units = units;
        this.referenceRange = referenceRange;
        this.status = status;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getObservationIdentifier() {
        return observationIdentifier;
    }

    public void setObservationIdentifier(String observationIdentifier) {
        this.observationIdentifier = observationIdentifier;
    }

    public String getObservationValue() {
        return observationValue;
    }

    public void setObservationValue(String observationValue) {
        this.observationValue = observationValue;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getReferenceRange() {
        return referenceRange;
    }

    public void setReferenceRange(String referenceRange) {
        this.referenceRange = referenceRange;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ParsedObservation{" +
                "valueType='" + valueType + '\'' +
                ", observationIdentifier='" + observationIdentifier + '\'' +
                ", observationValue='" + observationValue + '\'' +
                ", units='" + units + '\'' +
                ", referenceRange='" + referenceRange + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
