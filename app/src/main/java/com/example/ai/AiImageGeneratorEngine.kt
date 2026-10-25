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
 * Unified AI Image Generation service engine powered directly by Hugging Face Inference Router
 * with dynamic failover, lowest token consumption, and zero serverless proxy requests.
 */
object AiImageGeneratorEngine {

    const val MODEL_NAME = "Hugging Face Inference Router"

    /**
     * Generates an image with dynamic auto-failover, returning ImageGenerationResult
     * so UI chips can shift visual states automatically.
     */
    suspend fun generateImageWithFailover(
        prompt: String,
        aspectRatio: ImageAspectRatio = ImageAspectRatio.SQUARE_1_1,
        model: HuggingFaceModel = HuggingFaceModel.FLUX_1_SCHNELL,
        customModelId: String = "",
        apiKey: String = "",
        lowTokenMode: Boolean = true
    ): Result<ImageGenerationResult> {
        return HuggingFaceImageEngine.generateImageWithFailover(
            prompt = prompt,
            preferredModel = model,
            customModelId = customModelId,
            aspectRatio = aspectRatio,
            apiKey = apiKey,
            lowTokenMode = lowTokenMode
        )
    }

    /**
     * Generates an image from a natural language text prompt via direct Hugging Face Inference Router.
     */
    suspend fun generateImage(
        prompt: String,
        aspectRatio: ImageAspectRatio = ImageAspectRatio.SQUARE_1_1,
        model: HuggingFaceModel = HuggingFaceModel.FLUX_1_SCHNELL,
        customModelId: String = "",
        apiKey: String = "",
        lowTokenMode: Boolean = true
    ): Result<Bitmap> {
        return HuggingFaceImageEngine.generateImage(
            prompt = prompt,
            model = model,
            customModelId = customModelId,
            aspectRatio = aspectRatio,
            apiKey = apiKey,
            lowTokenMode = lowTokenMode
        )
    }

    /**
     * Edits or refines an existing Bitmap using prompt instructions.
     */
    suspend fun editImage(
        inputBitmap: Bitmap,
        editInstruction: String,
        model: HuggingFaceModel = HuggingFaceModel.FLUX_1_SCHNELL,
        customModelId: String = "",
        apiKey: String = "",
        lowTokenMode: Boolean = true
    ): Result<Bitmap> {
        return HuggingFaceImageEngine.editImage(
            inputBitmap = inputBitmap,
            editInstruction = editInstruction,
            model = model,
            customModelId = customModelId,
            apiKey = apiKey,
            lowTokenMode = lowTokenMode
        )
    }

    /**
     * Caches the resulting Bitmap to the local app cache directory and returns a content Uri.
     */
    fun saveBitmapToLocalCache(context: Context, bitmap: Bitmap, prefix: String = "ai_art"): Uri {
        return HuggingFaceImageEngine.saveBitmapToLocalCache(context, bitmap, prefix)
    }
}
