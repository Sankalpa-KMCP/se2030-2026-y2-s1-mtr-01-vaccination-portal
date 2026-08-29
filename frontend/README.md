# Frontend Application

The frontend application for the Web-Based Vaccination Portal is a single-page application built with React, Vite, and Bootstrap 5.

## Declared Technology Baseline

- **Framework**: React 19 (v19.2.8)
- **Build Tool / Bundler**: Vite 8 (v8.1.5)
- **UI Styling Framework**: Bootstrap 5 (v5.3.8)
- **Language**: JavaScript (ESNext)

## Prerequisites

- **Node.js**: v18+ or v20+ recommended (verified on Node.js v24.12.0)
- **npm**: v9+ or v10+ recommended (verified on npm v11.6.2)

## Environment Setup

Copy `.env.example` to create your local environment configuration file:

```bash
cp .env.example .env
```

Default environment variables:

- `VITE_API_BASE_URL`: Optional backend REST API base override (default: `/api`). During local Vite development, `/api` is proxied to `http://localhost:8080`.

*Note: Do not commit `.env` files to version control.*

## Available Commands

From the `frontend/` directory, the following standard npm scripts are available:

### Install Dependencies
```bash
npm install
```

### Start Development Server
```bash
npm run dev
```

### Build for Production
```bash
npm run build
```

### Lint Codebase
```bash
npm run lint
```

### Preview Production Build Locally
```bash
npm run preview
```

## Directory Structure

```text
frontend/
├── README.md
├── package.json
├── package-lock.json
├── vite.config.js
├── eslint.config.js
├── index.html
├── .env.example
├── public/
└── src/
    ├── app/
    │   └── App.jsx
    ├── assets/
    ├── components/
    │   └── common/
    │       ├── Header.jsx
    │       └── Footer.jsx
    ├── services/
    │   └── apiClient.js
    ├── styles/
    │   └── global.css
    └── main.jsx
```

## Scope and Current Status

This directory currently contains only the **shared frontend foundation** (application shell, Bootstrap layout, and API client baseline).

- No business modules (MF-01 through MF-05) are currently implemented in this foundation.
- Business functionality will be added incrementally through reviewed feature branches adhering to canonical project specifications.
