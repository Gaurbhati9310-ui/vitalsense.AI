# VitalSense — Tech Stack Document

**Platform:** Native Android, built in Android Studio
**Document status:** Draft v2.0 — SIH26-26133 Target Architecture

---

## 1. Primary Objective
This document defines the technical architecture for the VitalSense platform, aligning it with the SIH26-26133 problem statement. The architecture must support offline-first rural healthcare, AI-assisted clinical intake, deterministic triage, OCR processing, smart referrals, closed-loop follow-up, and community health intelligence. The stack is designed to be highly practical for an SIH prototype while remaining extensible for production deployment.

---

## 2. Audit of the Current Stack

| Technology | Action | Technical Reason |
|---|---|---|
| **Kotlin** | **KEEP** | Standard for modern Android; excellent coroutine support for asynchronous tasks. |
| **Jetpack Compose** | **KEEP** | Fast UI iteration, critical for building complex, localized, icon-heavy dashboards. |
| **MVVM + Repository** | **KEEP** | Clean separation of concerns, highly testable, standard Android architecture. |
| **Hilt (DI)** | **KEEP** | Simplifies dependency injection across multiple feature modules. |
| **Room Database** | **KEEP** | Essential for robust offline-first functionality and local caching. |
| **WorkManager** | **KEEP** | Optimal for background sync, exponential backoff, and outbox queuing. |
| **Firebase (Auth, Firestore, Storage, Functions)** | **KEEP** | Rapid prototyping, built-in offline persistence, and serverless compute for AI processing. |
| **Google Maps** | **MODIFY** | Keep, but integrate with a Smart Facility Finder backend instead of just "nearest locations". |
| **ML Kit OCR** | **KEEP** | On-device text recognition works perfectly offline; will augment with LLM structuring. |
| **Android SMS** | **KEEP** | Crucial cellular fallback for SOS and urgent triage when data connectivity fails. |

---

## 3. Target Architecture & High-Level Workflow

The architecture directly supports the following workflow:
`Patient/ASHA → Consent → Voice/Touch Clinical Intake → Structured Clinical History → Deterministic Triage Engine → (Routine/High Risk) → Smart Referral / Facility Finder → Doctor Treatment → Follow-up → Longitudinal Record → Privacy-preserving aggregation → Community Intelligence / Admin`

---

## 4. Architecture Style

We adopt a **Clean-ish modular MVVM architecture** utilizing the **Repository Pattern**. 
- **Local Database (Room)** is the Single Source of Truth (SSOT).
- The **UI (Compose)** observes StateFlows exposed by the **ViewModel**.
- The **ViewModel** interacts exclusively with the **Repository**.
- The **Repository** orchestrates data between **Room (Local)**, the **WorkManager Outbox**, and **Firebase (Remote)**.
- **Microservices are avoided.** Instead, **Firebase Cloud Functions** handle server-side AI processing and data aggregation securely.

---

## 5. Android Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Declarative, faster, easier localization)
- **Navigation:** Jetpack Compose Navigation
- **State Management:** StateFlow / SharedFlow (Coroutines)
- **Dependency Injection:** Dagger Hilt
- **Asynchronous Operations:** Kotlin Coroutines
- **Networking/Serialization:** Retrofit & Kotlinx Serialization (for any REST mock APIs/ABDM sandboxes)
- **Local Persistence:** Room (SQLite abstraction)
- **Background Work:** WorkManager
- **Image Handling:** Coil (lightweight, Compose-native)
- **Permissions:** Accompanist Permissions (or standard Compose ActivityResult API)
- **Testing:** JUnit4, MockK, Espresso

---

## 6. Offline-First Architecture

VitalSense is useless in rural areas if it breaks offline.
- **Local Source of Truth:** Read operations always query Room.
- **Outbox Pattern:** Offline writes (e.g., Intake, Referral) are stored in Room in a `SyncQueue` table:
  ```kotlin
  data class OutboxItem(
      val id: String,
      val entityType: String,
      val operation: OperationType (CREATE/UPDATE/DELETE),
      val payload: String, // JSON payload
      val createdAt: Long,
      val status: SyncStatus (PENDING/FAILED)
  )
  ```
- **Sync (WorkManager):** Monitors `ConnectivityManager`. Applies exponential backoff on failure. Enforces idempotency via unique IDs.
- **Conflict Strategy:** DO NOT use last-write-wins for clinical data. For conditions, allergies, or triage severity, conflicts trigger an "In-Review" state, requiring the Doctor to manually accept the merged timeline.

---

## 7. Clinical Intake Engine

A unified state machine for **Voice** and **Touch**.
- **Voice Response** and **Touch Response** map to the same structured intent schema.
- **Flow:** `Input (Speech/Tap) → Intent Extraction → State Machine Update → Next Question Generation`.
- Conversation state is saved in Room after every turn to support **resume-after-interruption**.

---

## 8. Voice Technology

- **Offline/Base STT:** Android native `SpeechRecognizer` (supports Hindi/English on-device).
- **Online/Enhanced (Backend):** Cloud LLM parses the STT string to extract medical entities.
- **Fallback:** If voice fails, UI degrades gracefully to Touch mode (large icons). Offline voice must rely exclusively on on-device STT capabilities.

---

## 9. AI Architecture

**AI ASSISTS, IT DOES NOT DIAGNOSE.**
- **Provider:** Google Gemini API (via Firebase Functions).
- **Security:** API Keys live in Firebase Secret Manager, NEVER in the Android app.
- **Flow:** `Raw Input → Firebase Function → LLM Prompt (Strict JSON Schema) → Structured JSON Output → App UI`.
- **Validation:** JSON schema validation in the Function. Hallucinations mitigated by enforcing human review on the final Physician Summary.

---

## 10. Triage Engine

**Safety-critical. Must run deterministically.**
- **Logic:** `Structured Clinical History → Normalized Symptoms → Deterministic Rule Engine → GREEN / YELLOW / RED`.
- **Execution:** Runs **on-device** so offline intakes are still flagged immediately.
- AI is only used to extract the symptom string (e.g., "I have chest pain" -> `chest_pain`). The rule `if (symptoms.contains("chest_pain")) -> RED` is hardcoded.

---

## 11. SOCRATES Engine

Reusable data structure applied to the Chief Complaint:
```kotlin
data class SocratesModel(
    val site: String?, val onset: String?, val character: String?, 
    val radiation: String?, val associations: List<String>?, val timing: String?, 
    val exacerbatingRelieving: String?, val severity: Int?
)
```
The state machine queries these sequentially if missing.

---

## 12. OCR Architecture

- **Pipeline:** `Camera Intent → Image Compression (Coil/Canvas) → Google ML Kit Text Recognition (On-Device/Offline) → Raw Text`.
- **Structuring (Online):** Raw text sent to Firebase Function → LLM extracts `Medicines, Labs, Diagnoses`.
- **Validation:** User MUST review and edit the extracted structured record before it saves.

---

## 13. Medical Document Model

- **Storage:** Local cache in app-private directory; Remote in Firebase Storage.
- **Metadata Entity:** `id, patientId, documentUrl, uploadDate, documentType, rawOcrText, extractedEntitiesJson, verifiedBy`.

---

## 14. Smart Referral Engine

Manages continuity of care when a patient must be moved.
- **State Machine:** `CREATED → SENT → ACCEPTED → IN_TRANSIT → CONSULTED → COMPLETED`.
- **Model:** `referralId, patientId, sourceAshaId, destinationFacilityId, urgency, reason, status, timestamp`.
- **Escalation:** If status remains `SENT` for > 2 hours on a RED referral, triggers FCM alert to regional Admin.

---

## 15. Facility Resource Engine

- **Model:** `Facility (id, name, location, type)` + `FacilityResource (facilityId, doctorsAvailable, hasEcg, pharmacyStockStatus)`.
- **Recommendation Logic:** Ranks facilities based on `Distance + Required Services + Load`.
- **Prototype Note:** Uses a static mocked JSON dataset for the hackathon, representing future real-time state health APIs.

---

## 16. Maps & Location

- **Provider:** Google Maps SDK.
- **Offline Fallback:** If internet is unavailable, map tile fails gracefully, but a cached list of "Last known nearby facilities" is displayed in a RecyclerView.

---

## 17. Follow-up Engine

- **Model:** `FollowUpTask (id, patientId, doctorId, dueDate, status [PENDING/COMPLETED/MISSED])`.
- **Workflow:** Doctor creates task → System schedules local Android Alarm / FCM → ASHA sees task on dashboard → Completes Follow-up Assessment.

---

## 18. Longitudinal Health Record

A unified timeline event model that merges diverse data sources:
- `TimelineEvent (id, patientId, date, eventType [ENCOUNTER/LAB/PRESCRIPTION], payloadJson, sourceReference)`.
- Sorted descending. Cached in Room for offline doctor/ASHA review.

---

## 19. Community Health Intelligence

Evolves the basic heatmap into an early-warning system.
- **Aggregation:** Firebase Cloud Functions trigger on new `Encounter` writes. They increment counters in an `AggregatedTrends` collection (grouped by village, symptom, week).
- **Privacy:** Admin dashboard reads *only* `AggregatedTrends`, never raw patient encounters.

---

## 20. Firebase Architecture

- **Authentication:** Phone/Email + Custom Claims (`role: admin|asha|doctor|patient`).
- **Firestore:** Operational data (Patients, Encounters, Referrals).
- **Storage:** Medical document images.
- **Functions:** STT/LLM wrappers, OCR structuring, Trend aggregation.
- **FCM:** Push notifications for referrals/appointments.

---

## 21. Database Architecture (Domain Entities)

- **CORE:** `User`, `Patient`, `Encounter`, `ClinicalHistory` (Intake), `MedicalDocument` (OCR), `Referral`, `FollowUpTask`.
- **OPTIONAL/MOCK:** `Facility`, `MedicineStock`.
- Ensure Room DAOs and Firestore document structures mirror each other where practical.

---

## 22. FHIR / Interoperability & ABHA Strategy

VitalSense will be **FHIR-ready / ABDM Prototype**, not production-certified.
- **ABHA:** Mock "Login with ABHA" UI screen that generates a dummy token.
- **FHIR:** Internal Room/Firestore models are standard JSON. An `EncounterExportMapper` will exist to translate an Encounter into a mock FHIR R4 `Bundle` string to demonstrate interoperability capability to judges.

---

## 23. Security Architecture

- **Auth:** Firebase Auth.
- **Authorization:** Firestore Security Rules ensure `request.auth.token.role == 'doctor'` or `patientId in asha.caseload`.
- **API Security:** LLM API keys remain in Firebase Functions backend.
- **Audit:** Production requires SQLCipher and access logs; Prototype relies on Firestore's native security boundaries.

---

## 24. Role-Based Architecture

Single Android app, Single Codebase.
- `MainActivity` checks Firebase Custom Claims on login.
- Routes to specific Jetpack Navigation Graph (`AshaNavGraph`, `DoctorNavGraph`, etc.).
- Shared components (e.g., TimelineViewer, Chat) are reused across graphs.

---

## 25. Notifications & SOS

- **FCM:** Primary transport for online notifications (e.g., "New Referral").
- **SMS Fallback:** If `Triage == RED` or SOS button pressed, and network is unavailable, Android `SmsManager` fires a localized SMS to the designated emergency contact / PHC.

---

## 26. Localization

- **UI:** Android `strings.xml`.
- **Language Switcher:** In-app toggle overriding system locale.
- **Voice:** Android native `TextToSpeech` (TTS) and `SpeechRecognizer` configured to the selected locale (e.g., `hi-IN`).

---

## 27. API Contracts (Firebase Functions)

- `initiateIntake(patientId) -> { stateId, greetingText }`
- `processIntakeTurn(stateId, transcript) -> { isComplete, nextQuestion, structuredJson }`
- `structureOcrText(rawText, docType) -> { extractedEntitiesJson }`
- `exportToFhir(encounterId) -> { fhirR4JsonString }`

---

## 28. Feature-Based Package Structure

```
com.vitalsense.app
 ├── core/
 │   ├── data/ (Room, Firestore, Mappers)
 │   ├── di/ (Hilt Modules)
 │   ├── network/ (Firebase Functions wrappers)
 │   ├── sync/ (WorkManager Outbox)
 │   └── ui/ (Compose Theme, Common Components)
 ├── feature/
 │   ├── auth/ (Login, ABHA Mock)
 │   ├── asha/ (Dashboard, Caseload, Follow-ups)
 │   ├── doctor/ (Dashboard, Summary View, Prescribe)
 │   ├── intake/ (Voice/Touch MediKiosk Engine)
 │   ├── triage/ (Deterministic Rules Engine)
 │   ├── ocr/ (ML Kit + Verification UI)
 │   ├── referral/ (Facility Finder, Status)
 │   ├── timeline/ (Longitudinal Record UI)
 │   └── admin/ (Community Heatmap, Broadcasts)
```

---

## 29. Testing Stack

- **Unit:** `TriageEngine` rules, `SocratesMapper` logic, Referral state transitions (JUnit4).
- **UI:** Intake Flow, Doctor Dashboard (Compose Testing / Espresso).
- **Integration:** Offline Outbox Queuing -> Reconnect -> Sync success.

---

## 30. Observability & Error Handling

- **Crashlytics:** Standard crash reporting.
- **AI Fallback:** If `processIntakeTurn` times out (>8s) or throws, UI immediately drops to the Offline Touch-based decision tree. App must NEVER freeze waiting for AI.

---

## 31. Performance Targets

- **Low-End Devices:** Limit Coil image caching memory. Limit Compose recompositions.
- **OCR:** Compress images before passing to ML Kit to prevent Out-Of-Memory (OOM) exceptions.
- **Sync:** Batch Firestore writes in WorkManager to save rural battery life.

---

## 32. Prototype vs Production

| Component | SIH Prototype | Future Production |
|---|---|---|
| **AI LLM** | Cloud Gemini via Firebase Functions | Fine-tuned, self-hosted medical LLM |
| **STT** | Android native SpeechRecognizer | Integrated Bhashini APIs |
| **ABDM** | Mock OAuth UI + FHIR JSON string generation | NHA Sandbox certified gateway integration |
| **Facility Data** | Static JSON file inside app | Real-time state health APIs |
| **SMS** | Android `SmsManager` Intent | Twilio/Telecom API Gateway |

---

## 33. Technology Decision Matrix

| Requirement | Technology | Why | Decision |
|---|---|---|---|
| Offline DB | Room | Native SQLite, reliable | SELECTED |
| Backend | Firebase | Fastest for hackathon, handles auth + offline | SELECTED |
| AI Pipeline | Cloud Functions + Gemini | Hides API keys, prevents heavy client loads | SELECTED |
| OCR Text | Google ML Kit | Free, runs completely offline on-device | SELECTED |
| Maps | Google Maps SDK | Best Android integration, handles facility pins well | SELECTED |

---

## 34. Team Development Architecture (Logical Ownership)

To prevent merge conflicts and parallelize work for the SIH team:
- **Member 1 (Intake):** `feature/intake`, `feature/triage`, Speech/Touch UI.
- **Member 2 (Doctor & Timeline):** `feature/doctor`, `feature/timeline`, Physician Summary UI.
- **Member 3 (Documents):** `feature/ocr`, ML Kit integration, Camera Intents.
- **Member 4 (Operations):** `feature/referral`, `feature/asha`, Follow-ups, Maps.
- **Member 5 (Backend/Admin):** Firebase Functions, Heatmap, `core/sync`, FHIR mappers.

---

## 35. Git / Branching Strategy

- **`main`**: Protected, always demo-ready.
- **`develop`**: Integration branch.
- **Feature Branches**: e.g., `feature/asha-intake`, `feature/ocr-mlkit`.
- **Rule:** Shared data models (e.g., `Encounter` schema) MUST be agreed upon and merged into `develop` first before parallel feature work begins.

---

## 36. Final Architecture Diagram

```text
                VITALSENSE ANDROID APP
                         |
       +-----------------+-----------------+
       |                 |                 |
    Patient            ASHA             Doctor
       |                 |                 |
       +-----------------+-----------------+
                         |
                 Clinical Intake
                  /            \
           Voice(STT)         Touch
                  \            /
               Structured JSON Data
                         |
           Deterministic Triage Engine
                   /         \
              Routine       High Risk
                |               |
         Doctor Review       Referral
                                |
                         Facility Finder
                                |
                             Doctor
                                |
                        Treatment / Rx
                                |
                           Follow-up
                                |
                         Health Timeline
                                |
                    Community Aggregation
                                |
                         Admin Dashboard

                    BACKEND / CLOUD
                         |
       +-----------------+-----------------+
       |                 |                 |
    Firebase      Cloud Functions(LLM)  Storage
```

---

## 37. Final Technical Risks & Mitigations

1. **AI Latency/Timeout:** *Mitigation:* Implement strict 8-second timeouts; fallback to hardcoded touch trees.
2. **Speech Recognition Failure (Noisy OPDs):** *Mitigation:* The UI must always display touchable fallback options.
3. **OCR OOM Errors:** *Mitigation:* Downscale bitmap images before passing them to the ML Kit analyzer.
4. **Sync Conflicts:** *Mitigation:* Triage and condition edits use manual review rather than last-write-wins to ensure clinical safety.
