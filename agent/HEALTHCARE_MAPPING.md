# Healthcare Mapping Specification — HL7 v2 → FHIR R4

## 1. Document Purpose

This document defines the healthcare-specific mapping rules for the HL7 v2 → FHIR R4 Healthcare Interoperability Engine.

It is the primary source of truth for:

* HL7 v2 message structure used by the MVP.
* Supported HL7 message types.
* Supported HL7 segments.
* HL7 field extraction.
* HL7 → FHIR resource mapping.
* Field transformation rules.
* Required fields.
* Validation rules.
* FHIR resource creation rules.
* Healthcare-specific error conditions.
* MVP scope boundaries.
* Future mapping extensions.

The AI coding agent must use this document when implementing:

```text
HL7Parser
HL7Validator
HL7ToFhirMapper
```

The frontend must NOT implement any mapping rules from this document.

---

# 2. Project Healthcare Scope

The application is a healthcare interoperability engine.

The core purpose is:

```text
HL7 v2 Message
      ↓
Parse
      ↓
Validate
      ↓
Extract Structured Healthcare Data
      ↓
Map to FHIR R4
      ↓
Generate FHIR Resources
```

The initial MVP focuses on:

```text
HL7 v2 ADT^A01
        ↓
FHIR R4
```

---

# 3. Initial HL7 Message Type

The initial supported message type is:

```text
ADT^A01
```

Meaning:

```text
ADT = Admit / Discharge / Transfer
A01 = Patient admission / visit notification
```

The application should not claim to support every HL7 ADT message type.

Initial implementation should explicitly recognize the supported message type.

Example:

```text
MSH|^~\&|HOSPITAL|FACILITY|RECEIVING|FACILITY|202610071200||ADT^A01|MSG00001|P|2.5
```

The parser should extract the message type from the MSH segment.

---

# 4. HL7 Version Scope

The MVP should support the HL7 v2 message structure required by the selected sample and mapping rules.

The application should not assume that every HL7 v2 version has identical field availability or semantics.

The parser should be designed so that future HL7 version support can be extended without rewriting the entire application.

FHIR target:

```text
FHIR R4
```

---

# 5. Supported HL7 Segments

The initial MVP focuses on:

```text
MSH
PID
PV1
OBX
```

Responsibilities:

| Segment | Purpose                     | Primary FHIR Target        |
| ------- | --------------------------- | -------------------------- |
| MSH     | Message metadata            | Message/processing context |
| PID     | Patient identification      | Patient                    |
| PV1     | Visit/encounter information | Encounter                  |
| OBX     | Observation/result data     | Observation                |

Additional segments may be added later.

Examples of future segments:

```text
AL1
NK1
DG1
IN1
ORC
OBR
```

These are outside the initial mapping scope unless explicitly added.

---

# 6. Overall Mapping Architecture

The complete healthcare transformation is:

```text
                    HL7 v2 Message
                          │
                          ▼
                       MSH
                          │
                 Message Metadata
                          │
                          ▼
                       PID
                          │
                  Patient Information
                          │
                          ▼
                       PV1
                          │
                 Encounter Information
                          │
                          ▼
                       OBX
                          │
                Observation Information
                          │
                          ▼
                  ParsedHL7Data
                          │
              ┌───────────┼───────────┐
              │           │           │
              ▼           ▼           ▼
           Patient     Encounter   Observation
              │           │           │
              └───────────┼───────────┘
                          ▼
                     FHIR R4
```

---

# 7. MSH Segment

## 7.1 Purpose

MSH is the message header.

It contains information about:

* Sending application.
* Sending facility.
* Receiving application.
* Receiving facility.
* Message timestamp.
* Message type.
* Message control ID.
* Processing ID.
* HL7 version.

---

# 8. Important MSH Fields

Conceptual fields:

```text
MSH-1   Field Separator
MSH-2   Encoding Characters
MSH-3   Sending Application
MSH-4   Sending Facility
MSH-5   Receiving Application
MSH-6   Receiving Facility
MSH-7   Date/Time of Message
MSH-9   Message Type
MSH-10  Message Control ID
MSH-11  Processing ID
MSH-12  Version ID
```

The exact field indexing must respect HL7 MSH special indexing rules.

The AI agent must not blindly treat MSH fields exactly like ordinary segments without considering the MSH segment's field separator behavior.

---

# 9. MSH Mapping Responsibilities

MSH primarily provides message-level metadata.

Example:

```text
MSH-9
  ↓
Message Type
  ↓
ADT^A01
```

The parser should use MSH-9 to determine whether the message type is supported.

Example:

```text
ADT^A01
```

Supported:

```text
YES
```

Example:

```text
ORM^O01
```

MVP:

```text
NOT SUPPORTED
```

The system should return a controlled error rather than attempting an incorrect conversion.

---

# 10. Message Control ID

MSH-10 contains the message control ID.

Example:

```text
MSG00001
```

This can be extracted into:

```text
ParsedHL7Data.messageControlId
```

It may be used for:

* Tracking.
* Conversion history.
* Debugging.
* Correlation.

Do not automatically use the message control ID as the patient's identifier.

These are different concepts.

---

# 11. HL7 Version

MSH-12 contains the HL7 version.

Example:

```text
2.5
```

The application may store this as message metadata.

The parser should not silently assume every HL7 version behaves identically.

If the MVP explicitly supports a limited version range, unsupported versions should produce a controlled validation error.

---

# 12. PID Segment

## 12.1 Purpose

PID contains patient identification and demographic information.

For the MVP, PID is the primary source for the FHIR `Patient` resource.

---

# 13. Important PID Fields

Primary fields:

```text
PID-3  Patient Identifier List
PID-5  Patient Name
PID-7  Date/Time of Birth
PID-8  Administrative Sex
```

Potential future fields:

```text
PID-11 Patient Address
PID-13 Phone Number
PID-16 Marital Status
PID-22 Ethnic Group
```

Only required fields should be implemented initially.

---

# 14. PID → FHIR Patient Mapping

Core mapping:

```text
PID-3
  ↓
Patient.identifier

PID-5
  ↓
Patient.name

PID-7
  ↓
Patient.birthDate

PID-8
  ↓
Patient.gender
```

Conceptual:

```text
HL7 PID
   │
   ├── PID-3 → FHIR Patient.identifier
   │
   ├── PID-5 → FHIR Patient.name
   │
   ├── PID-7 → FHIR Patient.birthDate
   │
   └── PID-8 → FHIR Patient.gender
```

---

# 15. PID-3 — Patient Identifier

## HL7

```text
PID-3
```

contains the patient identifier information.

Example:

```text
P1001
```

FHIR mapping:

```text
Patient.identifier[].value
```

Example:

```json
{
  "identifier": [
    {
      "value": "P1001"
    }
  ]
}
```

---

# 16. Patient Identifier Semantics

The patient identifier is not necessarily a globally unique FHIR ID.

Therefore distinguish:

```text
FHIR resource ID
```

from:

```text
Patient.identifier
```

Example:

```text
Patient.id = P1001
Patient.identifier[0].value = P1001
```

may be acceptable for the MVP if the implementation explicitly chooses this strategy.

However, the application must not assume that HL7 patient identifiers are universally globally unique.

For production-grade interoperability, identifier systems/assigners should be modeled where available.

---

# 17. PID-5 — Patient Name

PID-5 contains patient name components.

Example:

```text
Sharma^Rahul
```

Interpretation:

```text
Family name = Sharma
Given name = Rahul
```

FHIR:

```text
Patient.name[0].family = Sharma
Patient.name[0].given[0] = Rahul
```

FHIR JSON:

```json
{
  "name": [
    {
      "family": "Sharma",
      "given": [
        "Rahul"
      ]
    }
  ]
}
```

---

# 18. Multiple Name Components

HL7 names may contain multiple components.

The implementation should preserve relevant name components rather than assuming every name is exactly:

```text
Family^Given
```

For MVP, support the fields required by the agreed sample format.

Future implementation may support:

```text
Family
Given
Middle
Suffix
Prefix
```

The mapper must not accidentally concatenate components into an incorrect name.

---

# 19. PID-7 — Date of Birth

PID-7 contains the patient's date/time of birth.

Example:

```text
19950510
```

FHIR:

```text
Patient.birthDate
```

FHIR format:

```text
YYYY-MM-DD
```

Transformation:

```text
19950510
     ↓
1995-05-10
```

---

# 20. Birth Date Validation

The validator should verify that the value can be interpreted as a valid supported date.

Invalid examples:

```text
19951340
```

or malformed values should produce a controlled validation error.

Do not silently convert invalid dates.

---

# 21. PID-8 — Administrative Sex

PID-8 contains the HL7 administrative sex value.

Common values include:

```text
M
F
O
U
```

FHIR uses:

```text
male
female
other
unknown
```

Mapping:

```text
M → male
F → female
O → other
U → unknown
```

The mapper must not blindly copy:

```text
"M"
```

into:

```text
Patient.gender
```

FHIR requires the appropriate FHIR value.

---

# 22. Patient Resource Example

Given:

```text
PID|1||P1001||Sharma^Rahul||19950510|M
```

Expected conceptual FHIR:

```json
{
  "resourceType": "Patient",
  "id": "P1001",
  "identifier": [
    {
      "value": "P1001"
    }
  ],
  "name": [
    {
      "family": "Sharma",
      "given": [
        "Rahul"
      ]
    }
  ],
  "birthDate": "1995-05-10",
  "gender": "male"
}
```

The actual serialized JSON should be generated using HAPI FHIR.

---

# 23. PV1 Segment

## 23.1 Purpose

PV1 contains patient visit/encounter information.

For the MVP, PV1 primarily contributes to the FHIR `Encounter` resource.

---

# 24. Important PV1 Fields

Potentially relevant fields include:

```text
PV1-1  Set ID
PV1-2  Patient Class
PV1-3  Assigned Patient Location
PV1-7  Attending Doctor
PV1-10 Hospital Service
PV1-44 Admit Date/Time
PV1-45 Discharge Date/Time
```

Only fields required by the MVP should be implemented initially.

---

# 25. PV1-2 — Patient Class

Example values:

```text
I
O
E
```

Common interpretation:

```text
I = Inpatient
O = Outpatient
E = Emergency
```

This information can contribute to the FHIR Encounter classification/type.

The exact FHIR coding strategy must remain consistent with the mapping implementation.

Do not invent arbitrary FHIR codes without documentation.

---

# 26. PV1-3 — Assigned Patient Location

PV1-3 may contain location information.

Example:

```text
WARD1
```

This can contribute to:

```text
Encounter.location
```

The MVP may store a simplified location representation if a complete FHIR Location resource is not implemented.

If a full FHIR `Location` resource is required later, that should be explicitly added to the mapping specification.

---

# 27. PV1 → FHIR Encounter

Core conceptual mapping:

```text
PV1
 ↓
Encounter
```

Potential mappings:

```text
PV1-2
 ↓
Encounter.class / encounter classification

PV1-3
 ↓
Encounter.location

PV1-44
 ↓
Encounter.period.start

PV1-45
 ↓
Encounter.period.end
```

Exact fields implemented in MVP must follow the agreed mapping scope.

---

# 28. Encounter → Patient Relationship

The generated Encounter should reference the corresponding Patient.

Conceptually:

```text
Patient
   ↑
   │ subject
   │
Encounter
```

FHIR:

```text
Encounter.subject
        ↓
Patient reference
```

The mapper must maintain the relationship between the PID patient and PV1 encounter.

---

# 29. Encounter Identifier

If the HL7 message provides a reliable encounter/visit identifier, it may be mapped to:

```text
Encounter.identifier
```

The implementation must not use an arbitrary patient identifier as an encounter identifier.

Patient and encounter identifiers represent different concepts.

---

# 30. OBX Segment

## 30.1 Purpose

OBX represents observation/result information.

Examples:

* Lab results
* Measurements
* Clinical observations

The MVP may support a limited OBX mapping sufficient for the project's demonstration.

---

# 31. Important OBX Fields

Commonly relevant fields:

```text
OBX-1  Set ID
OBX-2  Value Type
OBX-3  Observation Identifier
OBX-5  Observation Value
OBX-6  Units
OBX-7  Reference Range
OBX-8  Abnormal Flags
OBX-11 Observation Result Status
```

---

# 32. OBX → FHIR Observation

Core mapping:

```text
OBX
 ↓
Observation
```

Potential mapping:

```text
OBX-3
 ↓
Observation.code

OBX-5
 ↓
Observation.value[x]

OBX-6
 ↓
Observation.valueQuantity.unit

OBX-7
 ↓
Observation.referenceRange

OBX-11
 ↓
Observation.status
```

Exact implementation depends on the supported OBX value types.

---

# 33. OBX-2 — Value Type

OBX-2 indicates the type of value in OBX-5.

Examples:

```text
NM
ST
TX
CE
```

For example:

```text
NM
```

can indicate a numeric value.

The mapper should use OBX-2 to determine how OBX-5 should be represented in FHIR.

---

# 34. Numeric Observation

Example conceptual HL7:

```text
OBX|1|NM|GLUCOSE||95|mg/dL|70-100|N|||F
```

Possible FHIR representation:

```json
{
  "resourceType": "Observation",
  "code": {
    "text": "GLUCOSE"
  },
  "valueQuantity": {
    "value": 95,
    "unit": "mg/dL"
  },
  "referenceRange": [
    {
      "text": "70-100"
    }
  ],
  "status": "final"
}
```

The actual code representation should use HAPI FHIR classes.

---

# 35. Text Observation

If OBX-2 indicates a textual value, the mapper may use an appropriate FHIR `value[x]` representation such as:

```text
valueString
```

Example:

```text
OBX|1|ST|COMMENT||Patient stable|||||F
```

Conceptual:

```json
{
  "valueString": "Patient stable"
}
```

The exact supported value types should be explicitly implemented rather than attempting to support every possible HL7 value type in the MVP.

---

# 36. Observation Status

OBX-11 indicates result status.

Common values may include:

```text
F = Final
P = Preliminary
C = Corrected
```

FHIR Observation uses its own status values.

Therefore transformation is required.

Example conceptual mapping:

```text
F → final
P → preliminary
C → corrected
```

Unsupported or unknown statuses should be handled according to validation rules.

---

# 37. Observation → Patient Relationship

Observations should reference the patient associated with the message.

Conceptually:

```text
Patient
   ↑
   │ subject
   │
Observation
```

FHIR:

```text
Observation.subject
```

should reference the corresponding Patient resource where the mapping requires it.

---

# 38. Observation → Encounter Relationship

Where encounter context is available, the Observation may reference the corresponding Encounter.

Conceptually:

```text
Encounter
   ↑
   │ encounter
   │
Observation
```

This preserves clinical context.

The exact reference should be implemented only if the corresponding Encounter resource exists in the conversion result.

---

# 39. FHIR Resource Relationships

The expected relationship is:

```text
                  Patient
                 /       \
                /         \
               ▼           ▼
         Encounter     Observation
               \           /
                \         /
                 Context
```

More technically:

```text
Encounter.subject
        ↓
     Patient

Observation.subject
        ↓
     Patient

Observation.encounter
        ↓
    Encounter
```

---

# 40. Bundle Consideration

If the conversion produces multiple FHIR resources:

```text
Patient
Encounter
Observation
```

the API may return them together using a FHIR `Bundle`.

Conceptual:

```json
{
  "resourceType": "Bundle",
  "type": "collection",
  "entry": [
    {
      "resource": {
        "resourceType": "Patient"
      }
    },
    {
      "resource": {
        "resourceType": "Encounter"
      }
    },
    {
      "resource": {
        "resourceType": "Observation"
      }
    }
  ]
}
```

The final API response design must remain consistent with `REQUIREMENTS.md` and `ARCHITECTURE.md`.

Do not introduce Bundle merely because multiple resources exist if the approved API contract uses another response structure.

---

# 41. Mapping Pipeline

The complete mapping pipeline should be:

```text
Raw HL7 Message
       ↓
HL7 Parser
       ↓
ParsedHL7Data
       ↓
HL7 Validator
       ↓
Validated ParsedHL7Data
       ↓
HL7ToFhirMapper
       ↓
FHIR Patient
FHIR Encounter
FHIR Observation
       ↓
FHIR Response / Bundle
```

---

# 42. ParsedHL7Data Model

`ParsedHL7Data` acts as the intermediate representation.

Conceptual:

```text
ParsedHL7Data
│
├── messageType
├── messageControlId
├── hl7Version
│
├── patient
│   ├── patientId
│   ├── firstName
│   ├── lastName
│   ├── birthDate
│   └── gender
│
├── encounter
│   ├── encounterId
│   ├── patientClass
│   ├── location
│   ├── admitDateTime
│   └── dischargeDateTime
│
└── observations
    ├── code
    ├── valueType
    ├── value
    ├── unit
    ├── referenceRange
    └── status
```

The actual Java class structure can be adjusted for clean design.

---

# 43. Why ParsedHL7Data Exists

The parser should not directly depend on FHIR.

Without an intermediate model:

```text
HL7Parser
   ↓
FHIR Patient
```

This tightly couples parsing and FHIR.

Preferred:

```text
HL7Parser
   ↓
ParsedHL7Data
   ↓
FHIR Mapper
```

Benefits:

* Easier testing.
* Cleaner architecture.
* Easier future mapping.
* Easier support for additional output formats.
* Clear separation of responsibilities.

---

# 44. Required Data

For the initial Patient conversion, the minimum expected data should include:

```text
Patient ID
Patient name
Date of birth
Gender
```

Depending on the final requirements, some fields may be optional.

The validator must distinguish:

```text
Required
Optional
Unsupported
```

Do not make every HL7 field mandatory.

---

# 45. Field Transformation Rules

HL7 and FHIR do not always use the same representation.

Therefore transformation may be required.

Examples:

```text
HL7 date:
19950510

FHIR date:
1995-05-10
```

```text
HL7 gender:
M

FHIR:
male
```

```text
HL7 result status:
F

FHIR:
final
```

The mapper is responsible for these transformations.

---

# 46. Null and Missing Field Handling

When an optional HL7 field is missing:

```text
Do not invent a value.
```

For example:

```text
Missing PID-7
```

must NOT become:

```text
birthDate = today's date
```

Instead:

```text
birthDate = absent
```

or validation should fail if the field is mandatory.

---

# 47. Empty String Handling

Values such as:

```text
""
```

should generally be treated as absent rather than meaningful healthcare data.

The parser should normalize empty fields appropriately.

The validator determines whether the missing field is acceptable.

---

# 48. Identifier Handling

The application must distinguish:

```text
Patient Identifier
Encounter Identifier
Message Control ID
FHIR Resource ID
```

These are not automatically interchangeable.

Example:

```text
MSH-10
    ↓
Message Control ID

PID-3
    ↓
Patient Identifier

Encounter identifier
    ↓
Encounter Identifier
```

Do not use one identifier for all four concepts unless explicitly defined by the project requirements.

---

# 49. Date and Time Handling

HL7 date/time values may contain:

```text
YYYYMMDD
YYYYMMDDHHMM
YYYYMMDDHHMMSS
```

The implementation should support only the formats required by the MVP.

Date/time conversion must be deterministic.

Do not silently interpret ambiguous dates.

FHIR uses standardized date/dateTime representations.

---

# 50. Time Zone Handling

If an HL7 timestamp contains timezone information, the implementation should preserve the timezone information when converting to FHIR dateTime where applicable.

If the MVP does not support timezone-bearing values, this limitation must be documented rather than silently ignored.

---

# 51. Code and Terminology Handling

HL7 values may contain coded data.

The application should distinguish:

```text
Code
Display Text
Coding System
```

Where the MVP mapping requires terminology preservation, use appropriate FHIR coding structures.

Do not invent a coding system URL.

If no coding system is provided by the source message, preserve safe source text where appropriate.

---

# 52. FHIR Resource ID Rules

FHIR resources may have IDs.

The implementation should use a deterministic and documented strategy.

For example:

```text
Patient.id
```

may use the source patient identifier for the MVP.

However, this strategy must be revisited for production interoperability because source identifiers may not be globally unique.

The same rule must be applied consistently.

---

# 53. FHIR Reference Rules

FHIR resources should reference one another correctly.

Example:

```text
Encounter.subject
        ↓
Patient/{id}
```

and:

```text
Observation.subject
        ↓
Patient/{id}
```

Potentially:

```text
Observation.encounter
        ↓
Encounter/{id}
```

References must not point to non-existent resources.

---

# 54. Validation Before Mapping

Mapping should occur only after required validation succeeds.

Correct:

```text
HL7
 ↓
Parse
 ↓
Validate
 ↓
Map
```

Incorrect:

```text
HL7
 ↓
Map
 ↓
Discover invalid data
```

The validator should prevent invalid source data from reaching the mapper when possible.

---

# 55. Parser Error Examples

Possible parser errors:

```text
HL7 message is empty.
```

```text
MSH segment is missing.
```

```text
Invalid HL7 segment structure.
```

```text
Unable to determine message type.
```

The parser should throw application-specific exceptions where appropriate.

---

# 56. Validation Error Examples

Examples:

```text
Unsupported message type: ORM^O01
```

```text
PID segment is missing.
```

```text
Patient identifier is required.
```

```text
Invalid date of birth.
```

```text
Unsupported gender value.
```

```text
Invalid observation value.
```

Errors should be safe to return through the API.

---

# 57. Unsupported Message Type

If the input contains:

```text
ADT^A08
```

while the MVP only supports:

```text
ADT^A01
```

the system should NOT attempt an approximate conversion.

Expected behavior:

```text
Parse
 ↓
Detect ADT^A08
 ↓
Validator
 ↓
UnsupportedMessageTypeException
 ↓
400 Bad Request
```

or another status defined by the API contract.

---

# 58. Unsupported Segment

If an HL7 message contains additional segments that the MVP does not map:

```text
AL1
NK1
DG1
```

the application should not necessarily reject the entire message.

Preferred behavior:

```text
Supported segments
        ↓
Process

Unsupported but non-required segments
        ↓
Ignore safely
```

unless those segments are required for a specific supported mapping.

This allows realistic HL7 messages to contain more information than the MVP currently maps.

---

# 59. Required vs Optional Segments

Conceptual:

### Required

```text
MSH
PID
```

### Conditionally required

```text
PV1
```

if Encounter creation is part of the selected conversion.

### Optional

```text
OBX
```

if the message contains observation data.

The final behavior must remain consistent with the MVP conversion contract.

---

# 60. MSH Validation

Validate:

* MSH exists.
* Message type exists.
* Message type is supported.
* Message control ID is present if required.
* HL7 version is acceptable if version validation is enabled.

---

# 61. PID Validation

Validate required patient information.

Potential checks:

```text
PID exists
Patient identifier exists
Patient name exists
```

Additional fields should be validated only when required.

---

# 62. PV1 Validation

If Encounter is required:

```text
PV1 exists
Patient class is valid if provided
Encounter-specific identifiers are valid if provided
```

Do not require every PV1 field.

---

# 63. OBX Validation

If OBX exists:

Validate:

```text
Value type
Observation identifier
Observation value
Result status
```

according to the supported OBX value types.

If the MVP only supports numeric and text observations, reject unsupported value types rather than producing incorrect FHIR.

---

# 64. Data Integrity Rules

The system must not:

* Invent patient information.
* Invent clinical observations.
* Change patient identifiers without documentation.
* Convert invalid dates into valid-looking dates.
* Guess gender.
* Create fake observation values.
* Create references to missing resources.

The principle is:

```text
Preserve source meaning.
Do not invent clinical data.
```

---

# 65. Persistence Mapping

FHIR resources and JPA entities are separate concerns.

Preferred flow:

```text
FHIR Patient
      ↓
PatientEntity
      ↓
PatientRepository
      ↓
MySQL
```

Similarly:

```text
FHIR Encounter
      ↓
EncounterEntity
      ↓
EncounterRepository
      ↓
MySQL
```

and:

```text
FHIR Observation
      ↓
ObservationEntity
      ↓
ObservationRepository
      ↓
MySQL
```

The exact database schema is defined by the backend implementation.

---

# 66. Important HAPI FHIR Rule

HAPI FHIR's `Patient`, `Encounter`, and `Observation` classes are FHIR resource models.

Do not automatically treat them as JPA entities.

Do not assume:

```java
@Entity
class Patient extends HAPI Patient
```

is the correct architecture.

Use a deliberate persistence model.

---

# 67. FHIR Serialization

FHIR resource creation:

```text
ParsedHL7Data
      ↓
HAPI FHIR Patient
```

Serialization:

```text
HAPI FHIR Patient
      ↓
FhirContext
      ↓
JSON
```

Conceptual:

```java
FhirContext ctx = FhirContext.forR4();

String json =
    ctx.newJsonParser()
       .setPrettyPrint(true)
       .encodeResourceToString(patient);
```

The exact implementation may serialize a resource or Bundle depending on the API contract.

---

# 68. Example Complete Input

Example conceptual HL7 message:

```text
MSH|^~\&|HOSPITAL|FACILITY|RECEIVING|FACILITY|202610071200||ADT^A01|MSG00001|P|2.5
PID|1||P1001||Sharma^Rahul||19950510|M
PV1|1|I|WARD1
OBX|1|NM|GLUCOSE||95|mg/dL|70-100|N|||F
```

---

# 69. Expected Parsing Result

Conceptually:

```text
messageType = ADT^A01

messageControlId = MSG00001

hl7Version = 2.5

patient:
    patientId = P1001
    firstName = Rahul
    lastName = Sharma
    birthDate = 1995-05-10
    gender = M

encounter:
    patientClass = I
    location = WARD1

observation:
    code = GLUCOSE
    valueType = NM
    value = 95
    unit = mg/dL
    referenceRange = 70-100
    status = F
```

---

# 70. Expected FHIR Result

Conceptually:

```text
Patient
 ├── id
 ├── identifier
 ├── name
 ├── birthDate
 └── gender

Encounter
 ├── subject → Patient
 ├── class
 └── location

Observation
 ├── subject → Patient
 ├── encounter → Encounter
 ├── code
 ├── valueQuantity
 ├── referenceRange
 └── status
```

---

# 71. Example FHIR Patient

```json
{
  "resourceType": "Patient",
  "id": "P1001",
  "identifier": [
    {
      "value": "P1001"
    }
  ],
  "name": [
    {
      "family": "Sharma",
      "given": [
        "Rahul"
      ]
    }
  ],
  "birthDate": "1995-05-10",
  "gender": "male"
}
```

---

# 72. Example FHIR Encounter

Conceptual:

```json
{
  "resourceType": "Encounter",
  "id": "ENC-001",
  "status": "in-progress",
  "class": {
    "code": "IMP"
  },
  "subject": {
    "reference": "Patient/P1001"
  }
}
```

The exact identifier/status/class mapping must follow the implemented mapping rules.

Do not invent an encounter ID if the project does not define how it should be generated.

---

# 73. Example FHIR Observation

Conceptual:

```json
{
  "resourceType": "Observation",
  "status": "final",
  "code": {
    "text": "GLUCOSE"
  },
  "subject": {
    "reference": "Patient/P1001"
  },
  "valueQuantity": {
    "value": 95,
    "unit": "mg/dL"
  },
  "referenceRange": [
    {
      "text": "70-100"
    }
  ]
}
```

---

# 74. Mapping Table — MVP

| HL7 Segment | HL7 Field | Meaning                | FHIR Resource       | FHIR Field         |
| ----------- | --------- | ---------------------- | ------------------- | ------------------ |
| MSH         | MSH-9     | Message Type           | Processing context  | Message type       |
| MSH         | MSH-10    | Control ID             | Conversion metadata | Message control ID |
| MSH         | MSH-12    | HL7 Version            | Conversion metadata | HL7 version        |
| PID         | PID-3     | Patient Identifier     | Patient             | identifier         |
| PID         | PID-5     | Patient Name           | Patient             | name               |
| PID         | PID-7     | Date of Birth          | Patient             | birthDate          |
| PID         | PID-8     | Administrative Sex     | Patient             | gender             |
| PV1         | PV1-2     | Patient Class          | Encounter           | class              |
| PV1         | PV1-3     | Patient Location       | Encounter           | location           |
| PV1         | PV1-44    | Admit Date/Time        | Encounter           | period.start       |
| PV1         | PV1-45    | Discharge Date/Time    | Encounter           | period.end         |
| OBX         | OBX-2     | Value Type             | Observation         | value[x] selection |
| OBX         | OBX-3     | Observation Identifier | Observation         | code               |
| OBX         | OBX-5     | Observation Value      | Observation         | value[x]           |
| OBX         | OBX-6     | Units                  | Observation         | valueQuantity.unit |
| OBX         | OBX-7     | Reference Range        | Observation         | referenceRange     |
| OBX         | OBX-11    | Result Status          | Observation         | status             |

The implementation must only support rows that are explicitly included in the MVP scope.

---

# 75. Mapping Priority

When implementing the mapper, use this priority:

```text
1. Patient
2. Encounter
3. Observation
4. Additional resources
```

Patient mapping must be stable before Encounter and Observation mapping is expanded.

---

# 76. Patient Mapping Priority

Minimum:

```text
PID-3
PID-5
PID-7
PID-8
```

These should be tested independently.

---

# 77. Encounter Mapping Priority

Minimum:

```text
PV1-2
PV1-3
```

Then add:

```text
PV1-44
PV1-45
```

if required by the final MVP.

---

# 78. Observation Mapping Priority

Minimum:

```text
OBX-2
OBX-3
OBX-5
OBX-11
```

Then:

```text
OBX-6
OBX-7
```

if supported by the MVP.

---

# 79. Testing Mapping Accuracy

Each mapping rule should have unit tests.

Example:

```text
PID-3 = P1001
Expected:
Patient.identifier[0].value = P1001
```

Example:

```text
PID-5 = Sharma^Rahul
Expected:
family = Sharma
given = Rahul
```

Example:

```text
PID-7 = 19950510
Expected:
birthDate = 1995-05-10
```

Example:

```text
PID-8 = M
Expected:
gender = male
```

---

# 80. Negative Mapping Tests

Test:

```text
Missing PID-3
Invalid PID-7
Unknown PID-8
Missing OBX-5
Unsupported OBX-2
Invalid OBX status
```

Expected result:

```text
Controlled validation error
```

not:

```text
Incorrect FHIR
```

---

# 81. Mapping Must Be Deterministic

Given the same valid HL7 input:

```text
Input A
```

the system should produce the same logical FHIR result:

```text
Output A
```

The mapper should not randomly generate healthcare values.

If resource IDs are generated dynamically, that strategy must be documented and predictable where necessary.

---

# 82. No Clinical Inference

The mapper must not infer clinical meaning that is not present in the HL7 message.

Example:

If HL7 contains:

```text
OBX|...|95|mg/dL
```

do not infer:

```text
Patient is healthy.
```

If the source does not provide a diagnosis, do not create one.

---

# 83. No Data Fabrication

The application must never generate:

```text
Fake patient names
Fake birth dates
Fake medical values
Fake diagnoses
Fake medications
Fake encounter details
```

Sample/demo data is allowed only when the user explicitly chooses a provided example.

---

# 84. Sensitive Data Handling

Healthcare data may contain PHI.

The application should:

* Process only required data.
* Avoid unnecessary logs.
* Avoid printing raw HL7 messages to production logs.
* Avoid logging complete FHIR resources containing patient information.
* Avoid putting PHI into JWT claims.
* Avoid exposing PHI through error messages.
* Protect conversion history behind authentication.

---

# 85. Logging Rules

Development logging may use safe metadata such as:

```text
Conversion started
Message type: ADT^A01
Conversion successful
```

Avoid:

```text
Full HL7 message
Full Patient resource
Patient name
Patient date of birth
```

in normal production logs.

---

# 86. Error Mapping

Healthcare mapping exceptions should be converted into safe API errors.

Conceptual:

```text
UnsupportedMessageTypeException
        ↓
GlobalExceptionHandler
        ↓
400 Bad Request
```

Example:

```json
{
  "status": 400,
  "error": "Unsupported message type",
  "message": "ADT^A08 is not supported by the current MVP."
}
```

---

# 87. Transaction Considerations

The conversion and persistence workflow may use a transaction.

Conceptual:

```text
Parse
 ↓
Validate
 ↓
Map
 ↓
Persist
 ↓
Commit
```

If persistence fails:

```text
Rollback
```

The exact transaction boundary belongs to the service layer.

---

# 88. Persistence and FHIR Separation

The system should distinguish:

```text
FHIR Resource
```

from:

```text
Database Entity
```

Flow:

```text
HL7
 ↓
ParsedHL7Data
 ↓
FHIR Resource
 ↓
Persistence Mapping
 ↓
JPA Entity
 ↓
MySQL
```

The database model should not unnecessarily mirror the complete FHIR specification.

Only required application data should be persisted.

---

# 89. MVP Out of Scope

The following are outside the initial healthcare mapping scope:

```text
All HL7 v2 message types
All HL7 segments
All FHIR resources
Full terminology server integration
Complete SNOMED CT mapping
Complete LOINC terminology mapping
DICOM integration
CDA conversion
FHIR R5
FHIR R6
Real hospital EMR integration
Production-grade national health network integration
```

These may become future extensions.

---

# 90. Future HL7 Extensions

Potential future support:

```text
ADT^A08
ADT^A03
ADT^A04
ORU^R01
ORM^O01
```

Each new message type should have its own documented mapping requirements.

Do not simply enable a message type without defining its mappings.

---

# 91. Future FHIR Extensions

Potential future resources:

```text
Practitioner
Organization
MedicationRequest
Condition
DiagnosticReport
Procedure
AllergyIntolerance
Location
Coverage
```

Each must have explicit source-field mappings before implementation.

---

# 92. Extensibility Strategy

Future mapping architecture should allow:

```text
HL7 Message
      ↓
Message Type Detection
      ↓
Message-Specific Parser/Mapping Strategy
      ↓
FHIR Resources
```

For example:

```text
ADT^A01
    ↓
ADT_A01 mapping

ORU^R01
    ↓
ORU_R01 mapping
```

Do not create a giant conditional mapper containing hundreds of unrelated message types.

---

# 93. Mapping Class Responsibility

Recommended:

```text
HL7ToFhirMapper
```

coordinates mapping.

If complexity increases, specialized mappers may be introduced:

```text
PatientMapper
EncounterMapper
ObservationMapper
```

or:

```text
ADT_A01Mapper
ORU_R01Mapper
```

Only introduce additional abstraction when complexity justifies it.

---

# 94. Parser Class Responsibility

`HL7Parser` should:

* Read raw HL7.
* Identify segments.
* Extract values.
* Build `ParsedHL7Data`.

It should NOT:

* Create FHIR resources.
* Save entities.
* Call repositories.
* Return HTTP responses.

---

# 95. Validator Class Responsibility

`HL7Validator` should:

* Validate parsed data.
* Validate required fields.
* Validate supported message types.
* Validate supported value formats.
* Throw controlled validation exceptions.

It should NOT:

* Create FHIR resources.
* Save database entities.
* Handle HTTP.

---

# 96. Mapper Class Responsibility

`HL7ToFhirMapper` should:

* Receive validated structured data.
* Create FHIR R4 resources.
* Apply mapping rules.
* Perform required representation transformations.
* Establish FHIR references.

It should NOT:

* Parse raw HL7.
* Access HTTP requests.
* Save directly to repositories.

---

# 97. HAPI FHIR Responsibility

HAPI FHIR provides:

* FHIR resource classes.
* FHIR R4 models.
* FHIR parsing/serialization.
* FHIR validation capabilities where configured.
* FHIR utility functionality.

HAPI FHIR does NOT decide:

```text
PID-3 → Patient.identifier
```

That is the application's mapping responsibility.

---

# 98. Final Healthcare Transformation

The complete healthcare pipeline is:

```text
                 RAW HL7 v2
                     │
                     ▼
              ┌─────────────┐
              │ HL7 Parser  │
              └──────┬──────┘
                     │
                     ▼
              ParsedHL7Data
                     │
                     ▼
             ┌──────────────┐
             │ HL7 Validator│
             └──────┬───────┘
                    │
             Validated Data
                    │
                    ▼
          ┌─────────────────────┐
          │ HL7 → FHIR Mapper   │
          └──────────┬──────────┘
                     │
        ┌────────────┼────────────┐
        ▼            ▼            ▼
     Patient      Encounter   Observation
        │            │            │
        └────────────┼────────────┘
                     ▼
                HAPI FHIR
                     │
                     ▼
                  FHIR R4
                     │
                     ▼
             Persistence Layer
                     │
                     ▼
                   MySQL
                     │
                     ▼
                API Response
```

---

# 99. Source-of-Truth Rule

This document is the source of truth for healthcare-specific field mappings.

If implementation code conflicts with this document:

```text
Review the mapping
        ↓
Determine intended behavior
        ↓
Update documentation if necessary
        ↓
Update implementation
        ↓
Update tests
```

The AI agent must not silently change a healthcare mapping because a different implementation is easier.

---

# 100. Final AI Agent Instructions

When implementing healthcare conversion:

1. Read this entire document.
2. Read `ARCHITECTURE.md`.
3. Read `REQUIREMENTS.md`.
4. Read `DEVELOPMENT_PLAN.md`.
5. Implement only the currently approved mapping scope.
6. Keep parser, validator, and mapper separate.
7. Use HAPI FHIR for FHIR resource creation.
8. Do not manually fabricate healthcare information.
9. Do not silently guess ambiguous mappings.
10. Add tests for every important mapping.
11. Keep unsupported message types explicitly unsupported.
12. Do not add new healthcare mappings without updating this document.
13. Do not put healthcare mapping logic into React.
14. Do not expose unnecessary PHI through logs or errors.
15. Preserve source meaning as accurately as possible.

The central principle is:

```text
HL7 contains source healthcare data.

Parser extracts it.

Validator verifies it.

Mapper transforms it.

HAPI FHIR represents it as FHIR R4.

Persistence stores only the application data that is actually required.

The system must never invent clinical information.
```

---

# 101. Final MVP Mapping Summary

```text
HL7 v2 ADT^A01
       │
       ├── MSH
       │    ├── Message Type
       │    ├── Control ID
       │    └── HL7 Version
       │
       ├── PID
       │    ├── PID-3 → Patient.identifier
       │    ├── PID-5 → Patient.name
       │    ├── PID-7 → Patient.birthDate
       │    └── PID-8 → Patient.gender
       │
       ├── PV1
       │    ├── PV1-2 → Encounter classification
       │    ├── PV1-3 → Encounter location
       │    ├── PV1-44 → Encounter.period.start
       │    └── PV1-45 → Encounter.period.end
       │
       └── OBX
            ├── OBX-2 → Observation value type
            ├── OBX-3 → Observation.code
            ├── OBX-5 → Observation.value[x]
            ├── OBX-6 → Observation.valueQuantity.unit
            ├── OBX-7 → Observation.referenceRange
            └── OBX-11 → Observation.status
```

The resulting FHIR model is:

```text
FHIR R4
│
├── Patient
│    ├── identifier
│    ├── name
│    ├── birthDate
│    └── gender
│
├── Encounter
│    ├── subject → Patient
│    ├── class
│    ├── location
│    └── period
│
└── Observation
     ├── subject → Patient
     ├── encounter → Encounter
     ├── code
     ├── value[x]
     ├── referenceRange
     └── status
```

This mapping represents the initial healthcare interoperability capability of the project.
