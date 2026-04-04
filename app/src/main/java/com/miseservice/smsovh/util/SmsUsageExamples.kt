package com.miseservice.smsovh.util

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Exemple d'utilisation des utilitaires SMS/MMS
 * 
 * Ce fichier montre comment utiliser les différentes fonctions
 * créées pour l'envoi optimisé de SMS et MMS via OVH.
 */
@Suppress("unused")
object SmsUsageExamples {
    private const val TAG = "SmsUsageExamples"

    /**
     * Exemple 1: Envoyer un SMS simple
     */
    fun example1_simpleSms(context: Context) {
        Log.d(TAG, "=== Exemple 1: SMS Simple ===")
        
        val phoneNumber = "+33612345678"
        val message = "Bonjour! Ceci est un SMS de test."
        
        // Via SmsHelper (recommandé)
        SmsHelper.sendSmsWithSystem(context, phoneNumber, message, null) { success, json ->
            if (success) {
                Log.d(TAG, "SMS envoyé: ${json.optString("message")}")
            } else {
                Log.e(TAG, "Erreur: ${json.optString("error")}")
            }
        }
    }

    /**
     * Exemple 2: Envoyer un SMS avec validation
     */
    fun example2_validateAndSend(context: Context) {
        Log.d(TAG, "=== Exemple 2: SMS Validé ===")
        
        val phoneNumber = "0612345678"  // Format français
        
        // Valider et normaliser
        val normalized = PhoneNumberValidator.normalize(phoneNumber)
        if (normalized != null) {
            Log.d(TAG, "Numéro normalisé: $normalized")
            
            SmsHelper.sendSmsWithSystem(context, normalized, "Message valide", "MyApp") { success, _ ->
                if (success) Log.d(TAG, "SMS envoyé")
            }
        } else {
            Log.e(TAG, "Numéro invalide: $phoneNumber")
        }
    }

    /**
     * Exemple 3: Envoyer un SMS avec vérification de permissions
     */
    fun example3_checkPermissions(context: Context) {
        Log.d(TAG, "=== Exemple 3: Vérifier Permissions ===")
        
        if (!SmsPermissionsManager.canSendSms(context)) {
            val missing = SmsPermissionsManager.getMissingPermissions(context)
            Log.w(TAG, "Permissions manquantes: $missing")
            return
        }
        
        SmsHelper.sendSmsWithSystem(context, "+33612345678", "OK à envoyer", null) { success, _ ->
            if (success) Log.d(TAG, "SMS envoyé avec succès")
        }
    }

    /**
     * Exemple 4: Envoyer un MMS avec image base64
     */
    fun example4_sendMms(context: Context, base64Image: String) {
        Log.d(TAG, "=== Exemple 4: Envoi MMS ===")
        
        if (!SmsPermissionsManager.canSendMms(context)) {
            Log.e(TAG, "Permissions MMS insuffisantes")
            return
        }
        
        SmsHelper.sendMmsWithStatus(
            context,
            "+33612345678",
            "Regardez cette image!",
            base64Image,
            "MonApp"
        ) { success, json ->
            if (success) {
                val imageSize = json.optInt("imageSize", 0)
                Log.d(TAG, "MMS envoyé (image: $imageSize bytes)")
            } else {
                Log.e(TAG, "Erreur MMS: ${json.optString("error")}")
            }
        }
    }

    /**
     * Exemple 5: Calculer le nombre de parties SMS
     */
    fun example5_smsCalculations() {
        Log.d(TAG, "=== Exemple 5: Calculs SMS ===")
        
        val messages = listOf(
            "Court",
            "Ceci est un SMS un peu plus long avec des accents éàü",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
            "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
        )
        
        for ((i, msg) in messages.withIndex()) {
            val canFit = OvhSmsConfig.willFitInSingleSms(msg)
            val partCount = OvhSmsConfig.calculateSmsPartCount(msg)
            
            Log.d(TAG, "Message $i: ${msg.take(30)}...")
            Log.d(TAG, "  - Longueur: ${msg.length} caractères")
            Log.d(TAG, "  - Tient en 1 SMS: $canFit")
            Log.d(TAG, "  - Parties SMS: $partCount")
        }
    }

    /**
     * Exemple 6: Valider une batch de numéros
     */
    fun example6_validateBatch() {
        Log.d(TAG, "=== Exemple 6: Validation Batch ===")
        
        val phoneNumbers = listOf(
            "+33612345678",    // Valide
            "0712345678",      // Valide (format français)
            "abc123",          // Invalide
            "+331",            // Invalide (trop court)
            "+33123456789012"  // Invalide (trop long)
        )
        
        val (valid, invalid) = PhoneNumberValidator.validateBatch(phoneNumbers)
        
        Log.d(TAG, "Numéros valides: $valid")
        Log.d(TAG, "Numéros invalides: $invalid")
    }

    /**
     * Exemple 7: Obtenir les informations d'un numéro
     */
    fun example7_phoneInfo() {
        Log.d(TAG, "=== Exemple 7: Infos Numéro ===")
        
        val phoneNumber = "+33612345678"
        
        val countryCode = PhoneNumberValidator.getCountryCode(phoneNumber)
        val countryName = PhoneNumberValidator.getCountryName(phoneNumber)
        
        Log.d(TAG, "Numéro: $phoneNumber")
        Log.d(TAG, "Code pays: $countryCode")
        Log.d(TAG, "Pays: $countryName")
    }

    /**
     * Exemple 8: Afficher la configuration OVH
     */
    fun example8_configOvh() {
        Log.d(TAG, "=== Exemple 8: Configuration OVH ===")
        
        OvhSmsConfig.logConfiguration()
    }

    /**
     * Exemple 9: Créer les Settings appropriées
     */
    fun example9_createSettings() {
        Log.d(TAG, "=== Exemple 9: Créer Settings ===")

        OvhSmsConfig.createSmsSettings()
        Log.d(TAG, "SMS Settings créées")

        OvhSmsConfig.createMmsSettings()
        Log.d(TAG, "MMS Settings créées (MMSC: ${OvhSmsConfig.Mmsc.URL})")

        OvhSmsConfig.createLongMessageSettings()
        Log.d(TAG, "Long Message Settings créées")
    }

    /**
     * Exemple 10: Utiliser MessageStatusManager
     */
    fun example10_trackMessageStatus(context: Context) {
        Log.d(TAG, "=== Exemple 10: Suivi d'État ===")
        
        val statusManager = MessageStatusManager(context)
        
        // S'enregistrer pour les mises à jour de statut
        statusManager.registerStatusCallback("msg-123") { event ->
            Log.d(TAG, "Statut du message ${event.messageId}:")
            Log.d(TAG, "  - Statut: ${event.status}")
            Log.d(TAG, "  - Téléphone: ${event.phoneNumber}")
            Log.d(TAG, "  - Timestamp: ${event.timestamp}")
            
            when (event.status) {
                MessageStatus.SENT -> Log.d(TAG, "✅ SMS envoyé")
                MessageStatus.DELIVERED -> Log.d(TAG, "✅ SMS livré")
                MessageStatus.FAILED -> Log.d(TAG, "❌ Échec: ${event.details}")
                else -> Log.d(TAG, "❓ Statut: ${event.status}")
            }
        }
        
        // ... Envoyer le message ...
        
        // Nettoyer à la fin
        statusManager.cleanup()
    }

    /**
     * Exemple 11: API REST - Envoyer SMS
     */
    @Deprecated(
        message = "Use example11_restApiSms()",
        replaceWith = ReplaceWith("example11_restApiSms()")
    )
    fun example11_restApiSmS() = example11_restApiSms()

    fun example11_restApiSms() {
        Log.d(TAG, "=== Exemple 11: API REST SMS ===")

        val url = "http://${NetworkInfoProvider.getHostIp()}:${NetworkInfoProvider.getApiPort()}/api/send-sms"
        val json = org.json.JSONObject().apply {
            put("senderId", "MonApp")
            put("destinataire", "+33612345678")
            put("text", "Message via API REST")
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = okhttp3.Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer token123")
            .post(requestBody)
            .build()

        Log.d(TAG, "Requête API: ${request.method} ${request.url}")
        Log.d(TAG, "Payload: $json")
    }

    /**
     * Exemple 12: API REST - Envoyer MMS
     */
    fun example12_restApiMms(base64Image: String) {
        Log.d(TAG, "=== Exemple 12: API REST MMS ===")
        
        val url = "http://${NetworkInfoProvider.getHostIp()}:${NetworkInfoProvider.getApiPort()}/api/send-mms"
        val json = org.json.JSONObject()
        json.put("senderId", "MonApp")
        json.put("destinataire", "+33612345678")
        json.put("text", "Message avec image")
        json.put("base64Jpeg", base64Image)
        
        Log.d(TAG, "Requête API: POST $url")
        Log.d(TAG, "Type: MMS avec image (${base64Image.length} bytes base64)")
    }

    /**
     * Exemple 13: Gestion des erreurs
     */
    fun example13_errorHandling(context: Context) {
        Log.d(TAG, "=== Exemple 13: Gestion Erreurs ===")
        
        // Numéro invalide
        if (!PhoneNumberValidator.isValid("abc123")) {
            Log.w(TAG, "Numéro invalide: abc123")
            return
        }
        
        // Permission manquante
        if (!SmsPermissionsManager.canSendSms(context)) {
            Log.e(TAG, "Permission SEND_SMS manquante")
            return
        }
        
        // Trop long pour 1 SMS
        val message = "A".repeat(200)
        if (!OvhSmsConfig.willFitInSingleSms(message)) {
            Log.d(TAG, "Message trop long: ${OvhSmsConfig.calculateSmsPartCount(message)} parties")
        }
        
        // Image MMS trop grosse (supposons 5MB)
        val sizeBytes = 5 * 1024 * 1024
        if (!OvhSmsConfig.isValidMmsImageSize(sizeBytes)) {
            Log.e(TAG, OvhSmsConfig.getImageSizeErrorMessage(sizeBytes))
        }
    }
}
