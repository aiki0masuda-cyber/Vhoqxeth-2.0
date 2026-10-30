# High-Contrast Light Theme & Chat Message Controls (Reply, Forward, Delete)

Fix all low-contrast/invisible text in the light UI theme by correcting font and container color mappings, and implement complete native message controls—replying with quoted message headers, forwarding to other chat threads with a "Forwarded" badge, and deleting messages either for everyone or for oneself via a long-press action menu.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions have been aligned based on your clarifying answers:
> - **Deletion Scope**: Messages can be deleted either for **Everyone** (tombstoned for all participants) or **For Me** (hidden exclusively on your device/account).
> - **Action Trigger**: Actions are triggered via **long-press** on any chat message bubble to display a clean Material 3 modal sheet with Reply, Forward, Copy, and Delete actions.
> - **Forwarded Presentation**: Forwarded messages in recipient chats feature a dedicated **Forwarded** badge icon above the message body while preserving content and attachments.

---

## 1. Overview & Core Concept

1. **Light UI Contrast Fix**:
   - Resolve the issue where input fields and dialog text were invisible due to legacy dark-theme tokens (such as `VhoqxethSlate900` being mapped to an elevated light surface `#F1F5F9` instead of dark slate `#0F172A`).
   - Standardize all text inputs, dialog labels, profile editors, and message elements to high-contrast deep slate (`#0F172A`) for primary text, slate-600 (`#475569`) for secondary text, and slate-400/500 (`#64748B`) for placeholders.

2. **Native Message Controls**:
   - **Long-Press Menu**: Long-pressing on any message bubble opens a bottom sheet offering:
     - **↩ Reply**: Pins a quoted preview bar above the composer with original sender and text snippet.
     - **↗ Forward**: Opens a dialog to pick any active chat channel or direct contact, delivering the message with a "Forwarded" indicator.
     - **⎘ Copy Text**: Copies text content to Android clipboard with a confirmation snackbar/toast.
     - **🗑 Delete**: Presents options to "Delete for everyone" (if sender or admin) or "Delete for me".

---

## 2. User Experience & Visual Design

### Long-Press Action Sheet & Reply Workflow
```
┌────────────────────────────────────────────────────────┐
│ Message Bubble (Long-Pressed)                          │
│ "Please review chapter 4 for tomorrow's quiz."         │
└────────────────────────────────────────────────────────┘
                           │ (Opens Action Sheet)
                           ▼
┌────────────────────────────────────────────────────────┐
│  Message Actions                                       │
│  ───────────────────────────────────────────────────   │
│  [↩ Reply]      Quote message in composer              │
│  [↗ Forward]    Send to another circle or contact      │
│  [⎘ Copy]       Copy message text to clipboard         │
│  [🗑 Delete]     Delete for everyone or for myself      │
└────────────────────────────────────────────────────────┘
```

### Quoted Reply in Composer
```
┌────────────────────────────────────────────────────────┐
│  ↩ Replying to Rohan Sharma                        [✕] │
│  "Please review chapter 4 for tomorrow's quiz."        │
├────────────────────────────────────────────────────────┤
│ [📎]  Type a message...                           [Send│
└────────────────────────────────────────────────────────┘
```

### Forwarded Message in Destination Chat
```
┌──────────────────────────────────────────────┐
│  ↗ Forwarded                                 │
│  Please review chapter 4 for tomorrow's quiz.│
│                                      10:24 AM│
└──────────────────────────────────────────────┘
```

---

## 3. Key Technical Architecture & Strategy

```
┌─────────────────────────────────────────────────────────────────────────┐
│ Jetpack Compose UI Layer                                                │
│                                                                         │
│  • ConversationThreadSheet.kt                                           │
│    - combinedClickable / onLongClick on MessageBubble                   │
│    - MessageActionBottomSheet (Reply, Forward, Copy, Delete)            │
│    - Active QuotedReplyPreviewBar above input composer                  │
│    - ForwardThreadPickerDialog (Select destination circle/contact)      │
│                                                                         │
│  • Color.kt & Theme.kt                                                  │
│    - Fix VhoqxethSlate900 -> DavTextPrimary (#0F172A)                   │
│    - Audit OutlinedTextField text & placeholder colors in all dialogs   │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│ Data & Firestore Layer                                                  │
│                                                                         │
│  • ChatMessage.kt                                                       │
│    - replyToMessageId, replyToSenderName, replyToContent                │
│    - isForwarded: Boolean                                               │
│    - deletedForEveryone: Boolean, deletedForUserIds: List<String>       │
│                                                                         │
│  • MeshChatRepository.kt                                                │
│    - sendMessage(..., replyTo, isForwarded)                             │
│    - deleteMessage(threadId, messageId, deleteForEveryone, userHandle)  │
│    - forwardMessage(targetThreadId, senderProfile, message)             │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Implementation Steps

1. **Fix Color Tokens & Contrast**:
   - In `Color.kt`, redefine `VhoqxethSlate900` to `DavTextPrimary` (`#0F172A`).
   - Audit text fields in `OptionalProfileSetupSheet.kt`, `ProfileEditorDialog.kt`, `ChangePasswordDialog.kt`, `AddContactDialog.kt`, and `CreateStudyCircleDialog.kt` to ensure dark text on light containers.

2. **Update `ChatMessage` Model**:
   - Add reply fields (`replyToMessageId`, `replyToSenderName`, `replyToContent`).
   - Add forward field (`isForwarded: Boolean = false`).
   - Add delete fields (`deletedForEveryone: Boolean = false`, `deletedForUserIds: List<String> = emptyList()`).
   - Add helper `fun isDeletedFor(userHandle: String): Boolean`.

3. **Extend `MeshChatRepository`**:
   - Support reply metadata and `isForwarded` flag in `sendMessage`.
   - Implement `deleteMessage(threadId, messageId, deleteForEveryone, userHandle)` in Firestore.
   - Implement `forwardMessage(targetThreadId, senderProfile, originalMessage)`.
   - Update `observeMessages` to filter out messages where `isDeletedFor(currentUserId)` is true, or display tombstone if deleted for everyone.

4. **Integrate Chat UI**:
   - In `ConversationThreadSheet.kt`, attach `Modifier.combinedClickable(onLongClick = ...)` to message cards.
   - Build `MessageActionsBottomSheet` with Reply, Forward, Copy, and Delete actions.
   - Build `ForwardMessageDialog` listing recent circles and contacts.
   - Render quoted reply cards in messages and in the composer input bar.
   - Render the `Forwarded` badge above forwarded messages.

---

## 5. Verification Plan

1. **Compilation**: Run `compile_applet` to verify build integrity.
2. **Contrast & Theme Testing**:
   - Check profile edit fields, password change dialogs, and text inputs to ensure text is dark, crisp, and 100% visible.
3. **Message Controls Testing**:
   - Long-press a message and verify the action bottom sheet appears.
   - Test **Reply**: Composer shows quoted bar, and sent message shows quoted bubble header.
   - Test **Forward**: Choose destination thread and verify message arrives with "Forwarded" badge.
   - Test **Delete for Myself**: Message disappears from local user view.
   - Test **Delete for Everyone**: Message reflects deletion tombstone for all participants.
4. **Unit Tests**: Run `gradle :app:testDebugUnitTest` to verify data models and repository methods.
