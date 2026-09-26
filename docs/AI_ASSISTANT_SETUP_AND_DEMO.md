# AI Assistant Setup, Architecture, and Evaluation Demonstration Guide

This guide describes the architecture, security controls, local configuration, testing, and evaluation procedures for the project-wide conversational AI assistant in the Web-Based Vaccination Portal (WBVP-SE2030).

---

## 1. Architecture Overview

The AI assistant is designed as a secure, privacy-preserving, read-only navigation and portal guidance system. The architecture maintains a strict boundary between public informational guidance, authenticated citizen context, and backend orchestration.

```text
React AssistantWidget (Floating UI)
    │
    ▼ (HTTP JSON via apiClient)
Spring AssistantController (/api/v1/assistant/messages)
    │
    ├── 1. Rate Limiter (Token bucket per client IP / JWT principal)
    │
    ├── 2. AssistantRefusalPolicy (Deterministic safety checks)
    │      └── Blocks medical advice, diagnosis, triage & mutation requests
    │
    ├── 3. Optional Authorized Read-Only Tools (Server-selected, authenticated only)
    │      └── MyAppointmentsAssistantTool / MyEligibilityAssistantTool / MyVaccinationSummaryAssistantTool
    │          (Strictly scoped to principal.getUserId(); prompt cannot inject IDs)
    │
    ├── 4. Verified Knowledge Context (AssistantKnowledgeCatalog & ContextBuilder)
    │      └── Grounded repository documentation, MF-01..MF-05 capabilities & routes
    │
    ├── 5. Selected Backend Provider (AiResponseProvider)
    │      ├── OpenAiResponsesProvider (server-side API key)
    │      └── OllamaChatProvider (free local model; loopback only)
    │
    ▼
Structured Response (AssistantChatResponse)
    ├── answer (Sanitized text response)
    ├── suggestedActions (Allowlisted & role-filtered route links)
    └── requestId (Sanitized correlation ID)
    │
    ▼
Authorized React Router Navigation
    └── Rendered as clickable action pills navigating within authorized client routes
```

### Endpoints and Data Contracts

#### 1. Status Endpoint
- **Method & Path**: `GET /api/v1/assistant/status`
- **Authentication**: Optional (Anonymous callers receive `personalizedFeaturesAvailable: false`; callers with a valid Citizen JWT receive `personalizedFeaturesAvailable: true` when AI is enabled).
- **Response Format (`AssistantStatusResponse`)**:
```json
{
  "available": true,
  "personalizedFeaturesAvailable": false,
  "disclaimer": "This assistant explains portal services. It does not provide medical advice or emergency care."
}
```

#### 2. Messages Endpoint
- **Method & Path**: `POST /api/v1/assistant/messages`
- **Authentication**: Optional (Anonymous callers receive public guidance; authenticated callers receive role-aware guidance and citizen-scoped personalized information).
- **Request Format (`AssistantChatRequest`)**:
```json
{
  "message": "How do I book an appointment?",
  "history": [],
  "currentPath": "/"
}
```
- **Response Format (`AssistantChatResponse`)** *(Illustrative example response; model-generated text may vary while JSON schema and allowlisted route authorizations remain deterministic)*:
```json
{
  "answer": "To book an appointment, navigate to the Book Appointment page...",
  "suggestedActions": [
    {
      "label": "Book Appointment",
      "path": "/app/appointments/book"
    }
  ],
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```


---

## 2. Implemented Capabilities

The assistant provides comprehensive assistance across the portal while enforcing deterministic governance:

- **Public Portal Guidance**: Explains public onboarding, registration steps, navigation, and role-based access flows.
- **MF-01 through MF-05 Workflow Explanations**:
  - **MF-01 (Pre-Vaccination Screening & Eligibility)**: Explains dynamic questionnaires, clinical triage criteria, contraindication assessments, and healthcare officer review workflows.
  - **MF-02 (Campaign, Center & Session Management)**: Explains vaccination campaigns, dose programmes, center opening hours, closures, recurring time slots, and capacity management.
  - **MF-03 (Appointment, Waitlist & Check-In)**: Explains slot booking, rescheduling rules, cancellation, waitlist queuing, and clinic arrival check-in.
  - **MF-04 (Vaccine Inventory & Batch Management)**: Explains cold-chain tracking, lot release, vaccine allocations, and waste reduction principles.
  - **MF-05 (Vaccination Administration & Records)**: Explains clinical dose recording, adverse event documentation, and verifiable digital certificate issuance.
- **Role-Aware Route Suggestions**: Evaluates caller authorities (`ROLE_CITIZEN`, `ROLE_ADMIN`, `ROLE_HEALTHCARE_OFFICER`, etc.) and filters suggested navigation pills so users only receive routes they are authorized to access.
- **Citizen-Owned Appointment and Waitlist Status**: For authenticated citizens, `MyAppointmentsAssistantTool` separates active/upcoming records from past/inactive records and provides recorded status, campaign ID, slot ID, and authoritative start/end times from the linked MF-02 time slot (up to 5 per appointment category). It also provides up to 5 recent waitlist records and the current FIFO position for active entries. It never treats `bookedAt` as the scheduled time and does not expose another citizen's queue data.
- **Citizen-Owned Eligibility Status**: For authenticated citizens, `MyEligibilityAssistantTool` retrieves recorded eligibility assessments (latest per campaign, up to 5 campaigns) including campaign ID, recorded status (`ELIGIBLE`, `INELIGIBLE`, `EXPIRED`, `REVOKED`), reviewed time, validity time, and lifecycle state (`ACTIVE`, `EXPIRED`, `REVOKED`). It does not return clinical reason notes or make independent clinical determinations.
- **Citizen-Owned Vaccination / Certificate Summary**: For authenticated citizens, `MyVaccinationSummaryAssistantTool` retrieves finalized vaccination record count, bounded recent administered doses (dose number and administration date/time; up to 5), certificate existence, certificate status, and issue date. Sensitive clinical notes, vaccine products, batch numbers, QR payloads, and verification tokens are excluded from assistant context.
- **Deterministic Safety Refusals**: Deterministically detects and refuses medical diagnosis, personal clinical advice, dosage/treatment recommendations, and emergency queries with safe referrals to national hotlines or clinical staff.
- **Disabled/Unavailable Behavior**: Gracefully indicates unavailable status without disrupting the rest of the portal when AI is disabled or unconfigured.
- **Rate Limiting**: In-memory token bucket rate limiting (5 requests/minute for anonymous clients; 15 requests/minute for authenticated callers) prevents abuse.
- **In-Memory Browser History**: Conversation history is maintained strictly in React component state in browser memory; conversations are cleared on logout, account switch, or manual reset.

---

## 3. Explicit Limitations

To preserve clinical safety, data integrity, and strict system security, the assistant enforces the following explicit limitations:

1. **No Medical Diagnosis or Treatment**: The assistant is strictly an administrative and informational guide. It does not provide medical diagnoses, treatment decisions, or emergency triage.
2. **No Vaccine Recommendations**: The assistant does not recommend specific vaccines, brands, or off-label dosage regimens.
3. **No Automatic Eligibility Decisions**: The assistant explains eligibility rules but cannot grant, modify, or override an eligibility outcome.
4. **No Assistant Database Mutations**: The assistant is 100% read-only. It cannot write to the database, modify user profiles, or trigger side effects.
5. **No Booking, Cancellation, Rescheduling, or Check-In Performed via Chat**: The assistant directs users to authorized UI workflows; it cannot book, cancel, reschedule, or check in an appointment on behalf of the user.
6. **No Stored AI Conversation Table**: Chat transcripts and prompt histories are never persisted in the database.
7. **No Staff Lookup of Arbitrary Citizens**: Staff members cannot use the chat interface to query arbitrary citizen records; personalized tools only read records belonging to the authenticated citizen principal.
8. **No SSE Streaming in the MVP**: Responses are delivered as atomic JSON payloads (`POST /api/v1/assistant/messages`); Server-Sent Events (SSE) are omitted by design for simplicity and deterministic safety verification.
9. **No Guarantee of Personalized Data for Anonymous Users**: Anonymous visitors cannot access personalized data; calling personalized tools requires an authenticated citizen JWT.
10. **Portal Independence**: The entire vaccination portal operates independently and fully functions when the AI assistant is disabled.

---

## 4. macOS Local Configuration

All configuration is performed using standard macOS `zsh` environment variables.

### Step 1: Database & Server Settings

```zsh
# Database credentials template
export DB_URL="jdbc:mysql://localhost:3306/vaccination_portal"
export DB_USERNAME="vaccination_app"
export DB_PASSWORD="<your-local-database-password>"
export SERVER_PORT="8080"
```

### Step 2A: Free Local Ollama Provider

Install [Ollama](https://ollama.com/download), then download the local model once:

```zsh
ollama pull qwen3:4b
```

Ollama's `qwen3:4b` download is approximately 2.5 GB. Start Ollama, then configure the backend in the same terminal used to start Spring Boot:

```zsh
export AI_ASSISTANT_ENABLED="true"
export AI_ASSISTANT_PROVIDER="ollama"
export AI_ASSISTANT_MODEL="qwen3:4b"
export OLLAMA_BASE_URL="http://localhost:11434"
```

No API key, OpenAI account, or API credit is required in this mode. The backend accepts only a loopback Ollama URL so authorized user context cannot be redirected to a remote model server. Model files and inference remain on the local computer.

Confirm that Ollama is running and the model is installed:

```zsh
curl -s http://localhost:11434/api/tags
```

### Step 2B: OpenAI Provider

To prevent secrets from leaking into shell history (`~/.zsh_history`), use a hidden terminal prompt (`read -s`) to export your API key:

```zsh
# Enable the assistant and select the OpenAI provider and model
export AI_ASSISTANT_ENABLED="true"
export AI_ASSISTANT_PROVIDER="openai"
export AI_ASSISTANT_MODEL="<supported-model-name>"

# Securely input the API key without echoing to the screen or history
read -s "OPENAI_API_KEY?OpenAI API key: "
export OPENAI_API_KEY
echo
```

> **IMPORTANT**:
> - Never hardcode or echo `OPENAI_API_KEY` into shell configuration files (`.zshrc`), scripts, or commits.
> - Do not include a real API key in `.env.example` or documentation.
> - `OPENAI_API_KEY` is strictly server-side and must never be provided to the frontend.

---

## 5. Starting the Backend

From the repository root, enter the `backend/` directory and run Spring Boot with the `local` profile:

```zsh
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

- **Default Backend URL**: `http://localhost:8080`
- **Health Check**: `curl -s http://localhost:8080/actuator/health`

---

## 6. Starting the Frontend

From the repository root, enter the `frontend/` directory, install dependencies, and start the Vite development server:

```zsh
cd frontend
npm install
npm run dev
```

- **Default Frontend URL**: `http://localhost:5173`
- **Backend API Proxy**: Vite automatically proxies `/api` requests to `http://localhost:8080`.
- **Custom Backend Port**: If the backend is running on a non-default port (e.g., `8081`), specify the port before starting Vite:
  ```zsh
  export BACKEND_PORT="8081"
  npm run dev
  ```
- **No Frontend Key**: React does **not** require an OpenAI API key or any AI credentials.

---

## 7. Disabled-State Demonstration

The assistant defaults to disabled (`AI_ASSISTANT_ENABLED=false`). In this state, the portal remains fully functional.

### Verification via cURL

```zsh
curl -s http://localhost:8080/api/v1/assistant/status
```

**Expected Response**:
```json
{
  "available": false,
  "personalizedFeaturesAvailable": false,
  "disclaimer": "This assistant explains portal services. It does not provide medical advice or emergency care."
}
```

### Expected UI Behavior
1. The floating assistant launcher button remains visible in the bottom-right corner of the screen.
2. Clicking the launcher opens the assistant panel with an informative banner indicating that the assistant is currently unavailable or undergoing maintenance.
3. The message input field and send button are disabled to prevent failed requests.
4. A **Retry** button is available to re-check service status.
5. All other portal functions (landing page, registration, login, dashboards) remain 100% operational.

---

## 8. Enabled Public Demonstration

When configured with either a running local Ollama model or valid OpenAI environment variables:

### 1. Check Assistant Status

```zsh
curl -s http://localhost:8080/api/v1/assistant/status
```

**Expected Response**:
```json
{
  "available": true,
  "personalizedFeaturesAvailable": false,
  "disclaimer": "This assistant explains portal services. It does not provide medical advice or emergency care."
}
```

### 2. Submit a Public Informational Query

```zsh
curl -s \
  -X POST \
  http://localhost:8080/api/v1/assistant/messages \
  -H "Content-Type: application/json" \
  -d '{
    "message": "How do I book an appointment?",
    "history": [],
    "currentPath": "/"
  }'
```

**Expected Response** *(Illustrative example response; model-generated text may vary while JSON schema and allowlisted route authorizations remain deterministic)*:
```json
{
  "answer": "To book a vaccination appointment, you can search for available sessions and select an open time slot. If you are already registered as a citizen, sign in and navigate to the Book Appointment page.",
  "suggestedActions": [
    {
      "label": "Book Appointment",
      "path": "/app/appointments/book"
    },
    {
      "label": "Login",
      "path": "/login"
    }
  ],
  "requestId": "..."
}
```

**Expected UI Behavior**:
- The assistant displays the answer as plain text with preserved whitespace and line breaks.
- Suggested action buttons appear below the message.
- Clicking an action pill navigates the user directly to the relevant authorized portal route.

---

## 9. Authenticated Personalized Demonstration

Authenticated citizens can ask about their personal records. Personalized tools require a valid Citizen JWT obtained via the portal login endpoint (`POST /api/v1/auth/login`).

> **Security Principle**: The backend derives caller identity exclusively from the verified JWT principal (`principal.getUserId()`). Any citizen IDs or usernames written into the user's prompt are ignored.

```zsh
curl -s \
  -X POST \
  http://localhost:8080/api/v1/assistant/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <citizen-jwt>" \
  -d '{
    "message": "What is the status of my appointment?",
    "history": [],
    "currentPath": "/app/citizen"
  }'
```

### Safe Example Questions for Evaluators

| Query | Scope | Expected Behavior |
| :--- | :--- | :--- |
| *"What is the status of my appointment?"* | Personalized | Backend calls `MyAppointmentsAssistantTool` with the caller's verified ID; separates active/upcoming and past/inactive records and returns recorded status plus authoritative MF-02 slot times. |
| *"What is my queue position?"* | Personalized | Backend reads only the signed-in citizen's waitlist entry and returns its recorded status and current FIFO position. |
| *"What is my recorded eligibility status?"* | Personalized | Backend calls `MyEligibilityAssistantTool` with caller's ID; returns recorded assessment status (`ELIGIBLE`, `INELIGIBLE`, `EXPIRED`, `REVOKED`), reviewed timestamp, validity time, and lifecycle state. |
| *"Do I have a digital certificate?"* | Personalized | Backend calls `MyVaccinationSummaryAssistantTool` with caller's ID; returns finalized dose count, bounded dose numbers and administration dates/times, and digital certificate status/issue date (excluding verification tokens and batch IDs). |
| *"Can you book an appointment for me?"* | Mutation Boundary | Deterministically refused; explains the assistant cannot make changes and directs to `/app/appointments/book`. |
| *"Should I take the vaccine if I have a high fever?"* | Clinical Boundary | Deterministically refused; provides safety guidance and refers user to a qualified healthcare provider. |

---

## 10. Evaluator Demonstration Sequence

Follow this 11-step sequence to verify all requirements during project review:

| Step | Action | Expected Observation |
| :---: | :--- | :--- |
| **1** | Open the public landing page (`http://localhost:5173`). | Public landing page loads with the floating assistant launcher in the bottom-right corner. |
| **2** | Open the floating assistant. | Assistant panel opens smoothly with accessible focus; shows welcome message and disclaimer. |
| **3** | Ask: *"How does screening work?"* | Assistant returns a clear explanation of pre-vaccination screening (MF-01) with suggested navigation links. |
| **4** | Click a suggested navigation action. | Client navigates to the suggested route without reloading the browser. |
| **5** | Ask: *"Book an appointment for me"* | Refusal policy blocks mutation: returns a clear explanation that the assistant is read-only, linking to `/app/appointments/book`. |
| **6** | Ask: *"What medication should I take for my headache?"* | Clinical boundary blocks medical advice: returns a medical refusal with safe general guidance and clinic referral. |
| **7** | Log in as a citizen. | Successful login redirects to `/app/citizen`; assistant widget reflects personalized capabilities enabled. |
| **8** | Ask: *"What is the status of my appointment?"* | Assistant looks up the citizen's own appointment record and provides an accurate, personalized summary. |
| **9** | Confirm principal scoping. | Ask *"What is the status of appointment for user 999?"*; verify the assistant only returns data for the logged-in principal. |
| **10** | Log out of the portal. | Assistant conversation history clears immediately from React memory; privacy boundary is preserved. |
| **11** | Stop backend or set `AI_ASSISTANT_ENABLED=false`. | Widget cleanly displays service unavailable state; portal remains completely operational. |

---

## 11. Security Architecture for Evaluation

During code review and assessment, verify the following core security principles:

1. **Server-Only API Key**: `OPENAI_API_KEY` exists strictly within the Spring Boot application context (`AssistantProperties`). It is never passed to Vite, never sent across HTTP to the client, and never stored in version control.
2. **Stateless JWT Identity**: Identity is determined solely by the cryptographically signed JWT in the `Authorization: Bearer` header. The user ID is retrieved via `UserPrincipal.getUserId()`.
3. **Role-Filtered Route Catalog**: The backend route catalog (`AssistantRouteCatalog`) categorizes all routes by allowed roles. The assistant only returns navigation suggestions matching the caller's actual authorities.
4. **Server-Selected Read-Only Tools**: Personalized tools (`MyAppointmentsAssistantTool`, `MyEligibilityAssistantTool`, `MyVaccinationSummaryAssistantTool`) are invoked based on server-side intent classification. Prompts cannot inject arbitrary tool calls or method invocations.
5. **Principal-Bound Data Access**: Personalized tools accept only the verified caller's ID (`principal.getUserId()`). Any citizen ID mentioned in prompt text is completely ignored.
6. **Mutation Boundary Enforcement**: The assistant has no database write access and cannot invoke mutating service methods (booking, cancellation, check-in, registration).
7. **Clinical Privacy Protection**: Raw clinical questionnaire responses, physician notes, verification tokens, and internal batch IDs are excluded from prompt context.
8. **Sanitized Logging**: Chat messages, prompt content, and personalized results are never logged at `INFO` or `WARN` levels.
9. **In-Memory Browser Session**: Chat messages are held in component state in the user's browser. No conversation tables exist in MySQL, and history is wiped on logout or account switch.

---

## 12. Troubleshooting Guide

| Issue | Root Cause | Solution |
| :--- | :--- | :--- |
| **Status returns `available: false`** | `AI_ASSISTANT_ENABLED` is `false` or missing. | Ensure `export AI_ASSISTANT_ENABLED="true"` is set in the backend environment before startup. |
| **Ollama status is available but replies fail** | Ollama is stopped or the configured model has not been downloaded. | Start Ollama, run `ollama pull qwen3:4b`, and verify `curl http://localhost:11434/api/tags`. |
| **Missing `OPENAI_API_KEY`** | OpenAI is selected but its secret key was not exported. | Re-run `read -s "OPENAI_API_KEY?OpenAI API key: "` and restart the backend, or select the local Ollama provider. |
| **Missing assistant model** | `AI_ASSISTANT_MODEL` is unset or blank. | Set it to `qwen3:4b` for the documented Ollama setup or a Responses API compatible model for OpenAI. |
| **Backend running on different port** | Port collision or custom port used. | Start frontend with `BACKEND_PORT=8081 npm run dev` (or `export BACKEND_PORT=8081` then `npm run dev`) so Vite proxies to the correct port. |
| **MySQL authentication failure** | Incorrect DB password or MySQL not running. | Verify MySQL is running on port 3306 and verify `DB_PASSWORD`. |
| **Frontend proxy failure (ECONNREFUSED)** | Backend is not started or still initializing. | Start the backend (`./mvnw spring-boot:run`) and confirm `http://localhost:8080/actuator/health` returns `UP`. |
| **HTTP 401 Unauthorized on messages** | Expired or invalid JWT token. | Re-authenticate through `/login` to obtain a fresh JWT. |
| **HTTP 429 Too Many Requests** | Rate limit exceeded (5 RPM anonymous, 15 RPM auth). | Wait 60 seconds before sending additional requests. |
| **HTTP 503 Service Unavailable** | AI is disabled or the selected provider is misconfigured. | Verify `AI_ASSISTANT_ENABLED`, `AI_ASSISTANT_PROVIDER`, and `AI_ASSISTANT_MODEL`; for Ollama also verify the local server. |
| **Accidental frontend key injection** | Attempting to solve errors by adding key to React. | **NEVER** place an OpenAI API key in `frontend/` or `VITE_*` variables. The key belongs exclusively on the backend. |
