# Guide d'Intégration SMS/MMS - MS-OVH-SMS

## 📱 Vue d'ensemble

Ce projet utilise la libraire **android-smsmms** pour envoyer des SMS et MMS via l'API REST sécurisée. La libraire `android-smsmms` est une solution complète et fiable pour gérer l'envoi de messages sur Android.

---

## 🎯 Fonctionnalités Principales

### SMS (Short Message Service)
- ✅ Envoi de SMS texte (jusqu'à 160 caractères)
- ✅ Support des SMS longs (concaténation automatique)
- ✅ Support des caractères accentués
- ✅ Rapports de livraison
- ✅ Validation des numéros de téléphone

### MMS (Multimedia Message Service)
- ✅ Envoi de messages avec images (JPEG, PNG)
- ✅ Support des pièces jointes multiples
- ✅ Configuration MMSC OVH
- ✅ Gestion de la taille des images
- ✅ Paramètres APN automatiques

### Configuration OVH
- Configuration MMSC: `http://mms.ovh.net`
- Port MMS: 8080
- APN: ovh
- Support des rapports de livraison
- Caractères accentués préservés

---

## 📦 Architecture et Composants

### Couche de Présentation (UI)
- **UI Compose** - Interface utilisateur réactive
- Écrans d'envoi, historique, paramètres

### Couche Métier (Domain)
- **UseCases** - Logique métier
  - `SendMessageUseCase` - Route SMS/MMS automatiquement
  - `SendSmsUseCase` - Envoi SMS spécifique
  - `SendRestMessageUseCase` - Compatibilité API REST

### Couche de Données (Data)
- **Repository** - Abstraction d'accès aux données
  - `SmsRepositoryImpl` - Implémentation d'envoi
- **DataSource** - Accès aux services
- **Local DB** - Room pour logs et historique

### Utilitaires Clés

#### `SmsHelper.kt`
Utilise la libraire `android-smsmms` pour l'envoi réel:
```kotlin
// Envoi SMS
SmsHelper.sendSmsWithSystem(context, "+33123456789", "Message", null, callback)

// Envoi MMS avec image
SmsHelper.sendMmsWithStatus(context, "+33123456789", "Message", base64Image, "Sender", callback)

// MMS avec plusieurs pièces jointes
SmsHelper.sendMmsWithAttachments(context, "+33123456789", "Message", imagesList, "Sender", callback)

// Messages longs
SmsHelper.sendLongMessage(context, "+33123456789", "Long message...", null, callback)
```

#### `OvhSmsConfig.kt`
Configuration spécifique OVH:
```kotlin
// Créer une configuration SMS
val smsSettings = OvhSmsConfig.createSmsSettings()

// Créer une configuration MMS
val mmsSettings = OvhSmsConfig.createMmsSettings()

// Vérifier les limites
val canSendAsOne = OvhSmsConfig.willFitInSingleSms(message)
val partCount = OvhSmsConfig.calculateSmsPartCount(message)
```

#### `PhoneNumberValidator.kt`
Validation et formatage des numéros:
```kotlin
// Valider un numéro
if (PhoneNumberValidator.isValid(number)) { ... }

// Formater en international
val international = PhoneNumberValidator.normalize("+33123456789")  // +33123456789

// Obtenir le code pays
val code = PhoneNumberValidator.getCountryCode(number)
```

#### `SmsPermissionsManager.kt`
Gestion des permissions:
```kotlin
// Vérifier les permissions
if (SmsPermissionsManager.canSendSms(context)) { ... }
if (SmsPermissionsManager.canSendMms(context)) { ... }

// Obtenir les permissions manquantes
val missing = SmsPermissionsManager.getMissingPermissionsArray(context)
```

#### `MessageStatusManager.kt`
Suivi l'état des messages:
```kotlin
val statusManager = MessageStatusManager(context)
statusManager.registerStatusCallback("msg-123") { event ->
    when (event.status) {
        MessageStatus.SENT -> { ... }
        MessageStatus.DELIVERED -> { ... }
        MessageStatus.FAILED -> { ... }
    }
}
```

---

## 🔌 API REST Endpoints

### Authentification
Tous les endpoints requièrent un token Bearer:
```
Authorization: Bearer {TOKEN}
```

### POST `/api/send-message`
Envoie SMS ou MMS selon la présence d'une image.

**Request:**
```json
{
  "senderId": "MyApp",
  "destinataire": "+33123456789",
  "text": "Bonjour!",
  "base64Jpeg": null  // Optionnel: image base64
}
```

**Response (Succès):**
```json
{
  "success": true,
  "message": "SMS envoyé avec succès",
  "type": "SMS",
  "timestamp": 1704067200000
}
```

### POST `/api/send-sms`
Envoie **uniquement** un SMS (pas de MMS).

**Request:**
```json
{
  "senderId": "MyApp",
  "destinataire": "+33123456789",
  "text": "Bonjour!"
}
```

**Response:**
```json
{
  "success": true,
  "message": "SMS envoyé avec succès",
  "type": "SMS",
  "parts": 1,
  "characters": 9,
  "timestamp": 1704067200000
}
```

### POST `/api/send-mms`
Envoie **uniquement** un MMS avec image.

**Request:**
```json
{
  "senderId": "MyApp",
  "destinataire": "+33123456789",
  "text": "Regardez cette image!",
  "base64Jpeg": "iVBORw0KGgoAAAANSUhEUgAA..."
}
```

**Response:**
```json
{
  "success": true,
  "message": "MMS envoyé avec succès",
  "type": "MMS",
  "imageSize": "12345",
  "timestamp": 1704067200000
}
```

### POST `/api/logs`
Enregistre un log.

**Request:**
```json
{
  "message": "Log message here"
}
```

### GET `/api/health`
Vérifie l'état du serveur.

**Response:**
```json
{
  "success": true,
  "status": "online",
  "version": "1.0",
  "timestamp": 1704067200000
}
```

---

## 🚀 Utilisation Typique

### 1. Envoyer un SMS Simple

```kotlin
// Via la REST API
val json = JSONObject()
json.put("senderId", "MonApp")
json.put("destinataire", "+33612345678")
json.put("text", "Bonjour!")

val request = okhttp3.Request.Builder()
    .url("http://localhost:8080/api/send-sms")
    .addHeader("Authorization", "Bearer token123")
    .post(okhttp3.RequestBody.create(..., json.toString()))
    .build()
```

### 2. Envoyer un MMS avec Image

```kotlin
val imageBytes = File("/path/to/image.jpg").readBytes()
val base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT)

val json = JSONObject()
json.put("senderId", "MonApp")
json.put("destinataire", "+33612345678")
json.put("text", "Voici l'image")
json.put("base64Jpeg", base64Image)

val request = okhttp3.Request.Builder()
    .url("http://localhost:8080/api/send-mms")
    .addHeader("Authorization", "Bearer token123")
    .post(okhttp3.RequestBody.create(..., json.toString()))
    .build()
```

### 3. Vérifier l'État du Service

```kotlin
val request = okhttp3.Request.Builder()
    .url("http://localhost:8080/api/health")
    .addHeader("Authorization", "Bearer token123")
    .build()

val response = httpClient.newCall(request).execute()
val json = JSONObject(response.body?.string() ?: "{}")
Log.d("Health", "Status: ${json.getString("status")}")
```

---

## 📋 Permissions Requises

Le `AndroidManifest.xml` inclut:

```xml
<!-- SMS/MMS -->
<uses-permission android:name="android.permission.SEND_SMS"/>
<uses-permission android:name="android.permission.READ_SMS"/>
<uses-permission android:name="android.permission.WRITE_SMS"/>
<uses-permission android:name="android.permission.RECEIVE_SMS"/>
<uses-permission android:name="android.permission.RECEIVE_MMS"/>

<!-- Réseau -->
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE"/>

<!-- Téléphone -->
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>

<!-- Batterie -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"/>
```

**Note:** Sur Android 6.0+, les permissions requièrent une approbation à l'exécution.

---

## ⚙️ Configuration OVH

### Paramètres MMSC

```kotlin
object Mmsc {
    const val URL = "http://mms.ovh.net"
    const val PROXY = "192.168.1.1"  // À adapter selon l'APN
    const val PORT = 8080
}
```

### Limites de Taille

- **SMS**: 160 caractères (sans accents)
- **SMS Unicode**: 70 caractères (avec accents)
- **MMS**: 3 MB maximum

### Réseau

Les paramètres de timeout par défaut:
- Connexion: 15 secondes
- Lecture: 15 secondes
- Écriture: 15 secondes

---

## 🔒 Sécurité

### Token Authentication
- Token Bearer stocké via Jetpack Security Crypto
- Validation sur chaque requête API
- Rotation recommandée des tokens

### Validation
- Validation des numéros de téléphone (format international)
- Validation des tailles d'image MMS
- Validation du contenu JSON

### Permissions
- Vérification des permissions au runtime
- Gestion des refus de permissions
- Fallbacks gracieux

---

## 📊 Gestion des Erreurs

### Codes d'Erreur Courants

| Code | Signification |
|------|---|
| 400 | Paramètres invalides |
| 401 | Non autorisé (token manquant/invalide) |
| 404 | Endpoint non trouvé |
| 500 | Erreur serveur (échec d'envoi) |
| 503 | Service indisponible (SIM absente) |

### Exemples de Réponse d'Erreur

```json
{
  "success": false,
  "error": "Invalid phone number: abc123",
  "code": 400,
  "timestamp": 1704067200000
}
```

---

## 🧪 Tests

### Test Unitaire (SMS)

```kotlin
val message = SmsMessage(
    from = "TestApp",
    to = "+33123456789",
    message = "Test SMS"
)

val result = repository.sendSms(message)
assert(result is SendResult.Success)
```

### Test Unitaire (MMS)

```kotlin
val request = SendMessageRequest(
    senderId = "TestApp",
    destinataire = "+33123456789",
    text = "Test MMS",
    base64Jpeg = "iVBORw0KGgoAAAANSUhEUgAA..."
)

val result = repository.sendMms(request)
assert(result is SendResult.Success)
```

---

## 🔧 Dépannage

### Problème: SMS non envoyé
1. ✅ Vérifier que la SIM est détectée
2. ✅ Vérifier les permissions
3. ✅ Vérifier la connectivité réseau
4. ✅ Consulter les logs

### Problème: MMS non envoyé
1. ✅ Vérifier la configuration MMSC OVH
2. ✅ Vérifier la taille de l'image (< 3MB)
3. ✅ Vérifier la connectivité données
4. ✅ Vérifier le paramètre APN

### Logs Utiles
```
logcat | grep "SmsHelper"
logcat | grep "SmsRepositoryImpl"
logcat | grep "SmsRestServer"
```

---

## 📚 Références

- **Libraire android-smsmms**: https://github.com/klinker41/android-smsmms
- **Android SMS/MMS**: https://developer.android.com/guide/topics/providers/sms-mms-provider
- **Android Permissions**: https://developer.android.com/guide/topics/permissions

---

## 📝 Changelog

### Version 1.0
- ✅ Envoi SMS via android-smsmms
- ✅ Envoi MMS avec images
- ✅ Validation des numéros
- ✅ Configuration OVH MMSC
- ✅ API REST sécurisée
- ✅ Gestion des permissions
- ✅ Suivi d'état des messages
- ✅ Logs structurés

---

**Dernière mise à jour**: 3 avril 2026  
**Version**: 1.0  
**Auteur**: MS-OVH-SMS Team

