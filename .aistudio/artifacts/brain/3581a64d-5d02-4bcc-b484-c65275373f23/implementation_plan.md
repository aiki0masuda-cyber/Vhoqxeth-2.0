# Implementation Plan: Universal SiliconFlow Secret Integration

Configure the SiliconFlow API key universally for all users in the application using the Secrets Gradle Plugin and `BuildConfig`, ensuring the key is hidden from end-users, requiring zero configuration by default, and keeping an optional override in advanced settings.

## User Review Required

> [!IMPORTANT]
> **Secrets Panel Setup**: The app will read `SILICONFLOW_API_KEY` from `BuildConfig`. To enable image generation, please ensure your SiliconFlow API key (`sk-...`) is added under the key name `SILICONFLOW_API_KEY` in the **Secrets panel in AI Studio**.

- **Key Privacy**: The universal secret key is never displayed or rendered in plain text to client users in the UI or written into logcat streams.
- **Client User Flow**: Normal users will not be prompted to enter an API key; the generate button will work automatically.
- **Advanced Override**: If advanced users/developers want to use their own personal SiliconFlow key, an optional override field remains accessible inside the dialog settings accordion.
- **Unset Secret Handling**: If the secret has not yet been supplied in AI Studio Secrets, a friendly administrator notice is displayed guiding them to configure `SILICONFLOW_API_KEY`.

---

## Proposed Changes

### Configuration Layer

#### [`.env.example`](file:///.env.example)
- Add `SILICONFLOW_API_KEY=sk_placeholder_key` to `.env.example`.
- This informs the Secrets Gradle Plugin to generate `BuildConfig.SILICONFLOW_API_KEY` and allows AI Studio's Secrets panel to detect and inject the secret at build/run time.

---

### AI Engine Layer

#### [`app/src/main/java/com/example/ai/SiliconFlowImageEngine.kt`](file:///app/src/main/java/com/example/ai/SiliconFlowImageEngine.kt)
- Update `resolveConfiguredApiKey()`:
  - Access `BuildConfig.SILICONFLOW_API_KEY` directly and safely via reflection/`BuildConfig`.
  - Treat default placeholder values (`sk_placeholder_key`, `MY_NEW_API_KEY_DEFAULT_VALUE`, empty strings) as unconfigured.
  - Support fallback to existing `HUGGINGFACE_API_KEY` if user previously saved their `sk-` token under that variable.
- Ensure sanitization removes any leading `Bearer `, whitespace, and surrounding quotes.
- Guarantee that error logs and exception messages never print the raw API key token.

---

### UI & Presentation Layer

#### [`app/src/main/java/com/example/ui/chat/AiGenerateImageDialog.kt`](file:///app/src/main/java/com/example/ui/chat/AiGenerateImageDialog.kt)
- **Status Indicator**:
  - When universal secret (or custom key) is active: display a subtle, reassuring badge: "SiliconFlow AI Ready" with a green shield/sparkle icon. No key text is displayed.
  - When no key is active: display a friendly notice banner: "SiliconFlow secret not configured. Please add SILICONFLOW_API_KEY in the AI Studio Secrets panel."
- **Settings Accordion (Advanced)**:
  - Retain the gear icon in the title bar for advanced options.
  - Change the API key input title to "Custom API Key (Optional Override)".
  - Add explanatory helper text: "The app uses the universal SiliconFlow secret key by default. You only need to enter a custom key if you wish to override it."
  - Mask the input using `PasswordVisualTransformation()` and placeholder text `●●●●●●●● (Universal Key Active)`.
- **Primary Action**:
  - The "Generate" button is enabled whenever prompt text is entered.
  - If triggered without a configured key, show a concise error message pointing to the AI Studio Secrets panel without exposing internal stack traces.

---

### Testing Layer

#### [`app/src/test/java/com/example/AiImageGeneratorEngineTest.kt`](file:///app/src/test/java/com/example/AiImageGeneratorEngineTest.kt)
- Add unit tests for:
  - `testUniversalApiKeyResolution`: Verifies placeholder recognition, trimming, and sanitization.
  - `testCustomApiKeyOverride`: Verifies that a user-specified custom key takes precedence over the universal default key.
  - `testApiKeyMaskingAndSanitization`: Verifies that `sanitizeApiKey` cleans prefix `Bearer `, quotes, and whitespace.

---

## Verification Plan

### Automated Tests
- Run Gradle unit tests locally via `run_command`:
  ```bash
  gradle :app:testDebugUnitTest
  ```
- Verify compilation and linting via:
  - `compile_applet`

### Manual Verification Steps
1. Open the AI Image Generator dialog in the chat interface.
2. Confirm the UI does not prompt for an API key by default and shows the clean status badge.
3. Open the Advanced Settings accordion to verify the optional override field with masked text.
4. Verify that missing key shows the friendly administrator notice instead of crashing or leaking raw tokens.
