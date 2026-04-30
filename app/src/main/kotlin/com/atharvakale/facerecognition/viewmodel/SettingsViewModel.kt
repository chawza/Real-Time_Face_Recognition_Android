package com.atharvakale.facerecognition.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atharvakale.facerecognition.data.datastore.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val distanceThreshold = settingsRepository.distanceThreshold.stateIn(
        viewModelScope, SharingStarted.Eagerly, 0.3f
    )

    fun updateThreshold(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateDistanceThreshold(value)
        }
    }
}