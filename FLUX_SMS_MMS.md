# 🔄 Flux d'Envoi SMS/MMS - Architecture

## 1️⃣ Flux Complet d'Envoi SMS

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT (APP/API)                         │
│  Demande d'envoi: POST /api/send-sms ou /api/send-message  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              SmsRestServer.kt (API REST)                    │
│  ✅ Validation: Bearer Token                               │
│  ✅ Parse JSON: senderId, destinataire, text               │
│  ✅ Validation: Numéro de téléphone                        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│         PhoneNumberValidator.normalize()                    │
│  "0612345678" ──→ "+33612345678"                           │
│  Validation regex + formatage international                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│         SendRestMessageUseCase(request)                     │
│  Délègue au UseCase unifié                                  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│         SendMessageUseCase(request)                         │
│  ❓ Contient une image base64?                              │
│    - NON → Appel sendSms()                                  │
│    - OUI → Appel sendMms()                                  │
└────────────────────┬────────────────────────────────────────┘
                     │
       ┌─────────────┴─────────────┐
       │                           │
       ▼ (Pas d'image)            ▼ (Avec image)
┌─────────────────┐        ┌──────────────────┐
│  SendSmsUseCase │        │ SmsRepository    │
│                 │        │  .sendMms()      │
└────────┬────────┘        └────────┬─────────┘
         │                          │
         ▼                          ▼
┌─────────────────────────────────────────────────────────────┐
│         SmsRepository.sendSms(SmsMessage)                   │
│  Implémentation dans: SmsRepositoryImpl.kt                   │
└────────────────────┬────────────────────────────────────────┘
                     │
         ┌───────────┴───────────┐
         │                       │
         ▼ Tentative 1          ▼ Si erreur
┌──────────────────────────┐   ┌──────────────────────────┐
│ Libraire android-smsmms  │   │ Fallback: SmsManager     │
│  (RECOMMANDÉ)            │   │  (Natif Android)         │
│                          │   │                          │
│ ✅ Transaction.java     │   │ ✅ SmsManager.sendText() │
│ ✅ Settings OVH         │   │ ✅ Broadcast Receivers   │
│ ✅ Message.java         │   │ ✅ Gestion d'erreurs    │
└──────────────┬───────────┘   └────────────┬─────────────┘
               │                             │
               └─────────────┬───────────────┘
                             │
                             ▼
                    ┌────────────────────┐
                    │ BroadcastReceiver  │
                    │   SentReceiver     │
                    │  onReceive()       │
                    │                    │
                    │ ✅ RESULT_OK       │
                    │ ❌ ERROR_*         │
                    └────────┬───────────┘
                             │
                             ▼
                    ┌────────────────────────────┐
                    │ SendResult.Success/Error   │
                    │ resume(continu)            │
                    └────────┬───────────────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │ SmsRestServer (Response)     │
              │                              │
              │ {"success": true,            │
              │  "message": "SMS envoyé",    │
              │  "type": "SMS",              │
              │  "parts": 1,                 │
              │  "timestamp": ...}           │
              └──────────┬───────────────────┘
                         │
                         ▼
              ┌──────────────────────────────┐
              │ RestServerEventManager.emit()│
              │                              │
              │ Émet événement:              │
              │ SMS_SENT_SUCCESS ou          │
              │ SMS_SENT_ERROR               │
              └──────────────────────────────┘
```

---

## 2️⃣ Flux Complet d'Envoi MMS

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT (APP/API)                         │
│  POST /api/send-mms ou /api/send-message (avec image)      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              SmsRestServer.kt (API REST)                    │
│  ✅ Validation: Bearer Token                               │
│  ✅ Parse JSON: senderId, destinataire, text, base64Jpeg  │
│  ✅ Validation: Numéro + Image base64                      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│         PhoneNumberValidator.normalize()                    │
│  Format international: +33...                              │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│         SendRestMessageUseCase(request)                     │
│  Détecte: request.base64Jpeg ≠ null                        │
│  Appel: repository.sendMms()                                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│    SmsRepository.sendMms(SendMessageRequest)               │
│    Implémentation: SmsRepositoryImpl.kt                      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│          SmsHelper.sendMmsWithStatus()                      │
│                                                              │
│  1. Base64 Decode:                                          │
│     imageBytes = Base64.decode(base64Jpeg)                 │
│                                                              │
│  2. Validation JPEG:                                        │
│     bitmap = BitmapFactory.decodeByteArray()               │
│                                                              │
│  3. Configuration OVH MMSC:                                 │
│     settings = getOvhMmsSettings()                          │
│     - MMSC: http://mms.ovh.net                             │
│     - Proxy: 192.168.1.1:8080                              │
│     - APN: ovh                                              │
│                                                              │
│  4. Création Message MMS:                                   │
│     Message(text, phoneNumber).apply {                      │
│       images = arrayOf(imageBytes)                          │
│       subject = "Sender: $senderId"                         │
│     }                                                       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│          Libraire android-smsmms                            │
│         Transaction.sendNewMessage()                        │
│                                                              │
│  ✅ Détecte: MMS (contient images)                          │
│  ✅ Configure: APN + MMSC OVH                              │
│  ✅ Encode: Message MMS avec images                         │
│  ✅ Envoie: Via le serveur MMSC OVH                        │
│  ✅ Gère: Rapports de livraison                            │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│          Réseau Android                                     │
│                                                              │
│  ✅ Vérification SIM                                        │
│  ✅ Activation données (si nécessaire)                      │
│  ✅ Connexion MMSC via proxy                                │
│  ✅ Envoi du message multimédia                             │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│          Serveur MMSC OVH                                   │
│                                                              │
│  http://mms.ovh.net (port 8080)                            │
│  ✅ Réception du MMS                                        │
│  ✅ Traitement                                              │
│  ✅ Stockage                                                │
│  ✅ Remise au destinataire                                  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│          Callback MmsStatus                                 │
│          (success: Boolean, json: JSONObject)              │
│                                                              │
│  ✅ success = true                                          │
│  ✅ json.put("success", true)                              │
│  ✅ json.put("message", "MMS envoyé")                      │
│  ✅ json.put("type", "MMS")                                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  SmsRepositoryImpl.sendMms() -> SendResult.Success          │
│  Resume coroutine                                           │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              SmsRestServer (Response)                       │
│                                                              │
│  {"success": true,                                          │
│   "message": "MMS envoyé avec succès",                      │
│   "type": "MMS",                                            │
│   "imageSize": "12345",                                     │
│   "timestamp": 1704067200000}                               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  RestServerEventManager.emit()                              │
│  SMS_SENT_SUCCESS event                                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 3️⃣ Validation & Sécurité

```
┌────────────────────────────────────────────────┐
│         REQUEST VALIDATION FLOW                │
└────────────────────────────────────────────────┘

   INPUT
     │
     ▼
┌─────────────────────────────────────────┐
│ 1. Token Authentication                 │
│    Header: "Authorization: Bearer ..."  │
│    ✅ Token Valide?                    │
│    ❌ Retour 401 UNAUTHORIZED           │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 2. JSON Parse                           │
│    Parse JSON body                      │
│    ✅ JSON valide?                     │
│    ❌ Retour 400 BAD_REQUEST            │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 3. Champs Obligatoires                  │
│    Vérifier: destinataire, text         │
│    ✅ Tous présents?                    │
│    ❌ Retour 400 BAD_REQUEST            │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 4. Validation Numéro                    │
│    PhoneNumberValidator.normalize()     │
│    ✅ Format valide?                    │
│    ❌ Retour 400 BAD_REQUEST            │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 5. Validation Image (si MMS)            │
│    ✅ Base64 décodable?                │
│    ✅ JPEG valide?                     │
│    ✅ Taille < 3MB?                    │
│    ❌ Retour 400 BAD_REQUEST            │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 6. Vérification Permissions             │
│    SmsPermissionsManager.canSend*()     │
│    ✅ Permissions accordées?           │
│    ⚠️  Warn si manquantes               │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 7. Vérification SIM                     │
│    telephonyManager.simState            │
│    ✅ SIM prête?                        │
│    ❌ Retour 503 SERVICE_UNAVAILABLE    │
└────────────┬────────────────────────────┘
             │
             ▼
        ✅ ENVOI OK
```

---

## 4️⃣ Structure de Données

```
REQUEST JSON
├─ senderId: "MyApp"              [Optionnel]
├─ destinataire: "+33612345678"  [Requis]
├─ text: "Bonjour!"              [Requis]
└─ base64Jpeg: "iVBORw0KGg..."  [Optionnel, SMS si absent]

RESPONSE JSON
├─ success: true/false
├─ message: "SMS envoyé..."      [Si succès]
├─ error: "Error message"         [Si erreur]
├─ code: 400/401/500/503         [Code HTTP]
├─ type: "SMS" / "MMS"           [Type de message]
├─ parts: 1                       [Nombre de parties SMS]
├─ characters: 9                  [Nombre de caractères]
├─ imageSize: "12345"             [Taille image MMS]
└─ timestamp: 1704067200000      [Timestamp de la réponse]

MESSAGE ENTITY
├─ from: "Sender"
├─ to: "+33612345678"
├─ message: "Contenu"
└─ images: [ByteArray]            [Pour MMS]

MMS MESSAGE ENTITY
├─ from: "Sender"
├─ to: "+33612345678"
├─ message: "Contenu"
├─ imageBytes: ByteArray          [Image JPEG]
└─ attachments: List<ByteArray>   [Pièces jointes]

CONFIG OVH
├─ MMSC
│  ├─ URL: "http://mms.ovh.net"
│  ├─ Proxy: "192.168.1.1"
│  └─ Port: 8080
├─ APN
│  ├─ Name: "ovh"
│  └─ MmsProxy: "192.168.1.1"
└─ Limits
   ├─ SMS: 160 chars
   ├─ SMS Unicode: 70 chars
   └─ MMS: 3 MB
```

---

## 5️⃣ Décision SMS vs MMS

```
┌──────────────────────────────────┐
│   SendMessageUseCase Check       │
└──────────────────────────────────┘
            │
            ▼
┌──────────────────────────────────┐
│  request.base64Jpeg.isNullOrBlank │
└────────┬──────────────────────────┘
         │
    ┌────┴────┐
    │          │
   YES        NO
    │          │
    ▼          ▼
 [SMS]       [MMS]
    │          │
    ▼          ▼
repository   repository
.sendSms()   .sendMms()
    │          │
    ▼          ▼
SmsMessage   SendMessageRequest
    │          │
    ▼          ▼
 SmsHelper    SmsHelper
sendSmsWithSystem sendMmsWithStatus
```

---

## 6️⃣ Gestion des Erreurs

```
┌──────────────────────────┐
│    ERROR HANDLING        │
└──────────────────────────┘

HTTP STATUS CODES:
├─ 200 OK ✅
│  └─ Message envoyé avec succès
├─ 400 BAD_REQUEST ❌
│  ├─ Missing destinataire/text
│  ├─ Invalid phone number
│  └─ Invalid image format
├─ 401 UNAUTHORIZED ❌
│  └─ Missing/invalid token
├─ 404 NOT_FOUND ❌
│  └─ Endpoint doesn't exist
├─ 500 INTERNAL_ERROR ❌
│  ├─ SMS send failed
│  ├─ MMS send failed
│  └─ Database error
└─ 503 SERVICE_UNAVAILABLE ❌
   ├─ SIM absent
   ├─ No network
   └─ Radio off

RESULT CODES:
├─ Activity.RESULT_OK
│  └─ ✅ Message sent
├─ RESULT_ERROR_GENERIC_FAILURE
│  └─ ❌ Generic error
├─ RESULT_ERROR_NO_SERVICE
│  └─ ❌ No network available
├─ RESULT_ERROR_NULL_PDU
│  └─ ❌ Invalid PDU
└─ RESULT_ERROR_RADIO_OFF
   └─ ❌ Radio disabled
```

---

**Version**: 1.0  
**Dernière mise à jour**: 3 avril 2026

