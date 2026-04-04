package com.miseservice.smsovh.util

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import org.json.JSONObject

/**
 * Événement de status de message
 */
data class MessageStatusEvent(
    val messageId: String,
    val phoneNumber: String,
    val status: MessageStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = ""
)

/**
 * Statuts possibles d'un message
 */
enum class MessageStatus {
    PENDING,       // En attente d'envoi
    SENT,          // Envoyé
    DELIVERED,     // Livré au destinataire
    FAILED,        // Échec d'envoi
    RECEIVED,      // Message reçu
    UNKNOWN        // Statut inconnu
}

/**
 * Gestionnaire des statuts de messages SMS/MMS
 * Écoute les broadcasts du système pour suivre l'état des messages
 */
class MessageStatusManager(private val context: Context) {
    private companion object {
        const val TAG = "MessageStatusManager"
        const val SMS_SENT_ACTION = "SMS_SENT"
        const val SMS_DELIVERED_ACTION = "SMS_DELIVERED"
    }

    private val statusCallbacks = mutableMapOf<String, (MessageStatusEvent) -> Unit>()
    private var sentReceiver: BroadcastReceiver? = null
    private var deliveredReceiver: BroadcastReceiver? = null
    private var isRegistered = false

    /**
     * Enregistre un callback pour les changements de statut
     */
    fun registerStatusCallback(messageId: String, callback: (MessageStatusEvent) -> Unit) {
        statusCallbacks[messageId] = callback
        ensureReceiversRegistered()
    }

    /**
     * Désenregistre un callback
     */
    fun unregisterStatusCallback(messageId: String) {
        statusCallbacks.remove(messageId)
    }

    /**
     * Enregistre les receivers pour les statuts SMS
     */
    private fun ensureReceiversRegistered() {
        if (isRegistered) return

        try {
            sentReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val phoneNumber = intent.getStringExtra("phoneNumber") ?: "Unknown"
                    val messageId = intent.getStringExtra("messageId") ?: "Unknown"
                    
                    val event = when (resultCode) {
                        android.app.Activity.RESULT_OK -> {
                            MessageStatusEvent(messageId, phoneNumber, MessageStatus.SENT)
                        }
                        else -> {
                            MessageStatusEvent(messageId, phoneNumber, MessageStatus.FAILED, 
                                details = "SMS send failed with code: $resultCode")
                        }
                    }
                    
                    statusCallbacks[messageId]?.invoke(event)
                    Log.d(TAG, "SMS sent status: $event")
                }
            }

            deliveredReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val phoneNumber = intent.getStringExtra("phoneNumber") ?: "Unknown"
                    val messageId = intent.getStringExtra("messageId") ?: "Unknown"
                    
                    val event = when (resultCode) {
                        android.app.Activity.RESULT_OK -> {
                            MessageStatusEvent(messageId, phoneNumber, MessageStatus.DELIVERED)
                        }
                        else -> {
                            MessageStatusEvent(messageId, phoneNumber, MessageStatus.FAILED,
                                details = "SMS delivery failed with code: $resultCode")
                        }
                    }
                    
                    statusCallbacks[messageId]?.invoke(event)
                    Log.d(TAG, "SMS delivered status: $event")
                }
            }

            val receiverFlag = if (Build.VERSION.SDK_INT >= 33) Context.RECEIVER_NOT_EXPORTED else 0
            
            context.registerReceiver(sentReceiver, IntentFilter(SMS_SENT_ACTION), receiverFlag)
            context.registerReceiver(deliveredReceiver, IntentFilter(SMS_DELIVERED_ACTION), receiverFlag)
            
            isRegistered = true
            Log.d(TAG, "MessageStatusManager receivers registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register status receivers", e)
        }
    }

    /**
     * Désenregistre les receivers
     */
    fun cleanup() {
        try {
            if (isRegistered && sentReceiver != null) {
                context.unregisterReceiver(sentReceiver)
            }
            if (isRegistered && deliveredReceiver != null) {
                context.unregisterReceiver(deliveredReceiver)
            }
            isRegistered = false
            statusCallbacks.clear()
            Log.d(TAG, "MessageStatusManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    /**
     * Convertit un MessageStatusEvent en JSON pour l'API
     */
    fun eventToJson(event: MessageStatusEvent): JSONObject {
        return JSONObject().apply {
            put("messageId", event.messageId)
            put("phoneNumber", event.phoneNumber)
            put("status", event.status.name)
            put("timestamp", event.timestamp)
            put("details", event.details)
        }
    }
}

