package com.miseservice.smsovh.util

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.widget.Toast
import com.miseservice.smsovh.R

object SmsHelper {
    private const val SMS_SENT = "SMS_SENT"
    private const val SMS_DELIVERED = "SMS_DELIVERED"

    fun sendSmsWithStatus(context: Context, phoneNumber: String, message: String) {
        // Vérification SIM
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simState = telephonyManager.simState
        if (simState != TelephonyManager.SIM_STATE_READY) {
            Toast.makeText(context, context.getString(R.string.sim_absente_ou_invalide), Toast.LENGTH_LONG).show()
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
        val receiverFlag = if (android.os.Build.VERSION.SDK_INT >= 33) Context.RECEIVER_NOT_EXPORTED else 0
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (resultCode) {
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> Toast.makeText(ctx, ctx.getString(R.string.sms_generic_failure), Toast.LENGTH_SHORT).show()
                    SmsManager.RESULT_ERROR_NO_SERVICE -> Toast.makeText(ctx, ctx.getString(R.string.sms_no_service), Toast.LENGTH_SHORT).show()
                    SmsManager.RESULT_ERROR_NULL_PDU -> Toast.makeText(ctx, ctx.getString(R.string.sms_null_pdu), Toast.LENGTH_SHORT).show()
                    SmsManager.RESULT_ERROR_RADIO_OFF -> Toast.makeText(ctx, ctx.getString(R.string.sms_radio_off), Toast.LENGTH_SHORT).show()
                }
            }
        }, IntentFilter(SMS_SENT), receiverFlag)
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (resultCode) {
                    Activity.RESULT_OK -> Toast.makeText(ctx, ctx.getString(R.string.sms_sent_success), Toast.LENGTH_SHORT).show()
                    Activity.RESULT_CANCELED -> Toast.makeText(ctx, ctx.getString(R.string.sms_not_delivered), Toast.LENGTH_SHORT).show()
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
