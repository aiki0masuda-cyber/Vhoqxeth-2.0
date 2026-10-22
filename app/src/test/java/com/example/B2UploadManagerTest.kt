package com.example

import com.example.data.B2UploadManager
import com.example.model.ChatMessage
import com.example.model.UploadState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class B2UploadManagerTest {

    @Test
    fun uploadStateTransitionsRepresentAllPipelinePhases() {
        val idle: UploadState = UploadState.Idle
        val ticket: UploadState = UploadState.FetchingTicket
        val uploading: UploadState = UploadState.Uploading(progress = 0.5f, bytesWritten = 500L, totalBytes = 1000L)
        val success: UploadState = UploadState.Success("https://s3.backblazeb2.com/test/download")
        val error: UploadState = UploadState.Error("Network timeout")

        assertNotNull(idle)
        assertNotNull(ticket)
        assertEquals(0.5f, (uploading as UploadState.Uploading).progress)
        assertEquals("https://s3.backblazeb2.com/test/download", (success as UploadState.Success).downloadUrl)
        assertEquals("Network timeout", (error as UploadState.Error).message)
    }

    @Test
    fun chatMessageResolvesCorrectAttachmentUrl() {
        val message = ChatMessage(
            messageId = "msg_1",
            content = "Here is my homework",
            temporaryDownloadUrl = "https://s3.backblazeb2.com/chat-attachments/temp_hw.pdf?X-Amz-Signature=abc",
            attachmentUrl = "https://s3.backblazeb2.com/chat-attachments/temp_hw.pdf",
            attachmentKey = "chat-attachments/temp_hw.pdf",
            attachmentName = "hw.pdf",
            attachmentType = "PDF"
        )

        assertTrue(message.hasAttachment)
        assertTrue(message.isS3SignedUrl)
        assertFalse(message.isAttachmentExpired())
        assertEquals("https://s3.backblazeb2.com/chat-attachments/temp_hw.pdf?X-Amz-Signature=abc", message.resolvedAttachmentUrl)
    }

    @Test
    fun chatMessageDetectsExpiredOrUnsignedAttachment() {
        val expiredMessage = ChatMessage(
            messageId = "msg_2",
            content = "Old project",
            temporaryDownloadUrl = null,
            attachmentUrl = "https://s3.backblazeb2.com/chat-attachments/old.pdf",
            attachmentKey = "chat-attachments/old.pdf",
            attachmentName = "old.pdf",
            attachmentType = "PDF"
        )

        assertTrue(expiredMessage.hasAttachment)
        assertFalse(expiredMessage.isS3SignedUrl)
        assertTrue(expiredMessage.isAttachmentExpired())
    }

    @Test
    fun uploadManagerResolvesCachedSignedUrlWithoutNetworkCall(): Unit = runBlocking {
        val manager = B2UploadManager()
        val key = "chat-attachments/test_image.png"
        val signedUrl = "https://s3.us-west-004.backblazeb2.com/bucket/$key?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=xyz"

        // First call caches the signed URL
        val resolvedFirst = manager.getOrRefreshDownloadUrl(key = key, currentUrl = signedUrl)
        assertEquals(signedUrl, resolvedFirst)

        // Subsequent call should immediately retrieve from cache
        val resolvedSecond = manager.getOrRefreshDownloadUrl(key = key, currentUrl = null)
        assertEquals(signedUrl, resolvedSecond)
    }

    @Test
    fun uploadManagerEffectiveEndpointCanBeOverridden() {
        val manager = B2UploadManager()
        assertEquals(B2UploadManager.DEFAULT_WORKER_ENDPOINT, manager.getEffectiveWorkerEndpoint())

        manager.configuredWorkerEndpoint = "https://custom-worker.example.workers.dev"
        assertEquals("https://custom-worker.example.workers.dev", manager.getEffectiveWorkerEndpoint())

        val overridden = manager.getEffectiveWorkerEndpoint("https://explicit-override.example.com")
        assertEquals("https://explicit-override.example.com", overridden)
    }

    @Test
    fun uploadManagerReturnsGracefulFailureWhenWorkerUnreachable(): Unit = runBlocking {
        val manager = B2UploadManager()
        val result = manager.fetchUploadTicket(
            workerEndpoint = "http://127.0.0.1:54321/nonexistent",
            filename = "document.pdf",
            contentType = "application/pdf"
        )
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Cloudflare worker unreachable") == true)
    }
}
