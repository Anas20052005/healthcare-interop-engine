# System Requirements Specification (SRS)

# HL7 v2 → FHIR Healthcare Interoperability Engine

## 1. Document Purpose

This document defines the detailed functional and non-functional requirements for the HL7 v2 → FHIR Healthcare Interoperability Engine.

It translates the product goals defined in `PRD.md` into implementable system requirements.

This document is intended to be used by the AI coding agent as a technical requirements reference during implementation.

The implementation must follow these requirements unless a later approved project document explicitly changes them.

---

# 2. System Objective

The system must allow an authenticated user to:

1. Enter an HL7 v2 message.
2. Submit the message to the backend.
3. Parse the HL7 message.
4. Validate the parsed data.
5. Map supported HL7 fields to FHIR R4 elements.
6. Generate FHIR R4 resources using HAPI FHIR.
7. Persist required application data.
8. Return the generated FHIR output to the frontend.
9. Display the result to the user.
10. Handle invalid input and system errors consistently.

---

# 3. Technology Requirements

## 3.1 Frontend

Required:

* React.js
* JavaScript
* HTML
* CSS
* Axios

Frontend responsibilities:

* UI rendering
* user interaction
* authentication state
* API communication
* displaying conversion results
* displaying errors
* displaying loading states

Frontend must NOT contain:

* HL7 parsing logic
* HL7 validation business rules
* HL7-to-FHIR mapping logic
* database logic
* backend authentication logic
* FHIR transformation logic

---

# 4. Backend Requirements

Required:

* Java
* Spring Boot
* Spring Web
* Spring Data JPA
* Hibernate
* Maven

Backend responsibilities:

* REST API
* authentication
* authorization
* HL7 processing
* validation
* FHIR conversion
* persistence
* exception handling

---

# 5. Healthcare Technology Requirements

Required:

* HL7 v2
* FHIR R4
* HAPI FHIR

The initial implementation must focus on the supported HL7 message types defined in `HEALTHCARE_MAPPING.md`.

The initial MVP should primarily support:

```text
ADT^A01
```

Additional message types should not be implemented unless explicitly added to the project scope.

---

# 6. Database Requirements

Database:

**MySQL**

Persistence technology:

```text
Spring Data JPA
        ↓
Hibernate
        ↓
JDBC
        ↓
MySQL
```

The database must store only the data required by the approved project design.

The application must not duplicate the same information unnecessarily across multiple tables.

Database entities must be separate from FHIR resource classes.

---

# 7. Authentication Requirements

The application must support authenticated access to protected functionality.

Required technologies:

* Spring Security
* JWT
* password hashing

---

## 7.1 Login

The system must provide a login API.

Conceptual endpoint:

```text
POST /api/auth/login
```

The login request should contain credentials.

Example:

```json
{
  "email": "user@example.com",
  "password": "password"
}
```

Successful response should provide a JWT.

Example conceptual response:

```json
{
  "token": "<JWT>",
  "tokenType": "Bearer"
}
```

The exact response DTO may be refined during implementation.

---

# 8. Authorization Requirements

Protected APIs must require a valid JWT.

Client requests should include:

```http
Authorization: Bearer <JWT>
```

Requests without a valid token must be rejected.

Expected status:

```text
401 Unauthorized
```

Authenticated but insufficiently authorized requests should return:

```text
403 Forbidden
```

Role-based authorization should only be introduced if the final requirements require multiple user roles.

Do not introduce unnecessary role complexity for the MVP.

---

# 9. HL7 Input Requirements

The frontend must allow the user to enter/paste a complete HL7 v2 message.

The backend must receive the raw message without modifying its healthcare meaning.

The system should support standard HL7 delimiters used by the supported message format.

The initial implementation should support the message structure defined in `HEALTHCARE_MAPPING.md`.

---

# 10. HL7 Conversion API

Primary endpoint:

```text
POST /api/hl7/convert
```

Authentication:

```text
Required
```

Content type:

```text
application/json
```

---

## 10.1 Request DTO

The preferred request structure is a DTO rather than passing arbitrary request parameters.

Example:

```json
{
  "hl7Message": "MSH|^~\\&|HIS|HOSPITAL|..."
}
```

Conceptual Java DTO:

```text
HL7ConvertRequest
    └── hl7Message
```

The final implementation should use validation on the request DTO.

---

# 11. Conversion API Processing

The conversion endpoint must follow this logical sequence:

```text
HTTP Request
     ↓
Controller
     ↓
Request DTO Validation
     ↓
Service
     ↓
HL7 Parser
     ↓
Parsed HL7 Model
     ↓
HL7 Validator
     ↓
HL7 → FHIR Mapper
     ↓
HAPI FHIR
     ↓
Persistence
     ↓
Response DTO
     ↓
HTTP Response
```

The controller must not directly execute parser, mapper, or repository logic.

---

# 12. Controller Requirements

The REST controller is responsible for:

* accepting HTTP requests
* validating request DTOs
* delegating to the service
* returning appropriate HTTP responses

The controller must NOT:

* parse HL7
* perform database operations
* contain FHIR mapping logic
* contain major business rules
* directly use repository classes

Example responsibility:

```text
Controller
    ↓
Service
```

---

# 13. Service Layer Requirements

The service layer must orchestrate the conversion workflow.

Conceptual flow:

```text
convert(request)
    ↓
parse
    ↓
validate
    ↓
map
    ↓
generate FHIR
    ↓
persist
    ↓
return response
```

The service should coordinate components rather than implementing every low-level operation itself.

---

# 14. HL7 Parser Requirements

A dedicated parser component must exist.

Suggested class:

```text
HL7Parser
```

Primary responsibility:

Convert:

```text
Raw HL7 String
```

into:

```text
ParsedHL7Data
```

The parser may:

* split HL7 segments
* identify segment types
* extract required fields
* normalize values into application-friendly formats

The parser must NOT:

* save to database
* call repositories
* generate HTTP responses
* authenticate users
* contain frontend logic
* perform unrelated FHIR persistence

---

# 15. Parsed HL7 Model

Parsed data should be represented using a dedicated application model/DTO where appropriate.

Example conceptual model:

```text
ParsedHL7Data
├── messageType
├── messageControlId
├── patient
│   ├── patientId
│   ├── familyName
│   ├── givenName
│   ├── birthDate
│   └── gender
├── encounter
│   └── encounter information
└── observations
    └── observation information
```

The exact fields must match the mapping defined in `HEALTHCARE_MAPPING.md`.

---

# 16. HL7 Validation Requirements

Validation must happen before FHIR mapping.

A dedicated validator component should exist.

Suggested class:

```text
HL7Validator
```

Validation must include, where applicable:

* message is not null
* message is not blank
* MSH segment exists
* supported message type exists
* required PID segment exists
* required patient identifier exists
* required patient name exists
* date fields use supported formats
* gender value is supported
* required PV1 information exists for Encounter conversion
* required OBX information exists before creating Observation resources

---

# 17. Validation Behaviour

For invalid input:

```text
Raw HL7
   ↓
Parser
   ↓
Validator
   ↓
INVALID
   ↓
Exception
   ↓
Global Exception Handler
   ↓
400 Bad Request
```

The system must not continue to FHIR mapping after a fatal validation failure.

---

# 18. HL7 Message Type Validation

The system must verify that the incoming message belongs to a supported message type.

Initial supported message:

```text
ADT^A01
```

If an unsupported message type is received, the system should return a controlled error.

Example:

```json
{
  "status": 400,
  "error": "Unsupported HL7 message type",
  "message": "Message type ADT^A08 is not supported by the current version."
}
```

The exact wording may be refined during implementation.

---

# 19. HL7 → FHIR Mapping Requirements

The mapping must be implemented separately from parsing.

Suggested component:

```text
HL7ToFhirMapper
```

Its responsibility is to transform structured parsed data into FHIR resources.

It must not be responsible for:

* raw HL7 parsing
* database access
* authentication
* HTTP handling

---

# 20. Initial FHIR Resource Requirements

The initial implementation must support:

### Patient

Patient-related HL7 data should be mapped to:

```text
FHIR Patient
```

### Encounter

Encounter-related HL7 data should be mapped to:

```text
FHIR Encounter
```

### Observation

Where supported OBX data is available:

```text
FHIR Observation
```

Exact field-level mappings are defined in:

```text
HEALTHCARE_MAPPING.md
```

---

# 21. HAPI FHIR Requirements

HAPI FHIR must be used for FHIR R4 resource handling.

The application should use HAPI FHIR for:

* FHIR resource object creation
* FHIR resource manipulation
* FHIR JSON serialization
* FHIR parsing where required
* FHIR R4 model support

The application must not depend on HAPI FHIR to decide custom business mappings.

Mapping decisions belong to:

```text
HL7ToFhirMapper
```

---

# 22. FHIR Output Requirements

Successful conversion must produce valid FHIR R4 representation.

Example conceptual Patient output:

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

The exact output must be generated using the FHIR model rather than manually constructing arbitrary JSON wherever possible.

---

# 23. FHIR Bundle Requirement

If the conversion generates multiple related FHIR resources, the system should use an appropriate FHIR representation for returning multiple resources.

For example:

```text
Patient
Encounter
Observation
```

may be returned together using a suitable FHIR Bundle when required by the final API design.

The final decision must be documented in `ARCHITECTURE.md`.

---

# 24. Persistence Requirements

Successful conversion may require persistence of:

* patient information
* encounter information
* observation information
* conversion metadata
* processing status
* timestamps
* relevant audit information

The exact entities and relationships must be defined in `ARCHITECTURE.md`.

The application must not blindly persist every field from the HL7 message.

Only required and approved information should be stored.

---

# 25. Repository Requirements

Repositories must be responsible for database access.

Example conceptual structure:

```text
PatientRepository
EncounterRepository
ObservationRepository
ConversionRepository
```

Only repositories should directly communicate with the persistence layer.

Controllers must not directly call repositories.

Parsers must not directly call repositories.

Mappers must not directly call repositories.

---

# 26. Entity Requirements

Database entities should be separate from:

* HL7 parser models
* request DTOs
* response DTOs
* HAPI FHIR resource classes

Conceptually:

```text
Request DTO
     ↓
Parsed Model
     ↓
FHIR Resource
     ↓
Persistence Entity
     ↓
Repository
     ↓
MySQL
```

The exact conversion between these models must be documented in `ARCHITECTURE.md`.

---

# 27. API Response Requirements

Successful conversion should return a structured response rather than an unstructured string.

Conceptual response:

```json
{
  "success": true,
  "message": "HL7 message converted successfully",
  "messageType": "ADT^A01",
  "resources": [
    {
      "resourceType": "Patient"
    },
    {
      "resourceType": "Encounter"
    }
  ],
  "fhirOutput": {}
}
```

The exact DTO structure may be finalized in `ARCHITECTURE.md`.

---

# 28. HTTP Status Requirements

Expected statuses:

| Situation                            |                    Status |
| ------------------------------------ | ------------------------: |
| Successful conversion                |                    200 OK |
| Successful creation where applicable |               201 Created |
| Invalid request                      |           400 Bad Request |
| Authentication missing/invalid       |          401 Unauthorized |
| Insufficient authorization           |             403 Forbidden |
| Requested resource not found         |             404 Not Found |
| Duplicate/conflicting data           |              409 Conflict |
| Unexpected server error              | 500 Internal Server Error |

The API must use status codes consistently.

---

# 29. Global Exception Handling

The backend must use centralized exception handling.

Suggested component:

```text
GlobalExceptionHandler
```

It should handle application-specific exceptions and return consistent responses.

Potential custom exceptions:

```text
InvalidHL7Exception
UnsupportedHL7MessageException
FHIRConversionException
ResourceNotFoundException
AuthenticationException
```

Only exceptions genuinely required by the application should be created.

Do not create dozens of unnecessary custom exception classes.

---

# 30. Standard Error Response

Error responses should follow a consistent structure.

Example:

```json
{
  "status": 400,
  "error": "Invalid HL7 message",
  "message": "PID segment is missing",
  "timestamp": "2026-10-07T12:00:00Z"
}
```

The response should not expose:

* stack traces
* internal class names
* database credentials
* JWT secrets
* sensitive implementation details

---

# 31. Request Validation

Spring validation should be used for request-level validation where appropriate.

Example:

```text
HL7ConvertRequest
    ↓
@NotBlank
    ↓
Controller
```

Request DTO validation should handle basic API-input validation.

HL7-specific semantic validation should remain inside the HL7 validation layer.

This distinction must be maintained.

---

# 32. Authentication Data Requirements

If user accounts are persisted, the user entity should contain only required authentication data.

Passwords must be stored using secure password hashing.

Plain-text password storage is prohibited.

JWT secrets must not be hardcoded into source code.

Sensitive configuration must be provided through environment variables or appropriate configuration mechanisms.

---

# 33. Configuration Requirements

Application configuration must support separate environments where appropriate.

Potential configuration areas:

* database URL
* database username
* database password
* JWT secret
* JWT expiration
* server configuration

Secrets must not be committed to GitHub.

The project should use environment-specific configuration mechanisms supported by Spring Boot.

---

# 34. Frontend Authentication Requirements

The frontend must:

1. Provide login UI.
2. Submit credentials to backend.
3. Receive JWT.
4. Maintain authentication state.
5. Attach JWT to protected API requests.
6. Handle expired/invalid authentication.
7. Redirect or notify the user when authentication is required.

Axios interceptors may be used where appropriate.

---

# 35. Frontend Conversion Requirements

The conversion screen must provide:

### Input

* HL7 message editor/input area
* Convert button
* Clear button

### Processing

* loading state
* disabled convert button while processing where appropriate

### Success

* FHIR output
* conversion status
* resource information

### Error

* readable error message
* ability to correct input and retry

---

# 36. Conversion History Requirements

If included in the MVP implementation, the history functionality should allow the authenticated user to view previously processed conversions.

Potential information:

* conversion ID
* message type
* processing status
* timestamp
* patient reference where appropriate
* conversion result/status

Sensitive healthcare information must not be unnecessarily exposed in history lists.

---

# 37. Healthcare Data Handling Requirements

The project must treat healthcare data as sensitive.

The application should:

* avoid unnecessary data collection
* avoid unnecessary logging of complete HL7 messages
* avoid exposing sensitive patient data in error messages
* avoid committing sample credentials/secrets
* avoid storing unnecessary PHI
* sanitize logs where appropriate

Development sample data should be synthetic/test data.

Do not use real patient data in the project repository.

---

# 38. Logging Requirements

The backend should provide useful application logs for:

* application startup
* conversion success/failure
* important processing failures
* authentication failures where appropriate
* database errors

Logs must not expose:

* passwords
* JWT secrets
* full sensitive patient records unnecessarily
* authorization tokens

---

# 39. Testing Requirements

## 39.1 Parser Tests

Test:

* valid message
* missing segment
* malformed segment
* missing patient identifier
* invalid date
* unsupported gender
* different field values

---

## 39.2 Validator Tests

Test:

* valid parsed data
* missing required fields
* invalid values
* unsupported message types
* edge cases

---

## 39.3 Mapper Tests

Test:

* Patient mapping
* Encounter mapping
* Observation mapping
* missing optional fields
* valid transformations
* date conversion
* gender conversion

---

## 39.4 Service Tests

Test:

* complete successful flow
* parser failure
* validation failure
* mapping failure
* persistence failure

Mockito should be used to isolate dependencies.

---

## 39.5 Controller Tests

MockMvc should test:

* successful request
* invalid request
* unauthorized request
* unsupported message type
* validation error
* server error

---

# 40. Frontend Testing Requirements

Where frontend testing is implemented, the project should cover:

* login behaviour
* form validation
* conversion request
* loading state
* successful response
* error response
* logout behaviour

Frontend testing framework may be selected during implementation if required.

---

# 41. Postman Requirements

Postman should be used to manually verify the backend API.

Minimum test flow:

```text
Login
 ↓
Receive JWT
 ↓
Call protected conversion API
 ↓
Send HL7 message
 ↓
Receive FHIR output
```

Negative cases should also be tested.

---

# 42. Maven Requirements

Maven must manage backend:

* dependencies
* compilation
* testing
* packaging

The `pom.xml` should contain only required dependencies.

Expected dependency categories:

```text
Spring Boot
Spring Web
Spring Data JPA
Spring Security
MySQL Driver
HAPI FHIR
JWT library
Validation
Testing
```

Exact dependency versions should be selected consistently during implementation.

---

# 43. Git Requirements

The project must use Git.

The repository should contain:

```text
agent/
frontend/
backend/
README.md
.gitignore
```

Sensitive files must be excluded through `.gitignore`.

Examples:

```text
.env
application-local.yml
secret configuration files
IDE-specific files
build output
node_modules
target
```

---

# 44. Docker Requirements

Docker is a post-core-development requirement unless required earlier for environment consistency.

Potential containerized components:

```text
Frontend
Backend
MySQL
```

Docker Compose may be introduced later.

Docker must not block the core application implementation.

---

# 45. Deployment Requirements

Deployment should be performed only after:

* frontend works
* backend works
* database works
* authentication works
* conversion works
* tests pass

Deployment platform may be selected later.

The application must support environment-specific configuration for deployment.

---

# 46. Performance Requirements

The system should process normal-sized supported HL7 messages without unnecessary overhead.

The MVP does not require:

* distributed processing
* Kafka
* Redis
* asynchronous message queues
* Kubernetes

Synchronous request-response processing is sufficient for the initial version.

---

# 47. Scalability Requirements

The architecture should allow future expansion.

Future possibilities:

```text
Additional HL7 Message Types
        ↓
Additional Mappers
        ↓
Additional FHIR Resources
```

The implementation should avoid hardcoding the entire application around only one patient example.

However, future scalability must not justify unnecessary complexity in the MVP.

---

# 48. Extensibility Requirements

The architecture should make it reasonably easy to add:

* ADT^A08
* ADT^A03
* ORM messages
* ORU messages
* additional FHIR resources

without rewriting the entire backend.

The implementation should use clear abstractions where they provide actual value.

Do not create abstraction layers only for theoretical future use.

---

# 49. Edge Cases

The application should handle:

### Input

* empty message
* null message
* whitespace-only message
* malformed HL7
* missing MSH
* missing PID
* missing PV1
* missing required fields

### Message Type

* unsupported message type
* malformed message type

### Patient

* missing patient ID
* missing name
* invalid date
* unsupported gender

### Observation

* missing observation identifier
* invalid observation value
* missing observation units where required

### Authentication

* missing JWT
* expired JWT
* invalid JWT

### Persistence

* database unavailable
* duplicate data
* transaction failure

### System

* unexpected exception
* malformed API request
* invalid JSON

---

# 50. Transaction Requirements

Operations involving multiple database writes should use appropriate transaction boundaries.

For example:

```text
Conversion
   ↓
Patient save
   ↓
Encounter save
   ↓
Observation save
```

If the operation requires atomicity, failure should not leave the database in an inconsistent state.

Spring transaction management should be used where required.

---

# 51. Data Consistency

The system must ensure that:

* a successfully reported conversion corresponds to successfully processed data
* failed conversions are not incorrectly marked as successful
* related persistence operations remain consistent
* duplicate processing behaviour is explicitly defined

The exact duplicate-handling strategy will be finalized in `ARCHITECTURE.md`.

---

# 52. API Security

The API must:

* require authentication for protected endpoints
* validate JWT
* reject invalid tokens
* avoid exposing sensitive exceptions
* validate incoming request data
* avoid storing credentials in source code

CORS should be configured to allow the deployed frontend to communicate with the backend without opening the API unnecessarily to all origins in production.

---

# 53. CORS Requirements

During local development, the frontend origin should be allowed to call the backend.

Production CORS configuration should be restricted to the actual frontend origin.

The AI agent must not use unrestricted:

```text
allowedOrigins("*")
```

as the final production configuration unless explicitly required.

---

# 54. DTO Requirements

DTOs should be used where they provide a clear API boundary.

Potential DTO categories:

```text
Auth
├── LoginRequest
└── LoginResponse

HL7
├── HL7ConvertRequest
└── HL7ConvertResponse

Error
└── ErrorResponse
```

DTOs must not unnecessarily duplicate every internal model.

---

# 55. Separation of Models

The system should distinguish:

```text
API DTO
   ↓
Parsed HL7 Model
   ↓
FHIR Resource
   ↓
Database Entity
```

These models serve different purposes.

The AI agent must not automatically reuse one class for all four responsibilities merely to reduce the number of files.

---

# 56. Naming Requirements

Use clear Java naming conventions.

Examples:

```text
HL7Controller
HL7Service
HL7Parser
HL7Validator
HL7ToFhirMapper
PatientRepository
EncounterRepository
ObservationRepository
GlobalExceptionHandler
```

Avoid unclear names such as:

```text
DataManager
CommonUtils
Helper
Processor
MainService
```

unless the class genuinely has that specific responsibility.

---

# 57. Package Organization Requirements

The final package structure should be responsibility-oriented.

A conceptual structure:

```text
com.example.healthcare
│
├── controller
├── service
├── parser
├── validator
├── mapper
├── dto
├── entity
├── repository
├── exception
├── security
├── config
└── util
```

The exact root package name can be chosen during implementation.

---

# 58. No Giant Class Requirement

The following anti-pattern is prohibited:

```text
HL7Processor.java

parse()
validate()
map()
createFHIR()
saveDatabase()
authenticate()
returnHTTP()
```

These responsibilities must be separated.

---

# 59. No Business Logic in Controller

Controllers should remain thin.

Preferred:

```text
Controller
   ↓
Service
```

Not:

```text
Controller
   ↓
Parser
   ↓
Mapper
   ↓
Repository
```

The service layer should orchestrate the business workflow.

---

# 60. No HL7 Logic in Frontend

The frontend should send the HL7 message to the backend.

It must not attempt to:

* split segments
* map PID fields
* generate FHIR resources
* validate healthcare semantics

The backend is the source of truth for conversion logic.

---

# 61. No Manual FHIR JSON Requirement

The application should prefer HAPI FHIR resource models for FHIR generation.

Avoid manually building large JSON strings such as:

```java
"{\"resourceType\":\"Patient\", ...}"
```

Instead, construct FHIR resources using HAPI FHIR and serialize them appropriately.

---

# 62. AI Coding Agent Rules

The AI coding agent must follow these rules.

### Rule 1

Read all files inside:

```text
agent/
```

before beginning implementation.

### Rule 2

Do not start coding based on assumptions if requirements are already defined in these documents.

### Rule 3

Follow `DEVELOPMENT_PLAN.md` task order.

### Rule 4

Do not skip validation and testing merely because the happy path works.

### Rule 5

Do not introduce new technologies without a clear requirement.

### Rule 6

Do not change HL7-to-FHIR mapping silently.

### Rule 7

When a mapping requirement changes, update:

```text
HEALTHCARE_MAPPING.md
```

### Rule 8

When architecture changes, update:

```text
ARCHITECTURE.md
```

### Rule 9

When product scope changes, update:

```text
PRD.md
```

### Rule 10

Keep documentation and implementation synchronized.

---

# 63. Definition of Requirement Completion

A requirement is considered implemented only when:

1. Code exists.
2. Code compiles.
3. Relevant tests exist.
4. Relevant tests pass.
5. The feature integrates correctly with dependent components.
6. Error cases are handled.
7. Documentation is consistent.
8. The feature does not violate existing architecture.

---

# 64. Requirement Priority

Requirements should be classified as:

### P0 — Critical

Required for MVP.

Examples:

* Spring Boot backend
* React frontend
* HL7 input
* HL7 parser
* validation
* HL7 → FHIR mapping
* HAPI FHIR
* Patient resource
* Encounter resource
* REST API
* database
* authentication
* core testing

### P1 — Important

Should be implemented after P0.

Examples:

* Observation
* conversion history
* richer UI
* improved error presentation
* Docker

### P2 — Future

Not required for MVP.

Examples:

* additional HL7 message types
* advanced monitoring
* complex integrations
* distributed processing
* enterprise-scale infrastructure

---

# 65. Final System Requirement

The final system must provide a complete working flow:

```text
Authenticated User
       ↓
React UI
       ↓
HL7 Input
       ↓
Axios
       ↓
Spring Boot REST Controller
       ↓
Request Validation
       ↓
Service Layer
       ↓
HL7 Parser
       ↓
Parsed HL7 Model
       ↓
HL7 Validator
       ↓
HL7 → FHIR Mapper
       ↓
HAPI FHIR
       ↓
FHIR R4 Resources
       ↓
Persistence Layer
       ↓
Repository
       ↓
MySQL
       ↓
Response DTO
       ↓
REST Response
       ↓
Axios
       ↓
React
       ↓
FHIR Output
       ↓
User
```

This end-to-end flow is the core functional requirement of the project.

Any implementation that bypasses major stages without a documented technical reason should be considered non-compliant with the requirements.
