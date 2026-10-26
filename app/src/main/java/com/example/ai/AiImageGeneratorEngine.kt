package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri

/**
 * Aspect ratio options for image generation.
 */
enum class ImageAspectRatio(val label: String, val ratioWidth: Float, val ratioHeight: Float) {
    SQUARE_1_1("1:1 Square", 1f, 1f),
    LANDSCAPE_16_9("16:9 Landscape", 16f, 9f),
    PORTRAIT_9_16("9:16 Portrait", 9f, 16f),
    LANDSCAPE_4_3("4:3 Standard", 4f, 3f),
    PORTRAIT_3_4("3:4 Tall", 3f, 4f);
}

/**
 * Unified AI Image Generation service engine powered by SiliconFlow
 * with FLUX.1 Schnell default and dynamic model failover.
 */
object AiImageGeneratorEngine {

    const val MODEL_NAME = "SiliconFlow FLUX.1 Schnell"

    /**
     * Generates an image with dynamic auto-failover via SiliconFlow.
     */
    suspend fun generateImageWithFailover(
        prompt: String,
        aspectRatio: ImageAspectRatio = ImageAspectRatio.SQUARE_1_1,
        model: SiliconFlowModel = SiliconFlowModel.FLUX_1_SCHNELL,
        apiKey: String = ""
    ): Result<SiliconFlowResult> {
        return SiliconFlowImageEngine.generateImageWithFailover(
            prompt = prompt,
            preferredModel = model,
            aspectRatio = aspectRatio,
            apiKey = apiKey
        )
    }

    /**
     * Generates an image from a natural language text prompt via SiliconFlow.
     */
    suspend fun generateImage(
        prompt: String,
        aspectRatio: ImageAspectRatio = ImageAspectRatio.SQUARE_1_1,
        model: SiliconFlowModel = SiliconFlowModel.FLUX_1_SCHNELL,
        apiKey: String = ""
    ): Result<Bitmap> {
        return SiliconFlowImageEngine.generateImageWithFailover(
            prompt = prompt,
            preferredModel = model,
            aspectRatio = aspectRatio,
            apiKey = apiKey
        ).map { it.bitmap }
    }

    /**
     * Caches the resulting Bitmap to the local app cache directory and returns a content Uri.
     */
    fun saveBitmapToLocalCache(context: Context, bitmap: Bitmap, prefix: String = "ai_art"): Uri {
        return SiliconFlowImageEngine.saveBitmapToLocalCache(context, bitmap, prefix)
    }
}
