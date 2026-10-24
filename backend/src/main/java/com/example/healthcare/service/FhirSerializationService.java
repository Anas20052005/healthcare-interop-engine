package com.example.healthcare.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Service;

/**
 * Service responsible for serializing HAPI FHIR R4 resources to valid FHIR JSON representations.
 */
@Service
public class FhirSerializationService {

    private final FhirContext fhirContext;

    public FhirSerializationService(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    /**
     * Serializes a FHIR resource to a pretty-printed JSON string.
     *
     * @param resource the FHIR resource to serialize
     * @return serialized JSON string
     */
    public String serializeToJson(IBaseResource resource) {
        return serializeToJson(resource, true);
    }

    /**
     * Serializes a FHIR resource to JSON with optional pretty-printing.
     *
     * @param resource the FHIR resource to serialize
     * @param prettyPrint whether to format JSON with indentation
     * @return serialized JSON string
     */
    public String serializeToJson(IBaseResource resource, boolean prettyPrint) {
        if (resource == null) {
            return null;
        }
        IParser parser = fhirContext.newJsonParser().setPrettyPrint(prettyPrint);
        return parser.encodeResourceToString(resource);
    }

    public FhirContext getFhirContext() {
        return fhirContext;
    }
}
