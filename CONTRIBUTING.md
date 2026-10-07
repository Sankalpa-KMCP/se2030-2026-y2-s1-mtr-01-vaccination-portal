# Contributing

Thank you for contributing to the Web-Based Vaccination Portal.

## Contribution Principles

- Follow the latest canonical project specification
- Keep changes limited to the assigned issue
- Do not combine unrelated features in one pull request
- Update tests and documentation when relevant

## Branch Naming

Use short-lived branches with descriptive names:

```text
feature/<short-description>
fix/<short-description>
docs/<short-description>
test/<short-description>
refactor/<short-description>
chore/<short-description>
```

Direct feature work should not be performed on `main`.

## Pull Requests

All changes must be submitted as pull requests. Each pull request should have:

- A linked issue when applicable
- A clear summary
- Relevant testing evidence
- At least one team review
- Resolution of review comments
- No unresolved merge conflicts

## Commit Messages

Use conventional-style commit message prefixes:

```text
feat:
fix:
docs:
test:
refactor:
chore:
```

Examples:

```text
docs: add canonical project documentation
chore: initialize repository structure
feat: add citizen registration endpoint
fix: prevent duplicate active appointments
test: add eligibility-rule unit tests
```

## Security and Privacy

Committing the following is strictly prohibited:

- Secrets
- Credentials
- Tokens
- Real citizen data
- Real medical data
- Real vaccination records
- Production database exports

## Definition of Done

A change is complete only when:

- Acceptance criteria are satisfied
- Relevant tests pass
- Documentation is updated where necessary
- Security and privacy implications are reviewed
- No secrets or real personal data are present
- The pull request is reviewed
- The change is compatible with canonical requirements

## Continuous Integration & Local Verification

Before submitting a pull request, contributors must run local validation commands to ensure CI checks will pass:

### Frontend Verification
```bash
cd frontend
npm test
npm run lint
npm run build
```

### Backend Verification
```bash
cd backend
./mvnw verify

# Optional targeted/full assurance against MySQL Testcontainers:
./mvnw verify -Pmysql-concurrency
```

On Windows (PowerShell):
```powershell
cd backend
.\mvnw.cmd verify

# Optional targeted/full assurance against MySQL Testcontainers:
.\mvnw.cmd verify -Pmysql-concurrency
```

Pull requests must pass all automated GitHub Actions CI checks (`Frontend CI` and `Backend CI`) before being merged.

## Conflict Resolution

Resolve conflicts using:

1. Latest canonical project specification
2. Approved GitHub issues and architectural decisions
3. Team discussion
4. Team-leader coordination when necessary

## Architecture changes

Use provider-owned APIs and immutable snapshots for cross-feature facts. Do not import another
feature's repositories/entities or internal services. The documented Assistant application services
and single-statement SessionImpact JPQL join are explicit exceptions, not general permission.
Place implementation providers under capability `query`/`adapter`, not public `api`.
Frontend route composition stays in `app`; business workflow hooks/APIs/styles stay in their feature.

Backend `FeatureBoundaryTests` runs in the ordinary Maven suite; frontend ESLint boundary checks
run with `npm run lint`. Tool unit tests mock provider queries, provider tests mock/persist repositories,
and integration tests use real production wiring. After Java package moves, remove stale compiled
output with Maven `clean` before validating; never mistake old `target` classes for current source.
Applied Flyway migrations are immutable; only `classpath:db/migration` is active.
See [current architecture](docs/Web_Based_Vaccination_Portal_Recommended_Full_File_Tree.md).
