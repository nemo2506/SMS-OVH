package com.miseservice.smsovh.data.repository

import com.miseservice.smsovh.domain.repository.SmsRepository
import com.miseservice.smsovh.model.SmsMessage
import com.miseservice.smsovh.model.SendResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRepositoryImpl @Inject constructor() : SmsRepository {
    // Liste fictive des Sender ID autorisés (à remplacer par une source dynamique si besoin)
    private val allowedSenderIds = setOf("OVHSMS", "MONENTREPRISE", "INFOALERT")

    override suspend fun sendSms(sms: SmsMessage): SendResult {
        // Vérifie si un Sender ID est fourni et s'il est autorisé
        val senderId = sms.from.trim()
        val useSenderId = senderId.isNotEmpty() && allowedSenderIds.contains(senderId)
        // Simule l'envoi via l'API OVH (à remplacer par un appel HTTP réel)
        return if (useSenderId) {
            // Envoi avec Sender ID autorisé
            SendResult.Success // TODO: remplacer par l'appel API OVH avec senderId
        } else if (senderId.isEmpty()) {
            // Envoi sans Sender ID (utilise le sender OVH par défaut)
            SendResult.Success // TODO: remplacer par l'appel API OVH sans senderId
        } else {
            // Sender ID non autorisé
            SendResult.Error(403, "Sender ID non autorisé : $senderId")
        }
    }
}
