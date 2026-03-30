package com.miseservice.smsovh.domain.usecase

import com.miseservice.smsovh.domain.repository.AdminRepository
import javax.inject.Inject

class SaveApiUrlUseCase @Inject constructor(
    private val adminRepository: AdminRepository
) {
    operator fun invoke(url: String) = adminRepository.saveApiUrl(url)
}

