package com.appbuild.gemce.ai

import android.content.Context
import android.os.Environment
import java.io.File

object ModelStorageManager {

    const val MODEL_FILENAME = "gemma-4-E2B-it.litertlm"

    /**
     * The primary persistent directory in shared public storage.
     * Files saved here persist across app updates, data clears, and complete app uninstalls.
     */
    fun getPersistentModelDir(): File {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadDir, "Gemce/models").apply { mkdirs() }
    }

    /**
     * Primary persistent file target.
     */
    fun getPersistentModelFile(): File {
        return File(getPersistentModelDir(), MODEL_FILENAME)
    }

    /**
     * Internal private storage directory (fallback or ADB sideload target).
     */
    fun getInternalModelFile(context: Context): File {
        val dir = File(context.filesDir, "models").apply { mkdirs() }
        return File(dir, MODEL_FILENAME)
    }

    /**
     * Returns candidate file locations in order of preference.
     */
    fun getCandidateLocations(context: Context): List<File> {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

        return listOf(
            // 1. Internal app files directory (guaranteed native open permission)
            File(context.filesDir, "models/$MODEL_FILENAME"),
            // 2. External app files dir
            File(context.getExternalFilesDir(null), "models/$MODEL_FILENAME"),
            // 3. Shared Public Download / Gemce / models (persists across uninstalls)
            File(downloadDir, "Gemce/models/$MODEL_FILENAME"),
            // 4. Direct browser download in Downloads folder
            File(downloadDir, MODEL_FILENAME),
            // 5. Shared Documents / Gemce / models
            File(docDir, "Gemce/models/$MODEL_FILENAME")
        )
    }

    /**
     * Finds the first existing and readable model file among all candidate locations.
     */
    fun resolveModelFile(context: Context): File {
        for (candidate in getCandidateLocations(context)) {
            if (candidate.exists() && candidate.canRead() && candidate.length() > 1024 * 1024) { // At least 1 MB
                return candidate
            }
        }
        val internalFallback = File(context.filesDir, "models/$MODEL_FILENAME")
        if (internalFallback.exists() && internalFallback.length() > 1024 * 1024) {
            return internalFallback
        }
        // If not found, return the primary persistent target where it should be downloaded/placed
        return getPersistentModelFile()
    }

    /**
     * Checks if the model exists in any candidate location.
     */
    fun isModelInstalled(context: Context): Boolean {
        val resolved = resolveModelFile(context)
        return resolved.exists() && resolved.canRead() && resolved.length() > 1024 * 1024
    }

    /**
     * Returns a human-friendly description of where the model is currently found.
     */
    fun getModelLocationDescription(context: Context): String {
        val file = resolveModelFile(context)
        return if (file.exists() && file.length() > 0) {
            val sizeMb = file.length() / (1024 * 1024)
            val isPersistent = file.absolutePath.contains("Download") || file.absolutePath.contains("Documents")
            if (isPersistent) {
                "Persistent Shared Storage (Survives uninstalls)\n${file.absolutePath} ($sizeMb MB)"
            } else {
                "App Storage (Ready & Verified)\n${file.absolutePath} ($sizeMb MB)"
            }
        } else {
            "Not installed. Will save to persistent folder:\n${getPersistentModelFile().absolutePath}"
        }
    }
}
