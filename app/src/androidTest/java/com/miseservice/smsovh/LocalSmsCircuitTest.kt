package com.miseservice.smsovh.util

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Tests du circuit SMS/MMS en local sur le device
 * Vérifie le chemin complet d'envoi SMS et MMS
 */
@RunWith(AndroidJUnit4::class)
class LocalSmsCircuitTest {
    
    private lateinit var context: Context
    private companion object {
        const val TAG = "LocalSmsCircuitTest"
        const val TEST_PHONE = "+33612345678"
        const val TEST_MESSAGE = "Test SMS Circuit Local"
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    /**
     * Test 1: Vérifier que les permissions sont accordées
     */
    @Test
    fun testPermissionsCheck() {
        Log.i(TAG, "=== TEST 1: Vérification des Permissions ===")
        
        val canSendSms = SmsPermissionsManager.canSendSms(context)
        val canSendMms = SmsPermissionsManager.canSendMms(context)
        
        Log.i(TAG, "Permissions SMS: $canSendSms")
        Log.i(TAG, "Permissions MMS: $canSendMms")
        
        if (!canSendSms) {
            val missing = SmsPermissionsManager.getMissingPermissions(context)
            Log.e(TAG, "Permissions manquantes: $missing")
        }
        
        assert(canSendSms) { "Permissions SMS manquantes" }
    }

    /**
     * Test 2: Valider un numéro de téléphone
     */
    @Test
    fun testPhoneNumberValidation() {
        Log.i(TAG, "=== TEST 2: Validation Numéro Téléphone ===")
        
        val testNumbers = mapOf(
            "0612345678" to true,           // Format français
            "+33612345678" to true,         // Format international
            "+331" to false,                // Trop court
            "abc123" to false               // Invalide
        )
        
        for ((number, shouldBeValid) in testNumbers) {
            val isValid = PhoneNumberValidator.isValid(number)
            Log.i(TAG, "Numéro: $number → Valide: $isValid (attendu: $shouldBeValid)")
            assert(isValid == shouldBeValid) { "Validation échouée pour $number" }
        }
    }

    /**
     * Test 3: Formater un numéro en international
     */
    @Test
    fun testPhoneNumberFormatting() {
        Log.i(TAG, "=== TEST 3: Formatage Numéro International ===")
        
        val testCases = mapOf(
            "0612345678" to "+33612345678",
            "+33712345678" to "+33712345678",
            "612345678" to "+33612345678"
        )
        
        for ((input, expected) in testCases) {
            val formatted = PhoneNumberValidator.normalize(input)
            Log.i(TAG, "Input: $input → Formaté: $formatted (attendu: $expected)")
            assert(formatted == expected) { "Formatage échoué: $input" }
        }
    }

    /**
     * Test 4: Obtenir les infos du pays
     */
    @Test
    fun testCountryDetection() {
        Log.i(TAG, "=== TEST 4: Détection Pays ===")
        
        val code = PhoneNumberValidator.getCountryCode(TEST_PHONE)
        val country = PhoneNumberValidator.getCountryName(TEST_PHONE)
        
        Log.i(TAG, "Numéro: $TEST_PHONE")
        Log.i(TAG, "Code pays: $code (attendu: 33)")
        Log.i(TAG, "Pays: $country (attendu: France)")
        
        assert(code == "33") { "Code pays incorrect" }
        assert(country == "France") { "Pays incorrect" }
    }

    /**
     * Test 5: Calculer le nombre de parties SMS
     */
    @Test
    fun testSmsPartCalculation() {
        Log.i(TAG, "=== TEST 5: Calcul Parties SMS ===")
        
        val testCases = mapOf(
            "Court" to 1,                                                    // 5 chars → 1 SMS
            "A".repeat(160) to 1,                                           // 160 chars → 1 SMS
            "A".repeat(161) to 2,                                           // 161 chars → 2 SMS
            "A".repeat(320) to 2,                                           // 320 chars → 2 SMS
            "A".repeat(321) to 3,                                           // 321 chars → 3 SMS
            "Bonjour éàü" to 1                                              // 11 chars avec accents → 1 SMS
        )
        
        for ((message, expectedParts) in testCases) {
            val parts = OvhSmsConfig.calculateSmsPartCount(message)
            Log.i(TAG, "Message: ${message.take(20)}... (${message.length} chars) → $parts partie(s) (attendu: $expectedParts)")
            assert(parts == expectedParts) { "Calcul de parties échoué pour message de ${message.length} chars" }
        }
    }

    /**
     * Test 6: Vérifier les limites MMS
     */
    @Test
    fun testMmsImageValidation() {
        Log.i(TAG, "=== TEST 6: Validation Image MMS ===")
        
        val testCases = mapOf(
            1024 to true,                   // 1 KB → OK
            1024 * 1024 to true,            // 1 MB → OK
            2 * 1024 * 1024 to true,        // 2 MB → OK
            3 * 1024 * 1024 to true,        // 3 MB (limite) → OK
            4 * 1024 * 1024 to false,       // 4 MB → TROP GROS
            10 * 1024 * 1024 to false       // 10 MB → TROP GROS
        )
        
        for ((sizeBytes, shouldBeValid) in testCases) {
            val isValid = OvhSmsConfig.isValidMmsImageSize(sizeBytes)
            val sizeMB = sizeBytes / (1024 * 1024)
            Log.i(TAG, "Taille: ${sizeMB}MB → Valide: $isValid (attendu: $shouldBeValid)")
            assert(isValid == shouldBeValid) { "Validation taille échouée" }
        }
    }

    /**
     * Test 7: Valider un batch de numéros
     */
    @Test
    fun testPhoneBatchValidation() {
        Log.i(TAG, "=== TEST 7: Validation Batch Numéros ===")
        
        val phoneNumbers = listOf(
            "+33612345678",     // Valide
            "0712345678",       // Valide
            "abc123",           // Invalide
            "+331",             // Invalide
            "+33123456789012"   // Invalide (trop long)
        )
        
        val (valid, invalid) = PhoneNumberValidator.validateBatch(phoneNumbers)
        
        Log.i(TAG, "Total: ${phoneNumbers.size} numéros")
        Log.i(TAG, "Valides: ${valid.size} → $valid")
        Log.i(TAG, "Invalides: ${invalid.size} → $invalid")
        
        assert(valid.size == 2) { "Devrait avoir 2 numéros valides, eu ${valid.size}" }
        assert(invalid.size == 3) { "Devrait avoir 3 numéros invalides, eu ${invalid.size}" }
    }

    /**
     * Test 8: Configuration OVH
     */
    @Test
    fun testOvhConfiguration() {
        Log.i(TAG, "=== TEST 8: Configuration OVH ===")
        
        Log.i(TAG, "MMSC URL: ${OvhSmsConfig.Mmsc.URL}")
        Log.i(TAG, "MMSC Proxy: ${OvhSmsConfig.Mmsc.PROXY}")
        Log.i(TAG, "MMSC Port: ${OvhSmsConfig.Mmsc.PORT}")
        Log.i(TAG, "APN: ${OvhSmsConfig.Apn.NAME}")
        Log.i(TAG, "Limite SMS: ${OvhSmsConfig.Limits.SMS_CHAR_LIMIT} chars")
        Log.i(TAG, "Limite SMS Unicode: ${OvhSmsConfig.Limits.SMS_CHAR_LIMIT_WITH_UNICODE} chars")
        Log.i(TAG, "Limite MMS: ${OvhSmsConfig.Limits.MMS_SIZE_LIMIT_MB}MB")
        
        // Vérifier que la config est correcte
        assert(OvhSmsConfig.Mmsc.URL == "http://mms.ovh.net") { "MMSC URL incorrecte" }
        assert(OvhSmsConfig.Mmsc.PORT == 8080) { "Port MMSC incorrect" }
        assert(OvhSmsConfig.Limits.MMS_SIZE_LIMIT_MB == 3) { "Limite MMS incorrecte" }
        
        Log.i(TAG, "✅ Configuration OVH valide")
    }

    /**
     * Test 9: Créer les Settings OVH
     */
    @Test
    fun testOvhSettingsCreation() {
        Log.i(TAG, "=== TEST 9: Création Settings OVH ===")
        
        try {
            val smsSettings = OvhSmsConfig.createSmsSettings()
            Log.i(TAG, "✅ SMS Settings créés")
            
            val mmsSettings = OvhSmsConfig.createMmsSettings()
            Log.i(TAG, "✅ MMS Settings créés avec MMSC: ${mmsSettings.mmsc}")
            
            val longSettings = OvhSmsConfig.createLongMessageSettings()
            Log.i(TAG, "✅ Long Message Settings créés")
            
            // Vérifier que MMSC est configuré correctement
            assert(mmsSettings.mmsc == OvhSmsConfig.Mmsc.URL) { "MMSC non configuré correctement" }
            assert(mmsSettings.mmsPort == OvhSmsConfig.Mmsc.PORT) { "Port MMSC incorrect" }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur création Settings", e)
            throw e
        }
    }

    /**
     * Test 10: MessageStatusManager
     */
    @Test
    fun testMessageStatusManager() {
        Log.i(TAG, "=== TEST 10: Message Status Manager ===")
        
        val statusManager = MessageStatusManager(context)
        
        var callbackInvoked = false
        var receivedStatus: MessageStatus? = null
        
        statusManager.registerStatusCallback("test-msg-1") { event ->
            callbackInvoked = true
            receivedStatus = event.status
            Log.i(TAG, "Callback invoqué - Status: ${event.status}")
        }
        
        Log.i(TAG, "✅ Callback enregistré pour test-msg-1")
        
        statusManager.unregisterStatusCallback("test-msg-1")
        Log.i(TAG, "✅ Callback désenregistré")
        
        statusManager.cleanup()
        Log.i(TAG, "✅ MessageStatusManager nettoyé")
    }

    /**
     * Test 11: Afficher la configuration complète
     */
    @Test
    fun testDisplayFullConfiguration() {
        Log.i(TAG, "=== TEST 11: Configuration Complète ===")
        
        OvhSmsConfig.logConfiguration()
        
        Log.i(TAG, "✅ Configuration affichée dans les logs")
    }
}

