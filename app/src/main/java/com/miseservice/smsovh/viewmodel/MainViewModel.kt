package com.miseservice.smsovh.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miseservice.smsovh.domain.usecase.SendSmsUseCase
import com.miseservice.smsovh.domain.usecase.SendApiMessageUseCase
import com.miseservice.smsovh.domain.usecase.GetApiUrlUseCase
import com.miseservice.smsovh.domain.usecase.SaveApiUrlUseCase
import com.miseservice.smsovh.model.SmsMessage
import com.miseservice.smsovh.model.SendResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.miseservice.smsovh.domain.repository.ApiSendResult

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sendSmsUseCase: SendSmsUseCase,
    private val sendApiMessageUseCase: SendApiMessageUseCase,
    private val getApiUrlUseCase: GetApiUrlUseCase,
    private val saveApiUrlUseCase: SaveApiUrlUseCase
) : ViewModel() {
    private val _sendResult = MutableStateFlow<SendResult?>(null)
    val sendResult: StateFlow<SendResult?> = _sendResult

    private val _apiLogs = MutableStateFlow<String?>(null)
    val apiLogs: StateFlow<String?> = _apiLogs

    private val _apiUrl = MutableStateFlow<String>(getApiUrlUseCase())
    val apiUrl: StateFlow<String> = _apiUrl

    fun sendSms(sms: SmsMessage) {
        viewModelScope.launch {
            _sendResult.value = sendSmsUseCase(sms)
        }
    }

    fun sendApiMessage(apiUrl: String, token: String, senderId: String, recipient: String, message: String) {
        viewModelScope.launch {
            when(val result = sendApiMessageUseCase(apiUrl, token, senderId, recipient, message)) {
                is ApiSendResult.Success -> {
                    _apiLogs.value = result.logs
                }
                is ApiSendResult.Error -> {
                    _apiLogs.value = result.logs
                }
            }
        }
    }

    fun saveApiUrl(url: String) {
        saveApiUrlUseCase(url)
        _apiUrl.value = url
    }
}
