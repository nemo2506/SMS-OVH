package com.miseservice.smsovh.di

import com.miseservice.smsovh.data.repository.AdminRepositoryImpl
import com.miseservice.smsovh.data.repository.CredentialsRepositoryImpl
import com.miseservice.smsovh.data.repository.SmsRepositoryImpl
import com.miseservice.smsovh.domain.repository.AdminRepository
import com.miseservice.smsovh.domain.repository.CredentialsRepository
import com.miseservice.smsovh.domain.repository.SmsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCredentialsRepository(
        impl: CredentialsRepositoryImpl
    ): CredentialsRepository

    @Binds
    @Singleton
    abstract fun bindSmsRepository(
        impl: SmsRepositoryImpl
    ): SmsRepository

    @Binds
    @Singleton
    abstract fun bindAdminRepository(
        impl: AdminRepositoryImpl
    ): AdminRepository
}
