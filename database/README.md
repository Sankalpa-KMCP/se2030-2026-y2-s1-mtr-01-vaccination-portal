# Database Configuration and Local Environment Guide

This directory contains database-related documentation, environment configuration, migration scripts, seed data, and design diagrams for the Web-Based Vaccination Portal.

## Declared Technology Baseline

- **Database Engine**: MySQL 8.4.10 LTS
- **Local Runtime Environment**: Docker Compose (recommended reproducible development workflow)
- **Port**: 3306 (default local mapping)
- **Database Name**: `vaccination_portal`

## Directory Purpose

- `migrations/` — Future version-controlled schema migration scripts (Flyway/Liquibase)
- `seeds/` — Future synthetic development and testing seed data
- `diagrams/` — Entity-Relationship (ER) diagrams and database design materials

*Note: No business database schema or business tables have been created yet. Schema migrations will be introduced incrementally on assigned feature branches.*

## Local Database Setup

### 1. Environment Setup

Copy `.env.example` to create your local environment configuration file:

#### Windows (PowerShell)
```powershell
Copy-Item database/.env.example database/.env
```

#### Unix-like (macOS / Linux)
```bash
cp database/.env.example database/.env
```

*Important: Replace placeholder passwords in `database/.env` with your secure local values. Do not commit `database/.env` to version control.*

### 2. Management Commands

Execute the following commands from the repository root:

#### Start MySQL Container
```bash
docker compose --env-file database/.env up -d mysql
```

#### Check Container Health Status
```bash
docker compose ps
```

#### Stop Container (Preserve Local Data)
```bash
docker compose --env-file database/.env down
```

#### Stop Container and Wipe Volume (Full Reset)
```bash
docker compose --env-file database/.env down -v
```

> [!WARNING]
> Running `down -v` permanently removes the project's local MySQL Docker volume (`mysql-data`) and all local data stored inside it.

## Backend Connection Details

- **JDBC URL**: `jdbc:mysql://localhost:3306/vaccination_portal`
- **Default Application User**: Configured via `MYSQL_USER` in `database/.env` (matches `DB_USERNAME` in backend configuration)
- **Password**: Configured via `MYSQL_PASSWORD` in `database/.env` (matches `DB_PASSWORD` in backend configuration)

Ensure your backend application configuration (`backend/src/main/resources/application.properties` or environment variables) matches your local `database/.env` settings.

## Security & Privacy Guidelines

Contributors must strictly adhere to the following policies:

- **Never commit `database/.env`** or any file containing real passwords or credentials.
- **Never use real citizen information**, medical records, or actual vaccination data.
- **Future seed data must be 100% synthetic.**
- **Never commit production database dumps** or exports to this repository.
