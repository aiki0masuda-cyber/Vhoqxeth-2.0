package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream

private const val TAG = "AiImageEngine"

/**
 * Aspect ratio options for Gemini Nano Banana 2.1 image generation.
 */
enum class ImageAspectRatio(val label: String, val ratioWidth: Float, val ratioHeight: Float) {
    SQUARE_1_1("1:1 Square", 1f, 1f),
    LANDSCAPE_16_9("16:9 Landscape", 16f, 9f),
    PORTRAIT_9_16("9:16 Portrait", 9f, 16f),
    LANDSCAPE_4_3("4:3 Standard", 4f, 3f),
    PORTRAIT_3_4("3:4 Tall", 3f, 4f);

    fun toSdkAspectRatio(): AspectRatio {
        return when (this) {
            SQUARE_1_1 -> AspectRatio.SQUARE_1x1
            LANDSCAPE_16_9 -> AspectRatio.LANDSCAPE_16x9
            PORTRAIT_9_16 -> AspectRatio.PORTRAIT_9x16
            LANDSCAPE_4_3 -> AspectRatio.LANDSCAPE_4x3
            PORTRAIT_3_4 -> AspectRatio.PORTRAIT_3x4
        }
    }
}

/**
 * Service engine that initializes and manages the Firebase AI Logic
 * generative model instance for Gemini image generation (`gemini-nano-banana-2.1`).
 */
object AiImageGeneratorEngine {

    const val MODEL_NAME = "gemini-nano-banana-2.1"

    /**
     * Lazily initialized default square 1:1 generative model instance.
     */
    val defaultModel: GenerativeModel by lazy {
        createModelInstance(ImageAspectRatio.SQUARE_1_1)
    }

    /**
     * Factory function to instantiate a configured GenerativeModel instance
     * with the specified aspect ratio using the Firebase AI Logic SDK.
     */
    fun createModelInstance(aspectRatio: ImageAspectRatio = ImageAspectRatio.SQUARE_1_1): GenerativeModel {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(aspectRatio.toSdkAspectRatio())
                .setImageSize(ImageSize.SIZE_1K)
                .build()
        }
        return Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = config
        )
    }

    /**
     * Generates an image from a natural language text prompt using Gemini Nano Banana.
     * Enforces a 60-second realistic timeout and re-throws [CancellationException].
     */
    suspend fun generateImage(
        prompt: String,
        aspectRatio: ImageAspectRatio = ImageAspectRatio.SQUARE_1_1
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val model = if (aspectRatio == ImageAspectRatio.SQUARE_1_1) defaultModel else createModelInstance(aspectRatio)

            // Realistic 60-second timeout for image generation
            val bitmap = withTimeout(60_000L) {
                val response = model.generateContent(prompt)
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                imagePart?.image
            }

            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("No image data returned from Gemini AI model."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out after 60s", e)
            Result.failure(IllegalStateException("Image generation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Edits or refines an existing Bitmap using prompt instructions (image-to-image prompting).
     */
    suspend fun editImage(
        inputBitmap: Bitmap,
        editInstruction: String
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val bitmap = withTimeout(60_000L) {
                val response = defaultModel.generateContent(
                    content {
                        image(inputBitmap)
                        text(editInstruction)
                    }
                )
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                imagePart?.image
            }

            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("No image data returned from editing session."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image editing timed out after 60s", e)
            Result.failure(IllegalStateException("Image editing timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image editing failed", e)
            Result.failure(e)
        }
    }

    /**
     * Caches the resulting Bitmap to the local app cache directory and returns a content Uri.
     */
    fun saveBitmapToLocalCache(context: Context, bitmap: Bitmap, prefix: String = "ai_art"): Uri {
        val imagesDir = File(context.cacheDir, "generated_images").apply { mkdirs() }
        val imageFile = File(imagesDir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return Uri.fromFile(imageFile)
    }
}
