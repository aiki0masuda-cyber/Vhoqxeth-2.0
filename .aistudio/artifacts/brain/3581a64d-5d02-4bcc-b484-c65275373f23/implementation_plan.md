# Admin Quarantined Account Enclave & Autonomous AI Self-Healing System

Deliver root-level quarantined account inspection in the Admin Portal—allowing administrators to tap any user to open a secured modal overlay sheet with full authority to view, send, and delete messages, manage contacts, and inspect identity—paired with a continuous background AI error detection and silent auto-healing engine.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product decisions were established during interactive consultation:
> - **Quarantined Session UI**: Rendered as a dedicated **Modal Overlay Sheet** directly in the Admin Panel with quick action tabs (**Messages & Threads**, **Contacts Roster**, **Identity & Credentials**, and **Enclave Audit Log**).
> - **Administrative Authority**: Full root authority inside quarantine—inspect all conversation threads, view encrypted message histories, send new messages directly under the quarantined user's persona, delete individual messages, add/remove contacts, and view or reset authentication state.
> - **AI Error Detection & Auto-Healing**: Operates via a **continuous background engine** that silently detects and resolves system anomalies (session cache drift, orphan threads, broken reciprocal contacts, Firestore listener drops, and unhandled state faults) without interrupting the user experience.
> - **Zero Disruption to User Privacy on Home Screens**: The quarantined enclave remains strictly isolated within the password-protected Admin Portal; standard users on home screens only experience the benefits of silent auto-healing and stability.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **Quarantined Account Enclave**: Allows root admins inside the Admin Portal to click any user in the Users list to instantly launch a high-privilege quarantined workspace. In this enclave, the admin can navigate the user's active conversations, send real messages as that user, delete compromised or abusive messages, add/remove contacts, and examine raw profile attributes.
  2. **Autonomous AI Self-Healing Engine (`AppSelfHealingEngine`)**: An active background scanner running in coroutine scope that continuously monitors app subsystems:
     - **Session & Identity Integrity**: Detects inconsistencies between local DataStore/Prefs and Firestore user documents, auto-repairing profile attributes.
     - **Reciprocal Contact Mesh**: Checks for asymmetrical contact relationships and silently auto-provisions missing reciprocal contact records.
     - **Thread Consistency & Message Ordering**: Identifies duplicate message IDs, malformed participant arrays, or broken timestamps and automatically cleans them up.
     - **Network & Listener Health**: Monitors Firestore connectivity and automatically recreates dropped real-time snapshot listeners.
     - **Silent Operation & Diagnostics**: Runs unobtrusively in the background while logging healed issues to the Diagnostics Log in Settings and the Admin Portal.
- **Target Persona**: Network administrators and operators managing mesh communication clusters who need diagnostic intervention capabilities and automated resilience against runtime errors.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Opening Quarantined Account Session**:
   - In Admin Portal -> **Users** tab, tapping any `UserAdminCard` or its new **"Enter Quarantine"** shield action opens the `QuarantinedAccountSheet`.
   - The sheet opens as an elevated modal overlay featuring an Amber & Crimson security banner: `"QUARANTINE ENCLAVE: @{handle}"` with live status, device IP, and root authority tags.

2. **Quarantine Quick Action Tabs**:
   - **Tab 1: Messages & Threads**:
     - Lists all active conversation threads for the quarantined user.
     - Clicking a thread expands the conversation view with full message history.
     - Admin can type and send messages (tagged with admin persona or native user handle).
     - Admin can delete any message directly with one tap and immediate Firestore deletion.
   - **Tab 2: Contacts Roster**:
     - Displays all contacts connected to the quarantined account.
     - "+ Add Contact" allows injecting a peer directly into the account's roster.
     - "Remove" deletes the contact reciprocal connection.
   - **Tab 3: Identity & Access**:
     - Displays Display Name, Custom User ID, Registered IP, Salted Password Hash, and Optional Profile details.
     - Quick actions to update visible attributes or trigger emergency password resets.
   - **Tab 4: Audit Log**:
     - Displays live timestamped logs of all administrative actions performed within this quarantine session.

3. **Autonomous AI Self-Healing in Background**:
   - Runs in the background on app startup and periodic intervals (every 45 seconds).
   - Performs automated health checks:
     - Checks session validity and recovers missing profile data.
     - Verifies database socket readiness and reconnects if dropped.
     - Validates local contact lists against cloud states.
   - When an issue is fixed, it registers a silent repair event in `DiagnosticsRepository`.
   - In **Settings -> Local Cache & Diagnostics**, users see an "Autonomous AI Health Guardian: ACTIVE (All systems nominal - X issues auto-repaired)".

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct In-Place Quarantined Sheet vs. Global Session Swap**:
  - *Chosen Approach*: Open the quarantined session inside an isolated modal bottom sheet within the Admin Portal rather than replacing the device's primary session.
  - *Why*: Prevents the admin's own credentials and device session from being overwritten. The admin can inspect multiple user accounts in succession and immediately return to the admin dashboard.
- **Decision 2: Comprehensive Authority (Read, Send, Delete)**:
  - *Chosen Approach*: Provide full read, send, and delete controls inside the quarantine interface.
  - *Why*: Allows admins to troubleshoot delivery problems, remove illicit messages, or test account communications directly.
- **Decision 3: Silent Background Auto-Healing**:
  - *Chosen Approach*: Execute error recovery routines asynchronously in background coroutines without disruptive popups or alerts.
  - *Why*: Ensures seamless user experience while preventing silent data drift and stale synchronization.

---

## 4. Technical Architecture & Data Strategy

### System Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        AdminPortalScreen                               │
│  ┌──────────────────────┐  ┌────────────────────────────────────────┐  │
│  │ Registered IPs Tab   │  │ Users Tab                              │  │
│  └──────────────────────┘  │  - User Card -> [Quarantine Click]    │  │
│                            └───────────────────┬────────────────────┘  │
│                                                │                       │
│                                                ▼                       │
│                       ┌─────────────────────────────────────────────┐  │
│                       │    QuarantinedAccountSheet (Modal Overlay)  │  │
│                       │ ┌─────────────────────────────────────────┐ │  │
│                       │ │ Tabs: Messages | Contacts | Identity    │ │  │
│                       │ ├─────────────────────────────────────────┤ │  │
│                       │ │ - Live Message Viewer (Send / Delete)   │ │  │
│                       │ │ - Contacts Manager (Add / Remove)       │ │  │
│                       │ │ - Enclave Audit Log                     │ │  │
│                       │ └─────────────────────────────────────────┘ │  │
│                       └─────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────┐
│             Autonomous AI Background Engine (AppSelfHealingEngine)     │
│  ┌────────────────────┐  ┌─────────────────────┐  ┌──────────────────┐ │
│  │ Session Auditor    │  │ Mesh Sync Auditor   │  │ Listener Guardian│ │
│  │ - Fixes drift      │  │ - Reciprocal peers  │  │ - Auto-reconnect │ │
│  └────────────────────┘  └─────────────────────┘  └──────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### Components to Build / Update

1. **`AppSelfHealingEngine.kt`** (`com.example.data`):
   - Background singleton coroutine worker.
   - Detects corrupted local preferences, dead listeners, and orphaned records.
   - Auto-executes repairs and logs healed events to memory and diagnostics.
2. **`QuarantinedAccountSheet.kt`** (`com.example.ui.admin`):
   - Modal bottom sheet displaying the quarantined user session with Amber/Crimson theme.
   - Quick action tabs: Messages, Contacts, Identity, and Enclave Logs.
   - Embedded live thread viewer with send and delete capabilities.
3. **`AdminPortalScreen.kt`**:
   - Connect clicking on any user card directly to launching the `QuarantinedAccountSheet`.
   - Pass `adminRepository` and `meshChatRepository` for root operations.
4. **`MainActivity.kt`**:
   - Initialize and start `AppSelfHealingEngine` upon app launch.
5. **`SettingsTabContent.kt`**:
   - Display AI Auto-Healing status and repair tally under Diagnostics.

---

## 5. Verification Plan

1. **Compilation Check**: Run `compile_applet` to ensure Kotlin compilation and Compose layouts succeed.
2. **Admin Quarantined Access Verification**:
   - Navigate to Admin Portal -> Users list.
   - Click on any user card -> verify `QuarantinedAccountSheet` opens with correct user handle.
   - Test viewing messages, sending an admin message, deleting a message, and viewing contacts roster.
3. **AI Self-Healing Verification**:
   - Verify `AppSelfHealingEngine` runs continuously in background.
   - Verify self-healing diagnostics entry in Settings and Admin Portal.
