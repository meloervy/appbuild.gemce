package com.appbuild.gemce.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progress: Float,
        val speedMbPerSec: Float = 0f
    ) : DownloadState
    data object Completed : DownloadState
    data class Error(val message: String) : DownloadState
}

object ModelDownloader {

    const val MODEL_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var isCancelled = false

    suspend fun startDownload(context: Context) = withContext(Dispatchers.IO) {
        if (_downloadState.value is DownloadState.Downloading) return@withContext

        isCancelled = false

        // Primary destination is persistent shared storage so it survives app uninstalls
        var targetFile = ModelStorageManager.getPersistentModelFile()
        var targetDir = targetFile.parentFile ?: ModelStorageManager.getPersistentModelDir()

        if (!targetDir.exists() && !targetDir.mkdirs()) {
            // Fallback to internal storage if shared storage cannot be created
            targetFile = ModelStorageManager.getInternalModelFile(context)
            targetDir = targetFile.parentFile ?: File(context.filesDir, "models")
            targetDir.mkdirs()
        }

        val tempFile = File(targetDir, "${ModelStorageManager.MODEL_FILENAME}.tmp")

        try {
            var url = URL(MODEL_URL)
            var connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000

            // Follow redirects if necessary
            var redirectCount = 0
            while (connection.responseCode in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, 307, 308) && redirectCount < 5) {
                val newLocation = connection.getHeaderField("Location")
                url = URL(newLocation)
                connection.disconnect()
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                redirectCount++
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: 2588147712L
            var downloadedBytes = 0L

            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var lastUpdateTime = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L

            _downloadState.value = DownloadState.Downloading(0L, totalBytes, 0f)

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isCancelled) {
                    outputStream.close()
                    inputStream.close()
                    tempFile.delete()
                    _downloadState.value = DownloadState.Idle
                    return@withContext
                }

                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                bytesSinceLastUpdate += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastUpdateTime >= 500) {
                    val durationSec = (now - lastUpdateTime) / 1000f
                    val speed = if (durationSec > 0) (bytesSinceLastUpdate / (1024f * 1024f)) / durationSec else 0f
                    val progress = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)

                    _downloadState.value = DownloadState.Downloading(
                        bytesDownloaded = downloadedBytes,
                        totalBytes = totalBytes,
                        progress = progress,
                        speedMbPerSec = speed
                    )

                    lastUpdateTime = now
                    bytesSinceLastUpdate = 0L
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            // Rename temp file to target file
            if (tempFile.renameTo(targetFile)) {
                _downloadState.value = DownloadState.Completed
            } else {
                _downloadState.value = DownloadState.Error("Failed to finalize downloaded model file.")
            }
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error(e.message ?: "Download failed")
        }
    }

    fun cancelDownload() {
        isCancelled = true
    }
}
