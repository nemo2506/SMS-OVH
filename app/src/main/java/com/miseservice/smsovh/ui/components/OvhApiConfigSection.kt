package com.miseservice.smsovh.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miseservice.smsovh.R

@Composable
fun OvhApiConfigSection(
    ovhAppKey: String,
    ovhAppSecret: String,
    ovhConsumerKey: String,
    ovhServiceName: String,
    ovhEndpoint: String,
    ovhCountryPrefix: String,
    onOvhAppKeyChange: (String) -> Unit,
    onOvhAppSecretChange: (String) -> Unit,
    onOvhConsumerKeyChange: (String) -> Unit,
    onOvhServiceNameChange: (String) -> Unit,
    onOvhEndpointChange: (String) -> Unit,
    onOvhCountryPrefixChange: (String) -> Unit
) {
    Text(
        text = "Identifiants OVH",
        fontSize = 16.5.sp,
        color = colorResource(id = R.color.smsovh_primary),
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value = ovhAppKey,
        onValueChange = onOvhAppKeyChange,
        label = { Text("Application Key") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(Modifier.height(10.dp))

    OutlinedTextField(
        value = ovhAppSecret,
        onValueChange = onOvhAppSecretChange,
        label = { Text("Application Secret") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(Modifier.height(10.dp))

    OutlinedTextField(
        value = ovhConsumerKey,
        onValueChange = onOvhConsumerKeyChange,
        label = { Text("Consumer Key") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(Modifier.height(10.dp))

    OutlinedTextField(
        value = ovhServiceName,
        onValueChange = onOvhServiceNameChange,
        label = { Text("Service SMS (compte OVH)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(Modifier.height(10.dp))

    OutlinedTextField(
        value = ovhEndpoint,
        onValueChange = onOvhEndpointChange,
        label = { Text("Endpoint OVH") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        supportingText = { Text("ovh-eu, ovh-ca, ovh-us") }
    )
    Spacer(Modifier.height(10.dp))

    OutlinedTextField(
        value = ovhCountryPrefix,
        onValueChange = onOvhCountryPrefixChange,
        label = { Text("Prefixe pays (recipient)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(Modifier.height(8.dp))

    Text(
        text = "Les cles s'obtiennent sur api.ovh.com/createToken. Droit requis: POST /sms/{serviceName}/jobs",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

