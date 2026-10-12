# Legal Consent Onboarding & Top Navigation Menu

Implementation plan for adding a top navigation bar with a three-dot overflow menu ("About the App", "Privacy Policy", "Terms & Conditions") and a first-boot tabbed consent onboarding screen before login or signup.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following requirements have been confirmed via Phase 1 clarification:
> - **First-Boot Policy Display**: On the first launch, the app presents an onboarding consent screen with tabs for **Privacy Policy** and **Terms & Conditions** before revealing the login/signup gateway.
> - **Top Bar Three-Dot Menu**: The top navbar includes a three-dot menu containing "About the App", "Privacy Policy", and "Terms & Conditions".
> - **Admin Stealth Rule**: "About the App" strictly highlights user-facing features (quantum encryption, IP identity, peer-to-peer messaging) and **must not list or disclose** the admin panel or masterkey.

- **Confirmed Decision 1**: Store `hasAcceptedTerms` locally in `SessionManager` so the introductory consent screen only appears on the very first boot.
- **Confirmed Decision 2**: Provide full-screen modal sheets with formatted sections when menu items ("About the App", "Privacy Policy", "Terms & Conditions") are selected from the three-dot menu.
- **Confirmed Decision 3**: Retain the hidden 7-tap gesture on the IP pill with `P@ssw0rd` masterkey for the Admin Portal without leaking its existence in any public copy.

---

## 1. Overview & Core Concept

- **What It Does**: Introduces a compliance and informational layer to Vhoqxeth. First-time users review and accept the Privacy Policy and Terms of Service via a clean, tabbed interface before proceeding to IP detection and account creation. The auth gateway now features a top navigation bar with a three-dot overflow menu allowing users to inspect app features, privacy commitments, and terms at any time.
- **Target Audience / Persona**: Users who value transparency regarding data handling and cryptography before entering their device identity.
- **Key Value**: Provides clear regulatory compliance, user transparency, and quick feature discovery while keeping administrative controls entirely unadvertised.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **First-Boot Legal Onboarding**:
   - App launches splash screen.
   - If not logged in and `hasAcceptedTerms == false`:
   - Transitions to `LegalConsentScreen` displaying:
     - Header: "Vhoqxeth Network Protocol Governance".
     - Tab row: **Privacy Policy** | **Terms & Conditions**.
     - Scrollable rich text explaining zero-telemetry policies, decentralized storage, and usage obligations.
     - Bottom pinned action: "Accept & Continue" button (disabled until tab viewed, or immediately enabled with clear consent).
     - Once accepted, sets `hasAcceptedTerms = true` in `SessionManager` and transitions to `AuthGatewayScreen`.
2. **Auth Gateway Navbar & Three-Dot Overflow Menu**:
   - Top bar contains:
     - Left: Brand mark with encrypted enclave badge.
     - Right: Three-dot overflow menu button (`MoreVert` icon).
   - Tapping the icon opens the dropdown menu with:
     - ℹ️ **About the App**: Opens modal dialog/sheet showcasing core features:
       - Quantum-grade end-to-end messaging encryption.
       - Zero-trust device IP routing & roaming support.
       - Decentralized ephemeral message channels.
       - Cross-device profile synchronization.
       - *(Explicitly omits any mention of admin console or masterkey).*
     - 🛡️ **Privacy Policy**: Opens full Privacy Policy sheet.
     - 📜 **Terms & Conditions**: Opens full Terms & Conditions sheet.
3. **Subsequent Boots**:
   - If already logged in, splash screen immediately opens `ChatListScreen` as before.
   - If logged out, directly opens `AuthGatewayScreen` (skipping the legal onboarding since terms were previously accepted).

### Visual Identity & Theme
- Consistent with the Vhoqxeth cyberpunk dark theme (`#0D0B14`, `#7C4DFF`, `#00E5FF`, `#1B1829`).
- Custom styling for dropdown menu and legal tabs with subtle neon indicators and clean typography.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Persistence of Acceptance State in `SessionManager`**
  - *Chosen Approach*: Add `hasAcceptedTerms(): Boolean` and `setAcceptedTerms(true)` to `SessionManager`.
  - *Why*: Synchronous zero-latency check during splash boot; does not block the app launch.
- **Decision 2: Comprehensive Legal Content Formatting**
  - *Chosen Approach*: Store structured legal copy in a dedicated `LegalContent.kt` object with clean markdown-like card sections for readability.
  - *Why*: Separates legal text from UI composables, making updates straightforward and readable.
- **Decision 3: Stealth Admin Preservation**
  - *Chosen Approach*: The three-dot menu and "About the App" dialog strictly contain user-facing product information. The 7-tap gesture on the IP pill remains the exclusive trigger for the Admin Portal.
  - *Why*: Directly honors the user's explicit instruction: *"admin panels is not listed"*.

---

## 4. Technical Architecture & Data Strategy

### Component Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                              MainActivity                              │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
               ┌────────────────────┼────────────────────┐
               ▼                    ▼                    ▼
   ┌───────────────────────┐ ┌──────────────┐ ┌─────────────────────────┐
   │     SplashScreen      │ │ChatListScreen│ │   LegalConsentScreen    │
   │  - isLoggedIn?        │ └──────────────┘ │   (First-boot only)     │
   │  - hasAcceptedTerms?  │                  └───────────┬─────────────┘
   └───────────┬───────────┘                              │ (Accept)
               │                                          ▼
               │                              ┌─────────────────────────┐
               └─────────────────────────────▶│    AuthGatewayScreen    │
                                              │ - Top Navbar            │
                                              │ - 3-Dot Overflow Menu   │
                                              │   ├─ About App Sheet    │
                                              │   ├─ Privacy Sheet      │
                                              │   └─ Terms Sheet        │
                                              │ - 7-Tap IP Admin Portal │
                                              └─────────────────────────┘
```

### Data Layer Specifications

- **`SessionManager` Extensions**:
  - `hasAcceptedTerms(): Boolean`
  - `setAcceptedTerms(accepted: Boolean)`
- **`LegalContent`**:
  - `aboutAppFeatures: List<FeatureItem>`
  - `privacyPolicySections: List<LegalSection>`
  - `termsAndConditionsSections: List<LegalSection>`
