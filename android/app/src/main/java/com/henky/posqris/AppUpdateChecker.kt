package com.henky.posqris

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val RELEASES_URL = "https://api.github.com/repos/henkyy/pos-qris-android/releases/latest"

internal data class AppUpdate(
    val versionCode: Int,
    val versionName: String,
    val notes: String,
    val apkUrl: String
)

internal object AppUpdateChecker {
    suspend fun check(activity: Activity): AppUpdate? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(RELEASES_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 7000
            connection.readTimeout = 7000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            if (connection.responseCode !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name").removePrefix("v")
            val versionCode = json.optInt("version_code", 0)
            val versionName = if (tag.isNotBlank()) tag else json.optString("name", "")
            val apkUrl = json.optJSONArray("assets")?.let { assets ->
                (0 until assets.length()).asSequence()
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk", true) }
                    ?.optString("browser_download_url")
            }.orEmpty()
            if (versionCode <= BuildConfig.VERSION_CODE || apkUrl.isBlank()) return@withContext null
            AppUpdate(versionCode, versionName, json.optString("body", "Pembaruan aplikasi tersedia."), apkUrl)
        } catch (_: Exception) {
            null
        }
    }

    fun showUpdateDialog(activity: Activity, update: AppUpdate, onDone: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle("Pembaruan tersedia")
            .setMessage("Versi ${update.versionName} tersedia.\n\n${update.notes.take(600)}")
            .setPositiveButton("Perbarui") { _, _ ->
                activity.lifecycleScope.launchWhenStarted {
                    try {
                        val apk = download(activity, update.apkUrl)
                        install(activity, apk)
                    } catch (_: Exception) {
                        onDone()
                    }
                }
            }
            .setNegativeButton("Nanti") { _, _ -> onDone() }
            .setOnCancelListener { onDone() }
            .show()
    }

    private suspend fun download(activity: Activity, apkUrl: String): File = withContext(Dispatchers.IO) {
        val target = File(activity.cacheDir, "pos-qris-update.apk")
        if (target.exists()) target.delete()
        val connection = URL(apkUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = true
        connection.connect()
        if (connection.responseCode !in 200..299) error("Download update gagal")
        connection.inputStream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        target
    }

    private fun install(activity: Activity, apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
            activity.startActivity(intent)
            return
        }
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }
}
