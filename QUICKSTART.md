# ⚡ QUICKSTART - MS-OVH-SMS v1.0

> Démarrage rapide en 5 minutes ⏱️

---

## 📱 Envoyer un SMS (30 secondes)

```kotlin
import com.miseservice.smsovh.util.SmsHelper

// Dans votre activité
SmsHelper.sendSmsWithSystem(
    context = this,
    phoneNumber = "+33612345678",
    message = "Bonjour!",
    senderId = "MyApp",
    callback = { success, json ->
        if (success) {
            Toast.makeText(this, "✅ SMS envoyé!", Toast.LENGTH_SHORT).show()
        } else {
            val error = json.optString("error", "Erreur inconnue")
            Toast.makeText(this, "❌ $error", Toast.LENGTH_SHORT).show()
        }
    }
)
```

---

## 🖼️ Envoyer un MMS (1 minute)

```kotlin
import com.miseservice.smsovh.util.SmsHelper
import android.util.Base64
import java.io.File

// Lire l'image
val imageFile = File("/path/to/image.jpg")
val imageBytes = imageFile.readBytes()
val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)

// Envoyer
SmsHelper.sendMmsWithStatus(
    context = this,
    phoneNumber = "+33612345678",
    message = "Regardez cette image!",
    base64Jpeg = base64Image,
    senderId = "MyApp",
    callback = { success, json ->
        if (success) {
            Toast.makeText(this, "✅ MMS envoyé!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "❌ Erreur MMS", Toast.LENGTH_SHORT).show()
        }
    }
)
```

---

## ✅ Vérifier les Permissions

```kotlin
import com.miseservice.smsovh.util.SmsPermissionsManager

// Vérifier avant d'envoyer
if (!SmsPermissionsManager.canSendSms(this)) {
    val missing = SmsPermissionsManager.getMissingPermissionsArray(this)
    ActivityCompat.requestPermissions(this, missing, REQUEST_CODE)
    return
}

// Puis envoyer...
SmsHelper.sendSmsWithSystem(...)
```

---

## 🔢 Valider un Numéro

```kotlin
import com.miseservice.smsovh.util.PhoneNumberValidator

val userInput = "0612345678"

// Valider et formater
val normalized = PhoneNumberValidator.normalize(userInput)
if (normalized != null) {
    println("✅ Numéro valide: $normalized")
    // Envoyer avec le numéro formaté
    SmsHelper.sendSmsWithSystem(context, normalized, message, null, callback)
} else {
    println("❌ Numéro invalide: $userInput")
}
```

---

## 📡 Utiliser l'API REST

### Envoyer un SMS via API

```bash
curl -X POST http://localhost:8080/api/send-sms \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "MyApp",
    "destinataire": "+33612345678",
    "text": "Bonjour depuis l'API!"
  }'
```

### Réponse

```json
{
  "success": true,
  "message": "SMS envoyé avec succès",
  "type": "SMS",
  "parts": 1,
  "characters": 26,
  "timestamp": 1704067200000
}
```

### Envoyer un MMS via API

```bash
curl -X POST http://localhost:8080/api/send-mms \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "MyApp",
    "destinataire": "+33612345678",
    "text": "MMS depuis l'API!",
    "base64Jpeg": "iVBORw0KGgoAAAANSUhEUgAAAAUA..."
  }'
```

---

## 📊 Calculer les Limites SMS

```kotlin
import com.miseservice.smsovh.util.OvhSmsConfig

val message = "Bonjour, ceci est un message de test avec accents: éàü"

// Vérifier si tient en 1 SMS
if (OvhSmsConfig.willFitInSingleSms(message)) {
    println("✅ Tient en 1 SMS")
} else {
    val parts = OvhSmsConfig.calculateSmsPartCount(message)
    println("⚠️ Nécessite $parts SMS")
}
```

---

## 🔄 Suivre l'État d'un Message

```kotlin
import com.miseservice.smsovh.util.MessageStatusManager
import com.miseservice.smsovh.util.MessageStatus

val statusManager = MessageStatusManager(this)

// S'enregistrer pour les mises à jour
statusManager.registerStatusCallback("msg-123") { event ->
    when (event.status) {
        MessageStatus.SENT -> Log.d("SMS", "✅ Envoyé")
        MessageStatus.DELIVERED -> Log.d("SMS", "✅ Livré")
        MessageStatus.FAILED -> Log.d("SMS", "❌ Échoué: ${event.details}")
        else -> Log.d("SMS", "❓ Status: ${event.status}")
    }
}

// ... Envoyer le message ...

// Nettoyer à la fin
statusManager.cleanup()
```

---

## 🏥 Vérifier la Santé du Service

```bash
curl -X GET http://localhost:8080/api/health \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Réponse OK:**
```json
{
  "success": true,
  "status": "online",
  "version": "1.0",
  "timestamp": 1704067200000
}
```

---

## ⚠️ Gestion des Erreurs

### Erreur: "Numéro invalide"
```kotlin
// ❌ Invalide
PhoneNumberValidator.normalize("abc123")  // null

// ✅ Valide
PhoneNumberValidator.normalize("+33612345678")  // "+33612345678"
PhoneNumberValidator.normalize("0612345678")    // "+33612345678"
```

### Erreur: "Permissions insuffisantes"
```kotlin
// Demander les permissions
if (!SmsPermissionsManager.canSendSms(context)) {
    val missing = SmsPermissionsManager.getMissingPermissionsArray(context)
    ActivityCompat.requestPermissions(this, missing, REQUEST_SMS)
}
```

### Erreur: "MMS trop gros"
```kotlin
// Vérifier la taille
val sizeBytes = imageBytes.size
if (!OvhSmsConfig.isValidMmsImageSize(sizeBytes)) {
    val error = OvhSmsConfig.getImageSizeErrorMessage(sizeBytes)
    Log.e("MMS", error)  // "Taille d'image invalide: 5MB (limite: 3MB)"
}
```

---

## 🎯 Cas Courants

### 1️⃣ Envoyer SMS simple
```kotlin
SmsHelper.sendSmsWithSystem(ctx, "+33612345678", "Coucou!", null, { s, j ->
    if (s) Toast.makeText(ctx, "Envoyé!", Toast.LENGTH_SHORT).show()
})
```

### 2️⃣ Envoyer MMS avec image
```kotlin
val base64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT)
SmsHelper.sendMmsWithStatus(ctx, "+33612345678", "Image!", base64, null, { s, j ->
    if (s) Toast.makeText(ctx, "MMS envoyé!", Toast.LENGTH_SHORT).show()
})
```

### 3️⃣ Batch d'envoi
```kotlin
val numbers = listOf("0612345678", "0723456789", "0834567890")
val (valid, invalid) = PhoneNumberValidator.validateBatch(numbers)
for (number in valid) {
    SmsHelper.sendSmsWithSystem(ctx, number, "Message", null, { s, _ ->
        Log.d("SMS", if (s) "Envoyé à $number" else "Erreur $number")
    })
}
```

### 4️⃣ SMS long → MMS
```kotlin
val longMessage = "A".repeat(200)  // 200 caractères
if (!OvhSmsConfig.willFitInSingleSms(longMessage)) {
    // Devient MMS automatiquement
    SmsHelper.sendLongMessage(ctx, number, longMessage, null, callback)
}
```

### 5️⃣ Intégration API REST
```bash
# Endpoint disponible
POST /api/send-message    # Route automatique SMS/MMS
POST /api/send-sms        # SMS seulement
POST /api/send-mms        # MMS seulement
GET  /api/health          # Vérifier statut
POST /api/logs            # Enregistrer log
```

---

## 📚 Aller Plus Loin

| Sujet | Fichier | Temps |
|-------|---------|-------|
| **Vue d'ensemble** | README_MODIFICATIONS.md | 5 min |
| **Exemples** | SmsUsageExamples.kt | 10 min |
| **Guide complet** | GUIDE_SMS_MMS.md | 45 min |
| **Détail technique** | MODIFICATIONS_RESUME.md | 40 min |
| **Flux** | FLUX_SMS_MMS.md | 35 min |
| **Navigation** | INDEX.md | 5 min |

---

## 🚨 Troubleshooting Rapide

| Problème | Solution |
|----------|----------|
| SMS non envoyé | ✅ Vérifier SIM, permissions, réseau |
| MMS non envoyé | ✅ Vérifier taille image, config MMSC, données activées |
| Erreur 401 API | ✅ Vérifier token Authorization |
| Erreur 400 API | ✅ Vérifier JSON valide, champs obligatoires |

---

## 💡 Tips & Tricks

### 1. Pré-formater les numéros
```kotlin
val numbers = listOf("0612345678", "+33712345678", "001234567890")
val formatted = PhoneNumberValidator.formatBatch(numbers)
// Tous en format international
```

### 2. Calculer coût SMS
```kotlin
val parts = OvhSmsConfig.calculateSmsPartCount(message)
val cost = parts * COST_PER_SMS
println("Coût total: €$cost")
```

### 3. Vérifier avant API
```kotlin
if (!SmsPermissionsManager.canSendMms(context)) {
    // Retourner erreur 503 Forbidden
    return Response.PERMISSION_DENIED
}
```

### 4. Logs production
```kotlin
OvhSmsConfig.logConfiguration()  // Affiche config en logs
```

---

## 🔒 Sécurité Rapide

```kotlin
// ✅ Toujours valider
PhoneNumberValidator.isValid(number)

// ✅ Toujours vérifier permissions
SmsPermissionsManager.canSendSms(context)

// ✅ Toujours avoir Bearer Token
"Authorization: Bearer $token"

// ✅ Toujours vérifier image taille
OvhSmsConfig.isValidMmsImageSize(size)
```

---

## 📞 Ressources

- **Documentation complet**: GUIDE_SMS_MMS.md
- **Libraire**: https://github.com/klinker41/android-smsmms
- **Android Docs**: https://developer.android.com/guide/topics/providers/sms-mms-provider

---

## 🎯 Prochain? Lire INDEX.md

[Aller à INDEX.md →](./INDEX.md)

---

**⏱️ Temps de setup**: 5 minutes  
**💻 Code prêt**: Copy-paste ready  
**✅ Statut**: Production-ready

Bon coding! 🚀

