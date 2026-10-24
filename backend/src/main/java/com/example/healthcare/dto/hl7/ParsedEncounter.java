package com.example.healthcare.dto.hl7;

/**
 * Structured model representing encounter/visit information extracted from HL7 PV1 segment.
 * Preserves raw extracted values without premature clinical validation or FHIR mapping.
 */
public class ParsedEncounter {

    private String encounterClass;
    private String location;
    private String start;
    private String end;

    public ParsedEncounter() {
    }

    public ParsedEncounter(String encounterClass, String location, String start, String end) {
        this.encounterClass = encounterClass;
        this.location = location;
        this.start = start;
        this.end = end;
    }

    public String getEncounterClass() {
        return encounterClass;
    }

    public void setEncounterClass(String encounterClass) {
        this.encounterClass = encounterClass;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "ParsedEncounter{" +
                "encounterClass='" + encounterClass + '\'' +
                ", location='" + location + '\'' +
                ", start='" + start + '\'' +
                ", end='" + end + '\'' +
                '}';
    }
}
