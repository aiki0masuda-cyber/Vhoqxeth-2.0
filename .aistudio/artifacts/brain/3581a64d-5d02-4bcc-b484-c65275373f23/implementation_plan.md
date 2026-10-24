# Direct Hugging Face Inference Router Setup & Serverless Request Removal

Eliminate all serverless proxy intermediaries from the AI image generation pipeline, connecting the Android application directly to the official **Hugging Face Inference Router** (`https://router.huggingface.co/hf-inference/models/{model_id}`). Inject the API token seamlessly from the AI Studio Secrets panel via `BuildConfig.HUGGINGFACE_API_KEY`, and resolve all endpoint and payload errors.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were selected based on your direct answers:
> - **Direct Hugging Face Inference Router**: Target `https://router.huggingface.co/hf-inference/models/{model_id}` directly from Android with fallback to `https://api-inference.huggingface.co/models/{model_id}`.
> - **Total Serverless Proxy Removal**: Completely remove `executeServerGeneration` and all serverless proxy routing from the image generation engine and UI dialog.
> - **AI Studio Secrets Token Injection**: Primary API key is automatically resolved from `BuildConfig.HUGGINGFACE_API_KEY` configured in the Secrets panel, with optional user override in settings.
> - **Lowest Token Usage Preserved**: Maintain prompt token optimization and 1-step `stabilityai/sdxl-turbo` / 4-step `black-forest-labs/FLUX.1-schnell` models for minimal quota expenditure.
> - **Private B2 Delivery Retained**: Once the image is generated directly on-device, clicking "Send into Chat" continues to upload and store the artwork securely in your private Backblaze B2 bucket.

---

### 1. Proposed Changes

#### A. Direct Inference Router Client (`app/src/main/java/com/example/ai/HuggingFaceImageEngine.kt`)
- Update primary endpoint URL to `https://router.huggingface.co/hf-inference/models/{model_id}` with automated fallback to `https://api-inference.huggingface.co/models/{model_id}`.
- Remove `executeServerGeneration()` and any serverless proxy logic.
- Ensure API token is automatically retrieved from `BuildConfig.HUGGINGFACE_API_KEY` (AI Studio Secrets panel) or in-app settings.
- Ensure robust decoding of both raw image binary streams (`image/png`, `image/jpeg`) and JSON base64 payloads.
- Retain lowest token parameters: `num_inference_steps` (1 for Turbo, 4 for Schnell) and `optimizePromptForLowestTokens()`.

#### B. Engine Facade (`app/src/main/java/com/example/ai/AiImageGeneratorEngine.kt`)
- Strip `serverEndpointUrl` from `generateImage` and `editImage` method signatures.

#### C. Clean Session Storage (`app/src/main/java/com/example/data/SessionManager.kt`)
- Remove `isServerAiEndpointEnabled`, `setServerAiEndpointEnabled`, `getCustomAiServerUrl`, and `setCustomAiServerUrl`.
- Retain `getHuggingFaceApiKey()`, `setHuggingFaceApiKey()`, `isLowTokenModeEnabled()`, and custom model persistence.

#### D. Streamlined UI Dialog (`app/src/main/java/com/example/ui/chat/AiGenerateImageDialog.kt`)
- Remove the "Server Endpoint Setup" toggle, server URL input, and "Test Server Health" button.
- Display a clear "Hugging Face Inference Router" badge and Secrets panel token status.
- If no token is configured in Secrets or settings, display an actionable banner guiding the user to configure `HUGGINGFACE_API_KEY` in the AI Studio Secrets panel.

#### E. Test Suite Alignment (`app/src/test/java/com/example/AiImageGeneratorEngineTest.kt`)
- Update unit tests to verify direct Inference Router URL generation, model configuration, and prompt token pruning.

---

### 2. Verification Plan

1. **Local JVM Tests**:
   - Run `gradle :app:testDebugUnitTest` to verify engine configuration and prompt optimization tests pass.
2. **Applet Compilation**:
   - Run `compile_applet` to confirm clean compilation and zero breaking changes.
