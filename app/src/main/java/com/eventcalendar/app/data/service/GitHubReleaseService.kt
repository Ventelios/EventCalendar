package com.eventcalendar.app.data.service

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String,
    @SerializedName("body") val body: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("published_at") val publishedAt: String,
    @SerializedName("assets") val assets: List<GitHubAsset>
)

data class GitHubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String,
    @SerializedName("size") val size: Long
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val publishedAt: String
    ) : UpdateCheckResult()
    
    data object NoUpdate : UpdateCheckResult()
    
    data class Error(val message: String) : UpdateCheckResult()
}

class GitHubReleaseService(
    private val owner: String = "Ventelios",
    private val repo: String = "EventCalendar"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext UpdateCheckResult.Error("无法获取版本信息: ${response.code}")
            }
            
            val responseBody = response.body?.string() 
                ?: return@withContext UpdateCheckResult.Error("响应内容为空")
            
            val release = gson.fromJson(responseBody, GitHubRelease::class.java)
            
            val latestVersion = release.tagName.removePrefix("v")
            
            if (compareVersions(latestVersion, currentVersion) > 0) {
                val apkAsset = release.assets.find { 
                    it.name.endsWith(".apk", ignoreCase = true) 
                }
                
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = latestVersion,
                    releaseNotes = release.body,
                    downloadUrl = apkAsset?.downloadUrl ?: release.htmlUrl,
                    publishedAt = release.publishedAt
                )
            } else {
                UpdateCheckResult.NoUpdate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error("检查更新失败: ${e.message}")
        }
    }
    
    private fun compareVersions(version1: String, version2: String): Int {
        val parts1 = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = version2.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxParts = maxOf(parts1.size, parts2.size)
        
        for (i in 0 until maxParts) {
            val v1 = parts1.getOrElse(i) { 0 }
            val v2 = parts2.getOrElse(i) { 0 }
            
            if (v1 > v2) return 1
            if (v1 < v2) return -1
        }
        
        return 0
    }
}
