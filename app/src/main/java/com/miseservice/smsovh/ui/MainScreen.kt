package com.miseservice.smsovh.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miseservice.smsovh.R
import com.miseservice.smsovh.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var senderId by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val token = remember { com.miseservice.smsovh.util.ApiTokenManager.getToken(context) }
    // Récupération de l'IP active
    val hostIp = remember { com.miseservice.smsovh.util.NetworkInfoProvider.getHostIp() }

    // Suppression de la gestion d'URL et des logs/statuts

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.header_sms),
                fontSize = 42.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.header_ovh),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            // Affichage de l'IP active et des endpoints
            Text(
                text = "IP active : $hostIp",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "Endpoints : http://$hostIp/api/logs, http://$hostIp/api/send",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.height(32.dp))
        // Section config
        Text(
            text = stringResource(R.string.config_ovh),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = senderId,
            onValueChange = { senderId = it.take(11) },
            label = { Text(stringResource(R.string.sender_id_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = recipient,
            onValueChange = { recipient = it },
            label = { Text(stringResource(R.string.recipient_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text(stringResource(R.string.message_hint)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 8,
            minLines = 4
        )
        Spacer(Modifier.height(14.dp))
        // Séparateur
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
        Spacer(Modifier.height(8.dp))
        // Suppression du champ URL API
        // Suppression du token API affiché
        // Bouton envoyer (API auto, plus d'URL à saisir)
        Button(
            onClick = {
                viewModel.sendSms(com.miseservice.smsovh.model.SmsMessage(senderId, recipient, message))
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = recipient.isNotBlank() && message.isNotBlank()
        ) {
            Text(stringResource(R.string.send_sms), fontSize = 15.sp)
        }
        Spacer(Modifier.height(12.dp))
        // Suppression de l'affichage des logs/statuts
    }
}
