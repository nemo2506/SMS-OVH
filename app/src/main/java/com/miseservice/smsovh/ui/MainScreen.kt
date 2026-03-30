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
    val apiUrl by viewModel.apiUrl.collectAsState()
    val apiLogs by viewModel.apiLogs.collectAsState()
    val sendResult by viewModel.sendResult.collectAsState()
    var senderId by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val apiUrlInput = remember { mutableStateOf(apiUrl) }
    val token = remember { com.miseservice.smsovh.util.ApiTokenManager.getToken(context) }
    val showStatus = remember { mutableStateOf(false) }
    val statusText = remember { mutableStateOf("") }
    val statusSuccess = remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(apiUrl) {
        apiUrlInput.value = apiUrl
    }

    LaunchedEffect(sendResult) {
        sendResult?.let {
            showStatus.value = true
            statusSuccess.value = it is com.miseservice.smsovh.model.SendResult.Success
            statusText.value = when (it) {
                is com.miseservice.smsovh.model.SendResult.Success -> context.getString(R.string.sms_sent_success)
                is com.miseservice.smsovh.model.SendResult.Error -> it.message
            }
        }
    }

    LaunchedEffect(apiLogs) {
        apiLogs?.let {
            showStatus.value = true
            statusSuccess.value = it.contains("Success")
            statusText.value = it
        }
    }

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
        // API URL admin
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.admin_api_url),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = apiUrlInput.value,
                onValueChange = { apiUrlInput.value = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("https://...") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                viewModel.saveApiUrl(apiUrlInput.value)
                Toast.makeText(context, "URL API enregistrée", Toast.LENGTH_SHORT).show()
            }) {
                Text("OK", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        // Token API
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Text("Token API : ", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            Text(token, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(token))
                Toast.makeText(context, context.getString(R.string.token_copied), Toast.LENGTH_SHORT).show()
            }) {
                Text("Copier", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        // Bouton envoyer
        Button(
            onClick = {
                if (apiUrlInput.value.isNotBlank() && (apiUrlInput.value.startsWith("http://") || apiUrlInput.value.startsWith("https://"))) {
                    viewModel.sendApiMessage(apiUrlInput.value, token, senderId, recipient, message)
                } else {
                    viewModel.sendSms(com.miseservice.smsovh.model.SmsMessage(senderId, recipient, message))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = recipient.isNotBlank() && message.isNotBlank()
        ) {
            Text(stringResource(R.string.send_sms), fontSize = 15.sp)
        }
        Spacer(Modifier.height(12.dp))
        // Statut/logs
        if (showStatus.value) {
            Surface(
                color = if (statusSuccess.value) Color(0xFFDFF0D8) else Color(0xFFF8D7DA),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                Text(
                    text = statusText.value,
                    color = if (statusSuccess.value) Color(0xFF3C763D) else Color(0xFFA94442),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
