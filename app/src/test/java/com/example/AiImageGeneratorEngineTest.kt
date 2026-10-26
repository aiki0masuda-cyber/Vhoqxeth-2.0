package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiImageGeneratorEngine
import com.example.ai.ImageAspectRatio
import com.example.ai.SiliconFlowImageEngine
import com.example.ai.SiliconFlowModel
import com.example.ai.SiliconFlowResult
import com.example.data.CatboxUploadManager
import org.junit.Assert.assertEquals
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
    fun testCatboxUploadManagerSpecifications() {
        assertEquals("https://catbox.moe/user/api.php", CatboxUploadManager.CATBOX_API_ENDPOINT)
        assertEquals(200 * 1024 * 1024L, CatboxUploadManager.MAX_SIZE_BYTES)
        assertEquals(
            "Upload failed. Please ensure the file size is under 200MB.",
            CatboxUploadManager.SIZE_LIMIT_ALERT
        )
        assertEquals("b38bbe619e7dc69e16d7df563", CatboxUploadManager.getUniversalUserhash())
    }

    @Test
    fun testSiliconFlowEngineModels() {
        assertTrue(AiImageGeneratorEngine.MODEL_NAME.contains("SiliconFlow"))
        assertEquals("https://api.siliconflow.cn/v1/images/generations", SiliconFlowImageEngine.SILICONFLOW_API_URL)

        assertEquals("black-forest-labs/FLUX.1-schnell", SiliconFlowModel.FLUX_1_SCHNELL.id)
        assertTrue(SiliconFlowModel.FLUX_1_SCHNELL.isPrimary)
        assertEquals(4, SiliconFlowModel.FLUX_1_SCHNELL.defaultSteps)

        assertEquals("stabilityai/stable-diffusion-3-medium", SiliconFlowModel.SD_3_MEDIUM.id)
        assertEquals("black-forest-labs/FLUX.1-dev", SiliconFlowModel.FLUX_1_DEV.id)

        assertEquals(SiliconFlowModel.FLUX_1_SCHNELL, SiliconFlowModel.fromId("unknown-model-id"))
    }

    @Test
    fun testSiliconFlowApiKeySanitization() {
        assertEquals("sk-test12345", SiliconFlowImageEngine.sanitizeApiKey("Bearer sk-test12345"))
        assertEquals("sk-test12345", SiliconFlowImageEngine.sanitizeApiKey("bearer sk-test12345"))
        assertEquals("sk-test12345", SiliconFlowImageEngine.sanitizeApiKey("  \"sk-test12345\"  "))
        assertEquals("sk-test12345", SiliconFlowImageEngine.sanitizeApiKey("'sk-test12345'"))
    }

    @Test
    fun testExtractImageCommandPrompt() {
        assertEquals("solar system orbits", SiliconFlowImageEngine.extractImageCommandPrompt("/imagine solar system orbits"))
        assertEquals("plant cell diagram", SiliconFlowImageEngine.extractImageCommandPrompt("/draw plant cell diagram"))
        assertEquals("quantum physics visual", SiliconFlowImageEngine.extractImageCommandPrompt("/image quantum physics visual"))
        assertEquals("DNA double helix", SiliconFlowImageEngine.extractImageCommandPrompt("generate an image of DNA double helix"))
        assertEquals("pythagorean proof", SiliconFlowImageEngine.extractImageCommandPrompt("draw a diagram of pythagorean proof"))
        assertEquals("harappan unicorn seal", SiliconFlowImageEngine.extractImageCommandPrompt("create illustration of harappan unicorn seal"))
        assertNull(SiliconFlowImageEngine.extractImageCommandPrompt("Hello class, remember the assignment is due tomorrow."))
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
    fun testSiliconFlowResultDataClass() {
        val testBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val result = SiliconFlowResult(
            bitmap = testBitmap,
            usedModel = SiliconFlowModel.FLUX_1_SCHNELL,
            wasFailover = true,
            imageUrl = "https://files.catbox.moe/abc123.png"
        )
        assertTrue(result.wasFailover)
        assertEquals(SiliconFlowModel.FLUX_1_SCHNELL, result.usedModel)
        assertEquals("https://files.catbox.moe/abc123.png", result.imageUrl)
    }
}
