package com.miseservice.smsovh.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miseservice.smsovh.domain.usecase.SendSmsUseCase
import com.miseservice.smsovh.model.SmsMessage
import com.miseservice.smsovh.model.SendResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sendSmsUseCase: SendSmsUseCase
) : ViewModel() {
    private val _sendResult = MutableStateFlow<SendResult?>(null)
    val sendResult: StateFlow<SendResult?> = _sendResult

    fun sendSms(sms: SmsMessage) {
        viewModelScope.launch {
            _sendResult.value = sendSmsUseCase(sms)
        }
    }
}
