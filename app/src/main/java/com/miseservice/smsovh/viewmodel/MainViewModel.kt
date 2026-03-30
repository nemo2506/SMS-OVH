package com.miseservice.smsovh.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miseservice.smsovh.domain.usecase.SendSmsUseCase
import com.miseservice.smsovh.model.SmsMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.miseservice.smsovh.viewmodel.MainUiState

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sendSmsUseCase: SendSmsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun setSenderId(senderId: String) {
        _uiState.value = _uiState.value.copy(senderId = senderId)
    }

    fun setRecipient(recipient: String) {
        _uiState.value = _uiState.value.copy(recipient = recipient)
    }

    fun setMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    fun setServiceActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(serviceActive = active)
    }

    fun setHostIp(ip: String, isValid: Boolean) {
        _uiState.value = _uiState.value.copy(hostIp = ip, isIpValid = isValid)
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(locationPermissionGranted = granted)
    }

    fun setLocationData(location: Pair<Double, Double>?) {
        _uiState.value = _uiState.value.copy(locationData = location)
    }

    fun setNetworkType(type: String) {
        _uiState.value = _uiState.value.copy(networkType = type)
    }

    fun setErrorMessage(message: String?) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun sendSms() {
        val state = _uiState.value
        val sms = SmsMessage(state.senderId, state.recipient, state.message)
        viewModelScope.launch {
            sendSmsUseCase(sms)
            // Optionnel : reset message après envoi
            _uiState.value = _uiState.value.copy(message = "")
        }
    }
}
