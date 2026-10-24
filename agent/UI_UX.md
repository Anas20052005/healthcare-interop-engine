# UI/UX Specification — HL7 v2 → FHIR Healthcare Interoperability Engine

## 1. Document Purpose

This document defines the complete frontend UI/UX requirements for the HL7 v2 → FHIR Healthcare Interoperability Engine.

The frontend must provide a clean, professional healthcare-integration interface where an authenticated user can:

1. Log in.
2. Access the application dashboard.
3. Paste or enter an HL7 v2 message.
4. Submit the message for conversion.
5. View conversion progress.
6. View the generated FHIR R4 output.
7. View conversion errors in a user-friendly way.
8. Access previous conversion history if history functionality is enabled.
9. Log out securely.

The frontend is responsible for presentation, user interaction, API communication, authentication state, and displaying backend responses.

The frontend must NOT contain HL7 parsing, HL7 validation, FHIR mapping, healthcare business logic, or database logic.

---

# 2. UI/UX Goals

The frontend should feel like a real healthcare integration/developer tool rather than a generic student CRUD application.

Primary goals:

* Professional healthcare/enterprise appearance.
* Simple conversion workflow.
* Minimal unnecessary UI complexity.
* Clear separation between HL7 input and FHIR output.
* Easy-to-read technical data.
* Clear error messages.
* Fast and responsive interaction.
* Good desktop experience.
* Usable tablet/mobile experience.
* Accessible controls and readable text.
* Consistent component design.
* No unnecessary animations or decorative elements.

The user should immediately understand:

```text
What do I enter?
        ↓
Where do I click?
        ↓
What happened?
        ↓
What FHIR output was generated?
```

---

# 3. Target Users

The initial frontend is designed for:

### Primary Users

* Healthcare integration developers
* Backend developers working with HL7/FHIR
* Healthcare IT students
* Integration engineers
* Technical users testing HL7 messages
* Recruiters/interviewers evaluating the project

### Secondary Users

* Developers learning healthcare interoperability
* QA/test engineers
* Technical administrators

The UI does not need to behave like a complete hospital EHR application.

This is an interoperability/conversion tool.

---

# 4. Overall User Journey

The primary user journey is:

```text
Open Application
       ↓
Login
       ↓
Dashboard
       ↓
Open HL7 → FHIR Converter
       ↓
Paste HL7 v2 Message
       ↓
Click "Convert to FHIR"
       ↓
Frontend sends HTTP request using Axios
       ↓
Spring Boot Backend
       ↓
HL7 Parse
       ↓
Validation
       ↓
HL7 → FHIR Mapping
       ↓
FHIR Response
       ↓
Frontend receives response
       ↓
Display FHIR JSON
       ↓
User can Copy / View / Start New Conversion
```

Error flow:

```text
User submits HL7
       ↓
Backend processing
       ↓
Error
       ↓
API Error Response
       ↓
Frontend receives error
       ↓
Display clear error message
       ↓
User fixes HL7
       ↓
Retry
```

---

# 5. Frontend Technology

The frontend should use:

* React.js
* JavaScript
* HTML
* CSS
* Axios

Optional libraries may be introduced only when they provide clear value.

Do not introduce large UI frameworks unnecessarily.

The implementation should remain understandable to a Java/Spring Boot developer who is primarily focused on backend development.

---

# 6. Application Layout

The application should use a consistent application shell after authentication.

Conceptual structure:

```text
┌──────────────────────────────────────────────────────────────┐
│                       TOP NAVBAR                             │
│  Logo / Project Name          Dashboard   History   Logout   │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│                         PAGE CONTENT                         │
│                                                              │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

The navbar should remain simple.

Do not overcrowd the navigation with unnecessary links.

---

# 7. Application Branding

Suggested application name:

**HL7 FHIR Integration Engine**

Short form:

**HL7 → FHIR Engine**

The UI should communicate that this is a healthcare interoperability platform.

Possible supporting text:

> Convert HL7 v2 healthcare messages into FHIR R4 resources.

The design should be professional and technical.

Avoid:

* Gaming-style UI
* Excessive gradients
* Excessive glassmorphism
* Neon colors
* Large decorative illustrations
* Excessive animations
* Blue + purple gradient-heavy designs
* Generic template-looking dashboards

A clean healthcare/enterprise aesthetic is preferred.

---

# 8. Design Principles

The frontend should follow these principles:

### 8.1 Clarity

Every major action should be obvious.

Example:

```text
HL7 v2 Input
     ↓
Convert to FHIR
     ↓
FHIR R4 Output
```

### 8.2 Simplicity

The user should not need to navigate through multiple screens to perform a conversion.

### 8.3 Consistency

Buttons, inputs, cards, alerts, spacing, typography, and navigation should follow a consistent design system.

### 8.4 Technical Readability

HL7 and FHIR are technical formats.

The UI should prioritize readable code/data presentation over decorative design.

### 8.5 Professionalism

The interface should resemble a lightweight enterprise developer tool.

---

# 9. Color System

Use a clean healthcare-oriented visual system.

Recommended approach:

* Light neutral background.
* White cards/panels.
* Dark readable text.
* One primary accent color.
* Green for success.
* Red for errors.
* Yellow/orange for warnings.
* Neutral gray for secondary information.

Do not use a blue-to-purple gradient as the primary visual identity.

Colors should have sufficient contrast for readability.

The exact hexadecimal values may be finalized during implementation, but the color system must remain consistent across the application.

---

# 10. Typography

Use a clean modern sans-serif font.

Recommended options:

* Inter
* Roboto
* System UI

Hierarchy:

```text
Application Title
      ↓
Page Heading
      ↓
Section Heading
      ↓
Body Text
      ↓
Helper Text
```

HL7/FHIR technical content should use a monospace font.

Recommended:

* JetBrains Mono
* Fira Code
* Consolas
* system monospace

Example:

```text
MSH|^~\&|HOSPITAL|...
PID|1||P1001||Sharma^Rahul...
```

and:

```json
{
  "resourceType": "Patient"
}
```

should use a monospace font.

---

# 11. Authentication Screen

## Route

```text
/login
```

The login screen should be the entry point for unauthenticated users.

Conceptual layout:

```text
┌──────────────────────────────────────────────┐
│                                              │
│          HL7 FHIR Integration Engine         │
│                                              │
│       Healthcare Interoperability Tool       │
│                                              │
│   ┌──────────────────────────────────────┐   │
│   │ Email / Username                     │   │
│   └──────────────────────────────────────┘   │
│                                              │
│   ┌──────────────────────────────────────┐   │
│   │ Password                             │   │
│   └──────────────────────────────────────┘   │
│                                              │
│             [       Login       ]             │
│                                              │
│             Error message if any             │
│                                              │
└──────────────────────────────────────────────┘
```

The login page should not contain unnecessary fields.

---

# 12. Login Interaction

When the user clicks Login:

```text
Login button
      ↓
Validate required fields
      ↓
Axios POST /api/auth/login
      ↓
Loading state
      ↓
Success
      ↓
Store authentication state/token according to security design
      ↓
Navigate to Dashboard
```

If authentication fails:

```text
401
 ↓
Display:
"Invalid username or password."
```

Do not display sensitive backend details.

The password field must use a password input.

---

# 13. Authentication Loading State

While login is processing:

```text
[ Logging in... ]
```

The login button should be disabled temporarily.

This prevents duplicate requests.

After the request finishes:

* Re-enable button.
* Navigate on success.
* Display error on failure.

---

# 14. Dashboard

## Route

```text
/dashboard
```

The dashboard should provide a simple overview of the application.

Example:

```text
┌───────────────────────────────────────────────────────────┐
│ Dashboard                                                 │
│ Healthcare Interoperability Overview                      │
│                                                           │
│ ┌────────────────┐ ┌────────────────┐ ┌────────────────┐ │
│ │ Conversions    │ │ Successful     │ │ Failed         │ │
│ │      24        │ │      21        │ │       3        │ │
│ └────────────────┘ └────────────────┘ └────────────────┘ │
│                                                           │
│                  [ Start New Conversion ]                 │
│                                                           │
│ Recent Conversions                                        │
│ --------------------------------------------------------- │
│ Date        Message Type       Status                     │
│ 07 Oct      ADT^A01            Success                    │
│ 06 Oct      ADT^A01            Success                    │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

Dashboard statistics are optional if the backend provides the required data.

Do not create fake statistics just for visual appearance.

If real statistics are not available, show useful application information instead.

---

# 15. Dashboard Responsibilities

The dashboard should allow the user to:

* Start a new conversion.
* See basic conversion information.
* Access recent conversion history.
* Navigate to the converter.
* Navigate to history.
* Logout.

The dashboard should not contain HL7 processing logic.

---

# 16. Converter Page

## Route

```text
/convert
```

This is the primary application screen.

The main purpose of the application should be immediately visible.

Suggested page title:

> HL7 v2 → FHIR R4 Converter

Supporting text:

> Paste an HL7 v2 message and convert it into a FHIR R4 resource.

---

# 17. Converter Layout

Desktop layout:

```text
┌──────────────────────────────────────────────────────────────────┐
│ HL7 v2 → FHIR R4 Converter                                      │
│                                                                  │
│ ┌────────────────────────────┐   ┌────────────────────────────┐ │
│ │ HL7 v2 Input               │   │ FHIR R4 Output             │ │
│ │                            │   │                            │ │
│ │ MSH|^~\&|...               │   │ {                          │ │
│ │ PID|1||P1001|...           │   │   "resourceType": "Patient"│ │
│ │ PV1|1|...                  │   │   ...                      │ │
│ │                            │   │ }                          │ │
│ │                            │   │                            │ │
│ └────────────────────────────┘   └────────────────────────────┘ │
│                                                                  │
│ [Load Example]          [Clear]       [Convert to FHIR]         │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

On smaller screens, the panels should stack vertically:

```text
HL7 Input
   ↓
Convert Button
   ↓
FHIR Output
```

---

# 18. HL7 Input Editor

The HL7 input area is the main user input.

Requirements:

* Large text area/editor.
* Monospace font.
* Preserve line breaks.
* Support multiline HL7 messages.
* Allow copy/paste.
* Provide sufficient visible height.
* Clearly label the field as HL7 v2 Input.
* Show a placeholder/example.
* Do not automatically modify the user's HL7 message.

Example placeholder:

```text
MSH|^~\&|HOSPITAL|FACILITY|RECEIVING|FACILITY|...
PID|1||P1001||Sharma^Rahul||19950510|M
PV1|1|I|WARD1
```

---

# 19. HL7 Input Validation on Frontend

Frontend validation should remain basic.

Allowed frontend checks:

* Empty input.
* Extremely large input if a request-size limit is defined.
* Basic required-field validation.

Example:

```text
HL7 message is required.
```

The frontend must NOT implement complete HL7 validation.

The backend is the source of truth for:

* HL7 structure validation.
* Required segments.
* Field validation.
* Message type validation.
* Healthcare business rules.

---

# 20. Example HL7 Button

Provide an optional:

```text
Load Example
```

button.

When clicked, it should populate the editor with a valid sample HL7 message.

This is useful for:

* Demo
* Testing
* Recruiter walkthrough
* Development
* User onboarding

The example must correspond to the message type supported by the MVP.

For example:

```text
ADT^A01
```

---

# 21. Clear Button

Provide:

```text
Clear
```

behavior.

When clicked:

* Clear HL7 input.
* Clear previous FHIR output.
* Clear previous success/error state if appropriate.

If the user has entered significant content, a confirmation may be used, but unnecessary confirmation dialogs should be avoided.

---

# 22. Convert Button

Primary CTA:

```text
Convert to FHIR
```

When clicked:

```text
User clicks button
       ↓
Check HL7 input is not empty
       ↓
Set loading state
       ↓
Disable duplicate submission
       ↓
Axios request
       ↓
Backend conversion
       ↓
Receive response
       ↓
Display FHIR output
```

Button text during processing:

```text
Converting...
```

The button must be disabled while the request is in progress.

---

# 23. Axios API Layer

Frontend API calls should be separated from UI components.

Do not place large Axios requests directly inside JSX components.

Recommended structure:

```text
src/
└── services/
    ├── authService.js
    ├── hl7Service.js
    └── historyService.js
```

Example conceptual responsibility:

```text
hl7Service.js
      ↓
POST /api/hl7/convert
```

The converter component should call the service rather than implementing HTTP logic itself.

---

# 24. Conversion Request

The frontend should send the HL7 message according to the backend API contract.

Conceptual request:

```json
{
  "hl7Message": "MSH|^~\\&|..."
}
```

Axios:

```text
React
  ↓
hl7Service
  ↓
Axios
  ↓
POST /api/hl7/convert
  ↓
Spring Boot
```

The exact request/response DTO must follow `REQUIREMENTS.md` and backend implementation.

---

# 25. FHIR Output Viewer

The FHIR output should be presented as a technical JSON viewer.

Example:

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

Requirements:

* Monospace font.
* Proper indentation.
* Scrollable container.
* Syntax highlighting if a lightweight implementation is used.
* Preserve valid JSON formatting.
* Clearly show FHIR resource type.
* Do not truncate important output.

---

# 26. FHIR Output Actions

Useful actions:

```text
Copy JSON
```

Optional:

```text
Download JSON
```

If Copy is implemented:

```text
Copy JSON
     ↓
Clipboard API
     ↓
"Copied!"
```

Do not add unnecessary export functionality in the MVP.

---

# 27. Conversion Success State

After successful conversion, show a clear success indicator.

Example:

```text
✓ Conversion successful

FHIR R4 resource generated successfully.
```

Then display:

```text
FHIR R4 Output
```

The success message should not cover the actual FHIR response.

---

# 28. Conversion Error State

If backend returns an error, display it near the converter.

Example:

```text
┌────────────────────────────────────────────────────────────┐
│ ✕ Conversion failed                                       │
│                                                            │
│ Invalid HL7 message: PID segment is missing.               │
└────────────────────────────────────────────────────────────┘
```

The frontend should convert technical backend errors into readable messages when possible.

Do not show:

* Java stack traces
* Database stack traces
* SQL queries
* JWT secrets
* Internal class names
* File paths
* Sensitive healthcare information

---

# 29. HTTP Error Handling

Recommended frontend behavior:

### 400 Bad Request

Display:

```text
Invalid HL7 message. Please check the input and try again.
```

If the backend provides a safe detailed message, display it.

### 401 Unauthorized

Display:

```text
Your session has expired. Please log in again.
```

Then redirect to login if appropriate.

### 403 Forbidden

Display:

```text
You do not have permission to perform this action.
```

### 404 Not Found

Display:

```text
The requested resource was not found.
```

### 409 Conflict

Display the backend-provided safe conflict message.

### 500 Internal Server Error

Display:

```text
Something went wrong while processing the request. Please try again.
```

Never expose internal server details.

---

# 30. Loading State

The application must provide feedback during API operations.

For conversion:

```text
Converting HL7 message...
```

Possible UI:

```text
[ spinner ] Converting...
```

The user must never be left wondering whether the button click worked.

---

# 31. Empty State

When no conversion has been performed:

```text
FHIR R4 Output

No FHIR output yet.

Paste an HL7 v2 message and click
"Convert to FHIR".
```

This is preferable to showing an empty white box.

---

# 32. History Page

## Route

```text
/history
```

If conversion history is included in the MVP, show previous conversions in a clean table.

Example:

```text
┌──────────────────────────────────────────────────────────────┐
│ Conversion History                                           │
│                                                              │
│ Date        Message Type     Status       Action             │
│ ------------------------------------------------------------ │
│ 07 Oct      ADT^A01          Success      View               │
│ 06 Oct      ADT^A01          Success      View               │
│ 05 Oct      ADT^A01          Failed       View Error         │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

Do not display complete patient information in the table.

Only show minimum necessary information.

---

# 33. History Details

When the user selects a history record:

```text
History Record
       ↓
Conversion details
       ↓
Original HL7 / generated FHIR
```

Whether the complete original HL7 is displayed depends on backend security and data-storage decisions.

Healthcare data should not be unnecessarily exposed.

If PHI exists, access must follow the application's authentication and authorization rules.

---

# 34. Navigation

Recommended routes:

```text
/login
/dashboard
/convert
/history
```

Potential future routes:

```text
/profile
/settings
```

These should not be implemented unless required.

Authenticated routes should be protected.

Unauthenticated users should not be able to access:

```text
/dashboard
/convert
/history
```

---

# 35. Protected Route Flow

Conceptually:

```text
User opens /convert
       ↓
Is authenticated?
       ↓
YES ─────────→ Show Converter
       │
       NO
       ↓
Redirect to /login
```

Authentication state should be centrally managed.

Do not duplicate authentication checks across every component.

---

# 36. Navbar

Authenticated application navbar:

```text
┌──────────────────────────────────────────────────────────────┐
│ HL7 FHIR Engine      Dashboard   Converter   History   User │
└──────────────────────────────────────────────────────────────┘
```

The active route should be visually identifiable.

Logout should be easily accessible.

On mobile, the navbar may collapse into a menu.

---

# 37. Reusable Components

The frontend should use reusable components where appropriate.

Suggested components:

```text
components/
├── Navbar.jsx
├── Button.jsx
├── Input.jsx
├── TextArea.jsx
├── Card.jsx
├── Alert.jsx
├── LoadingSpinner.jsx
├── HL7Editor.jsx
├── FhirViewer.jsx
├── ConversionStatus.jsx
├── EmptyState.jsx
└── ProtectedRoute.jsx
```

Do not create a separate component for every tiny HTML element.

Components should be created when reuse or readability justifies them.

---

# 38. Page Structure

Suggested:

```text
pages/
├── Login.jsx
├── Dashboard.jsx
├── Converter.jsx
└── History.jsx
```

The page should coordinate components.

Business logic should remain outside the UI where possible.

---

# 39. Frontend Services

Recommended:

```text
services/
├── apiClient.js
├── authService.js
├── hl7Service.js
└── historyService.js
```

### apiClient.js

Responsible for:

* Axios instance.
* Base URL.
* Common headers.
* Authentication token handling where appropriate.
* Common interceptors if needed.

### authService.js

Responsible for:

* Login request.
* Logout-related client state.
* Authentication-related API communication.

### hl7Service.js

Responsible for:

* HL7 conversion API calls.

### historyService.js

Responsible for:

* History API calls.

---

# 40. Authentication Token Handling

The exact token-storage mechanism must follow the security design.

The frontend must:

* Include the JWT in authenticated requests.
* Send it using the appropriate Authorization mechanism.
* Never place JWT secrets in source code.
* Never expose backend secrets.
* Handle expired sessions.

Conceptual request:

```http
Authorization: Bearer <JWT>
```

The frontend should not decode or modify the JWT unnecessarily.

---

# 41. Global Application State

Keep global state minimal.

Potential global state:

```text
Authentication
Current user
Theme if implemented
```

Local component state should handle:

```text
HL7 input
FHIR output
Loading
Conversion status
Error
```

Do not introduce Redux or another global-state framework unless application complexity genuinely requires it.

---

# 42. Converter State Model

The converter can conceptually maintain:

```text
hl7Message
fhirOutput
loading
error
success
```

State transitions:

```text
INITIAL
  ↓
USER_INPUT
  ↓
SUBMITTING
  ↓
SUCCESS
  ↓
FHIR_OUTPUT
```

or:

```text
SUBMITTING
  ↓
ERROR
  ↓
USER_CORRECTS_INPUT
  ↓
SUBMITTING
```

---

# 43. Prevent Duplicate Requests

During conversion:

```text
Convert button = disabled
```

This prevents accidental multiple API requests.

After the response:

```text
Convert button = enabled
```

---

# 44. Responsive Design

The application must support:

* Desktop
* Laptop
* Tablet
* Mobile

Desktop:

```text
HL7 Input | FHIR Output
```

Mobile:

```text
HL7 Input

[Convert]

FHIR Output
```

The interface must not require horizontal scrolling for normal application content.

The FHIR JSON viewer may have its own controlled horizontal scrolling if long lines exist.

---

# 45. Accessibility

The frontend should follow basic accessibility practices.

Requirements:

* Every input must have a label.
* Buttons must have clear text.
* Do not rely only on color to communicate errors/success.
* Keyboard navigation should work.
* Focus states should be visible.
* Error messages should be readable.
* Text should have sufficient contrast.
* Form controls should have accessible names.
* Avoid tiny text.

Example:

Bad:

```text
[🔴]
```

Better:

```text
✕ Conversion failed
```

---

# 46. Error Message Design

Error messages should answer:

```text
What went wrong?
What should the user do?
```

Bad:

```text
Error 400
```

Better:

```text
Invalid HL7 message.
Please check that the required PID segment is present.
```

Backend errors should be displayed only when they are safe for the user.

---

# 47. Confirmation Messages

Success messages should be short.

Examples:

```text
Conversion successful.
```

```text
FHIR JSON copied to clipboard.
```

Avoid unnecessary long notifications.

---

# 48. Toast Notifications

Toast notifications may be used for temporary events such as:

* JSON copied.
* Logout successful.
* Minor API notification.

Do not use toasts for critical validation errors that need to remain visible.

Critical conversion errors should appear directly in the converter area.

---

# 49. Modal Usage

Avoid excessive modal dialogs.

Modals may be used for:

* Destructive action confirmation.
* Important confirmation requiring user attention.

Normal conversion should NOT open a modal.

The conversion result should appear directly on the page.

---

# 50. FHIR Resource Visualization

The MVP should prioritize JSON output.

The UI may optionally display basic resource information above the JSON:

```text
FHIR Resource
Patient

FHIR Version
R4

Status
Generated successfully
```

Do not build a complex graphical FHIR explorer unless explicitly required.

---

# 51. HL7 and FHIR Side-by-Side Experience

The converter page should make transformation easy to understand.

Ideal structure:

```text
┌───────────────────────────────┐
│ HL7 v2 Input                  │
│                               │
│ PID|1||P1001...               │
│                               │
└───────────────────────────────┘

              ↓
       Convert to FHIR

┌───────────────────────────────┐
│ FHIR R4 Output                │
│                               │
│ {                             │
│   "resourceType": "Patient"   │
│ }                             │
└───────────────────────────────┘
```

This is an important visual representation of the project's core purpose.

---

# 52. UI Must Not Implement Healthcare Logic

The frontend must NOT contain code that decides:

```text
PID-3 means Patient.identifier
PID-5 means Patient.name
PID-7 means Patient.birthDate
```

Those mappings belong to the backend.

The frontend only displays the input and output.

Correct:

```text
React
 ↓
Send HL7
 ↓
Backend
 ↓
Receive FHIR
 ↓
Display FHIR
```

Incorrect:

```text
React
 ↓
Parse HL7
 ↓
Create FHIR
 ↓
Send FHIR
```

---

# 53. UI Must Not Parse HL7

Do not implement:

```javascript
hl7Message.split("|")
```

or similar healthcare parsing logic inside React components.

HL7 parsing is a backend responsibility.

---

# 54. UI Must Not Generate FHIR

Do not manually construct:

```javascript
{
    resourceType: "Patient"
}
```

as the actual healthcare conversion logic in frontend code.

FHIR generation belongs to:

```text
Spring Boot
+
HL7 Mapper
+
HAPI FHIR
```

The frontend only renders the response.

---

# 55. API Response Handling

The frontend should be designed against the backend response contract.

Example conceptual response:

```json
{
  "success": true,
  "message": "HL7 converted successfully",
  "resourceType": "Patient",
  "fhirResource": {
    "resourceType": "Patient",
    "id": "P1001"
  }
}
```

The exact response structure must follow `REQUIREMENTS.md` and backend implementation.

Do not invent a different response contract inside React.

---

# 56. Frontend Folder Structure

Recommended:

```text
frontend/
├── public/
├── src/
│   ├── assets/
│   ├── components/
│   │   ├── Navbar.jsx
│   │   ├── Button.jsx
│   │   ├── Card.jsx
│   │   ├── Alert.jsx
│   │   ├── LoadingSpinner.jsx
│   │   ├── HL7Editor.jsx
│   │   ├── FhirViewer.jsx
│   │   └── ProtectedRoute.jsx
│   │
│   ├── pages/
│   │   ├── Login.jsx
│   │   ├── Dashboard.jsx
│   │   ├── Converter.jsx
│   │   └── History.jsx
│   │
│   ├── services/
│   │   ├── apiClient.js
│   │   ├── authService.js
│   │   ├── hl7Service.js
│   │   └── historyService.js
│   │
│   ├── context/
│   │   └── AuthContext.jsx
│   │
│   ├── hooks/
│   │
│   ├── utils/
│   │
│   ├── App.jsx
│   ├── main.jsx
│   └── styles/
│
├── package.json
└── README.md
```

The AI agent may adjust this structure if there is a strong technical reason, but it must preserve the separation of concerns.

---

# 57. Component Responsibility Rules

### Navbar

Responsible for:

* Navigation
* Active route
* Logout action

Not responsible for:

* API conversion
* HL7 parsing

### HL7Editor

Responsible for:

* Displaying/editing HL7 input.
* Input interaction.
* Clear/load-example actions.

Not responsible for:

* Parsing HL7.
* Validating healthcare rules.

### FhirViewer

Responsible for:

* Displaying FHIR JSON.
* Copy action.
* Formatting.

Not responsible for:

* Creating FHIR.

### ProtectedRoute

Responsible for:

* Authentication-based route protection.

Not responsible for:

* Healthcare processing.

---

# 58. Frontend Error Boundary

A React error boundary may be used to prevent the entire application from crashing because of an unexpected rendering error.

Example:

```text
Something went wrong.

Please refresh the page or try again.
```

Do not expose internal JavaScript stack traces to users.

---

# 59. Performance Requirements

The frontend should remain lightweight.

Avoid:

* Large unnecessary dependencies.
* Heavy animations.
* Huge UI libraries for simple components.
* Loading unnecessary data.
* Repeated API requests.

The converter should make one conversion API request per user submission unless retry behavior is explicitly required.

---

# 60. Security Requirements

Frontend security requirements:

* Never hardcode JWT secrets.
* Never hardcode database credentials.
* Never expose Spring Boot secrets.
* Never expose API keys.
* Do not log sensitive healthcare data unnecessarily.
* Avoid displaying PHI in browser console.
* Use HTTPS in production.
* Handle authentication failures.
* Protect authenticated routes.
* Do not trust frontend validation as backend security.

The backend remains the security authority.

---

# 61. PHI Handling in UI

The application may process healthcare information.

Therefore:

* Do not unnecessarily display patient information outside relevant screens.
* Do not include patient data in debug logs.
* Do not place HL7/FHIR data into analytics events.
* Do not expose conversion data to unauthenticated users.
* Do not show PHI in generic error messages.
* Keep history access authenticated.

The UI should follow the principle of minimum necessary exposure.

---

# 62. Browser Refresh Behavior

When the user refreshes the application:

* Authentication state should be restored if the chosen authentication mechanism supports it.
* Protected pages should remain protected.
* Unsaved HL7 input does not need to survive refresh in the MVP unless explicitly implemented.
* No healthcare data should be persisted in browser storage unnecessarily.

---

# 63. Navigation After Conversion

After successful conversion:

The user should remain on the converter page.

Do NOT automatically navigate away.

Reason:

The user needs to inspect the FHIR output.

Available actions:

```text
Copy JSON
Clear
New Conversion
```

A "New Conversion" action may simply clear the current state.

---

# 64. Dashboard-to-Converter Flow

Recommended:

```text
Dashboard
   ↓
Start New Conversion
   ↓
/convert
```

The converter should immediately show the HL7 input area.

---

# 65. History-to-Converter Flow

If history supports re-opening a conversion:

```text
History
   ↓
View
   ↓
Conversion Details
```

Optional future functionality:

```text
Use as New Input
```

This should not be implemented unless it provides real value.

---

# 66. UI States Checklist

Every major API-driven screen should consider:

### Initial

```text
No data yet
```

### Loading

```text
Loading...
```

### Success

```text
Data displayed
```

### Empty

```text
No records available
```

### Error

```text
Readable error message
```

### Unauthorized

```text
Redirect to login
```

These states must be explicitly implemented.

---

# 67. Form Behavior

Forms should:

* Validate required fields.
* Prevent accidental duplicate submissions.
* Display validation errors close to the field.
* Preserve user input after validation errors.
* Clear state only when appropriate.
* Support keyboard submission where sensible.

---

# 68. Desktop Priority

The project is primarily a developer/integration tool.

Therefore, desktop and laptop experience should be prioritized.

Ideal desktop converter:

```text
--------------------------------------------------------------
| HL7 Input                         | FHIR Output             |
|                                   |                         |
| Large editor                      | Large JSON viewer      |
|                                   |                         |
--------------------------------------------------------------
```

However, the application must still remain functional on mobile.

---

# 69. Mobile Behavior

On mobile:

```text
Navbar
   ↓
Page Title
   ↓
HL7 Input
   ↓
Actions
   ↓
FHIR Output
```

Buttons should be large enough to tap.

Input/output areas should have appropriate height.

Do not force two-column layouts on narrow screens.

---

# 70. Visual Hierarchy

The strongest visual hierarchy should be:

```text
1. Page purpose
2. HL7 input
3. Convert action
4. FHIR output
5. Secondary actions
```

The user should never confuse secondary dashboard statistics with the main conversion functionality.

---

# 71. Empty Dashboard

If there are no previous conversions:

```text
No conversions yet.

Start your first HL7 → FHIR conversion.
```

Provide:

```text
[ Start Conversion ]
```

Do not show empty tables without explanation.

---

# 72. History Empty State

If no history exists:

```text
No conversion history yet.

Your completed conversions will appear here.
```

---

# 73. Button Hierarchy

Primary:

```text
Convert to FHIR
```

Secondary:

```text
Load Example
Clear
Copy JSON
```

Destructive:

```text
Delete
```

if implemented later.

The primary CTA should be visually stronger than secondary actions.

---

# 74. Avoid Unnecessary Features

The AI agent must NOT add the following merely for visual complexity:

* Chatbot
* AI assistant
* Analytics dashboard
* Complex charts
* Dark mode unless specifically requested
* Notification center
* Social features
* User management dashboard
* Admin panel
* Complex settings
* Real-time WebSockets
* Drag-and-drop HL7 file upload unless explicitly required

The goal is a focused interoperability engine.

---

# 75. Frontend Architecture Boundary

The frontend follows:

```text
UI
 ↓
Component
 ↓
Page
 ↓
Service
 ↓
Axios
 ↓
Backend API
```

It must NOT follow:

```text
UI
 ↓
HL7 Parser
 ↓
FHIR Mapper
 ↓
Database
```

---

# 76. UI/UX and Backend Contract

The frontend must follow backend contracts defined in:

```text
agent/REQUIREMENTS.md
agent/ARCHITECTURE.md
```

If the backend API contract changes, the frontend must be updated accordingly.

The AI agent must not silently create a new API format just to simplify frontend implementation.

---

# 77. API Base URL Configuration

The frontend should not hardcode environment-specific backend URLs throughout the code.

Use environment configuration.

Conceptual:

```text
Development:
http://localhost:8080

Production:
Configured deployment URL
```

Use a centralized Axios configuration.

---

# 78. CORS Awareness

The frontend will likely run separately from the Spring Boot backend during development.

Example:

```text
React
localhost:5173

Spring Boot
localhost:8080
```

The backend must allow the required frontend origin through CORS configuration.

CORS configuration belongs to the backend.

The frontend should not attempt to bypass CORS.

---

# 79. Frontend Testing

The frontend should be tested for important user interactions.

Minimum useful scenarios:

### Login

* Valid login.
* Invalid login.
* Empty credentials.
* Loading state.

### Converter

* Empty HL7 submission.
* Valid HL7 conversion.
* Invalid HL7 response.
* Loading state.
* Successful FHIR output.
* Copy JSON.
* Clear input.

### Authentication

* Access protected route without authentication.
* Expired/invalid token.
* Logout.

### History

* History loads.
* Empty history.
* API error.

---

# 80. UI Acceptance Criteria

The UI is considered complete when:

* User can log in.
* Protected routes work.
* Dashboard loads.
* Converter page loads.
* User can paste multiline HL7.
* User can load a sample HL7 message.
* User can clear the editor.
* User can submit the HL7 message.
* Loading state is visible.
* Duplicate submissions are prevented.
* Backend success response is displayed.
* FHIR JSON is readable.
* Copy functionality works if implemented.
* Backend errors are shown clearly.
* Empty states are handled.
* Logout works.
* Mobile layout remains usable.
* No HL7/FHIR business logic exists inside React.
* API calls are separated into service modules.
* Sensitive information is not unnecessarily exposed in the UI or browser logs.

---

# 81. UI Definition of Done

The frontend should not be considered complete merely because the page visually renders.

It is complete only when:

```text
UI
+
Routing
+
Authentication
+
API Integration
+
Loading States
+
Error Handling
+
Success States
+
Responsive Layout
+
Accessibility Basics
+
Security Boundaries
+
Testing
```

are implemented and verified.

---

# 82. AI Agent Implementation Rules

The AI coding agent must follow these rules while implementing the frontend.

### Rule 1

Read all files inside:

```text
agent/
```

before making major implementation decisions.

### Rule 2

Follow `ARCHITECTURE.md`.

### Rule 3

Follow API requirements from `REQUIREMENTS.md`.

### Rule 4

Do not move healthcare logic into React.

### Rule 5

Do not create duplicate API implementations.

### Rule 6

Use reusable components where appropriate.

### Rule 7

Do not introduce unnecessary libraries.

### Rule 8

Do not create fake backend data for final functionality.

Mock data may be used temporarily during development but must be clearly isolated and removed/replaced when real APIs are available.

### Rule 9

Do not silently change backend API contracts.

### Rule 10

Do not silently add major features outside the PRD.

### Rule 11

Keep the UI professional and focused.

### Rule 12

Avoid excessive gradients, animations, and decorative elements.

### Rule 13

Do not log HL7 messages or FHIR responses unnecessarily.

### Rule 14

If an API contract is unclear, inspect the backend requirements/architecture before inventing a contract.

### Rule 15

If a UI requirement conflicts with backend architecture, preserve the architecture and update the UI implementation accordingly.

---

# 83. Final Frontend Architecture

The intended frontend architecture is:

```text
                         USER
                           │
                           ▼
                    React Application
                           │
              ┌────────────┴────────────┐
              │                         │
           Pages                    Components
              │                         │
              └────────────┬────────────┘
                           │
                           ▼
                       Services
                           │
                           ▼
                         Axios
                           │
                           ▼
                  Spring Boot REST API
                           │
                           ▼
                    Backend Processing
                           │
                           ▼
                     JSON Response
                           │
                           ▼
                         Axios
                           │
                           ▼
                       Services
                           │
                           ▼
                         React
                           │
                ┌──────────┴──────────┐
                │                     │
           Success UI              Error UI
                │
                ▼
             FHIR Viewer
```

---

# 84. Final Converter Experience

The most important screen should provide this experience:

```text
                HL7 v2 → FHIR R4
        Healthcare Interoperability Engine

 ┌─────────────────────────┐
 │ HL7 v2 Input             │
 │                          │
 │ MSH|^~\&|...             │
 │ PID|1||P1001...          │
 │ PV1|1|I|...              │
 │                          │
 └─────────────────────────┘

        [ Load Example ]
        [ Clear ] [ Convert to FHIR ]

                  ↓

 ┌─────────────────────────────────────────┐
 │ ✓ Conversion successful                 │
 │                                         │
 │ FHIR R4 Output                          │
 │                                         │
 │ {                                       │
 │   "resourceType": "Patient",             │
 │   "id": "P1001",                         │
 │   ...                                    │
 │ }                                       │
 │                                         │
 │ [ Copy JSON ]                            │
 └─────────────────────────────────────────┘
```

This screen represents the core value proposition of the entire project.

---

# 85. Final UI Principle

The frontend should communicate one simple concept:

```text
RAW HL7
   ↓
CONVERT
   ↓
FHIR R4
```

Everything else in the UI exists to make this workflow:

* secure,
* understandable,
* readable,
* reliable,
* professional,
* and easy to demonstrate.

The frontend must remain a presentation and API-consumption layer.

All HL7 parsing, validation, healthcare mapping, FHIR resource creation, persistence, and business logic belong to the Spring Boot backend.
