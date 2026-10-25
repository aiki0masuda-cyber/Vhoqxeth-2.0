# Fix Backblaze B2 S3 PUT 403 & Dynamic Model Failover Pipeline

Resolve the Backblaze B2 S3 PUT HTTP 403 Forbidden upload error by rewriting the Cloudflare Worker with verified AWS SigV4 signature calculation, implement a 3-attempt upload retry loop with local database fallback, diagnostic troubleshooting tips, and admin reporting, and introduce dynamic automatic model failover that auto-routes to `black-forest-labs/FLUX.1-schnell` with silent UI chip updates.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were selected based on your direct answers:
> - **Silent Dynamic Model Failover**: If any model throws "Model not supported", 404, or network unavailability, the engine seamlessly auto-routes to `black-forest-labs/FLUX.1-schnell` and silently updates the active selection chip in the UI.
> - **3-Attempt Upload Retry Loop**: Media uploads retry up to 3 times before falling back to persistent local device storage.
> - **Actionable Diagnostic Tips**: On 403 failure, the app explains the exact cause (Backblaze Application Key missing `writeFiles` permission or bucket name mismatch) with actionable resolution steps.
> - **Admin Reporting**: Upload failures are automatically logged into the Admin Diagnostics panel for centralized monitoring.
> - **Cloudflare Worker S3 Signature Fix**: Correct canonical headers (`host`, `content-type`, `x-amz-content-sha256`, `x-amz-date`), remove conflicting ACLs that trigger B2 403s, and support both direct worker proxy upload and pre-signed PUT URLs.

---

### 1. Proposed Changes

#### A. Cloudflare Worker Rewrite (`serverless/b2-presigned-worker.js`)
- Full AWS SigV4 implementation for Backblaze B2 S3 endpoint (`https://s3.<region>.backblaze.com`).
- Explicitly sign and append `content-type` (`image/png`, `image/jpeg`) matching incoming file blobs.
- Omit conflicting `x-amz-acl` headers that cause B2 private buckets to return HTTP 403 Forbidden.
- Add direct server-side upload action (`action=upload_direct`) as a resilient alternative to presigned PUTs.
- Embed explicit code documentation indicating where in the Backblaze B2 dashboard to ensure the Application Key has the `writeFiles`, `readFiles`, and `listFiles` permissions.

#### B. Dynamic Model Failover Engine (`app/src/main/java/com/example/ai/HuggingFaceImageEngine.kt` & `AiImageGeneratorEngine.kt`)
- Update `generateImage` to return `ImageGenerationOutput(val bitmap: Bitmap, val usedModel: HuggingFaceModel, val wasFallback: Boolean)`.
- If the requested model throws "Model not supported", 404, or 503 unavailable, immediately and transparently retry with `HuggingFaceModel.FLUX_1_SCHNELL`.
- Return the winning model so the calling UI can synchronize its state.

#### C. Dynamic UI Chip Updating (`app/src/main/java/com/example/ui/chat/AiGenerateImageDialog.kt` & `ConversationThreadSheet.kt`)
- In `AiGenerateImageDialog`: On successful generation output, silently update `selectedModel = result.usedModel` and `useCustomModel = false` so the UI chip shifts seamlessly to FLUX.1 Schnell without alerting the user with an error.
- In `ConversationThreadSheet`: Use the dynamic failover engine for in-chat `/imagine` commands.

#### D. B2 Upload Manager 3-Attempt Retry & Diagnostic Reporting (`app/src/main/java/com/example/data/B2UploadManager.kt`)
- Implement a 3-attempt exponential backoff retry loop for B2 uploads.
- If HTTP 403 occurs after 3 retries:
  - Deliver the chat message immediately using the local device storage URI (`cachedUri`), preventing chat bubble render interruption.
  - Record the diagnostic incident in `SessionManager` / Admin Diagnostics with specific resolution advice (*"Backblaze B2 403: Verify Application Key has 'writeFiles' permission in B2 Dashboard"*).
  - Display a helpful, non-blocking toast/notice with troubleshooting guidance.

#### E. Test Verification (`app/src/test/java/com/example/AiImageGeneratorEngineTest.kt`)
- Add unit tests verifying dynamic model failover logic and fallback to FLUX.1 Schnell.

---

### 2. Verification Plan

1. **Local JVM Tests**:
   - Run `gradle :app:testDebugUnitTest` to verify dynamic failover and upload fallback tests.
2. **Applet Compilation**:
   - Run `compile_applet` to confirm clean compilation and zero build errors.
