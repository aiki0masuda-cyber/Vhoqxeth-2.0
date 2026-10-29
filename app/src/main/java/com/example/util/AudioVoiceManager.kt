package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Handles audio recording and playback for voice messages.
 */
class AudioVoiceManager(private val context: Context) {
    private val tag = "AudioVoiceManager"

    // --- Recording State ---
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime = 0L
    private var tickerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    // --- Playback State ---
    private var mediaPlayer: MediaPlayer? = null
    private val _currentlyPlayingId = MutableStateFlow<String?>(null)
    val currentlyPlayingId: StateFlow<String?> = _currentlyPlayingId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private var playbackJob: Job? = null

    /**
     * Starts recording voice note to a cached .m4a file.
     */
    fun startRecording(): Boolean {
        return try {
            stopPlayback()

            val file = File(context.cacheDir, "voice_msg_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDurationSeconds.value = 0

            tickerJob?.cancel()
            tickerJob = scope.launch {
                while (isActive && _isRecording.value) {
                    delay(500)
                    val elapsed = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
                    _recordingDurationSeconds.value = elapsed
                }
            }

            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to start audio recording: ${e.message}", e)
            cancelRecording()
            false
        }
    }

    /**
     * Stops recording and returns the recorded file and duration in seconds.
     */
    fun stopRecording(): Pair<File, Int>? {
        tickerJob?.cancel()
        tickerJob = null

        return try {
            val recorder = mediaRecorder
            mediaRecorder = null
            _isRecording.value = false

            if (recorder != null) {
                try {
                    recorder.stop()
                } catch (e: Exception) {
                    Log.w(tag, "Exception on recorder stop: ${e.message}")
                }
                recorder.release()
            }

            val file = currentOutputFile
            currentOutputFile = null

            val duration = _recordingDurationSeconds.value.coerceAtLeast(1)
            _recordingDurationSeconds.value = 0

            if (file != null && file.exists() && file.length() > 0) {
                Pair(file, duration)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error finalizing recording: ${e.message}", e)
            cancelRecording()
            null
        }
    }

    /**
     * Cancels recording and deletes temporary file.
     */
    fun cancelRecording() {
        tickerJob?.cancel()
        tickerJob = null
        _isRecording.value = false
        _recordingDurationSeconds.value = 0

        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {}
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null

        try {
            currentOutputFile?.let {
                if (it.exists()) it.delete()
            }
        } catch (_: Exception) {}
        currentOutputFile = null
    }

    /**
     * Plays or pauses audio from URL / path for a specific messageId.
     */
    fun togglePlay(messageId: String, audioUrlOrPath: String) {
        if (_currentlyPlayingId.value == messageId) {
            stopPlayback()
            return
        }

        stopPlayback()
        if (audioUrlOrPath.isBlank()) return

        try {
            val player = MediaPlayer()
            player.setDataSource(audioUrlOrPath)
            player.prepareAsync()
            player.setOnPreparedListener { mp ->
                mp.start()
                _currentlyPlayingId.value = messageId
                startProgressTracker(mp)
            }
            player.setOnCompletionListener {
                stopPlayback()
            }
            player.setOnErrorListener { _, what, extra ->
                Log.e(tag, "MediaPlayer error: what=$what, extra=$extra")
                stopPlayback()
                true
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(tag, "Failed to play voice message: ${e.message}", e)
            stopPlayback()
        }
    }

    private fun startProgressTracker(player: MediaPlayer) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (isActive && player.isPlaying) {
                val current = player.currentPosition
                val total = player.duration
                if (total > 0) {
                    _playbackProgress.value = current.toFloat() / total.toFloat()
                }
                delay(100)
            }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _currentlyPlayingId.value = null
        _playbackProgress.value = 0f

        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun release() {
        cancelRecording()
        stopPlayback()
    }

    companion object {
        fun formatDuration(totalSeconds: Int): String {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }
}
