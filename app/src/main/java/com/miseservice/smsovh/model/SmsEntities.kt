package com.miseservice.smsovh.model

// Entité représentant un SMS à envoyer
 data class SmsMessage(
    val from: String,
    val to: String,
    val message: String
 )

// Résultat de l'envoi
sealed class SendResult {
    object Success : SendResult()
    data class Error(val code: Int, val message: String) : SendResult()
}

