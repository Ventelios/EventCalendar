package com.eventcalendar.app.ui.screens.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventcalendar.app.BuildConfig
import com.eventcalendar.app.data.service.GitHubReleaseService
import com.eventcalendar.app.data.service.ReleaseInfo
import com.eventcalendar.app.data.service.UpdateCheckResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateCheckUiState(
    val isChecking: Boolean = false,
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val currentReleaseInfo: ReleaseInfo? = null,
    val isLoadingCurrentRelease: Boolean = false,
    val updateResult: UpdateCheckResult? = null,
    val lastCheckTime: Long? = null
)

@HiltViewModel
class UpdateCheckViewModel @Inject constructor(
    private val releaseService: GitHubReleaseService
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateCheckUiState())
    val uiState: StateFlow<UpdateCheckUiState> = _uiState.asStateFlow()

    fun loadCurrentReleaseInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCurrentRelease = true)
            
            val releaseInfo = releaseService.getReleaseByVersion(BuildConfig.VERSION_NAME)
            
            _uiState.value = _uiState.value.copy(
                currentReleaseInfo = releaseInfo,
                isLoadingCurrentRelease = false
            )
            
            checkForUpdate()
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isChecking = true,
                updateResult = null
            )
            
            val result = releaseService.checkForUpdate(BuildConfig.VERSION_NAME)
            
            _uiState.value = _uiState.value.copy(
                isChecking = false,
                updateResult = result,
                lastCheckTime = System.currentTimeMillis()
            )
        }
    }

    fun clearUpdateResult() {
        _uiState.value = _uiState.value.copy(updateResult = null)
    }
}
