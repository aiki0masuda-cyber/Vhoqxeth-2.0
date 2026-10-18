# DAV InfoExchange — Group Collaboration, Universal File Attachments & Clean Scholastic Light Theme

Transform **DAV InfoExchange** into a collaborative scholastic exchange by enabling any user to create groups, select and add contacts to existing or new groups, send all file attachment types via the Android system document picker, and switch the application visual design to a clean scholastic **Light Theme** with pure white, royal blue, and slate accents.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user-confirmed specifications guide this implementation:
> 1. **Group Creation & Member Management**: Any user (Student or Teacher) can create groups and add existing contacts directly during creation or at any time from group details.
> 2. **File Attachments**: Supported for all file types (PDFs, study notes, images, documents, audio, spreadsheets, archives) using the zero-permission Android system document picker (`ActivityResultContracts.OpenDocument`).
> 3. **Clean Scholastic Light Theme**: Default posture set to crisp scholastic white (`#FFFFFF` / `#F8FAFC`), deep navy text (`#0F172A`), royal blue brand accent (`#1E2A78`), sky blue highlights (`#0284C7`), and clean slate borders (`#E2E8F0`).

---

## 1. Overview & Core Concept

- **What It Does**:
  - **Group Creation**: Users can create study circles, subject channels, or faculty groups, assigning a name, category, and inviting contacts with interactive multi-select chips.
  - **Add Contacts to Existing Groups**: Group threads feature an "Add Members" action allowing group participants to seamlessly enroll peers from their contacts list into the group.
  - **Universal File Attachments**: Users can tap an attachment button in any direct or group conversation to attach any document, assignment, image, or study file. File bubbles display filename, size, extension tag, and open/view actions.
  - **Crisp Scholastic Light Theme**: The dark interface is replaced with an airy, academic aesthetic matching official DAV institutions.

- **Target Audience**:
  - Students sharing homework, study circle notes, and project documents.
  - Teachers organizing department groups, broadcasting announcements, and distributing PDFs.

---

## 2. User Experience & Visual Design

### 2.1 Clean Scholastic Light Theme Palette
- **Background / Canvas**: `#F8FAFC` (Slate 50) and `#FFFFFF` (Pure Scholastic White).
- **Surface Cards**: `#FFFFFF` with crisp `#E2E8F0` (Slate 200) borders and soft elevation.
- **Brand Primary**: Official Royal Blue `#1E2A78` (representing DAV institutional heritage).
- **Secondary / Action Accent**: Sky Cyan `#0284C7` (for verified badges, active tabs, and primary action buttons).
- **Text Hierarchy**:
  - Primary Headlines & Text: Deep Slate/Navy `#0F172A`.
  - Secondary Text & Meta: Muted Slate `#475569`.
  - Captions & Timestamps: `#64748B`.
- **Chat Bubbles**:
  - User's Sent Messages: Royal Blue gradient (`#1E2A78` → `#2563EB`) with crisp white text.
  - Received Messages: Crisp White card (`#FFFFFF`) with `#E2E8F0` border and dark text (`#0F172A`).

### 2.2 Group Creation & Member Management Flow
1. **Fab / Header Action in Chats Tab**: A "+ Group" or "New Study Circle" action button.
2. **Create Group Dialog**:
   - Group Title & Description.
   - Group Type / Academic Category (Study Circle, Class Group, Subject Project, Faculty Room).
   - "Select Contacts to Add": A scrollable checklist/chip row of the user's added contacts with quick search.
3. **Group Header & Participant Enclave**:
   - In `ConversationThreadSheet`, group threads display participant avatars and member count.
   - An "Add Contacts" icon opens a fast selector to add more contacts to the group in real time.

### 2.3 Universal File Attachments Flow
1. **Chat Input Bar**: A paperclip icon (`Icons.Default.AttachFile`) next to the text input.
2. **Picker Launch**: Invokes `ActivityResultContracts.OpenDocument()` with MIME type `*/*`.
3. **Preview & Confirmation**:
   - Shows a pending attachment preview badge (icon, filename, filesize) above the input bar before sending.
   - User can type an accompanying caption or send the file directly.
4. **Chat Bubble Presentation**:
   - File bubble card with category-coded badge (PDF, DOCX, IMG, ZIP).
   - Display of file title, human-readable size (e.g. `2.4 MB`), and timestamp.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: File Storage Strategy (Android Content Provider + Metadata)**
  - *Chosen Approach*: Files picked via `OpenDocument` persist persistent read permission via `takePersistableUriPermission` and store metadata (URI string, filename, size, MIME type) in the Firestore message document.
  - *Why*: Allows zero-permission document picking across all Android versions without requiring risky external storage permissions, providing instant sharing of real files.
- **Decision 2: Light Theme Architecture & Contrast Compliance**
  - *Chosen Approach*: Update `Theme.kt` with a tailored `LightColorScheme` where `isAppearanceLightStatusBars = true` and `isAppearanceLightNavigationBars = true`.
  - *Why*: Ensures optimal readability and eliminates all dark-mode eye strain while preserving the DAV institutional branding.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        DAV InfoExchange Light Theme                    │
├──────────────────────────┬─────────────────────────────────────────────┤
│   ChatListScreen         │   ConversationThreadSheet                   │
│   ├── Active Groups Filter│   ├── Message List (Text & File Cards)      │
│   ├── Create Group Dialog│   ├── Group Members Header + "Add Contacts" │
│   └── Contact Pickers    │   └── Input Bar + System Document Picker    │
└────────────┬─────────────┴──────────────────────┬──────────────────────┘
             │                                    │
             ▼                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        MeshChatRepository                              │
│   ├── createGroup(name, category, participants)                       │
│   ├── addMemberToGroup(threadId, newParticipantHandle)                │
│   ├── sendFileMessage(threadId, sender, content, attachmentMeta)      │
│   └── observeThreads() & observeMessages()                             │
└────────────────────────────────────────────────────────────────────────┘
```

### 4.1 Data Schema Extensions

#### `ChatMessage` Model
```kotlin
data class ChatMessage(
    val messageId: String = "",
    val threadId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "STUDENT",
    val content: String = "",
    val timestamp: String = "",
    val attachmentUri: String? = null,
    val attachmentName: String? = null,
    val attachmentType: String? = null,
    val attachmentSize: String? = null
)
```

#### `ConversationThread` Model
```kotlin
data class ConversationThread(
    val id: String = "",
    val name: String = "",
    val isGroup: Boolean = false,
    val participantIds: List<String> = emptyList(),
    val groupCategory: String = "STUDY_CIRCLE",
    val lastMessage: String = "",
    ...
)
```

---

## 5. Step-by-Step Implementation Sequence

1. **Light Theme Overhaul**:
   - Update `Color.kt` and `Theme.kt` to default to the clean scholastic white, royal blue, and slate palette.
   - Update `MainActivity.kt`, `ChatListScreen.kt`, `ContactsTabContent.kt`, `ConversationThreadSheet.kt`, and `AdminPortalScreen.kt` to clean light surfaces and high-contrast dark text.
2. **File Attachments Integration**:
   - Extend `ChatMessage.kt` with attachment properties (`attachmentUri`, `attachmentName`, `attachmentType`, `attachmentSize`).
   - Add `sendFileMessage` method in `MeshChatRepository.kt`.
   - Add attachment launcher and file card composables in `ConversationThreadSheet.kt`.
3. **Group Creation & Adding Contacts**:
   - Enhance group creation dialog with multi-select contact picker chips.
   - Add `addMemberToGroup` method in `MeshChatRepository.kt`.
   - Add "Add Members" action and dialog inside `ConversationThreadSheet.kt` for existing groups.
4. **Build & Compilation Verification**:
   - Run `compile_applet` to ensure zero compilation or syntax errors.
