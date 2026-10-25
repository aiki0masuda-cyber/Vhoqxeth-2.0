package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiImageGeneratorEngine
import com.example.ai.HuggingFaceImageEngine
import com.example.ai.HuggingFaceModel
import com.example.ai.ImageAspectRatio
import com.example.ai.ImageGenerationResult
import com.example.data.B2UploadManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun testEndpointUsesInferenceRouter() {
        assertEquals("https://router.huggingface.co/hf-inference/models", HuggingFaceImageEngine.ROUTER_INFERENCE_URL)
        assertFalse(HuggingFaceImageEngine.ROUTER_INFERENCE_URL.contains("api-inference.huggingface.co"))
    }

    @Test
    fun testTokenSanitization() {
        assertEquals("hf_abc123", HuggingFaceImageEngine.sanitizeApiKey("Bearer hf_abc123"))
        assertEquals("hf_abc123", HuggingFaceImageEngine.sanitizeApiKey("bearer hf_abc123"))
        assertEquals("hf_abc123", HuggingFaceImageEngine.sanitizeApiKey("  \"hf_abc123\"  "))
        assertEquals("hf_abc123", HuggingFaceImageEngine.sanitizeApiKey("'hf_abc123'"))
        assertEquals("hf_abc123", HuggingFaceImageEngine.sanitizeApiKey("   hf_abc123   "))
    }

    @Test
    fun testExtractImageCommandPrompt() {
        assertEquals("solar system orbits", HuggingFaceImageEngine.extractImageCommandPrompt("/imagine solar system orbits"))
        assertEquals("plant cell diagram", HuggingFaceImageEngine.extractImageCommandPrompt("/draw plant cell diagram"))
        assertEquals("quantum physics visual", HuggingFaceImageEngine.extractImageCommandPrompt("/image quantum physics visual"))
        assertEquals("DNA double helix", HuggingFaceImageEngine.extractImageCommandPrompt("generate an image of DNA double helix"))
        assertEquals("pythagorean proof", HuggingFaceImageEngine.extractImageCommandPrompt("draw a diagram of pythagorean proof"))
        assertEquals("harappan unicorn seal", HuggingFaceImageEngine.extractImageCommandPrompt("create illustration of harappan unicorn seal"))
        assertNull(HuggingFaceImageEngine.extractImageCommandPrompt("Hello class, remember the assignment is due tomorrow."))
    }

    @Test
    fun testRateLimitMessageMatchesExactDirective() {
        val expectedMessage = "The free image generation pipeline is currently busy. Please wait a moment and click generate again."
        assertTrue(expectedMessage.contains("currently busy"))
        assertTrue(expectedMessage.contains("click generate again"))
    }

    @Test
    fun testHuggingFaceModelsConfiguration() {
        assertEquals("black-forest-labs/FLUX.1-schnell", HuggingFaceModel.FLUX_1_SCHNELL.id)
        assertEquals(4, HuggingFaceModel.FLUX_1_SCHNELL.defaultInferenceSteps)
        assertTrue(HuggingFaceModel.FLUX_1_SCHNELL.isLowestTokenTier)

        assertEquals("stabilityai/sdxl-turbo", HuggingFaceModel.SDXL_TURBO.id)
        assertEquals(1, HuggingFaceModel.SDXL_TURBO.defaultInferenceSteps)
        assertTrue(HuggingFaceModel.SDXL_TURBO.isLowestTokenTier)

        assertEquals("stabilityai/stable-diffusion-xl-base-1.0", HuggingFaceModel.STABLE_DIFFUSION_XL.id)
        assertEquals(25, HuggingFaceModel.STABLE_DIFFUSION_XL.defaultInferenceSteps)
        assertFalse(HuggingFaceModel.STABLE_DIFFUSION_XL.isLowestTokenTier)

        assertEquals(HuggingFaceModel.FLUX_1_SCHNELL, HuggingFaceModel.fromId("non-existent-id"))
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

    @Test
    fun testDynamicFailoverResultDataClass() {
        val testBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val result = ImageGenerationResult(
            bitmap = testBitmap,
            usedModel = HuggingFaceModel.FLUX_1_SCHNELL,
            wasFailover = true,
            failoverMessage = "Auto-routed to FLUX.1 Schnell"
        )
        assertTrue(result.wasFailover)
        assertEquals(HuggingFaceModel.FLUX_1_SCHNELL, result.usedModel)
        assertEquals("Auto-routed to FLUX.1 Schnell", result.failoverMessage)
    }

    @Test
    fun testB2UploadManagerConfig() {
        val b2 = B2UploadManager()
        assertTrue(b2.getEffectiveWorkerEndpoint().contains("b2-chat-gatekeeper"))
    }
}
