package com.miseservice.smsovh.service

import android.content.Context
import android.util.Log
import com.miseservice.smsovh.util.ApiTokenManager
import com.miseservice.smsovh.util.SmsHelper
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SmsRestServer(val context: Context, port: Int = 8080) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        // Vérification du token d'API
        val headers = session.headers
        val authHeader = headers["authorization"]
        val expectedToken = "Bearer ${ApiTokenManager.getToken(context)}"
        if (authHeader == null || authHeader != expectedToken) {
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "Unauthorized: missing or invalid token")
        }
        if (session.method == Method.POST && session.uri == "/api/send-message") {
            val map = HashMap<String, String>()
            session.parseBody(map)
            val body = map["postData"] ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing body")
            return handleSendMessage(body)
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
    }

    private fun handleSendMessage(body: String): Response {
        return try {
            val json = JSONObject(body)
            val senderId = json.optString("senderId", null)
            val destinataire = json.optString("destinataire", null)
            val text = json.optString("text", null)
            val base64Jpeg = json.optString("base64Jpeg", null)
            if (destinataire == null || text == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing destinataire or text")
            }
            Log.i("SmsRestServer", "Reçu: senderId=$senderId, destinataire=$destinataire, text=$text, base64Jpeg=${base64Jpeg?.length}")
            if (base64Jpeg.isNullOrEmpty()) {
                // SMS simple, on attend le retour d'état
                val latch = CountDownLatch(1)
                var result: String? = null
                var status: Response.Status = Response.Status.INTERNAL_ERROR
                SmsHelper.sendSmsWithStatus(context, destinataire, text) { success, message ->
                    result = message
                    status = if (success) Response.Status.OK else Response.Status.INTERNAL_ERROR
                    latch.countDown()
                }
                // On attend max 10 secondes la réponse
                latch.await(10, TimeUnit.SECONDS)
                newFixedLengthResponse(status, MIME_PLAINTEXT, result ?: "Timeout SMS")
            } else {
                // MMS (à implémenter selon la logique de votre projet)
                newFixedLengthResponse(Response.Status.NOT_IMPLEMENTED, MIME_PLAINTEXT, "MMS non implémenté")
            }
        } catch (e: Exception) {
            Log.e("SmsRestServer", "Erreur: ", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Erreur: ${e.message}")
        }
    }
}
