# Local Session Persistence & First-Run Login Gating

Implementation plan to store authentication and login state locally, ensuring the login/signup screen only appears on the first run, followed by seamless direct entry into chats on subsequent launches.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences have been confirmed via Phase 1 clarification:
> - **Subsequent Launch Behavior**: Brief splash screen followed by instant opening of the chat screen using locally persisted credentials.
> - **Sign-Out Behavior**: Explicitly clears the local login session and returns to the login/signup gateway screen.

- **Confirmed Decision 1**: Store session data (login state, Display Name, Custom User ID, and linked IP history) locally using Android's persistent preferences via a dedicated `SessionManager`.
- **Confirmed Decision 2**: During splash boot, check local session state. If logged in, load the cached user profile and transition directly to `ChatListScreen` without prompting for login or signup.
- **Confirmed Decision 3**: Provide robust error recovery during IP detection and Firestore synchronization so network hiccups never lock the user out.

---

## 1. Overview & Core Concept

- **What It Does**: Persists user session state on the device. On the very first launch, the app prompts the user to create an account or log in via IP address and custom handle. Once authenticated, the login/signup screen never reappears on subsequent launches; instead, the app displays a brief splash screen and immediately opens the chat experience.
- **Target Audience / Persona**: Users seeking a streamlined, frictionless messaging client that remembers their device identity and launches directly into active conversations.
- **Key Value**: Eliminates repetitive login screens, reduces cold-start latency, and provides reliable local session control with explicit sign-out support.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **First-Run Launch (Zero State)**:
   - App boots with animated splash screen.
   - Detects `isLoggedIn == false`.
   - Transitions into `AuthGatewayScreen` showing the detected device IP with options to "Create Account" or "Log In with Custom User ID".
   - User completes registration or logs in; session is saved locally (`isLoggedIn = true`, credentials cached).
   - Seamlessly navigates to `ChatListScreen`.
2. **Subsequent Launches (Logged-In State)**:
   - App boots with a brief quantum pulse splash animation (~1.2 seconds).
   - Detects `isLoggedIn == true` from local storage.
   - Instantly navigates to `ChatListScreen` with cached user profile loaded.
   - Quietly verifies and updates IP network coordinates in the background.
3. **Sign-Out Flow**:
   - User opens the Profile Sheet from the top-left avatar in `ChatListScreen`.
   - Taps "Sign Out / Switch Identity".
   - Clears local session cache (`isLoggedIn = false`, cleared preferences).
   - Navigates back to `AuthGatewayScreen`.

### Visual Identity & Theme
- Maintains the obsidian dark theme (`#0D0B14`), neon violet (`#7C4DFF`), and quantum cyan (`#00E5FF`) visual language.
- Animated transition between Splash and Chat screens with smooth fade-in/fade-out physics.
- Clear, descriptive inline error banners with retry buttons on network or database validation failures.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Local Session Storage via `SessionManager`**
  - *Chosen Approach*: Persist login state, user ID, custom handle, display name, and last known IP using Android `SharedPreferences`.
  - *Why*: Synchronous zero-latency reads at application boot without asynchronous initialization locks or SQLite migrations.
  - *Alternatives Considered*: Pure in-memory state forces re-login on every app restart; heavy Room database for 4 key-value fields adds unnecessary overhead.
- **Decision 2: Background IP Synchronization for Returning Users**
  - *Chosen Approach*: When launching directly into chats from a saved session, the app resolves the current IP in the background and links it to the profile if changed.
  - *Why*: Prevents network calls from delaying the instant chat UI launch while keeping database IP associations up-to-date across Wi-Fi/mobile roaming.
- **Decision 3: Hardened Error Recovery in Auth Flows**
  - *Chosen Approach*: Wrap all IP lookups and Firestore document writes in explicit retry and error-handling blocks with friendly error banners and fallback device IDs.
  - *Why*: Prevents app freezes or blank states if the public IP lookup times out or if Firestore encounters transient network latency.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                              MainActivity                              │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
               ┌────────────────────┴───────────────────┐
               ▼                                        ▼
   ┌───────────────────────┐               ┌─────────────────────────┐
   │     SplashScreen      │               │     SessionManager      │
   │  (Checks Session)     │──────────────▶│  - isLoggedIn()         │
   └───────────┬───────────┘               │  - getSavedProfile()    │
               │                           │  - saveSession()        │
               │                           │  - clearSession()       │
               ├─────────────────────────┐ └─────────────────────────┘
               ▼                         ▼
   ┌────────────────────────┐┌────────────────────────┐
   │   AuthGatewayScreen    ││     ChatListScreen     │
   │  (First Run / Logged   ││   (Subsequent Launches │
   │   Out State)           ││    Direct Entry)       │
   └───────────┬────────────┘└───────────┬────────────┘
               │                         │
               ▼ (on register/login)     ▼ (on sign out)
       saveSession(profile)         clearSession()
```

### Data Storage & Schema

1. **Local Preferences (`vhoqxeth_session`)**:
   - `KEY_IS_LOGGED_IN`: Boolean
   - `KEY_USER_ID`: String (Custom handle)
   - `KEY_CUSTOM_USER_ID`: String
   - `KEY_DISPLAY_NAME`: String
   - `KEY_CURRENT_IP`: String
   - `KEY_ASSOCIATED_IPS`: Set<String>
2. **Error Handling Matrix**:
   - IP lookup failure -> Uses `DEV-<android_id>` fallback, does not block registration.
   - Duplicate custom handle during registration -> Gracefully prompts to choose another handle or log in.
   - Unfound custom handle during IP linking -> Displays "Custom user ID not found, please check handle or create account".
