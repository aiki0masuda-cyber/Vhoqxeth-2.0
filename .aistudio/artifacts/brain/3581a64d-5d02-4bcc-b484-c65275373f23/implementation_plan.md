# Sync Real Chats & Contacts with User ID Discovery

Transform Vhoqxeth from static sample mock data into a live, real-time Firestore-synchronized messaging network. New accounts start with a pristine clean state (zero placeholder contacts or threads), provide a peer discovery modal to add contacts by User ID, and synchronize two-way end-to-end conversation threads directly via Firestore snapshot listeners.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product decisions were confirmed during interactive consultation:
> - **Peer Addition**: Modal dialog with real-time `@customUserId` search, instant verification badge, and one-tap "Add Contact".
> - **First-Launch Zero State**: Clean empty state card featuring an inviting mesh communication graphic and an "Add Contact" call-to-action button.
> - **Database Structure**: Real-time two-way shared threads under `/threads/{threadId}` with deterministic participant pairing (`thread_${min(u1,u2)}_${max(u1,u2)}`) and subcollection `/messages`, accompanied by personal contact rosters under `/users/{userId}/contacts`.
> - **Sample Data Elimination**: Complete removal of `DummyData.placeholderThreads` and `DummyData.activeContacts` from user account views.

---

## 1. Overview & Core Concept

- **What It Does**: Replaces static, hardcoded sample chats and contacts with dynamic, cloud-backed records. When an account is registered or logged in, their contacts and conversation history are pulled directly from Firestore in real time. Users can discover and add other registered users using their unique User ID (`customUserId`), immediately creating an active two-way thread.
- **Target Audience / Persona**: Users communicating over the encrypted mesh who expect their personal address book and conversations to persist, sync across devices, and contain exclusively authentic user interactions.
- **Key Value**: Delivers complete data authenticity, eliminates deceptive sample data, and provides an effortless peer addition experience with instant two-way sync.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **New User Account / Clean Zero State**:
   - On initial login, the user sees an empty conversation dashboard with zero sample chats.
   - An elegant centered card features an indigo connection glyph, headline *"No mesh conversations yet"*, descriptive guidance *"Search for registered peers by their User ID to begin exchanging encrypted messages"*, and an **Add Contact** primary button.
2. **Add Contact via User ID Modal**:
   - Tapping the FAB or the empty state button opens the **Add Contact Dialog** (`add_contact_dialog`).
   - The user enters a handle (e.g. `alice` or `@alice`).
   - The system performs a debounced Firestore lookup against `/users` collection:
     - **Found**: Displays a contact preview card with avatar initials, display name, verified `@customUserId`, and an **Add to Contacts** button.
     - **Not Found**: Displays an informative *"No registered user found with ID @handle"* status.
     - **Self-addition**: Prevents adding own User ID with friendly feedback.
3. **Active Contact & Conversation Row**:
   - Adding a contact stores it in `/users/{currentUserId}/contacts` and automatically starts or opens the deterministic two-way thread.
   - Contacts appear in the top horizontal roster with their authentic display names and status indicators.
   - Chats appear in the thread list sorted by `updatedAt` with live last-message snippets and unread counters.
4. **Interactive Message Exchange**:
   - Tapping any conversation thread opens the live message viewer with real-time Firestore message streams.
   - Sending a message writes to `/threads/{threadId}/messages` and updates `/threads/{threadId}` metadata, instantly updating both participants' devices.

### Visual Identity & Theme Alignment
- **Empty State Card**: Crisp white surface (`#FFFFFF`) with soft warm border (`#E2E8F0`), deep indigo icon badge, and slate typography.
- **Add Contact Dialog**: Clean white container (`#FFFFFF`), rounded corners (16.dp), clear search text field with slate container (`#F8FAFC`), and vibrant deep indigo action buttons.
- **Contact Chips**: Minimalist circle avatar with indigo-to-cobalt gradient and bold initials.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Deterministic Thread IDs for Two-Way Messaging**:
  - *Chosen Approach*: `threadId = "thread_" + listOf(userId1, userId2).sorted().joinToString("_")`.
  - *Why*: Guarantees that whether User A messages User B or User B messages User A, both participants read and write to the exact same Firestore thread without race conditions or duplicate thread documents.
  - *Alternatives Considered*: Independent copies per user (causes desynchronization and double-writes).

- **Decision 2: Subcollection Messages with Root Threads**:
  - *Chosen Approach*: `/threads/{threadId}` holds participants and snippet preview; `/threads/{threadId}/messages/{messageId}` holds individual encrypted messages.
  - *Why*: Allows listening to lightweight thread metadata in the main list without loading thousands of historical message payloads into memory.

- **Decision 3: Complete Removal of Sample Mock Data**:
  - *Chosen Approach*: Clean deprecation of `DummyData.placeholderThreads` and `DummyData.activeContacts` from the UI.
  - *Why*: Directly satisfies the user's explicit requirement: "Clear all chat and contacts on new user account and sync chats and contacts on database and remove the semple contacts".

---

## 4. Technical Architecture & Data Strategy

### System Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                       ChatListScreen                        │
│  ┌───────────────────────┐       ┌───────────────────────┐  │
│  │   ActiveContactsRow   │       │   FilteredThreadList  │  │
│  │ (Live from Firestore) │       │ (Live from Firestore) │  │
│  └───────────┬───────────┘       └───────────┬───────────┘  │
│              │                               │              │
│              └───────────────┬───────────────┘              │
│                              ▼                              │
│              ┌───────────────────────────────┐              │
│              │     AddContactDialog (Modal)  │              │
│              │   - Search by customUserId    │              │
│              │   - Verify in /users          │              │
│              └───────────────┬───────────────┘              │
└──────────────────────────────┼──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    MeshChatRepository                       │
│  ├─ observeContacts(userId): Flow<List<UserContact>>        │
│  ├─ observeThreads(userId): Flow<List<ChatThread>>          │
│  ├─ searchUserByHandle(handle): Result<UserProfile?>        │
│  ├─ addContact(userId, targetProfile): Result<Unit>         │
│  └─ sendMessage(threadId, sender, content): Result<Unit>    │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      Firestore Backend                      │
│  ├─ /users/{userId}/contacts/{contactId}                    │
│  ├─ /threads/{threadId} (participants: [u1, u2])            │
│  └─ /threads/{threadId}/messages/{messageId}                │
└─────────────────────────────────────────────────────────────┘
```

### Data Schema Updates (`firebase-blueprint.json` & `firestore.rules`)

1. **`UserContact` Entity**:
   - `contactUserId`: String
   - `displayName`: String
   - `currentIp`: String
   - `addedAt`: String
2. **`ChatThread` Entity**:
   - `threadId`: String
   - `participantIds`: Array of String
   - `participantNames`: Map of String -> String
   - `lastMessage`: String
   - `lastSenderId`: String
   - `updatedAt`: String
3. **`ChatMessage` Entity**:
   - `messageId`: String
   - `threadId`: String
   - `senderId`: String
   - `senderName`: String
   - `content`: String
   - `timestamp`: String

### Security Rules (`firestore.rules`)
- `/users/{userId}/contacts/{contactId}`: Allow read/write for contact entries.
- `/threads/{threadId}`: Allow read/write for thread participants.
- `/threads/{threadId}/messages/{messageId}`: Allow read/write for thread participants.

---

## 5. Implementation Phases

1. **Phase 1: Database Blueprint & Rules**:
   - Add `UserContact`, `ChatThread`, and `ChatMessage` to `firebase-blueprint.json`.
   - Update `firestore.rules` and test against local emulator.
2. **Phase 2: Repository & Data Models**:
   - Create `UserContact.kt` and `ChatMessage.kt`.
   - Create `MeshChatRepository.kt` with coroutine Flow snapshot listeners for contacts, threads, and peer search.
3. **Phase 3: Add Contact Modal Dialog**:
   - Create `AddContactDialog.kt` supporting debounce handle query, user profile preview, and instant add.
4. **Phase 4: ChatListScreen & Clean Empty State Integration**:
   - Replace `DummyData` with real-time `MeshChatRepository` flows.
   - Build `EmptyChatState` card with "Add Contact" trigger.
   - Wire live threads into conversation viewer with real message sending.
5. **Phase 5: Verification & Testing**:
   - Run unit and Robolectric tests (`gradle :app:testDebugUnitTest`).
   - Validate full application build (`compile_applet`).
