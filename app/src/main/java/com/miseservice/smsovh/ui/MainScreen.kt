package com.miseservice.smsovh.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.miseservice.smsovh.R
import com.miseservice.smsovh.ui.components.ApiNetworkSection
import com.miseservice.smsovh.ui.components.MainHeader
import com.miseservice.smsovh.ui.components.OvhSmsFormSection
import com.miseservice.smsovh.ui.components.SendSmsSection
import com.miseservice.smsovh.ui.components.ServiceStatusRow
import com.miseservice.smsovh.ui.components.dialogs.LocationPermissionDeniedDialog
import com.miseservice.smsovh.ui.components.dialogs.LocationPermissionRequiredDialog
import com.miseservice.smsovh.util.NetworkInfoProvider
import com.miseservice.smsovh.viewmodel.FeedbackType
import com.miseservice.smsovh.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    hasSendSmsPermission: () -> Boolean,
    onRequestSmsPermission: () -> Unit,
    onSendSmsRequested: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uiState = viewModel.uiState.collectAsState().value
    val showLocationDeniedDialog = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val copyToClipboard: (String, String) -> Unit = { label, value ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        val messageRes = when (label) {
            "send-endpoint" -> R.string.endpoint_send_copied
            "logs-endpoint" -> R.string.endpoint_logs_copied
            "battery-endpoint" -> R.string.endpoint_battery_copied
            "connected-ip" -> R.string.connected_ip_copied
            "token" -> R.string.token_copied
            "location" -> R.string.location_copied
            "network" -> R.string.network_copied
            else -> R.string.endpoint_copied
        }
        Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    // Champs du formulaire SMS pilotés par le ViewModel
    val senderId = uiState.senderId
    val recipient = uiState.recipient
    val message = uiState.message
    val activity = context as? android.app.Activity

    LaunchedEffect(uiState.serviceActive) {
        if (uiState.serviceActive) {
            while (true) {
                viewModel.refreshHostIp(NetworkInfoProvider.getHostIp())
                delay(1500)
            }
        } else {
            viewModel.refreshHostIp(NetworkInfoProvider.getHostIp())
        }
    }

    // Gestion de la permission de localisation
    val locationPermissionGranted = uiState.locationPermissionGranted
    var showLocationDialog by rememberSaveable { mutableStateOf(false) }

    val startupPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        viewModel.setLocationPermissionGranted(locationGranted)
        showLocationDialog = false
        if (!locationGranted) {
            showLocationDeniedDialog.value = true
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setLocationPermissionGranted(granted)
        showLocationDialog = false
        if (!granted) {
            showLocationDeniedDialog.value = true
        }
    }
    LaunchedEffect(Unit) {
        val locationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val smsGranted = hasSendSmsPermission()

        viewModel.setLocationPermissionGranted(locationGranted)
        showLocationDialog = false

        if (!locationGranted || !smsGranted) {
            startupPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    if (showLocationDialog && activity != null) {
        LocationPermissionRequiredDialog(
            onAllow = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            onOpenSettings = {
                val intent =
                    android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = ("package:" + context.packageName).toUri()
                context.startActivity(intent)
            }
        )
    }

    if (showLocationDeniedDialog.value) {
        LocationPermissionDeniedDialog(
            onOpenSettings = {
                showLocationDeniedDialog.value = false
                val intent =
                    android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = ("package:" + context.packageName).toUri()
                context.startActivity(intent)
            },
            onDismiss = { showLocationDeniedDialog.value = false }
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
                } catch (_: Exception) {
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

    // Récupération du token API sécurisé
    val token by viewModel.token.collectAsState()

    // Affichage du feedback visuel (succès/erreur)
    LaunchedEffect(uiState.feedbackMessage) {
        if (uiState.feedbackMessage != null) {
            snackbarHostState.showSnackbar(
                message = uiState.feedbackMessage,
                duration = if (uiState.feedbackType == FeedbackType.SUCCESS) SnackbarDuration.Short else SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                Snackbar(
                    snackbarData,
                    modifier = Modifier.padding(16.dp),
                    containerColor = when (uiState.feedbackType) {
                        FeedbackType.SUCCESS -> MaterialTheme.colorScheme.primary
                        FeedbackType.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.surface
                    },
                    contentColor = when (uiState.feedbackType) {
                        FeedbackType.SUCCESS, FeedbackType.ERROR -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(20.dp)
        ) {
            MainHeader()
            Spacer(modifier = Modifier.height(12.dp))
            ServiceStatusRow(
                uiState = uiState,
                onCheckedChange = { checked -> viewModel.setServiceActive(checked) }
            )
            Spacer(Modifier.height(32.dp))
            OvhSmsFormSection(
                senderId = senderId,
                recipient = recipient,
                message = message,
                onSenderIdChange = { viewModel.setSenderId(it.take(11)) },
                onRecipientChange = { viewModel.setRecipient(it) },
                onMessageChange = { viewModel.setMessage(it) }
            )
            ApiNetworkSection(
                uiState = uiState,
                token = token,
                locationPermissionGranted = locationPermissionGranted,
                locationData = locationData,
                onRestPortInputChange = { viewModel.setRestPortInput(it) },
                onRestPortCommit = { isValidPort ->
                    viewModel.commitRestPort()
                    if (isValidPort) {
                        focusManager.clearFocus()
                    }
                },
                onResetToken = { viewModel.resetToken() },
                onCopy = copyToClipboard
            )
            SendSmsSection(
                enabled = uiState.canSendLocalSms,
                hasSendSmsPermission = hasSendSmsPermission,
                onRequestSmsPermission = onRequestSmsPermission,
                onSendSmsRequested = onSendSmsRequested
            )
            // Suppression de l'affichage des logs/statuts
        }
    }
}
