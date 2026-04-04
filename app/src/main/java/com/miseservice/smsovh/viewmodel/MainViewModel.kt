package com.miseservice.smsovh.viewmodel

import android.content.Context
import com.miseservice.smsovh.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miseservice.smsovh.domain.usecase.SendSmsUseCase
import com.miseservice.smsovh.domain.usecase.GetSettingsUseCase
import com.miseservice.smsovh.domain.usecase.UpdateRestPortUseCase
import com.miseservice.smsovh.model.SmsMessage
import com.miseservice.smsovh.model.SendResult
import com.miseservice.smsovh.data.repository.SettingsRepository
import com.miseservice.smsovh.service.ServiceControlManager
import com.miseservice.smsovh.util.ApiTokenManager
import com.miseservice.smsovh.util.RestServerEventManager
import com.miseservice.smsovh.util.RestServerEventType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sendSmsUseCase: SendSmsUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateRestPortUseCase: UpdateRestPortUseCase,
    private val settingsRepository: SettingsRepository,
    private val restServerEventManager: RestServerEventManager,
    private val serviceControlManager: ServiceControlManager,
    private val serviceToggleProcessor: ServiceToggleProcessor
) : ViewModel() {
    private var saveSettingsJob: Job? = null
    private var lastAppliedServiceActive: Boolean? = null

    private companion object {
        const val SETTINGS_SAVE_DEBOUNCE_MS = 500L
        const val DEFAULT_REST_PORT = 8080
    }

    private fun isHostIpUsable(ip: String): Boolean {
        return ip.isNotBlank() && ip != "127.0.0.1"
    }

    private fun isPortValid(port: Int): Boolean = port in 1..65535

    private fun parsePortOrNull(portText: String): Int? {
        val normalized = portText.trim()
        if (normalized.isBlank()) return null
        val port = normalized.toIntOrNull() ?: return null
        return port.takeIf { isPortValid(it) }
    }

    private fun persistCurrentSettingsNow() {
        val state = _uiState.value
        val currentToken = _token.value
        viewModelScope.launch {
            val entity = com.miseservice.smsovh.data.local.AppSettingsEntity(
                senderId = state.senderId,
                recipient = state.recipient,
                message = state.message,
                serviceActive = state.serviceActive,
                hostIp = state.hostIp,
                restPort = state.restPort,
                token = currentToken
            )
            settingsRepository.saveSettings(entity)
        }
    }

    private fun schedulePersistCurrentSettings() {
        saveSettingsJob?.cancel()
        saveSettingsJob = viewModelScope.launch {
            delay(SETTINGS_SAVE_DEBOUNCE_MS)
            persistCurrentSettingsNow()
        }
    }

    private fun applyServiceActiveState(active: Boolean) {
        if (lastAppliedServiceActive == active) return

        if (active) {
            serviceControlManager.start()
        } else {
            serviceControlManager.stop()
        }

        lastAppliedServiceActive = active
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token.asStateFlow()

    init {
        viewModelScope.launch {
            val secureToken = ApiTokenManager.getToken(context)
            var settings = getSettingsUseCase()
            if (settings == null) {
                settings = com.miseservice.smsovh.data.local.AppSettingsEntity(
                    senderId = null,
                    recipient = null,
                    message = null,
                    serviceActive = false,
                    hostIp = null,
                    restPort = DEFAULT_REST_PORT,
                    token = secureToken
                )
                settingsRepository.saveSettings(settings)
            } else {
                val storedToken = settings.token.orEmpty()
                if (storedToken != secureToken) {
                    settingsRepository.updateToken(secureToken)
                }
            }

            settingsRepository.observeSettings().collect { observed ->
                val currentSettings = observed ?: return@collect
                val host = currentSettings.hostIp ?: _uiState.value.hostIp
                val restPort = currentSettings.restPort.takeIf { isPortValid(it) } ?: DEFAULT_REST_PORT
                _token.value = currentSettings.token ?: secureToken
                runCatching {
                    applyServiceActiveState(currentSettings.serviceActive)
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        feedbackMessage = "❌ Service indisponible: ${error.message}",
                        feedbackType = FeedbackType.ERROR
                    )
                }
                _uiState.value = _uiState.value.copy(
                    senderId = currentSettings.senderId.orEmpty(),
                    recipient = currentSettings.recipient.orEmpty(),
                    message = currentSettings.message.orEmpty(),
                    serviceActive = currentSettings.serviceActive,
                    hostIp = host,
                    restPort = restPort,
                    restPortInput = restPort.toString(),
                    restPortError = null,
                    isIpValid = isHostIpUsable(host)
                )
            }
        }

        // Écoute des événements du serveur REST
        viewModelScope.launch {
            restServerEventManager.eventFlow.collect { event ->
                val feedbackType = when (event.type) {
                    RestServerEventType.SMS_SENT_SUCCESS -> FeedbackType.SUCCESS
                    RestServerEventType.SMS_SENT_ERROR -> FeedbackType.ERROR
                    RestServerEventType.LOG_RECEIVED_SUCCESS -> FeedbackType.SUCCESS
                    RestServerEventType.LOG_RECEIVED_ERROR -> FeedbackType.ERROR
                    RestServerEventType.SERVER_START_SUCCESS -> FeedbackType.SUCCESS
                    RestServerEventType.SERVER_START_ERROR -> FeedbackType.ERROR
                    RestServerEventType.SERVER_PORT_IN_USE -> FeedbackType.ERROR
                }
                _uiState.value = _uiState.value.copy(
                    feedbackMessage = event.message,
                    feedbackType = feedbackType
                )
                delay(if (feedbackType == FeedbackType.SUCCESS) 3000 else 5000)
                clearFeedback()
            }
        }
    }

    fun resetToken() {
        viewModelScope.launch {
            val newToken = java.util.UUID.randomUUID().toString().replace("-", "")
            ApiTokenManager.setToken(context, newToken)
            settingsRepository.updateToken(newToken)
            _token.value = newToken
            persistCurrentSettingsNow()
        }
    }

    fun setSenderId(senderId: String) {
        _uiState.value = _uiState.value.copy(senderId = senderId)
        schedulePersistCurrentSettings()
    }

    fun setRecipient(recipient: String) {
        _uiState.value = _uiState.value.copy(recipient = recipient)
        schedulePersistCurrentSettings()
    }

    fun setMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
        schedulePersistCurrentSettings()
    }

    fun setRestPortInput(portText: String) {
        val trimmed = portText.filter { it.isDigit() }.take(5)
        val parsed = parsePortOrNull(trimmed)
        val error = when {
            trimmed.isBlank() -> context.getString(R.string.rest_port_required)
            parsed == null -> context.getString(R.string.rest_port_invalid)
            else -> null
        }

        _uiState.value = _uiState.value.copy(
            restPortInput = trimmed,
            restPortError = error
        )
    }

    fun commitRestPort(): Boolean {
        val port = parsePortOrNull(_uiState.value.restPortInput)
        if (port == null) {
            _uiState.value = _uiState.value.copy(
                restPortError = context.getString(R.string.rest_port_invalid),
                restPortInput = _uiState.value.restPort.toString()
            )
            return false
        }

        viewModelScope.launch {
            updateRestPortUseCase(port)
        }
        _uiState.value = _uiState.value.copy(
            restPort = port,
            restPortInput = port.toString(),
            restPortError = null
        )
        schedulePersistCurrentSettings()
        return true
    }

    fun setServiceActive(active: Boolean) {
        val currentHostIp = _uiState.value.hostIp
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                serviceToggleTargetActive = active
            )
            kotlinx.coroutines.yield()
            runCatching {
                val feedback = if (active) {
                    context.getString(R.string.service_started_success)
                } else {
                    context.getString(R.string.service_stopped_success)
                }
                val updatedState = serviceToggleProcessor.toggle(
                    active = active,
                    currentState = _uiState.value,
                    currentHostIp = currentHostIp,
                    feedbackMessage = feedback
                )
                _uiState.value = updatedState.copy(
                    isLoading = false,
                    serviceToggleTargetActive = null
                )
            }.onSuccess {
                schedulePersistCurrentSettings()
                delay(3000)
                clearFeedback()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    serviceToggleTargetActive = null,
                    feedbackMessage = "❌ Impossible de ${if (active) "démarrer" else "arrêter"} le service: ${error.message}",
                    feedbackType = FeedbackType.ERROR
                )
            }
        }
    }

    fun refreshHostIp(hostIp: String) {
        _uiState.value = _uiState.value.copy(
            hostIp = hostIp,
            isIpValid = isHostIpUsable(hostIp)
        )
        schedulePersistCurrentSettings()
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(locationPermissionGranted = granted)
    }

    fun setLocationData(location: Pair<Double, Double>?) {
        _uiState.value = _uiState.value.copy(locationData = location)
    }


    fun sendSms() {
        val state = _uiState.value
        val sms = SmsMessage(state.senderId, state.recipient, state.message)
        viewModelScope.launch {
            val result = sendSmsUseCase(sms)
            when (result) {
                is SendResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        message = "",
                        feedbackMessage = "✅ SMS envoyé avec succès",
                            feedbackType = FeedbackType.SUCCESS
                    )
                    delay(4000)
                    clearFeedback()
                }
                is SendResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        feedbackMessage = "❌ ${result.message}",
                            feedbackType = FeedbackType.ERROR
                    )
                    delay(5000)
                    clearFeedback()
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(
            feedbackMessage = null,
            feedbackType = FeedbackType.NONE
        )
    }

    fun saveAllSettings() {
        saveSettingsJob?.cancel()
        persistCurrentSettingsNow()
    }

    override fun onCleared() {
        saveSettingsJob?.cancel()
        super.onCleared()
    }
}
