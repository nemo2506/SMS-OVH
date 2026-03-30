package com.miseservice.smsovh.domain.repository

interface AdminRepository {
    suspend fun sendApiMessage(
        apiUrl: String,
        token: String,
        senderId: String,
        recipient: String,
        message: String
    ): ApiSendResult

    fun getApiUrl(): String
    fun saveApiUrl(url: String)
}

sealed class ApiSendResult {
    data class Success(val logs: String) : ApiSendResult()
    data class Error(val logs: String) : ApiSendResult()
}

