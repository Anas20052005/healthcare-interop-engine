/**
 * Mock Data Repository for Healthcare Interoperability Engine
 * Kept isolated and structured for seamless replacement with real API calls in Milestone 11.
 */

export const SAMPLE_HL7_MESSAGE = `MSH|^~\\&|HIS|HOSPITAL|FHIR_ENGINE|HOSPITAL|202610080900||ADT^A01|MSG00001|P|2.5
PID|1||P1001||Sharma^Rahul||19950510|M
PV1|1|I|WARD1||||DOC001|||||||||||||||||||||||||||||||||||||202610080900|202610151200
OBX|1|NM|GLUCOSE||120|mg/dL|70-100||||F
OBX|2|NM|HEMOGLOBIN||13.5|g/dL|12-16||||N`;

export const SAMPLE_FHIR_BUNDLE = {
  resourceType: "Bundle",
  type: "collection",
  timestamp: "2026-10-08T09:00:00Z",
  entry: [
    {
      resource: {
        resourceType: "Patient",
        id: "P1001",
        identifier: [
          {
            system: "urn:oid:1.2.840.114350.1.13.0.1.7.1.1",
            value: "P1001"
          }
        ],
        name: [
          {
            family: "Sharma",
            given: ["Rahul"]
          }
        ],
        gender: "male",
        birthDate: "1995-05-10"
      }
    },
    {
      resource: {
        resourceType: "Encounter",
        id: "ENC-P1001-1",
        status: "in-progress",
        class: {
          system: "http://terminology.hl7.org/CodeSystem/v3-ActCode",
          code: "IMP",
          display: "inpatient encounter"
        },
        subject: {
          reference: "Patient/P1001"
        },
        period: {
          start: "2026-10-08T09:00:00",
          end: "2026-10-15T12:00:00"
        },
        location: [
          {
            location: {
              display: "WARD1"
            }
          }
        ]
      }
    },
    {
      resource: {
        resourceType: "Observation",
        id: "OBS-P1001-1",
        status: "final",
        code: {
          coding: [
            {
              system: "http://loinc.org",
              code: "GLUCOSE",
              display: "Blood Glucose"
            }
          ]
        },
        subject: {
          reference: "Patient/P1001"
        },
        encounter: {
          reference: "Encounter/ENC-P1001-1"
        },
        valueQuantity: {
          value: 120,
          unit: "mg/dL",
          system: "http://unitsofmeasure.org"
        },
        referenceRange: [
          {
            text: "70-100"
          }
        ]
      }
    },
    {
      resource: {
        resourceType: "Observation",
        id: "OBS-P1001-2",
        status: "final",
        code: {
          coding: [
            {
              system: "http://loinc.org",
              code: "HEMOGLOBIN",
              display: "Hemoglobin"
            }
          ]
        },
        subject: {
          reference: "Patient/P1001"
        },
        encounter: {
          reference: "Encounter/ENC-P1001-1"
        },
        valueQuantity: {
          value: 13.5,
          unit: "g/dL",
          system: "http://unitsofmeasure.org"
        },
        referenceRange: [
          {
            text: "12-16"
          }
        ]
      }
    }
  ]
};

