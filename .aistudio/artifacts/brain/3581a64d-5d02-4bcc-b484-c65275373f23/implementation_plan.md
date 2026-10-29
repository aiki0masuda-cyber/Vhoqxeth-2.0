# Implementation Plan - Voice Messaging, Factory Database Reset & Minimalist Light Admin UI

Replace the SiliconFlow AI text/image generation pipelines with a dedicated audio voice messaging system, provide a complete factory database reset action in the Admin portal, and redesign the entire Admin UI into a clean, minimalist light-themed layout.

---

## User Requirements & Preferences
1. **Voice Messaging**:
   - Completely remove the AI image and text generation tools (dialogs, slash commands, engines, and buttons).
   - Add a voice message option in chat (record audio and send voice notes).
   - Voice player UI appears only on messages that are voice notes ("not every message contains it, only if needed").
2. **Admin Database Reset**:
   - Add a full factory reset button in the Admin portal that clears all messages, custom channels, and users (with confirmation dialog and status indication).
3. **Admin UI Redesign**:
   - Redesign the entire Admin interface with a minimalist, crisp white card aesthetic, soft slate borders, subtle elevation, and clean typography.

---

## Proposed Changes

### 1. Chat Message Model & Voice Audio Infrastructure
#### `app/src/main/java/com/example/model/ChatMessage.kt`
- Add `voiceDurationSeconds: Int = 0` to `ChatMessage` (with Firestore default).
- Support `attachmentType == "VOICE"` for audio notes.
- Include helper properties `isVoiceMessage: Boolean get() = attachmentType == "VOICE" || ...`.

#### `app/src/main/java/com/example/util/AudioVoiceManager.kt` *(New)*
- Manage native audio recording via `MediaRecorder` (`MPEG_4` / `AAC`) to temporary cache files.
- Track recording elapsed duration with timer flow.
- Manage native audio playback via `MediaPlayer` with play/pause state and playback progress.
- Clean up resources gracefully on disposal.

### 2. Chat UI: Voice Messaging & Removal of AI Generation
#### `app/src/main/java/com/example/ui/chat/ConversationThreadSheet.kt` & `ChatItemRow.kt`
- Remove all AI generation buttons, dialog triggers (`AiGenerateImageDialog`), and `/ai` or `/imagine` slash command handling.
- In the chat bottom bar:
  - Add audio recording button / recorder state (record timer, cancel, send).
  - Upload recorded `.m4a` voice file via `CatboxUploadManager` (or existing upload pipeline) and post as a voice chat message.
- In chat message bubbles (`ChatItemRow.kt` / `ConversationThreadSheet.kt`):
  - Render an interactive voice message player for messages with `attachmentType == "VOICE"` (play/pause toggle, audio waveform bars, progress indicator, and timestamp duration).
  - Regular text and file messages render cleanly without any voice widgets.

#### AI File Cleanup / Deprecation
- Remove or bypass `AiGenerateImageDialog.kt`, and remove AI generation calls from chat view models.

### 3. Factory Database Reset in Admin Repository
#### `app/src/main/java/com/example/data/AdminRepository.kt` & `MeshChatRepository.kt`
- Add `factoryResetDatabase()`:
  - Deletes all messages across all conversation threads in `/threads/{threadId}/messages`.
  - Clears all `/threads` documents and resets default channels (`general`, `announcements`, `study-circles`).
  - Clears all custom `/users` documents, re-seeding default administrative/faculty accounts.
  - Clears registered IP entries in `/registered_ips`.
  - Clears local cache/state to ensure instant UI sync.

### 4. Admin Portal Screen Redesign: Minimalist Light Theme
#### `app/src/main/java/com/example/ui/admin/AdminPortalScreen.kt`
- Redesign from dark theme to an elegant, minimalist light theme:
  - Background: Crisp off-white (`#F8FAFC`).
  - Cards: Pure white (`#FFFFFF`) with subtle slate borders (`#E2E8F0`), 12dp rounded corners, and soft shadows.
  - Text & Icons: Deep slate primary (`#0F172A`), muted secondary (`#64748B`), royal blue brand accent (`#2563EB`).
  - Header & Tabs: Minimal tab row with pill indicators for Users, Network/IPs, Self-Healing, and System Maintenance.
- Add a prominent **Database Maintenance & Factory Reset** section:
  - "Reset Database to Factory Defaults" card with a warning badge, two-step confirmation dialog ("Type RESET or confirm"), and clear progress indicators.
  - Quick action buttons to wipe message threads, reset IP mappings, and restore defaults.
- Retain all core administrative features (user management, role toggles, IP whitelisting, self-healing logs) within the clean, uncluttered light layout.

---

## Verification Plan

### Automated Tests
- Run unit tests: `gradle :app:testDebugUnitTest`.
- Update or adapt unit tests in `app/src/test/` to reflect removal of AI generation and addition of voice message model properties and factory reset methods.

### Manual / Integration Verification Steps
1. **Compilation**: Run `compile_applet` to verify clean build without deprecation errors or missing imports.
2. **Chat & Voice Message**:
   - Verify the AI sparkle button and generation dialogs are completely gone.
   - Record a voice message in chat, send it, and verify the audio note card appears with duration and play button.
   - Verify regular text messages display normally without voice UI components.
3. **Admin Portal**:
   - Open Admin Portal and verify the clean, minimalist white card styling and slate borders.
   - Tap "Factory Reset Database", confirm the dialog, and verify threads/messages/users reset cleanly.
