package com.eventcalendar.app.ui.screens.menu

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.eventcalendar.app.data.service.ReleaseInfo
import com.eventcalendar.app.data.service.UpdateCheckResult
import com.eventcalendar.app.ui.theme.Primary
import com.eventcalendar.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateCheckScreen(
    navController: NavController,
    viewModel: UpdateCheckViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDownloadDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadCurrentReleaseInfo()
    }
    
    if (showDownloadDialog && uiState.updateResult is UpdateCheckResult.UpdateAvailable) {
        val updateResult = uiState.updateResult as UpdateCheckResult.UpdateAvailable
        DownloadSourceDialog(
            githubUrl = updateResult.githubDownloadUrl,
            giteeUrl = updateResult.giteeDownloadUrl,
            githubLatency = uiState.githubLatency,
            giteeLatency = uiState.giteeLatency,
            isMeasuringLatency = uiState.isMeasuringLatency,
            onDismiss = { showDownloadDialog = false },
            onDownload = { url ->
                showDownloadDialog = false
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
        )
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "检查更新",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CurrentVersionCard(
                currentVersion = uiState.currentVersion,
                releaseInfo = uiState.currentReleaseInfo,
                isLoading = uiState.isLoadingCurrentRelease
            )
            
            UpdateStatusCard(
                isChecking = uiState.isChecking,
                updateResult = uiState.updateResult,
                onDownload = { 
                    showDownloadDialog = true
                    viewModel.measureNetworkLatency()
                },
                onRetry = { viewModel.checkForUpdate() }
            )
            
            uiState.currentReleaseInfo?.let { releaseInfo ->
                ReleaseNotesCard(releaseInfo = releaseInfo)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CurrentVersionCard(
    currentVersion: String,
    releaseInfo: ReleaseInfo?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Primary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "当前版本",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "v$currentVersion",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
        }
    }
}

@Composable
private fun UpdateStatusCard(
    isChecking: Boolean,
    updateResult: UpdateCheckResult?,
    onDownload: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Primary,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "正在检查更新...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                when (val result = updateResult) {
                    is UpdateCheckResult.UpdateAvailable -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(32.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.NewReleases,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "发现新版本",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "v${result.latestVersion}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = onDownload,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary
                            )
                        ) {
                            Icon(
                                Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("下载更新")
                        }
                    }
                    is UpdateCheckResult.NoUpdate -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    RoundedCornerShape(32.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "已是最新版本",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "无需更新",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    is UpdateCheckResult.Error -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    RoundedCornerShape(32.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "检查失败",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = result.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("重试")
                        }
                    }
                    null -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = Primary,
                            strokeWidth = 3.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseNotesCard(
    releaseInfo: ReleaseInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "更新日志",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (releaseInfo.releaseNotes.isNotEmpty()) {
                SelectionContainer {
                    Text(
                        text = parseMarkdown(releaseInfo.releaseNotes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            } else {
                Text(
                    text = "暂无更新说明",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

private fun parseMarkdown(markdown: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.lines()
        
        lines.forEachIndexed { lineIndex, line ->
            var processedLine = line
            var linePrefix = ""
            
            when {
                processedLine.startsWith("### ") -> {
                    linePrefix = ""
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
                        append(processedLine.removePrefix("### "))
                    }
                }
                processedLine.startsWith("## ") -> {
                    linePrefix = ""
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                        append(processedLine.removePrefix("## "))
                    }
                }
                processedLine.startsWith("# ") -> {
                    linePrefix = ""
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                        append(processedLine.removePrefix("# "))
                    }
                }
                processedLine.startsWith("- ") -> {
                    linePrefix = "• "
                    processedLine = processedLine.removePrefix("- ")
                    append(linePrefix)
                    appendInlineFormattedText(processedLine)
                }
                processedLine.startsWith("* ") -> {
                    linePrefix = "• "
                    processedLine = processedLine.removePrefix("* ")
                    append(linePrefix)
                    appendInlineFormattedText(processedLine)
                }
                else -> {
                    appendInlineFormattedText(processedLine)
                }
            }
            
            if (lineIndex < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.appendInlineFormattedText(text: String) {
    val boldPattern = Regex("\\*\\*(.+?)\\*\\*")
    val italicPattern = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
    val codePattern = Regex("`(.+?)`")
    
    data class MatchInfo(
        val range: IntRange,
        val style: SpanStyle,
        val contentStart: Int,
        val contentEnd: Int
    )
    
    val allMatches = mutableListOf<MatchInfo>()
    
    boldPattern.findAll(text).forEach { match ->
        allMatches.add(MatchInfo(
            range = match.range,
            style = SpanStyle(fontWeight = FontWeight.Bold),
            contentStart = match.range.first + 2,
            contentEnd = match.range.last - 1
        ))
    }
    
    italicPattern.findAll(text).forEach { match ->
        allMatches.add(MatchInfo(
            range = match.range,
            style = SpanStyle(fontStyle = FontStyle.Italic),
            contentStart = match.range.first + 1,
            contentEnd = match.range.last - 1
        ))
    }
    
    codePattern.findAll(text).forEach { match ->
        allMatches.add(MatchInfo(
            range = match.range,
            style = SpanStyle(
                background = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.2f),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            ),
            contentStart = match.range.first + 1,
            contentEnd = match.range.last - 1
        ))
    }
    
    if (allMatches.isEmpty()) {
        append(text)
        return
    }
    
    val sortedMatches = allMatches.sortedBy { it.range.first }
    val mergedMatches = mutableListOf<MatchInfo>()
    for (match in sortedMatches) {
        val overlaps = mergedMatches.any { existing ->
            match.range.first < existing.range.last && match.range.last > existing.range.first
        }
        if (!overlaps) {
            mergedMatches.add(match)
        }
    }
    
    var lastEnd = 0
    
    mergedMatches.sortedBy { it.range.first }.forEach { match ->
        if (match.range.first > lastEnd) {
            append(text.substring(lastEnd, match.range.first))
        }
        
        withStyle(match.style) {
            append(text.substring(match.contentStart, match.contentEnd))
        }
        
        lastEnd = match.range.last + 1
    }
    
    if (lastEnd < text.length) {
        append(text.substring(lastEnd))
    }
}

@Composable
private fun DownloadSourceDialog(
    githubUrl: String?,
    giteeUrl: String?,
    githubLatency: Long?,
    giteeLatency: Long?,
    isMeasuringLatency: Boolean,
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit
) {
    var selectedSource by remember { mutableStateOf(if (githubUrl != null) "github" else "gitee") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "选择下载源",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "请选择下载更新的服务器：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (isMeasuringLatency) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "测速中",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (githubUrl != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSource == "github",
                            onClick = { selectedSource = "github" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "GitHub",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                LatencyIndicator(latency = githubLatency, isMeasuring = isMeasuringLatency)
                            }
                            Text(
                                text = "国际线路，推荐海外用户使用",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                
                if (giteeUrl != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSource == "gitee",
                            onClick = { selectedSource = "gitee" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gitee",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                LatencyIndicator(latency = giteeLatency, isMeasuring = isMeasuringLatency)
                            }
                            Text(
                                text = "国内线路，推荐国内用户使用",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val url = when (selectedSource) {
                        "github" -> githubUrl ?: giteeUrl
                        else -> giteeUrl ?: githubUrl
                    }
                    url?.let { onDownload(it) }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("开始下载")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun LatencyIndicator(
    latency: Long?,
    isMeasuring: Boolean
) {
    when {
        isMeasuring -> {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                color = TextSecondary,
                strokeWidth = 1.5.dp
            )
        }
        latency != null -> {
            val color = when {
                latency < 200 -> Color(0xFF4CAF50)
                latency < 500 -> Color(0xFFFFA726)
                else -> Color(0xFFEF5350)
            }
            val status = when {
                latency < 200 -> "极快"
                latency < 500 -> "较快"
                else -> "较慢"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(color, RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${latency}ms $status",
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
        }
        else -> {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFFEF5350), RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "不可用",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFEF5350)
                )
            }
        }
    }
}
