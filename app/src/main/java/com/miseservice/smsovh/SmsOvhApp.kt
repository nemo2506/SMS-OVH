package com.miseservice.smsovh

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmsOvhApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialisation de la détection d'IP locale
        com.miseservice.smsovh.util.NetworkInfoProvider.init(this)
        // Démarrage du serveur REST à l'initialisation de l'application
        try {
            com.miseservice.smsovh.service.SmsRestServer(this).start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
