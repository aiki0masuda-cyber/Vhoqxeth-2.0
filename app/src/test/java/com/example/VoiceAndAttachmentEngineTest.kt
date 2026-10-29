package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CatboxUploadManager
import com.example.model.ChatMessage
import com.example.util.AudioVoiceManager
import com.example.util.FileAttachmentActionManager
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
class VoiceAndAttachmentEngineTest {

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
    fun testFileAttachmentActionManagerMimeTypes() {
        assertEquals("image/png", FileAttachmentActionManager.getMimeType("image.png"))
        assertEquals("image/jpeg", FileAttachmentActionManager.getMimeType("photo.jpg"))
        assertEquals("image/jpeg", FileAttachmentActionManager.getMimeType("photo.jpeg"))
        assertEquals("image/gif", FileAttachmentActionManager.getMimeType("anim.gif"))
        assertEquals("image/webp", FileAttachmentActionManager.getMimeType("card.webp"))
        assertEquals("application/pdf", FileAttachmentActionManager.getMimeType("document.pdf"))
        assertEquals("video/mp4", FileAttachmentActionManager.getMimeType("lecture.mp4"))
        assertEquals("text/plain", FileAttachmentActionManager.getMimeType("notes.txt"))
        assertEquals("application/zip", FileAttachmentActionManager.getMimeType("archive.zip"))
        assertEquals("image/jpeg", FileAttachmentActionManager.getMimeType("unknown_file", "IMAGE"))
    }

    @Test
    fun testToBypassUrlAndMirrorConversions() {
        assertEquals(
            "https://files.catbox.moe/example.png",
            FileAttachmentActionManager.toBypassUrl("https://de.catbox.moe/example.png")
        )
        assertEquals(
            "https://files.catbox.moe/example.png",
            FileAttachmentActionManager.toBypassUrl("http://de.catbox.moe/example.png")
        )
        assertEquals(
            "https://files.catbox.moe/doc.pdf",
            FileAttachmentActionManager.toBypassUrl("http://files.catbox.moe/doc.pdf")
        )
        assertEquals(
            "https://files.catbox.moe/doc.pdf",
            FileAttachmentActionManager.toBypassUrl("https://files.catbox.moe/doc.pdf")
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
            FileAttachmentActionManager.toBypassUrl("https://example.com/other.png")
        )
    }

    @Test
    fun testFileProviderUriGeneration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testFile = File(context.cacheDir, "test_download.pdf").apply {
            writeText("dummy content")
        }
        val uri = FileAttachmentActionManager.getFileProviderUri(context, testFile)
        assertNotNull(uri)
        assertTrue(uri.scheme == "content" || uri.scheme == "file")
    }

    @Test
    fun testCatboxExtractCleanUrl() {
        assertEquals(
            "https://files.catbox.moe/abc123.jpg",
            CatboxUploadManager.extractCleanUrl("https://files.catbox.moe/abc123.jpg")
        )
        assertEquals(
            "https://files.catbox.moe/abc123.jpg",
            CatboxUploadManager.extractCleanUrl("https://files.catbox.moe/abc123.jpg\r\n")
        )
        assertEquals(
            "https://files.catbox.moe/abc123.jpg",
            CatboxUploadManager.extractCleanUrl("\"https://files.catbox.moe/abc123.jpg\"")
        )
        assertEquals(
            "https://files.catbox.moe/abc123.jpg",
            CatboxUploadManager.extractCleanUrl("https://catbox.moe/abc123.jpg")
        )
        assertEquals(
            "https://files.catbox.moe/abc123.jpg",
            CatboxUploadManager.extractCleanUrl("http://files.catbox.moe/abc123.jpg")
        )
        assertEquals(
            "https://files.catbox.moe/abc123.jpg",
            CatboxUploadManager.extractCleanUrl("http://catbox.moe/abc123.jpg")
        )
        assertEquals(
            "https://files.catbox.moe/abc123.jpg",
            CatboxUploadManager.extractCleanUrl("https://de.catbox.moe/abc123.jpg")
        )
        assertEquals(
            "https://files.catbox.moe/abc123.png",
            CatboxUploadManager.extractCleanUrl("abc123.png")
        )
        assertEquals(
            "https://files.catbox.moe/abc123.pdf",
            CatboxUploadManager.extractCleanUrl("<pre>https://files.catbox.moe/abc123.pdf</pre>")
        )
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

    @Test
    fun testVoiceMessageModelAndDuration() {
        val voiceMsg = ChatMessage(
            messageId = "voice_123",
            attachmentType = "VOICE",
            attachmentName = "voice_note_1.m4a",
            attachmentUri = "https://files.catbox.moe/voice123.m4a",
            voiceDurationSeconds = 14
        )
        assertTrue(voiceMsg.isVoiceMessage)
        assertEquals(14, voiceMsg.voiceDurationSeconds)

        val textMsg = ChatMessage(
            messageId = "txt_123",
            content = "Standard chat message"
        )
        assertFalse(textMsg.isVoiceMessage)

        // Duration formatting
        assertEquals("00:00", AudioVoiceManager.formatDuration(0))
        assertEquals("00:14", AudioVoiceManager.formatDuration(14))
        assertEquals("01:05", AudioVoiceManager.formatDuration(65))
        assertEquals("02:30", AudioVoiceManager.formatDuration(150))
    }
}
