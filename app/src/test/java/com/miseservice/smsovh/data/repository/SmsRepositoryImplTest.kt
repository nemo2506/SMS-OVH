package com.miseservice.smsovh.data.repository

import com.miseservice.smsovh.model.SmsMessage
import com.miseservice.smsovh.data.repository.SmsRepositoryImpl
import com.miseservice.smsovh.model.SendResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SmsRepositoryImplTest {
    private val repo = SmsRepositoryImpl()

    @Test
    fun `envoi avec sender id autorisé retourne success`() = runBlocking {
        val sms = SmsMessage(from = "OVHSMS", to = "0600000000", message = "test")
        val result = repo.sendSms(sms)
        assertEquals(SendResult.Success, result)
    }

    @Test
    fun `envoi sans sender id retourne success`() = runBlocking {
        val sms = SmsMessage(from = "", to = "0600000000", message = "test")
        val result = repo.sendSms(sms)
        assertEquals(SendResult.Success, result)
    }

    @Test
    fun `envoi avec sender id non autorisé retourne erreur`() = runBlocking {
        val sms = SmsMessage(from = "INCONNU", to = "0600000000", message = "test")
        val result = repo.sendSms(sms)
        assert(result is SendResult.Error)
        result as SendResult.Error
        assertEquals(403, result.code)
    }
}

