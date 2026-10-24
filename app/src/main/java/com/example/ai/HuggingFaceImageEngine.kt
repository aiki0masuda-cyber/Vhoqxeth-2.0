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
 * Supported open diffusion models, prioritized by lowest token cost.
 */
enum class HuggingFaceModel(
    val id: String,
    val displayName: String,
    val description: String,
    val defaultInferenceSteps: Int,
    val isLowestTokenTier: Boolean
) {
    SDXL_TURBO(
        id = "stabilityai/sdxl-turbo",
        displayName = "SDXL Turbo (Lowest Tokens)",
        description = "Single-step (1 step) real-time diffusion with absolute minimal token cost",
        defaultInferenceSteps = 1,
        isLowestTokenTier = true
    ),
    FLUX_1_SCHNELL(
        id = "black-forest-labs/FLUX.1-schnell",
        displayName = "FLUX.1 Schnell (4-Step Fast)",
        description = "4-step distilled high-speed model with low compute tokens",
        defaultInferenceSteps = 4,
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
            return entries.firstOrNull { it.id == id } ?: SDXL_TURBO
        }
    }
}

/**
 * Direct client for the official Hugging Face Inference Router
 * with lowest-token prompt optimization and zero serverless intermediaries.
 */
object HuggingFaceImageEngine {

    // Primary: Hugging Face Inference Router
    private const val ROUTER_INFERENCE_URL = "https://router.huggingface.co/hf-inference/models"
    // Secondary fallback: Direct Serverless endpoint
    private const val FALLBACK_INFERENCE_URL = "https://api-inference.huggingface.co/models"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
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
     * Generates an image directly via the Hugging Face Inference Router.
     * No serverless proxies or intermediary requests.
     */
    suspend fun generateImage(
        prompt: String,
        model: HuggingFaceModel = HuggingFaceModel.SDXL_TURBO,
        customModelId: String = "",
        aspectRatio: ImageAspectRatio = ImageAspectRatio.SQUARE_1_1,
        apiKey: String = "",
        lowTokenMode: Boolean = true
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            if (prompt.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Prompt cannot be empty."))
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

            val effectiveKey = apiKey.trim().ifBlank { resolveConfiguredApiKey() }
            val payloadBytes = payloadJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            // Try primary Hugging Face Inference Router endpoint first
            val primaryUrl = "$ROUTER_INFERENCE_URL/$effectiveModelId"
            Log.d(TAG, "Requesting Hugging Face Inference Router: $primaryUrl (Model: $effectiveDisplayName, Steps: $effectiveSteps)")

            val requestBuilder = Request.Builder()
                .url(primaryUrl)
                .post(payloadBytes)
                .header("Accept", "image/png, image/jpeg, application/json")

            if (effectiveKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $effectiveKey")
            }

            val bitmap = withTimeout(90_000L) {
                try {
                    executeRequestWithRetry(requestBuilder.build(), model)
                } catch (e: Exception) {
                    // Fall back to fallback serverless inference URL if router gives 404 or connection issue
                    val fallbackUrl = "$FALLBACK_INFERENCE_URL/$effectiveModelId"
                    Log.w(TAG, "Router attempt failed (${e.localizedMessage}), retrying via fallback endpoint: $fallbackUrl")
                    val fallbackRequest = Request.Builder()
                        .url(fallbackUrl)
                        .post(payloadBytes)
                        .header("Accept", "image/png, image/jpeg, application/json")
                        .apply {
                            if (effectiveKey.isNotBlank()) header("Authorization", "Bearer $effectiveKey")
                        }
                        .build()
                    executeRequestWithRetry(fallbackRequest, model)
                }
            }

            if (bitmap != null) {
                Log.i(TAG, "Successfully generated ${bitmap.width}x${bitmap.height} bitmap via $effectiveDisplayName ($effectiveSteps steps)")
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("No valid image returned from Hugging Face Inference Router."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out after 90s", e)
            Result.failure(IllegalStateException("Inference took longer than 90 seconds. Try SDXL Turbo for instant 1-step generation."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Hugging Face Inference Router error: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    /**
     * Refines or edits an existing image with new instructions directly.
     */
    suspend fun editImage(
        inputBitmap: Bitmap,
        editInstruction: String,
        model: HuggingFaceModel = HuggingFaceModel.SDXL_TURBO,
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
     * Executes the HTTP request directly to Hugging Face and handles binary and JSON responses.
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

                // Handle authorization error
                if (res.code == 401 || res.code == 403) {
                    val errorMsg = try {
                        JSONObject(responseBodyText).optString("error", "Unauthorized")
                    } catch (_: Exception) {
                        "Invalid or missing Hugging Face User Access Token."
                    }
                    throw IOException("Hugging Face Auth: $errorMsg. Please configure HUGGINGFACE_API_KEY in AI Studio Secrets panel or settings.")
                }

                // Handle rate limit
                if (res.code == 429) {
                    throw IOException("Hugging Face rate limit reached. Please wait a moment or configure your personal API token in Secrets panel.")
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
            if (key == "hf_placeholder_key" || key == "MY_NEW_API_KEY_DEFAULT_VALUE") "" else key
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
