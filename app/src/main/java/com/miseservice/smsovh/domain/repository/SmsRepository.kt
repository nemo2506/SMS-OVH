package com.miseservice.smsovh.domain.repository

import com.miseservice.smsovh.model.SmsMessage
import com.miseservice.smsovh.model.SendResult

interface SmsRepository {
    suspend fun sendSms(sms: SmsMessage): SendResult
}

