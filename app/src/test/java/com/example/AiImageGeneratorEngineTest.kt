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
import com.example.util.FileAttachmentActionManager
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
        assertEquals("sk-test12345", SiliconFlowImageEngine.sanitizeApiKey(" \n sk-test12345 \t "))
    }

    @Test
    fun testSiliconFlowPlaceholderDetection() {
        assertTrue(SiliconFlowImageEngine.isPlaceholderKey(""))
        assertTrue(SiliconFlowImageEngine.isPlaceholderKey("   "))
        assertTrue(SiliconFlowImageEngine.isPlaceholderKey("sk_placeholder_key"))
        assertTrue(SiliconFlowImageEngine.isPlaceholderKey("SK_PLACEHOLDER_KEY"))
        assertTrue(SiliconFlowImageEngine.isPlaceholderKey("hf_placeholder_key"))
        assertTrue(SiliconFlowImageEngine.isPlaceholderKey("MY_NEW_API_KEY_DEFAULT_VALUE"))
        assertTrue(SiliconFlowImageEngine.isPlaceholderKey("your_api_key_here"))
        assertTrue(SiliconFlowImageEngine.isPlaceholderKey("placeholder"))

        assertFalse(SiliconFlowImageEngine.isPlaceholderKey("sk-valid1234567890abcdef"))
        assertFalse(SiliconFlowImageEngine.isPlaceholderKey("custom_secret_key_prod"))
    }

    @Test
    fun testResolveConfiguredApiKeyNeverReturnsPlaceholder() {
        val resolved = SiliconFlowImageEngine.resolveConfiguredApiKey()
        // If unconfigured or holding a placeholder, it must resolve to empty string instead of dummy string
        assertFalse(SiliconFlowImageEngine.isPlaceholderKey(resolved) && resolved.isNotEmpty())
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

    @Test
    fun testFileAttachmentActionManagerMimeTypes() {
        assertEquals("image/png", com.example.util.FileAttachmentActionManager.getMimeType("image.png"))
        assertEquals("image/jpeg", com.example.util.FileAttachmentActionManager.getMimeType("photo.jpg"))
        assertEquals("image/jpeg", com.example.util.FileAttachmentActionManager.getMimeType("photo.jpeg"))
        assertEquals("image/gif", com.example.util.FileAttachmentActionManager.getMimeType("anim.gif"))
        assertEquals("image/webp", com.example.util.FileAttachmentActionManager.getMimeType("card.webp"))
        assertEquals("application/pdf", com.example.util.FileAttachmentActionManager.getMimeType("document.pdf"))
        assertEquals("video/mp4", com.example.util.FileAttachmentActionManager.getMimeType("lecture.mp4"))
        assertEquals("text/plain", com.example.util.FileAttachmentActionManager.getMimeType("notes.txt"))
        assertEquals("application/zip", com.example.util.FileAttachmentActionManager.getMimeType("archive.zip"))
        assertEquals("image/jpeg", com.example.util.FileAttachmentActionManager.getMimeType("unknown_file", "IMAGE"))
    }

    @Test
    fun testToBypassUrlAndMirrorConversions() {
        assertEquals(
            "https://files.catbox.moe/example.png",
            com.example.util.FileAttachmentActionManager.toBypassUrl("https://de.catbox.moe/example.png")
        )
        assertEquals(
            "https://files.catbox.moe/example.png",
            com.example.util.FileAttachmentActionManager.toBypassUrl("http://de.catbox.moe/example.png")
        )
        assertEquals(
            "https://files.catbox.moe/doc.pdf",
            com.example.util.FileAttachmentActionManager.toBypassUrl("http://files.catbox.moe/doc.pdf")
        )
        assertEquals(
            "https://files.catbox.moe/doc.pdf",
            com.example.util.FileAttachmentActionManager.toBypassUrl("https://files.catbox.moe/doc.pdf")
        )
        assertEquals(
            "https://files.catbox.moe/sample.jpg",
            CatboxUploadManager.applyBypassMirror("http://files.catbox.moe/sample.jpg")
        )
        assertEquals(
            "https://files.catbox.moe/sample.jpg",
            CatboxUploadManager.applyBypassMirror("https://de.catbox.moe/sample.jpg")
        )
        assertEquals(
            "https://example.com/other.png",
            com.example.util.FileAttachmentActionManager.toBypassUrl("https://example.com/other.png")
        )
    }

    @Test
    fun testFileProviderUriGeneration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testFile = File(context.cacheDir, "test_download.pdf").apply {
            writeText("dummy content")
        }
        val uri = com.example.util.FileAttachmentActionManager.getFileProviderUri(context, testFile)
        assertNotNull(uri)
        assertTrue(uri.scheme == "content" || uri.scheme == "file")
    }

    @Test
    fun testCatboxExtractCleanUrl() {
        // Standard clean HTTPS URL
        assertEquals(
            "https://files.catbox.moe/abc123.jpg",
            CatboxUploadManager.extractCleanUrl("https://files.catbox.moe/abc123.jpg")
        )
        // With trailing newlines and carriage return
        assertEquals(
            "https://files.catbox.moe/abc123.jpg",
            CatboxUploadManager.extractCleanUrl("https://files.catbox.moe/abc123.jpg\r\n")
        )
        // With quotes
        assertEquals(
            "https://files.catbox.moe/abc123.jpg",
            CatboxUploadManager.extractCleanUrl("\"https://files.catbox.moe/abc123.jpg\"")
        )
        // Root domain catbox.moe mapped to files.catbox.moe to prevent 404
        assertEquals(
            "https://files.catbox.moe/abc123.jpg",
            CatboxUploadManager.extractCleanUrl("https://catbox.moe/abc123.jpg")
        )
        // Insecure HTTP mapped to secure HTTPS
        assertEquals(
            "https://files.catbox.moe/abc123.jpg",
            CatboxUploadManager.extractCleanUrl("http://files.catbox.moe/abc123.jpg")
        )
        // Insecure HTTP on root domain
        assertEquals(
            "https://files.catbox.moe/abc123.jpg",
            CatboxUploadManager.extractCleanUrl("http://catbox.moe/abc123.jpg")
        )
        // Mirror domain de.catbox.moe
        assertEquals(
            "https://files.catbox.moe/abc123.jpg",
            CatboxUploadManager.extractCleanUrl("https://de.catbox.moe/abc123.jpg")
        )
        // Bare filename/hash returned directly
        assertEquals(
            "https://files.catbox.moe/abc123.png",
            CatboxUploadManager.extractCleanUrl("abc123.png")
        )
        // HTML response body wrapper
        assertEquals(
            "https://files.catbox.moe/abc123.pdf",
            CatboxUploadManager.extractCleanUrl("<pre>https://files.catbox.moe/abc123.pdf</pre>")
        )
        // Error response returns null
        assertEquals(
            null,
            CatboxUploadManager.extractCleanUrl("Error: File too large")
        )
        assertEquals(
            null,
            CatboxUploadManager.extractCleanUrl("")
        )
    }

    @Test
    fun testClassifiedDownloadSubdirectories() {
        assertEquals("DAV InfoExchange", FileAttachmentActionManager.APP_DOWNLOAD_DIR_NAME)
        assertEquals("Images", FileAttachmentActionManager.CATEGORY_IMAGES)
        assertEquals("Documents", FileAttachmentActionManager.CATEGORY_DOCUMENTS)
        assertEquals("Videos", FileAttachmentActionManager.CATEGORY_VIDEOS)
        assertEquals("Audio", FileAttachmentActionManager.CATEGORY_AUDIO)
        assertEquals("Other", FileAttachmentActionManager.CATEGORY_OTHER)

        // Images classification
        assertEquals("Images", FileAttachmentActionManager.classifyFileCategory("science_lab.png", "image/png"))
        assertEquals("Images", FileAttachmentActionManager.classifyFileCategory("photo.jpg", ""))
        assertEquals("Images", FileAttachmentActionManager.classifyFileCategory("banner.webp", "image/webp"))
        assertEquals("Images", FileAttachmentActionManager.classifyFileCategory("animation.gif", ""))

        // Documents classification
        assertEquals("Documents", FileAttachmentActionManager.classifyFileCategory("syllabus.pdf", "application/pdf"))
        assertEquals("Documents", FileAttachmentActionManager.classifyFileCategory("assignment.docx", ""))
        assertEquals("Documents", FileAttachmentActionManager.classifyFileCategory("grades.xlsx", ""))
        assertEquals("Documents", FileAttachmentActionManager.classifyFileCategory("lecture_notes.txt", "text/plain"))
        assertEquals("Documents", FileAttachmentActionManager.classifyFileCategory("data.csv", ""))

        // Videos classification
        assertEquals("Videos", FileAttachmentActionManager.classifyFileCategory("experiment.mp4", "video/mp4"))
        assertEquals("Videos", FileAttachmentActionManager.classifyFileCategory("annual_day.mkv", ""))
        assertEquals("Videos", FileAttachmentActionManager.classifyFileCategory("clip.webm", ""))

        // Audio classification
        assertEquals("Audio", FileAttachmentActionManager.classifyFileCategory("prayer_song.mp3", "audio/mpeg"))
        assertEquals("Audio", FileAttachmentActionManager.classifyFileCategory("recording.wav", ""))
        assertEquals("Audio", FileAttachmentActionManager.classifyFileCategory("voice_note.m4a", ""))

        // Other / Archives / Unknown
        assertEquals("Other", FileAttachmentActionManager.classifyFileCategory("archive.zip", "application/zip"))
        assertEquals("Other", FileAttachmentActionManager.classifyFileCategory("backup.tar.gz", ""))
        assertEquals("Other", FileAttachmentActionManager.classifyFileCategory("installer.bin", "application/octet-stream"))
    }
}
