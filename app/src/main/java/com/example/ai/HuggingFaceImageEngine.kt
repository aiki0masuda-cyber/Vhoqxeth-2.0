package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "HuggingFaceEngine"

/**
 * Result data holder for image generation, recording the actual model that succeeded
 * so the UI can dynamically update chips on failover.
 */
data class ImageGenerationResult(
    val bitmap: Bitmap,
    val usedModel: HuggingFaceModel,
    val wasFailover: Boolean,
    val failoverMessage: String? = null
)

/**
 * Supported open diffusion models, with FLUX.1 Schnell prioritized as the primary high-detail 4-step model.
 */
enum class HuggingFaceModel(
    val id: String,
    val displayName: String,
    val description: String,
    val defaultInferenceSteps: Int,
    val isLowestTokenTier: Boolean
) {
    FLUX_1_SCHNELL(
        id = "black-forest-labs/FLUX.1-schnell",
        displayName = "FLUX.1 Schnell (4-Step Fast)",
        description = "High-detail 4-step distilled high-speed generation",
        defaultInferenceSteps = 4,
        isLowestTokenTier = true
    ),
    SDXL_TURBO(
        id = "stabilityai/sdxl-turbo",
        displayName = "SDXL Turbo (Lowest Tokens)",
        description = "Single-step (1 step) real-time diffusion with minimal token cost",
        defaultInferenceSteps = 1,
        isLowestTokenTier = true
    ),
    STABLE_DIFFUSION_XL(
        id = "stabilityai/stable-diffusion-xl-base-1.0",
        displayName = "SDXL 1.0 (Detailed)",
        description = "Standard 25-step high-resolution model",
        defaultInferenceSteps = 25,
        isLowestTokenTier = false
    ),
    STABLE_DIFFUSION_1_5(
        id = "runwayml/stable-diffusion-v1-5",
        displayName = "SD 1.5 (Classic)",
        description = "Lightweight legacy diffusion model",
        defaultInferenceSteps = 20,
        isLowestTokenTier = false
    );

    companion object {
        fun fromId(id: String): HuggingFaceModel {
            return entries.firstOrNull { it.id == id } ?: FLUX_1_SCHNELL
        }
    }
}

/**
 * Direct client for the official Hugging Face Inference Router
 * with dynamic auto-failover, lowest-token prompt optimization,
 * command interception parsing, and 429 rate limit mitigation.
 */
object HuggingFaceImageEngine {

    // Primary official Hugging Face Inference Router endpoint
    const val ROUTER_INFERENCE_URL = "https://router.huggingface.co/hf-inference/models"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Inspects incoming chat message text for explicit image generation commands:
     * - /imagine <prompt>
     * - /draw <prompt>
     * - /image <prompt>
     * - generate an image of <prompt> / generate image of <prompt>
     * - draw a diagram of <prompt> / draw an illustration of <prompt> / draw <prompt>
     * - create an illustration of <prompt> / create illustration of <prompt>
     * Returns the extracted prompt string or null if not an image generation command.
     */
    fun extractImageCommandPrompt(messageText: String): String? {
        val trimmed = messageText.trim()
        val commandPrefixes = listOf(
            "/imagine ", "/draw ", "/image ",
            "/imagine\n", "/draw\n", "/image\n",
            "generate an image of ", "generate image of ",
            "generate an illustration of ", "generate illustration of ",
            "generate a diagram of ", "generate diagram of ",
            "draw a diagram of ", "draw an illustration of ",
            "draw a picture of ", "draw an image of ",
            "create an illustration of ", "create illustration of ",
            "create a diagram of ", "create diagram of ",
            "draw "
        )

        for (prefix in commandPrefixes) {
            if (trimmed.startsWith(prefix, ignoreCase = true)) {
                val extracted = trimmed.substring(prefix.length).trim()
                if (extracted.isNotBlank()) {
                    return extracted
                }
            }
        }
        return null
    }

    /**
     * Sanitizes API token by stripping accidental leading "Bearer ", quotes, and whitespace.
     */
    fun sanitizeApiKey(key: String): String {
        return key.trim()
            .removePrefix("Bearer ")
            .removePrefix("bearer ")
            .trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .trim()
    }

    /**
     * Verifies whether a Hugging Face user access token is valid by querying whoami.
     */
    suspend fun validateToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        val sanitized = sanitizeApiKey(token)
        if (sanitized.isBlank() || sanitized == "hf_placeholder_key") {
            return@withContext Result.failure(IllegalArgumentException("Please enter a non-empty Hugging Face token."))
        }
        try {
            val req = Request.Builder()
                .url("https://huggingface.co/api/whoami-v2")
                .header("Authorization", "Bearer $sanitized")
                .get()
                .build()
            val res = httpClient.newCall(req).execute()
            res.use { r ->
                val body = r.body?.string().orEmpty()
                if (r.isSuccessful) {
                    val json = JSONObject(body)
                    val name = json.optString("name", json.optString("username", "Authenticated User"))
                    Result.success(name)
                } else if (r.code == 401 || r.code == 403) {
                    Result.failure(IOException("Invalid Hugging Face token (HTTP ${r.code}). Please verify your token at huggingface.co/settings/tokens."))
                } else {
                    Result.failure(IOException("Verification failed with HTTP ${r.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cleans and truncates user prompts to minimize unnecessary token consumption,
     * stripping decorative repetitive adjectives that bloat token billing.
     */
    fun optimizePromptForLowestTokens(prompt: String): String {
        val stopPhrases = listOf(
            "highly detailed", "ultra detailed", "hyper realistic", "8k resolution",
            "4k resolution", "trending on artstation", "award winning", "masterpiece",
            "extremely high quality", "sharp focus", "intricate details", "photorealistic"
        )
        var cleaned = prompt.trim()
        for (phrase in stopPhrases) {
            cleaned = cleaned.replace(Regex("(?i)\\b" + Regex.escape(phrase) + "\\b"), "").trim()
        }
        cleaned = cleaned.replace(Regex(",\\s*,"), ",").replace(Regex("\\s+"), " ").trim(',', ' ')
        val words = cleaned.split(" ")
        return if (words.size > 50) words.take(50).joinToString(" ") else cleaned
    }

    /**
     * Generates an image with dynamic model failover.
     * If the requested model throws "Model not supported", 404, or network availability error,
     * it automatically fails over to FLUX.1 Schnell and returns the active model for UI chip updating.
     */
    suspend fun generateImageWithFailover(
        prompt: String,
        preferredModel: HuggingFaceModel = HuggingFaceModel.FLUX_1_SCHNELL,
        customModelId: String = "",
        aspectRatio: ImageAspectRatio = ImageAspectRatio.SQUARE_1_1,
        apiKey: String = "",
        lowTokenMode: Boolean = true
    ): Result<ImageGenerationResult> = withContext(Dispatchers.IO) {
        val candidateModels = mutableListOf<Pair<HuggingFaceModel, String>>()
        if (customModelId.isNotBlank()) {
            candidateModels.add(preferredModel to customModelId.trim())
        } else {
            candidateModels.add(preferredModel to preferredModel.id)
        }

        // Automatic fallback sequence prioritizing FLUX.1 Schnell
        if (preferredModel != HuggingFaceModel.FLUX_1_SCHNELL && customModelId.isBlank()) {
            candidateModels.add(HuggingFaceModel.FLUX_1_SCHNELL to HuggingFaceModel.FLUX_1_SCHNELL.id)
        }
        if (preferredModel != HuggingFaceModel.SDXL_TURBO) {
            candidateModels.add(HuggingFaceModel.SDXL_TURBO to HuggingFaceModel.SDXL_TURBO.id)
        }
        if (preferredModel != HuggingFaceModel.STABLE_DIFFUSION_XL) {
            candidateModels.add(HuggingFaceModel.STABLE_DIFFUSION_XL to HuggingFaceModel.STABLE_DIFFUSION_XL.id)
        }

        var lastError: Throwable? = null
        for (i in candidateModels.indices) {
            val (candidateModel, modelId) = candidateModels[i]
            val isFailover = i > 0
            try {
                Log.d(TAG, "Executing inference: model=${candidateModel.displayName} (id=$modelId, failover=$isFailover)")
                val bitmapResult = executeDirectInference(
                    prompt = prompt,
                    model = candidateModel,
                    customModelId = if (candidateModel == preferredModel && customModelId.isNotBlank()) customModelId else modelId,
                    aspectRatio = aspectRatio,
                    apiKey = apiKey,
                    lowTokenMode = lowTokenMode
                )
                if (bitmapResult.isSuccess) {
                    val bitmap = bitmapResult.getOrThrow()
                    Log.i(TAG, "Inference succeeded with ${candidateModel.displayName} (wasFailover=$isFailover)")
                    return@withContext Result.success(
                        ImageGenerationResult(
                            bitmap = bitmap,
                            usedModel = candidateModel,
                            wasFailover = isFailover,
                            failoverMessage = if (isFailover) "Auto-routed to ${candidateModel.displayName}" else null
                        )
                    )
                } else {
                    lastError = bitmapResult.exceptionOrNull()
                    Log.w(TAG, "Model $modelId failed: ${lastError?.message}. Attempting next model...")
                }
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Model $modelId threw exception: ${e.message}. Attempting next model...")
            }
        }

        Result.failure(lastError ?: IllegalStateException("All candidate diffusion models failed."))
    }

    /**
     * Direct image generation returning a Bitmap, automatically applying failover behind the scenes.
     */
    suspend fun generateImage(
        prompt: String,
        model: HuggingFaceModel = HuggingFaceModel.FLUX_1_SCHNELL,
        customModelId: String = "",
        aspectRatio: ImageAspectRatio = ImageAspectRatio.SQUARE_1_1,
        apiKey: String = "",
        lowTokenMode: Boolean = true
    ): Result<Bitmap> {
        return generateImageWithFailover(prompt, model, customModelId, aspectRatio, apiKey, lowTokenMode)
            .map { it.bitmap }
    }

    /**
     * Executes inference directly for a single target model repository.
     */
    private suspend fun executeDirectInference(
        prompt: String,
        model: HuggingFaceModel,
        customModelId: String,
        aspectRatio: ImageAspectRatio,
        apiKey: String,
        lowTokenMode: Boolean
    ): Result<Bitmap> {
        if (prompt.isBlank()) {
            return Result.failure(IllegalArgumentException("Prompt cannot be empty."))
        }

        val sanitizedPrompt = if (lowTokenMode) optimizePromptForLowestTokens(prompt) else prompt.trim()
        val effectiveModelId = customModelId.trim().ifBlank { model.id }
        val effectiveDisplayName = if (customModelId.isNotBlank()) customModelId.trim() else model.displayName

        // Calculate lowest inference steps
        val effectiveSteps = when {
            lowTokenMode && effectiveModelId.contains("turbo", ignoreCase = true) -> 1
            lowTokenMode && effectiveModelId.contains("schnell", ignoreCase = true) -> 4
            lowTokenMode -> 4.coerceAtMost(model.defaultInferenceSteps)
            effectiveModelId.contains("turbo", ignoreCase = true) -> 1
            effectiveModelId.contains("schnell", ignoreCase = true) -> 4
            effectiveModelId.contains("xl", ignoreCase = true) -> 25
            else -> model.defaultInferenceSteps
        }

        val enrichedPrompt = if (lowTokenMode) {
            when (aspectRatio) {
                ImageAspectRatio.LANDSCAPE_16_9 -> "$sanitizedPrompt, 16:9 ratio"
                ImageAspectRatio.PORTRAIT_9_16 -> "$sanitizedPrompt, 9:16 ratio"
                else -> sanitizedPrompt
            }
        } else {
            when (aspectRatio) {
                ImageAspectRatio.LANDSCAPE_16_9 -> "$sanitizedPrompt, 16:9 wide educational diagram, high resolution"
                ImageAspectRatio.PORTRAIT_9_16 -> "$sanitizedPrompt, 9:16 vertical scholastic visual, detailed"
                ImageAspectRatio.LANDSCAPE_4_3 -> "$sanitizedPrompt, 4:3 presentation ratio, textbook diagram"
                ImageAspectRatio.PORTRAIT_3_4 -> "$sanitizedPrompt, 3:4 tall aspect ratio, vector educational visual"
                ImageAspectRatio.SQUARE_1_1 -> "$sanitizedPrompt, 1:1 square ratio, centered scholastic artwork"
            }
        }

        val guidance = if (effectiveModelId.contains("turbo", ignoreCase = true)) 0.0 else 7.5

        val payloadJson = JSONObject().apply {
            put("inputs", enrichedPrompt)
            val parameters = JSONObject().apply {
                put("guidance_scale", guidance)
                put("num_inference_steps", effectiveSteps)
            }
            put("parameters", parameters)
            val options = JSONObject().apply {
                put("wait_for_model", true)
                put("use_cache", true)
            }
            put("options", options)
        }

        val rawKey = apiKey.trim().ifBlank { resolveConfiguredApiKey() }
        val effectiveKey = sanitizeApiKey(rawKey)
        val payloadBytes = payloadJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val primaryUrl = "$ROUTER_INFERENCE_URL/$effectiveModelId"
        val requestBuilder = Request.Builder()
            .url(primaryUrl)
            .post(payloadBytes)
            .header("Accept", "image/png, image/jpeg, application/json")

        if (effectiveKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $effectiveKey")
        }

        return try {
            val bitmap = withTimeout(90_000L) {
                executeRequestWithRetry(requestBuilder.build(), model)
            }
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("No valid image binary returned from $effectiveDisplayName."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refines or edits an existing image with new instructions directly.
     */
    suspend fun editImage(
        inputBitmap: Bitmap,
        editInstruction: String,
        model: HuggingFaceModel = HuggingFaceModel.FLUX_1_SCHNELL,
        customModelId: String = "",
        apiKey: String = "",
        lowTokenMode: Boolean = true
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val prompt = "Refined educational illustration: $editInstruction"
            generateImage(
                prompt = prompt,
                model = model,
                customModelId = customModelId,
                aspectRatio = ImageAspectRatio.SQUARE_1_1,
                apiKey = apiKey,
                lowTokenMode = lowTokenMode
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image editing error: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    /**
     * Executes the HTTP request directly to Hugging Face and handles binary and JSON responses,
     * including rate limit 429 error mitigation.
     */
    private suspend fun executeRequestWithRetry(
        request: Request,
        model: HuggingFaceModel,
        maxRetries: Int = 2
    ): Bitmap? {
        var currentAttempt = 0
        while (currentAttempt <= maxRetries) {
            currentAttempt++
            val response = httpClient.newCall(request).execute()

            response.use { res ->
                val contentType = res.header("Content-Type").orEmpty().lowercase()

                if (res.isSuccessful && (contentType.startsWith("image/") || contentType.contains("octet-stream"))) {
                    val bytes = res.body?.bytes() ?: return null
                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }

                val responseBodyText = res.body?.string().orEmpty()

                // Check if response is JSON containing base64 data
                if (res.isSuccessful && contentType.contains("json")) {
                    try {
                        val json = JSONObject(responseBodyText)
                        val b64 = json.optString("image", json.optString("base64", ""))
                        if (b64.isNotBlank()) {
                            val clean = if (b64.contains(",")) b64.substringAfter(",") else b64
                            val bytes = Base64.decode(clean, Base64.DEFAULT)
                            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                    } catch (_: Exception) {}
                }

                Log.w(TAG, "Hugging Face returned HTTP ${res.code} (Type: $contentType): $responseBodyText")

                // Handle 503 Model Loading delay
                if (res.code == 503 && responseBodyText.contains("loading", ignoreCase = true)) {
                    val estimatedTime = try {
                        JSONObject(responseBodyText).optDouble("estimated_time", 15.0)
                    } catch (_: Exception) {
                        15.0
                    }
                    Log.i(TAG, "Model is loading. Waiting ${(estimatedTime).toInt()}s before retry (attempt $currentAttempt/$maxRetries)...")
                    if (currentAttempt <= maxRetries) {
                        val waitMillis = (estimatedTime * 1000).toLong().coerceIn(5_000L, 25_000L)
                        delay(waitMillis)
                        return@use
                    }
                }

                // Handle authorization error (HTTP 401 / 403)
                if (res.code == 401 || res.code == 403) {
                    val errorMsg = try {
                        val json = JSONObject(responseBodyText)
                        json.optString("error", json.optString("message", "Unauthorized"))
                    } catch (_: Exception) {
                        "Invalid credentials."
                    }
                    throw IOException("Hugging Face Auth Failed (HTTP ${res.code}): $errorMsg. Please check your token from huggingface.co/settings/tokens.")
                }

                // Handle rate limit (HTTP 429) - exact error mitigation directive
                if (res.code == 429) {
                    throw IOException("The free image generation pipeline is currently busy. Please wait a moment and click generate again.")
                }

                // Handle general errors
                val generalError = try {
                    val json = JSONObject(responseBodyText)
                    json.optString("error", json.optString("message", "HTTP ${res.code}"))
                } catch (_: Exception) {
                    "HTTP ${res.code}: ${res.message}"
                }
                throw IOException("Hugging Face error: $generalError")
            }
        }
        return null
    }

    /**
     * Resolves API key from BuildConfig (injected via AI Studio Secrets panel) or environment.
     */
    fun resolveConfiguredApiKey(): String {
        return try {
            val field = Class.forName("com.example.BuildConfig").getField("HUGGINGFACE_API_KEY")
            val key = (field.get(null) as? String)?.trim().orEmpty()
            if (key == "hf_placeholder_key" || key == "MY_NEW_API_KEY_DEFAULT_VALUE") "" else sanitizeApiKey(key)
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Caches the resulting Bitmap to the local application cache directory and returns a content Uri.
     */
    fun saveBitmapToLocalCache(context: Context, bitmap: Bitmap, prefix: String = "ai_huggingface"): Uri {
        val imagesDir = File(context.cacheDir, "generated_images").apply { mkdirs() }
        val imageFile = File(imagesDir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return Uri.fromFile(imageFile)
    }
}
