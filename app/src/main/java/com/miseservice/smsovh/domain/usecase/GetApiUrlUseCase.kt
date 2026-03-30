package com.miseservice.smsovh.domain.usecase

import com.miseservice.smsovh.domain.repository.AdminRepository
import javax.inject.Inject

class GetApiUrlUseCase @Inject constructor(
    private val adminRepository: AdminRepository
) {
    operator fun invoke(): String = adminRepository.getApiUrl()
}

