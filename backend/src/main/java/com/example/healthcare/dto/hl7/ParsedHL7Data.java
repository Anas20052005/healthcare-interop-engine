package com.example.healthcare.dto.hl7;

import java.util.ArrayList;
import java.util.List;

/**
 * Root structured model representing data extracted from an HL7 v2 message.
 * Bridges raw HL7 parsing and downstream validation/mapping.
 */
public class ParsedHL7Data {

    private String messageType;
    private String messageControlId;
    private String version;

    private ParsedPatient patient;
    private ParsedEncounter encounter;
    private List<ParsedObservation> observations = new ArrayList<>();

    public ParsedHL7Data() {
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageControlId() {
        return messageControlId;
    }

    public void setMessageControlId(String messageControlId) {
        this.messageControlId = messageControlId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ParsedPatient getPatient() {
        return patient;
    }

    public void setPatient(ParsedPatient patient) {
        this.patient = patient;
    }

    public ParsedEncounter getEncounter() {
        return encounter;
    }

    public void setEncounter(ParsedEncounter encounter) {
        this.encounter = encounter;
    }

    public List<ParsedObservation> getObservations() {
        return observations;
    }

    public void setObservations(List<ParsedObservation> observations) {
        this.observations = (observations != null) ? observations : new ArrayList<>();
    }

    public void addObservation(ParsedObservation observation) {
        if (observation != null) {
            this.observations.add(observation);
        }
    }

    @Override
    public String toString() {
        return "ParsedHL7Data{" +
                "messageType='" + messageType + '\'' +
                ", messageControlId='" + messageControlId + '\'' +
                ", version='" + version + '\'' +
                ", patient=" + patient +
                ", encounter=" + encounter +
                ", observations=" + observations +
                '}';
    }
}
