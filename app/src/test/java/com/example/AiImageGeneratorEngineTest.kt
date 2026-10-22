package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiImageGeneratorEngine
import com.example.ai.ImageAspectRatio
import com.google.firebase.ai.type.AspectRatio
import org.junit.Assert.assertEquals
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
    fun testModelNameMatchesNanoBanana() {
        assertEquals("gemini-nano-banana-2.1", AiImageGeneratorEngine.MODEL_NAME)
    }

    @Test
    fun testAspectRatioMapping() {
        assertEquals(AspectRatio.SQUARE_1x1, ImageAspectRatio.SQUARE_1_1.toSdkAspectRatio())
        assertEquals(AspectRatio.LANDSCAPE_16x9, ImageAspectRatio.LANDSCAPE_16_9.toSdkAspectRatio())
        assertEquals(AspectRatio.PORTRAIT_9x16, ImageAspectRatio.PORTRAIT_9_16.toSdkAspectRatio())
        assertEquals(AspectRatio.LANDSCAPE_4x3, ImageAspectRatio.LANDSCAPE_4_3.toSdkAspectRatio())
        assertEquals(AspectRatio.PORTRAIT_3x4, ImageAspectRatio.PORTRAIT_3_4.toSdkAspectRatio())
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
}
