package com.example

import com.example.upload.AttachmentUploadState
import com.example.upload.ChatAttachmentManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatAttachmentManagerTest {

    @Test
    fun testAttachmentUploadStateTransitions() {
        val idle = AttachmentUploadState.Idle
        val fetching = AttachmentUploadState.FetchingTicket
        val uploading = AttachmentUploadState.Uploading(progress = 0.75f, bytesWritten = 750L, totalBytes = 1000L)
        val success = AttachmentUploadState.Success("https://workers.dev/download/file.png")
        val error = AttachmentUploadState.Error("Upload ticket error: Connection refused")

        assertNotNull(idle)
        assertNotNull(fetching)
        assertEquals(75, uploading.percentage)
        assertEquals("https://workers.dev/download/file.png", success.url)
        assertEquals("Upload ticket error: Connection refused", error.msg)
    }

    @Test
    fun testChatAttachmentManagerGracefullyHandlesUnreachableEndpoint(): Unit = runBlocking {
        val manager = ChatAttachmentManager(workerBaseUrl = "http://127.0.0.1:54329/nonexistent")
        val result = manager.fetchUploadUrl("sample.pdf")

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex?.message?.contains("Upload ticket error") == true)
    }

    @Test
    fun testFetchSecureDownloadUrlGracefullyHandlesUnreachableEndpoint(): Unit = runBlocking {
        val manager = ChatAttachmentManager(workerBaseUrl = "http://127.0.0.1:54329/nonexistent")
        val result = manager.fetchSecureDownloadUrl("sample.pdf")

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex?.message?.contains("Download link error") == true)
    }
}
