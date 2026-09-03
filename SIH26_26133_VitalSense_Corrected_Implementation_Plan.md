# SIH26_26133 Corrected Implementation Blueprint for VitalSense

This document serves as the **official implementation roadmap** to upgrade VitalSense. The goal is to strongly address SIH26-26133 while preserving VitalSense's unique rural-health and ASHA-worker identity.

**Core Product Positioning:**
> An AI-powered, offline-first community healthcare and clinical-intake platform where ASHA workers act as human-assisted digital health facilitators, helping rural patients capture structured medical history, digitize previous records, identify red flags, and deliver a physician-ready summary to doctors.

---

## 1. FIRST: RE-AUDIT THE PREVIOUS PLAN

| Previous Recommendation | Status | Reason |
| :--- | :--- | :--- |
| **Voice AI Conversation** | ✅ KEEP | Essential for the SIH problem statement to eliminate the history-taking bottleneck. |
| **ABHA/ABDM Integration** | 🔄 MODIFY | The previous plan pushed for full integration (P0). Real ABDM Sandbox integration is too complex for a prototype timeframe. We will implement Level 2 (Mocked/Prototype Architecture) that is *ABDM-ready*. |
| **FHIR Schemas** | 🔄 MODIFY | Do not rewrite the entire Firestore DB to be strict FHIR. Keep simple NoSQL models but add an `exportToFHIR()` mapper for the demo payload. |
| **Bhashini ASR** | 🔄 MODIFY | Bhashini APIs can be unstable or slow. We will use Android's native `SpeechRecognizer` (which supports Indian languages offline/online) as the primary engine, and use LLM for the conversational logic. |
| **SOCRATES Framework** | ✅ KEEP | Perfect for structuring chief complaints systematically. |
| **AYUSH Dashavidha Pariksha** | 🟡 DEFER | Will overcomplicate the core MVP. We will add a simple "AYUSH Mode" toggle that adds 3-4 extra holistic questions, rather than a massive separate ontology. |
| **ML Kit OCR + LLM** | ✅ KEEP | ML Kit handles offline raw text extraction; LLM structures it when online. Highly practical. |
| **AI Red-Flag Detection** | 🔄 MODIFY | Cannot be 100% LLM-driven due to hallucination risks. Must be a hybrid: deterministic rules + LLM interpretation. |
| **Telemedicine / Video** | ❌ REMOVE | The PS focuses on the *intake* bottleneck before OPD consultations. Asynchronous text chat + physical appointment booking is sufficient. |

---

## 2. P0 — MUST IMPLEMENT (Core SIH)
* **AI-Assisted Clinical History Intake:** Hybrid Voice + Touch UI for capturing symptoms.
* **Structured Clinical History Generation:** AI synthesis of the conversation.
* **Red-Flag / Triage Detection:** Deterministic rules matching extracted symptoms to triage levels.
* **Medical Document OCR:** On-device scanning of old prescriptions/labs.
* **Physician-Ready Summary Dashboard:** Doctor's view showing Chief Complaint, HPI, and OCR timelines before they see the patient.
* **ASHA-Assisted Patient Intake:** Expanding ASHA proxy flows to run the MediKiosk session.

## 3. P1 — HIGH VALUE (Demo Enhancers)
* **Longitudinal Medical Timeline:** Merging old OCR'd documents with new AI intake sessions.
* **Multilingual UI & TTS:** Android native Text-to-Speech reading out AI questions in regional languages.
* **Offline-First Resilience:** Room Outbox queuing for AI processing when the ASHA regains internet.

## 4. P2 — OPTIONAL / DEMO ENHANCEMENTS
* **ABDM Mock Sandbox API:** A simulated endpoint showing how the data *would* push to the government network.
* **Facility Medicine Availability:** Tying prescriptions to local PHC stock (using the existing mocked dispensary data).

---

## 5. EXPLICITLY REMOVE OVER-ENGINEERING

### THINGS WE SHOULD NOT BUILD RIGHT NOW
* **Autonomous AI Diagnosis:** Extremely dangerous and explicitly not requested by the PS. AI must only summarize and flag, never diagnose.
* **Complex Video Telemedicine:** Distracts from the clinical intake problem.
* **Full Local LLMs:** Running an LLM on a cheap rural Android device will crash it. We must use cloud LLMs (Gemini/Claude) and degrade to deterministic touch-trees when offline.
* **Strict FHIR Database:** Building a fully compliant FHIR database in Firestore is overkill and hurts iteration speed. We will just map our internal objects to FHIR JSON at the boundary.

---

## 6. CORRECT ABHA / ABDM / FHIR STRATEGY

We will position VitalSense as **"ABDM/FHIR-ready"** rather than certified.

* **Level 1 — SIH Demo:** A fake "Login with ABHA" button that simulates the OTP flow and loads a mock profile.
* **Level 2 — Prototype Architecture:** The system stores data in simple JSON but includes an `EncounterMapper` utility that formats the final clinical summary into a FHIR R4 `Bundle` (containing `Patient`, `Encounter`, `Condition`, `Observation`) to prove interoperability logic.
* **Level 3 — Production:** (Out of scope) Official NHA Sandbox registration and milestone clearance.

---

## 7. AI ARCHITECTURE

**The Pipeline:**
1. **Input:** Android `SpeechRecognizer` (handles offline/online Indian languages) captures patient voice.
2. **Conversation Manager (Backend):** Firebase Cloud Function calls Gemini 1.5 Flash. Prompt: "Extract symptoms and decide the next logical clinical question."
3. **Structuring:** Once LLM decides intake is complete, it outputs a strict JSON summary.
4. **Red-Flag (Rules):** Android app checks the JSON against a local deterministic list (e.g., `if (symptoms.contains("chest pain")) status = RED`).
5. **OCR:** Google ML Kit extracts raw text from camera images on-device.
6. **OCR Structuring:** Raw text sent to LLM to extract Medications, Diagnoses, and Lab Values.
7. **Synthesis:** LLM merges Conversation JSON + OCR JSON into the final Physician Summary.

---

## 8. VOICE + TOUCH EXPERIENCE

**Hybrid Mode is Mandatory:**
* **Voice:** User holds a mic button and speaks ("Mujhe bukhar hai"). Android translates to text.
* **Touch Fallback:** The screen always shows 2-4 large, icon-based tap options predicted by the LLM (e.g., "Fever", "Cough", "Pain") so the user can just tap if the mic fails.
* **Language:** UI labels localized via standard Android `strings.xml`. TTS reads the LLM's question out loud.

---

## 9. ADAPTIVE QUESTIONING

We will not build an open-ended chatbot. The LLM prompt will be strictly constrained:
1. Identify Chief Complaint.
2. Run SOCRATES on the Chief Complaint.
3. Ask about relevant Past Medical History (max 2 questions).
4. Terminate conversation and output JSON.

*Constraint:* The LLM must not ask more than 7 questions total to prevent user fatigue in an OPD queue.

---

## 10. SOCRATES

The AI prompt will enforce the SOCRATES structure for pain or primary symptoms:
- **S**ite (Where is it?)
- **O**nset (When did it start?)
- **C**haracter (What does it feel like?)
- **R**adiation (Does it move anywhere?)
- **A**ssociations (Any other symptoms?)
- **T**iming (Constant or intermittent?)
- **E**xacerbating/Relieving (What makes it better/worse?)
- **S**everity (1-10 scale)

---

## 11. AYUSH MODE

**Lightweight Implementation:**
Add a toggle on the ASHA/Patient dashboard: `[ ] AYUSH Intake`.
If checked, the LLM is instructed to append three specific questions to the end of the interview:
1. Digestion/Appetite (Agni)
2. Bowel habits (Koshtha)
3. Sleep & Lifestyle (Vihara)
These will be summarized in a dedicated "AYUSH Parameters" section of the physician summary.

---

## 12. RED-FLAG / TRIAGE ENGINE

**Safety-Critical Architecture:**
Do NOT rely on the LLM to output `"is_emergency": true`.
Instead:
1. LLM outputs extracted symptoms: `["chest pain", "sweating", "left arm pain"]`
2. Local Android `TriageEngine.kt` runs a deterministic rule check:
   `val redFlags = listOf("chest pain", "breathing difficulty", "paralysis")`
   `if (symptoms.any { it in redFlags }) { triggerSos(patient) }`

This guarantees that life-threatening symptoms are never missed due to AI hallucination.

---

## 13. OCR PIPELINE

**Workflow:**
1. Camera intent captures image.
2. `TextRecognition.getClient()` (Google ML Kit) extracts raw text blocks locally (works offline!).
3. If online, raw text is passed to Firebase Function.
4. LLM Prompt: "Extract date, diagnosis, prescribed medicines (with dose), and lab tests (flag if out of reference range). Output JSON."
5. UI presents an editable form so the ASHA/Patient can correct OCR mistakes before saving to DB.

---

## 14. LONGITUDINAL MEDICAL TIMELINE

Instead of a scattered list of uploads, `PatientTimelineScreen` will show a unified vertical timeline:
- **Jan 2024:** Lab Report (OCR extracted: High HbA1c)
- **Mar 2024:** Prescription (OCR extracted: Metformin 500mg)
- **Today:** AI Intake Session (CC: Dizziness)

---

## 15. AI PHYSICIAN SUMMARY

The final output rendered for the Doctor must be clean, structured, and clinically familiar:

```text
⚠️ AI-Generated Summary — Clinician Verification Required ⚠️

CHIEF COMPLAINT: Chest pain (Onset: 2 days ago)
HPI (SOCRATES): Crushing pain, radiates to left arm, constant. Severe (8/10).
RELEVANT PAST HISTORY: Hypertension (diagnosed 2019).
MEDICATIONS (From OCR): Amlodipine 5mg.
RED FLAGS: 🚨 Chest Pain, 🚨 Radiation to arm.
```

---

## 16. DOCTOR EXPERIENCE

- **Dashboard:** Sorted by Triage Level (Red patients at the top, then Yellow, then Green).
- **Clicking a Patient:** Immediately shows the **AI Physician Summary** (not the raw chat log).
- **Tabs:** `[Summary] [Timeline/Documents] [Prescribe/Refer]`

---

## 17. ASHA WORKER EXPERIENCE

ASHA remains the **Human-Assisted Digital Facilitator**.
- ASHA selects a patient from her caseload.
- Clicks `Start Clinical Intake`.
- Hands phone to patient for Voice input, OR ASHA speaks on behalf of the patient, OR ASHA taps the UI options.
- ASHA snaps photos of the patient's crumpled documents.
- ASHA reviews the AI's final summary and hits `Submit to Doctor`.

---

## 18. OFFLINE-FIRST ARCHITECTURE

We will preserve VitalSense's Room + WorkManager architecture.
- **Offline Intake:** If no internet, the LLM voice conversation is disabled. It falls back to a hardcoded Tap-Based decision tree (e.g., Select Body Part -> Select Symptom -> Select Duration).
- **Offline OCR:** ML Kit extracts raw text. The raw text and tap-based symptoms are queued in Room Outbox.
- **Reconnection:** WorkManager detects internet, fires the raw text and offline symptoms to the backend LLM to generate the final physician summary, then pushes it to the Doctor's queue.

---

## 19. EXISTING VITALSENSE FEATURES

| Feature | Action | Reason |
| :--- | :--- | :--- |
| **ASHA Proxy** | KEEP | Essential for the "Human Kiosk" positioning. |
| **Disease Heatmap** | KEEP | Massive differentiator for Admin/Public Health. |
| **SOS** | MODIFY | Wire it into the deterministic Red-Flag engine. |
| **Mental Health** | KEEP | Differentiator. Include a mental-health prompt in the AI intake. |
| **Condition Entry** | REPLACE | Replaced by the new AI MediKiosk conversation engine. |
| **Dispensary Stock**| DEFER | Keep mocked, use it to warn doctor if they prescribe out-of-stock items. |

---

## 20. COMMUNITY HEALTH INTELLIGENCE

The AI Intake outputs structured JSON. This makes it trivial to aggregate data for the Admin Heatmap.
- Backend aggregates: `count of 'fever' in Village A over last 7 days`.
- The Heatmap plots these early warning signals before formal diagnoses are even made by the doctor.

---

## 21. DATABASE DESIGN (Firestore / Room)

*Keep existing models, add the following fields/entities:*

* **`Encounter` (New):** Represents a single OPD visit.
  - `id`, `patientId`, `ashaId`, `timestamp`, `status` (pending, reviewed, completed).
* **`ClinicalHistory` (New):** Linked to Encounter.
  - `rawChatLog` (List of messages), `structuredSummary` (JSON), `triageLevel` (RED/YELLOW/GREEN).
* **`MedicalDocument` (Modified):** 
  - `imageUrl`, `rawOcrText`, `extractedEntities` (JSON), `documentDate`.
* **`Patient` (Modified):**
  - `abhaId` (String), `consentGranted` (Boolean).

---

## 22. EXACT FILE-LEVEL IMPLEMENTATION PLAN

| Existing File | Action | New / Modified File | Description |
| :--- | :--- | :--- | :--- |
| `feature/patient/ConditionEntryScreen.kt` | MODIFY | `AiMediKioskScreen.kt` | Replace manual entry with Voice/Touch AI chat interface. |
| `feature/prescriptions/ocr/PrescriptionOcrDialog.kt` | MODIFY | `PrescriptionOcrScreen.kt` | Integrate Google ML Kit Text Recognition API. |
| `feature/doctor/CaseDetailScreen.kt` | MODIFY | `ClinicalSummaryScreen.kt` | Redesign to show the AI Physician Summary and Timeline. |
| `feature/patient/HealthCardViewerScreen.kt` | MODIFY | `PatientTimelineScreen.kt` | Evolve the basic card into a chronological list of documents and intakes. |
| `core/data/local/...` | MODIFY | `TriageEngine.kt` | Add deterministic red-flag rule evaluator. |

---

## 23. API / SERVICE PLAN (Firebase Cloud Functions)

*   `initiateAiSession(patientContext)` -> Returns first AI greeting.
*   `processVoiceTurn(transcript, chatHistory)` -> Returns next question OR structured JSON if complete.
*   `extractMedicalEntities(rawOcrText)` -> Returns JSON of meds/labs.
*   `generateFHIRBundle(encounterId)` -> Maps internal data to FHIR JSON for mock ABDM export.

---

## 24. SECURITY

**SIH Prototype vs Production:**
*   **Prototype:** We will use Firebase Auth custom claims (already implemented). We will add a mock `ConsentScreen` with a checkbox and audio playback explaining data usage. API keys for LLMs will be stored securely in Firebase Functions config, NOT in the Android app.
*   **Production:** Would require local SQLCipher encryption, DPDP audit logs, and NHA certified ABHA gateways.

---

## 25. IMPLEMENTATION ORDER

*   **Phase 1 — OCR & Timeline:** Implement ML Kit in `PrescriptionOcrScreen`. Build `PatientTimelineScreen` to display them.
*   **Phase 2 — AI Conversational UI:** Build `AiMediKioskScreen` with Android SpeechRecognizer and basic chat UI.
*   **Phase 3 — LLM Backend:** Write the Firebase Functions to drive the conversation and summarize into SOCRATES format.
*   **Phase 4 — Triage Engine:** Write local deterministic rules to flag emergencies.
*   **Phase 5 — Doctor Dashboard:** Update `ClinicalSummaryScreen` to consume the new JSON summaries.
*   **Phase 6 — ABDM / Consent:** Add the fake ABHA login and consent flow. Ensure offline queuing works.

---

## 26. GOLDEN DEMO PATH (7 Minutes)

1. **(1m) Identification & Consent:** ASHA opens VitalSense, clicks "New Patient", simulates ABHA scan. Patient taps "I Consent".
2. **(2m) AI Intake (Voice):** ASHA hits microphone. Speaks in Hindi: "The patient has had severe stomach pain for 2 days." App replies (text+audio) asking if there is vomiting (SOCRATES).
3. **(1m) OCR Digitization:** ASHA points camera at a handwritten mock prescription. App extracts "Paracetamol 500mg".
4. **(1m) Triage & Push:** App evaluates "severe stomach pain" -> flags as YELLOW. Saves structured summary.
5. **(1m) Doctor Review:** Switch to Doctor tablet. Doctor clicks top patient. Sees perfect English summary, SOCRATES breakdown, and digitized old prescription.
6. **(1m) Admin Heatmap:** Switch to Admin. Shows a new blip on the village map for gastrointestinal symptoms.

---

## 27. TESTING PLAN

*   **AI Guardrails:** Feed the AI prompt non-medical inputs ("Tell me a joke") and ensure it forcefully redirects to history-taking.
*   **Triage Safety:** Input "crushing chest pain" and verify the local rule engine flags it RED immediately, bypassing the LLM if necessary.
*   **Offline Mode:** Turn off device Wi-Fi. Verify the app drops into "Touch-only mode", queues the payload in Room, and resumes when Wi-Fi is restored.
*   **OCR Noise:** Test ML Kit with blurry photos to ensure the human-editable confirmation screen works properly.

---

## 28. PERFORMANCE & RELIABILITY

*   **LLM Timeout:** If Firebase Function takes > 8 seconds, the UI must fallback to the hardcoded tap-tree automatically.
*   **Duplicate Sync:** WorkManager constraints must use unique Worker IDs to prevent submitting the same intake twice.
*   **Crash Prevention:** `SpeechRecognizer` throws frequent errors on noisy audio; wrap it in robust try/catch blocks that gracefully prompt the user to "Please try again or use buttons."

---

## 29. FINAL FEATURE MATRIX

| Feature | Current VitalSense | SIH Required? | Action | Priority | Final State |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Condition Entry | Manual taps | Yes (Intake) | REPLACE | P0 | AI Voice+Touch MediKiosk |
| Prescription OCR | Mocked string | Yes | UPGRADE | P0 | ML Kit + LLM Extraction |
| Clinical Summary | Missing | Yes | NEW | P0 | LLM Generated structured view |
| Red Flag Triage | Manual severity | Yes | MODIFY | P0 | Deterministic Rules + LLM |
| ABHA Login | Missing | Yes | NEW | P1 | Mocked OAuth/Consent flow |
| Medical Timeline | Missing | Yes | NEW | P1 | Unified chronological view |
| ASHA Proxy | Yes | Yes (Kiosk) | KEEP | P0 | ASHA facilitates AI intake |
| Disease Heatmap | Yes | Optional | KEEP | P2 | Aggregates AI-structured data |

---

## 30. FINAL ARCHITECTURE

```text
                    VITALSENSE
                         |
          +--------------+--------------+
          |                             |
       PATIENT                         ASHA (Human Facilitator)
          |                             |
          +-------------+---------------+
                        |
            AiMediKiosk Clinical Intake
         (Android SpeechRecognizer + ML Kit)
                        |
              +---------+---------+
              |                   |
        Cloud LLM Logic     Room Outbox (Offline)
              |                   |
              +---------+---------+
                        |
            Structured Clinical JSON
                        |
             +----------+----------+
             |                     |
   Local Triage Engine     FHIR Export Mapper (Mock)
             |                     |
             ↓                     ↓
       Red-Flag Alert        Doctor Dashboard
             |                     |
             +----------+----------+
                        |
                 Clinical Summary
                (SOCRATES + Timeline)
                        |
                Prescription / Referral
                        |
              Aggregated Community Data
                        ↓
                  Admin Heatmap
```

---

## 31. WHY VITALSENSE IS NOT JUST ANOTHER SIH HEALTHCARE APP

Most teams will build a generic tablet kiosk or a WhatsApp chatbot. VitalSense differentiates by acknowledging the reality of rural Indian healthcare: **Hardware kiosks fail where digital literacy is zero.**

VitalSense leverages the **ASHA worker as a human-assisted digital facilitator**. By giving the ASHA worker an offline-capable, voice-enabled AI tool, we solve the intake bottleneck without expecting a 70-year-old rural farmer to navigate a touch screen alone. The inclusion of the **Disease Heatmap** proves that VitalSense understands the macro public health value of digitizing this data, not just the micro clinical value.

---

## 32. FINAL SCORE

### Current VitalSense
* SIH Problem Alignment: 4/10
* Rural Relevance: 9/10
* Clinical Intake: 3/10
* AI Readiness: 2/10
* Offline Readiness: 6/10
* Interoperability: 0/10
* Differentiation: 7/10
* Demo Readiness: 5/10

### After Implementation
* SIH Problem Alignment: 10/10
* Rural Relevance: 10/10
* Clinical Intake: 9/10 (Safe, hybrid AI)
* AI Readiness: 9/10
* Offline Readiness: 8/10
* Interoperability: 7/10 (Mocked/Prototype level)
* Differentiation: 10/10 (ASHA human-kiosk model)
* Demo Readiness: 10/10

---

## 33. IMPLEMENTATION CHECKLIST

### 🔴 P0 — DO FIRST
- [ ] Replace `ConditionEntryScreen.kt` with `AiMediKioskScreen.kt`.
- [ ] Implement Android `SpeechRecognizer` for voice input.
- [ ] Write Firebase Cloud Function to handle LLM conversational logic.
- [ ] Integrate Google ML Kit in `PrescriptionOcrScreen.kt`.
- [ ] Write `TriageEngine.kt` with deterministic red-flag rules.
- [ ] Redesign `CaseDetailScreen.kt` to show the structured AI Physician Summary.

### 🟠 P1 — DO NEXT
- [ ] Implement `PatientTimelineScreen.kt` to merge old OCR documents and new intakes.
- [ ] Add fake ABHA Login and audio-guided Consent flow.
- [ ] Implement Android TTS for reading LLM questions aloud.
- [ ] Ensure Room Outbox queues AI intake payloads when offline.

### 🟢 P2 — IF TIME PERMITS
- [ ] Add "AYUSH Mode" toggle and LLM prompt modifiers.
- [ ] Write `EncounterMapper.kt` to export the summary as a FHIR R4 JSON string.
- [ ] Tie Doctor's prescription UI to the mock dispensary stock data.

### ❌ DO NOT BUILD NOW
- [ ] Autonomous AI diagnosis engine.
- [ ] Real ABDM Sandbox integration (keys, certificates, webhooks).
- [ ] Video telemedicine consultation.
- [ ] Complex local on-device LLMs.
