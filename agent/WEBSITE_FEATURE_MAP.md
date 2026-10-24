# Healthcare Interoperability Engine — Website Feature Map & Manual Testing Guide

> **Document Version:** 1.0.0  
> **Target Audience:** QA Engineers, Manual Testers, Clinical IT Auditors, Security Evaluators, Software Engineers  
> **Application:** Healthcare Interoperability Gateway (HL7 v2.5 / v2.3 to FHIR R4 Translation Engine)  
> **Frontend:** React 18, Vite, React Router v6, Axios, Vanilla CSS  
> **Backend:** Spring Boot 3.3.4, Java 17, Spring Security 6, HAPI FHIR R4 (7.4.0), Hibernate 6 / Spring Data JPA  
> **Database:** MySQL 8.0 (`hl7_fhir_engine`), Flyway Migrations (V1, V2)

---

## 1. Application Overview

### 1.1 What the Application Does
The **Healthcare Interoperability Engine** is a secure, HIPAA-compliant clinical integration platform designed to bridge legacy Hospital Information Systems (HIS / EHR / LIS) with modern FHIR-native healthcare ecosystems. 

Key capabilities:
1. **Clinical Ingestion & Validation:** Ingests pipe-delimited HL7 v2 messages (specifically `ADT^A01` inpatient admission events) and strictly validates message headers (`MSH`), patient demographics (`PID`), encounter metadata (`PV1`), and clinical laboratory observations (`OBX`).
2. **Standard-Compliant FHIR R4 Translation:** Translates validated HL7 v2 data into standard HL7 FHIR R4 JSON resources (`Patient`, `Encounter`, `Observation`) bundled into a FHIR `collection` Bundle.
3. **Multi-Tenant Clinical Persistence:** Automatically persists patient master records, hospital visits/encounters, lab observations, and conversion audit logs in MySQL with strict hospital tenant isolation.
4. **Outbound FHIR Dispatch & Resilience:** Dispatches generated FHIR R4 bundles to configured external FHIR servers or the embedded local Mock FHIR Server. Features SSRF protection, AES-256-GCM token encryption, bounded exponential backoff retries (3 attempts with jitter), and scheduled background retry polling.
5. **Auditing & Compliance:** Records an immutable audit trail of authentication events, conversion successes/failures, destination dispatches, credential changes, and tenant access attempts without recording PHI/passwords.

### 1.2 User Roles & Permissions Matrix
The system enforces Role-Based Access Control (RBAC) and Multi-Tenant Isolation using stateless JWT tokens:

| Capability / Section | Platform Admin (`ADMIN`, `hospital_id = null`) | Hospital Admin (`ADMIN`, `hospital_id = X`) | Clinical User (`USER`, `hospital_id = X`) | Unauthenticated User |
| :--- | :---: | :---: | :---: | :---: |
| **Login (`/login`)** | Yes | Yes | Yes | Yes |
| **System Health (`/api/health`)** | Yes | Yes | Yes | Yes |
| **Dashboard (`/dashboard`)** | Global / Multi-Tenant View | Tenant-Scoped View | Tenant-Scoped View | Redirect to `/login` |
| **Convert HL7 (`/convert`)** | Yes | Yes (scoped to tenant) | Yes (scoped to tenant) | Redirect to `/login` |
| **Conversion History (`/history`)** | All Hospitals | Own Hospital Only | Own Hospital Only | Redirect to `/login` |
| **Clinical Records (`/patients`)** | Accessible | Own Hospital Only | Own Hospital Only | Redirect to `/login` |
| **Manage Destinations (`/destinations`)** | Full Management | Own + Platform-wide | Own + Platform-wide | Redirect to `/login` |
| **Mock FHIR Inspector** | Full Ingest & Clear | Full Ingest & Clear | Full Ingest & Clear | Full Ingest & Clear |
| **System Audit Logs (`/audit-logs`)** | All Logs / Filter by Tenant | Own Hospital Logs | Own Hospital Logs | Redirect to `/login` |
| **Hospital Management (`/hospitals`)** | Full CRUD & Provisioning | View Own Tenant | Denied (Redirect `/dashboard`) | Redirect to `/login` |
| **Register Users (`/users/create`)** | Any Hospital or Platform Admin | Own Hospital Users Only | Denied (Redirect `/dashboard`) | Redirect to `/login` |

### 1.3 System Interaction Architecture
```
  ┌─────────────────────────────────────────────────────────────┐
  │                 React 18 Frontend SPA                       │
  │  (Vite Dev Server :5173 / Nginx :80)                       │
  └──────────────────────────────┬──────────────────────────────┘
                                 │ HTTP REST (JSON / Bearer JWT)
                                 ▼
  ┌─────────────────────────────────────────────────────────────┐
  │             Spring Boot 3.3.4 Backend (:8080)               │
  │  ┌────────────────────┐   ┌───────────────────────────────┐ │
  │  │ Spring Security 6  │   │  TenantSecurityService (RBAC) │ │
  │  └─────────┬──────────┘   └───────────────┬───────────────┘ │
  │            │                              │                 │
  │  ┌─────────▼──────────┐   ┌───────────────▼───────────────┐ │
  │  │  HL7 Parser &      │   │  HAPI FHIR R4 Model &         │ │
  │  │  Validator         │──▶│  Serialization Engine         │ │
  │  └────────────────────┘   └───────────────┬───────────────┘ │
  │                                           │                 │
  │  ┌────────────────────┐   ┌───────────────▼───────────────┐ │
  │  │ Delivery & Retry   │◀──│  Spring Data JPA Repositories │ │
  │  │ Scheduler Service  │   │  (Audit, Patient, Delivery)   │ │
  │  └─────────┬──────────┘   └───────────────┬───────────────┘ │
  └────────────┼──────────────────────────────┼─────────────────┘
               │ Outbound FHIR                │ HikariCP / JDBC
               ▼                              ▼
  ┌───────────────────────────┐  ┌──────────────────────────────┐
  │ External FHIR Server /    │  │ MySQL 8.0 (hl7_fhir_engine)  │
  │ Embedded Mock FHIR Server │  │ Flyway Schemas V1 & V2       │
  └───────────────────────────┘  └──────────────────────────────┘
```

---

## 2. All Frontend Pages

### 2.1 Login Page
- **Route:** `/login`
- **Purpose:** Authenticates clinical users and administrators to obtain a stateless signed Bearer JWT token.
- **Visible Elements:**
  - Centered brand card with healthcare cross icon, title *"HL7 → FHIR Engine"*, and subtitle *"Healthcare Interoperability Gateway"*.
  - Error banner (`ErrorMessage` alert) with dismiss button.
  - Username input field (`id="username"`).
  - Password input field (`id="password"`) with an inline visibility toggle eye button (`showPassword` / `hidePassword`).
  - Submit Button: *"Sign In to Interop Engine"* with spinner loading state.
  - Development hint box (only visible in development mode): Displays demo credentials hint.
- **Button & Form Behavior:**
  - **Eye toggle button:** Toggles password input type between `password` and `text`.
  - **Submit form:** 
    - Validates that username and password are non-empty. If empty, displays *"Please enter both username and password."* without sending an API call.
    - Sends `POST /api/auth/login` with `{ username, password }`.
    - On success: Saves JWT token to `localStorage.getItem('hl7_auth_token')`, parses user claims (username, role), saves profile to `localStorage.getItem('hl7_auth_user')`, and redirects via `navigate('/dashboard')`.
    - On failure: Displays the server's user-friendly error message (e.g. *"Bad credentials"* or sanitized network error).
- **APIs Called:** `POST /api/auth/login`
- **Implementation Status:** **Fully Implemented**.

---

### 2.2 Dashboard Page
- **Route:** `/dashboard` (also default index `/` redirects here)
- **Purpose:** Primary real-time operational monitor displaying pipeline conversion statistics, delivery transmission rates, tenant badges, recent audit records, recent deliveries, and backend/database health indicators.
- **Visible Elements:**
  - **Top Banner:** Heading *"Clinical Interoperability Dashboard"*, tenant badge pill displaying hospital name and code if tenant-associated, subtitle detailing tenant scope, *"Refresh"* button with spin animation, *"Register User"* button (ADMIN role only), and *"Start New Conversion"* button.
  - **Error Alert Banner:** Appears with a *"Retry"* button if metric retrieval fails.
  - **Conversion Pipeline KPI Grid (5 cards):**
    - *Total Conversions* (count + success percentage).
    - *Successful Conversions* (count).
    - *Failed Conversions* (count).
    - *Patients Processed* (count of unique MRNs).
    - *Observations Processed* (count of lab/vital observation records).
  - **FHIR Delivery & Integration KPI Grid (5 cards):**
    - *Total FHIR Deliveries* (count + delivery rate percentage).
    - *Successful Deliveries* (count).
    - *Failed Deliveries* (count).
    - *In-Flight / Retrying* (count of active retry + pending deliveries).
    - *FHIR Destinations* (enabled count vs. total configured count).
  - **Recent Conversions Card:** Table displaying Control ID, Patient ID, Message Type, Status badge, Timestamp, and a *"Details"* link leading to `/history`.
  - **Recent FHIR Deliveries Card:** Table displaying Destination Name, Status badge, HTTP Code pill, Attempt count (e.g., `1 / 3`), Timestamp, and a *"View in History"* link.
  - **Engine Health & Specs Side Card:** Real-time status badges for Integration Engine (`OPERATIONAL` / `DEGRADED`), Database Connectivity (`UP` / `DOWN`), tenant isolation mode (`Strict Multi-Tenant (HIPAA BAA)`), supported HL7 message (`ADT^A01`), HL7 versions (`v2.5 / v2.3`), and FHIR version (`FHIR R4 v4.0.1`).
- **Button & Form Behavior:**
  - **"Refresh" button:** Triggers `loadStats(true)`, re-fetching metrics and health probes with a rotating spinner.
  - **"Start New Conversion" button:** Navigates to `/convert`.
  - **"Register User" button (Admin only):** Navigates to `/users/create`.
  - **"View All History →" / "Details" links:** Navigates to `/history`.
  - **"Manage Destinations →" link:** Navigates to `/destinations`.
- **APIs Called:**
  - `GET /api/dashboard/stats`
  - `GET /api/health`
- **Implementation Status:** **Fully Implemented**.

---

### 2.3 HL7-to-FHIR Convert Page
- **Route:** `/convert`
- **Purpose:** Interactive translation console allowing clinical engineers to paste raw pipe-delimited HL7 v2 messages, validate clinical segments, view generated FHIR R4 bundles, and directly dispatch the output to a destination.
- **Visible Elements:**
  - **Page Header:** Translation console overview and supported segments explanation.
  - **Left Panel (HL7 v2 Message Input):**
    - Header actions: *"Load Example"* button, *"Clear"* button.
    - Large monospace textarea pre-populated with a valid synthetic sample (`SAMPLE_HL7_MESSAGE`).
    - Segment counter showing number of detected segments (e.g., *"5 segments detected"*).
    - Submit Button: *"Convert to FHIR R4"* with loading state.
  - **Right Panel (Conversion Results & FHIR Output):**
    - *Empty state (when no conversion performed):* Icon, prompt, and *"Load Synthetic ADT^A01 Sample"* button.
    - *Metadata strip (after conversion):* Status badge (`SUCCESS` / `FAILED`), Conversion ID, Control ID, Message Type, Patient ID, Encounter ID, and Observation Count.
    - *FHIR Bundle Viewer (`FHIRViewer` component):* Formatted JSON tree with syntax highlighting, copy-to-clipboard button, and collapsible tree view.
    - *Dispatch to Destination Server Card:*
      - Target destination dropdown selector listing enabled destinations.
      - *"Send to Destination"* button with icon and spinner.
      - Delivery result banner showing destination name, status, HTTP status pill, timestamp, and response summary.
      - Collapsible *"Show past delivery attempts for this conversion"* toggle if multiple attempts exist.
- **Button & Form Behavior:**
  - **"Load Example" button:** Replaces textarea content with a standard ADT^A01 HL7 message containing MSH, PID, PV1, and OBX segments. Clears errors.
  - **"Clear" button:** Wipes textarea input, results, errors, and delivery state.
  - **"Convert to FHIR R4" button:**
    - Validates textarea is not blank. If blank, shows *"HL7 message is required. Please paste or enter an HL7 message to convert."*
    - Calls `POST /api/hl7/convert` with `{ hl7Message }`.
    - On success: Displays metadata chips and renders the FHIR R4 collection Bundle in the viewer.
    - On validation error (e.g., unsupported message type `ORM^O01` or missing `PID-3`): Shows a red `ErrorMessage` alert banner with the exact parser/validator error.
  - **"Send to Destination" button:**
    - Prompts browser confirmation dialog: *"Confirm Delivery: Send generated FHIR R4 output (Conversion #X) to [Destination]?"*.
    - If confirmed: Calls `POST /api/hl7/conversions/{id}/send` with `{ destinationId }`.
    - Updates inline delivery result banner with status (`SUCCESS`, `RETRYING`, or `FAILED`) and HTTP code.
- **APIs Called:**
  - `GET /api/destinations`
  - `POST /api/hl7/convert`
  - `POST /api/hl7/conversions/{id}/send`
  - `GET /api/hl7/conversions/{id}/deliveries`
- **Implementation Status:** **Fully Implemented**.

---

### 2.4 Conversion History Page
- **Route:** `/history`
- **Purpose:** Audit log of all historical conversions performed within the user's hospital tenant, allowing inspection of original HL7 messages, generated FHIR bundles, and delivery dispatch history with manual retry capability.
- **Visible Elements:**
  - **Header:** Title, subtitle, and *"Refresh"* button.
  - **Filter Toolbar Card:**
    - Search text input (searches Patient ID, Control ID, Encounter ID, Message Type, or Conversion ID).
    - Status filter buttons: `ALL`, `SUCCESS`, `FAILED`.
  - **History Table Card:**
    - Table columns: Conv. ID, Control ID, Message Type, Patient ID, Encounter ID, Obs Count, Status badge, Timestamp, and Action buttons (*"Inspect"* and *"Send FHIR"*).
    - Empty state when no records exist or search yields no matches.
  - **Record Inspection Modal:**
    - Modal header with status badge and record ID.
    - Metadata attribute grid: Patient ID, Encounter ID, Message Type, Observations, Timestamp, Status.
    - Original HL7 message raw view (scrollable preformatted text).
    - Generated FHIR R4 viewer (`FHIRViewer`).
    - *Destination Dispatch & Delivery Audit section:*
      - Dropdown of configured destinations.
      - *"Dispatch Now"* button.
      - Delivery attempts list showing destination name, HTTP code, attempt counter (`Attempt X / Y`), timestamp, response summary, and error message.
      - If delivery is in `RETRYING` state: Displays a pulsating animated notice with the exact scheduled time of the next automatic retry.
      - If delivery is `FAILED` or eligible: Displays an inline *"Retry"* button.
- **Button & Form Behavior:**
  - **"Inspect" button:** Opens the modal for the selected record and fetches delivery history.
  - **"Send FHIR" button:** Opens the modal and auto-scrolls down to the Destination Dispatch section.
  - **"Dispatch Now" button:** Triggers confirmation prompt and calls `POST /api/hl7/conversions/{id}/send`.
  - **"Retry" delivery button:** Calls `POST /api/hl7/deliveries/{deliveryId}/retry`, updates attempt status immediately, and refetches delivery attempts.
- **APIs Called:**
  - `GET /api/hl7/history`
  - `GET /api/destinations`
  - `GET /api/hl7/conversions/{id}/deliveries`
  - `POST /api/hl7/conversions/{id}/send`
  - `POST /api/hl7/deliveries/{id}/retry`
- **Implementation Status:** **Fully Implemented**.

---

### 2.5 FHIR Destinations & Mock Server Page
- **Route:** `/destinations`
- **Purpose:** Administration of outbound FHIR endpoints (external EHRs, HIEs, FHIR repositories) and inspection of the built-in Mock FHIR Server.
- **Visible Elements:**
  - **Tabs Navigation:**
    - `Destinations (N)` tab.
    - `Mock FHIR Inspector (M)` tab.
  - **Tab 1: Destinations View:**
    - Metric summary cards: *Configured Destinations*, *Active Destinations*, *Platform & Mock Destinations*.
    - Search input and Status dropdown filter (`All Statuses`, `Enabled Only`, `Disabled Only`).
    - Destinations Table with columns: Destination Name (with embedded mock pill), Base URL, Version (`R4`), Authentication (`Bearer Token` / `None`, with `🔒 Token Set` badge), Scope (`Platform-wide` or `Hospital-specific`), Status badge, and Actions.
    - Action icon buttons per row:
      - *Test Connection* (probes `{baseUrl}/metadata` with live latency ms and server version, without sending patient data).
      - *View Deliveries* (opens modal with transmission log).
      - *Edit Destination* (opens edit modal).
      - *Toggle Status* (enables/disables destination immediately).
      - *Delete Destination* (prompts confirmation and deletes record).
  - **Tab 2: Mock FHIR Inspector View:**
    - Embedded Mock Server banner explaining functionality (`/api/mock-fhir/Bundle`, in-memory buffer up to 100 messages).
    - Actions: *"Refresh Feed"*, *"Clear Buffer"*.
    - Messages list showing received resources with HTTP 201 status, tracking ID, timestamp, summary, auth header verification (`🔒 Auth Header: Bearer ••••••••`), and *"Inspect Raw Payload"* button.
  - **Modals:**
    - *Create/Edit Destination Modal:* Input fields for Name, Base URL, FHIR Version dropdown (`R4`), Auth Type dropdown (`NONE`, `BEARER_TOKEN`), Bearer Token password input with eye toggle, and Enable checkbox. For new destinations, includes a *"⚡ Fill Local Mock Preset"* quick-fill button.
    - *Delivery History Modal:* Lists historical delivery attempts for that specific destination.
    - *Raw Mock Payload Inspection Modal:* Displays syntax-formatted JSON payload with a *"📋 Copy JSON"* button.
- **Button & Form Behavior:**
  - **"⚡ Fill Local Mock Preset" button:** Auto-populates `Name: Local Mock FHIR Server`, `Base URL: http://localhost:8080/api/mock-fhir`, `Auth: NONE`.
  - **"Test Connection" button:** Calls `GET /api/destinations/{id}/test`. Renders an inline result banner showing latency (ms), HTTP code, and server software.
  - **"Clear Buffer" button:** Calls `DELETE /api/mock-fhir/received` after confirmation.
- **APIs Called:**
  - `GET /api/destinations`
  - `POST /api/destinations`
  - `PUT /api/destinations/{id}`
  - `PATCH /api/destinations/{id}/status`
  - `DELETE /api/destinations/{id}`
  - `GET /api/destinations/{id}/deliveries`
  - `GET /api/destinations/{id}/test`
  - `GET /api/mock-fhir/received`
  - `DELETE /api/mock-fhir/received`
- **Implementation Status:** **Fully Implemented**.

---

### 2.6 System Audit Logs Page
- **Route:** `/audit-logs`
- **Purpose:** Comprehensive compliance and security audit viewer for tracking HIPAA-relevant events (authentication successes/failures, conversions, dispatches, retries, destination updates, user/hospital creation).
- **Visible Elements:**
  - **Header:** Title, subtitle, and *"Refresh Logs"* button.
  - **Filter Card:**
    - Search input (searches actor username, resource ID, or details).
    - Event Action dropdown (20 distinct action categories).
    - Outcome filter pills: `ALL`, `SUCCESS`, `FAILURE`.
    - Resource Type dropdown (`ALL`, `SECURITY`, `CONVERSION`, `FHIR_DELIVERY`, `DESTINATION`, `USER`, `HOSPITAL`).
    - Date range pickers: *From Date* and *To Date*.
    - *"Apply Filters"* and *"Reset"* buttons.
  - **Audit Logs Table:**
    - Columns: Log ID, Timestamp, Actor Username/ID, Tenant/Hospital Badge, Action Badge (color-coded), Resource Type & ID, Outcome Badge, Details, and *"Inspect"* button.
    - Pagination controls: Records-per-page selector (`10`, `20`, `50`), Previous/Next buttons, and current page info.
  - **Audit Record Inspection Modal:**
    - Detailed grid showing Actor, Tenant, Action, ISO Timestamp, Correlation ID, Outcome, and Full Details text.
    - HIPAA Privacy guarantee note confirming sensitive credentials and PHI are never logged.
- **Button & Form Behavior:**
  - **"Apply Filters" button:** Re-queries logs starting from page 0.
  - **"Reset" button:** Clears all filters and reloads default log view.
  - **"Inspect" button:** Opens detailed audit entry modal.
- **APIs Called:**
  - `GET /api/audit-logs` (with pagination and filter query params)
  - `GET /api/audit-logs/{id}`
- **Implementation Status:** **Fully Implemented**.

---

### 2.7 Clinical Records Page
- **Route:** `/patients`
- **Purpose:** Master-detail explorer for hospital patients, encounters/visits, and lab/vital observations ingested from converted HL7 messages.
- **Visible Elements:**
  - **Left Column (Patient Registry):**
    - Search input (searches MRN, patient name, phone).
    - Scrollable list of patient cards showing full name, MRN, gender, and date of birth.
  - **Right Column (Patient Clinical Detail View):**
    - Card header with patient name, MRN, and hospital tenant scope.
    - Tab bar: *Demographics*, *Encounters (N)*, *Observations (M)*.
    - *Demographics Tab:* Grid showing MRN, Full Legal Name, DOB, Administrative Gender, Phone Number, Residential Address, and Ingested Timestamp.
    - *Encounters Tab:* Table listing Encounter ID, Patient Class badge (`INPATIENT`, `EMERGENCY`, `OUTPATIENT`), Location, Admit Time, Discharge Time, and Attending Doctor.
    - *Observations Tab:* Table listing Code/Test, Value Type, Observed Value, Units, Reference Range, Abnormal Flag badge (`NORMAL` / `ABNORMAL`), Status badge, and Recorded Timestamp.
- **Button & Form Behavior:**
  - **Clicking a patient card:** Loads patient details, encounters, and observations via parallel backend calls, defaulting to the Demographics tab.
  - **Switching tabs:** Toggles between demographics, encounter visit history, and lab test results.
- **APIs Called:**
  - `GET /api/patients`
  - `GET /api/patients/{id}`
  - `GET /api/patients/{id}/encounters`
  - `GET /api/patients/{id}/observations`
- **Implementation Status:** **Fully Implemented**.

---

### 2.8 Hospital Management Page
- **Route:** `/hospitals` (Admin Only: guarded by `<ProtectedRoute requiredRole="ADMIN">`)
- **Purpose:** Multi-tenant facility management allowing administrators to register new hospitals, update hospital metadata, and view active/inactive healthcare facilities.
- **Visible Elements:**
  - **Header:** Breadcrumbs, title, and *"Register Hospital"* button.
  - **KPI Cards:** *Total Hospitals*, *Active Facilities*, *Inactive / Disabled*.
  - **Toolbar:** Search input (name, code, email, address), Status filter dropdown (`ALL`, `ACTIVE`, `INACTIVE`), and *"Refresh"* button.
  - **Hospitals Table:**
    - Columns: Hospital Name & Address, Code pill (e.g. `STJUDE_01`), Status badge, Contact Email, Phone, Registered Date, and Actions (*"View"*, *"Edit"*).
  - **Register / Edit Hospital Modal:**
    - Fields: Hospital Name (required), Unique Hospital Code (required, uppercase alphanumeric), Contact Email, Contact Phone, Facility Address, and Enabled checkbox.
- **Button & Form Behavior:**
  - **"Register Hospital" button:** Opens creation modal with clean form.
  - **"Edit" button:** Opens modal pre-populated with existing hospital data.
  - **"View" button:** Shows details.
  - **Modal Submit:**
    - Validates required fields (Name ≥ 2 chars, Code alphanumeric).
    - Calls `POST /api/hospitals` or `PUT /api/hospitals/{id}`.
    - Refreshes hospital list and shows success alert banner.
- **APIs Called:**
  - `GET /api/hospitals`
  - `POST /api/hospitals`
  - `PUT /api/hospitals/{id}`
- **Implementation Status:** **Fully Implemented**.

---

### 2.9 User Registration Page
- **Route:** `/users/create` (also `/admin/users/create` redirects here) (Admin Only: guarded by `<ProtectedRoute requiredRole="ADMIN">`)
- **Purpose:** Provisions new clinical users and administrators with optional hospital tenant association and BCrypt password encryption.
- **Visible Elements:**
  - **Header:** Breadcrumbs, title, and subtitle.
  - **Account Information Form Card:**
    - Username input (required, alphanumeric, ≥ 3 chars).
    - Assigned Hospital Tenant dropdown (lists active hospitals; platform admins can select none for platform-level account).
    - Account Role selector: Radio cards for *Clinical User (`USER`)* and *System Administrator (`ADMIN`)*.
    - Password & Confirm Password inputs with eye toggle buttons (required, ≥ 8 chars).
    - Submit Button: *"Create Account"* with spinner.
    - Reset Button: *"Reset"*.
  - **HIPAA Security Notice Column:** Information card detailing BCrypt hashing, hospital tenant association, RBAC, and stateless JWT handling.
- **Button & Form Behavior:**
  - **"Create Account" submit:**
    - Validates username length and characters, password length (≥ 8), and matching confirmation password.
    - Calls `POST /api/users` with `{ username, password, role, hospitalId }`.
    - On success: Clears form fields and shows green success banner displaying created user, assigned role, and hospital tenant name.
    - On duplicate username error: Shows error alert banner.
  - **"Reset" button:** Clears all input fields and resets role to `USER`.
- **APIs Called:**
  - `GET /api/hospitals` (to populate hospital dropdown)
  - `POST /api/users`
- **Implementation Status:** **Fully Implemented**.

---

### 2.10 Not Found Page
- **Route:** `*` (any unmatched URL inside the application layout)
- **Purpose:** Friendly 404 fallback page.
- **Visible Elements:**
  - `404` error code callout.
  - Title *"Page Not Found"*.
  - Description *"The requested route does not exist in the Healthcare Interoperability Engine console."*
  - Button *"Return to Dashboard"* (navigates to `/dashboard`).
- **Implementation Status:** **Fully Implemented**.

---

## 3. Complete User Workflows

### 3.1 Login and Logout Workflow
1. Navigate to `http://localhost:5173/login`.
2. Enter username (e.g. `admin`) and password (e.g. `admin123`).
3. Click *"Sign In to Interop Engine"*.
4. The client dispatches `POST /api/auth/login`. The server verifies credentials with `BCryptPasswordEncoder`, creates a JWT token signed with HMAC-SHA256 containing `sub: admin` and `role: ROLE_ADMIN`, and logs `LOGIN_SUCCESS` to `audit_logs`.
5. Frontend stores token in `localStorage.hl7_auth_token`, decodes claims, sets user state in `AuthContext`, and navigates to `/dashboard`.
6. To log out: Click *"Logout"* at the bottom of the sidebar. The client clears `localStorage`, resets auth context, and redirects to `/login`.

### 3.2 Hospital and User Management Workflow (Admin Only)
1. Log in as an Administrator (`admin`).
2. **Register a Hospital:**
   - In the sidebar, click *"Hospitals"* (navigating to `/hospitals`).
   - Click *"Register Hospital"*.
   - Fill in Name: `General Health Hospital`, Code: `GEN_01`, Email: `contact@genhealth.org`.
   - Click *"Register Hospital"*. Backend persists to `hospitals` table and creates an audit entry `HOSPITAL_CREATE`.
3. **Register a User:**
   - In the sidebar, click *"Register User"* (navigating to `/users/create`).
   - Enter Username: `dr_smith`, select Hospital: `General Health Hospital [GEN_01]`, select Role: `Clinical User (USER)`, enter Password: `SecurePassword123!` (8+ characters).
   - Click *"Create Account"*. Backend encodes password with BCrypt, associates `hospital_id`, records `USER_CREATE` in audit logs, and returns HTTP 201 Created.

### 3.3 HL7 Message Submission & FHIR Conversion Workflow
1. In the sidebar, click *"Convert HL7"* (navigating to `/convert`).
2. Inspect the HL7 input box. Click *"Load Example"* to populate a valid standard ADT^A01 message:
   ```hl7
   MSH|^~\&|HIS|HOSPITAL|FHIR_ENGINE|HOSPITAL|202610080900||ADT^A01|MSG00001|P|2.5
   PID|1||P1001||Sharma^Rahul||19950510|M
   PV1|1|I|WARD1||||DOC001|||||||||||||||||||||||||||||||||||||202610080900|202610151200
   OBX|1|NM|GLUCOSE||120|mg/dL|70-100||||F
   OBX|2|NM|HEMOGLOBIN||13.5|g/dL|12-16||||N
   ```
3. Click *"Convert to FHIR R4"*.
4. **Backend Processing Sequence:**
   - Validates HL7 structure (`DefaultHL7Validator`).
   - Maps `PID` to FHIR `Patient` resource.
   - Maps `PV1` to FHIR `Encounter` resource.
   - Maps `OBX` to FHIR `Observation` resources.
   - Packages resources into a FHIR `collection` Bundle.
   - Saves patient record in `patients`, encounter in `encounters`, observations in `observations`, and conversion log in `conversions` under the current user's hospital tenant.
   - Records `HL7_CONVERSION_SUCCESS` in `audit_logs`.
5. **Frontend Display:**
   - Metadata strip renders Conversion ID, Control ID (`MSG00001`), Patient ID (`P1001`), Encounter ID, and Observation Count (2).
   - `FHIRViewer` displays the formatted JSON FHIR Bundle with resource trees.

### 3.4 Outbound FHIR Dispatch & Mock Destination Workflow
1. On the `/convert` page (or from `/history` after clicking *"Send FHIR"*):
2. Under *"Dispatch to Destination Server"*, select target destination: `Local Mock FHIR Server (Dev/Test) (http://localhost:8080/api/mock-fhir) [Mock Server]`.
3. Click *"Send to Destination"*.
4. A confirmation dialog appears: *"Confirm Delivery: Send generated FHIR R4 output (Conversion #X) to 'Local Mock FHIR Server'?"*. Click OK.
5. The backend `FhirDeliveryService` routes the payload to the Mock server, validates the bundle using HAPI FHIR, buffers the resource in memory, logs `FHIR_DELIVERY_SUCCESS` in `audit_logs`, and records a `delivery_results` row with HTTP 201 Created.
6. The UI displays a green success banner with status `SUCCESS` and `HTTP 201`.
7. Navigate to *"Destinations"* in sidebar, switch to *"Mock FHIR Inspector"* tab: The received message is listed. Click *"Inspect Raw Payload"* to view the JSON bundle that was dispatched.

### 3.5 Delivery Retry & Resilience Workflow
1. Navigate to `/destinations`. Click *"New Destination"*.
2. Enter Name: `Failing External EHR`, Base URL: `https://httpbin.org/status/500`, Auth: `NONE`. Click *"Create Destination"*.
3. Go to `/convert`, load example, convert, and choose `Failing External EHR` as target destination. Click *"Send to Destination"*.
4. The destination responds with HTTP 500 (transient error).
5. The engine executes the exponential retry policy:
   - Sets status to `RETRYING`.
   - Computes exponential backoff with jitter and sets `next_retry_at`.
   - Logs `RETRY_SCHEDULED` in `audit_logs`.
6. In `/history`, inspect the conversion:
   - Delivery attempt status displays an animated yellow `RETRYING` badge with the scheduled next retry time.
   - Once max attempts (3) are reached, status transitions to `FAILED`.
   - A manual *"Retry"* button is available on eligible records to re-trigger transmission on demand.

### 3.6 Clinical Records Exploration Workflow
1. In the sidebar, click *"Clinical Records"* (navigating to `/patients`).
2. Search for patient `P1001` or `Sharma` in the registry search box.
3. Click on the patient item in the left column.
4. **Inspect Demographics:** View Family Name, Given Name, MRN, Administrative Gender, Date of Birth.
5. **Inspect Encounters:** Click *"Encounters (1)"* tab. View inpatient visit details (class `INPATIENT`, location `WARD1`, admit and discharge dates).
6. **Inspect Observations:** Click *"Observations (2)"* tab. View GLUCOSE (`120 mg/dL`, range `70-100`, status `FINAL`) and HEMOGLOBIN (`13.5 g/dL`, range `12-16`).

---

## 4. All Backend API Endpoints

| Method | Endpoint URL | Purpose | Auth Required | Request Body / Params | Expected Response | Role / Tenant Restrictions | Associated Frontend Page |
| :--- | :--- | :--- | :---: | :--- | :--- | :--- | :--- |
| **POST** | `/api/auth/login` | Authenticate user & issue JWT | Public | JSON: `{ username, password }` | `200 OK`: `{ token, tokenType: "Bearer" }` | None | `/login` |
| **GET** | `/api/health` | Service & DB health check | Public | None | `200 OK`: `{ status: "UP", database: "UP", engine: "UP" }` | None | `/dashboard` (Engine Specs) |
| **POST** | `/api/hl7/convert` | Parse, validate, convert & persist HL7 v2 | Bearer JWT | JSON: `{ hl7Message: string }` | `200 OK`: `HL7ConvertResponse` (IDs, FHIR payload) | Hospital tenant derived from token | `/convert` |
| **GET** | `/api/hl7/history` | List previous conversions | Bearer JWT | None | `200 OK`: List of `ConversionHistoryResponse` | Scoped to hospital tenant (or all for platform admin) | `/history`, `/dashboard` |
| **GET** | `/api/hl7/history/{id}` | Get conversion audit details | Bearer JWT | Path: `{id}` | `200 OK`: `ConversionHistoryResponse` | Cross-hospital access forbidden (403) | `/history` (Modal) |
| **POST** | `/api/hl7/conversions/{id}/send` | Dispatch conversion to destination | Bearer JWT | Path: `{id}`, Body: `{ destinationId: number }` | `200 OK`: `DeliveryResultResponse` | Cross-hospital access forbidden (403) | `/convert`, `/history` |
| **GET** | `/api/hl7/conversions/{id}/deliveries`| Get delivery attempts for conversion | Bearer JWT | Path: `{id}` | `200 OK`: List of `DeliveryResultResponse` | Cross-hospital access forbidden (403) | `/convert`, `/history` |
| **GET** | `/api/hl7/deliveries/{id}` | Get delivery attempt by ID | Bearer JWT | Path: `{id}` | `200 OK`: `DeliveryResultResponse` | Cross-hospital access forbidden (403) | `/history` |
| **POST** | `/api/hl7/deliveries/{id}/retry` | Manually retry failed delivery | Bearer JWT | Path: `{id}` | `200 OK`: `DeliveryResultResponse` | Cross-hospital access forbidden (403) | `/history` |
| **GET** | `/api/destinations` | List accessible destinations | Bearer JWT | None | `200 OK`: List of `DestinationResponse` | Shows hospital-owned + platform-wide destinations | `/destinations`, `/convert` |
| **GET** | `/api/destinations/{id}` | Get destination config | Bearer JWT | Path: `{id}` | `200 OK`: `DestinationResponse` | Cross-hospital access forbidden (403) | `/destinations` |
| **POST** | `/api/destinations` | Create new destination | Bearer JWT | JSON: `{ name, baseUrl, fhirVersion, authType, bearerToken, enabled }` | `201 Created`: `DestinationResponse` | Associated with caller's hospital | `/destinations` |
| **PUT** | `/api/destinations/{id}` | Update destination config | Bearer JWT | Path: `{id}`, JSON update payload | `200 OK`: `DestinationResponse` | Cross-hospital access forbidden (403) | `/destinations` |
| **PATCH**| `/api/destinations/{id}/status` | Enable/disable destination | Bearer JWT | Path: `{id}`, Body: `{ enabled: boolean }` | `200 OK`: `DestinationResponse` | Cross-hospital access forbidden (403) | `/destinations` |
| **DELETE**| `/api/destinations/{id}` | Delete destination | Bearer JWT | Path: `{id}` | `204 No Content` | Cross-hospital access forbidden (403) | `/destinations` |
| **GET** | `/api/destinations/{id}/deliveries` | Get deliveries for destination | Bearer JWT | Path: `{id}` | `200 OK`: List of `DeliveryResultResponse` | Cross-hospital access forbidden (403) | `/destinations` (Modal) |
| **GET** | `/api/destinations/{id}/test` | Test destination connection | Bearer JWT | Path: `{id}` | `200 OK`: `{ success, message, latencyMs, serverSoftware, ... }` | Cross-hospital access forbidden (403) | `/destinations` |
| **POST** | `/api/mock-fhir/Bundle` | Mock FHIR Bundle receiver | Public | Body: FHIR Bundle JSON | `201 Created`: FHIR `OperationOutcome` | Universal testing endpoint | Ingest from converter |
| **POST** | `/api/mock-fhir/{resourceType}` | Mock FHIR resource receiver | Public | Body: FHIR Resource JSON | `201 Created`: FHIR `OperationOutcome` | Universal testing endpoint | Ingest from converter |
| **GET** | `/api/mock-fhir/received` | List mock received resources | Public | None | `200 OK`: List of `MockReceivedResource` | Testing feed | `/destinations` (Inspector) |
| **GET** | `/api/mock-fhir/received/{id}` | Get raw received mock payload | Public | Path: `{id}` | `200 OK`: `MockReceivedResource` | Testing feed | `/destinations` (Inspector) |
| **DELETE**| `/api/mock-fhir/received` | Clear mock message buffer | Public | None | `204 No Content` | Testing feed | `/destinations` (Inspector) |
| **GET** | `/api/audit-logs` | Filtered paginated audit logs | Bearer JWT | Params: `page, size, action, outcome, resourceType, search, startDate, endDate` | `200 OK`: `AuditLogPageResponse` | Tenant-scoped (admins see platform logs) | `/audit-logs` |
| **GET** | `/api/audit-logs/{id}` | Get single audit log entry | Bearer JWT | Path: `{id}` | `200 OK`: `AuditLogResponse` | Cross-hospital access forbidden (403) | `/audit-logs` (Modal) |
| **GET** | `/api/dashboard/stats` | Tenant dashboard metrics | Bearer JWT | None | `200 OK`: `DashboardStatsResponse` | Strictly scoped to caller's hospital | `/dashboard` |
| **GET** | `/api/hospitals` | List registered hospitals | Bearer JWT | None | `200 OK`: List of `HospitalResponse` | Platform admin sees all; hospital user sees own | `/hospitals`, `/users/create` |
| **GET** | `/api/hospitals/{id}` | Get hospital details | Bearer JWT | Path: `{id}` | `200 OK`: `HospitalResponse` | Tenant boundary enforced | `/hospitals` |
| **POST** | `/api/hospitals` | Register new hospital | Bearer JWT | JSON: `{ name, code, email, phone, address, enabled }` | `201 Created`: `HospitalResponse` | **Platform Admin only** | `/hospitals` |
| **PUT** | `/api/hospitals/{id}` | Update hospital tenant | Bearer JWT | Path: `{id}`, JSON update payload | `200 OK`: `HospitalResponse` | **Admin only** | `/hospitals` |
| **POST** | `/api/users` | Register user account | Bearer JWT | JSON: `{ username, password, role, hospitalId }` | `201 Created`: `CreateUserResponse` | **Admin only** (Hospital admin restricted to own tenant) | `/users/create` |
| **GET** | `/api/users` | List user accounts | Bearer JWT | None | `200 OK`: List of `UserResponse` | **Admin only** (scoped to tenant) | Backend service |
| **GET** | `/api/users/{id}` | Get user by ID | Bearer JWT | Path: `{id}` | `200 OK`: `UserResponse` | **Admin only** | Backend service |
| **PUT** | `/api/users/{id}` | Update user account | Bearer JWT | Path: `{id}`, JSON update payload | `200 OK`: `UserResponse` | **Admin only** | Backend service |
| **GET** | `/api/patients` | List hospital patients | Bearer JWT | None | `200 OK`: List of `PatientResponse` | Strictly scoped to caller's hospital | `/patients` |
| **GET** | `/api/patients/{id}` | Get patient details | Bearer JWT | Path: `{id}` | `200 OK`: `PatientResponse` | Cross-hospital access forbidden (403) | `/patients` |
| **GET** | `/api/patients/{id}/encounters` | Get patient encounters | Bearer JWT | Path: `{id}` | `200 OK`: List of `EncounterResponse` | Cross-hospital access forbidden (403) | `/patients` |
| **GET** | `/api/patients/{id}/observations` | Get patient observations | Bearer JWT | Path: `{id}` | `200 OK`: List of `ObservationResponse` | Cross-hospital access forbidden (403) | `/patients` |
| **DELETE**| `/api/patients/{id}` | Delete patient record | Bearer JWT | Path: `{id}` | `204 No Content` | Cross-hospital access forbidden (403) | Backend API |
| **GET** | `/api/encounters/{id}` | Get encounter by ID | Bearer JWT | Path: `{id}` | `200 OK`: `EncounterResponse` | Cross-hospital access forbidden (403) | Backend API |
| **GET** | `/api/observations/{id}` | Get observation by ID | Bearer JWT | Path: `{id}` | `200 OK`: `ObservationResponse` | Cross-hospital access forbidden (403) | Backend API |

---

## 5. Database Schema & Data Models

### 5.1 Entities and Tables

#### 1. `hospitals`
- **Purpose:** Represents healthcare institution tenants.
- **Columns:** `id` (PK, BigInt auto-increment), `name` (varchar 150), `code` (varchar 100, unique), `email`, `phone`, `address`, `enabled` (boolean), `created_at` (datetime).
- **CRUD Triggers:**
  - *Create:* Platform Admin registers hospital via `/api/hospitals`.
  - *Read:* Queried on login, user creation, destination lookup, dashboard KPIs.
  - *Update:* Admin edits hospital metadata via `PUT /api/hospitals/{id}`.
  - *Delete:* Not exposed (soft-disabled via `enabled = false`).

#### 2. `users`
- **Purpose:** User and administrator login accounts.
- **Columns:** `id` (PK), `username` (varchar 100, unique), `password` (BCrypt hash), `role` (`ADMIN` or `USER`), `hospital_id` (FK to `hospitals.id`, nullable for platform admin), `enabled` (boolean), `created_at`, `updated_at`.
- **CRUD Triggers:**
  - *Create:* Admin provisions user via `/api/users`.
  - *Read:* Verified during authentication (`/api/auth/login`) and tenant security checks.
  - *Update:* Admin updates role/status via `PUT /api/users/{id}`.

#### 3. `destinations`
- **Purpose:** Remote or local FHIR servers where converted bundles are dispatched.
- **Columns:** `id` (PK), `name` (varchar 150), `base_url` (varchar 500), `fhir_version` (`R4`), `auth_type` (`NONE` or `BEARER_TOKEN`), `auth_token` (AES-256-GCM encrypted string), `enabled` (boolean), `mock` (boolean), `hospital_id` (FK to `hospitals.id`, nullable for platform-wide destinations), `created_at`, `updated_at`.
- **CRUD Triggers:**
  - *Create:* User creates destination via `/api/destinations` or mock seed at startup.
  - *Read:* Loaded on converter, history, and destination management.
  - *Update:* User modifies URL, name, token, or toggles status via `PATCH /api/destinations/{id}/status`.
  - *Delete:* User deletes destination via `DELETE /api/destinations/{id}`.

#### 4. `conversions`
- **Purpose:** Audit record and persistent storage of HL7-to-FHIR conversion operations.
- **Columns:** `id` (PK), `message_control_id` (varchar 100), `message_type` (varchar 50), `status` (`SUCCESS` or `FAILED`), `patient_identifier` (varchar 100), `encounter_identifier` (varchar 100), `observation_count` (int), `error_message` (text), `hospital_id` (FK to `hospitals.id`), `fhir_payload` (longtext storing full JSON bundle), `created_at` (datetime).
- **Performance Indexes:** `idx_conv_hosp_created (hospital_id, created_at)`, `idx_conv_hosp_status (hospital_id, status)`, `idx_conv_msg_ctrl (message_control_id)`.
- **CRUD Triggers:**
  - *Create:* Created on every `POST /api/hl7/convert` execution.
  - *Read:* Listed on dashboard, history page, and inspected in conversion details modal.

#### 5. `delivery_results`
- **Purpose:** Outbound transmission attempts and retry state machine logs for FHIR dispatches.
- **Columns:** `id` (PK), `conversion_id` (BigInt), `destination_id` (BigInt), `destination_name` (varchar 150), `attempt_timestamp` (datetime), `status` (`SUCCESS`, `RETRYING`, `FAILED`, `PENDING`), `http_status_code` (int), `error_message` (text), `response_summary` (varchar 500), `attempt_count` (int), `max_attempts` (int, default 3), `next_retry_at` (datetime), `hospital_id` (FK to `hospitals.id`), `created_at`, `updated_at`.
- **Performance Indexes:** `idx_deliv_status_retry (status, next_retry_at)`, `idx_deliv_hospital_status (hospital_id, status)`, `idx_deliv_hospital_attempt (hospital_id, attempt_timestamp)`, `idx_deliv_conversion_id (conversion_id)`.
- **CRUD Triggers:**
  - *Create:* Created when user clicks *"Send to Destination"*.
  - *Update:* Updated by `DeliveryRetryScheduler` when automatic retries execute, or when manual retry is triggered.

#### 6. `patients`
- **Purpose:** Extracted and deduplicated patient demographic master records.
- **Columns:** `id` (PK), `patient_identifier` (MRN, varchar 100), `family_name`, `given_name`, `birth_date` (date), `gender` (`M`, `F`, `O`, `U`), `address_line`, `city`, `state`, `postal_code`, `phone_number`, `hospital_id` (FK to `hospitals.id`), `created_at`.
- **Performance Indexes:** `idx_patient_ident_hosp (patient_identifier, hospital_id)`, `idx_patient_hosp_created (hospital_id, created_at)`.
- **CRUD Triggers:**
  - *Create / Upsert:* Persisted during HL7 conversion. Deduplicated by `(patient_identifier, hospital_id)`.
  - *Read:* Listed on Clinical Records page (`/patients`).
  - *Delete:* `DELETE /api/patients/{id}` (cascades to encounters and observations).

#### 7. `encounters`
- **Purpose:** Inpatient, outpatient, or emergency clinical encounters extracted from `PV1` segments.
- **Columns:** `id` (PK), `encounter_identifier` (varchar 100), `patient_id` (FK to `patients.id`), `patient_class` (`I`, `O`, `E`), `location`, `admit_date_time`, `discharge_date_time`, `attending_doctor`, `hospital_id` (FK to `hospitals.id`), `created_at`.
- **Performance Indexes:** `idx_enc_patient_hosp (patient_id, hospital_id)`, `idx_enc_ident_hosp (encounter_identifier, hospital_id)`, `idx_enc_hosp_created (hospital_id, created_at)`.
- **CRUD Triggers:** Created during HL7 conversion; queried in Clinical Records under Encounters tab.

#### 8. `observations`
- **Purpose:** Clinical lab metrics, test codes, and quantitative/string results extracted from `OBX` segments.
- **Columns:** `id` (PK), `patient_id` (FK to `patients.id`), `encounter_id` (FK to `encounters.id`, nullable), `observation_code`, `value_type` (`NM`, `ST`), `value_numeric` (double), `value_string` (varchar 255), `units` (varchar 50), `reference_range` (varchar 100), `abnormal_flags` (varchar 50), `status` (`F`, `P`, `C`), `observation_date_time`, `hospital_id` (FK to `hospitals.id`), `created_at`.
- **Performance Indexes:** `idx_obs_patient_hosp (patient_id, hospital_id)`, `idx_obs_encounter_hosp (encounter_id, hospital_id)`, `idx_obs_hosp_created (hospital_id, created_at)`.
- **CRUD Triggers:** Created during HL7 conversion; queried in Clinical Records under Observations tab.

#### 9. `audit_logs`
- **Purpose:** Immutable compliance and security event ledger.
- **Columns:** `id` (PK), `actor_id` (BigInt), `username` (varchar 100), `action` (varchar 100), `resource_type` (varchar 50), `resource_id` (varchar 100), `outcome` (`SUCCESS` / `FAILURE`), `details` (text), `correlation_id` (varchar 100), `hospital_id` (FK to `hospitals.id`), `timestamp` (datetime).
- **Performance Indexes:** `idx_audit_logs_hospital_id`, `idx_audit_logs_timestamp`, `idx_audit_logs_action`, `idx_audit_logs_outcome`, `idx_audit_logs_resource_type`.
- **CRUD Triggers:**
  - *Create:* Logged by `AuditService` on login, conversions, deliveries, and configuration changes.
  - *Read:* Queried on `/audit-logs`.
  - *Update / Delete:* NEVER. Read-only audit log.

---

## 6. Validation and Error Handling

### 6.1 HL7 v2 Message Validation Rules
- **Header Check:** Message must start with `MSH|^~\&|` (or custom delimiters defined in MSH-1 and MSH-2).
- **Message Type:** Strictly `ADT^A01` (case-insensitive). Submitting `ORM^O01`, `ORU^R01`, or missing MSH-9 throws `UnsupportedHL7MessageException`: *"Unsupported HL7 message type: [type]. Only ADT^A01 is supported by the current version."*
- **Patient Segment (`PID`):**
  - `PID-3` (Patient ID) is strictly required. Missing PID-3 throws `InvalidHL7Exception`: *"Patient identifier is required"*.
  - `PID-5` (Patient Name) must include both family name and given name separated by `^` (e.g. `Sharma^Rahul`).
  - `PID-7` (Birth Date): Must be valid calendar date matching format `YYYYMMDD`, `YYYYMMDDHHMM`, or `YYYYMMDDHHMMSS`. Invalid leap years (e.g. `19950229`) or non-numeric dates are rejected.
  - `PID-8` (Gender): Supported values are `M` (Male), `F` (Female), `O` (Other), `U` (Unknown). Any other character throws `InvalidHL7Exception`.
- **Encounter Segment (`PV1`):**
  - Optional, but if present:
    - `PV1-2` (Patient Class): Supported values are `I` (Inpatient), `O` (Outpatient), `E` (Emergency).
    - `PV1-44` / `PV1-45` (Admit/Discharge Datetimes): Must be valid format. If discharge date precedes admit date, throws `InvalidHL7Exception`: *"Invalid encounter date: end date cannot be before start date"*.
- **Observation Segment (`OBX`):**
  - Optional, multiple permitted:
    - `OBX-2` (Value Type): Must be `NM` (Numeric) or `ST` (String).
    - `OBX-3` (Identifier): Required (e.g. `GLUCOSE`, `HEMOGLOBIN`).
    - `OBX-5` (Value): Required. If `NM`, must parse to a valid Double (e.g. `120`). Non-numeric string throws: *"Invalid numeric observation value: [val]"*.
    - `OBX-11` (Status): Supported values are `F` (Final), `P` (Preliminary), `C` (Corrected), `N` (Not asked).

### 6.2 Destination Validation & SSRF Rules
- **Name:** Required, trimmed.
- **Base URL:** Must begin with `http://` or `https://`.
- **SSRF Filter:** External destination URLs resolving to private IPv4/IPv6 ranges (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `127.0.0.0/8`, `::1`, `169.254.0.0/16` AWS metadata) are rejected with `InvalidDestinationException`.
  - *Exception:* The local loopback URL `http://localhost:8080/api/mock-fhir` is explicitly permitted for local development.
- **Bearer Token:** Required when `authType = 'BEARER_TOKEN'`. Stored encrypted using `AES-256-GCM` (`TokenCryptoService`). Never serialized back in plain text to HTTP responses (`hasAuthToken: true`).

### 6.3 HTTP Error Codes & Client Handling
- **`400 Bad Request`:** Validation failure on DTOs, malformed HL7 syntax, or invalid JSON. Response contains user-friendly message.
- **`401 Unauthorized`:** Missing, expired, or invalid JWT Bearer token. `apiClient.js` intercepts 401, clears `localStorage`, dispatches `auth:unauthorized`, and redirects to `/login`.
- **`403 Forbidden`:** Role access denial or cross-hospital multi-tenant violation (`TenantAccessDeniedException`).
- **`404 Not Found`:** Resource does not exist (unknown destination, hospital, conversion, patient).
- **`409 Conflict`:** Duplicate unique hospital code (`DuplicateHospitalCodeException`) or duplicate username (`UserAlreadyExistsException`).
- **`500 Internal Server Error`:** Unhandled system error.

---

## 7. Security and Access Control

### 7.1 Multi-Tenant Hospital Isolation
The application implements strict multi-tenancy complying with HIPAA Business Associate Agreements (BAA):
- Every clinical data row in `patients`, `encounters`, `observations`, `conversions`, and `delivery_results` carries a mandatory `hospital_id` foreign key.
- Whenever an authenticated user performs a database query or ingestion:
  - `TenantSecurityService.requireCurrentHospital()` extracts the caller's assigned hospital tenant from the database.
  - All queries include `WHERE hospital_id = :hospitalId`.
  - If a user attempts to query a resource belonging to hospital B with credentials for hospital A (e.g., `GET /api/patients/99`), the service compares `resource.hospital.id` with `caller.hospital.id` and throws `TenantAccessDeniedException` (HTTP 403 Forbidden).

### 7.2 Password and Token Security
- **BCrypt:** All user passwords are salted and hashed using BCrypt. Passwords are never logged in application logs or stored in plaintext.
- **AES-256-GCM:** External FHIR destination Bearer tokens are encrypted at rest in MySQL using AES-256 in Galois/Counter Mode (GCM) with random IVs to ensure confidentiality and integrity.
- **Stateless JWT:** Bearer tokens are signed using HMAC-SHA256 with a minimum 32-byte secret key and carry standard expiration (24 hours).
- **Audit Trails:** The audit logger sanitizes details and never records authorization headers, passwords, or patient medical records into `audit_logs`.

---

## 8. Manual Testing Checklist

Follow this comprehensive browser testing checklist to verify all implemented features.

### Phase 1: Authentication & Navigation

| Test ID | Page / Route | Preconditions | Exact User Action | Expected Result | DB / Backend Verification Needed? |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **AUTH-01** | `/login` | Logged out | Attempt submit with blank fields | Error alert *"Please enter both username and password."* appears. No API request sent. | No |
| **AUTH-02** | `/login` | Logged out | Enter `admin` and wrong password `wrongpass` | Red error banner *"Invalid username or password."* (or *"Bad credentials"*). Stays on `/login`. | Verify `audit_logs` has `LOGIN_FAILURE` |
| **AUTH-03** | `/login` | Logged out | Enter `admin` and `admin123`. Click submit | Redirects to `/dashboard`. Header shows *"Engine Operational"*. Sidebar shows *"ADMIN"*. Token in `localStorage`. | Verify `audit_logs` has `LOGIN_SUCCESS` |
| **AUTH-04** | Sidebar | Logged in | Click *"Logout"* button in sidebar footer | Clears token from `localStorage`, immediately redirects to `/login`. | No |
| **AUTH-05** | `/dashboard` | Logged out | Type `http://localhost:5173/dashboard` directly in address bar | Route guard intercepts and immediately redirects to `/login`. | No |
| **AUTH-06** | `/unknown` | Logged in | Type `http://localhost:5173/some-random-route` in address bar | Displays `404 Page Not Found` card with *"Return to Dashboard"* button. | No |

---

### Phase 2: Hospital & User Administration

| Test ID | Page / Route | Preconditions | Exact User Action | Expected Result | DB / Backend Verification Needed? |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **ADM-01** | `/hospitals` | Logged in as Admin | Click *"Register Hospital"*. Enter Name: `Apollo Memorial`, Code: `APOLLO_01`, Email: `admin@apollo.org`. Submit | Modal closes, green success banner displays, new hospital appears in table. | Check `SELECT * FROM hospitals WHERE code='APOLLO_01'` |
| **ADM-02** | `/hospitals` | Logged in as Admin | Click *"Register Hospital"*. Enter Name: `Apollo Duplicate`, Code: `APOLLO_01` (same code). Submit | Form error banner displays *"Hospital code 'APOLLO_01' is already in use"*. | No |
| **ADM-03** | `/hospitals` | Logged in as Admin | Click *"Edit"* on `APOLLO_01`. Change phone to `555-9988`. Submit | Modal closes, table reflects updated phone, success banner displays. | Check `hospitals` table updated |
| **ADM-04** | `/users/create` | Logged in as Admin | Enter Username: `clinician_apollo`, Hospital: `Apollo Memorial`, Role: `Clinical User`, Password: `TestPassword123!`. Submit | Green banner *"Account Created Successfully"* appears. Form resets. | Check `users` table for `clinician_apollo` |
| **ADM-05** | `/users/create` | Logged in as Admin | Enter Username: `clinician_apollo` (duplicate). Submit with valid password | Creation error banner *"Username 'clinician_apollo' is already taken"* displays. | No |
| **ADM-06** | `/users/create` | Logged in as Admin | Enter Password: `short`, Confirm: `short` (<8 chars). Submit | Field error *"Password must be at least 8 characters long"* displays. No API call. | No |
| **ADM-07** | `/login` | User created in ADM-04 | Log out of Admin, log in with `clinician_apollo` / `TestPassword123!` | Successfully logs in. Sidebar displays role `USER`. Sidebar does **not** show *"Hospitals"* or *"Register User"*. | Check `audit_logs` has `LOGIN_SUCCESS` |
| **ADM-08** | `/hospitals` | Logged in as `clinician_apollo` | Attempt to navigate to `http://localhost:5173/hospitals` via address bar | Guard detects non-ADMIN role, redirects automatically to `/dashboard`. | No |

---

### Phase 3: HL7 v2 to FHIR R4 Conversion Console

| Test ID | Page / Route | Preconditions | Exact User Action | Expected Result | DB / Backend Verification Needed? |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **CONV-01** | `/convert` | Logged in | Click *"Clear"* to empty textarea. Click *"Convert to FHIR R4"* | Error banner *"HL7 message is required. Please paste or enter an HL7 message to convert."* displays. | No |
| **CONV-02** | `/convert` | Logged in | Click *"Load Example"*. Modify MSH-9 from `ADT^A01` to `ORM^O01`. Click Convert | Error alert *"Unsupported HL7 message type: ORM^O01. Only ADT^A01 is supported by the current version."* displays. | No |
| **CONV-03** | `/convert` | Logged in | Click *"Load Example"*. Remove PID line completely. Click Convert | Error alert *"Patient information (PID) is required"* displays. | No |
| **CONV-04** | `/convert` | Logged in | Click *"Load Example"*. In OBX-1 change value from `120` to `NOT_A_NUMBER`. Click Convert | Error alert *"Invalid numeric observation value: NOT_A_NUMBER"* displays. | No |
| **CONV-05** | `/convert` | Logged in | Click *"Load Example"*. Click *"Convert to FHIR R4"* with default valid ADT^A01 | Conversion succeeds. Metadata chips show `Control ID: MSG00001`, `Patient ID: P1001`, `Obs: 2`. `FHIRViewer` shows valid JSON Bundle. | Check `conversions`, `patients`, `encounters`, `observations` |
| **CONV-06** | `/convert` | Conversion succeeded (CONV-05) | In `FHIRViewer`, click the copy icon button | Toast or copy confirmation appears. Clipboard contains clean JSON of the FHIR Bundle. | No |

---

### Phase 4: FHIR Destinations & Dispatch

| Test ID | Page / Route | Preconditions | Exact User Action | Expected Result | DB / Backend Verification Needed? |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **DEST-01** | `/destinations` | Logged in | Click *"New Destination"*. Click *"⚡ Fill Local Mock Preset"*. Click *"Create Destination"* | Modal closes, destination `Local Mock FHIR Server` appears in table with `Embedded Mock` badge. | Check `destinations` table |
| **DEST-02** | `/destinations` | Logged in | Click *"New Destination"*. Enter URL `http://192.168.1.5/fhir`. Submit | Error banner displays SSRF rejection: private IP ranges are forbidden. | No |
| **DEST-03** | `/destinations` | Logged in | On `Local Mock FHIR Server` row, click the pulse icon button (*Test Connection*) | Spinner appears, then inline green badge displays `✅ ... latency X ms, HTTP 200 OK`. | No |
| **DEST-04** | `/convert` | Completed CONV-05 | In *"Dispatch to Destination Server"*, select `Local Mock FHIR Server`. Click *"Send to Destination"*. Confirm prompt | Banner shows green `SUCCESS` with `HTTP 201 Created` and summary note. | Check `delivery_results` table |
| **DEST-05** | `/destinations` | Completed DEST-04 | Click *"Mock FHIR Inspector"* tab | The dispatched resource is listed in the feed. Shows `HTTP 201 Created` and tracking ID. | No |
| **DEST-06** | `/destinations` | Completed DEST-05 | Click *"Inspect Raw Payload"* on the mock item | Modal opens displaying formatted JSON bundle received by mock server. Click *"Copy JSON"*. | No |
| **DEST-07** | `/destinations` | Completed DEST-06 | Click *"Clear Buffer"*. Confirm prompt | Buffer is emptied. Inspector shows empty state *"Mock Buffer Empty"*. | No |
| **DEST-08** | `/destinations` | Logged in | Toggle the status switch/button on a destination | Status badge flips between `ACTIVE` and `INACTIVE`. Green alert confirms update. | Check `destinations.enabled` column |

---

### Phase 5: Conversion History & Delivery Retries

| Test ID | Page / Route | Preconditions | Exact User Action | Expected Result | DB / Backend Verification Needed? |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **HIST-01** | `/history` | Completed CONV-05 | Navigate to `/history`. Type `P1001` in the search bar | Table filters to show only conversions with Patient ID `P1001`. | No |
| **HIST-02** | `/history` | Completed CONV-05 | Click *"Inspect"* on conversion `P1001` | Modal opens showing metadata, Original HL7 message, FHIR viewer, and Delivery attempts list. | No |
| **HIST-03** | `/history` | Modal open | In modal's *"Send to Destination"*, select mock server and click *"Dispatch Now"* | Dispatches bundle. Delivery attempt counter increments in modal. Success banner displays. | Check `delivery_results` row inserted |
| **HIST-04** | `/history` | Delivery with failure | On a failed delivery attempt row, click *"Retry"* | Button enters loading state, re-invokes delivery attempt, and updates status accordingly. | Check `delivery_results` attempt count |

---

### Phase 6: Clinical Records (Patients, Encounters, Observations)

| Test ID | Page / Route | Preconditions | Exact User Action | Expected Result | DB / Backend Verification Needed? |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **CLIN-01** | `/patients` | Completed CONV-05 | Navigate to `/patients`. Search `P1001` in registry | Patient card for `Sharma, Rahul` (MRN: P1001) is displayed in left column. | No |
| **CLIN-02** | `/patients` | Patient list loaded | Click on `Sharma, Rahul` card | Right panel loads patient header, showing Demographics tab with MRN `P1001`, DOB `1995-05-10`, Gender `M`. | Check `patients` table |
| **CLIN-03** | `/patients` | Patient selected | Click *"Encounters (1)"* tab | Table displays encounter `ENC-P1001-1`, class `INPATIENT`, location `WARD1`, admit and discharge times. | Check `encounters` table |
| **CLIN-04** | `/patients` | Patient selected | Click *"Observations (2)"* tab | Table displays `GLUCOSE` (value `120`, units `mg/dL`, range `70-100`) and `HEMOGLOBIN` (value `13.5`, units `g/dL`). | Check `observations` table |

---

### Phase 7: Audit Logs & Dashboard Verification

| Test ID | Page / Route | Preconditions | Exact User Action | Expected Result | DB / Backend Verification Needed? |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **AUD-01** | `/audit-logs` | Completed previous tests | Navigate to `/audit-logs`. Filter Action: `HL7 Conversion Success` | Table filters to show `HL7_CONVERSION_SUCCESS` entries with actor username and resource IDs. | Check `audit_logs` table |
| **AUD-02** | `/audit-logs` | Audit list loaded | Click *"Inspect"* on an entry | Modal opens displaying full audit details, actor ID, ISO timestamp, correlation ID, and details. | No |
| **DASH-01** | `/dashboard` | Completed previous tests | Navigate to `/dashboard` | Total Conversions, Successful Conversions, Patients, Observations, Deliveries, and Destinations KPI numbers accurately reflect database counts. | Compare with `SELECT count(*) FROM conversions` etc. |
| **DASH-02** | `/dashboard` | On dashboard | Click *"Refresh"* button | Refresh icon rotates with animation. Metrics refresh smoothly without page reload. | No |

---

## 9. Feature Status Summary Table

| Feature Name | Route | Backend Available | Frontend Available | Authentication | Current Limitations / Notes | Manual Test Priority |
| :--- | :--- | :---: | :---: | :---: | :--- | :---: |
| **User Authentication** | `/login` | ✅ Yes | ✅ Yes | Public | Stateless JWT; session lasts 24 hours. | **High** |
| **Dashboard Metrics** | `/dashboard` | ✅ Yes | ✅ Yes | Bearer JWT | Strictly scoped to authenticated hospital tenant. | **High** |
| **HL7 Converter Console** | `/convert` | ✅ Yes | ✅ Yes | Bearer JWT | Only `ADT^A01` messages supported. | **Critical** |
| **Conversion Audit History**| `/history` | ✅ Yes | ✅ Yes | Bearer JWT | Search & filter by status; full inspection modal. | **High** |
| **FHIR Destinations** | `/destinations` | ✅ Yes | ✅ Yes | Bearer JWT | Supports Bearer Token (AES-256 encrypted) and SSRF filter. | **High** |
| **Mock FHIR Inspector** | `/destinations` (Tab 2) | ✅ Yes | ✅ Yes | Public Ingest / JWT View | In-memory buffer holds up to 100 recent messages. | **Medium** |
| **Connection Prober** | `/destinations` (Test) | ✅ Yes | ✅ Yes | Bearer JWT | Probes `{baseUrl}/metadata` without sending clinical data. | **Medium** |
| **Outbound FHIR Dispatch** | `/convert`, `/history` | ✅ Yes | ✅ Yes | Bearer JWT | Sends FHIR bundle to selected destination endpoint. | **Critical** |
| **Automated Delivery Retries**| Background / `@Scheduled`| ✅ Yes | ✅ Viewable in UI | Internal System | Exponential backoff (max 3 attempts) + jitter. | **High** |
| **Manual Delivery Retry** | `/history` (Modal) | ✅ Yes | ✅ Yes | Bearer JWT | Re-triggers delivery for eligible failed attempts. | **Medium** |
| **Clinical Records (Patients)**| `/patients` | ✅ Yes | ✅ Yes | Bearer JWT | Lists patients, demographics, visits, lab tests. | **High** |
| **Hospital Management** | `/hospitals` | ✅ Yes | ✅ Yes | Admin Only | Full registration and metadata editing; no hard delete. | **High** |
| **User Provisioning** | `/users/create` | ✅ Yes | ✅ Yes | Admin Only | Provisions accounts with BCrypt; no edit page in UI. | **Medium** |
| **System Audit Trails** | `/audit-logs` | ✅ Yes | ✅ Yes | Bearer JWT | Immutable trail; filters by action, outcome, dates. | **High** |
| **Health Check Endpoint** | `/api/health` | ✅ Yes | ✅ Consumed | Public | Returns engine, DB, and overall health status. | **Medium** |
