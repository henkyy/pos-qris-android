package com.henky.posqris

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val RELEASES_URL = "https://api.github.com/repos/henkyy/pos-qris-android/releases/latest"

internal data class AppUpdate(val versionCode: Int, val versionName: String, val notes: String, val apkUrl: String)

internal object AppUpdateChecker {
    suspend fun check(activity: Activity): AppUpdate? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(RELEASES_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 7000
            connection.readTimeout = 7000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            if (connection.responseCode !in 200..299) return@withContext null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val tag = json.optString("tag_name").removePrefix("v")
            val code = json.optInt("version_code", 0)
            val apk = json.optJSONArray("assets")?.let { assets ->
                (0 until assets.length()).asSequence()
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk", true) }
                    ?.optString("browser_download_url")
            }.orEmpty()
            if (code <= BuildConfig.VERSION_CODE || apk.isBlank()) null
            else AppUpdate(code, tag.ifBlank { "baru" }, json.optString("body", "Pembaruan aplikasi tersedia."), apk)
        } catch (_: Exception) { null }
    }

    fun showUpdateDialog(activity: Activity, update: AppUpdate, onDone: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle("Pembaruan tersedia")
            .setMessage("Versi ${update.versionName} tersedia.\n\n${update.notes.take(600)}")
            .setPositiveButton("Buka pembaruan") { _, _ ->
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.apkUrl)))
                onDone()
            }
            .setNegativeButton("Nanti") { _, _ -> onDone() }
            .setOnCancelListener { onDone() }
            .show()
    }
}
