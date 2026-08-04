# Web-Based Vaccination Portal

## Project Overview

The Web-Based Vaccination Portal supports vaccination-program operations for citizens, healthcare personnel, administrators, and authorized external integrations. The exact scope of the supported operations is defined by the canonical project documents.

## Five Major Functions

1. Pre-Vaccination Screening and Eligibility Rule Management
2. Vaccination Campaign, Center and Session Management
3. Appointment, Waitlist and Check-In Management
4. Vaccine Inventory and Batch Management
5. Vaccination Administration, Draft Records and Digital Certificate Management

## Current Status

Repository initialization and project foundation

## Canonical Documentation

- [AI Canonical Project Specification](docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md)
- [Master Project Document](docs/Web_Based_Vaccination_Portal_AI_Canonical_Master_v3.3.docx)

The Markdown canonical document is the authoritative AI-readable project source and defines the project baseline, business rules, data model, and technical decisions.

Contributors must review the canonical documents before making functional or architectural changes.

## Repository Structure

- `docs/` — Canonical project documents (AI-readable Markdown source of truth and the human-readable master document)
- `frontend/` — Frontend application source
- `backend/` — Backend application, REST APIs, and integrations
- `database/` — Database migrations, seed data, and design diagrams
- `tests/` — Cross-module, integration, system, end-to-end, security, and acceptance tests
- `.github/` — GitHub issue and pull-request templates

## Development Workflow

- Feature work must not be performed directly on `main`
- Work should use short-lived branches
- Pull requests are required for all changes
- Changes must be reviewed
- Tests and documentation should be updated when applicable
- Canonical project requirements must be followed

## Declared Technology Baseline

The canonical v3.3 project specification defines the following technology baseline:

- Frontend: React, Vite, and Bootstrap 5
- Backend: Java 21 with Spring Boot
- Database: MySQL
- Build tool: Maven

## Setup

Implementation and environment setup instructions will be added as the application foundation is created.

## Security and Privacy

Contributors must never commit:

- Passwords
- API keys
- Access tokens
- `.env` files
- Production credentials
- Real citizen information
- Real medical information
- Real vaccination records
- Production database exports
