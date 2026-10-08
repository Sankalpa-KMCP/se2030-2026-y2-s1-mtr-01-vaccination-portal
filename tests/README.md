# Test ownership and validation

This directory is reserved for cross-module/system/acceptance resources. Executable Java tests live
under `backend/src/test/java` and follow the feature capability they verify. Shared infrastructure
and architecture checks have their own test packages. React tests are colocated with components,
pages and hooks; ESLint RuleTester coverage lives under `frontend/eslint`.

Assistant tool unit tests mock provider-owned queries. Provider tests exercise persistence fixtures
and selection policy; `AssistantProviderQueryIntegrationTests` uses a real Spring graph with
persisted other-user exclusion cases. The five deterministic F1/F4 timing tests stay at provider
boundaries and use controlled Clocks, never sleeps. Workflow-hook tests cover stale responses and
serialized previews/mutations, alongside existing rendered-page tests.

From `backend`: `.\mvnw.cmd test` runs the default H2 suite, including architecture tests.
`.\mvnw.cmd test -Pmysql-concurrency` selects the existing MySQL/Testcontainers tagged suite;
it requires an already available Docker engine. It is separate from the default suite.
From `frontend`: `npm test`, `npm run lint`, `npm run build`.

Use synthetic data only. Do not weaken assertions to make a package move pass.
