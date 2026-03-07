package com.eventcalendar.app.ui.screens.datamanagement

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventcalendar.app.data.model.AppBackupData
import com.eventcalendar.app.data.repository.EventRepository
import com.eventcalendar.app.data.repository.ImportResult
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DataManagementUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val importResult: ImportResult? = null,
    val eventTypesCount: Int = 0,
    val eventRecordsCount: Int = 0,
    val lastExportTime: Long? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val repository: EventRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState: StateFlow<DataManagementUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        loadDataStats()
    }

    private fun loadDataStats() {
        viewModelScope.launch {
            repository.getAllEventTypes().collect { types ->
                _uiState.value = _uiState.value.copy(eventTypesCount = types.size)
            }
        }
        viewModelScope.launch {
            repository.getAllRecords().collect { records ->
                _uiState.value = _uiState.value.copy(eventRecordsCount = records.size)
            }
        }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, errorMessage = null)
            
            try {
                val backupData = repository.exportAllData()
                val json = gson.toJson(backupData)
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray(Charsets.UTF_8))
                }
                
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = true,
                    lastExportTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = "导出失败: ${e.message}"
                )
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, errorMessage = null, importResult = null)
            
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes().toString(Charsets.UTF_8)
                } ?: throw Exception("无法读取文件")

                val backupData = gson.fromJson(json, AppBackupData::class.java)
                
                if (backupData.eventTypes.isEmpty() && backupData.eventRecords.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        errorMessage = "备份文件中没有数据"
                    )
                    return@launch
                }

                val result = repository.importAllData(backupData)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importResult = result
                )
            } catch (e: JsonSyntaxException) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    errorMessage = "无效的备份文件格式"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    errorMessage = "导入失败: ${e.message}"
                )
            }
        }
    }

    fun clearImportResult() {
        _uiState.value = _uiState.value.copy(importResult = null)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearExportSuccess() {
        _uiState.value = _uiState.value.copy(exportSuccess = false)
    }

    companion object {
        fun generateExportFileName(): String {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            return "xunji_backup_${dateFormat.format(Date())}.json"
        }
    }
}
