# 🧪 GUIDE DE TEST COMPLET - SMS/MMS Circuit

## Objectif

Vérifier le circuit complet SMS/MMS en local (sur le device Android) et à distance (via l'API REST).

---

## 📋 Prérequis

### Matériel/Logiciel
- [ ] Device Android ou émulateur (SDK 21+)
- [ ] SIM avec SMS activé (pour tests réels)
- [ ] Android Studio ou SDK Tools
- [ ] API en cours d'exécution (port 8080)
- [ ] Bearer Token valide

### Configuration
1. Remplacer `BEARER_TOKEN` dans les scripts/tests
2. Remplacer `TEST_PHONE` si besoin
3. Vérifier que le serveur REST écoute sur `localhost:8080`

---

## 🏃 Exécution Rapide

### Option 1: Exécuter tous les tests à la fois

```bash
# Depuis le répertoire du projet
cd D:\MISESERVICE\apps\MS-OVH-SMS

# Rendre le script exécutable
chmod +x test_circuit_complete.sh

# Exécuter tous les tests
./test_circuit_complete.sh
```

### Option 2: Tests gradle

```bash
# Tests unitaires locaux
./gradlew test

# Tests instrumentés (sur device/émulateur)
./gradlew connectedAndroidTest
```

---

## 🔍 Section 1: Tests Locaux (Device Android)

### Test 1.1: Vérifier les Permissions

**But**: Valider que toutes les permissions SMS/MMS sont accordées

```kotlin
import com.miseservice.smsovh.util.SmsPermissionsManager

// Sur le device
if (SmsPermissionsManager.canSendSms(context)) {
    Log.d("TEST", "✅ Permissions SMS OK")
} else {
    val missing = SmsPermissionsManager.getMissingPermissions(context)
    Log.e("TEST", "❌ Permissions manquantes: $missing")
}
```

**Vérification**:
```
✅ SEND_SMS
✅ READ_SMS  
✅ WRITE_SMS
✅ RECEIVE_SMS
✅ INTERNET
✅ ACCESS_NETWORK_STATE
```

---

### Test 1.2: Validation des Numéros

**But**: Tester la validation et formatage des numéros de téléphone

```kotlin
import com.miseservice.smsovh.util.PhoneNumberValidator

// Cas de test
val testCases = mapOf(
    "0612345678" to true,       // Format français ✅
    "+33612345678" to true,     // Format international ✅
    "abc123" to false,          // Invalide ❌
    "+331" to false             // Trop court ❌
)

for ((number, expected) in testCases) {
    val isValid = PhoneNumberValidator.isValid(number)
    assert(isValid == expected)
}
```

**Résultat attendu**:
```
Numéro: 0612345678 → Valide: true ✅
Numéro: +33612345678 → Valide: true ✅
Numéro: abc123 → Valide: false ✅
Numéro: +331 → Valide: false ✅
```

---

### Test 1.3: Formatage International

**But**: Vérifier que les numéros sont formatés en +33...

```kotlin
val formatted = PhoneNumberValidator.normalize("0612345678")
assert(formatted == "+33612345678")
```

**Résultat attendu**:
```
Input: 0612345678 → Formaté: +33612345678 ✅
```

---

### Test 1.4: Calcul Parties SMS

**But**: Valider les calculs de limite SMS

```kotlin
val testCases = mapOf(
    "A".repeat(160) to 1,       // 160 chars = 1 SMS
    "A".repeat(161) to 2,       // 161 chars = 2 SMS
    "A".repeat(320) to 2,       // 320 chars = 2 SMS
    "A".repeat(321) to 3        // 321 chars = 3 SMS
)

for ((message, expectedParts) in testCases) {
    val parts = OvhSmsConfig.calculateSmsPartCount(message)
    assert(parts == expectedParts)
}
```

**Résultat attendu**:
```
Message: 160 chars → 1 partie ✅
Message: 161 chars → 2 parties ✅
Message: 320 chars → 2 parties ✅
Message: 321 chars → 3 parties ✅
```

---

### Test 1.5: Validation Image MMS

**But**: Vérifier les limites de taille d'image

```kotlin
val testCases = mapOf(
    1024 * 1024 to true,            // 1 MB ✅
    3 * 1024 * 1024 to true,        // 3 MB (limite) ✅
    4 * 1024 * 1024 to false        // 4 MB ❌
)

for ((sizeBytes, expected) in testCases) {
    val isValid = OvhSmsConfig.isValidMmsImageSize(sizeBytes)
    assert(isValid == expected)
}
```

**Résultat attendu**:
```
Taille: 1MB → Valide: true ✅
Taille: 3MB → Valide: true ✅
Taille: 4MB → Valide: false ✅
```

---

### Test 1.6: Configuration OVH

**But**: Valider que la configuration MMSC OVH est correcte

```kotlin
OvhSmsConfig.logConfiguration()

// Résultat attendu dans les logs:
// MMSC URL: http://mms.ovh.net ✅
// MMSC Proxy: 192.168.1.1 ✅
// Port: 8080 ✅
// APN: ovh ✅
```

---

## 🌐 Section 2: Tests API REST Distante

### Test 2.1: Health Check (GET /api/health)

**But**: Vérifier que le serveur répond

```bash
curl -X GET http://localhost:8080/api/health \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Résultat attendu**:
```json
{
  "success": true,
  "status": "online",
  "version": "1.0",
  "timestamp": 1704067200000
}
```

✅ HTTP 200

---

### Test 2.2: Authentification - Pas de Token

**But**: Vérifier qu'on ne peut pas accéder sans token

```bash
curl -X GET http://localhost:8080/api/health
```

**Résultat attendu**:
```json
{
  "success": false,
  "error": "Unauthorized: missing or invalid token"
}
```

❌ HTTP 401

---

### Test 2.3: Authentification - Token Invalide

**But**: Vérifier qu'un mauvais token est rejeté

```bash
curl -X GET http://localhost:8080/api/health \
  -H "Authorization: Bearer INVALID_TOKEN"
```

**Résultat attendu**:
```json
{
  "success": false,
  "error": "Unauthorized: missing or invalid token"
}
```

❌ HTTP 401

---

### Test 2.4: Envoi SMS Simple (POST /api/send-sms)

**But**: Envoyer un SMS via l'API

```bash
curl -X POST http://localhost:8080/api/send-sms \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "TestApp",
    "destinataire": "+33612345678",
    "text": "Message de test"
  }'
```

**Résultat attendu**:
```json
{
  "success": true,
  "message": "SMS envoyé avec succès",
  "type": "SMS",
  "parts": 1,
  "characters": 16,
  "timestamp": 1704067200000
}
```

✅ HTTP 200

---

### Test 2.5: Validation - Numéro Invalide

**But**: Vérifier qu'un numéro invalide est rejeté

```bash
curl -X POST http://localhost:8080/api/send-sms \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "TestApp",
    "destinataire": "abc123",
    "text": "Message"
  }'
```

**Résultat attendu**:
```json
{
  "success": false,
  "error": "Invalid phone number: abc123",
  "code": 400
}
```

❌ HTTP 400

---

### Test 2.6: Envoi MMS (POST /api/send-mms)

**But**: Envoyer un MMS avec image

```bash
curl -X POST http://localhost:8080/api/send-mms \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "TestApp",
    "destinataire": "+33612345678",
    "text": "Regardez cette image",
    "base64Jpeg": "/9j/4AAQSkZJRgABAQEAYABgAAD..."
  }'
```

**Résultat attendu**:
```json
{
  "success": true,
  "message": "MMS envoyé avec succès",
  "type": "MMS",
  "imageSize": "12345",
  "timestamp": 1704067200000
}
```

✅ HTTP 200

---

### Test 2.7: Route Automatique (POST /api/send-message)

**But**: Tester le routage automatique SMS/MMS

**Sans image** → SMS:
```bash
curl -X POST http://localhost:8080/api/send-message \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "TestApp",
    "destinataire": "+33612345678",
    "text": "SMS"
  }'
```

**Résultat**: `type: "SMS"` ✅

**Avec image** → MMS:
```bash
curl -X POST http://localhost:8080/api/send-message \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "TestApp",
    "destinataire": "+33612345678",
    "text": "MMS",
    "base64Jpeg": "/9j/4AAQSkZJRgABAQEAYABgAAD..."
  }'
```

**Résultat**: `type: "MMS"` ✅

---

### Test 2.8: Enregistrement Log (POST /api/logs)

**But**: Enregistrer un log

```bash
curl -X POST http://localhost:8080/api/logs \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Test log depuis API"
  }'
```

**Résultat attendu**:
```json
{
  "success": true,
  "message": "Test log depuis API",
  "timestamp": 1704067200000
}
```

✅ HTTP 200

---

### Test 2.9: 404 Not Found

**But**: Vérifier les erreurs 404

```bash
curl -X GET http://localhost:8080/api/invalid-endpoint \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Résultat attendu**:
```json
{
  "success": false,
  "error": "Endpoint not found"
}
```

❌ HTTP 404

---

### Test 2.10: 400 Bad Request

**But**: Vérifier les erreurs 400

```bash
curl -X POST http://localhost:8080/api/send-sms \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ }'  # Champs manquants
```

**Résultat attendu**:
```json
{
  "success": false,
  "error": "Missing destinataire or text",
  "code": 400
}
```

❌ HTTP 400

---

## 📊 Résumé des Vérifications

| Test | Local | API | Statut |
|------|-------|-----|--------|
| Permissions | ✅ | N/A | ✅ |
| Validation Numéro | ✅ | ✅ | ✅ |
| Formatage +33 | ✅ | ✅ | ✅ |
| Calcul SMS Parts | ✅ | ✅ | ✅ |
| Limite MMS | ✅ | ✅ | ✅ |
| Config OVH | ✅ | ✅ | ✅ |
| Health Check | N/A | ✅ | ✅ |
| Auth - No Token | N/A | ✅ | ✅ |
| Auth - Invalid Token | N/A | ✅ | ✅ |
| Send SMS | ✅ | ✅ | ✅ |
| Send MMS | ✅ | ✅ | ✅ |
| Route Auto | N/A | ✅ | ✅ |
| Send Log | N/A | ✅ | ✅ |
| 404 Error | N/A | ✅ | ✅ |
| 400 Error | N/A | ✅ | ✅ |

---

## 🔧 Dépannage

### Problème: Permission SEND_SMS manquante

```
❌ Permission SEND_SMS manquante

Solution:
1. Vérifier AndroidManifest.xml contient <uses-permission>
2. Sur device: Paramètres → Apps → Permissions → SMS
3. Redémarrer l'app
```

### Problème: Serveur API non accessible

```
❌ Connexion refusée (localhost:8080)

Solution:
1. Vérifier le service REST est lancé
2. Vérifier le port 8080 n'est pas bloqué
3. Depuis émulateur: utiliser 10.0.2.2 au lieu de localhost
```

### Problème: Token invalide

```
❌ HTTP 401 Unauthorized

Solution:
1. Récupérer le token auprès du serveur
2. Remplacer YOUR_TOKEN dans les tests
3. Vérifier format: "Bearer <token>"
```

---

## 📈 Interprétation des Résultats

### 100% de réussite ✅
```
Circuit SMS/MMS complètement fonctionnel
- Local: ✅ SMS/MMS validé
- API: ✅ Tous endpoints répondent
- Statut: PRODUCTION-READY
```

### 80-99% de réussite ⚠️
```
Circuit partiellement fonctionnel
- Vérifier les tests échoués
- Consulter les logs détaillés
- Corriger les problèmes identifiés
```

### < 80% de réussite ❌
```
Circuit non opérationnel
- Vérifier la configuration OVH
- Vérifier les permissions
- Relancer les tests après correction
```

---

## 📝 Fichiers de Test

- **LocalSmsCircuitTest.kt** - Tests unitaires locaux (11 tests)
- **ApiCircuitTest.kt** - Tests API REST (10 tests)  
- **test_circuit_complete.sh** - Script bash complet (14 tests)

---

## 🎯 Checklist Final

- [ ] Tous les tests locaux passent
- [ ] Serveur API répond (200 OK)
- [ ] Authentification fonctionne
- [ ] SMS peut être envoyé
- [ ] MMS peut être envoyé
- [ ] Validation des numéros fonctionne
- [ ] Erreurs retournées correctement
- [ ] Logs enregistrés

---

**Date**: 3 avril 2026  
**Version**: 1.0  
**Prêt pour**: Tests complets et Production

