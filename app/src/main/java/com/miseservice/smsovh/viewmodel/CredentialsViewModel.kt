package com.miseservice.smsovh.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miseservice.smsovh.domain.usecase.ClearCredentialsUseCase
import com.miseservice.smsovh.domain.usecase.GetLoginUseCase
import com.miseservice.smsovh.domain.usecase.GetPasswordUseCase
import com.miseservice.smsovh.domain.usecase.SaveCredentialsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CredentialsViewModel @Inject constructor(
    private val saveCredentialsUseCase: SaveCredentialsUseCase,
    private val getLoginUseCase: GetLoginUseCase,
    private val getPasswordUseCase: GetPasswordUseCase,
    private val clearCredentialsUseCase: ClearCredentialsUseCase
) : ViewModel() {
    private val _login = MutableStateFlow<String?>(null)
    val login: StateFlow<String?> = _login

    // ...reste du code...
}
