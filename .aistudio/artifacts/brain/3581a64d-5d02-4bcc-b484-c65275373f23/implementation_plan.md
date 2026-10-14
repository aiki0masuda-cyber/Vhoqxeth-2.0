# Password Protection, Profile Customization & Functional Settings Panel

Secure Vhoqxeth accounts with salted SHA-256 password protection at registration and login, present a non-blocking post-login optional profile completion modal sheet, replace cluttered bottom navigation tabs with a clean 3-tab structure (**Chats**, **Contacts**, **Settings**), and deliver a functional Settings panel with Profile Management.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product decisions were confirmed during interactive consultation:
> - **Bottom Navigation Structure**: Simplified to 3 core tabs: **Chats**, **Contacts**, and **Settings** (permanently removing *Calls*, *Channels*, and *Vault*).
> - **Profile Settings & Editability**: Inside Settings, the **Profile Settings** screen allows viewing and editing Visible Name, First Name, Last Name, Date of Birth, Description/Bio, and Password. The **Handle / User ID** is immutable and securely locked.
> - **Settings Categories**:
>   1. **Profile Settings**: Personal identity customization and password change with current password validation.
>   2. **Network IP & Connection Status**: Live device IP, roaming network recognition, and Firestore heartbeat.
>   3. **Notifications & Privacy**: Encryption toggles, alert tones, and read receipt preferences.
>   4. **Local Cache & Diagnostics**: Cache cleanup tool and diagnostic latency monitor.
>   5. **Protocol Charter**: Direct access to Privacy Policy, Terms & Conditions, and App Info.
>   6. **Sign Out**: Clean enclave logout returning to the auth gateway.
> - **Registration & Login**: Visible Name, Handle, and Password are required. First Name, Last Name, DOB, and Bio are optional and can be filled or skipped post-login.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **Password-Gated Authentication**: Enforces passwords during account creation and login, preventing unauthorized actors on the same local network or IP from accessing personal conversations.
  2. **Post-Authentication Profile Setup**: Displays a clean modal sheet after registration or initial login for optional details (First Name, Last Name, DOB, Bio) with **Save & Continue** and **Skip for Now** actions.
  3. **Streamlined Navigation (3 Tabs)**:
     - **Chats**: Active conversations and real-time messaging.
     - **Contacts**: Roster of added peers with real-time "Add Contact by User ID" discovery.
     - **Settings**: Functional command center for profile management, network telemetry, and app preferences.
  4. **Settings & Profile Customizer**: Central hub where users can update visible name, personal bio, full name, birthday, and account password whenever desired, with handle immutability preserved.
- **Target Audience / Persona**: Security-conscious mesh chat users who desire a minimalist, intuitive chat app without bloatware navigation tabs.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Account Registration**:
   - Fields: Visible Display Name, Handle/User ID (`@username`), Password, and Confirm Password with visibility toggles.
   - Validates password strength (minimum length) and handle availability.
   - On completion, logs in and presents the optional details onboarding modal sheet.

2. **Account Login**:
   - Fields: Handle/User ID and Password.
   - Verifies against salted SHA-256 hash in Firestore.
   - Prevents unauthenticated bypass on recognized IPs.

3. **Optional Profile Setup Modal (`OptionalProfileSetupSheet`)**:
   - Modal bottom sheet displayed upon registration or first login if optional attributes are empty.
   - Fields: First Name (optional), Last Name (optional), Date of Birth (optional), Description (optional).
   - Buttons: **Save & Continue** (primary) and **Skip for Now** (outlined).

4. **3-Tab Bottom Navigation**:
   - **Tab 1: Chats**: Conversation list, unread filter chips, empty state card, and direct messaging sheet.
   - **Tab 2: Contacts**: Searchable contact roster, "+ Add Peer" action button, peer status badges, and one-tap thread launching.
   - **Tab 3: Settings**: Comprehensive settings list with clean categorized card groups.

5. **Settings Panel & Profile Settings Screen**:
   - **Profile Settings Item**: Tapping opens the Profile Editor with avatar preview, locked handle (`@handle` with lock icon), editable Visible Name, First Name, Last Name, DOB, Description, and **"Update Password"** dialog.
   - **Network & Enclave Status**: Live IP address, Firestore cluster status, and cryptographic enclave indicator.
   - **Privacy & Notifications**: Toggles for notification alerts and message encryption headers.
   - **Local Storage & Cache**: Displays cached session size with a "Clear Cache" button.
   - **Protocol Charter**: Privacy Policy and Terms & Conditions modal viewers.
   - **Sign Out Button**: Safe session clearance with confirmation prompt.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Three Dedicated Tabs (Chats, Contacts, Settings)**:
  - *Chosen Approach*: Replace the previous 5 tabs (*Chats, Calls, Channels, Vault, Settings*) with a focused 3-tab layout.
  - *Why*: Eliminates non-functional dead ends (*Calls, Vault, Channels*) and elevates Contacts into a dedicated second tab for fast peer navigation.
- **Decision 2: Immutable Handle in Profile Settings**:
  - *Chosen Approach*: Allow editing Visible Name, First Name, Last Name, DOB, and Bio, but lock the Handle (`customUserId`).
  - *Why*: The handle is the primary key in Firestore (`/users/{userId}` and reciprocal contacts `/users/{peer}/contacts/{userId}`). Changing the handle would break reciprocal contacts and deterministic thread IDs (`thread_userA_userB`).
- **Decision 3: Password Update Security Guard**:
  - *Chosen Approach*: Updating the password requires entering the current password first.
  - *Why*: Ensures that even if an unlocked device is temporarily in another person's hands, they cannot lock out the true owner by silently changing the password.

---

## 4. Technical Architecture & Data Strategy

### System Layout

```
┌─────────────────────────────────────────────────────────────────┐
│                      MainActivity / Scaffold                    │
│                                                                 │
│  ┌─────────────────┐   ┌───────────────────┐   ┌─────────────┐  │
│  │   Chats Tab     │   │   Contacts Tab    │   │ Settings Tab│  │
│  │   - Threads     │   │   - Active Roster │   │ - Profile   │  │
│  │   - Direct Chat │   │   - Add Peer Modal│   │ - Network IP│  │
│  │   - Empty State │   │   - Peer Actions  │   │ - Privacy   │  │
│  └─────────────────┘   └───────────────────┘   │ - Cache     │  │
│                                                │ - Charter   │  │
│                                                └─────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │      Simplified NavigationBar (Chats | Contacts | Settings)│ │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Data Schema Updates (`UserProfile.kt` & `firebase-blueprint.json`)

```kotlin
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val customUserId: String = "",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val description: String = "",
    val hasCompletedOptionalSetup: Boolean = false,
    val associatedIps: List<String> = emptyList(),
    val currentIp: String = "",
    val createdAt: String = ""
)
```

### Security Rules (`firestore.rules`)
Ensure `isValidUser` validates the updated profile payload and permits updates from verified sessions.

---

## 5. Implementation Phases

1. **Phase 1: Cryptographic Password & Data Schema Layer**:
   - Update `UserProfile.kt` and `firebase-blueprint.json` with password hash, salt, and optional identity fields.
   - Implement `PasswordSecurityUtil.kt` (SHA-256 salted hashing and verification).
   - Update `AuthRepository.kt` with password registration, authentication, profile updates, and password change methods.
   - Update `firestore.rules` and verify with JavaScript rules unit tests.
2. **Phase 2: AuthGatewayScreen Password Registration & Login**:
   - Add Password and Confirm Password inputs with visibility toggles to `RegisterForm`.
   - Update `LinkExisting` and `Recognized` forms to mandate password entry.
3. **Phase 3: Post-Login Optional Profile Setup Modal**:
   - Create `OptionalProfileSetupSheet.kt` with First Name, Last Name, DOB, Description, **Save Details**, and **Skip for Now** actions.
   - Wire trigger in `ChatListScreen` / `MainActivity` upon login/registration when `!hasCompletedOptionalSetup`.
4. **Phase 4: Simplified 3-Tab Navigation & Contacts Screen**:
   - Update bottom navigation in `ChatListScreen` to have exactly 3 tabs: **Chats**, **Contacts**, and **Settings**.
   - Create `ContactsTabContent.kt` displaying the full user contact roster with search and "+ Add Peer by User ID" CTA.
5. **Phase 5: Full Settings Panel & Profile Editor**:
   - Create `SettingsScreenContent.kt` featuring:
     - Profile Settings (view/edit visible name, full name, DOB, bio; change password; locked handle).
     - Network IP & Status card.
     - Notifications & Privacy toggles card.
     - Local Cache & Diagnostics card with cache clear button.
     - Legal & Protocol Charter card.
     - Sign Out button.
   - Create `ProfileEditorDialog.kt` and `ChangePasswordDialog.kt`.
6. **Phase 6: Testing & Verification**:
   - Run compilation check (`compile_applet`).
   - Run unit & Robolectric tests (`gradle :app:testDebugUnitTest`).
