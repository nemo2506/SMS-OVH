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
 * @property restPort Port API REST actif
 * @property restPortInput Valeur saisie dans l'UI pour le port
 * @property restPortError Erreur de validation du port
 * @property isIpValid Indique si l'IP est valide
 * @property locationPermissionGranted Permission de localisation accordée
 * @property locationData Coordonnées GPS (latitude, longitude)
 * @property networkType Type de réseau actif
 * @property errorMessage Message d’erreur éventuel
 */
data class MainUiState(
    val isLoading: Boolean = false,
    val serviceToggleTargetActive: Boolean? = null,
    val serviceActive: Boolean = false,
    val senderId: String = "",
    val recipient: String = "",
    val message: String = "",
    val hostIp: String = "127.0.0.1",
    val restPort: Int = 8080,
    val restPortInput: String = "8080",
    val restPortError: String? = null,
    val isIpValid: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val locationData: Pair<Double, Double>? = null,
    val networkType: String = "Non connecté",
    val errorMessage: String? = null,
    val feedbackMessage: String? = null,
    val feedbackType: FeedbackType = FeedbackType.NONE
) {
    val canSendLocalSms: Boolean
        get() = serviceActive && !isLoading && recipient.isNotBlank() && message.isNotBlank()
}

enum class FeedbackType {
    NONE, SUCCESS, ERROR
}
