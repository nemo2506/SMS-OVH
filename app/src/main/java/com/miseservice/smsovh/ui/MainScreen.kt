package com.miseservice.smsovh.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miseservice.smsovh.R
import com.miseservice.smsovh.util.SmsHelper
import com.miseservice.smsovh.viewmodel.MainViewModel
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsState().value

    // Bouton Activer/Désactiver le service foreground (Jetpack Compose Switch)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = if (uiState.serviceActive) "Service actif" else "Service inactif",
            color = if (uiState.serviceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = uiState.serviceActive,
            onCheckedChange = { checked -> viewModel.setServiceActive(checked) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.error
            )
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    // Champs du formulaire SMS pilotés par le ViewModel
    val senderId = uiState.senderId
    val recipient = uiState.recipient
    val message = uiState.message
    val activity = context as? android.app.Activity

    // Détection du type de réseau (WiFi, mobile, etc.)
    val networkType = uiState.networkType

    // Gestion de la permission de localisation
    val locationPermissionGranted = uiState.locationPermissionGranted
    var showLocationDialog by remember { mutableStateOf(false) }
    if (!locationPermissionGranted && activity != null) {
        showLocationDialog = true
    }
    if (showLocationDialog && activity != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Permission de localisation requise") },
            text = { Text("L'application a besoin de la localisation pour afficher les informations réseau. Veuillez l'accorder.") },
            confirmButton = {
                Button(onClick = {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        1002
                    )
                    showLocationDialog = false
                    // Arrêt du service foreground après réponse utilisateur
                    SmsHelper.stopSmsOvhForegroundService(context)
                    viewModel.setLocationPermissionGranted(true)
                }) { Text("Autoriser") }
            },
            dismissButton = {
                Button(onClick = {
                    val intent =
                        android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = ("package:" + context.packageName).toUri()
                    context.startActivity(intent)
                    // Arrêt du service foreground après réponse utilisateur
                    SmsHelper.stopSmsOvhForegroundService(context)
                }) { Text("Réglages") }
            }
        )
    }

    // Récupération de la localisation réseau
    val locationData = uiState.locationData
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val l = try {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        locationManager.getLastKnownLocation(provider)
                    } else null
                } catch (e: Exception) {
                    null
                }
                if (l != null && (bestLocation == null || l.accuracy < bestLocation.accuracy)) {
                    bestLocation = l
                }
            }
            bestLocation?.let {
                viewModel.setLocationData(Pair(it.latitude, it.longitude))
            }
        }
    }

    // Récupération dynamique de l'IP active
    val hostIp = uiState.hostIp
    val isIpValid = uiState.isIpValid

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
                color = colorResource(id = R.color.smsovh_primary),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.header_ovh),
                fontSize = 13.sp,
                color = colorResource(id = R.color.smsovh_primary),
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            // Cadre convivial pour IP active, endpoints et localisation
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "IP",
                            tint = if (isIpValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isIpValid) "IP active : $hostIp" else "IP non connectée",
                            fontSize = 15.sp,
                            color = colorResource(id = R.color.smsovh_primary),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isIpValid)
                            "Endpoints : http://$hostIp/api/logs\n              http://$hostIp/api/send"
                        else
                            "Aucun endpoint disponible",
                        fontSize = 12.sp,
                        color = colorResource(id = R.color.smsovh_primary)
                    )
                    Spacer(Modifier.height(10.dp))
                    // Affichage des coordonnées de localisation si disponibles
                    if (locationPermissionGranted) {
                        if (locationData != null) {
                            Text(
                                text = "Localisation : ${
                                    String.format(
                                        Locale.getDefault(),
                                        "%.5f",
                                        locationData.first
                                    )
                                }, ${String.format(Locale.getDefault(), "%.5f", locationData.second)}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Localisation : en cours...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Localisation non autorisée",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    // Affichage du type de réseau
                    Text(
                        text = "Réseau : $networkType",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        // Section config
        Text(
            text = stringResource(R.string.config_ovh),
            fontSize = 11.sp,
            color = colorResource(id = R.color.smsovh_primary),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = senderId,
            onValueChange = { viewModel.setSenderId(it.take(11)) },
            label = { Text(stringResource(R.string.sender_id_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = recipient,
            onValueChange = { viewModel.setRecipient(it) },
            label = { Text(stringResource(R.string.recipient_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = message,
            onValueChange = { viewModel.setMessage(it) },
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
                viewModel.sendSms()
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
