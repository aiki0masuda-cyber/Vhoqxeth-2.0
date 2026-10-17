# DAV InfoExchange — Institutional Rebranding & Faculty-Student Role Architecture

Transform the application from **DAV Cheeka** to **DAV InfoExchange** across the entire UI, adopt the official **DAV College Managing Committee** emblem (ESTD 1886) as the primary launcher and in-app insignia, and introduce dedicated **Student** vs **Teacher** role selection during account onboarding and administration.

---

## User Review & Clarifications Alignment

> [!IMPORTANT]
> The following user-confirmed specifications guide this implementation:
> 1. **Application Name**: Rename to **DAV InfoExchange** throughout the entire UI, launcher label, metadata, and build configurations.
> 2. **Official DAV Emblem**: Adopt the uploaded DAV College Managing Committee crest (circular seal with "D.A.V. COLLEGE MANAGING COMMITTEE", radiant rising sun, sacred "ओ३म्", blue water ripples, laurel wreath, and "ESTD 1886" ribbon banner) with clean presentation as requested ("Just like as it is").
> 3. **Role-Based Onboarding (Student vs Teacher)**:
>    - During user registration or admin creation, user first chooses: **Student** or **Teacher**.
>    - **Teacher Fields**: Prompts for **Subject / Department** and **Employee ID / Designation**.
>    - **Student Fields**: Prompts for **Grade / Class**, **Section**, and **Subject Specialization**.
> 4. **Visual Differentiation**:
>    - **Verified Faculty Badge**: Displays a prominent `[ FACULTY ✓ ]` badge with verified shield icon beside Teacher handles in chat bubbles, conversation threads, peer rosters, and profile cards.
> 5. **Core Architecture Preserved**:
>    - Admin Quarantined Enclave with full authority.
>    - Autonomous AI Self-Healing Guardian.
>    - Salted SHA-256 password cryptography and real-time Firestore listeners.

---

## 1. User Flows & Interface Changes

### 1.1 Splash & Authentication Gateway
- **Splash Screen**:
  - Displays the official DAV College Managing Committee insignia.
  - Title: **DAV InfoExchange**.
  - Subtitle: **DAV COLLEGE MANAGING COMMITTEE • ESTD 1886**.
- **Auth Gateway Screen**:
  - Top header and brand card display the DAV emblem and **DAV InfoExchange** title.
  - In the Registration / Sign-Up tab:
    - User enters Visible Name, Handle (`@username`), Password, and Confirm Password.
    - Immediately below, an interactive Role Selector appears:
      - **[ Student ]** (Icon: `School`) vs **[ Teacher ]** (Icon: `WorkspacePremium`).
    - Depending on the selected role, the initial scholastic onboarding pre-configures the account.

### 1.2 Academic Profile Setup & Editing
- **Optional Profile Setup Sheet & Profile Editor Dialog**:
  - If **Student**:
    - Grade / Class dropdown or input (e.g., "10", "11", "12")
    - Section (e.g., "A", "B", "C")
    - Subject Interests / Bio
  - If **Teacher**:
    - Department / Subject Area (e.g., "Mathematics & Computing", "Department of Physics")
    - Designation / Employee ID (e.g., "Senior PGT Lecturer", "DAV-FAC-204")
    - Academic Bio & Office Hours (e.g., "Senior Wing • Room 204")

### 1.3 Chat List & Conversation Threads
- **Messages & Headers**:
  - Messages sent by Teachers display a distinctive `[ FACULTY ✓ ]` verified badge with sky-blue/indigo highlight next to their name.
  - Student messages display their class badge (e.g., `Class 10-B`).

### 1.4 Directory / Contacts Roster
- Directory entries display role filter chips (`All`, `Faculty`, `Students`).
- Teacher contact cards feature an institutional graduation cap/shield icon, designation, and department details.

### 1.5 Admin Portal & Quarantined Enclave
- In the "Add User" dialog:
  - Admin selects **Student** or **Teacher**, with input fields tailored to the chosen role.
- In the **Quarantined Account Enclave**:
  - Displays whether the inspected user is a Faculty Member or Student.
  - Admin can update role attributes (Department, Employee ID, Grade, Section).

---

## 2. Technical Architecture & Data Schema Changes

### 2.1 Model Updates (`UserProfile.kt`)
Extend `UserProfile` with role attributes:
```kotlin
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val customUserId: String = "",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val role: String = "STUDENT",             // "STUDENT" or "TEACHER"
    val designation: String = "",             // e.g. "Senior PGT", "Faculty Member"
    val department: String = "",              // e.g. "Department of Mathematics"
    val employeeId: String = "",              // e.g. "DAV-FAC-102"
    val grade: String = "",                   // e.g. "10", "12"
    val section: String = "",                 // e.g. "A", "B"
    val subject: String = "",
    val description: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val hasCompletedOptionalSetup: Boolean = false,
    val associatedIps: List<String> = emptyList(),
    val currentIp: String = "",
    val createdAt: String = ""
) {
    val isTeacher: Boolean get() = role.equals("TEACHER", ignoreCase = true)

    val scholasticBadge: String
        get() = if (isTeacher) {
            if (designation.isNotBlank()) designation else "Faculty"
        } else {
            val g = grade.trim()
            val s = section.trim()
            when {
                g.isNotBlank() && s.isNotBlank() -> "Class $g-$s"
                g.isNotBlank() -> "Class $g"
                s.isNotBlank() -> "Sec $s"
                else -> "Student"
            }
        }
}
```

### 2.2 App Icon & Platform Synchronizations
1. **`metadata.json`**:
   - `name`: `"DAV InfoExchange"`
   - `description`: `"Official scholastic communication exchange for DAV institutions, supporting students and faculty."`
2. **`app/src/main/res/values/strings.xml`**:
   - `app_name`: `"DAV InfoExchange"`
   - `app_full_name`: `"DAV InfoExchange"`
   - `institution_name`: `"DAV College Managing Committee"`
3. **`settings.gradle.kts`**:
   - `rootProject.name = "DAV InfoExchange"`
4. **App Icon Asset Generation**:
   - Centered circular DAV College Managing Committee emblem with crisp white background as requested (`ic_launcher_background.xml` = `#FFFFFF`).
   - Standard 66dp safe-zone layer in `ic_launcher_foreground.xml`.
   - PNG conversions across all five density folders (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).

---

## 3. Step-by-Step Implementation Steps

1. **Step 1: Launcher Icon & Branding Assets**
   - Create official DAV emblem drawable asset `ic_dav_official_crest.png` / `jpg`.
   - Update `ic_launcher_background.xml` to `#FFFFFF` and `ic_launcher_foreground.xml` to reference the official crest.
   - Generate raster densities for `ic_launcher.png` and `ic_launcher_round.png`.

2. **Step 2: Platform Identity Update**
   - Update `metadata.json`, `strings.xml`, and `settings.gradle.kts`.

3. **Step 3: UserProfile Schema Extension**
   - Add `role`, `designation`, `department`, `employeeId`, and helper properties (`isTeacher`, `scholasticBadge`) to `UserProfile.kt`.
   - Update `AuthRepository.kt` to persist role and teacher attributes during registration.
   - Update `AdminRepository.kt` to support role and teacher attributes when creating/editing users.

4. **Step 4: Auth Gateway & Registration Screen**
   - Add **Student** vs **Teacher** role selection to `AuthGatewayScreen.kt`.
   - Dynamically adjust profile inputs based on the selected role.
   - Update branding text and logo to **DAV InfoExchange**.

5. **Step 5: Profile Setup & Editor Dialogs**
   - Update `OptionalProfileSetupSheet.kt` and `ProfileEditorDialog.kt` to present teacher fields (Department, Employee ID, Designation) when `role == "TEACHER"`, or student fields (Grade, Section) when `role == "STUDENT"`.

6. **Step 6: Chat List, Chat Item, & Directory Integration**
   - Display `[ FACULTY ✓ ]` verified badge beside Teacher names in `ChatItemRow.kt`, `ChatListScreen.kt`, and `ActiveContactsRow`.
   - Add role filter or distinction in `ContactsTabContent.kt`.

7. **Step 7: Admin Portal & Quarantined Enclave Updates**
   - Add role selector (Student/Teacher) to `AddUserDialog` in `AdminPortalScreen.kt`.
   - Show and allow updating role attributes in `QuarantinedAccountSheet.kt`.

8. **Step 8: Verification & Compilation**
   - Execute `compile_applet` to confirm zero compilation errors.
   - Verify proper responsive layout, edge-to-edge padding, and test tags.
