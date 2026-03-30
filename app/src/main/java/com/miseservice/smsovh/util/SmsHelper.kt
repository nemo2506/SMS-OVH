package com.miseservice.smsovh.util

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import com.miseservice.smsovh.R
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object SmsHelper {

            /**
             * Arrête le service foreground (notification permanente)
             */
            fun stopSmsOvhForegroundService(context: Context) {
                val stopIntent = Intent(context, com.miseservice.smsovh.service.SmsOvhForegroundService::class.java)
                stopIntent.action = com.miseservice.smsovh.service.SmsOvhForegroundService.ACTION_STOP
                context.stopService(stopIntent)
            }
    private const val SMS_SENT = "SMS_SENT"
    private const val SMS_DELIVERED = "SMS_DELIVERED"

    // Nouvelle fonction pour envoyer les logs à une API et obtenir la réponse JSON
    private fun sendLogToApi(
    message: String,
    callback: (JSONObject) -> Unit
    ) {
        val client = OkHttpClient()
        val url = "http://${NetworkInfoProvider.getHostIp()}/api/logs"
        val json = JSONObject().apply { put("message", message) }
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(JSONObject().apply {
                    put("success", false)
                    put("exception", e.message ?: "Unknown exception")
                })
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseBody = response.body?.string()
                val result = if (response.isSuccessful && responseBody != null) {
                    JSONObject(responseBody)
                } else {
                    JSONObject().apply {
                        put("success", false)
                        put("error", responseBody ?: "Unknown error")
                    }
                }
                callback(result)
            }
        })
    }


    fun sendSmsWithStatus(context: Context, phoneNumber: String, message: String) {
        // Vérification SIM
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simState = telephonyManager.simState
        if (simState != TelephonyManager.SIM_STATE_READY) {
            sendLogToApi(context.getString(R.string.sim_absente_ou_invalide)) { response ->
                android.util.Log.d("SmsHelperApiResponse", response.toString())
            }
            return
        }
        val sentPI = PendingIntent.getBroadcast(
            context, 0,
            Intent(SMS_SENT),
            PendingIntent.FLAG_IMMUTABLE
        )
        val deliveredPI = PendingIntent.getBroadcast(
            context, 0,
            Intent(SMS_DELIVERED),
            PendingIntent.FLAG_IMMUTABLE
        )
        val receiverFlag =
            if (android.os.Build.VERSION.SDK_INT >= 33) Context.RECEIVER_NOT_EXPORTED else 0
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (resultCode) {
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> sendLogToApi(ctx.getString(R.string.sms_generic_failure)) { response -> android.util.Log.d("SmsHelperApiResponse", response.toString()) }
                    SmsManager.RESULT_ERROR_NO_SERVICE -> sendLogToApi(ctx.getString(R.string.sms_no_service)) { response -> android.util.Log.d("SmsHelperApiResponse", response.toString()) }
                    SmsManager.RESULT_ERROR_NULL_PDU -> sendLogToApi(ctx.getString(R.string.sms_null_pdu)) { response -> android.util.Log.d("SmsHelperApiResponse", response.toString()) }
                    SmsManager.RESULT_ERROR_RADIO_OFF -> sendLogToApi(ctx.getString(R.string.sms_radio_off)) { response -> android.util.Log.d("SmsHelperApiResponse", response.toString()) }
                }
            }
        }, IntentFilter(SMS_SENT), receiverFlag)
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (resultCode) {
                    Activity.RESULT_OK -> sendLogToApi(ctx.getString(R.string.sms_sent_success)) { response -> android.util.Log.d("SmsHelperApiResponse", response.toString()) }
                    Activity.RESULT_CANCELED -> sendLogToApi(ctx.getString(R.string.sms_not_delivered)) { response -> android.util.Log.d("SmsHelperApiResponse", response.toString()) }
                }
            }
        }, IntentFilter(SMS_DELIVERED), receiverFlag)
        val smsManager = if (android.os.Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        smsManager.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI)
    }

    fun sendSmsWithStatus(
        context: Context,
        phoneNumber: String,
        message: String,
        callback: ((Boolean, JSONObject) -> Unit)
    ) {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simState = telephonyManager.simState
        if (simState != TelephonyManager.SIM_STATE_READY) {
            val msg = context.getString(R.string.sim_absente_ou_invalide)
            sendLogToApi(msg) { response ->
                callback(false, response)
            }
            return
        }
        val sentPI = PendingIntent.getBroadcast(
            context, 0,
            Intent(SMS_SENT),
            PendingIntent.FLAG_IMMUTABLE
        )
        val deliveredPI = PendingIntent.getBroadcast(
            context, 0,
            Intent(SMS_DELIVERED),
            PendingIntent.FLAG_IMMUTABLE
        )
        val receiverFlag =
            if (android.os.Build.VERSION.SDK_INT >= 33) Context.RECEIVER_NOT_EXPORTED else 0
        var sentResult: Boolean? = null
        var sentMessage: String? = null
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        sentResult = true
                        sentMessage = ctx.getString(R.string.sms_sent_success)
                    }
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                        sentResult = false
                        sentMessage = ctx.getString(R.string.sms_generic_failure)
                    }
                    SmsManager.RESULT_ERROR_NO_SERVICE -> {
                        sentResult = false
                        sentMessage = ctx.getString(R.string.sms_no_service)
                    }
                    SmsManager.RESULT_ERROR_NULL_PDU -> {
                        sentResult = false
                        sentMessage = ctx.getString(R.string.sms_null_pdu)
                    }
                    SmsManager.RESULT_ERROR_RADIO_OFF -> {
                        sentResult = false
                        sentMessage = ctx.getString(R.string.sms_radio_off)
                    }
                }
                val safeResult = sentResult
                val safeMessage = sentMessage
                if (safeResult != null && safeMessage != null) {
                    sendLogToApi(safeMessage) { response ->
                        callback(safeResult, response)
                    }
                }
            }
        }, IntentFilter(SMS_SENT), receiverFlag)
        // On log aussi la livraison
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (resultCode) {
                    Activity.RESULT_OK -> sendLogToApi(ctx.getString(R.string.sms_delivered)) { response -> android.util.Log.d("SmsHelperApiResponse", response.toString()) }
                    Activity.RESULT_CANCELED -> sendLogToApi(ctx.getString(R.string.sms_not_delivered)) { response -> android.util.Log.d("SmsHelperApiResponse", response.toString()) }
                }
            }
        }, IntentFilter(SMS_DELIVERED), receiverFlag)
        val smsManager = if (android.os.Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        smsManager.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI)
    }
}
