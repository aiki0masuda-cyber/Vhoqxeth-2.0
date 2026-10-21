package com.example.model

/**
 * Reactive states for the two-step Backblaze B2 pre-signed upload pipeline.
 * Exposes real-time progress and errors to Jetpack Compose UI.
 */
sealed interface UploadState {
    /**
     * No active upload in progress.
     */
    data object Idle : UploadState

    /**
     * Step A: Requesting a short-lived pre-signed PUT URL from the serverless edge gatekeeper.
     */
    data object FetchingTicket : UploadState

    /**
     * Step B: Actively streaming raw file binary directly to the S3 pre-signed URL.
     * @param progress Float between 0.0f and 1.0f.
     * @param bytesWritten Number of bytes successfully transferred to the socket.
     * @param totalBytes Total file size in bytes (-1L if unknown).
     */
    data class Uploading(
        val progress: Float,
        val bytesWritten: Long = 0L,
        val totalBytes: Long = -1L
    ) : UploadState

    /**
     * Upload successfully finished and verified on Backblaze B2.
     * @param downloadUrl Public, permanent URL where the attachment can be downloaded/viewed.
     */
    data class Success(val downloadUrl: String) : UploadState

    /**
     * An error occurred during ticket retrieval or binary streaming.
     * @param message Human-readable error description.
     */
    data class Error(val message: String) : UploadState
}
