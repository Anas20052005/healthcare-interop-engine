package com.example.healthcare.dto.hl7;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for HL7 v2 to FHIR R4 conversion.
 */
public class HL7ConvertRequest {

    @NotBlank(message = "HL7 message is required")
    private String hl7Message;

    public HL7ConvertRequest() {
    }

    public HL7ConvertRequest(String hl7Message) {
        this.hl7Message = hl7Message;
    }

    public String getHl7Message() {
        return hl7Message;
    }

    public void setHl7Message(String hl7Message) {
        this.hl7Message = hl7Message;
    }
}
