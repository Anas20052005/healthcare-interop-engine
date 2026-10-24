# System Architecture Document

# HL7 v2 → FHIR Healthcare Interoperability Engine

## 1. Architecture Overview

The application will use a **modular monolithic layered architecture**.

The initial system must remain simple enough to develop, test, debug, deploy, and explain.

The architecture must not introduce microservices, Kafka, Redis, Kubernetes, or other distributed infrastructure unless a future requirement explicitly justifies it.

---

# 2. High-Level Architecture

```text
                              USER
                                │
                                ▼
                    ┌─────────────────────┐
                    │   React Frontend    │
                    │                     │
                    │  Login              │
                    │  HL7 Input          │
                    │  FHIR Output        │
                    │  History            │
                    └──────────┬──────────┘
                               │
                             Axios
                               │
                         HTTP / JSON
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Spring Boot Backend │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ REST Controller     │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Service Layer       │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
       ┌────────────┐   ┌────────────┐   ┌──────────────┐
       │ HL7 Parser │   │ Validator  │   │ HL7 → FHIR   │
       │            │   │            │   │ Mapper       │
       └────────────┘   └────────────┘   └──────┬───────┘
                                                │
                                                ▼
                                      ┌──────────────────┐
                                      │    HAPI FHIR     │
                                      │     FHIR R4      │
                                      └────────┬─────────┘
                                               │
                                               ▼
                                      ┌──────────────────┐
                                      │ FHIR Resources   │
                                      └────────┬─────────┘
                                               │
                                               ▼
                                      ┌──────────────────┐
                                      │ Persistence      │
                                      │ Layer            │
                                      └────────┬─────────┘
                                               │
                                      ┌────────▼────────┐
                                      │   Repository    │
                                      └────────┬────────┘
                                               │
                                               ▼
                                      ┌──────────────────┐
                                      │      MySQL       │
                                      └──────────────────┘
```

---

# 3. Architecture Style

The backend follows a layered architecture:

```text
Presentation Layer
       ↓
Controller
       ↓
Application / Business Layer
       ↓
Service
       ↓
Processing Components
       ↓
Persistence Layer
       ↓
Database
```

The major backend responsibilities are:

```text
Controller
Service
Parser
Validator
Mapper
FHIR Integration
Repository
Entity
Exception Handling
Security
Configuration
```

Each layer should have a clear responsibility.

---

# 4. Technology Stack

## Frontend

```text
React.js
JavaScript
HTML
CSS
Axios
```

## Backend

```text
Java
Spring Boot
Spring Web
Spring Data JPA
Hibernate
Maven
```

## Healthcare

```text
HL7 v2
FHIR R4
HAPI FHIR
```

## Database

```text
MySQL
```

## Security

```text
Spring Security
JWT
Password Hashing
```

## Testing

```text
JUnit 5
Mockito
MockMvc
Postman
```

## Version Control

```text
Git
GitHub
```

## Optional Later

```text
Docker
Docker Compose
```

---

# 5. Complete End-to-End Flow

The primary conversion flow is:

```text
User
 ↓
React UI
 ↓
Axios
 ↓
HTTP POST /api/hl7/convert
 ↓
HL7Controller
 ↓
HL7Service
 ↓
HL7Parser
 ↓
ParsedHL7Data
 ↓
HL7Validator
 ↓
Validated Parsed Data
 ↓
HL7ToFhirMapper
 ↓
HAPI FHIR
 ↓
FHIR R4 Resources
 ↓
Persistence Mapping
 ↓
JPA Entity
 ↓
Repository
 ↓
MySQL
 ↓
Service
 ↓
Response DTO
 ↓
Controller
 ↓
HTTP Response
 ↓
Axios
 ↓
React
 ↓
User
```

This is the primary architecture of the project.

---

# 6. Frontend Architecture

The frontend is responsible only for presentation and communication with the backend.

Conceptual structure:

```text
frontend/
│
├── src/
│   ├── components/
│   ├── pages/
│   ├── services/
│   ├── hooks/
│   ├── context/
│   ├── utils/
│   ├── App.jsx
│   └── main.jsx
│
└── package.json
```

The exact structure may be adjusted during implementation, but responsibilities must remain separated.

---

# 7. Frontend Responsibilities

React handles:

* rendering UI
* collecting HL7 input
* login form
* authentication state
* sending API requests
* receiving responses
* displaying FHIR output
* displaying errors
* displaying loading state
* navigation

React does NOT handle:

* HL7 parsing
* HL7-to-FHIR mapping
* healthcare validation
* database operations
* FHIR resource construction

---

# 8. Axios Layer

API communication should be centralized where practical.

Conceptual structure:

```text
React Component
      ↓
API Service
      ↓
Axios
      ↓
Spring Boot API
```

Potential API service modules:

```text
authApi
hl7Api
historyApi
```

Axios may use:

* base URL
* request interceptor
* response interceptor

The request interceptor may attach the JWT.

Example conceptual request:

```http
POST /api/hl7/convert
Authorization: Bearer <JWT>
Content-Type: application/json
```

---

# 9. Backend Package Architecture

The backend should follow responsibility-oriented package organization.

Recommended structure:

```text
backend/
└── src/
    └── main/
        └── java/
            └── <root-package>/
                │
                ├── controller/
                │
                ├── service/
                │
                ├── parser/
                │
                ├── validator/
                │
                ├── mapper/
                │
                ├── dto/
                │   ├── auth/
                │   ├── hl7/
                │   └── common/
                │
                ├── entity/
                │
                ├── repository/
                │
                ├── exception/
                │
                ├── security/
                │
                ├── config/
                │
                └── util/
```

The actual root package name must match the project's Maven group/package configuration.

---

# 10. Controller Layer

Controllers are the HTTP entry point.

Potential controllers:

```text
AuthController
HL7Controller
HistoryController
```

---

## 10.1 AuthController

Responsibilities:

* receive login requests
* validate request DTO
* delegate authentication to authentication service
* return authentication response

It must not:

* manually validate passwords
* create JWT logic directly
* access database directly

---

## 10.2 HL7Controller

Responsibilities:

* receive HL7 conversion request
* validate request DTO
* call HL7Service
* return conversion response

Conceptual:

```java
@PostMapping("/convert")
public ResponseEntity<HL7ConvertResponse> convert(
        @Valid @RequestBody HL7ConvertRequest request) {

    return ResponseEntity.ok(
        hl7Service.convert(request)
    );
}
```

The controller must remain thin.

---

# 11. Service Layer

The service layer contains application/business orchestration.

Potential services:

```text
AuthService
HL7Service
ConversionService
HistoryService
```

The exact number of services should be kept reasonable.

---

# 12. HL7Service Responsibilities

`HL7Service` orchestrates the complete HL7 conversion workflow.

Conceptual:

```text
HL7Service.convert()
        │
        ├── Parser
        │
        ├── Validator
        │
        ├── Mapper
        │
        ├── FHIR Processing
        │
        ├── Persistence
        │
        └── Response creation
```

The service should coordinate components instead of implementing every operation itself.

---

# 13. Parser Architecture

The parser is responsible for converting raw HL7 text into structured application data.

```text
Raw HL7
   ↓
HL7Parser
   ↓
ParsedHL7Data
```

Suggested interface:

```text
HL7Parser
```

Potential implementation:

```text
DefaultHL7Parser
```

The parser should be independently testable.

---

# 14. Parser Responsibilities

The parser can:

* identify segments
* identify fields
* extract values
* normalize raw values
* construct parsed application models

Example:

```text
PID|1||P1001||Sharma^Rahul||19950510|M
```

becomes conceptually:

```text
ParsedHL7Data
 └── patient
      ├── patientId = P1001
      ├── familyName = Sharma
      ├── givenName = Rahul
      ├── birthDate = 1995-05-10
      └── gender = M
```

The parser must not decide where those fields belong in FHIR.

---

# 15. Parser vs Mapper

This distinction is critical.

## Parser

Answers:

> "What data is present in the HL7 message?"

Example:

```text
PID-3 = P1001
PID-5 = Sharma^Rahul
PID-7 = 19950510
PID-8 = M
```

## Mapper

Answers:

> "Where should this data go in FHIR?"

Example:

```text
PID-3 → Patient.identifier
PID-5 → Patient.name
PID-7 → Patient.birthDate
PID-8 → Patient.gender
```

Therefore:

```text
Parser ≠ Mapper
```

---

# 16. ParsedHL7Data

A dedicated model should represent parsed information.

Conceptual:

```text
ParsedHL7Data
│
├── messageType
├── messageControlId
│
├── PatientData
│   ├── patientId
│   ├── familyName
│   ├── givenName
│   ├── birthDate
│   └── gender
│
├── EncounterData
│
└── List<ObservationData>
```

This model acts as the bridge between raw HL7 and FHIR mapping.

---

# 17. Validation Architecture

Validation should be separate from parsing.

```text
Raw HL7
   ↓
Parser
   ↓
ParsedHL7Data
   ↓
Validator
   ↓
Valid / Invalid
```

Suggested component:

```text
HL7Validator
```

The validator works on parsed/structured data and validates project-specific requirements.

---

# 18. Validation Responsibilities

Examples:

```text
Message type supported?
Patient ID present?
Patient name present?
Date valid?
Gender valid?
Required encounter data available?
Required observation data available?
```

If validation fails:

```text
Validator
   ↓
Application Exception
   ↓
GlobalExceptionHandler
   ↓
HTTP 400
```

---

# 19. Mapper Architecture

The mapper converts structured parsed data into FHIR resources.

Suggested component:

```text
HL7ToFhirMapper
```

Conceptual:

```text
ParsedHL7Data
      ↓
HL7ToFhirMapper
      ↓
FHIR Patient
FHIR Encounter
FHIR Observation
```

The mapper must use HAPI FHIR resource classes.

---

# 20. Mapper Responsibilities

The mapper decides:

* which HL7 data becomes which FHIR field
* how values are transformed
* which FHIR resource is created
* how related data is associated

Example:

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

Detailed mapping rules are maintained separately in:

```text
HEALTHCARE_MAPPING.md
```

---

# 21. HAPI FHIR Layer

HAPI FHIR is the Java FHIR implementation/library used by the backend.

Conceptual flow:

```text
HL7ToFhirMapper
        ↓
HAPI FHIR Model Classes
        ↓
FHIR R4 Resource
        ↓
FHIR JSON Serialization
```

HAPI FHIR provides:

* FHIR resource classes
* FHIR data types
* FHIR parsing/serialization
* FHIR R4 model support

---

# 22. HAPI FHIR Does Not Perform Business Mapping

The following distinction must be maintained:

```text
HL7ToFhirMapper
        │
        │ Business mapping decision
        ▼
HAPI FHIR
        │
        │ FHIR object construction/serialization
        ▼
FHIR R4 Resource
```

HAPI FHIR does not automatically understand project-specific decisions such as:

```text
PID-3 should become Patient.identifier
```

The application's mapper makes that decision.

---

# 23. FHIR Resource Creation

Conceptually:

```java
Patient patient = new Patient();

patient.addIdentifier()
       .setValue(parsedPatient.getPatientId());

patient.addName()
       .setFamily(parsedPatient.getFamilyName())
       .addGiven(parsedPatient.getGivenName());

patient.setBirthDate(parsedPatient.getBirthDate());

patient.setGender(
    Enumerations.AdministrativeGender.MALE
);
```

The exact implementation must follow the selected HAPI FHIR R4 version/API.

---

# 24. FHIR Serialization

HAPI FHIR should serialize resources into FHIR JSON.

Conceptually:

```java
FhirContext ctx = FhirContext.forR4();

String json = ctx
        .newJsonParser()
        .setPrettyPrint(true)
        .encodeResourceToString(patient);
```

Result:

```json
{
  "resourceType": "Patient",
  "identifier": [
    {
      "value": "P1001"
    }
  ],
  "name": [
    {
      "family": "Sharma",
      "given": ["Rahul"]
    }
  ],
  "birthDate": "1995-05-10",
  "gender": "male"
}
```

---

# 25. Multiple FHIR Resources

One HL7 message may produce multiple FHIR resources.

Example:

```text
ADT^A01
    │
    ├── Patient
    │
    └── Encounter
```

If OBX data is present:

```text
ADT / clinical message
    │
    ├── Patient
    ├── Encounter
    └── Observation
```

The final API response may use a FHIR Bundle or another documented response structure.

The decision must remain consistent across:

```text
ARCHITECTURE.md
REQUIREMENTS.md
HEALTHCARE_MAPPING.md
```

---

# 26. Persistence Architecture

FHIR resources and database entities are separate concepts.

The preferred conceptual flow is:

```text
Parsed Data
     ↓
FHIR Resource
     ↓
Persistence Mapping
     ↓
JPA Entity
     ↓
Repository
     ↓
MySQL
```

The project must not assume that:

```text
HAPI FHIR Patient == JPA @Entity
```

They have different responsibilities.

---

# 27. Entity Layer

Potential entities:

```text
User
PatientEntity
EncounterEntity
ObservationEntity
ConversionEntity
```

The exact entity set should be finalized based on the approved database requirements.

Entities should represent persistent application data.

They should not be used as API request/response DTOs by default.

---

# 28. Repository Layer

Repositories provide database access through Spring Data JPA.

Conceptual:

```text
PatientRepository
EncounterRepository
ObservationRepository
ConversionRepository
UserRepository
```

Example relationship:

```text
Service
   ↓
PatientRepository
   ↓
Hibernate
   ↓
JDBC
   ↓
MySQL
```

Repositories should not contain healthcare transformation logic.

---

# 29. JPA / Hibernate / JDBC / MySQL Relationship

The architecture is:

```text
Application
     ↓
Spring Data JPA
     ↓
Hibernate
     ↓
JDBC
     ↓
MySQL
```

### Spring Data JPA

Provides repository abstraction.

### JPA

Defines persistence specification concepts.

### Hibernate

Provides the JPA implementation used by the application.

### JDBC

Provides low-level Java database connectivity.

### MySQL

Stores persistent data.

---

# 30. Transaction Architecture

The conversion workflow may involve multiple database operations.

Example:

```text
Conversion
    ↓
Save Patient
    ↓
Save Encounter
    ↓
Save Observation
```

Where atomicity is required, the service layer should define an appropriate transaction boundary.

Conceptually:

```java
@Transactional
public HL7ConvertResponse convert(...) {
    ...
}
```

The exact transaction boundary must be determined based on the final persistence workflow.

---

# 31. Database Flow

Conceptual:

```text
HL7 Message
     ↓
Parsed Data
     ↓
FHIR Resources
     ↓
Persistence Model
     ↓
JPA Entity
     ↓
Repository
     ↓
Hibernate
     ↓
JDBC
     ↓
MySQL
```

The database should store only information required by the project.

---

# 32. Authentication Architecture

Authentication flow:

```text
React Login Page
       ↓
Axios
       ↓
POST /api/auth/login
       ↓
AuthController
       ↓
AuthService
       ↓
UserRepository
       ↓
Password Verification
       ↓
JWT Generation
       ↓
Login Response
       ↓
React
```

---

# 33. JWT Request Flow

After login:

```text
React
   ↓
Stores authentication state/token
   ↓
Axios Request
   ↓
Authorization: Bearer <JWT>
   ↓
Spring Security Filter Chain
   ↓
JWT Validation
   ↓
Authenticated Request
   ↓
Controller
```

Invalid JWT:

```text
JWT
 ↓
Invalid
 ↓
401 Unauthorized
```

---

# 34. Spring Security Responsibility

Spring Security handles:

* authentication
* authorization
* security filter chain
* protected endpoints
* password encoding integration
* JWT authentication integration

Spring Security should not contain HL7 parsing or FHIR mapping logic.

---

# 35. JWT Responsibility

JWT is the token mechanism used to represent authenticated sessions in a stateless API architecture.

JWT contains authentication-related claims as appropriate.

The application must not place sensitive healthcare information inside JWT claims.

---

# 36. Exception Architecture

All major application exceptions should be handled centrally.

Flow:

```text
Controller
    ↓
Service
    ↓
Exception
    ↓
GlobalExceptionHandler
    ↓
ErrorResponse
    ↓
HTTP Response
```

Potential exceptions:

```text
InvalidHL7Exception
UnsupportedHL7MessageException
FHIRConversionException
ResourceNotFoundException
```

Only necessary exceptions should be introduced.

---

# 37. Global Exception Handler

Suggested:

```text
GlobalExceptionHandler
```

It should translate application exceptions into HTTP responses.

Example:

```text
InvalidHL7Exception
        ↓
400 Bad Request
```

Example:

```text
ResourceNotFoundException
        ↓
404 Not Found
```

Unexpected exceptions:

```text
Unexpected Exception
        ↓
500 Internal Server Error
```

Internal stack traces must not be returned to the frontend.

---

# 38. Complete Successful Conversion Sequence

```text
1. User opens application
        ↓
2. User logs in
        ↓
3. Backend authenticates credentials
        ↓
4. Backend returns JWT
        ↓
5. User opens conversion screen
        ↓
6. User pastes HL7 message
        ↓
7. User clicks Convert
        ↓
8. React sends Axios request
        ↓
9. Spring Security validates JWT
        ↓
10. HL7Controller receives request
        ↓
11. Request DTO is validated
        ↓
12. Controller calls HL7Service
        ↓
13. HL7Service calls HL7Parser
        ↓
14. Parser creates ParsedHL7Data
        ↓
15. HL7Service calls HL7Validator
        ↓
16. Validator validates parsed data
        ↓
17. HL7Service calls HL7ToFhirMapper
        ↓
18. Mapper creates HAPI FHIR resources
        ↓
19. FHIR resources are generated
        ↓
20. Required data is converted to persistence models
        ↓
21. Repository saves required data
        ↓
22. Service creates response DTO
        ↓
23. Controller returns HTTP response
        ↓
24. Axios receives response
        ↓
25. React displays FHIR output
        ↓
26. User views result
```

---

# 39. Complete Failed Conversion Sequence

Example: missing PID segment.

```text
User
 ↓
React
 ↓
Axios
 ↓
Controller
 ↓
Service
 ↓
Parser
 ↓
Parsed Data
 ↓
Validator
 ↓
PID Missing
 ↓
InvalidHL7Exception
 ↓
GlobalExceptionHandler
 ↓
400 Bad Request
 ↓
Axios
 ↓
React
 ↓
Error Message
 ↓
User
```

No FHIR resource should be created after a fatal validation failure.

No successful persistence should occur for that failed conversion.

---

# 40. Package-to-Responsibility Matrix

| Package      | Responsibility                          |
| ------------ | --------------------------------------- |
| `controller` | HTTP/API entry point                    |
| `service`    | Business/application orchestration      |
| `parser`     | Raw HL7 → structured data               |
| `validator`  | HL7-specific validation                 |
| `mapper`     | HL7 data → FHIR resources               |
| `dto`        | API request/response models             |
| `entity`     | Database persistence models             |
| `repository` | Database access                         |
| `exception`  | Application exceptions                  |
| `security`   | Authentication/JWT/Spring Security      |
| `config`     | Application configuration               |
| `util`       | Small reusable technical utilities only |

---

# 41. Class Responsibility Matrix

| Class                    | Primary Responsibility         |
| ------------------------ | ------------------------------ |
| `AuthController`         | Authentication HTTP API        |
| `HL7Controller`          | HL7 conversion HTTP API        |
| `AuthService`            | Authentication orchestration   |
| `HL7Service`             | Conversion orchestration       |
| `HL7Parser`              | Parse raw HL7                  |
| `HL7Validator`           | Validate parsed HL7            |
| `HL7ToFhirMapper`        | Map HL7 data to FHIR           |
| `PatientRepository`      | Patient persistence            |
| `EncounterRepository`    | Encounter persistence          |
| `ObservationRepository`  | Observation persistence        |
| `UserRepository`         | User persistence               |
| `GlobalExceptionHandler` | Centralized API error handling |
| JWT/security components  | Authentication/security        |

No class should accumulate unrelated responsibilities.

---

# 42. Dependency Direction

Preferred dependency direction:

```text
Controller
    ↓
Service
    ↓
Parser / Validator / Mapper / Repository
```

Supporting components should not depend upward on controllers.

For example:

```text
Parser → Controller
```

is prohibited.

Similarly:

```text
Repository → Controller
```

is prohibited.

---

# 43. Controller Dependency Rule

Controller may depend on:

```text
Service
DTO
```

Controller should not directly depend on:

```text
Repository
Parser
Mapper
Entity
```

unless there is a compelling documented reason.

---

# 44. Service Dependency Rule

Service may depend on:

```text
Parser
Validator
Mapper
Repository
FHIR components
other required services
```

Service is the orchestration layer.

---

# 45. Parser Dependency Rule

Parser should have minimal dependencies.

It should primarily depend on:

* parsing utilities
* parsed data models
* required healthcare parsing library/utilities if selected

Parser should not depend on:

* repositories
* controllers
* Spring Security
* frontend
* database

---

# 46. Mapper Dependency Rule

Mapper may depend on:

* ParsedHL7Data
* HAPI FHIR
* mapping-related utilities

Mapper must not depend on:

* Controller
* Repository
* database
* frontend
* authentication

---

# 47. Repository Dependency Rule

Repositories interact with persistence.

They should not:

* parse HL7
* create FHIR resources
* authenticate users
* generate HTTP responses

---

# 48. DTO Architecture

DTOs form boundaries between external API contracts and internal models.

Conceptual:

```text
HTTP Request
     ↓
Request DTO
     ↓
Service
     ↓
Internal Model
```

Response:

```text
Internal Result
     ↓
Response DTO
     ↓
Controller
     ↓
HTTP Response
```

Do not expose database entities directly unless explicitly justified.

---

# 49. Entity vs FHIR Resource

The project contains two distinct model worlds.

## FHIR Resource

Represents standardized healthcare data.

Example:

```text
Patient
Encounter
Observation
```

## JPA Entity

Represents application persistence.

Example:

```text
PatientEntity
EncounterEntity
ObservationEntity
```

They must not be treated as the same thing by default.

---

# 50. Healthcare Mapping Architecture

The mapping knowledge is separated into:

```text
HEALTHCARE_MAPPING.md
```

Architecture:

```text
Raw HL7
   ↓
Parser
   ↓
ParsedHL7Data
   ↓
HL7ToFhirMapper
   ↓
FHIR R4 Resource
```

Example:

```text
PID-3
 ↓
Parsed patientId
 ↓
Mapper
 ↓
Patient.identifier
```

The mapping document is the source of truth for field-level healthcare mapping.

---

# 51. Configuration Architecture

Spring Boot configuration should be externalized where appropriate.

Potential files:

```text
application.yml
application-dev.yml
application-prod.yml
```

Sensitive values should come from environment variables.

Example conceptual:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION
```

Do not commit real secrets.

---

# 52. Maven Architecture

Maven manages backend dependencies and build lifecycle.

Conceptual:

```text
pom.xml
   ↓
Maven
   ↓
Dependencies
   ↓
Compile
   ↓
Test
   ↓
Package
   ↓
Spring Boot JAR
```

The final `pom.xml` must contain only dependencies actually required by the application.

---

# 53. Testing Architecture

Testing should follow the architecture.

```text
HL7Parser
    ↓
Unit Test
    ↓
JUnit 5
```

```text
HL7Validator
    ↓
Unit Test
    ↓
JUnit 5
```

```text
HL7ToFhirMapper
    ↓
Unit Test
    ↓
JUnit 5
```

```text
HL7Service
    ↓
Unit Test
    ↓
Mockito
```

```text
HL7Controller
    ↓
Controller Test
    ↓
MockMvc
```

```text
Complete API
    ↓
Postman
```

---

# 54. Testing Pyramid

The project should prefer:

```text
             ┌─────────────┐
             │   Postman   │
             │ API / E2E   │
             └──────┬──────┘
                    │
              ┌─────▼─────┐
              │  MockMvc  │
              │ Controller│
              └─────┬─────┘
                    │
          ┌─────────▼─────────┐
          │ Unit Tests        │
          │ Parser/Validator  │
          │ Mapper/Service    │
          └───────────────────┘
```

Most business logic should be covered with focused unit tests.

---

# 55. Deployment Architecture

Initial deployment can remain simple.

Conceptual:

```text
                    Internet
                       │
          ┌────────────┴────────────┐
          │                         │
          ▼                         ▼
    React Frontend             Spring Boot API
                                     │
                                     ▼
                                   MySQL
```

Frontend and backend may be deployed independently.

Environment-specific configuration must be used.

---

# 56. Docker Architecture

Docker is optional for the first working implementation.

Later:

```text
Docker Compose
│
├── frontend
├── backend
└── mysql
```

Docker must not complicate the core application before the core functionality works.

---

# 57. Architecture Decisions

## Decision 1 — Modular Monolith

Use a modular monolith instead of microservices.

Reason:

* simpler development
* easier debugging
* suitable for project scale
* easier deployment
* easier interview explanation

---

## Decision 2 — Layered Architecture

Use Controller → Service → Processing → Repository.

Reason:

* separation of concerns
* maintainability
* testability
* clear responsibilities

---

## Decision 3 — Separate Parser, Validator, Mapper

Do not create one giant HL7 processor.

Reason:

* parser has one responsibility
* validator has one responsibility
* mapper has one responsibility
* easier unit testing
* easier future extension

---

## Decision 4 — HAPI FHIR

Use HAPI FHIR rather than manually constructing FHIR JSON.

Reason:

* FHIR R4 model support
* standardized resource classes
* serialization utilities
* less error-prone resource construction

---

## Decision 5 — MySQL

Use MySQL for relational persistence.

Reason:

* structured healthcare/application data
* relationship support
* SQL knowledge
* Spring Data JPA compatibility

---

## Decision 6 — JWT

Use JWT for stateless authentication.

Reason:

* suitable for REST APIs
* easy frontend/backend integration
* supported by Spring Security

---

# 58. Technologies Explicitly Avoided in MVP

The following must not be introduced without a specific approved requirement:

```text
Kafka
Redis
Kubernetes
Microservices
RabbitMQ
Elasticsearch
MongoDB
GraphQL
Complex event-driven architecture
```

The purpose is not to demonstrate the maximum number of technologies.

The purpose is to build a coherent healthcare interoperability system.

---

# 59. Architecture Extension Strategy

Future message types should extend the processing architecture rather than bypass it.

Current:

```text
ADT^A01
   ↓
HL7Parser
   ↓
HL7Validator
   ↓
HL7ToFhirMapper
```

Future:

```text
ADT^A01 ─┐
ADT^A03 ─┤
ADT^A08 ─┤
ORU^R01 ─┤
ORM^O01 ─┘
          ↓
    Message-specific
    processing/mapping
          ↓
      FHIR R4
```

The implementation should evolve toward this only when additional message types are actually required.

---

# 60. Architecture Quality Rules

The AI coding agent must enforce these rules:

### Rule 1

Controllers remain thin.

### Rule 2

Business logic belongs in services or dedicated domain-processing components.

### Rule 3

Parsing belongs to parser components.

### Rule 4

Validation belongs to validator components.

### Rule 5

HL7-to-FHIR mapping belongs to mapper components.

### Rule 6

Database access belongs to repositories.

### Rule 7

FHIR resource construction uses HAPI FHIR.

### Rule 8

Database entities are not automatically API DTOs.

### Rule 9

FHIR resources are not automatically JPA entities.

### Rule 10

Frontend never contains healthcare conversion logic.

### Rule 11

No unnecessary technology may be added.

### Rule 12

Each class should have a clear primary responsibility.

---

# 61. AI Agent Implementation Boundary

Before implementing a class, the AI agent must identify:

```text
What is this class responsible for?
What does it receive?
What does it return?
Which classes can it call?
Which classes must it NOT call?
```

Example:

```text
HL7Parser

Receives:
Raw HL7 String

Returns:
ParsedHL7Data

Can use:
Parsing utilities

Must NOT use:
Repository
Controller
Spring Security
MySQL
React
```

---

# 62. Final Architecture

The complete architecture is:

```text
┌─────────────────────────────────────────────────────────────┐
│                         FRONTEND                            │
│                                                             │
│ React                                                        │
│ ├── Login                                                   │
│ ├── Dashboard                                               │
│ ├── HL7 Input                                               │
│ ├── Conversion Result                                       │
│ └── History                                                 │
│                                                             │
│ Axios                                                       │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              │ HTTP / JSON
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       SPRING BOOT                           │
│                                                             │
│ Spring Security + JWT                                       │
│                 │                                           │
│                 ▼                                           │
│ Controllers                                                 │
│                 │                                           │
│                 ▼                                           │
│ Services                                                    │
│                 │                                           │
│       ┌─────────┼──────────┐                                │
│       ▼         ▼          ▼                                │
│    Parser   Validator    Mapper                             │
│                              │                              │
│                              ▼                              │
│                         HAPI FHIR                           │
│                              │                              │
│                              ▼                              │
│                         FHIR R4                             │
│                              │                              │
│                              ▼                              │
│                     Persistence Mapping                     │
│                              │                              │
│                              ▼                              │
│                         Repository                          │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                          MySQL                              │
│                                                             │
│ User                                                        │
│ Patient                                                     │
│ Encounter                                                   │
│ Observation                                                 │
│ Conversion / Metadata                                       │
└─────────────────────────────────────────────────────────────┘
```

---

# 63. Primary Interview Explanation

The project should be explainable in the following concise way:

> "The application follows a modular monolithic layered architecture. React is responsible for the UI and Axios communicates with the Spring Boot REST API. The controller only handles HTTP requests and delegates the operation to the service layer. The service orchestrates the conversion pipeline. A dedicated HL7 parser extracts structured data from the raw HL7 message, a validator checks the required data, and an HL7-to-FHIR mapper converts that structured data into FHIR R4 resources using HAPI FHIR. Required application data is then mapped to persistence entities and stored through Spring Data JPA and Hibernate in MySQL. The generated FHIR response is returned through the controller to the React frontend. Spring Security and JWT protect the APIs, while global exception handling provides consistent error responses."

This explanation represents the intended architecture and should remain consistent with the implementation.

---

# 64. Architecture Source-of-Truth Rule

For technical architecture decisions, this file is the primary reference.

If implementation differs from this architecture:

1. Identify the reason.
2. Verify that the change is genuinely necessary.
3. Update `ARCHITECTURE.md`.
4. Update dependent documentation if required.
5. Ensure the implementation and documentation remain synchronized.

The AI coding agent must not silently introduce architectural changes.
