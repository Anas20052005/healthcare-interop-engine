package com.example.healthcare.validator;

import com.example.healthcare.dto.hl7.ParsedHL7Data;
import com.example.healthcare.exception.InvalidHL7Exception;
import com.example.healthcare.exception.UnsupportedHL7MessageException;

/**
 * Validates parsed HL7 structured data models according to healthcare integration rules.
 * Strictly separates validation from parsing, FHIR mapping, and persistence.
 */
public interface HL7Validator {

    /**
     * Validates parsed HL7 structured data.
     *
     * @param data the parsed HL7 data model to validate
     * @throws InvalidHL7Exception if any validation rule is violated
     * @throws UnsupportedHL7MessageException if the message type is not supported
     */
    void validate(ParsedHL7Data data);
}
