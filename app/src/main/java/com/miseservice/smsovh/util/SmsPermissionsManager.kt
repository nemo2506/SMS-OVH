package com.miseservice.smsovh.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Gère les permissions requises pour l'envoi de SMS et MMS.
 */
object SmsPermissionsManager {

    /**
     * Liste des permissions nécessaires à l'envoi (sans lecture de boîte SMS).
     */
    val REQUIRED_PERMISSIONS: List<String> = buildList {
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.INTERNET)
        add(Manifest.permission.ACCESS_NETWORK_STATE)
        add(Manifest.permission.CHANGE_NETWORK_STATE)
        add(Manifest.permission.READ_PHONE_STATE)

        // RECEIVE_MMS n'est utile que pour la réception, conservée ici pour compatibilité.
        add(Manifest.permission.RECEIVE_MMS)
    }

    /**
     * Permissions optionnelles pour une expérience améliorée
     */
    @Suppress("unused")
    val OPTIONAL_PERMISSIONS = listOf(
        Manifest.permission.VIBRATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /**
     * Vérifie si une permission est accordée
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Vérifie si toutes les permissions requises sont accordées
     */
    @Suppress("unused")
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all { hasPermission(context, it) }
    }

    /**
     * Retourne la liste des permissions manquantes
     */
    fun getMissingPermissions(context: Context): List<String> {
        return REQUIRED_PERMISSIONS.filter { !hasPermission(context, it) }
    }

    /**
     * Convertit une liste de permissions manquantes en array pour demande de permission
     */
    @Suppress("unused")
    fun getMissingPermissionsArray(context: Context): Array<String> {
        return getMissingPermissions(context).toTypedArray()
    }

    /**
     * Vérifie les permissions SMS critiques pour l'envoi
     */
    fun canSendSms(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.SEND_SMS) &&
               hasPermission(context, Manifest.permission.INTERNET)
    }

    /**
     * Vérifie les permissions pour l'envoi de MMS
     */
    fun canSendMms(context: Context): Boolean {
        return canSendSms(context) &&
               hasPermission(context, Manifest.permission.CHANGE_NETWORK_STATE) &&
               hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)
    }
}
