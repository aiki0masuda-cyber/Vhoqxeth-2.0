# Transition to Catbox.moe Storage & SiliconFlow Image Engine

Replace Backblaze B2 entirely with a streamlined keyless **Catbox.moe** upload gateway for all chat file attachments (images, screenshots, diagrams, media blobs), and transition the image generation infrastructure from Hugging Face to **SiliconFlow** (`https://api.siliconflow.cn/v1/images/generations`).

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were selected based on your direct answers:
> - **Exclusively Use Catbox.moe**: Remove Backblaze B2 entirely. Route all chat attachments (photos, diagrams, AI artwork, files) directly to `https://catbox.moe/user/api.php` via multipart `reqtype=fileupload` without any auth headers or secret keys.
> - **200MB File Size & Error Mitigation**: On timeout, 502 Bad Gateway, or size errors, alert the user with the exact message: *"Upload failed. Please ensure the file size is under 200MB."*, while smoothly preserving the local device cache URI to ensure active chat message bubbles render without interruption.
> - **Prepare SiliconFlow Image Generation**: Remove legacy Hugging Face Inference Router references and implement `SiliconFlowImageEngine` targeting SiliconFlow's high-speed API (`https://api.siliconflow.cn/v1/images/generations`) with `black-forest-labs/FLUX.1-schnell` as the primary diffusion model, ready for your SiliconFlow key.

---

### 1. Proposed Changes

#### A. Catbox.moe Keyless Upload Manager (`app/src/main/java/com/example/data/CatboxUploadManager.kt`)
- Create `CatboxUploadManager`:
  - Target URL: `https://catbox.moe/user/api.php`.
  - Multipart form-data body:
    - `"reqtype"` = `"fileupload"`
    - `"fileToUpload"` = streaming binary RequestBody from content resolver or local file.
  - No authorization headers or API keys (completely public and keyless).
  - Parse direct CDN text response (e.g. `https://files.catbox.moe/abc123.png`).
  - Graceful try/catch handling: On timeout or HTTP 502/error, report:
    *"Upload failed. Please ensure the file size is under 200MB."*
  - Fall back smoothly to local file URI if Catbox network fails.

#### B. Transition Chat Uploads to Catbox (`app/src/main/java/com/example/ui/chat/ConversationThreadSheet.kt`)
- Replace `B2UploadManager` calls with `CatboxUploadManager.uploadFile()`.
- Route both user-selected attachments and AI-generated study diagrams through Catbox.moe.
- Inject the resulting `https://files.catbox.moe/...` CDN URL directly into the `ChatMessage` object for instant rendering in the chat thread.

#### C. Transition AI Engine to SiliconFlow (`app/src/main/java/com/example/ai/SiliconFlowImageEngine.kt` & `AiImageGeneratorEngine.kt`)
- Implement `SiliconFlowImageEngine` targeting `https://api.siliconflow.cn/v1/images/generations`.
- Default model: `black-forest-labs/FLUX.1-schnell` (with fallback to `stabilityai/stable-diffusion-3-medium`).
- Provide clean token handling in dialog settings for your SiliconFlow API key (`sk-...`).
- Update `AiGenerateImageDialog.kt` to reflect SiliconFlow as the active AI engine.

#### D. Unit Test Verification (`app/src/test/java/com/example/CatboxAndSiliconFlowTest.kt`)
- Add unit test verifying Catbox.moe upload request formatting (`reqtype=fileupload`, `fileToUpload`).
- Add unit test verifying 200MB error message text formatting.

---

### 2. Verification Plan

1. **Local JVM Tests**:
   - Run `gradle :app:testDebugUnitTest` to verify Catbox and SiliconFlow logic.
2. **Applet Compilation**:
   - Run `compile_applet` to confirm clean compilation with zero build errors.
