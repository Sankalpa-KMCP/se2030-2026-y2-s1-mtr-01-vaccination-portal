# Modular-monolith component view

Source: [current architecture](../../Web_Based_Vaccination_Portal_Recommended_Full_File_Tree.md). Provider-owned dependency ports separate modules; this diagram is not a deployment topology.

```mermaid
flowchart LR
  UI[React routes and feature UI] --> Client[apiClient and memory auth state]
  Client --> Auth[Shared Spring Security and auth sessions]
  Client --> Screening[MF01 Screening]
  Client --> Campaign[MF02 Campaign Center Session]
  Client --> Appointment[MF03 Appointment Waitlist Check-In]
  Client --> Inventory[MF04 Inventory Batch]
  Client --> Vaccination[MF05 Draft Record Certificate]
  Client --> Support[Profiles Analytics Assistant support]
  Appointment --> Eligibility[Screening provider eligibility API]
  Appointment --> Capacity[Campaign provider slot API]
  Campaign --> Impact[Appointment provider change-impact API]
  Vaccination --> Stock[Inventory provider batch API]
  Vaccination --> Completion[Appointment provider completion API]
  Auth --> DB[(MySQL)]
  Screening --> DB
  Campaign --> DB
  Appointment --> DB
  Inventory --> DB
  Vaccination --> DB
  Support --> DB
```

Module arrows represent provider APIs, not permission to import another module's repository. Assistant external model provider configuration is optional; no external host is asserted.
