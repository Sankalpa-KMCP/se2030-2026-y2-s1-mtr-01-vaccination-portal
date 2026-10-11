# LinkedIn Showcase Pack: Web-Based Vaccination Portal (WBVP)

This guide provides publication-ready post copy, technical metrics, visual layout recommendations, and hashtag strategies to showcase the **Web-Based Vaccination Portal** on LinkedIn.

---

## 📊 Key Metrics Card (For Post Graphics / Headers)

| Metric | Verified Count | Significance |
| :--- | :--- | :--- |
| **Java Classes** | `1,043` Total (`700` Main / `343` Test) | Enterprise-scale Spring Boot codebase |
| **Automated Tests** | `4,200+` Across Full Stack | Dual-test pyramid with zero CI failures |
| **Database Schema** | `59` Relational Tables | Managed across `28` Flyway migrations (V0–V27) |
| **Pessimistic Locks** | 3-Level Hierarchy (`Campaign -> Session -> Slot`) | Zero-oversell guarantee under high concurrency |
| **PDF Engine** | 100% Pure Java (Zero Dependencies) | Eliminates Apache PDFBox / iText vulnerabilities |
| **Frontend Stack** | React 19 + Vite 8 + React Router 7 | Memory-only JWTs & AST-enforced boundaries |

---

## 📝 Option A: Long-Form Technical Engineering Post (Recommended)

*Best suited for engineering managers, tech leads, recruiters, and distributed systems engineers.*

```markdown
Building software for healthcare requires a fundamentally different engineering mindset than building standard CRUD applications.

When human immunization records, scarce clinic capacity, and clinical accountability are on the line, race conditions, overbooking, and token leaks are unacceptable.

Over the past months, our team architected and built the Web-Based Vaccination Portal (WBVP) — an enterprise healthcare orchestration platform built on Java 21, Spring Boot, MySQL 8.4 LTS, and React 19.

Here is a look under the hood at the architectural decisions and engineering feats powering the system:

1️⃣ Concurrency & Zero-Oversell Guarantees
Handling massive concurrent booking spikes without overbooking required strict concurrency control. We engineered a 3-level hierarchical pessimistic locking pattern:
Campaign ➔ Session ➔ TimeSlot
By acquiring row locks in strict sequence via `SELECT ... FOR UPDATE`, cyclic database deadlocks are eliminated. Paired with citizen-campaign guard-row mutexes, concurrent appointments and waitlist promotions serialize cleanly, guaranteeing zero over-allocation even when dozens of citizens compete for the final slot.

2️⃣ Zero-Compromise Security Architecture
- Tokens never touch localStorage: Short-lived access JWTs reside solely in browser module memory, eliminating XSS token harvesting vectors.
- HttpOnly rotating refresh cookies with automatic family reuse revocation: If a compromised token is replayed, the entire session family is revoked immediately across the database.
- Atomic SQL brute-force login lockout: Single-query atomic increments track failed attempts without read-modify-write lost-update races.

3️⃣ Two-Phase Clinical Finalization & Zero-Dependency PDF Engine
- Vaccination administration follows a clinical two-phase workflow: healthcare providers draft lot and dose details with physical administration verification before finalization.
- Finalization commits in a single atomic database transaction coordinating clinical record creation, lot inventory deduction, appointment completion, and digital certificate generation.
- To produce tamper-evident certificates, we built a zero-dependency ISO 32000 / PDF 1.4 generation engine in pure Java — constructing raw PDF byte streams, font dictionaries, and xref tables directly without heavy third-party libraries (no PDFBox, no iText).
- Integrated with a privacy-preserving public verification endpoint that confirms authenticity without leaking citizen PII.

4️⃣ Pluggable AI Assistant with Safety Guardrails
- Built a multi-provider conversational assistant supporting OpenAI, Google Gemini, and local offline Ollama models.
- Enforced strict deterministic refusal policies (blocking medical triage and unauthorized mutations) paired with role-validated safe route recommendations.
- All domain tools are read-only and strictly scoped to the authenticated JWT principal ID.

5️⃣ Modern Frontend & AST Architecture Linting
- Built on React 19, Vite 8, and React Router 7 with modular feature slices.
- Implemented a custom AST ESLint architectural boundary rule (`eslint/featureBoundaries.js`) that enforces hexagonal boundaries at build time, preventing cross-feature internal coupling.

📈 The Engineering Scale:
- 1,043 Java files (700 production classes + 343 test suites)
- 4,200+ automated tests across the stack (3,009 Spring Boot/MySQL + 1,265+ React Vitest) with 0 failures on CI
- 59 relational database tables across 28 Flyway migrations

Check out the full open-source architecture, diagrams, and quickstart:
🔗 https://github.com/Sankalpa-KMCP/se2030-2026-y2-s1-mtr-01-vaccination-portal

#SoftwareEngineering #Java #SpringBoot #React #SystemDesign #Concurrency #WebDevelopment #SoftwareArchitecture #CleanCode #OpenSource
```

---

## 📝 Option B: Executive / Product-Focused Post

*Best suited for general technical audiences, product managers, and startup founders.*

```markdown
Proud to share the Web-Based Vaccination Portal (WBVP) — an enterprise platform designed to orchestrate national vaccination programs at scale.

Healthcare platforms demand high reliability, patient privacy, and seamless workflows for citizens and clinicians alike. Here is what we delivered:

🏥 Complete Clinical Lifecycle:
From dynamic pre-screening questionnaires and real-time center capacity booking to clinic check-in queues, inventory lot tracking, and verifiable digital immunization passes.

🔐 Privacy by Design:
Stateless authentication, memory-only session tokens, automatic brute-force lockout, and privacy-preserving public QR verification that validates vaccination certificates without exposing citizen PII.

⚡ Enterprise Scale:
- 1,000+ Java classes built on Spring Boot & Java 21
- React 19 frontend with real-time stepper journeys and clinical queues
- 4,200+ automated tests across JUnit 5, MySQL Testcontainers, and Vitest
- Pluggable AI Assistant supporting cloud and local offline LLMs

Explore the architecture and documentation on GitHub:
👉 https://github.com/Sankalpa-KMCP/se2030-2026-y2-s1-mtr-01-vaccination-portal

#HealthTech #DigitalHealth #Java #React #SoftwareEngineering #CloudComputing #ProductDesign
```

---

## 🎨 Recommended Visual Attachments for LinkedIn

For maximum engagement on LinkedIn, pair the post with 2 to 4 high-resolution visual assets from the repository:

1. **System Architecture Diagram** (Slide 1):
   - Export the Mermaid component flow diagram from `README.md` (Client UI $\rightarrow$ Stateless Security $\rightarrow$ Hexagonal Core $\rightarrow$ MySQL 8.4).
2. **Clinical Workflow Sequence** (Slide 2):
   - The Two-Phase Clinical Finalization diagram from `README.md` showcasing the atomic transaction boundary across draft, inventory, appointment, and PDF certificate issuance.
3. **Responsive UI Previews** (Slide 3 & 4):
   - Public Landing & Citizen Stepper: `frontend/public/images/citizen-vaccination-care.jpg` or `output/playwright/campaign-desktop.png`.
   - AI Assistant Panel & Responsive View: `output/playwright/assistant-premium-desktop.png`.
   - Brand Mark: `frontend/public/brand/vaccination-portal-logo.svg`.
