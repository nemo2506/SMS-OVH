# OVH SMS — Sender ID Alphanumérique

App Android pour envoyer des SMS avec un **Sender ID alphanumérique** (ex: `MISESERVICE`)
via l'API HTTP OVH SMS.

---

## 📋 Prérequis OVH

1. Avoir un compte OVH avec un **service SMS actif**
   → https://www.ovhtelecloud.com/fr/sms/

2. Récupérer vos identifiants depuis l'espace client OVH :
   - **Nom du service** : ex `sms-ab12345-1`
   - **Login** : ex `sms-ab12345-1`
   - **Mot de passe** : défini dans le manager OVH

3. Valider votre **Sender ID** dans le manager OVH :
   → Espace client > SMS > Expéditeurs > Ajouter un expéditeur alphanumérique

---

## 🚀 Installation Android Studio

```bash
# Cloner / copier le projet
# Ouvrir dans Android Studio

# File > Open > sélectionner le dossier ovh-sms-app
# Sync Gradle puis Run sur émulateur ou appareil réel
```

---

## 📡 API OVH utilisée

L'app utilise l'**API HTTP simple OVH** :

```
GET https://www.ovh.com/cgi-bin/sms/http2sms.cgi
  ?account=sms-ab12345-1
  &login=sms-ab12345-1
  &password=VOTRE_MDP
  &from=MISESERVICE        ← Sender ID alphanumérique
  &to=+33612345678
  &msg=Votre+message
  &contentType=text/json
```

### Codes retour OVH

| Status | Signification          |
|--------|------------------------|
| 100    | SMS envoyé             |
| 101    | SMS en file d'attente  |
| 201    | Login invalide         |
| 202    | Mot de passe invalide  |
| 301    | Crédit insuffisant     |
| 302    | Numéro invalide        |

---

## ⚠️ Limitations du Sender ID alphanumérique

- **Max 11 caractères** (lettres + chiffres uniquement)
- **Pas de réponse possible** du destinataire
- Certains opérateurs / pays peuvent bloquer l'affichage
- Le Sender ID doit être validé au préalable chez OVH

---

## 🔐 Sécurité en production

Pour une app en production, **ne jamais stocker les credentials en dur**.
Utiliser :
- Android `EncryptedSharedPreferences`
- Un backend intermédiaire (proxy API)
- Les Android Keystore System

---

## 📁 Structure du projet

```
app/src/main/
├── java/com/ovhsms/app/
│   └── MainActivity.kt       ← Logique principale + appel API OVH
├── res/
│   ├── layout/
│   │   └── activity_main.xml ← Interface utilisateur
│   ├── values/
│   │   ├── colors.xml        ← Palette sombre
│   │   ├── strings.xml
│   │   └── themes.xml        ← Thème Material
│   └── drawable/
│       ├── bg_status_success.xml
│       ├── bg_status_error.xml
│       └── bg_info.xml
└── AndroidManifest.xml       ← Permission INTERNET
```
