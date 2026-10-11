# GitHub Repository Setup & Showcase Guide

This guide details the settings to configure on GitHub to make the **Web-Based Vaccination Portal (WBVP)** repository stand out to recruiters, engineering managers, and visitors on GitHub and LinkedIn.

---

## 🏷️ 1. Repository "About" Section Settings

On the main repository GitHub page, click the ⚙️ gear icon next to **"About"** in the top-right corner of the sidebar:

### Description
Copy and paste this high-impact, professional description:
```text
Enterprise healthcare orchestration platform engineered with hexagonal architecture, strict concurrency guarantees, zero-compromise session security, and verifiable digital health certification.
```

### Website URL
Set to your live deployed site, documentation link, or repository homepage:
```text
https://github.com/Sankalpa-KMCP/se2030-2026-y2-s1-mtr-01-vaccination-portal
```

### Topics (Tags)
Copy and paste each of these 16 curated topics into the **Topics** field to maximize search discoverability and portfolio tagging:

| Topic | Category |
| :--- | :--- |
| `java-21` | Core Stack |
| `spring-boot` | Backend Framework |
| `spring-security` | Security & Auth |
| `react-19` | Frontend Framework |
| `vite` | Frontend Tooling |
| `mysql-8` | Database Engine |
| `flyway-migration` | Schema Evolution |
| `testcontainers` | Integration Testing |
| `vitest` | Component Testing |
| `hexagonal-architecture` | System Architecture |
| `concurrency-control` | Concurrency & Locks |
| `zero-trust` | Application Security |
| `healthcare` | Domain Focus |
| `digital-certificates` | Clinical Verification |
| `ai-assistant` | AI Gateway |
| `rest-api` | API Design |

### Include in Home Page
Check all boxes:
- [x] **Releases**
- [x] **Packages**
- [x] **Environments**

---

## 🖼️ 2. Repository Social Preview Card (Open Graph Image)

When sharing your repository link on **LinkedIn**, **X (Twitter)**, or **Discord**, GitHub automatically attaches the repository's Social Preview banner.

### How to Apply:
1. Navigate to your repository on GitHub.
2. Click **Settings** (top navigation tab).
3. Under the **General** settings section, scroll down to **Social preview**.
4. Click **Edit** $\rightarrow$ **Upload an image...**
5. Select the high-resolution vector preview image generated in this repository:
   ```text
   .github/assets/social-preview.svg
   ```
6. Save changes.
7. *Verification*: Paste your GitHub repo link into the [LinkedIn Post Inspector](https://www.linkedin.com/post-inspector/) to preview the generated card.

---

## 🚀 3. GitHub Actions & CI Badge Status

Both CI workflows have been upgraded to official stable action versions (`@v4`):
- `.github/workflows/backend-ci.yml`: Uses `actions/checkout@v4` and `actions/setup-java@v4`.
- `.github/workflows/frontend-ci.yml`: Uses `actions/checkout@v4` and `actions/setup-node@v4`.

This ensures that whenever commits or pull requests are triggered, the **Actions** tab displays crisp green checkmarks (`Passing`) across all build and test verification stages.

---

## 📌 4. Key Takeaways for Recruiters & Visitors

When someone visits your repository, they will immediately see:
1. **Interactive Open Graph Card & Hero Badges**: Showing modern Java 21, Spring Boot, React 19, and 4,200+ passing tests.
2. **Visual Screenshot Gallery**: 4 high-resolution preview cards showing the real operational console, AI assistant, citizen care pathway, and 3D companion mascots.
3. **Deep Architecture Specifications**: Complete Mermaid sequence and component flowcharts detailing hexagonal ports and concurrency locks.
4. **Frictionless Quickstart**: Zero-config `docker compose up -d` with pre-seeded demo accounts for instant evaluation.
