package com.miseservice.smsovh.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miseservice.smsovh.domain.usecase.SendSmsUseCase
import com.miseservice.smsovh.model.SmsMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sendSmsUseCase: SendSmsUseCase
) : ViewModel() {
    fun sendSms(sms: SmsMessage) {
        viewModelScope.launch {
            sendSmsUseCase(sms)
        }
    }
}
