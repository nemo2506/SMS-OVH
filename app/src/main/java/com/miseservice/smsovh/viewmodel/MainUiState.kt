package com.miseservice.smsovh.viewmodel

/**
 * État de l’interface principale pour l'envoi de SMS OVH.
 *
 * @property isLoading Indique si les données sont en cours de chargement
 * @property serviceActive Indique si le service foreground est actif
 * @property senderId Identifiant de l'expéditeur
 * @property recipient Destinataire du SMS
 * @property message Contenu du SMS
 * @property hostIp Adresse IP locale de l’appareil
 * @property isIpValid Indique si l'IP est valide
 * @property locationPermissionGranted Permission de localisation accordée
 * @property locationData Coordonnées GPS (latitude, longitude)
 * @property networkType Type de réseau actif
 * @property errorMessage Message d’erreur éventuel
 */
data class MainUiState(
    val isLoading: Boolean = false,
    val serviceActive: Boolean = false,
    val senderId: String = "",
    val recipient: String = "",
    val message: String = "",
    val hostIp: String = "127.0.0.1",
    val isIpValid: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val locationData: Pair<Double, Double>? = null,
    val networkType: String = "Non connecté",
    val errorMessage: String? = null
)

