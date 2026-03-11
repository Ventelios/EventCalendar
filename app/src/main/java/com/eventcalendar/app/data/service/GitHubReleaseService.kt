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
        val githubDownloadUrl: String?,
        val giteeDownloadUrl: String?,
        val publishedAt: String
    ) : UpdateCheckResult()
    
    data object NoUpdate : UpdateCheckResult()
    
    data class Error(val message: String) : UpdateCheckResult()
}

data class NetworkStatus(
    val available: Boolean,
    val latency: Long?
)

data class ReleaseInfo(
    val version: String,
    val releaseNotes: String,
    val githubDownloadUrl: String?,
    val giteeDownloadUrl: String?,
    val publishedAt: String
)

class GitHubReleaseService(
    private val githubOwner: String = "Ventelios",
    private val githubRepo: String = "EventCalendar",
    private val giteeOwner: String = "Ventelios",
    private val giteeRepo: String = "EventCalendar"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    private val githubApkUrlCache = mutableMapOf<String, String>()
    private val giteeApkUrlCache = mutableMapOf<String, String>()
    
    suspend fun getReleaseByVersion(version: String): ReleaseInfo? = withContext(Dispatchers.IO) {
        val githubInfo = getGitHubReleaseByVersion(version)
        val giteeInfo = getGiteeReleaseByVersion(version)
        
        if (githubInfo != null || giteeInfo != null) {
            ReleaseInfo(
                version = githubInfo?.version ?: giteeInfo?.version ?: version,
                releaseNotes = githubInfo?.releaseNotes ?: giteeInfo?.releaseNotes ?: "",
                githubDownloadUrl = githubInfo?.downloadUrl,
                giteeDownloadUrl = giteeInfo?.downloadUrl,
                publishedAt = githubInfo?.publishedAt ?: giteeInfo?.publishedAt ?: ""
            )
        } else {
            null
        }
    }
    
    private data class ReleaseInfoInternal(
        val version: String,
        val releaseNotes: String,
        val downloadUrl: String?,
        val publishedAt: String
    )
    
    private suspend fun getGitHubReleaseByVersion(version: String): ReleaseInfoInternal? = withContext(Dispatchers.IO) {
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
            
            val downloadUrl = apkAsset?.downloadUrl ?: release.htmlUrl
            if (apkAsset != null) {
                githubApkUrlCache[version] = downloadUrl
            }
            
            ReleaseInfoInternal(
                version = release.tagName.removePrefix("v"),
                releaseNotes = release.body,
                downloadUrl = downloadUrl,
                publishedAt = release.publishedAt
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun getGiteeReleaseByVersion(version: String): ReleaseInfoInternal? = withContext(Dispatchers.IO) {
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
            
            val downloadUrl = apkAsset?.downloadUrl ?: release.htmlUrl
            if (apkAsset != null) {
                giteeApkUrlCache[version] = downloadUrl
            }
            
            ReleaseInfoInternal(
                version = release.tagName.removePrefix("v"),
                releaseNotes = release.body,
                downloadUrl = downloadUrl,
                publishedAt = release.createdAt
            )
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        checkForUpdateWithStatus(currentVersion).first
    }
    
    suspend fun checkForUpdateWithStatus(currentVersion: String): Triple<UpdateCheckResult, Boolean?, Boolean?> = withContext(Dispatchers.IO) {
        val githubResult = checkGitHubForUpdate(currentVersion)
        val giteeResult = checkGiteeForUpdate(currentVersion)
        
        val githubAvailable = githubResult != null
        val giteeAvailable = giteeResult != null
        
        val result = when {
            githubResult is UpdateCheckResult.UpdateAvailable || giteeResult is UpdateCheckResult.UpdateAvailable -> {
                val githubUpdate = githubResult as? UpdateCheckResult.UpdateAvailable
                val giteeUpdate = giteeResult as? UpdateCheckResult.UpdateAvailable
                
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = githubUpdate?.latestVersion ?: giteeUpdate?.latestVersion ?: "",
                    releaseNotes = githubUpdate?.releaseNotes ?: giteeUpdate?.releaseNotes ?: "",
                    githubDownloadUrl = githubUpdate?.githubDownloadUrl,
                    giteeDownloadUrl = giteeUpdate?.giteeDownloadUrl,
                    publishedAt = githubUpdate?.publishedAt ?: giteeUpdate?.publishedAt ?: ""
                )
            }
            githubResult is UpdateCheckResult.NoUpdate || giteeResult is UpdateCheckResult.NoUpdate -> {
                UpdateCheckResult.NoUpdate
            }
            else -> UpdateCheckResult.Error("无法获取版本信息，请检查网络连接")
        }
        
        Triple(result, githubAvailable, giteeAvailable)
    }
    
    suspend fun measureNetworkLatency(): Pair<NetworkStatus, NetworkStatus> = withContext(Dispatchers.IO) {
        val githubStatus = measureGithubLatency()
        val giteeStatus = measureGiteeLatency()
        Pair(githubStatus, giteeStatus)
    }
    
    private fun measureGithubLatency(): NetworkStatus {
        return try {
            val startTime = System.currentTimeMillis()
            val url = "https://api.github.com/repos/$githubOwner/$githubRepo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .head()
                .build()
            
            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            
            NetworkStatus(
                available = response.isSuccessful,
                latency = if (response.isSuccessful) latency else null
            )
        } catch (e: Exception) {
            NetworkStatus(available = false, latency = null)
        }
    }
    
    private fun measureGiteeLatency(): NetworkStatus {
        return try {
            val startTime = System.currentTimeMillis()
            val url = "https://gitee.com/api/v5/repos/$giteeOwner/$giteeRepo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .head()
                .build()
            
            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            
            NetworkStatus(
                available = response.isSuccessful,
                latency = if (response.isSuccessful) latency else null
            )
        } catch (e: Exception) {
            NetworkStatus(available = false, latency = null)
        }
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
                    githubDownloadUrl = apkAsset?.downloadUrl,
                    giteeDownloadUrl = null,
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
                    githubDownloadUrl = null,
                    giteeDownloadUrl = apkAsset?.downloadUrl,
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
