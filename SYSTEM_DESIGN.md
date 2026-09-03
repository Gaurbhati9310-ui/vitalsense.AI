# VitalSense — System Design Document

**Document status:** Draft v2.0 — SIH26-26133 Target Architecture

---

## 1. System Goals

The new VitalSense system design explicitly supports:
1. Rural/underserved healthcare access
2. ASHA-assisted healthcare
3. Offline-first operation
4. Multilingual interaction
5. Voice + touch clinical intake
6. Structured clinical history
7. Deterministic triage
8. Medical document OCR
9. Smart referral
10. Facility/resource discovery
11. Doctor clinical workflow
12. Follow-up and continuity of care
13. Longitudinal patient record
14. Community health analytics
15. Public-health early warning
16. Secure role-based access
17. Privacy-preserving data aggregation
18. Future interoperability/FHIR readiness

---

## 2. High-Level System Architecture

```text
                    VITALSENSE
                        |
        +---------------+---------------+
        |               |               |
     PATIENT           ASHA           DOCTOR
        |               |               |
        +---------------+---------------+
                        |
                Clinical Intake
                   /        \
                Voice       Touch
                   \        /
                Structured Data
                        |
                 Triage Engine
                  /          \
             Routine        High Risk
                |              |
             Doctor        Smart Referral
                               |
                       Facility Finder
                               |
                            Doctor
                               |
                          Treatment
                               |
                          Follow-up
                               |
                     Health Timeline
                               |
                 Privacy-preserving
                     Aggregation
                               |
                  Community Intelligence
                               |
                          ADMIN
```

---

## 3. Architectural Layers

- **Presentation Layer (Jetpack Compose):** Renders UI state, captures user intents. Contains strictly no business logic.
- **Domain Layer (ViewModels / Use Cases):** Contains state machines (e.g., Intake Engine, Triage Rules) and orchestrates data from repositories.
- **Repository Layer:** Single Source of Truth coordinator. Decides whether to fetch from Local DB or Remote API.
- **Local Data Layer (Room DB):** Stores cached models. Essential for offline-first reads.
- **Sync Layer (WorkManager Outbox):** Queues offline mutations and handles reliable background synchronization with exponential backoff.
- **Remote Data Layer (Firebase):** Handles Auth, Firestore sync, and Storage.
- **AI / External Services:** Firebase Cloud Functions securely wrap LLM APIs (Gemini) and execute heavy data aggregations.

---

## 4. Android Architecture

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Navigation:** Jetpack Navigation Compose
- **Pattern:** MVVM (Model-View-ViewModel) with the Repository pattern.
- **Dependency Injection:** Hilt
- **Asynchrony:** Coroutines / Flow
- **Persistence:** Room (Local), WorkManager (Background)

**Flow:** `Compose UI → Intent → ViewModel → Repository → Room DB Flow → UI updates instantly`. The Repository concurrently triggers `WorkManager` for network sync.

---

## 5. Feature-Based Module Architecture

```text
core/
 ├── data/ (Repositories, Models)
 ├── database/ (Room DB, DAOs)
 ├── network/ (Firebase API wrappers)
 ├── sync/ (WorkManager Outbox)
 ├── ui/ (Theme, shared Composables)
feature/
 ├── auth/ (Login, RBAC)
 ├── asha/ (Dashboard, Patient List)
 ├── doctor/ (Dashboard, Rx)
 ├── clinicalintake/ (Voice/Touch State Machine)
 ├── triage/ (Deterministic Rules)
 ├── ocr/ (ML Kit Scanner, Verification)
 ├── referral/ (Facility Matcher, Status)
 ├── followup/ (Task Tracker, Re-triage)
 ├── timeline/ (Longitudinal Record Viewer)
 ├── community/ (Heatmap, Aggregation UI)
 ├── admin/ (Broadcasts, Approvals)
 ├── sos/ (Emergency Trigger)
```

---

## 6. Role & Authorization Architecture

- **Roles:** PATIENT, ASHA, DOCTOR, ADMIN.
- **Auth Flow:** `Firebase Auth Login → Fetch Custom Claims → Navigate to Role Graph`.
- **Authorization:** Handled via Firestore Security Rules and local DB filters. A Patient cannot see another Patient. An ASHA can only see Patients in her caseload or where she is explicitly registered as a Proxy.

---

## 7. Consent & ASHA Proxy Access

**Flow:** `Patient → Audio-Guided Consent → ASHA Proxy Authorization → Limited Patient Access → Clinical Workflow`.
- ASHA explicitly asks for consent (OTP/Mock).
- The Patient record in DB updates: `authorizedHelpers.add(ashaId)`.
- Access allows the ASHA to act as a human-kiosk to perform intakes and OCR uploads.

---

## 8. Offline-First Architecture

**Architecture:**
```text
                  OFFLINE
                     |
              Local Room DB
                     |
              Outbox Queue
                     |
              WorkManager
                     |
               Connectivity
                     |
                  Sync API
                     |
                 Firebase
```
- **Reads:** Always from Room.
- **Writes:** Saved to Room + inserted into `OutboxItem` table.
- **Sync:** WorkManager observes Network availability. On reconnect, processes `OutboxItem` queue sequentially.
- **Conflict Resolution:** For clinical fields (e.g., Triage, Allergies), last-write-wins is disabled. Conflicting server vs. local states result in an "In-Review" flag requiring Doctor validation.

---

## 9. Voice + Touch Architecture (Clinical Intake)

A unified state machine guarantees data consistency regardless of input method.
- **Voice:** `Speech → STT → Intent Extraction (AI) → Structured Answer`.
- **Touch:** `UI Selection → Structured Answer`.
- **State Machine:** Both update the exact same `IntakeState` flow. 
- **Language/Fallback:** If the patient stops talking or STT fails, the UI instantly displays the Touch fallback buttons for the current question context.

---

## 10. Adaptive Clinical Question Engine & SOCRATES

- **Engine:** Evaluates current `IntakeState`. If `chiefComplaint` is known, it asks SOCRATES questions sequentially.
- **SOCRATES:** Site, Onset, Character, Radiation, Associations, Timing, Exacerbating/relieving factors, Severity.
- **AI Assistance:** AI helps parse conversational answers into the structured SOCRATES JSON fields, but deterministic logic decides which question to ask next.

---

## 11. AI Architecture & Fallbacks

- **Secure Path:** `Android → Firebase Function → LLM (Gemini) → Validated JSON → Android`. NO API keys in the app.
- **Role:** AI assists with language parsing, summarization, and OCR entity extraction. It does NOT triage.
- **Failure Fallback:**
  - *AI Unavailable:* Switches to rigid touch-tree workflow.
  - *Voice Unavailable:* Switches to Touch.
  - *OCR Unavailable:* Switches to Manual Entry.

---

## 12. Triage Engine (Deterministic)

- **Flow:** `Clinical Input → Normalization → Deterministic Rules → GREEN / YELLOW / RED`.
- **Execution:** Runs fully on-device (offline).
- **Rules:** e.g., `If symptoms.containsAny(["chest_pain", "unconscious"]) -> RED`.

---

## 13. Medical OCR Pipeline

- **Flow:** `Camera → On-device ML Kit OCR → Raw Text → (Online Only) Firebase Function LLM Extraction → Structured Data → Human Verification UI → DB`.
- **Safety:** OCR output is ALWAYS editable by the user before saving.

---

## 14. Smart Referral & Facility Resource Engine

- **Trigger:** Triage = RED or Doctor decision.
- **Facility Model:** Includes location, services, doctors, medicine stock (Prototype Data).
- **Matching:** Ranks facilities by `Distance + Required Services`.
- **Referral State Machine:** `CREATED → SENT → ACCEPTED → IN_TRANSIT → CONSULTED → COMPLETED`.

---

## 15. Doctor Workflow & Clinical Brief

- **Queue:** Sorted by Triage Severity (RED first).
- **Clinical Brief:** AI synthesizes the SOCRATES intake, past OCRs, and triage level into a clean medical summary. 
- **Action:** Doctor reviews the summary, verifies the AI tag, issues a prescription, or creates a referral/follow-up.

---

## 16. Follow-Up & Longitudinal Record

- **Follow-up:** `Doctor action → Schedule Task → ASHA Dashboard → Patient Assessment → Re-triage`.
- **Longitudinal Record:** Merges `Encounter`, `Lab`, `Prescription`, and `FollowUp` into a unified `TimelineEvent` flow, cached locally for offline review.

---

## 17. Community Health Intelligence & Early Warning

- **Privacy:** `Patient Encounters → Firebase Function Privacy Filter → Aggregation (Village level)`.
- **Early Warning Engine (Prototype):** Checks `recent_cases (last 7 days) > baseline (previous 30 days) * 1.5`. If true, alerts Admin.
- **Admin View:** Displays trend graphs and heatmaps without exposing PHI.

---

## 18. Data Storage & Firebase Architecture

- **Room:** Active patient data, drafts, cached timelines, Outbox queue.
- **Firebase Auth:** JWT, RBAC Custom claims.
- **Firestore:** Sync state, synced encounters, facility data, referral states, community aggregates.
- **Storage:** Encrypted medical document images.
- **Functions:** Secure LLM execution, trend aggregation.

---

## 19. Security, Consent & Interoperability

- **Security:** Firebase Security Rules, JWT verification, explicit Patient Consent timestamps in DB.
- **FHIR Prototype:** `Encounter` objects have an `exportToFhirR4()` mapper to output JSON string representations. We do NOT claim real ABDM/NHA integration.

---

## 20. Notification & SOS Architecture

- **FCM:** Push notifications for Appointments, Referrals, ASHA Tasks.
- **SOS:** Triggered manually or by a RED Triage result offline. Android `SmsManager` sends an emergency SMS to the PHC/ASHA if the internet is down.

---

## 21. API / Service Boundaries (Logical)

- `AuthService`: Role resolution.
- `ClinicalIntakeService`: Voice/Touch state machine.
- `TriageService`: Deterministic local rules.
- `OcrService`: ML Kit wrap + LLM verification.
- `ReferralService`: State transitions.
- `FacilityService`: Geospatial/mock data matching.
- `AnalyticsService`: Cloud-side aggregations.

---

## 22. Conceptual Database ER

```text
User 1:1 Patient
User 1:1 ASHA
User 1:1 Doctor

Patient 1:N Encounter
Patient 1:N MedicalDocument
Patient 1:N Referral
Patient 1:N FollowUpTask

Encounter 1:1 ClinicalHistory (SOCRATES)
Encounter 1:1 TriageAssessment
Encounter 1:1 ClinicalSummary (AI Generated)
Encounter 1:1 Prescription

Facility 1:N Doctor
Facility 1:1 MedicineStock
```

---

## 23. Key Sequence Diagrams

### 23.1 Clinical Intake Sequence
```text
ASHA -> App: Start Intake
App -> ASHA: Mic / Touch UI
ASHA -> App: Speaks symptoms
App -> STT: Convert to Text
App -> Cloud Function: Extract Intent
Cloud Function -> App: JSON (Symptom=Fever)
App -> StateMachine: Update State
StateMachine -> App: Next Question (SOCRATES: Duration?)
App -> ASHA: TTS reads next question
```

### 23.2 Offline Sync Sequence
```text
User -> App: Save Encounter
App -> Room: Insert Encounter
App -> Room: Insert OutboxItem
WorkManager -> Network: Check Connectivity
Network -> WorkManager: Online
WorkManager -> Room: Read OutboxItem
WorkManager -> Firestore: Write Encounter
Firestore -> WorkManager: Success
WorkManager -> Room: Delete OutboxItem
```

---

## 24. Observability & Performance

- **Observability:** Firebase Crashlytics. Log sync failures, OCR confidence levels, and AI timeouts. Do not log PHI.
- **Performance:** Room pagination for long timelines. Coil image compression for OCR. WorkManager batching for battery savings.

---

## 25. Team Development Architecture

- **Member A (Intake):** ASHA flows, Clinical Intake State Machine, Triage Rules.
- **Member B (Review):** Doctor Dashboard, Clinical Brief, Longitudinal Timeline.
- **Member C (Data):** Patient UI, ML Kit OCR, Health Card.
- **Member D (Ops):** Referral Engine, Facility Matching, Follow-up Tasks.
- **Member E (Admin):** Firebase Functions, Heatmap, Community Aggregation.
- **Integration Lead:** Room DAOs, WorkManager Sync, Auth, NavGraphs.

**Git Rules:** `feature/*` branches -> PR -> `develop` -> `main`.

---

## 26. System Design vs Actual Implementation Status

| Component | Status |
|---|---|
| ASHA Proxy Architecture | **IMPLEMENTED** |
| Room + Basic Offline UI | **IMPLEMENTED** (Basic) |
| Disease Heatmap | **IMPLEMENTED** (Basic) |
| Firebase Auth / RBAC | **IMPLEMENTED** |
| Voice + Touch AI Intake | **PLANNED** |
| Deterministic Triage | **PLANNED** |
| ML Kit OCR | **MOCK/PROTOTYPE** |
| Smart Referral & Facilities | **PLANNED** (Data will be mocked) |
| Follow-up Tasks | **PLANNED** |
| FHIR Export String | **PLANNED** |

---

## 27. Prototype vs Production

| Component | SIH Prototype | Production |
|---|---|---|
| AI Processing | Cloud Functions + Gemini | Self-hosted secure Medical LLM |
| Database | Firebase Firestore | Dedicated Postgres/NoSQL HIPAA compliant cluster |
| Facilities | Mocked JSON data | Integrated State Health APIs |
| Interoperability | FHIR JSON String generation | NHA ABDM Sandbox Certification |

---

## 28. Architectural Decision Record (ADR)

- **ADR-001:** Native Android chosen for superior offline/Room control and ML Kit capabilities.
- **ADR-002:** Room + WorkManager chosen as SSOT over pure Firestore cache to enforce clinical conflict resolution rules.
- **ADR-003:** Triage MUST be deterministic (Local rules). AI is strictly barred from autonomous triage.
- **ADR-004:** AI API keys remain in Firebase Functions to prevent APK reverse-engineering.
- **ADR-005:** Community analytics rely solely on server-side aggregated counters to preserve privacy.

---

## 29. Technical Risks

| Risk | Impact | Mitigation |
|---|---|---|
| **AI Timeout/Latency** | High | UI strictly enforces an 8s timeout, instantly degrading to offline Touch-tree. |
| **OCR Hallucination** | High | Human verification UI is mandatory before save. |
| **Speech Recog Errors** | Medium | Touch fallback always available on-screen during voice mode. |
| **Sync Conflicts** | High | Triage and Diagnosis fields reject last-write-wins; flagged for Doctor review. |
| **Low-End Devices** | Medium | Heavy ML processing (except OCR) is offloaded to Cloud Functions. Image compression before OCR. |
