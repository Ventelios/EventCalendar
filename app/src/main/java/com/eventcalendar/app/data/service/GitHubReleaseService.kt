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

data class GiteeRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String,
    @SerializedName("body") val body: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("assets") val assets: List<GiteeAsset>
)

data class GiteeAsset(
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

data class ReleaseInfo(
    val version: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val publishedAt: String
)

class GitHubReleaseService(
    private val githubOwner: String = "Ventelios",
    private val githubRepo: String = "EventCalendar",
    private val giteeOwner: String = "ventelios",
    private val giteeRepo: String = "EventCalendar"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    suspend fun getReleaseByVersion(version: String): ReleaseInfo? = withContext(Dispatchers.IO) {
        getGitHubReleaseByVersion(version) ?: getGiteeReleaseByVersion(version)
    }
    
    private suspend fun getGitHubReleaseByVersion(version: String): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$githubOwner/$githubRepo/releases/tags/v$version"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext null
            }
            
            val responseBody = response.body?.string() ?: return@withContext null
            val release = gson.fromJson(responseBody, GitHubRelease::class.java)
            
            val apkAsset = release.assets.find { 
                it.name.endsWith(".apk", ignoreCase = true) 
            }
            
            ReleaseInfo(
                version = release.tagName.removePrefix("v"),
                releaseNotes = release.body,
                downloadUrl = apkAsset?.downloadUrl ?: release.htmlUrl,
                publishedAt = release.publishedAt
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun getGiteeReleaseByVersion(version: String): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://gitee.com/api/v5/repos/$giteeOwner/$giteeRepo/releases/tags/v$version"
            val request = Request.Builder().url(url).build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext null
            }
            
            val responseBody = response.body?.string() ?: return@withContext null
            val release = gson.fromJson(responseBody, GiteeRelease::class.java)
            
            val apkAsset = release.assets.find { 
                it.name.endsWith(".apk", ignoreCase = true) 
            }
            
            ReleaseInfo(
                version = release.tagName.removePrefix("v"),
                releaseNotes = release.body,
                downloadUrl = apkAsset?.downloadUrl ?: release.htmlUrl,
                publishedAt = release.createdAt
            )
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        checkGitHubForUpdate(currentVersion) ?: checkGiteeForUpdate(currentVersion) 
            ?: UpdateCheckResult.Error("无法获取版本信息，请检查网络连接")
    }
    
    private suspend fun checkGitHubForUpdate(currentVersion: String): UpdateCheckResult? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$githubOwner/$githubRepo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext null
            }
            
            val responseBody = response.body?.string() ?: return@withContext null
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
            null
        }
    }
    
    private suspend fun checkGiteeForUpdate(currentVersion: String): UpdateCheckResult? = withContext(Dispatchers.IO) {
        try {
            val url = "https://gitee.com/api/v5/repos/$giteeOwner/$giteeRepo/releases/latest"
            val request = Request.Builder().url(url).build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext null
            }
            
            val responseBody = response.body?.string() ?: return@withContext null
            val release = gson.fromJson(responseBody, GiteeRelease::class.java)
            
            val latestVersion = release.tagName.removePrefix("v")
            
            if (compareVersions(latestVersion, currentVersion) > 0) {
                val apkAsset = release.assets.find { 
                    it.name.endsWith(".apk", ignoreCase = true) 
                }
                
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = latestVersion,
                    releaseNotes = release.body,
                    downloadUrl = apkAsset?.downloadUrl ?: release.htmlUrl,
                    publishedAt = release.createdAt
                )
            } else {
                UpdateCheckResult.NoUpdate
            }
        } catch (e: Exception) {
            null
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
