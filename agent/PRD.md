# Product Requirements Document (PRD)

## 1. Product Overview

### Product Name

**HL7 v2 → FHIR Healthcare Interoperability Engine**

### Product Type

Healthcare interoperability and data transformation web application.

### Primary Technology

* Frontend: React.js
* Backend: Java + Spring Boot
* Healthcare Standard: HL7 v2 and FHIR R4
* FHIR Library: HAPI FHIR
* Database: MySQL
* Authentication: Spring Security + JWT
* Build Tool: Maven
* Testing: JUnit 5, Mockito, MockMvc, Postman
* Version Control: Git + GitHub
* Containerization: Docker

---

# 2. Problem Statement

Healthcare systems often exchange patient and clinical information using different healthcare data standards.

A legacy healthcare system may produce patient information in **HL7 v2 messages**, while a modern healthcare application or API may require the same information in **FHIR R4 resources**.

Manually converting these messages is:

* time-consuming
* error-prone
* difficult to maintain
* difficult to validate
* unsuitable for repeated integration workflows

The project solves this problem by providing an integration engine that accepts an HL7 v2 message, processes it through a controlled backend pipeline, converts the relevant healthcare data into FHIR R4 resources, persists required data, and returns the resulting FHIR representation to the user.

---

# 3. Product Goal

The goal is to build a working healthcare interoperability engine that demonstrates how a Java/Spring Boot application can transform an HL7 v2 healthcare message into standardized FHIR R4 resources.

The system should demonstrate a realistic integration pipeline:

```text
HL7 v2 Message
      ↓
Receive Request
      ↓
Parse
      ↓
Validate
      ↓
Transform
      ↓
Map HL7 → FHIR
      ↓
Create FHIR R4 Resources
      ↓
Persist Required Data
      ↓
Return FHIR Response
```

The system should be designed with clean separation of responsibilities so that each stage can be independently tested, maintained, and extended.

---

# 4. Product Vision

Build a practical, portfolio-ready healthcare interoperability application that demonstrates:

1. Java backend development
2. Spring Boot REST API development
3. Layered backend architecture
4. HL7 v2 message processing
5. HL7-to-FHIR transformation
6. HAPI FHIR integration
7. FHIR R4 resource creation
8. Database persistence
9. Authentication and authorization
10. Validation and exception handling
11. Automated testing
12. Modern frontend integration

The project should be realistic enough to explain in a technical interview without introducing unnecessary enterprise complexity.

---

# 5. Target Users

## 5.1 Primary User

### Healthcare Integration Developer

A developer who needs to test or demonstrate transformation of HL7 v2 messages into FHIR resources.

The user should be able to:

* authenticate
* enter/paste an HL7 message
* submit the message for processing
* view validation results
* view generated FHIR resources
* inspect conversion details
* view previously processed messages/results where applicable

---

## 5.2 Secondary User

### Healthcare IT / Integration Team Member

A technical user who wants to:

* test HL7 messages
* identify malformed input
* verify HL7-to-FHIR mappings
* inspect generated FHIR output
* review conversion history

---

# 6. Core User Journey

The primary user journey is:

```text
User
 ↓
Login
 ↓
Dashboard
 ↓
Paste HL7 v2 Message
 ↓
Click "Convert"
 ↓
Frontend sends HTTP request using Axios
 ↓
Spring Boot REST Controller
 ↓
Service Layer
 ↓
HL7 Parser
 ↓
HL7 Validator
 ↓
HL7 → FHIR Mapper
 ↓
HAPI FHIR
 ↓
FHIR R4 Resource
 ↓
Persistence Layer
 ↓
MySQL
 ↓
API Response
 ↓
Axios
 ↓
React UI
 ↓
User views FHIR output
```

---

# 7. Core Product Features

## 7.1 User Authentication

The application must provide secure authentication.

Users should be able to:

* register where required by the final implementation
* log in
* receive a JWT
* use the JWT for protected API requests
* log out from the frontend

Backend security should be implemented using:

* Spring Security
* JWT-based authentication

---

# 8. HL7 Message Input

The application must provide an interface where the user can paste an HL7 v2 message.

Example input:

```text
MSH|^~\&|HIS|HOSPITAL|LAB|HOSPITAL|202610071200||ADT^A01|MSG001|P|2.5
PID|1||P1001||Sharma^Rahul||19950510|M
PV1|1|I|WARD1||||1234^Khan^Aamir
```

The interface should provide:

* HL7 input area
* clear/reset action
* convert/process action
* validation feedback
* loading state
* error state

The frontend must not contain the actual HL7 parsing or FHIR mapping business logic.

---

# 9. HL7 Processing

The backend must process the submitted HL7 message using separate responsibilities.

The processing pipeline should conceptually be:

```text
Raw HL7
   ↓
HL7Parser
   ↓
ParsedHL7Data
   ↓
HL7Validator
   ↓
Validated Data
   ↓
HL7ToFhirMapper
   ↓
FHIR Resource
```

Responsibilities must remain separated.

### HL7Parser

Responsible for:

* reading raw HL7 structure
* identifying segments
* extracting required fields
* converting raw values into structured application data

It must not be responsible for:

* database operations
* HTTP response handling
* authentication
* FHIR business mapping

---

### HL7Validator

Responsible for:

* checking required segments
* checking required fields
* validating data formats
* detecting malformed HL7 input
* returning meaningful validation errors

It must not be responsible for:

* database operations
* frontend logic
* authentication

---

### HL7ToFhirMapper

Responsible for:

* deciding which parsed HL7 data maps to which FHIR element
* constructing appropriate FHIR resources
* applying project-defined transformation rules

Example:

```text
PID-3 → Patient.identifier
PID-5 → Patient.name
PID-7 → Patient.birthDate
PID-8 → Patient.gender

PV1 → Encounter

OBX → Observation
```

---

# 10. FHIR Resource Generation

The project will use **FHIR R4**.

HAPI FHIR will be used as the Java implementation/library for working with FHIR resources.

The application should generate appropriate resources from the parsed HL7 message.

Initial core resources:

* Patient
* Encounter

Clinical observation support:

* Observation

The system should not create unnecessary FHIR resources that are not required by the project's supported HL7 message scope.

---

# 11. HAPI FHIR Integration

HAPI FHIR will be responsible for providing Java representations and utilities for FHIR R4 resources.

The application will use HAPI FHIR to:

* create FHIR resource objects
* populate FHIR fields
* serialize FHIR resources
* deserialize FHIR resources where required
* generate valid FHIR JSON representation

The business mapping must remain inside the application's mapping layer.

Conceptually:

```text
HL7ToFhirMapper
        │
        │ decides WHAT maps WHERE
        ▼
HAPI FHIR
        │
        │ provides FHIR resource classes
        │ and serialization utilities
        ▼
FHIR R4 Resource / JSON
```

HAPI FHIR must not be treated as the business-rule engine for HL7-to-FHIR mapping.

---

# 12. Database Persistence

The application must persist required processed data.

The backend will use:

```text
Spring Data JPA
       ↓
Hibernate
       ↓
JDBC
       ↓
MySQL
```

The project must maintain a clear separation between:

* FHIR resource models
* persistence entities
* repositories

FHIR resource objects must not automatically be assumed to be JPA entities.

Where required, the application should use application-specific persistence entities and repositories.

Example conceptual flow:

```text
FHIR Patient
      ↓
Application Persistence Model
      ↓
Repository
      ↓
MySQL
```

The exact database schema will be defined in `REQUIREMENTS.md` and `ARCHITECTURE.md`.

---

# 13. REST API

The backend must expose REST APIs for frontend communication.

The main conversion operation should conceptually follow:

```text
POST /api/hl7/convert
```

Request:

```text
HL7 v2 message
```

Processing:

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
HAPI FHIR
 ↓
Persistence
```

Response:

```text
FHIR JSON + conversion information
```

Exact API contracts, DTOs, HTTP methods, status codes, request/response schemas, and endpoint naming conventions will be defined in `REQUIREMENTS.md`.

---

# 14. Frontend

The frontend will be built using React.js.

Responsibilities of React:

* display application UI
* collect user input
* call backend APIs
* display loading states
* display validation/errors
* display FHIR output
* display conversion history where implemented
* manage authentication state

Axios will be used as the HTTP client.

Conceptual flow:

```text
React Component
      ↓
Axios
      ↓
Spring Boot REST API
```

React must not contain:

* HL7 parsing logic
* HL7 validation rules
* HL7-to-FHIR mapping rules
* database logic
* FHIR business transformation logic

---

# 15. Error Handling

The application must provide consistent error handling.

Possible errors include:

* malformed HL7 message
* missing required segment
* missing required field
* invalid date
* invalid patient data
* unsupported message type
* FHIR conversion failure
* database failure
* unauthorized request
* internal server error

Errors should be handled centrally where appropriate using a global exception-handling mechanism.

Example response:

```json
{
  "status": 400,
  "error": "Invalid HL7 message",
  "message": "PID segment is missing"
}
```

The frontend should display meaningful user-friendly error messages instead of raw backend exceptions.

---

# 16. Validation

Validation must occur before attempting FHIR conversion.

Conceptually:

```text
Receive HL7
      ↓
Parse
      ↓
Validate
      ↓
Valid?
 ┌────┴────┐
NO         YES
↓           ↓
Error       Mapping
            ↓
         FHIR
```

Invalid input must not proceed to the normal FHIR conversion pipeline.

---

# 17. Security Requirements

The application must implement basic production-style security.

Required:

* Spring Security
* JWT authentication
* protected backend endpoints
* authorization header
* password hashing
* validation of authenticated requests
* secure handling of credentials

Subsequent API requests should conceptually use:

```http
Authorization: Bearer <JWT>
```

The project should not store plain-text passwords.

Advanced enterprise security mechanisms are outside the initial project scope.

---

# 18. Testing Requirements

The project must include testing at multiple levels.

### Unit Testing

Use:

* JUnit 5
* Mockito

Primary units to test:

* HL7Parser
* HL7Validator
* HL7ToFhirMapper
* Service layer
* validation logic

### Controller Testing

Use:

* MockMvc

Test:

* valid requests
* invalid requests
* authentication failures
* expected HTTP status codes
* response structures

### API Testing

Use:

* Postman

Test the complete API flow:

```text
Request
 ↓
Backend
 ↓
Processing
 ↓
FHIR Response
```

---

# 19. Non-Functional Goals

## Performance

For normal-sized HL7 messages, the conversion should complete quickly enough for interactive use.

The application does not initially target high-volume enterprise message processing.

---

## Maintainability

The application must use clear separation of responsibilities.

Avoid putting all logic into:

* Controller
* Service
* one giant HL7 class

The following responsibilities should remain independently identifiable:

```text
Controller
Service
Parser
Validator
Mapper
FHIR Service / Utility
Repository
Exception Handler
```

---

## Scalability

The initial application should use a modular monolithic architecture.

Do not introduce microservices unless a later requirement genuinely justifies them.

---

# 20. MVP Scope

The Minimum Viable Product must support:

### Authentication

* Login
* JWT authentication
* Protected conversion API

### HL7

* Accept HL7 v2 message
* Support the selected initial message type, primarily ADT^A01
* Parse required segments
* Validate required data

### FHIR

* Convert patient information into FHIR Patient
* Convert encounter information into FHIR Encounter
* Support Observation conversion where the selected message contains relevant OBX data
* Return FHIR JSON

### Backend

* Spring Boot REST API
* Layered architecture
* Exception handling
* DTO validation
* Persistence

### Database

* Store required processing/patient/conversion information according to the final schema

### Frontend

* Login
* Dashboard
* HL7 input
* Convert button
* Loading state
* Error state
* FHIR output display

### Testing

* Unit tests
* Controller tests
* API testing

---

# 21. Post-MVP Scope

Features that may be considered after the MVP:

* support for additional HL7 message types
* additional FHIR resources
* conversion history improvements
* advanced filtering/search
* downloadable FHIR output
* richer integration monitoring
* advanced audit functionality
* Docker-based deployment
* production deployment improvements

These must not interfere with completing the MVP.

---

# 22. Explicitly Out of Scope for Initial Version

The initial implementation should NOT introduce unnecessary complexity such as:

* Kafka
* Redis
* Kubernetes
* microservices
* complex event-driven architecture
* MongoDB
* large-scale distributed processing
* advanced healthcare terminology services
* complex enterprise interoperability infrastructure
* unnecessary third-party integrations

The objective is a strong, understandable, working interoperability engine rather than an over-engineered enterprise platform.

---

# 23. Project Architecture Principle

The application must follow a clean layered architecture.

```text
                    React Frontend
                          │
                        Axios
                          │
                          ▼
                 REST Controller
                          │
                          ▼
                    Service Layer
                          │
              ┌───────────┼───────────┐
              ▼           ▼           ▼
          HL7 Parser  Validator     Mapper
                                      │
                                      ▼
                                  HAPI FHIR
                                      │
                                      ▼
                                FHIR Resources
                                      │
                                      ▼
                                Persistence
                                      │
                                  Repository
                                      │
                                      ▼
                                    MySQL
```

Each component must have one clear primary responsibility.

---

# 24. Core Design Principle

The system must follow:

> **Separation of concerns over convenience.**

For example, the application must NOT implement:

```java
Controller
    ↓
parse HL7
    ↓
validate HL7
    ↓
create FHIR
    ↓
save database
    ↓
return response
```

inside one controller method.

Instead:

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
FHIR processing
   ↓
Persistence
```

This makes the application:

* easier to test
* easier to debug
* easier to extend
* easier to maintain
* easier to explain in technical interviews

---

# 25. Success Criteria

The MVP will be considered successful when a user can:

1. Log into the application.
2. Paste a supported HL7 v2 message.
3. Submit the message.
4. The frontend sends the request using Axios.
5. The Spring Boot controller receives the request.
6. The service layer orchestrates processing.
7. The HL7 parser extracts the required data.
8. The validator validates the extracted data.
9. The mapper maps the data to appropriate FHIR resources.
10. HAPI FHIR creates the FHIR R4 resource representation.
11. Required data is persisted.
12. The backend returns a structured response.
13. React receives the response.
14. The user can view the generated FHIR output.
15. Invalid HL7 input produces a meaningful error.
16. Unauthorized API requests are rejected.
17. Automated tests cover the core processing logic.

---

# 26. Acceptance Criteria

### AC-01 — HL7 Input

Given a supported HL7 v2 message,

when the user submits it,

then the backend must receive the complete message.

---

### AC-02 — Parsing

Given a valid supported HL7 message,

the parser must extract the required fields into structured application data.

---

### AC-03 — Validation

Given an invalid or incomplete message,

the validator must reject it before normal FHIR conversion.

---

### AC-04 — Mapping

Given valid parsed HL7 data,

the mapper must apply the project's defined HL7-to-FHIR mapping rules.

---

### AC-05 — FHIR

Given valid mapped data,

the system must generate the corresponding FHIR R4 resource representation using HAPI FHIR.

---

### AC-06 — Persistence

Given successfully processed data,

the system must persist the required information according to the approved database design.

---

### AC-07 — API Response

Given successful processing,

the backend must return a structured HTTP response containing the generated FHIR output and relevant conversion information.

---

### AC-08 — Error Response

Given an invalid request,

the backend must return a meaningful HTTP error response instead of an unhandled exception.

---

### AC-09 — Authentication

Given a protected endpoint,

a request without valid authentication must be rejected.

---

### AC-10 — Frontend

Given a successful conversion,

the frontend must display the resulting FHIR output in a readable format.

---

# 27. Definition of MVP Done

The MVP is considered complete only when:

* backend builds successfully
* frontend builds successfully
* database connection works
* authentication works
* supported HL7 message can be submitted
* parser works
* validation works
* mapping works
* HAPI FHIR integration works
* FHIR output is generated
* required persistence works
* API returns expected responses
* frontend displays results
* invalid input is handled correctly
* core unit tests pass
* controller/API tests pass
* project documentation is updated
* project can be run from a clean environment using the documented setup steps

---

# 28. Documentation Dependencies

The following project documents must remain consistent with this PRD:

```text
PRD.md
   │
   ├── REQUIREMENTS.md
   │
   ├── ARCHITECTURE.md
   │
   ├── UI_UX.md
   │
   ├── DEVELOPMENT_PLAN.md
   │
   └── HEALTHCARE_MAPPING.md
```

`PRD.md` defines **what the product is and what the MVP must achieve**.

The other documents define the detailed requirements, technical implementation, UI, development sequence, and healthcare mapping rules.

---

# 29. AI Agent Instruction

When implementing this project, the AI coding agent must treat this PRD as a product-level source of truth.

The agent must:

* follow the defined MVP scope
* follow separation of concerns
* avoid unnecessary technologies
* not introduce unrequested architecture
* not change healthcare mapping rules without updating the relevant documentation
* not place business logic in controllers
* not place HL7 parsing logic in React
* not mix HL7 parsing, validation, mapping, and persistence into one class
* keep backend and frontend responsibilities separated
* implement the project incrementally according to `DEVELOPMENT_PLAN.md`
* verify each major milestone before proceeding
* maintain consistency across all files in the `agent/` directory

If a technical implementation decision is unclear, the agent should prefer the simplest maintainable design that satisfies the documented requirements rather than introducing unnecessary enterprise complexity.
