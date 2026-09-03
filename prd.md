# VitalSense — Product Requirements Document (PRD)

**Platform:** Android (built with Android Studio)  
**Document status:** Draft v2.0 — SIH26-26133 Alignment  
**Audience:** Team engineers, designers, and stakeholders  

---

## 1. Product Positioning

VitalSense is an AI-assisted, offline-first rural and community healthcare platform. It is designed to help ASHA workers and patients capture structured clinical information, digitize previous medical records, identify urgency/red flags, connect patients to appropriate healthcare facilities and doctors, and maintain continuity of care through referral and follow-up workflows.

**The platform connects:**
`Patient → ASHA Worker → Clinical Intake → Triage → Referral / Facility → Doctor → Treatment → Follow-up → Longitudinal Health Record → Community Health Intelligence → Admin`

*Note: VitalSense is NOT an autonomous AI doctor, a generic telemedicine app, or a simple OCR application. AI acts exclusively as an assistant to human clinicians and ASHA workers.*

---

## 2. Why VitalSense is Different

VitalSense differentiates itself from generic SIH MediKiosk concepts by leveraging the existing rural healthcare ecosystem:
- **ASHA as Human-Assisted Digital Health Facilitator:** Instead of expecting low-literacy rural patients to navigate a tablet kiosk alone, ASHA workers use VitalSense on their behalf.
- **Offline-First Rural Operation:** Functions without internet, queuing data for background sync.
- **AI Clinical Intake:** Voice and touch-based structured history gathering prior to consultation.
- **Medical Record Digitization:** On-device OCR to extract data from crumpled, physical documents.
- **Community Health Intelligence:** Aggregates individual intake data into early-warning heatmaps for administrators, proving macro public health value alongside micro clinical value.
- **Closed-Loop Follow-up & Smart Referral:** Tracks a patient from initial rural intake to the PHC/hospital, through treatment, and back to the village for follow-up.

---

## 3. Core Workflow

The primary healthcare workflow follows this path:
```text
Patient / ASHA
        ↓
Consent (Audio-guided)
        ↓
AI Clinical Intake
        ↓
Voice + Touch Interaction
        ↓
Structured Medical History (SOCRATES)
        ↓
Red-Flag / Triage Evaluation
        ↓
┌───────────────┐
│               │
Routine       High Risk
│               │
↓               ↓
Doctor /      Smart Referral
Appointment        ↓
              Smart Facility Finder
                   ↓
                 Doctor
                   ↓
               Treatment
                   ↓
              Follow-up Task (ASHA)
                   ↓
          Longitudinal Health Record
```

---

## 4. Key Functional Requirements

### 4.1 AI Clinical Intake Engine
- **Voice Mode:** Patient speaks naturally in regional language (e.g., "Mujhe teen din se bukhar hai"). System converts this to structured JSON (Symptom: Fever, Duration: 3 days).
- **Touch Mode:** For low literacy, noisy environments, offline states, or privacy. Uses large icons and simple choices.
- **Hybrid Mode:** Users can seamlessly switch between voice and touch without losing conversation state.

### 4.2 Adaptive Questioning & SOCRATES
The AI engine uses controlled clinical question structures, assisted by AI, to identify: Chief complaint, onset, duration, severity, associated symptoms, medications, and allergies.
For pain/primary symptoms, it enforces the **SOCRATES** structure:
- **S**ite, **O**nset, **C**haracter, **R**adiation, **A**ssociations, **T**iming, **E**xacerbating/relieving factors, **S**everity.

### 4.3 Triage & Red-Flag System
- **Input:** Structured clinical data from the intake.
- **Processing:** Deterministic red-flag rules (e.g., "Chest pain" + "Breathing difficulty" = RED) paired with AI assistance.
- **Output:** Categorizes urgency into GREEN, YELLOW, or RED.
- **Action:** Warns the user (e.g., "Red-flag symptoms detected. Urgent medical evaluation recommended") and escalates to the nearest facility/doctor.

### 4.4 OCR & Medical Document Digitization
- **Pipeline:** Camera Upload → On-device Preprocessing & OCR → AI Entity Extraction → Human Review/Edit → Save.
- **Extraction Targets:** Date, medicine name, dosage, frequency, diagnoses, lab tests, results, reference ranges, and abnormal flags.
- **Rule:** OCR output is *always* editable and requires human verification.

### 4.5 Smart Referral & Tracking System
Tracks referrals across states: `CREATED → SENT → ACCEPTED → IN_TRANSIT → CONSULTED → COMPLETED`.
- Captures referral reason, urgency, originating ASHA, and destination facility.
- Supports escalation for high-risk, missed, or expired referrals.

### 4.6 Smart Facility Finder
Upgrades the standard map to an **Appropriate Facility Recommendation** engine.
- Evaluates: Distance, facility type, doctor availability, medicine stock, diagnostic capability, and emergency capacity.
- *Prototype Note:* Data will be mocked for the SIH demo to simulate real-time API integrations.

### 4.7 Closed-Loop Follow-up
- Workflow: Doctor sets follow-up date → App issues Patient reminder & ASHA task → ASHA performs follow-up assessment (Improved/Same/Worse) → Re-triage if necessary.
- Tracks high-risk patients and overdue follow-ups.

### 4.8 Longitudinal Medical Record
Creates a chronological patient timeline merging past and present data:
- E.g., `2021 Lab Report` (OCR) → `2024 Prescription` (OCR) → `2026 Current AI Intake`.
- Provides doctors with a rapid, structured history overview.

### 4.9 Community Health Early Warning (Admin Heatmap)
- Anonymizes and aggregates patient-level intake data.
- Detects clusters: "Fever cases increased 42% in Village X."
- Visualizes symptom trends, disease categories, severity, and facility load on a geographic heatmap.

---

## 5. Dashboard Experiences

### 5.1 ASHA Dashboard (Human-Assisted Digital Facilitator)
- Patient registration, search, and proxy access.
- Initiate AI Clinical Intake (voice/touch assistance) and OCR document scanning.
- View triage status, create referrals, use the facility finder, and track high-risk follow-ups.
- Manage government schemes and broadcast notices.

### 5.2 Doctor Dashboard
Prioritizes clinical urgency. The doctor sees:
1. Critical/red cases & New referrals.
2. Today's appointments.
3. **AI-Generated Clinical Brief** (clearly marked: "AI-generated — clinician verification required").
4. Longitudinal medical timeline & Abnormal reports.
- **Actions:** Review, prescribe, refer, schedule follow-up, close encounter.

### 5.3 Patient Dashboard
- Icon-first, low-literacy friendly, and multilingual.
- Features: Start health check, view Health Card, medical history, prescriptions, reports, appointment status.
- SOS emergency trigger, mental stress support, and ASHA helper link.

### 5.4 Admin Dashboard (Community Health & Facility Operations)
- Village/region heatmaps for disease/symptom trends.
- Statistics on facility load, referrals, appointment completion, and medicine/diagnostic availability.
- ASHA/Doctor activity monitoring and system-wide broadcasts.

---

## 6. Offline-First Architecture

Offline operation is a core differentiator. 
- **Always Available Offline:** Cached patient records, Health Card, previously accessed reports, Touch-based structured intake, queued drafts/messages, deterministic red-flag rules.
- **Reconnection Sync:** Local Queue → WorkManager Sync → Server → Cloud AI Processing (if required) → Update local state. 
- **Conflict Resolution:** Last-write-wins with manual flagging for critical data (e.g., triage severity).

---

## 7. Multilingual & Accessibility
- Regional language UI support via Android string resources.
- Voice input / Text-to-Speech output.
- Icon-heavy interfaces with large touch targets.
- Hybrid touch fallback for noisy environments or speech failure.

---

## 8. ABDM / ABHA / FHIR Strategy

We define three integration levels to remain honest about compliance:
1. **Prototype (SIH Demo):** FHIR-compatible data structures (JSON exports) and a simulated "Login with ABHA" UI flow.
2. **Integration-Ready:** Codebase architected to easily map to FHIR resources (`Patient`, `Observation`, `Condition`, `Encounter`, `MedicationRequest`).
3. **Production (Out of Scope):** Official NHA Sandbox credentials, certified gateways, and live production APIs.

---

## 9. Security & Privacy

- **Prototype:** Firebase Auth (RBAC), explicit patient consent screens (audio-guided), ASHA proxy authorization checks in Firestore rules.
- **Production (Future):** SQLCipher local encryption, DPDP Act compliant audit logs, secure medical document storage.
- *AI constraints:* No API keys in the client; AI processing runs securely via backend Cloud Functions.

---

## 10. Existing VitalSense Features (Preservation Strategy)

| Feature | Action | Justification |
|---|---|---|
| ASHA Proxy | **KEEP** | Core to the "Human Facilitator" identity. |
| Heatmap | **MODIFY** | Upgrade from raw counts to an aggregated Community Health Early Warning system. |
| Offline-First | **KEEP** | Essential for rural deployment. |
| SOS | **MODIFY** | Tie into the deterministic triage engine; triggers ASHA/Emergency alerts with SMS fallback. |
| Mental Health | **KEEP** | Holistic care. Keep it simple: mood check-in, relaxation, and psychologist referral. |
| OCR | **MODIFY** | Upgrade from mocked UI to real ML Kit text extraction + AI structuring. |
| Government Schemes | **KEEP** | High value for rural patients; keep as informational directory. |
| Appointments | **MERGE** | Merge with the new Smart Referral and Follow-up workflows. |
| Medicine Stock | **MODIFY** | Keep as a prototype mock, but link it to the Facility Finder to recommend clinics that actually have the required drugs. |
| Admin Broadcasts | **KEEP** | Useful for public health alerts. |

---

## 11. Data Model (Conceptual)

- **User / Patient / ASHA / Doctor / Admin:** Core identities and RBAC.
- **Encounter:** Groups an intake session, doctor review, and prescription.
- **ClinicalHistory / Symptom:** Structured output from the AI intake.
- **TriageAssessment:** Rule-based urgency score (Red/Yellow/Green).
- **MedicalDocument / OCRExtraction:** Stored images and structured JSON of past labs/prescriptions.
- **Referral / Appointment / FollowUp:** Closed-loop tracking entities.
- **Facility / MedicineStock:** Mocked data for the smart facility finder.
- **Consent:** Records patient opt-in for data processing.

---

## 12. Role Permissions (Least Privilege)

| Feature | Admin | ASHA | Doctor | Patient |
|---|:---:|:---:|:---:|:---:|
| Patient Registration | ❌ | ✅ | ❌ | ✅ (Self) |
| Proxy Clinical Intake | ❌ | ✅ | ❌ | ❌ |
| View Assigned Medical Records | ❌ | ✅ (Proxy) | ✅ | ✅ |
| Generate Referrals | ❌ | ✅ | ✅ | ❌ |
| Write Prescriptions | ❌ | ❌ | ✅ | ❌ |
| View Community Heatmap | ✅ | ❌ | ❌ | ❌ |

---

## 13. Success Metrics (Prototype)

- **Time to complete clinical intake:** < 3 minutes via Voice/Touch.
- **OCR correction rate:** < 20% manual edits required on extracted data.
- **Doctor review time:** < 60 seconds to read the AI summary and make a decision.
- **Offline workflow completion:** 100% of touch-intakes queue successfully offline.

---

## 14. MVP / Phased Scope

### P0 — SIH Demo Critical
- AI-Assisted Clinical Intake (Voice + Touch).
- Triage & Red-Flag deterministic engine.
- OCR document extraction (ML Kit).
- AI Physician-ready structured summary.
- ASHA proxy workflow.
- Offline-first queuing (Room + WorkManager).

### P1 — High Value
- Longitudinal Medical Timeline.
- Smart Referral & Facility Finder (Mocked data).
- Closed-Loop Follow-up tracking.
- Community Health Early Warning Heatmap.
- ABDM / ABHA mock login screens.

### P2 — Optional
- AYUSH Dashavidha Pariksha toggle.
- Extended multilingual TTS.

### Out of Scope
- Autonomous AI diagnosis or AI-generated prescriptions.
- Video telemedicine.
- Real production ABDM/FHIR API integration.

---

## 15. Golden SIH Demo (7 Minutes)

1. **ASHA Login & Consent (1m):** ASHA worker selects a patient, simulates an ABHA ID scan, and the patient grants audio-guided consent.
2. **AI Clinical Intake (2m):** Patient speaks in Hindi: "I have had severe chest pain since yesterday." The app transcribes, runs SOCRATES, asks follow-up questions, and flags the case as RED (Urgent).
3. **Digitization (1m):** ASHA uses the camera to scan a crumpled past lab report. The OCR extracts an abnormal blood sugar level and flags it.
4. **Smart Referral (1m):** The app recommends transferring to the nearest PHC that has an available doctor and ECG equipment.
5. **Doctor Dashboard (1m):** The Doctor receives the case, views the AI-generated structured summary (Chief Complaint, SOCRATES, Triage Status, Abnormal Labs timeline), and issues a treatment plan.
6. **Community Impact (1m):** Admin dashboard heatmap updates to show a regional cluster of cardiac symptoms.

---

## 16. Non-Functional Requirements

- **Reliability & AI Fallback:** If the Cloud LLM fails/times out, the app gracefully degrades to the local Touch-based decision tree.
- **Offline Resilience:** App must not crash without network. Must clearly indicate "Sync Pending."
- **Privacy & Security:** No API keys in Android client. Sensitive AI processing occurs on secure backend Functions.

---

## 17. Prototype vs. Production

| Capability | SIH Prototype | Future Production |
|---|---|---|
| **AI Processing** | Cloud LLM (Gemini/Claude) via Firebase. | Fine-tuned, self-hosted medical LLMs for data privacy. |
| **ABDM / FHIR** | Mocked UI flows and internal JSON-to-FHIR mappers. | Certified NHA Sandbox gateways and real ABHA tokens. |
| **Facility Data** | Hardcoded mock JSON of clinics and medicine stock. | Real-time state health API integrations. |
| **Security** | Firebase Auth RBAC. | SQLCipher DB encryption, strict DPDP audit logs. |

---

## 18. Open Questions

- What specific regional language should be prioritized for the primary SIH live demo?
- Will the judging panel provide specific mock medical cases to test the AI intake against?

---

## 19. Final Feature Matrix

| Feature | Existing VitalSense | New Requirement | Final Decision | Priority |
|---|---|---|---|---|
| Condition Entry | Manual taps | AI Voice+Touch Intake | **MODIFY** | P0 |
| Prescription OCR | Mocked string | Real extraction + LLM | **MODIFY** | P0 |
| AI Clinical Summary | Missing | Yes | **NEW** | P0 |
| Triage Engine | Manual severity | Deterministic Rules | **MODIFY** | P0 |
| Medical Timeline | Basic card | Longitudinal view | **MODIFY** | P1 |
| Referrals | Missing | Smart Referral System | **NEW** | P1 |
| Facility Map | Basic nearest | Resource-aware routing | **MODIFY** | P1 |
| ASHA Proxy | Yes | Assist AI Intake | **KEEP** | P0 |
| Disease Heatmap | Yes | Early Warning Engine | **MODIFY** | P1 |
| Mental Health | Yes | - | **KEEP** | P2 |
| SOS | Yes | Tie to Triage Engine | **MODIFY** | P1 |
| Appointments | Yes | Tie to Follow-ups | **MERGE** | P1 |
