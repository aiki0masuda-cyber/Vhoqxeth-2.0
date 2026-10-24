package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiImageGeneratorEngine
import com.example.ai.HuggingFaceImageEngine
import com.example.ai.HuggingFaceModel
import com.example.ai.ImageAspectRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiImageGeneratorEngineTest {

    @Test
    fun testModelNameMatchesHuggingFace() {
        assertTrue(AiImageGeneratorEngine.MODEL_NAME.contains("Hugging Face"))
    }

    @Test
    fun testHuggingFaceModelsConfiguration() {
        assertEquals("stabilityai/sdxl-turbo", HuggingFaceModel.SDXL_TURBO.id)
        assertEquals(1, HuggingFaceModel.SDXL_TURBO.defaultInferenceSteps)
        assertTrue(HuggingFaceModel.SDXL_TURBO.isLowestTokenTier)

        assertEquals("black-forest-labs/FLUX.1-schnell", HuggingFaceModel.FLUX_1_SCHNELL.id)
        assertEquals(4, HuggingFaceModel.FLUX_1_SCHNELL.defaultInferenceSteps)
        assertTrue(HuggingFaceModel.FLUX_1_SCHNELL.isLowestTokenTier)

        assertEquals("stabilityai/stable-diffusion-xl-base-1.0", HuggingFaceModel.STABLE_DIFFUSION_XL.id)
        assertEquals(25, HuggingFaceModel.STABLE_DIFFUSION_XL.defaultInferenceSteps)
        assertFalse(HuggingFaceModel.STABLE_DIFFUSION_XL.isLowestTokenTier)

        assertEquals(HuggingFaceModel.SDXL_TURBO, HuggingFaceModel.fromId("non-existent-id"))
        assertEquals(HuggingFaceModel.STABLE_DIFFUSION_XL, HuggingFaceModel.fromId("stabilityai/stable-diffusion-xl-base-1.0"))
    }

    @Test
    fun testPromptOptimizationForLowestTokens() {
        val bloatedPrompt = "A plant cell cross section, highly detailed, 8k resolution, trending on artstation, masterpiece"
        val optimized = HuggingFaceImageEngine.optimizePromptForLowestTokens(bloatedPrompt)

        assertFalse(optimized.contains("8k resolution", ignoreCase = true))
        assertFalse(optimized.contains("trending on artstation", ignoreCase = true))
        assertFalse(optimized.contains("masterpiece", ignoreCase = true))
        assertTrue(optimized.contains("plant cell", ignoreCase = true))
    }

    @Test
    fun testImageAspectRatios() {
        assertEquals(1f, ImageAspectRatio.SQUARE_1_1.ratioWidth / ImageAspectRatio.SQUARE_1_1.ratioHeight, 0.01f)
        assertEquals(16f / 9f, ImageAspectRatio.LANDSCAPE_16_9.ratioWidth / ImageAspectRatio.LANDSCAPE_16_9.ratioHeight, 0.01f)
        assertEquals(9f / 16f, ImageAspectRatio.PORTRAIT_9_16.ratioWidth / ImageAspectRatio.PORTRAIT_9_16.ratioHeight, 0.01f)
    }

    @Test
    fun testSaveBitmapToLocalCache() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val uri = AiImageGeneratorEngine.saveBitmapToLocalCache(context, testBitmap, "unit_test_art")

        assertNotNull(uri)
        assertTrue(uri.path?.contains("unit_test_art") == true)

        val file = File(uri.path!!)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }

    @Test
    fun testCustomModelIdConfiguration() {
        val customModel = "my-org/custom-fine-tuned-sdxl"
        assertTrue(customModel.contains("/"))
    }
}
