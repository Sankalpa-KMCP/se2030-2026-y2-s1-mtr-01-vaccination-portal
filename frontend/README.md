# Frontend Application

The frontend application for the Web-Based Vaccination Portal is a single-page application built with React 19, Vite, React Router DOM, and Bootstrap 5. It provides responsive, accessible interfaces for citizens, healthcare professionals, campaign officers, center managers, and system administrators.

## Declared Technology Baseline

- **Framework**: React 19 (`react`, `react-dom` v19.2+)
- **Routing**: React Router DOM 7 (`react-router-dom` v7.18+)
- **Build Tool / Bundler**: Vite 8 (`vite` v8.1+)
- **UI Styling Framework**: Bootstrap 5 (`bootstrap` v5.3+) and modular Vanilla CSS
- **Testing & Verification**: Vitest 4 (`vitest` v4.1+), JSDOM (`jsdom` v30.0+), and Testing Library React (`@testing-library/react` v16.3+)
- **Linter**: ESLint 9 (`eslint` v9.21+)
- **Language**: JavaScript (ESNext)

## Prerequisites

- **Node.js**: v18+ or v20+ recommended (verified on Node.js v24.x)
- **npm**: v9+ or v10+ recommended (verified on npm v11.x)

## Environment Setup

Copy `.env.example` to create your local environment configuration file:

```bash
cp .env.example .env
```

Default environment variables:

- `VITE_API_BASE_URL`: Optional backend REST API base override (default: `/api`). During local Vite development, `/api` is proxied to `http://localhost:8080`.

*Note: Never commit `.env` files to version control.*

## Available Commands

From the `frontend/` directory, execute:

### Install Dependencies
```bash
npm install
```

### Start Development Server
```bash
npm run dev
```
Starts the local development server at `http://localhost:5173`. Incoming requests matching `/api` are automatically proxied to the backend at `http://localhost:8080`.

### Run Automated Tests
```bash
npm test
```
Executes the automated unit and component test suite using Vitest in a simulated browser (JSDOM) environment.

### Lint Codebase
```bash
npm run lint
```
Runs ESLint across the codebase to verify code quality and rule compliance.

### Build for Production
```bash
npm run build
```
Compiles and bundles the application assets into the `dist/` directory for production deployment.

### Preview Production Build Locally
```bash
npm run preview
```
Serves the compiled production bundle locally for pre-deployment inspection.

## Application Architecture & Routing

The frontend uses React Router DOM with a centralized route tree defined in `src/app/routes.jsx`:

### Public Routes
- `/` - Public home landing page with portal overview, hero artwork, readiness indicators, and trust metrics
- `/how-it-works` - Citizen and provider workflow guide
- `/about` - Public portal mission, architecture, and healthcare governance overview
- `/login` - Universal authentication entry point for all roles (legacy `/login/citizen` and `/login/staff` routes redirect to `/login`)
- `/register/citizen` - Citizen self-registration
- `/register/staff` - Operational and medical staff account registration request

All active citizen and staff accounts authenticate through `/login`. Upon authentication, users are redirected to their designated workspace according to their assigned role. Citizen registration (`/register/citizen`) and staff access requests (`/register/staff`) remain separate workflows.

### Protected Routes (`/app/*`)
Access to `/app` is guarded by `ProtectedRoute` and requires a valid authenticated session. Users are automatically directed to their role-specific view:
- `/app/citizen` - Citizen Dashboard (`ROLE_CITIZEN`)
- `/app/citizen/profile` - Citizen profile management (`ROLE_CITIZEN`)
- `/app/citizen/vaccinations` - Personal administered vaccination records and digital certificates (`ROLE_CITIZEN`, alias `/app/vaccinations`)
- `/app/screening` - Citizen pre-vaccination screening campaigns (`ROLE_CITIZEN`)
- `/app/screening/eligibility` - Citizen pre-vaccination eligibility status overview (`ROLE_CITIZEN`)
- `/app/screening/history` - Screening history, details, and correction submissions (`ROLE_CITIZEN`)
- `/app/appointments/book` - Vaccination session availability search and appointment booking (`ROLE_CITIZEN`)
- `/app/appointments/mine` - Personal appointment listing, rescheduling, and cancellation management (`ROLE_CITIZEN`)
- `/app/appointments/waitlist` - Citizen waitlist status and registration (`ROLE_CITIZEN`)
- `/app/staff` - Staff Operations Dashboard (`ROLE_ADMIN`, `ROLE_CAMPAIGN_OFFICER`, `ROLE_CENTER_MANAGER`, `ROLE_HEALTHCARE_OFFICER`, `ROLE_INVENTORY_OFFICER`, `ROLE_MANAGEMENT_USER`, `ROLE_VACCINATOR`)
- `/app/staff/vaccination-drafts` - Clinical vaccination draft recording and finalization (`ROLE_VACCINATOR`)
- `/app/staff/screening/reviews` - Clinical screening review queue and history (`ROLE_HEALTHCARE_OFFICER`)
- `/app/campaigns` - Vaccination campaign management and dose programme configuration (`ROLE_ADMIN`, `ROLE_CAMPAIGN_OFFICER`, `ROLE_CENTER_MANAGER`, `ROLE_MANAGEMENT_USER`)
- `/app/center-operations` - Center master data, operating hours, and closures (`ROLE_ADMIN`, `ROLE_CAMPAIGN_OFFICER`, `ROLE_CENTER_MANAGER`, `ROLE_MANAGEMENT_USER`)
- `/app/sessions` - Vaccination session planning, recurrence series, and staff rostering (`ROLE_ADMIN`, `ROLE_CENTER_MANAGER`, `ROLE_CAMPAIGN_OFFICER`)
- `/app/analytics` - Operational analytics and program metrics visualization (`ROLE_ADMIN`, `ROLE_CAMPAIGN_OFFICER`, `ROLE_CENTER_MANAGER`)
- `/app/checkin/queue` - Clinic arrival check-in queue management (`ROLE_ADMIN`, `ROLE_CENTER_MANAGER`, `ROLE_VACCINATOR`)
- `/app/inventory` - Vaccine inventory, stock levels, and batch allocations (`ROLE_ADMIN`, `ROLE_INVENTORY_OFFICER`)
- `/app/screening/questions` - Screening question configuration (`ROLE_HEALTHCARE_OFFICER`)
- `/app/screening/questionnaires` - Screening questionnaire versioning (`ROLE_HEALTHCARE_OFFICER`)
- `/app/admin` - System Administration Dashboard (`ROLE_ADMIN`)
- `/app/admin/staff-requests` - Pending staff registration request management (`ROLE_ADMIN`)

## Implemented Feature Areas & Status

| Feature Module | Directory | Current Implementation Status |
| :--- | :--- | :--- |
| **Project-Wide AI Assistant** | `src/features/assistant/` | **Implemented**: Floating `AssistantWidget` accessible on public and authenticated pages, in-memory conversation state, role-filtered route navigation pills, and personalized citizen queries. |
| **Public Landing & Information** | `src/features/public-landing/` | **Implemented**: Multi-page informational experience (`/`, `/how-it-works`, `/about`), accessible navigation, journey timelines, and responsive styling. |
| **Authentication & Access Control** | `src/auth/` | **Implemented**: Stateless JWT authentication, citizen registration, staff registration requests, admin review, and role-based route protection. |
| **Pre-Vaccination Screening & Eligibility** | `src/features/screening/` | **Implemented**: Screening questions/questionnaires, citizen screening workflows, eligibility status, history tracking, and healthcare officer review queue. |
| **Campaign Management** | `src/features/campaign-management/` | **Implemented**: Campaign lifecycle states, dose programme versions, target groups, and location targeting. |
| **Center Operations** | `src/features/center-operations/` | **Implemented**: Vaccination center master records, operating calendars, and closure management. |
| **Session Management** | `src/features/session-management/` | **Implemented**: Recurring session generation, time slots, operational capacity, and staff rostering. |
| **Appointment Management** | `src/features/appointment-management/` | **Implemented**: Availability search, slot booking (`/app/appointments/book`), personal appointment management (`/app/appointments/mine`), citizen waitlist (`/app/appointments/waitlist`), and staff arrival check-in queue (`/app/checkin/queue`). |
| **Vaccine Inventory** | `src/features/inventory-management/` | **Implemented**: Center inventory management, stock levels, lot allocations, and replenishment tracking. |
| **Vaccination Administration** | `src/features/vaccination-administration/` | **Implemented**: Clinical vaccination draft recording, citizen vaccination history (`/app/citizen/vaccinations`), and public digital certificate verification (`/verify-certificate`). |
| **Operational Analytics** | `src/features/analytics/` | **Implemented**: Interactive operations analytics view with filterable program metrics. |

## Project-Wide AI Assistant Widget

The frontend incorporates a floating AI assistant widget (`AssistantWidget`) available across both public pages and authenticated portals:

- **Universal Placement**: Mounted in `src/app/App.jsx` and anchored to the viewport; visible to anonymous visitors, citizens, and staff members.
- **Backend Communication**: Communicates exclusively with backend REST endpoints (`GET /api/v1/assistant/status` and `POST /api/v1/assistant/messages`) through `apiClient.js`.
- **Zero Frontend Secrets**: React **never receives or stores an OpenAI API key**. All model invocations and tool executions occur server-side.
- **In-Memory Conversation Isolation**: Conversation dialogue is maintained strictly in React component state in browser memory. Conversation state automatically clears upon user logout, account switch, or manual reset to guarantee privacy across sessions.
- **Graceful Disabled State**: If the backend reports `available: false` (or is unreachable), the widget displays an accessible notification banner, disables input, and provides a retry button while allowing all other portal features to remain fully operational.
- **Role-Aware Suggested Actions**: Displays suggested route action pills that dynamically adapt based on caller role authorities, enabling quick navigation across the portal.
- **Setup & Evaluation Guide**: See [docs/AI_ASSISTANT_SETUP_AND_DEMO.md](../docs/AI_ASSISTANT_SETUP_AND_DEMO.md) for full configuration and demonstration steps.

## Directory Structure

```text
frontend/
|-- README.md
|-- package.json
|-- package-lock.json
|-- vite.config.js
|-- eslint.config.js
|-- index.html
|-- .env.example
|-- public/
\-- src/
    |-- app/
    |   |-- App.jsx
    |   |-- AuthenticatedLayout.jsx
    |   |-- ProtectedRoute.jsx
    |   |-- routes.jsx
    |   \-- pages/
    |-- auth/
    |   |-- api/
    |   |-- constants/
    |   |-- context/
    |   |-- pages/
    |   \-- utils/
    |-- components/
    |   \-- common/
    |-- features/
    |   |-- analytics/
    |   |-- appointment-management/
    |   |-- assistant/
    |   |   |-- api/
    |   |   |-- components/
    |   |   |-- hooks/
    |   |   \-- styles/
    |   |-- campaign-management/
    |   |-- center-operations/
    |   |-- citizen-profile/
    |   |-- inventory-management/
    |   |-- public-landing/
    |   |-- screening/
    |   |-- session-management/
    |   \-- vaccination-administration/
    |-- services/
    |   \-- apiClient.js
    \-- styles/
        \-- global.css
```
