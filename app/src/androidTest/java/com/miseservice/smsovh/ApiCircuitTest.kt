package com.miseservice.smsovh.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import android.util.Log

/**
 * Tests du circuit API REST SMS/MMS
 * Vérifie les endpoints de l'API du serveur REST
 */
class ApiCircuitTest {
    
    companion object {
        const val TAG = "ApiCircuitTest"
        const val BASE_URL = "http://localhost:8080"
        const val BEARER_TOKEN = "YOUR_TOKEN_HERE"  // À remplacer par le vrai token
        
        // Test fixtures
        const val TEST_PHONE = "+33612345678"
        const val TEST_MESSAGE = "Test API Circuit"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Test 1: Vérifier la santé du serveur (GET /api/health)
     */
    fun testHealthCheck(): Boolean {
        Log.i(TAG, "=== TEST 1: Health Check ===")
        
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/api/health")
                .addHeader("Authorization", "Bearer $BEARER_TOKEN")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            
            Log.i(TAG, "Status Code: ${response.code}")
            
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "{}"
                val json = JSONObject(body)
                val status = json.optString("status", "unknown")
                val version = json.optString("version", "unknown")
                
                Log.i(TAG, "✅ Serveur online")
                Log.i(TAG, "Status: $status")
                Log.i(TAG, "Version: $version")
                true
            } else {
                Log.e(TAG, "❌ Erreur Health Check: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Health Check", e)
            false
        }
    }

    /**
     * Test 2: Authentification - Test sans token
     */
    fun testAuthenticationMissingToken(): Boolean {
        Log.i(TAG, "=== TEST 2: Auth - Missing Token ===")
        
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/api/health")
                .get()  // Pas d'Authorization header
                .build()

            val response = httpClient.newCall(request).execute()
            
            Log.i(TAG, "Status Code: ${response.code}")
            
            if (response.code == 401) {
                Log.i(TAG, "✅ 401 Unauthorized (attendu)")
                true
            } else {
                Log.e(TAG, "❌ Devrait retourner 401, reçu: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Auth Test", e)
            false
        }
    }

    /**
     * Test 3: Authentification - Test avec token invalide
     */
    fun testAuthenticationInvalidToken(): Boolean {
        Log.i(TAG, "=== TEST 3: Auth - Invalid Token ===")
        
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/api/health")
                .addHeader("Authorization", "Bearer INVALID_TOKEN_12345")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            
            Log.i(TAG, "Status Code: ${response.code}")
            
            if (response.code == 401) {
                Log.i(TAG, "✅ 401 Unauthorized pour token invalide")
                true
            } else {
                Log.e(TAG, "❌ Devrait retourner 401, reçu: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Invalid Token Test", e)
            false
        }
    }

    /**
     * Test 4: Envoyer un SMS simple (POST /api/send-sms)
     */
    fun testSendSmsBasic(): Boolean {
        Log.i(TAG, "=== TEST 4: Send SMS Basic ===")
        
        return try {
            val json = JSONObject()
            json.put("senderId", "TestApp")
            json.put("destinataire", TEST_PHONE)
            json.put("text", TEST_MESSAGE)
            
            val body = json.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL/api/send-sms")
                .addHeader("Authorization", "Bearer $BEARER_TOKEN")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            val responseJson = JSONObject(responseBody)
            
            Log.i(TAG, "Status Code: ${response.code}")
            Log.i(TAG, "Response: $responseBody")
            
            if (response.isSuccessful) {
                val success = responseJson.optBoolean("success", false)
                val messageType = responseJson.optString("type", "")
                val parts = responseJson.optInt("parts", 0)
                
                Log.i(TAG, "✅ SMS envoyé")
                Log.i(TAG, "Type: $messageType (attendu: SMS)")
                Log.i(TAG, "Parties: $parts")
                
                success && messageType == "SMS"
            } else {
                Log.e(TAG, "❌ Erreur envoi SMS: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Send SMS", e)
            false
        }
    }

    /**
     * Test 5: Envoyer un SMS avec validation de numéro
     */
    fun testSendSmsWithValidation(): Boolean {
        Log.i(TAG, "=== TEST 5: Send SMS With Validation ===")
        
        return try {
            // Test avec numéro invalide
            val json = JSONObject()
            json.put("senderId", "TestApp")
            json.put("destinataire", "abc123")  // Invalide
            json.put("text", TEST_MESSAGE)
            
            val body = json.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL/api/send-sms")
                .addHeader("Authorization", "Bearer $BEARER_TOKEN")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            
            Log.i(TAG, "Status Code: ${response.code}")
            
            if (response.code == 400) {
                Log.i(TAG, "✅ Numéro invalide rejeté (400)")
                true
            } else {
                Log.e(TAG, "❌ Devrait retourner 400, reçu: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Validation Test", e)
            false
        }
    }

    /**
     * Test 6: Envoyer un MMS avec image (POST /api/send-mms)
     */
    fun testSendMmsWithImage(): Boolean {
        Log.i(TAG, "=== TEST 6: Send MMS With Image ===")
        
        return try {
            // Image base64 simple (1x1 pixel JPEG)
            val base64Image = "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAA" +
                "EBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8VAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCwAA8A/9k"
            
            val json = JSONObject()
            json.put("senderId", "TestApp")
            json.put("destinataire", TEST_PHONE)
            json.put("text", "Test MMS")
            json.put("base64Jpeg", base64Image)
            
            val body = json.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL/api/send-mms")
                .addHeader("Authorization", "Bearer $BEARER_TOKEN")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            val responseJson = JSONObject(responseBody)
            
            Log.i(TAG, "Status Code: ${response.code}")
            Log.i(TAG, "Response: $responseBody")
            
            if (response.isSuccessful) {
                val success = responseJson.optBoolean("success", false)
                val messageType = responseJson.optString("type", "")
                
                Log.i(TAG, "✅ MMS envoyé")
                Log.i(TAG, "Type: $messageType (attendu: MMS)")
                
                success && messageType == "MMS"
            } else {
                Log.e(TAG, "❌ Erreur envoi MMS: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Send MMS", e)
            false
        }
    }

    /**
     * Test 7: Envoyer un message via /api/send-message (routage automatique)
     */
    fun testSendMessageAutoRoute(): Boolean {
        Log.i(TAG, "=== TEST 7: Send Message Auto-Route ===")
        
        return try {
            val json = JSONObject()
            json.put("senderId", "TestApp")
            json.put("destinataire", TEST_PHONE)
            json.put("text", TEST_MESSAGE)
            // Pas de base64Jpeg → devrait être SMS
            
            val body = json.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL/api/send-message")
                .addHeader("Authorization", "Bearer $BEARER_TOKEN")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            val responseJson = JSONObject(responseBody)
            
            Log.i(TAG, "Status Code: ${response.code}")
            Log.i(TAG, "Response: $responseBody")
            
            if (response.isSuccessful) {
                val success = responseJson.optBoolean("success", false)
                val messageType = responseJson.optString("type", "")
                
                Log.i(TAG, "✅ Message routé automatiquement")
                Log.i(TAG, "Type détecté: $messageType (attendu: SMS)")
                
                success && messageType == "SMS"
            } else {
                Log.e(TAG, "❌ Erreur Send Message: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Send Message", e)
            false
        }
    }

    /**
     * Test 8: Enregistrer un log (POST /api/logs)
     */
    fun testSendLog(): Boolean {
        Log.i(TAG, "=== TEST 8: Send Log ===")
        
        return try {
            val json = JSONObject()
            json.put("message", "Test log message from API circuit test")
            
            val body = json.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL/api/logs")
                .addHeader("Authorization", "Bearer $BEARER_TOKEN")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            
            Log.i(TAG, "Status Code: ${response.code}")
            Log.i(TAG, "Response: $responseBody")
            
            if (response.isSuccessful) {
                Log.i(TAG, "✅ Log enregistré")
                true
            } else {
                Log.e(TAG, "❌ Erreur envoi log: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception Send Log", e)
            false
        }
    }

    /**
     * Test 9: Erreur 404 - Endpoint inexistant
     */
    fun testNotFoundError(): Boolean {
        Log.i(TAG, "=== TEST 9: 404 Not Found ===")
        
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/api/invalid-endpoint")
                .addHeader("Authorization", "Bearer $BEARER_TOKEN")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            
            Log.i(TAG, "Status Code: ${response.code}")
            
            if (response.code == 404) {
                Log.i(TAG, "✅ 404 Not Found (attendu)")
                true
            } else {
                Log.e(TAG, "❌ Devrait retourner 404, reçu: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception 404 Test", e)
            false
        }
    }

    /**
     * Test 10: Erreur 400 - Body manquant
     */
    fun testBadRequestError(): Boolean {
        Log.i(TAG, "=== TEST 10: 400 Bad Request ===")
        
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/api/send-sms")
                .addHeader("Authorization", "Bearer $BEARER_TOKEN")
                .post("".toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            
            Log.i(TAG, "Status Code: ${response.code}")
            
            if (response.code == 400) {
                Log.i(TAG, "✅ 400 Bad Request (attendu)")
                true
            } else {
                Log.e(TAG, "❌ Devrait retourner 400, reçu: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception 400 Test", e)
            false
        }
    }

    /**
     * Exécuter tous les tests et afficher un résumé
     */
    fun runAllTests(): Map<String, Boolean> {
        Log.i(TAG, "╔════════════════════════════════════════╗")
        Log.i(TAG, "║  API CIRCUIT TEST - TOUS LES TESTS      ║")
        Log.i(TAG, "╚════════════════════════════════════════╝")
        
        val results = mutableMapOf<String, Boolean>()
        
        results["1. Health Check"] = testHealthCheck()
        results["2. Auth - Missing Token"] = testAuthenticationMissingToken()
        results["3. Auth - Invalid Token"] = testAuthenticationInvalidToken()
        results["4. Send SMS Basic"] = testSendSmsBasic()
        results["5. Send SMS Validation"] = testSendSmsWithValidation()
        results["6. Send MMS With Image"] = testSendMmsWithImage()
        results["7. Send Message Auto-Route"] = testSendMessageAutoRoute()
        results["8. Send Log"] = testSendLog()
        results["9. 404 Not Found"] = testNotFoundError()
        results["10. 400 Bad Request"] = testBadRequestError()
        
        Log.i(TAG, "")
        Log.i(TAG, "╔════════════════════════════════════════╗")
        Log.i(TAG, "║          RÉSUMÉ DES RÉSULTATS           ║")
        Log.i(TAG, "╚════════════════════════════════════════╝")
        
        var successCount = 0
        for ((testName, passed) in results) {
            val status = if (passed) "✅ PASS" else "❌ FAIL"
            Log.i(TAG, "$status - $testName")
            if (passed) successCount++
        }
        
        val totalTests = results.size
        Log.i(TAG, "")
        Log.i(TAG, "Résultat: $successCount/$totalTests tests réussis")
        Log.i(TAG, "Pourcentage: ${(successCount * 100) / totalTests}%")
        
        return results
    }
}

