package com.miseservice.smsovh.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject
import com.miseservice.smsovh.domain.repository.AdminRepository
import com.miseservice.smsovh.domain.repository.ApiSendResult

@Singleton
class AdminRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AdminRepository {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("admin_prefs", Context.MODE_PRIVATE)
    }

    override fun getApiUrl(): String = prefs.getString("api_url", "") ?: ""
    override fun saveApiUrl(url: String) { prefs.edit().putString("api_url", url).apply() }

    override suspend fun sendApiMessage(
        apiUrl: String,
        token: String,
        senderId: String,
        recipient: String,
        message: String
    ): ApiSendResult {
        val logs = StringBuilder()
        return try {
            logs.appendLine("[API] URL: $apiUrl")
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.doOutput = true
            val json = JSONObject()
            json.put("senderId", senderId)
            json.put("destinataire", recipient)
            json.put("text", message)
            logs.appendLine("[API] Payload: $json")
            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(json.toString())
            writer.flush()
            writer.close()
            val code = conn.responseCode
            logs.appendLine("[API] HTTP code: $code")
            val responseText = conn.inputStream.bufferedReader().readText()
            logs.appendLine("[API] Response: $responseText")
            val msg = try {
                val respJson = JSONObject(responseText)
                respJson.optString("message", responseText)
            } catch (e: Exception) {
                responseText
            }
            if (code in 200..299) {
                ApiSendResult.Success(logs.appendLine("[API] Success: $msg").toString())
            } else {
                ApiSendResult.Error(logs.appendLine("[API] Erreur: $msg").toString())
            }
        } catch (e: MalformedURLException) {
            logs.appendLine("[API] URL API invalide")
            ApiSendResult.Error(logs.toString())
        } catch (e: Exception) {
            logs.appendLine("[API] Erreur réseau: ${e.localizedMessage}")
            ApiSendResult.Error(logs.toString())
        }
    }
}

