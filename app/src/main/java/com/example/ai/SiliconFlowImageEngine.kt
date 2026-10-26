package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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

private const val TAG = "SiliconFlowEngine"

/**
 * Result data holder for SiliconFlow image generation.
 */
data class SiliconFlowResult(
    val bitmap: Bitmap,
    val usedModel: SiliconFlowModel,
    val wasFailover: Boolean = false,
    val imageUrl: String? = null
)

/**
 * Supported SiliconFlow diffusion models.
 */
enum class SiliconFlowModel(
    val id: String,
    val displayName: String,
    val defaultSteps: Int = 4,
    val isPrimary: Boolean = false
) {
    FLUX_1_SCHNELL(
        id = "black-forest-labs/FLUX.1-schnell",
        displayName = "FLUX.1 Schnell (4-Step Fast)",
        defaultSteps = 4,
        isPrimary = true
    ),
    SD_3_MEDIUM(
        id = "stabilityai/stable-diffusion-3-medium",
        displayName = "SD 3 Medium",
        defaultSteps = 20,
        isPrimary = false
    ),
    FLUX_1_DEV(
        id = "black-forest-labs/FLUX.1-dev",
        displayName = "FLUX.1 Dev (Pro Detail)",
        defaultSteps = 20,
        isPrimary = false
    );

    companion object {
        fun fromId(id: String): SiliconFlowModel {
            return entries.firstOrNull { it.id == id } ?: FLUX_1_SCHNELL
        }
    }
}

/**
 * Production-ready SiliconFlow AI Image Generation Engine.
 * API Endpoint: https://api.siliconflow.cn/v1/images/generations
 */
object SiliconFlowImageEngine {

    const val SILICONFLOW_API_URL = "https://api.siliconflow.cn/v1/images/generations"

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
     * Sanitizes SiliconFlow API key (e.g. sk-xxxxxxxx).
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
     * Generates an image using SiliconFlow with automatic model failover.
     */
    suspend fun generateImageWithFailover(
        prompt: String,
        preferredModel: SiliconFlowModel = SiliconFlowModel.FLUX_1_SCHNELL,
        aspectRatio: ImageAspectRatio = ImageAspectRatio.SQUARE_1_1,
        apiKey: String = ""
    ): Result<SiliconFlowResult> = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<SiliconFlowModel>()
        candidates.add(preferredModel)
        if (preferredModel != SiliconFlowModel.FLUX_1_SCHNELL) {
            candidates.add(SiliconFlowModel.FLUX_1_SCHNELL)
        }
        if (preferredModel != SiliconFlowModel.SD_3_MEDIUM) {
            candidates.add(SiliconFlowModel.SD_3_MEDIUM)
        }

        var lastException: Throwable? = null

        for (i in candidates.indices) {
            val candidate = candidates[i]
            val isFailover = i > 0
            try {
                Log.d(TAG, "Requesting SiliconFlow generation: model=${candidate.displayName} (failover=$isFailover)")
                val res = executeSiliconFlowRequest(prompt, candidate, aspectRatio, apiKey)
                if (res.isSuccess) {
                    val (bitmap, url) = res.getOrThrow()
                    return@withContext Result.success(
                        SiliconFlowResult(
                            bitmap = bitmap,
                            usedModel = candidate,
                            wasFailover = isFailover,
                            imageUrl = url
                        )
                    )
                } else {
                    lastException = res.exceptionOrNull()
                    Log.w(TAG, "SiliconFlow model ${candidate.id} failed: ${lastException?.message}")
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "SiliconFlow candidate ${candidate.id} threw error: ${e.message}")
            }
        }

        Result.failure(lastException ?: IOException("Failed to generate image with SiliconFlow."))
    }

    /**
     * Executes single request to SiliconFlow API.
     */
    private suspend fun executeSiliconFlowRequest(
        prompt: String,
        model: SiliconFlowModel,
        aspectRatio: ImageAspectRatio,
        apiKey: String
    ): Result<Pair<Bitmap, String?>> {
        val effectiveKey = sanitizeApiKey(apiKey).ifBlank { resolveConfiguredApiKey() }

        val imageSize = when (aspectRatio) {
            ImageAspectRatio.LANDSCAPE_16_9 -> "1024x576"
            ImageAspectRatio.PORTRAIT_9_16 -> "576x1024"
            ImageAspectRatio.LANDSCAPE_4_3 -> "1024x768"
            ImageAspectRatio.PORTRAIT_3_4 -> "768x1024"
            ImageAspectRatio.SQUARE_1_1 -> "1024x1024"
        }

        val payload = JSONObject().apply {
            put("model", model.id)
            put("prompt", prompt.trim())
            put("image_size", imageSize)
            put("num_inference_steps", model.defaultSteps)
        }

        val requestBuilder = Request.Builder()
            .url(SILICONFLOW_API_URL)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "application/json")

        if (effectiveKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $effectiveKey")
        }

        return try {
            val response = httpClient.newCall(requestBuilder.build()).execute()
            response.use { res ->
                val bodyText = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    val err = try {
                        val json = JSONObject(bodyText)
                        json.optString("message", json.optString("error", "HTTP ${res.code}"))
                    } catch (_: Exception) {
                        "HTTP ${res.code}: ${res.message}"
                    }
                    return Result.failure(IOException("SiliconFlow Error ($err)"))
                }

                val json = JSONObject(bodyText)
                val imagesArray = json.optJSONArray("images")
                if (imagesArray != null && imagesArray.length() > 0) {
                    val firstImage = imagesArray.getJSONObject(0)
                    val imgUrl = firstImage.optString("url")
                    val b64 = firstImage.optString("b64_json")

                    if (imgUrl.isNotBlank()) {
                        // Download the generated image bitmap
                        val downloadReq = Request.Builder().url(imgUrl).get().build()
                        val downloadRes = httpClient.newCall(downloadReq).execute()
                        downloadRes.use { dl ->
                            if (dl.isSuccessful) {
                                val bytes = dl.body?.bytes() ?: return Result.failure(IOException("Empty image stream"))
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                return Result.success(bmp to imgUrl)
                            }
                        }
                    } else if (b64.isNotBlank()) {
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        return Result.success(bmp to null)
                    }
                }

                Result.failure(IOException("No image returned in SiliconFlow response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves API key from BuildConfig or environment.
     */
    fun resolveConfiguredApiKey(): String {
        return try {
            val field = Class.forName("com.example.BuildConfig").getField("SILICONFLOW_API_KEY")
            val key = (field.get(null) as? String)?.trim().orEmpty()
            if (key == "sk_placeholder_key" || key == "MY_NEW_API_KEY_DEFAULT_VALUE") "" else sanitizeApiKey(key)
        } catch (_: Exception) {
            // Also check fallback HUGGINGFACE_API_KEY field if user stored their key there
            try {
                val field = Class.forName("com.example.BuildConfig").getField("HUGGINGFACE_API_KEY")
                val key = (field.get(null) as? String)?.trim().orEmpty()
                if (key.startsWith("sk-")) sanitizeApiKey(key) else ""
            } catch (_: Exception) {
                ""
            }
        }
    }

    /**
     * Caches Bitmap to local application cache directory.
     */
    fun saveBitmapToLocalCache(context: Context, bitmap: Bitmap, prefix: String = "ai_siliconflow"): Uri {
        val imagesDir = File(context.cacheDir, "generated_images").apply { mkdirs() }
        val imageFile = File(imagesDir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return Uri.fromFile(imageFile)
    }
}
