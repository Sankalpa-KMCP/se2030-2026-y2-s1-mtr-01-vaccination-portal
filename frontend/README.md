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
- `/` â€” Public home landing page with portal overview, hero artwork, readiness indicators, and trust metrics
- `/how-it-works` â€” Citizen and provider workflow guide
- `/about` â€” Public portal mission, architecture, and healthcare governance overview
- `/login` â€” Generic authentication entry point
- `/login/citizen` â€” Dedicated citizen authentication
- `/login/staff` â€” Operational and healthcare staff authentication
- `/register/citizen` â€” Citizen self-registration
- `/register/staff` â€” Operational and medical staff account registration request

### Protected Routes (`/app/*`)
Access to `/app` is guarded by `ProtectedRoute` and requires a valid authenticated session. Users are automatically directed to their role-specific view:
- `/app/citizen` â€” Citizen Dashboard (accessible to `ROLE_CITIZEN`)
- `/app/staff` â€” Staff Operations Dashboard (accessible to healthcare, campaign, and center staff)
- `/app/admin` â€” System Administration Dashboard (`ROLE_ADMIN`)
- `/app/admin/staff-requests` â€” Pending staff registration request management (`ROLE_ADMIN`)
- `/app/campaigns` â€” Vaccination campaign management and dose programme configuration
- `/app/center-operations` â€” Center master data, operating hours, and closures
- `/app/sessions` â€” Vaccination session planning, recurrence series, and staff rostering
- `/app/analytics` â€” Operational analytics and program metrics visualization
- `/app/screening/questions` â€” Screening question configuration (`ROLE_ADMIN`)
- `/app/screening/questionnaires` â€” Screening questionnaire versioning (`ROLE_ADMIN`)

## Implemented Feature Areas & Status

| Feature Module | Directory | Current Implementation Status |
| :--- | :--- | :--- |
| **Public Landing & Information** | `src/features/public-landing/` | **Implemented**: Multi-page informational experience (`/`, `/how-it-works`, `/about`), accessible navigation, journey timelines, and responsive styling. |
| **Authentication & Access Control** | `src/auth/` | **Implemented**: Stateless JWT authentication, citizen registration, staff registration requests, admin review, and role-based route protection. |
| **Campaign Management** | `src/features/campaign-management/` | **Implemented**: Campaign lifecycle states, dose programme versions, target groups, and location targeting. |
| **Center Operations** | `src/features/center-operations/` | **Implemented**: Vaccination center master records, operating calendars, and closure management. |
| **Session Management** | `src/features/session-management/` | **Implemented**: Recurring session generation, time slots, operational capacity, and staff rostering. |
| **Operational Analytics** | `src/features/analytics/` | **Implemented**: Interactive operations analytics view with filterable program metrics. |
| **Screening & Eligibility** | `src/features/screening/` | **Implemented**: Screening question definition, answer options, and questionnaire version administration. |
| **Vaccination Administration** | `src/features/vaccination-administration/` | **Partial**: Certificate verification form component implemented; complete clinical administration workflow UI is pending. |
| **Appointment Management** | `src/features/appointment-management/` | **Pending**: Booking guard, slot selection, and queue management UI are pending integration (stub directories only). |
| **Vaccine Inventory** | `src/features/inventory-management/` | **Planned**: Not implemented in current frontend release. |

## Directory Structure

```text
frontend/
â”œâ”€â”€ README.md
â”œâ”€â”€ package.json
â”œâ”€â”€ package-lock.json
â”œâ”€â”€ vite.config.js
â”œâ”€â”€ eslint.config.js
â”œâ”€â”€ index.html
â”œâ”€â”€ .env.example
â”œâ”€â”€ public/
â””â”€â”€ src/
    â”œâ”€â”€ app/
    â”‚   â”œâ”€â”€ App.jsx
    â”‚   â”œâ”€â”€ AuthenticatedLayout.jsx
    â”‚   â”œâ”€â”€ ProtectedRoute.jsx
    â”‚   â”œâ”€â”€ routes.jsx
    â”‚   â””â”€â”€ pages/
    â”œâ”€â”€ auth/
    â”‚   â”œâ”€â”€ api/
    â”‚   â”œâ”€â”€ constants/
    â”‚   â”œâ”€â”€ context/
    â”‚   â”œâ”€â”€ pages/
    â”‚   â””â”€â”€ utils/
    â”œâ”€â”€ components/
    â”‚   â””â”€â”€ common/
    â”œâ”€â”€ features/
    â”‚   â”œâ”€â”€ analytics/
    â”‚   â”œâ”€â”€ appointment-management/
    â”‚   â”œâ”€â”€ campaign-management/
    â”‚   â”œâ”€â”€ center-operations/
    â”‚   â”œâ”€â”€ public-landing/
    â”‚   â”œâ”€â”€ screening/
    â”‚   â”œâ”€â”€ session-management/
    â”‚   â””â”€â”€ vaccination-administration/
    â”œâ”€â”€ services/
    â”‚   â””â”€â”€ apiClient.js
    â””â”€â”€ styles/
        â””â”€â”€ global.css
```
