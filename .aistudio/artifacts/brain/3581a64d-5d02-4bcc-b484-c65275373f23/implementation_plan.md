# Private Backblaze B2 Pre-Signed Download Architecture

A secure, zero-cost architecture updating the Cloudflare Worker and Android client to support Private Backblaze B2 buckets using 24-hour pre-signed GET download URLs, object key persistence, in-memory token caching, and automated UI token refreshes.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were aligned based on your confirmed specifications:
> - **Storage Key Tracking**: The unique S3 object key (`chat-attachments/timestamp_filename`) is stored directly in `ChatMessage` as `attachmentKey: String?` alongside Firestore records.
> - **In-Memory URL Cache**: An in-memory cache (`ConcurrentHashMap<String, CachedSignedUrl>`) stores refreshed URLs with expiration timestamps, preventing redundant network requests.
> - **Automatic UI Refresh**: Media bubbles proactively verify token validity when appearing on screen; if expired or within a safety buffer (e.g. < 5 minutes remaining), they silently request a fresh 24-hour GET link.

---

### 1. Overview & Core Concept

- **What It Does**: Upgrades the file attachment infrastructure from public bucket assumptions to private bucket security. Since private Backblaze B2 buckets deny unauthorized GET requests, the serverless edge worker provides both pre-signed PUT URLs for upload and 24-hour pre-signed GET URLs for downloading/viewing. The Android app stores the object key, caches active signed download links in memory, and automatically refreshes them when displaying images, PDFs, or videos in chat bubbles.
- **Target Audience / Persona**: Students and educators sharing confidential homework files, study circle recordings, and media without risking public exposure or incurring paid public bucket fees on Backblaze B2.
- **Key Value**: 100% compliance with Backblaze B2 free private bucket policies, zero public URL exposure, automatic renewal of expired links, and zero flicker during chat rendering through smart in-memory caching.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Upload & Initial Link**: User attaches an image/video/PDF; the worker returns `{ "uploadUrl": "...", "temporaryDownloadUrl": "...", "key": "..." }`. The message is saved to Firestore with `attachmentKey = key`.
  2. **Viewing Fresh Messages**: On screen load, the app checks if `temporaryDownloadUrl` is valid; if so, it renders immediately via Coil (`AsyncImage`) or system document viewer.
  3. **Viewing Old Messages (>24 hours)**: When an older message scrolls into view, the bubble detects that the 24-hour signature has expired or that only the storage key is present.
  4. **Silent In-Line Refresh**: A subtle pulsing placeholder or thumbnail shimmer briefly appears while a fresh 24-hour signed GET link is fetched from `https://workers.dev/?action=download&key=...`.
  5. **Instant Cache Resolution**: Once fetched, the URL is stored in the client cache so scrolling up and down requires zero repeat network calls.

- **Visual Identity & Theme**:
  - Consistent with **DAV InfoExchange** design tokens (`DavBrandRoyalBlue` #1E3A8A, `DavAccentSky` #0284C7, `DavSurfaceElevated` #F1F5F9).
  - Media cards display a subtle lock badge indicating end-to-end encrypted S3 bucket access.
  - Refresh transitions use smooth crossfade animations to prevent layout jumps.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual-Action Serverless Edge Routing (`action=upload` vs `action=download`)**
  - *Chosen Approach*: The Cloudflare Worker handles `action=upload` (issuing 5-min PUT URL + initial 24-hour GET URL) and `action=download` (issuing a fresh 24-hour GET URL using `GetObjectCommand` and `getSignedUrl`).
  - *Why*: Consolidates all S3 signing logic into a single zero-maintenance edge worker.
  - *Alternatives Considered*: Separate workers for upload and download (rejected to avoid managing multiple deployment endpoints).

- **Decision 2: 24-Hour Expiration with 10-Minute Safety Window**
  - *Chosen Approach*: Issue download URLs with `expiresIn: 86400` (24 hours). The Android client treats URLs as expired if remaining time is under 600 seconds.
  - *Why*: Eliminates edge-case race conditions where an image expires mid-download or during video streaming.

- **Decision 3: Object Key Storage in Firestore Document**
  - *Chosen Approach*: Save `attachmentKey: String?` in Firestore `threads/{threadId}/messages/{messageId}` alongside `attachmentUrl`.
  - *Why*: Guarantees that any participant in a group chat can re-request a valid download signature even months after the message was sent.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose Chat UI                         │
│  MessageBubble ──> Checks in-memory cache for valid signed URL         │
└───────────────────┬───────────────────────────────────▲────────────────┘
                    │ Expired or Missing                │ Fresh 24-hour
                    ▼                                   │ Signed GET URL
┌───────────────────────────────────────────────────────┴────────────────┐
│               B2UploadManager (Download URL Cache)                     │
│  - Checks ConcurrentHashMap<Key, CachedSignedUrl>                      │
│  - If expired: Calls Worker with action=download&key=...               │
└──────────────┬────────────────────────────────┬────────────────────────┘
               │ GET /?action=download&key=...  │
               ▼                                │
┌──────────────────────────────┐                │
│  Cloudflare Worker Edge      │                │
│  - Validates key             │                │
│  - Generates GetObjectCommand│                │
│  - Returns 24h Signed URL    │                │
└──────────────┬───────────────┘                │
               │                                │
               ▼ 24-Hour Pre-signed GET URL     │
┌───────────────────────────────────────────────┴────────────────────────┐
│                      Backblaze B2 Private Bucket                       │
│  Streams image / PDF / video binary securely to Coil / Android Player  │
└────────────────────────────────────────────────────────────────────────┘
```

#### Detailed Component Plan:

1. **Serverless Edge Script (`serverless/b2-presigned-worker.js`)**:
   - `action=upload` (default): Generates 5-min PUT URL + 24-hour GET URL. Returns:
     ```json
     {
       "uploadUrl": "https://s3.../chat-attachments/...?X-Amz-...",
       "temporaryDownloadUrl": "https://s3.../chat-attachments/...?X-Amz-...",
       "key": "chat-attachments/1713681200000_sample.png",
       "expiresInSeconds": 86400
     }
     ```
   - `action=download`: Accepts `key` or `filename`. Uses `GetObjectCommand` + `getSignedUrl(s3Client, getCommand, { expiresIn: 86400 })`. Returns:
     ```json
     {
       "temporaryDownloadUrl": "https://s3.../chat-attachments/...?X-Amz-...",
       "key": "chat-attachments/1713681200000_sample.png",
       "expiresInSeconds": 86400
     }
     ```

2. **Data Model Updates (`ChatMessage.kt`)**:
   - Add `val attachmentKey: String? = null` to represent the private S3 key.
   - Helper function `isAttachmentExpired(currentTimeMillis: Long): Boolean`.

3. **Client Network Manager (`B2UploadManager.kt`)**:
   - Cache model: `data class CachedSignedUrl(val url: String, val expiresAtMillis: Long)`.
   - `suspend fun getOrRefreshDownloadUrl(key: String, initialUrl: String? = null, workerEndpoint: String): String`.
   - `suspend fun fetchTemporaryDownloadUrl(key: String, workerEndpoint: String): Result<String>`.

4. **UI Integration (`ConversationThreadSheet.kt` & `MessageBubble`)**:
   - In `MessageBubble`, replace raw URI rendering with a reactive state that resolves via `b2UploadManager.getOrRefreshDownloadUrl`.
   - Renders `AsyncImage` with crossfade and loading placeholder for images.
   - Provides tap-to-open action for PDFs and videos with freshly signed URLs.
