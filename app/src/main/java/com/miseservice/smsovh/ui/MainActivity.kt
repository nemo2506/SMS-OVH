package com.miseservice.smsovh.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.miseservice.smsovh.R
import com.miseservice.smsovh.util.SmsHelper
import com.miseservice.smsovh.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    // Ouvre la page des paramètres de l'application pour accorder la permission manuellement
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
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
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        val viewModel: MainViewModel by viewModels()
        setContent {
            MainScreen(viewModel)
        }
    }
}
