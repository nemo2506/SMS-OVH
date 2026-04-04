package com.miseservice.smsovh.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.miseservice.smsovh.domain.usecase.SendRestMessageUseCase
import com.miseservice.smsovh.model.SendMessageRequest
import com.miseservice.smsovh.model.SendResult
import com.miseservice.smsovh.util.ApiTokenManager
import com.miseservice.smsovh.util.RestServerEventManager
import com.miseservice.smsovh.util.RestServerEvent
import com.miseservice.smsovh.util.RestServerEventType
import com.miseservice.smsovh.util.PhoneNumberValidator
import com.miseservice.smsovh.util.OvhSmsConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.iki.elonen.NanoHTTPD
import java.net.BindException
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import com.miseservice.smsovh.data.local.LogDao

@Singleton
class SmsRestServer @Inject constructor(
    @ApplicationContext val context: Context,
    private val sendRestMessageUseCase: SendRestMessageUseCase,
    private val logDao: LogDao,
    private val restServerEventManager: RestServerEventManager,
    @Named("restPort") port: Int = 8080
) : NanoHTTPD(port) {
    private companion object {
        const val TAG = "SmsRestServer"
        const val API_VERSION = "1.0"
    }

    @Volatile
    private var serverStarted = false

    @Volatile
    private var currentPort: Int = port.coerceIn(1, 65535)

    @Volatile
    private var activeServer: NanoHTTPD? = null

    @Synchronized
    fun isRunning(): Boolean = serverStarted

    @Synchronized
    fun startServer(): Boolean {
        if (serverStarted) return true

        return try {
            val server = object : NanoHTTPD(currentPort) {
                override fun serve(session: IHTTPSession): Response = this@SmsRestServer.serve(session)
            }
            server.start()
            activeServer = server
            serverStarted = true
            Log.i(TAG, "SMS REST Server demarre sur le port $currentPort")
            emitEvent(RestServerEventType.SERVER_START_SUCCESS, "Serveur REST demarre sur le port $currentPort")
            true
        } catch (e: BindException) {
            activeServer = null
            serverStarted = false
            Log.e(TAG, "Port $currentPort deja utilise (EADDRINUSE)", e)
            emitEvent(RestServerEventType.SERVER_PORT_IN_USE, "Port $currentPort deja utilise")
            false
        } catch (e: Exception) {
            activeServer = null
            serverStarted = false
            Log.e(TAG, "Echec du demarrage serveur sur le port $currentPort", e)
            emitEvent(RestServerEventType.SERVER_START_ERROR, "Echec demarrage serveur: ${e.message}")
            false
        }
    }

    @Synchronized
    fun stopServer() {
        if (!serverStarted) return
        runCatching { activeServer?.stop() }
        activeServer = null
        serverStarted = false
        Log.i(TAG, "SMS REST Server arrete")
    }

    @Synchronized
    @Suppress("unused")
    fun updatePort(newPort: Int) {
        val sanitized = newPort.coerceIn(1, 65535)
        if (sanitized == currentPort) return

        val shouldRestart = serverStarted
        if (shouldRestart) {
            stopServer()
        }

        currentPort = sanitized

        if (shouldRestart) {
            startServer()
        }
    }

    override fun serve(session: IHTTPSession): Response {
        try {
            val headers = session.headers
            val authHeader = headers["authorization"]
            val expectedToken = "Bearer ${ApiTokenManager.getToken(context)}"

            if (authHeader == null || authHeader != expectedToken) {
                saveApiLog("❌ Unauthorized ${session.method} ${session.uri}")
                val json = JSONObject()
                json.put("success", false)
                json.put("error", "Unauthorized: missing or invalid token")
                json.put("timestamp", System.currentTimeMillis())
                return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json", json.toString())
            }

            return when {
                session.method == Method.POST && session.uri == "/api/send-message" -> {
                    val map = HashMap<String, String>()
                    session.parseBody(map)
                    val body = map["postData"]
                    if (body.isNullOrBlank()) {
                        saveApiLog("❌ Missing body POST /api/send-message")
                        return createErrorResponse(Response.Status.BAD_REQUEST, "Missing request body", 400)
                    }
                    handleSendMessage(body)
                }
                session.method == Method.POST && session.uri == "/api/send-sms" -> {
                    val map = HashMap<String, String>()
                    session.parseBody(map)
                    val body = map["postData"]
                    if (body.isNullOrBlank()) {
                        saveApiLog("❌ Missing body POST /api/send-sms")
                        return createErrorResponse(Response.Status.BAD_REQUEST, "Missing request body", 400)
                    }
                    handleSendSms(body)
                }
                session.method == Method.POST && session.uri == "/api/send-mms" -> {
                    val map = HashMap<String, String>()
                    session.parseBody(map)
                    val body = map["postData"]
                    if (body.isNullOrBlank()) {
                        saveApiLog("❌ Missing body POST /api/send-mms")
                        return createErrorResponse(Response.Status.BAD_REQUEST, "Missing request body", 400)
                    }
                    handleSendMms(body)
                }
                session.method == Method.GET && session.uri == "/api/logs" -> {
                    handleGetLogs()
                }
                session.method == Method.POST && session.uri == "/api/logs" -> {
                    val map = HashMap<String, String>()
                    session.parseBody(map)
                    val body = map["postData"]
                    if (body.isNullOrBlank()) {
                        saveApiLog("❌ Missing body POST /api/logs")
                        return createErrorResponse(Response.Status.BAD_REQUEST, "Missing request body", 400)
                    }
                    handleLogMessage(body)
                }
                session.method == Method.GET && session.uri == "/api/health" -> {
                    handleHealthCheck()
                }
                session.method == Method.GET && session.uri == "/api/battery" -> {
                    handleBatteryStatus()
                }
                else -> {
                    saveApiLog("❌ Endpoint not found ${session.method} ${session.uri}")
                    createErrorResponse(Response.Status.NOT_FOUND, "Endpoint not found", 404)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur non gérée dans serve()", e)
            saveApiLog("❌ Internal server error: ${e.message}")
            return createErrorResponse(Response.Status.INTERNAL_ERROR, "Internal server error: ${e.message}")
        }
    }

    private fun handleHealthCheck(): Response {
        val json = JSONObject()
        json.put("success", true)
        json.put("status", "online")
        json.put("version", API_VERSION)
        json.put("timestamp", System.currentTimeMillis())
        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
    }

    private fun handleBatteryStatus(): Response {
        return try {
            val batteryIntent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            if (batteryIntent == null) {
                return createErrorResponse(Response.Status.INTERNAL_ERROR, "Battery data unavailable")
            }

            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            if (level < 0 || scale <= 0) {
                return createErrorResponse(Response.Status.INTERNAL_ERROR, "Invalid battery values")
            }

            val percent = (level * 100) / scale
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

            val json = JSONObject().apply {
                put("ok", true)
                put("level", percent)
                put("isCharging", isCharging)
                put("timestamp", System.currentTimeMillis())
            }

            newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Erreur handleBatteryStatus", e)
            createErrorResponse(Response.Status.INTERNAL_ERROR, "Error: ${e.message}")
        }
    }

    private fun handleSendMessage(body: String): Response {
        try {
            val json = JSONObject(body)
            val senderId = json.optString("senderId", "").ifBlank { null }
            val destinataire = json.optString("destinataire", "").trim()
            val text = json.optString("text", "").trim()
            val base64Jpeg = if (json.has("base64Jpeg") && !json.isNull("base64Jpeg")) {
                json.getString("base64Jpeg")
            } else {
                null
            }

            // Valide les champs
            if (destinataire.isBlank() || text.isBlank()) {
                emitEvent(RestServerEventType.SMS_SENT_ERROR, "❌ Erreur: destinataire ou texte manquants")
                return createErrorResponse(Response.Status.BAD_REQUEST, "Missing destinataire or text", 400)
            }

            // Valide et formate le numéro
            val normalizedNumber = PhoneNumberValidator.normalize(destinataire)
                ?: run {
                    emitEvent(RestServerEventType.SMS_SENT_ERROR, "❌ Erreur: numéro invalide ($destinataire)")
                    return createErrorResponse(Response.Status.BAD_REQUEST, "Invalid phone number: $destinataire", 400)
                }

            val request = SendMessageRequest(senderId, normalizedNumber, text, base64Jpeg)
            val result = runBlocking { sendRestMessageUseCase(request) }

            return when (result) {
                is SendResult.Success -> {
                    val messageType = if (base64Jpeg.isNullOrBlank()) "SMS" else "MMS"
                    val eventMessage = "✅ $messageType envoyé vers $normalizedNumber"
                    emitEvent(RestServerEventType.SMS_SENT_SUCCESS, eventMessage)
                    saveApiLog(eventMessage)
                    createSuccessResponse("$messageType envoyé avec succès vers $normalizedNumber", mapOf(
                        "type" to messageType,
                        "phoneNumber" to normalizedNumber
                    ))
                }
                is SendResult.Error -> {
                    val eventMessage = "❌ ${result.message}"
                    emitEvent(RestServerEventType.SMS_SENT_ERROR, eventMessage)
                    saveApiLog(eventMessage)
                    createErrorResponse(Response.Status.INTERNAL_ERROR, result.message, result.code)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur handleSendMessage", e)
            val eventMessage = "❌ Erreur: ${e.message}"
            emitEvent(RestServerEventType.SMS_SENT_ERROR, eventMessage)
            saveApiLog(eventMessage)
            return createErrorResponse(Response.Status.INTERNAL_ERROR, "Error: ${e.message}")
        }
    }

    private fun handleSendSms(body: String): Response {
        try {
            val json = JSONObject(body)
            val senderId = json.optString("senderId", "").ifBlank { null }
            val destinataire = json.optString("destinataire", "").trim()
            val text = json.optString("text", "").trim()

            if (destinataire.isBlank() || text.isBlank()) {
                return createErrorResponse(Response.Status.BAD_REQUEST, "Missing destinataire or text", 400)
            }

            val normalizedNumber = PhoneNumberValidator.normalize(destinataire)
                ?: return createErrorResponse(Response.Status.BAD_REQUEST, "Invalid phone number", 400)

            // Vérifie la longueur du SMS
            val partCount = OvhSmsConfig.calculateSmsPartCount(text)
            Log.d(TAG, "SMS: $partCount partie(s) - ${text.length} caractères")

            val request = SendMessageRequest(senderId, normalizedNumber, text, null)
            val result = runBlocking { sendRestMessageUseCase(request) }

            return when (result) {
                is SendResult.Success -> {
                    val eventMessage = "✅ SMS envoyé en $partCount partie(s)"
                    emitEvent(RestServerEventType.SMS_SENT_SUCCESS, eventMessage)
                    saveApiLog(eventMessage)
                    createSuccessResponse("SMS envoyé avec succès", mapOf(
                        "type" to "SMS",
                        "parts" to partCount.toString(),
                        "characters" to text.length.toString()
                    ))
                }
                is SendResult.Error -> {
                    val eventMessage = "❌ ${result.message}"
                    emitEvent(RestServerEventType.SMS_SENT_ERROR, eventMessage)
                    saveApiLog(eventMessage)
                    createErrorResponse(Response.Status.INTERNAL_ERROR, result.message, result.code)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur handleSendSms", e)
            val eventMessage = "❌ Erreur: ${e.message}"
            saveApiLog(eventMessage)
            return createErrorResponse(Response.Status.INTERNAL_ERROR, "Error: ${e.message}")
        }
    }

    private fun handleSendMms(body: String): Response {
        try {
            val json = JSONObject(body)
            val senderId = json.optString("senderId", "").ifBlank { null }
            val destinataire = json.optString("destinataire", "").trim()
            val text = json.optString("text", "").trim()
            val base64Jpeg = json.optString("base64Jpeg", "").ifBlank { null }

            if (destinataire.isBlank() || base64Jpeg.isNullOrBlank()) {
                return createErrorResponse(Response.Status.BAD_REQUEST, "Missing destinataire or image", 400)
            }

            val normalizedNumber = PhoneNumberValidator.normalize(destinataire)
                ?: return createErrorResponse(Response.Status.BAD_REQUEST, "Invalid phone number", 400)

            Log.d(TAG, "MMS: image ${base64Jpeg.length} caractères base64")

            val request = SendMessageRequest(senderId, normalizedNumber, text, base64Jpeg)
            val result = runBlocking { sendRestMessageUseCase(request) }

            return when (result) {
                is SendResult.Success -> {
                    val eventMessage = "✅ MMS envoyé avec image"
                    emitEvent(RestServerEventType.SMS_SENT_SUCCESS, eventMessage)
                    saveApiLog(eventMessage)
                    createSuccessResponse("MMS envoyé avec succès", mapOf(
                        "type" to "MMS",
                        "imageSize" to base64Jpeg.length.toString()
                    ))
                }
                is SendResult.Error -> {
                    val eventMessage = "❌ ${result.message}"
                    emitEvent(RestServerEventType.SMS_SENT_ERROR, eventMessage)
                    saveApiLog(eventMessage)
                    createErrorResponse(Response.Status.INTERNAL_ERROR, result.message, result.code)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur handleSendMms", e)
            val eventMessage = "❌ Erreur: ${e.message}"
            saveApiLog(eventMessage)
            return createErrorResponse(Response.Status.INTERNAL_ERROR, "Error: ${e.message}")
        }
    }

    private fun handleGetLogs(): Response {
        return try {
            val logs = runBlocking { logDao.getLast5Logs() }
            val logsArray = JSONArray()
            logs.forEach { log ->
                logsArray.put(
                    JSONObject().apply {
                        put("id", log.id)
                        put("message", log.message)
                        put("timestamp", log.timestamp)
                    }
                )
            }

            val json = JSONObject().apply {
                put("success", true)
                put("count", logs.size)
                put("logs", logsArray)
                put("timestamp", System.currentTimeMillis())
            }
            newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Erreur handleGetLogs", e)
            createErrorResponse(Response.Status.INTERNAL_ERROR, "Error: ${e.message}")
        }
    }

    private fun handleLogMessage(body: String): Response {
        try {
            val json = JSONObject(body)
            val message = json.optString("message", "").trim()
            if (message.isBlank()) {
                return createErrorResponse(Response.Status.BAD_REQUEST, "Missing log message", 400)
            }

            runBlocking {
                logDao.insertLog(com.miseservice.smsovh.data.local.LogEntity(message = message))
                logDao.deleteOldLogs()
                emitEvent(RestServerEventType.LOG_RECEIVED_SUCCESS, "✅ Log reçu et enregistré")
            }

            return createSuccessResponse("Log enregistré", mapOf("message" to message))
        } catch (e: Exception) {
            Log.e(TAG, "Erreur handleLogMessage", e)
            emitEvent(RestServerEventType.LOG_RECEIVED_ERROR, "❌ ${e.message}")
            return createErrorResponse(Response.Status.INTERNAL_ERROR, "Error: ${e.message}")
        }
    }

    private fun createSuccessResponse(message: String, extras: Map<String, String> = emptyMap()): Response {
        val json = JSONObject()
        json.put("success", true)
        json.put("message", message)
        json.put("timestamp", System.currentTimeMillis())

        for ((key, value) in extras) {
            json.put(key, value)
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
    }

    private fun createErrorResponse(status: Response.Status, error: String, code: Int = 500): Response {
        val json = JSONObject()
        json.put("success", false)
        json.put("error", error)
        json.put("code", code)
        json.put("timestamp", System.currentTimeMillis())
        return newFixedLengthResponse(status, "application/json", json.toString())
    }

    private fun emitEvent(type: RestServerEventType, message: String) {
        try {
            runBlocking {
                restServerEventManager.emitEvent(RestServerEvent(type, message))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erreur lors de l'émission d'événement", e)
        }
    }

    private fun saveApiLog(message: String) {
        runCatching {
            runBlocking {
                logDao.insertLog(com.miseservice.smsovh.data.local.LogEntity(message = message))
                logDao.deleteOldLogs()
            }
        }.onFailure { error ->
            Log.w(TAG, "Erreur sauvegarde log API", error)
        }
    }
}
