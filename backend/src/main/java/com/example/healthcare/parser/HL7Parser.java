package com.example.healthcare.parser;

import com.example.healthcare.dto.hl7.ParsedHL7Data;
import com.example.healthcare.exception.InvalidHL7Exception;

/**
 * Interface defining the HL7 parsing contract.
 * Translates raw HL7 v2 messages into structured {@link ParsedHL7Data}.
 * Adheres strictly to the parser responsibility: extracting data without performing
 * business validation, FHIR mapping, persistence, or HTTP transport handling.
 */
public interface HL7Parser {

    /**
     * Parses a raw HL7 message string into structured application data.
     *
     * @param rawMessage the raw HL7 message string
     * @return structured {@link ParsedHL7Data}
     * @throws InvalidHL7Exception if the message is structurally invalid or unparseable
     */
    ParsedHL7Data parse(String rawMessage);
}
