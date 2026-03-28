package com.miseservice.smsovh.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import com.miseservice.smsovh.R
import com.miseservice.smsovh.model.SmsMessage
import com.miseservice.smsovh.util.SmsHelper
import com.miseservice.smsovh.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
            // Ouvre la page des paramètres de l'application pour accorder la permission manuellement
    private fun openAppSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:" + packageName)
        }
        startActivity(intent)
    }

            // Gestionnaire de permission SMS
    private val requestSendSmsPermission =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                sendSmsIfPermitted()
            } else {
                if (!androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.SEND_SMS)) {
                    android.app.AlertDialog.Builder(this)
                        .setTitle(getString(R.string.permission_required_title))
                        .setMessage(getString(R.string.permission_denied_permanently_message))
                        .setPositiveButton(getString(R.string.settings)) { _, _ -> openAppSettings() }
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show()
                } else {
                    android.widget.Toast.makeText(this, getString(R.string.permission_denied_message), android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }

            private var pendingRecipient: String? = null
            private var pendingMessage: String? = null

            private fun sendSmsIfPermitted() {
                val recipient = pendingRecipient ?: return
                val message = pendingMessage ?: return
                SmsHelper.sendSmsWithStatus(this, recipient, message)
            }
    // TODO: Fournir le ViewModel via un factory/injection
    override fun onCreate(savedInstanceState: Bundle?) {
        // Applique le thème clair/sombre selon la config système
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Vérifie et demande l'exception d'optimisation batterie si nécessaire
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = packageName
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
        val viewModel: MainViewModel by viewModels()
        val etSenderId = findViewById<EditText>(R.id.etServiceName)
        val etRecipient = findViewById<EditText>(R.id.etRecipient)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<Button>(R.id.btnSend)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tilSenderId = findViewById<TextInputLayout>(R.id.tilServiceName)
        val cardStatus = findViewById<android.view.View>(R.id.cardStatus)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        cardStatus.visibility = android.view.View.GONE
        btnSend.setOnClickListener {
            tilSenderId.error = null
            cardStatus.visibility = android.view.View.GONE
            val senderId = etSenderId.text?.toString() ?: ""
            val recipient = etRecipient.text?.toString() ?: ""
            val message = etMessage.text?.toString() ?: ""
            if (recipient.isBlank() || message.isBlank()) {
                tvStatus.text = getString(R.string.missing_number_or_message)
                cardStatus.setBackgroundResource(R.drawable.bg_status_error)
                cardStatus.visibility = android.view.View.VISIBLE
                return@setOnClickListener
            }
            btnSend.isEnabled = false
            progressBar.visibility = android.view.View.VISIBLE
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                SmsHelper.sendSmsWithStatus(this, recipient, message)
            } else {
                if (androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.SEND_SMS)) {
                    android.app.AlertDialog.Builder(this)
                        .setTitle(getString(R.string.permission_required_title))
                        .setMessage(getString(R.string.permission_required_message))
                        .setPositiveButton(getString(R.string.allow)) { _, _ ->
                            pendingRecipient = recipient
                            pendingMessage = message
                            requestSendSmsPermission.launch(android.Manifest.permission.SEND_SMS)
                        }
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show()
                } else {
                    pendingRecipient = recipient
                    pendingMessage = message
                    requestSendSmsPermission.launch(android.Manifest.permission.SEND_SMS)
                }
            }
            lifecycleScope.launch {
                viewModel.sendSms(SmsMessage(from = senderId, to = recipient, message = message))
            }
        }
        lifecycleScope.launch {
            viewModel.sendResult.collectLatest { result ->
                btnSend.isEnabled = true
                progressBar.visibility = android.view.View.GONE
                if (result == null) return@collectLatest
                when (result) {
                    is com.miseservice.smsovh.model.SendResult.Success -> {
                        tvStatus.text = getString(R.string.sms_sent_success)
                        cardStatus.setBackgroundResource(R.drawable.bg_status_success)
                        cardStatus.visibility = android.view.View.VISIBLE
                    }
                    is com.miseservice.smsovh.model.SendResult.Error -> {
                        tvStatus.text = result.message
                        cardStatus.setBackgroundResource(R.drawable.bg_status_error)
                        cardStatus.visibility = android.view.View.VISIBLE
                        if (result.code == 403) {
                            tilSenderId.error = getString(R.string.sender_id_not_allowed)
                        }
                    }
                }
            }
        }
    }

}
