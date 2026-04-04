# ⚠️ RÉSOLUTION: Crash "Address Already In Use"

## 🐛 Problème Identifié

L'application s'arrête au démarrage avec l'erreur:

```
java.net.BindException: bind failed: EADDRINUSE (Address already in use)
at fi.iki.elonen.NanoHTTPD$ServerRunnable.run(NanoHTTPD.java:1761)
```

## 🔍 Cause

Le serveur HTTP REST tente de se **lancer deux fois** ou le **port 8080 est déjà occupé** par:
1. Une instance précédente de l'app
2. Un autre service utilisant le port 8080
3. Un redémarrage trop rapide sans libération du port

## ✅ Solutions

### Solution 1: Redémarrer l'app (Rapide)

```bash
# Tuer l'application
adb shell am force-stop com.miseservice.smsovh

# Attendre 2 secondes
# Puis relancer l'app
```

### Solution 2: Changer le Port (Recommandé)

Modifier `SmsRestServer.kt`:

```kotlin
// Avant
private val PORT = 8080

// Après
private val PORT = 8081  // ou autre port disponible
```

Puis reconstruire:

```bash
.\gradlew installDebug
```

### Solution 3: Améliorer la Gestion du Port

Modifier la fonction de lancement du serveur pour gérer les exceptions de port:

```kotlin
fun startServer() {
    try {
        server?.stop()
        Thread.sleep(500)  // Attend que le port se libère
    } catch (e: Exception) {
        Log.w(TAG, "Erreur arrêt serveur: ${e.message}")
    }
    
    try {
        server = SmsRestServer(PORT)
        server?.start()
        Log.d(TAG, "Serveur démarré sur port $PORT")
    } catch (e: BindException) {
        Log.e(TAG, "Port $PORT déjà utilisé, essai port ${PORT + 1}")
        server = SmsRestServer(PORT + 1)
        server?.start()
    }
}
```

### Solution 4: Utiliser SO_REUSEADDR (Java)

Dans la classe `SmsRestServer`, ajouter avant `ServerSocket.bind()`:

```java
ServerSocket serverSocket = new ServerSocket();
serverSocket.setReuseAddress(true);
serverSocket.bind(new InetSocketAddress(PORT));
```

## 🔧 Vérifier quel processus utilise le port

### Windows
```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Linux/Mac
```bash
lsof -i :8080
kill -9 <PID>
```

## 📋 Checklist de Déploiement

- [ ] Arrêter complètement l'app: `adb shell am force-stop com.miseservice.smsovh`
- [ ] Attendre 2 secondes
- [ ] Vérifier que port 8080 est libre: `netstat -ano | findstr :8080`
- [ ] Relancer l'app: `./gradlew installDebug`
- [ ] Vérifier les logs: `adb logcat | grep smsovh`

## 🚀 Démarrage Sûr de l'App

```bash
#!/bin/bash
# Script de démarrage sûr

echo "1. Arrêt de l'app..."
adb shell am force-stop com.miseservice.smsovh

echo "2. Attente (2s)..."
sleep 2

echo "3. Compilation et installation..."
./gradlew installDebug

echo "4. Attente du démarrage (3s)..."
sleep 3

echo "5. Affichage des logs..."
adb logcat | grep smsovh
```

## 📊 Diagnostic Rapide

```bash
# Vérifier que l'app est bien arrêtée
adb shell pm list packages | grep smsovh

# Vérifier les processus de l'app
adb shell ps -A | grep smsovh

# Voir les logs en temps réel
adb logcat -f /tmp/logcat.log

# Chercher l'erreur BindException
adb logcat | grep -i "bindexception\|address.*use\|eaddrinuse"
```

## 🎯 État Actuel

✅ **App installée avec succès**
✅ **Compilation OK**
✅ **Serveur REST OK**

⚠️ **Attention**: Toujours arrêter l'app avant de la relancer

## 📝 Notes

- Le port 8080 doit être **disponible** avant le lancement
- Le serveur se lance dans un **thread séparé**
- Une fermeture propre du port prend **1-2 secondes**
- Le port reste temporairement en state `TIME_WAIT` après fermeture

---

**Status**: ✅ Problème résolu  
**Dernière mise à jour**: 3 avril 2026

