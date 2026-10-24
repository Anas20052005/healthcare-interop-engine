# Development Plan — HL7 v2 → FHIR Healthcare Interoperability Engine

## 1. Document Purpose

This document defines the implementation plan for the HL7 v2 → FHIR Healthcare Interoperability Engine.

It provides the AI coding agent and developers with:

* Implementation order
* Development phases
* Milestones
* Dependencies
* Task breakdown
* Testing strategy
* Integration strategy
* Bug-fixing workflow
* Deployment preparation
* Definition of Done
* MVP boundaries
* AI coding rules

The project must be implemented incrementally.

The AI agent must NOT attempt to generate the entire application in one uncontrolled step.

The preferred approach is:

```text
PLAN
 ↓
SETUP
 ↓
BACKEND FOUNDATION
 ↓
DATABASE
 ↓
HEALTHCARE PIPELINE
 ↓
FHIR INTEGRATION
 ↓
AUTHENTICATION
 ↓
FRONTEND
 ↓
FRONTEND-BACKEND INTEGRATION
 ↓
TESTING
 ↓
BUG FIXING
 ↓
DEPLOYMENT
```

---

# 2. Development Philosophy

The project should be developed as a modular monolith.

The implementation should prioritize:

1. Correctness
2. Clear architecture
3. Healthcare mapping accuracy
4. Testability
5. Security
6. Maintainability
7. Clean UI
8. Deployment readiness

The project should NOT prioritize adding the maximum number of technologies.

The goal is to build a realistic, understandable healthcare interoperability project that can be explained confidently in interviews.

---

# 3. MVP Definition

The MVP must support the following complete workflow:

```text
User
 ↓
Login
 ↓
Dashboard / Converter
 ↓
Paste HL7 v2 ADT^A01
 ↓
Submit
 ↓
Spring Boot API
 ↓
Parse HL7
 ↓
Validate HL7
 ↓
Map HL7 → FHIR
 ↓
Generate FHIR R4 using HAPI FHIR
 ↓
Persist required application data
 ↓
Return FHIR response
 ↓
React displays FHIR JSON
```

The MVP should focus on one reliable end-to-end conversion path before supporting many message types.

---

# 4. MVP Message Type

Initial supported message type:

```text
ADT^A01
```

ADT^A01 is the primary message type for the first working version.

The architecture should be extensible enough to support additional message types later.

Do NOT attempt to support every HL7 message type during the initial implementation.

---

# 5. MVP FHIR Resources

Initial target resources:

```text
Patient
Encounter
Observation
```

The exact resource set depends on the final mapping requirements in:

```text
agent/HEALTHCARE_MAPPING.md
```

Patient should be implemented first.

Encounter and Observation should be added after the Patient conversion path is stable.

---

# 6. Overall Development Roadmap

```text
Phase 0  → Project Planning
Phase 1  → Repository & Environment Setup
Phase 2  → Spring Boot Backend Foundation
Phase 3  → MySQL & Persistence
Phase 4  → HL7 Parser
Phase 5  → HL7 Validator
Phase 6  → FHIR Mapper + HAPI FHIR
Phase 7  → End-to-End Conversion Service
Phase 8  → Authentication + Security
Phase 9  → Backend Testing
Phase 10 → React Frontend Foundation
Phase 11 → Frontend UI
Phase 12 → Frontend-Backend Integration
Phase 13 → History / Additional Features
Phase 14 → Integration Testing & Bug Fixing
Phase 15 → Docker & Deployment
Phase 16 → Documentation & Final Polish
```

---

# 7. Phase 0 — Project Planning

## Objective

Understand the project documentation before writing application code.

The AI agent must first read:

```text
agent/PRD.md
agent/REQUIREMENTS.md
agent/ARCHITECTURE.md
agent/UI_UX.md
agent/DEVELOPMENT_PLAN.md
agent/HEALTHCARE_MAPPING.md
```

If one of these files is missing, the agent should not silently invent major requirements.

---

## Tasks

### Task 0.1

Confirm project root.

Expected:

```text
HL7-FHIR-Integration-Engine/
```

### Task 0.2

Confirm documentation:

```text
agent/
├── PRD.md
├── REQUIREMENTS.md
├── ARCHITECTURE.md
├── UI_UX.md
├── DEVELOPMENT_PLAN.md
└── HEALTHCARE_MAPPING.md
```

### Task 0.3

Confirm source folders:

```text
frontend/
backend/
```

### Task 0.4

Confirm MVP:

```text
HL7 v2 ADT^A01
        ↓
FHIR R4
```

### Task 0.5

Confirm technology stack.

---

## Milestone

Planning is complete when the agent can explain:

* What the project does.
* Who uses it.
* What the MVP supports.
* What technologies are required.
* What the backend architecture is.
* What the frontend architecture is.
* What the healthcare mapping scope is.

---

# 8. Phase 1 — Repository and Environment Setup

## Objective

Create the project structure and development environment.

Expected structure:

```text
HL7-FHIR-Integration-Engine/
│
├── agent/
│
├── frontend/
│
├── backend/
│
├── README.md
└── .gitignore
```

---

## Backend Setup

Create Spring Boot project with:

* Java
* Spring Boot
* Maven
* Spring Web
* Spring Data JPA
* Validation
* MySQL Driver

Additional dependencies will be added when required.

Do not add every dependency at the beginning.

---

## Frontend Setup

Create React application.

Required:

* React
* JavaScript
* Axios
* Routing solution if required

---

## Git Setup

Initialize Git repository.

Create:

```text
.gitignore
```

The following must not be committed:

```text
.env
application-local.properties
database credentials
JWT secrets
IDE-specific files
target/
node_modules/
build artifacts
logs
```

---

## Milestone 1

The project should successfully contain:

```text
frontend/
backend/
agent/
```

Both frontend and backend should start independently.

---

# 9. Phase 2 — Spring Boot Backend Foundation

## Objective

Create the backend application structure before implementing healthcare logic.

Recommended package structure:

```text
backend/src/main/java/<root-package>/

├── controller/
├── service/
├── parser/
├── validator/
├── mapper/
├── dto/
│   ├── auth/
│   ├── hl7/
│   └── common/
├── entity/
├── repository/
├── exception/
├── security/
├── config/
└── util/
```

---

## Tasks

### Task 2.1

Create Spring Boot main application.

### Task 2.2

Configure Maven.

### Task 2.3

Configure application properties/YAML.

### Task 2.4

Create basic health endpoint.

Example:

```text
GET /api/health
```

Expected:

```json
{
  "status": "UP"
}
```

### Task 2.5

Verify application starts without errors.

---

## Milestone 2

Backend starts successfully and responds to a basic API request.

---

# 10. Phase 3 — MySQL and Persistence Foundation

## Objective

Connect Spring Boot to MySQL and establish persistence architecture.

Flow:

```text
Spring Data JPA
 ↓
Hibernate
 ↓
JDBC
 ↓
MySQL
```

---

## Tasks

### Task 3.1

Create MySQL database.

Example:

```text
hl7_fhir_engine
```

### Task 3.2

Configure database connection through environment/configuration.

Never hardcode production credentials.

### Task 3.3

Create initial entities only when required.

Potential entities:

```text
User
PatientEntity
EncounterEntity
ObservationEntity
ConversionEntity
```

The final entity set must follow the requirements and mapping documents.

### Task 3.4

Create repositories.

Example:

```text
UserRepository
PatientRepository
EncounterRepository
ObservationRepository
ConversionRepository
```

Only create repositories that are actually required.

### Task 3.5

Verify database connectivity.

---

## Milestone 3

Spring Boot can:

```text
Connect
 ↓
Persist
 ↓
Read
```

from MySQL successfully.

---

# 11. Phase 4 — HL7 Parser

## Objective

Convert raw HL7 text into a structured Java model.

Flow:

```text
Raw HL7 String
      ↓
HL7Parser
      ↓
ParsedHL7Data
```

---

## Tasks

### Task 4.1

Define `ParsedHL7Data`.

Example conceptual fields:

```text
messageType
messageControlId
patientId
firstName
lastName
birthDate
gender
encounterId
encounterClass
observations
```

The exact fields must follow `HEALTHCARE_MAPPING.md`.

### Task 4.2

Implement `HL7Parser`.

### Task 4.3

Parse required segments:

```text
MSH
PID
PV1
OBX
```

Only implement segments required by the MVP mapping.

### Task 4.4

Handle HL7 delimiters correctly.

### Task 4.5

Handle missing/invalid structure gracefully.

### Task 4.6

Do not create FHIR resources inside the parser.

---

## Parser Rule

The parser performs:

```text
Raw HL7
 ↓
Structured Java data
```

It does NOT perform:

```text
Raw HL7
 ↓
FHIR
```

---

## Milestone 4

Given a valid sample ADT^A01 message, the parser produces correct `ParsedHL7Data`.

---

# 12. Phase 5 — HL7 Validator

## Objective

Validate the parsed HL7 data.

Flow:

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

---

## Validation Categories

### Structural validation

Check:

* Message exists.
* Required segments exist.
* Segment structure is usable.

### Message validation

Check:

* Expected message type.
* Required message fields.

### Patient validation

Check required patient information.

### Date validation

Check supported date formats.

### Field validation

Check values according to the healthcare mapping rules.

---

## Important Rule

Do not duplicate parsing logic inside the validator.

Correct:

```text
Parser
 ↓
Parsed model
 ↓
Validator
```

Incorrect:

```text
Validator
 ↓
Re-parse raw HL7
```

---

## Milestone 5

Invalid HL7 produces a controlled validation error.

Valid HL7 proceeds to mapping.

---

# 13. Phase 6 — FHIR Mapper and HAPI FHIR

## Objective

Convert structured HL7 data into FHIR R4 resources.

Flow:

```text
ParsedHL7Data
      ↓
HL7ToFhirMapper
      ↓
HAPI FHIR
      ↓
FHIR R4 Resource
```

---

## Implementation Order

Implement:

### Step 6.1

Patient mapping.

```text
PID
 ↓
Patient
```

### Step 6.2

Encounter mapping.

```text
PV1
 ↓
Encounter
```

### Step 6.3

Observation mapping.

```text
OBX
 ↓
Observation
```

---

## Patient Mapping

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

Exact mapping must follow:

```text
agent/HEALTHCARE_MAPPING.md
```

---

## HAPI FHIR Rule

Use HAPI FHIR resource classes.

Do not manually construct FHIR JSON as the primary conversion mechanism.

Conceptual:

```java
Patient patient = new Patient();

patient.setId(...);

patient.addIdentifier()
       .setValue(...);

patient.addName()
       .setFamily(...)
       .addGiven(...);
```

---

## FHIR Serialization

Use HAPI FHIR serialization:

```text
FHIR Resource
 ↓
HAPI FHIR Parser
 ↓
FHIR JSON
```

---

## Milestone 6

A valid ADT^A01 message can produce a valid FHIR R4 Patient resource.

---

# 14. Phase 7 — End-to-End Conversion Service

## Objective

Connect parser, validator, mapper, and persistence into one application workflow.

Expected service flow:

```text
HL7Service
    ↓
Parser
    ↓
Validator
    ↓
Mapper
    ↓
FHIR Resource
    ↓
Persistence Mapping
    ↓
Repository
    ↓
MySQL
    ↓
FHIR Response
```

---

## Tasks

### Task 7.1

Create conversion request DTO.

Example:

```json
{
  "hl7Message": "..."
}
```

### Task 7.2

Create conversion response DTO.

### Task 7.3

Create `HL7Service`.

### Task 7.4

Connect parser.

### Task 7.5

Connect validator.

### Task 7.6

Connect mapper.

### Task 7.7

Connect persistence.

### Task 7.8

Create conversion controller.

Conceptual endpoint:

```text
POST /api/hl7/convert
```

---

# 15. Conversion Endpoint Flow

```text
POST /api/hl7/convert
        ↓
HL7Controller
        ↓
HL7Service
        ↓
HL7Parser
        ↓
HL7Validator
        ↓
HL7ToFhirMapper
        ↓
FHIR Resource
        ↓
Persistence
        ↓
Response DTO
        ↓
HTTP 200
```

---

# 16. Controller Responsibility

Controller must:

* Receive HTTP request.
* Validate request DTO.
* Call service.
* Return response.

Controller must NOT:

* Parse HL7.
* Map HL7 to FHIR.
* Access repositories directly.
* Write SQL.
* Create complex FHIR resources.

---

# 17. Service Responsibility

Service coordinates the application workflow.

It may conceptually perform:

```java
ParsedHL7Data data = parser.parse(message);

validator.validate(data);

FHIRResource resource = mapper.map(data);

persist(data);

return response;
```

The service is the orchestrator.

---

# 18. Milestone 7 — Complete Backend Conversion

At this point the backend must support:

```text
Postman
 ↓
POST /api/hl7/convert
 ↓
HL7 input
 ↓
FHIR output
```

This is the first major end-to-end milestone.

Do not start complex frontend work until this basic backend conversion flow works reliably.

---

# 19. Phase 8 — Authentication and Security

## Objective

Secure application APIs using Spring Security and JWT.

Authentication flow:

```text
Login
 ↓
AuthController
 ↓
AuthService
 ↓
UserRepository
 ↓
Verify credentials
 ↓
Generate JWT
 ↓
Return token
```

---

## Tasks

### Task 8.1

Create `User` entity.

### Task 8.2

Create `UserRepository`.

### Task 8.3

Create authentication DTOs.

### Task 8.4

Create login endpoint.

Conceptual:

```text
POST /api/auth/login
```

### Task 8.5

Configure password hashing.

### Task 8.6

Generate JWT.

### Task 8.7

Configure Spring Security.

### Task 8.8

Create JWT authentication filter/mechanism.

### Task 8.9

Protect conversion endpoint.

### Task 8.10

Verify unauthorized access returns:

```text
401 Unauthorized
```

---

# 20. Security Rules

Never:

* Hardcode JWT secrets.
* Store plaintext passwords.
* Put PHI inside JWT claims.
* Return passwords.
* Log credentials.
* Log JWT secrets.

Secrets must come from secure configuration/environment variables.

---

# 21. Milestone 8

Authentication works:

```text
Login
 ↓
JWT
 ↓
Authenticated request
 ↓
Protected API
```

Unauthenticated requests are rejected.

---

# 22. Phase 9 — Backend Testing

Testing must happen before frontend integration is considered complete.

---

# 23. Unit Testing

Use:

```text
JUnit 5
Mockito
```

Test:

### Parser

* Valid HL7.
* Missing PID.
* Missing required fields.
* Incorrect structure.

### Validator

* Valid model.
* Invalid patient data.
* Invalid message type.
* Missing required fields.

### Mapper

* Correct Patient mapping.
* Correct Encounter mapping.
* Correct Observation mapping.

### Service

* Successful conversion.
* Parser failure.
* Validation failure.
* Mapping failure.
* Persistence failure.

---

# 24. Controller Testing

Use MockMvc where appropriate.

Test:

```text
POST /api/hl7/convert
```

Scenarios:

* Valid request.
* Empty request.
* Invalid HL7.
* Unauthorized request.
* Successful response.
* Internal error.

---

# 25. Integration Testing

Verify:

```text
Controller
 ↓
Service
 ↓
Parser
 ↓
Validator
 ↓
Mapper
 ↓
Persistence
```

where appropriate.

Database integration tests should verify that required data is correctly persisted.

---

# 26. Postman Testing

Create a Postman collection covering:

```text
1. Login
2. Valid HL7 conversion
3. Invalid HL7
4. Missing authentication
5. Valid authenticated conversion
6. History if implemented
```

The Postman collection should be saved with the project if appropriate.

---

# 27. Negative Testing

Negative cases are extremely important for this project.

Test at least:

```text
Empty HL7
Malformed HL7
Missing MSH
Missing PID
Invalid message type
Missing patient ID
Invalid date
Invalid gender
Malformed OBX
Unauthorized request
Invalid JWT
Database failure
```

The exact cases must follow the supported MVP mapping.

---

# 28. Phase 10 — React Frontend Foundation

## Objective

Create the frontend structure after backend API contracts are stable.

Create:

```text
frontend/
└── src/
    ├── components/
    ├── pages/
    ├── services/
    ├── context/
    ├── hooks/
    ├── utils/
    └── styles/
```

---

## Tasks

### Task 10.1

Configure React.

### Task 10.2

Configure routing.

### Task 10.3

Create application shell.

### Task 10.4

Create Navbar.

### Task 10.5

Create authentication context/state.

### Task 10.6

Create Axios client.

### Task 10.7

Configure environment-based API URL.

---

# 29. Phase 11 — Login UI

Implement:

```text
/login
```

Features:

* Username/email input.
* Password input.
* Login button.
* Loading state.
* Error state.
* Redirect after successful login.

Flow:

```text
Login UI
 ↓
Axios
 ↓
POST /api/auth/login
 ↓
JWT
 ↓
Auth state
 ↓
Dashboard
```

---

# 30. Phase 12 — Dashboard UI

Implement:

```text
/dashboard
```

Features:

* Application title.
* Short project description.
* Start Conversion button.
* Optional conversion statistics.
* Recent conversions if backend supports them.

Do not create fake metrics for the final application.

---

# 31. Phase 13 — Converter UI

Implement:

```text
/convert
```

Core components:

```text
HL7Editor
ConversionStatus
FhirViewer
```

Features:

* HL7 textarea/editor.
* Load Example.
* Clear.
* Convert to FHIR.
* Loading state.
* Success state.
* Error state.
* FHIR JSON output.
* Copy JSON.

---

# 32. Converter API Integration

Flow:

```text
Converter.jsx
       ↓
hl7Service.js
       ↓
Axios
       ↓
POST /api/hl7/convert
       ↓
Spring Boot
       ↓
FHIR response
       ↓
hl7Service
       ↓
Converter.jsx
       ↓
FhirViewer
```

The React page must not contain HL7 parsing or FHIR mapping logic.

---

# 33. Phase 14 — History

History should only be implemented if it is included in the approved MVP scope.

If implemented:

```text
/history
```

should support:

* List conversions.
* Date/time.
* Message type.
* Status.
* View details.

Avoid unnecessary PHI exposure.

---

# 34. Phase 15 — Frontend-Backend Integration

## Objective

Connect the complete application.

Final flow:

```text
React Login
    ↓
Spring Security
    ↓
JWT
    ↓
React Converter
    ↓
Axios
    ↓
Spring Boot
    ↓
HL7 Parser
    ↓
Validator
    ↓
FHIR Mapper
    ↓
HAPI FHIR
    ↓
MySQL
    ↓
Response
    ↓
Axios
    ↓
React
    ↓
FHIR Viewer
```

---

# 35. Integration Checklist

Verify:

* Backend URL is correct.
* CORS is configured.
* Login works.
* JWT is sent.
* Protected API works.
* HL7 request format matches backend DTO.
* FHIR response format matches frontend expectations.
* Errors are handled.
* Loading states work.
* Logout works.

---

# 36. Phase 16 — Full-System Testing

Perform complete user journey testing.

### Scenario 1 — Successful conversion

```text
Open app
 ↓
Login
 ↓
Dashboard
 ↓
Converter
 ↓
Load Example
 ↓
Convert
 ↓
FHIR output
```

Expected: successful conversion.

---

### Scenario 2 — Invalid HL7

```text
Login
 ↓
Converter
 ↓
Enter malformed HL7
 ↓
Convert
```

Expected:

```text
Readable validation error
```

---

### Scenario 3 — Empty input

```text
Converter
 ↓
Click Convert
```

Expected:

```text
HL7 message is required.
```

No unnecessary API request should be made.

---

### Scenario 4 — Unauthorized API

Call protected endpoint without JWT.

Expected:

```text
401 Unauthorized
```

---

### Scenario 5 — Expired/invalid JWT

Expected:

```text
Session expired
 ↓
Login
```

---

# 37. Phase 17 — Bug-Fixing Workflow

When a bug occurs, do not immediately rewrite unrelated code.

Use:

```text
Reproduce
 ↓
Identify layer
 ↓
Read relevant code
 ↓
Find root cause
 ↓
Fix smallest correct area
 ↓
Run affected tests
 ↓
Run integration test
 ↓
Verify no regression
```

---

# 38. Bug Classification

Classify bugs by layer.

### Frontend

```text
UI
Routing
State
Axios
Rendering
```

### Controller

```text
Request
Response
HTTP status
DTO
```

### Service

```text
Business flow
Orchestration
Transaction
```

### Parser

```text
HL7 extraction
```

### Validator

```text
Validation
```

### Mapper

```text
HL7 → FHIR mapping
```

### Persistence

```text
Entity
Repository
Database
```

### Security

```text
JWT
Authentication
Authorization
```

This prevents random code modifications.

---

# 39. Regression Testing

After fixing a bug:

```text
Bug fixed
 ↓
Run related unit test
 ↓
Run related integration test
 ↓
Run complete conversion
```

A bug fix must not break:

* Existing mappings.
* Authentication.
* Database persistence.
* API response.
* Frontend rendering.

---

# 40. Phase 18 — Code Quality Review

Before deployment, review:

### Backend

* Controller responsibilities.
* Service responsibilities.
* Parser separation.
* Validator separation.
* Mapper separation.
* Repository usage.
* Exception handling.
* DTO/entity separation.
* Security configuration.
* Transaction boundaries.

### Frontend

* Component responsibilities.
* API service separation.
* Authentication state.
* Error handling.
* Loading states.
* Responsive layout.
* Unnecessary dependencies.

---

# 41. Architecture Review

Confirm:

```text
Controller
   ↓
Service
   ↓
Parser / Validator / Mapper
   ↓
Persistence
```

No incorrect dependencies should exist.

Examples of prohibited architecture:

```text
Controller → Repository
React → Database
React → HL7 Parser
Parser → Repository
Mapper → Controller
```

---

# 42. Phase 19 — Docker Preparation

Docker is not required for the first development milestone.

After the application works locally, containerization can be added.

Potential architecture:

```text
Docker Compose
      │
 ┌────┼───────────────┐
 │    │               │
 ▼    ▼               ▼
Frontend Backend     MySQL
```

The exact production deployment architecture can be finalized later.

---

# 43. Docker Tasks

Potential files:

```text
Dockerfile
docker-compose.yml
```

Requirements:

* Backend container.
* Frontend container if required.
* MySQL container for local development.
* Environment variables.
* No hardcoded credentials.

Docker should simplify reproducibility, not complicate the application.

---

# 44. Phase 20 — Deployment

Deployment should happen only after local integration is stable.

Deployment checklist:

```text
Backend
 ↓
Database
 ↓
Frontend
 ↓
Environment variables
 ↓
CORS
 ↓
JWT configuration
 ↓
HTTPS
 ↓
API connectivity
```

---

# 45. Production Configuration

Production configuration must not contain:

```text
Hardcoded passwords
JWT secret
Database credentials
Localhost URLs
Development-only debug configuration
```

Use environment variables or deployment secret management.

---

# 46. Deployment Verification

After deployment:

### Test frontend

```text
Open production URL
 ↓
Login
 ↓
Dashboard
 ↓
Converter
```

### Test backend

```text
API available
 ↓
Authentication works
 ↓
Conversion works
```

### Test database

```text
Data persists
```

### Test CORS

```text
Frontend
 ↓
Production Backend
```

must work correctly.

---

# 47. Phase 21 — Documentation

Update:

```text
README.md
```

README should include:

* Project overview.
* Architecture.
* Features.
* Technology stack.
* Setup instructions.
* Environment variables.
* Running backend.
* Running frontend.
* Database setup.
* API endpoints.
* Example HL7 message.
* Example FHIR output.
* Testing instructions.
* Deployment information.

---

# 48. Development Milestones Summary

## Milestone 1 — Project Setup

```text
Repository
Frontend
Backend
Documentation
```

---

## Milestone 2 — Backend Foundation

```text
Spring Boot
Maven
Basic API
Configuration
```

---

## Milestone 3 — Database

```text
MySQL
JPA
Hibernate
Entities
Repositories
```

---

## Milestone 4 — HL7 Parser

```text
Raw HL7
 ↓
ParsedHL7Data
```

---

## Milestone 5 — Validator

```text
ParsedHL7Data
 ↓
Valid / Invalid
```

---

## Milestone 6 — FHIR Mapper

```text
ParsedHL7Data
 ↓
FHIR R4
```

---

## Milestone 7 — End-to-End Backend

```text
POST /api/hl7/convert
 ↓
HL7
 ↓
FHIR
```

---

## Milestone 8 — Security

```text
Login
 ↓
JWT
 ↓
Protected API
```

---

## Milestone 9 — Backend Testing

```text
JUnit
Mockito
MockMvc
Postman
```

---

## Milestone 10 — Frontend

```text
Login
Dashboard
Converter
History
```

---

## Milestone 11 — Integration

```text
React
 ↓
Axios
 ↓
Spring Boot
 ↓
FHIR
```

---

## Milestone 12 — Final Testing

```text
Unit
Integration
End-to-End
Negative Cases
```

---

## Milestone 13 — Deployment

```text
Docker
Environment
Frontend
Backend
Database
```

---

# 49. Dependency Order

The following dependencies should be respected:

```text
Project Setup
      ↓
Backend Foundation
      ↓
Database Foundation
      ↓
Parser
      ↓
Validator
      ↓
FHIR Mapper
      ↓
Conversion Service
      ↓
Conversion API
      ↓
Authentication
      ↓
Backend Testing
      ↓
Frontend Foundation
      ↓
Frontend UI
      ↓
API Integration
      ↓
End-to-End Testing
      ↓
Docker
      ↓
Deployment
```

Some tasks may run in parallel after their dependencies are stable.

For example:

```text
Backend Testing
        │
        ├──────────────→ Frontend UI development
        │
        └──────────────→ Documentation
```

However, frontend API integration should wait until the API contract is stable.

---

# 50. Priority System

Tasks should be classified as:

## P0 — Must Have

Required for MVP.

Examples:

* Spring Boot backend.
* MySQL.
* HL7 parser.
* HL7 validator.
* FHIR mapper.
* HAPI FHIR.
* ADT^A01 support.
* Conversion API.
* Authentication.
* React converter.
* FHIR output.
* Error handling.
* Testing.

## P1 — Important

Useful but can follow MVP.

Examples:

* Conversion history.
* Dashboard statistics.
* Copy JSON.
* Docker.
* Better UI polish.
* Additional FHIR resources.

## P2 — Future

Do not implement unless explicitly approved.

Examples:

* Multiple advanced message types.
* Complex analytics.
* Admin panel.
* Advanced search.
* Event-driven architecture.
* Kafka.
* Redis.
* Kubernetes.
* Microservices.
* Complex monitoring platform.

---

# 51. What NOT to Build During MVP

Do not add:

```text
Kafka
Redis
RabbitMQ
Kubernetes
Elasticsearch
GraphQL
Microservices
MongoDB
WebSockets
Complex event-driven architecture
```

unless a genuine requirement appears.

The project is intentionally a modular monolith.

---

# 52. AI/Vibe Coding Workflow

The AI agent will be used heavily for implementation.

However, implementation must happen in controlled stages.

Correct workflow:

```text
Read Documentation
      ↓
Understand Current State
      ↓
Implement One Phase
      ↓
Run Application
      ↓
Test
      ↓
Fix Errors
      ↓
Confirm Milestone
      ↓
Move to Next Phase
```

Incorrect workflow:

```text
"Build the whole project"
        ↓
Generate thousands of lines
        ↓
Unknown errors
        ↓
Architecture becomes inconsistent
```

---

# 53. AI Agent Task Size

The agent should work in reasonably small implementation units.

Good task:

> Implement the HL7 parser for MSH and PID segments according to HEALTHCARE_MAPPING.md. Add unit tests for valid and invalid messages.

Bad task:

> Build the entire healthcare interoperability backend.

Small tasks make debugging easier.

---

# 54. Before Every Major Task

The AI agent should determine:

1. Which architecture layer is affected?
2. Which existing files are relevant?
3. Which requirements apply?
4. Which dependencies are required?
5. What tests should be added?
6. Does this change affect existing functionality?

---

# 55. After Every Major Task

The AI agent should:

1. Compile/build the affected project.
2. Run relevant tests.
3. Fix compilation errors.
4. Fix failing tests.
5. Verify application startup.
6. Review changed files.
7. Confirm architecture is still respected.

---

# 56. Do Not Hide Errors

The AI agent must not solve errors by:

* Removing required functionality.
* Commenting out broken code.
* Ignoring failing tests.
* Suppressing exceptions.
* Replacing real APIs with fake data.
* Removing validation.
* Removing security.
* Creating duplicate implementations.

The root cause should be fixed.

---

# 57. Dependency Management Rules

Before adding a dependency:

1. Check whether an existing dependency already provides the functionality.
2. Check whether the dependency is genuinely required.
3. Prefer established libraries.
4. Avoid redundant libraries.
5. Keep `pom.xml` and `package.json` clean.

Do not add libraries simply because they are popular.

---

# 58. Database Development Rules

Database schema should evolve according to actual requirements.

Do not create dozens of tables in advance.

Start with the minimum required persistence model.

Every entity must have a clear purpose.

Avoid storing the same healthcare information unnecessarily in multiple tables.

---

# 59. Healthcare Mapping Development Rule

All HL7 → FHIR mappings must be based on:

```text
agent/HEALTHCARE_MAPPING.md
```

The AI agent must not invent mappings casually.

When a mapping is ambiguous:

```text
Stop
 ↓
Review mapping requirements
 ↓
Determine correct behavior
 ↓
Document decision
 ↓
Implement
```

---

# 60. FHIR Development Rule

FHIR resources must be generated using HAPI FHIR.

Do not manually assemble raw FHIR JSON as the primary implementation.

Correct:

```text
ParsedHL7Data
 ↓
HAPI FHIR Patient
 ↓
FHIR JSON
```

---

# 61. Parser Development Rule

Parser and mapper must remain separate.

Correct:

```text
HL7Parser
 ↓
ParsedHL7Data
 ↓
HL7ToFhirMapper
```

Incorrect:

```text
HL7Parser
 ↓
Patient
 ↓
Database
```

---

# 62. Testing Strategy

Testing should happen continuously.

```text
Feature
 ↓
Unit Test
 ↓
Integration Test
 ↓
API Test
 ↓
End-to-End Test
```

Do not wait until the final day to test the complete project.

---

# 63. Recommended Testing Order

For each backend feature:

```text
1. Write/implement feature
2. Compile
3. Unit test
4. Integration test
5. Postman test
6. Continue
```

For frontend:

```text
1. Implement component
2. Run frontend
3. Test interaction
4. Test API
5. Test error state
6. Test responsive layout
```

---

# 64. Definition of Done — Individual Task

A task is Done when:

* Code is implemented.
* Code compiles.
* Relevant tests pass.
* No obvious regression exists.
* Architecture rules are respected.
* No unnecessary dependency was introduced.
* Error handling exists where required.
* Documentation is updated if behavior/architecture changed.

---

# 65. Definition of Done — Backend MVP

Backend MVP is complete when:

```text
✓ Spring Boot starts
✓ MySQL connection works
✓ Authentication works
✓ JWT security works
✓ HL7 ADT^A01 accepted
✓ HL7 parsed
✓ HL7 validated
✓ Patient mapped
✓ Encounter mapped where required
✓ Observation mapped where required
✓ HAPI FHIR generates R4 resources
✓ Required data persisted
✓ Conversion API works
✓ Errors handled
✓ Unit tests pass
✓ Integration tests pass
✓ Postman tests pass
```

---

# 66. Definition of Done — Frontend MVP

Frontend MVP is complete when:

```text
✓ Login works
✓ Protected routes work
✓ Dashboard works
✓ Converter works
✓ HL7 input works
✓ Sample HL7 works
✓ Convert button works
✓ Loading state works
✓ FHIR output displays
✓ Error state works
✓ Clear works
✓ Copy works if included
✓ Logout works
✓ Responsive layout works
✓ API integration works
```

---

# 67. Definition of Done — Complete MVP

The entire MVP is complete when:

```text
User
 ↓
Login
 ↓
Dashboard
 ↓
Converter
 ↓
Paste ADT^A01
 ↓
Convert
 ↓
Spring Boot
 ↓
Parse
 ↓
Validate
 ↓
Map
 ↓
HAPI FHIR
 ↓
Persist
 ↓
Return
 ↓
React
 ↓
FHIR JSON
```

works successfully from beginning to end.

---

# 68. Final Pre-Deployment Checklist

## Architecture

* [ ] Layered architecture maintained.
* [ ] Controller contains no business logic.
* [ ] Parser separated from mapper.
* [ ] Repository accessed through service layer.
* [ ] DTO/entity separation maintained.

## Backend

* [ ] Application starts.
* [ ] Database works.
* [ ] APIs work.
* [ ] Exceptions handled.
* [ ] Validation works.
* [ ] FHIR generation works.

## Security

* [ ] JWT works.
* [ ] Passwords hashed.
* [ ] Secrets externalized.
* [ ] Protected APIs reject unauthenticated users.
* [ ] PHI is not unnecessarily logged.

## Frontend

* [ ] Login works.
* [ ] Navigation works.
* [ ] Converter works.
* [ ] FHIR output readable.
* [ ] Errors readable.
* [ ] Responsive design verified.

## Testing

* [ ] Unit tests.
* [ ] Integration tests.
* [ ] API tests.
* [ ] Negative tests.
* [ ] End-to-end test.

## Deployment

* [ ] Environment variables configured.
* [ ] CORS configured.
* [ ] Production database configured.
* [ ] Backend deployed.
* [ ] Frontend deployed.
* [ ] Production conversion tested.

---

# 69. Final Implementation Order

The AI agent should follow this order unless a documented technical reason requires a change:

```text
01. Read all agent documentation
        ↓
02. Create project structure
        ↓
03. Initialize Git
        ↓
04. Setup Spring Boot
        ↓
05. Setup React
        ↓
06. Setup MySQL
        ↓
07. Configure JPA/Hibernate
        ↓
08. Create persistence foundation
        ↓
09. Implement ParsedHL7Data
        ↓
10. Implement HL7Parser
        ↓
11. Test parser
        ↓
12. Implement HL7Validator
        ↓
13. Test validator
        ↓
14. Implement Patient FHIR mapping
        ↓
15. Implement Encounter mapping
        ↓
16. Implement Observation mapping
        ↓
17. Test FHIR mappings
        ↓
18. Implement HL7Service
        ↓
19. Implement conversion API
        ↓
20. Test complete backend conversion
        ↓
21. Implement authentication
        ↓
22. Secure APIs
        ↓
23. Backend unit/integration testing
        ↓
24. Build React application shell
        ↓
25. Build Login
        ↓
26. Build Dashboard
        ↓
27. Build Converter
        ↓
28. Build FHIR Viewer
        ↓
29. Connect Axios
        ↓
30. Connect authentication
        ↓
31. Connect conversion API
        ↓
32. Implement History if approved
        ↓
33. End-to-end testing
        ↓
34. Bug fixing
        ↓
35. UI/UX polish
        ↓
36. Docker
        ↓
37. Deployment
        ↓
38. README/documentation
        ↓
39. Final project review
```

---

# 70. Final Project Development Flow

The final development lifecycle is:

```text
                 PROJECT DOCUMENTATION
                         │
                         ▼
                  PROJECT SETUP
                         │
                         ▼
                 BACKEND FOUNDATION
                         │
                         ▼
                    DATABASE
                         │
                         ▼
                   HL7 PARSER
                         │
                         ▼
                  HL7 VALIDATOR
                         │
                         ▼
                  FHIR MAPPER
                         │
                         ▼
                 CONVERSION API
                         │
                         ▼
                  AUTH + SECURITY
                         │
                         ▼
                  BACKEND TESTING
                         │
                         ▼
                  REACT FRONTEND
                         │
                         ▼
                 API INTEGRATION
                         │
                         ▼
                 END-TO-END TEST
                         │
                         ▼
                    BUG FIXING
                         │
                         ▼
                    DOCKER
                         │
                         ▼
                   DEPLOYMENT
                         │
                         ▼
                FINAL DOCUMENTATION
```

---

# 71. Source-of-Truth Rules

The following files have specific responsibilities:

```text
PRD.md
    ↓
Why are we building this?
What is the product?

REQUIREMENTS.md
    ↓
What must the system do?

ARCHITECTURE.md
    ↓
How is the system structured?

UI_UX.md
    ↓
How should the frontend behave/look?

DEVELOPMENT_PLAN.md
    ↓
In what order should the system be built?

HEALTHCARE_MAPPING.md
    ↓
How exactly should HL7 fields map to FHIR?
```

If there is a conflict:

```text
Product requirement
        ↓
Technical architecture
        ↓
Implementation detail
```

The AI agent must identify the conflict rather than silently choosing an arbitrary implementation.

---

# 72. Final AI Agent Instruction

Build the application incrementally according to this document.

Do not attempt to implement every feature simultaneously.

At every stage:

```text
Understand
 ↓
Implement
 ↓
Compile
 ↓
Test
 ↓
Verify
 ↓
Continue
```

The highest priority is a correct and explainable end-to-end healthcare interoperability pipeline:

```text
HL7 v2
   ↓
Parse
   ↓
Validate
   ↓
Structured Data
   ↓
Map
   ↓
FHIR R4
   ↓
Persist
   ↓
Return
   ↓
React UI
```

The final application should be production-minded, but the MVP should remain intentionally simple.

Do not introduce enterprise infrastructure unless the actual requirements justify it.

The goal is a clean, modular, secure, testable, and interview-explainable Java Spring Boot healthcare interoperability project.
