package com.example.upload

/**
 * Represents the state lifecycle of an active file upload to Cloudflare R2 / S3 storage.
 * Designed to be collected as Compose state in the UI.
 */
sealed interface UploadState {
    /**
     * Initial or idle state before upload starts.
     */
    data object Idle : UploadState

    /**
     * Contacting the Cloudflare Worker gatekeeper endpoint to obtain a pre-signed PUT URL.
     */
    data object RequestingUrl : UploadState

    /**
     * Actively streaming the file binary to the pre-signed R2 URL.
     * @param progress Progress fraction from 0.0f to 1.0f.
     * @param bytesWritten Number of bytes successfully sent over the wire.
     * @param totalBytes Total size of the file in bytes (-1 if unknown).
     */
    data class Uploading(
        val progress: Float,
        val bytesWritten: Long = 0L,
        val totalBytes: Long = 0L
    ) : UploadState {
        val percentage: Int
            get() = (progress * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Binary was successfully written to Cloudflare R2.
     * @param downloadUrl Public CDN or bucket URL of the uploaded asset.
     */
    data class Success(
        val downloadUrl: String
    ) : UploadState

    /**
     * Upload failed due to a network interruption, timeout, or server error.
     * @param message Human-readable error description.
     * @param canRetry Whether the user can retry the upload.
     */
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : UploadState
}
