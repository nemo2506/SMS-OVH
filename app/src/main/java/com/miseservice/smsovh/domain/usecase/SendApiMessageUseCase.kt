package com.miseservice.smsovh.domain.usecase

import com.miseservice.smsovh.domain.repository.AdminRepository
import com.miseservice.smsovh.domain.repository.ApiSendResult
import javax.inject.Inject

class SendApiMessageUseCase @Inject constructor(
    private val adminRepository: AdminRepository
) {
    suspend operator fun invoke(
        apiUrl: String,
        token: String,
        senderId: String,
        recipient: String,
        message: String
    ): ApiSendResult =
        adminRepository.sendApiMessage(apiUrl, token, senderId, recipient, message)
}

