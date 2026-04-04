# Analyse du Dépôt android-smsmms

## Vue d'ensemble
**Dépôt**: https://github.com/klinker41/android-smsmms.git  
**Auteur**: Jacob Klinker  
**Description**: Une libraire Android complète et facile à utiliser pour envoyer des messages SMS et MMS sans avoir à manipuler directement le code source Android.

---

## Structure du Projet

### Répertoires Principaux
```
android-smsmms/
├── library/          # Libraire principale réutilisable
├── sample/           # Application d'exemple
├── gradle/           # Configuration Gradle
└── build.gradle      # Configuration de build racine
```

### Répertoires de la Libraire (`library/`)
```
library/
├── src/main/
│   ├── java/
│   │   ├── android/          # Classes utilitaires du framework Android
│   │   ├── com/android/      # Implémentations internes Android (MMS, réseau)
│   │   ├── com/klinker/      # **Classes principales de la libraire**
│   │   └── org/              # Classes de support
│   ├── res/                  # Ressources
│   └── AndroidManifest.xml   # Manifest de la libraire
├── build.gradle              # Configuration de compilation
└── gradle.properties          # Propriétés Gradle
```

---

## Classes Principales de la Libraire

La majeure partie du code utile se trouve dans `library/src/main/java/com/klinker/android/send_message/`

### 1. **Message.java** (574 lignes)
**Fonction**: Représente un message à envoyer (SMS ou MMS)

**Caractéristiques principales**:
- Stocke le texte du message
- Gère les images/pièces jointes (MMS)
- Gère les adresses destinataires
- Support des sujets pour MMS
- Support des délais d'envoi
- Classe interne `Part` pour gérer les médias

**Utilisation typique**:
```java
Message message = new Message("Hello World", "+33123456789");
message.setImage(bitmap);  // Pour MMS
```

### 2. **Transaction.java** (43514 lignes - fichier volumineux!)
**Fonction**: Gère l'envoi réel des messages

**Caractéristiques principales**:
- Coordonne l'envoi SMS et MMS
- Gère la connectivité réseau
- Gère les rapports de livraison
- Gère les APN settings
- Support du système d'envoi natif (Lollipop+)
- Gestion des erreurs et des tentatives

**Méthodes clés**:
- `sendNewMessage(message, threadId)` - Envoyer un nouveau message
- Gestion automatique du type (SMS vs MMS)

### 3. **Settings.java** (13198 lignes)
**Fonction**: Configuration pour l'envoi de messages

**Paramètres importants**:
- Paramètres MMSC (MMS Service Center)
- Proxy et port pour les connexions MMS
- APN (Access Point Name)
- Authentification réseau
- Options d'envoi système

**Utilisation typique**:
```java
Settings settings = new Settings();
// Configurer les paramètres MMS
settings.setMmsc("http://mms.operateur.fr");
settings.setMmsProxy("proxy.operateur.fr");
settings.setMmsPort(8080);
```

### 4. **Receivers (BroadcastReceivers)**

#### SentReceiver.java (5000 lignes)
- Gère les confirmations d'envoi SMS
- Met à jour le statut des messages dans la BD

#### DeliveredReceiver.java (5442 lignes)
- Gère les rapports de livraison
- Marque les messages comme lus

#### MmsReceivedReceiver.java (17057 lignes)
- Gère la réception des MMS
- Stocke les MMS reçus

#### MmsSentReceiver.java (2104 lignes)
- Gère les confirmations d'envoi MMS

### 5. **Utils et Helpers**

#### ApnUtils.java (15146 lignes)
- Gère les paramètres APN
- Récupère les settings d'accès réseau

#### Utils.java (18691 lignes)
- Fonctions utilitaires d'aide
- Conversion de formats
- Gestion de la connectivité

#### BroadcastUtils.java (1494 lignes)
- Utilitaires pour diffuser des broadcasts

#### StripAccents.java (2411 lignes)
- Nettoyage des caractères accentués

### 6. **Autres Classes Importantes**

- **MmsFileProvider.java** - Fournisseur de fichiers pour MMS
- **SmsManagerFactory.java** - Factory pour gérer différentes versions d'Android
- **MmsConfig.java** - Configuration MMS
- **MmsReceivedService.java** - Service pour gérer la réception MMS

---

## Classes de Support Android

### Package `android.net`
Contient les interfaces et classes pour gérer la connectivité réseau:
- `IConnectivityManager.java` - Gestion de la connectivité
- `INetworkPolicyManager.java` - Politiques réseau
- `NetworkPolicyManager.java` - Gestionnaire de politiques
- `LinkProperties.java`, `NetworkPolicy.java`, etc.

### Package `android.provider`
- Accès aux providers de contenu (SMS, MMS)

### Package `com.android.mms`
Implémentation interne des fonctionnalités MMS:
- Encodage/décodage MMS
- Gestion des layouts SMIL
- Parsing XML pour MMS

---

## Dépendances

### Dépendances Maven (depuis build.gradle)
```groovy
- com.klinkerapps:logger:1.0.3
- com.squareup.okhttp:okhttp:2.5.0
- com.squareup.okhttp:okhttp-urlconnection:2.5.0
```

### Versions Supportées
- **compileSdkVersion**: 25 (Android 7.1)
- **minSdkVersion**: 14 (Android 4.0)
- **targetSdkVersion**: 25

---

## Permissions Requises dans AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.SEND_SMS"/>
<uses-permission android:name="android.permission.READ_SMS"/>
<uses-permission android:name="android.permission.WRITE_SMS"/>
<uses-permission android:name="android.permission.RECEIVE_SMS"/>
<uses-permission android:name="android.permission.RECEIVE_MMS"/>
<uses-permission android:name="android.permission.VIBRATE"/>
<uses-permission android:name="android.provider.Telephony.SMS_RECEIVED"/>
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.WRITE_SETTINGS"/>
```

### BroadcastReceivers à Déclarer
```xml
<service android:name="com.android.mms.transaction.TransactionService"/>

<receiver
    android:name="com.klinker.android.send_message.SentReceiver"
    android:taskAffinity="[YOUR_PACKAGE_NAME].SMS_SENT"/>

<receiver
    android:name="com.klinker.android.send_message.DeliveredReceiver"
    android:taskAffinity="[YOUR_PACKAGE_NAME].SMS_DELIVERED"/>

<receiver
    android:name="[YOUR_CUSTOM_MMS_RECEIVER]"
    android:taskAffinity="com.klinker.android.messaging.MMS_SENT"/>
```

---

## Exemple d'Utilisation Simple

### Envoyer un SMS

```java
// 1. Créer les paramètres
Settings settings = new Settings();

// 2. Créer une transaction
Transaction transaction = new Transaction(context, settings);

// 3. Créer le message
Message message = new Message("Hello World", "+33123456789");

// 4. Envoyer
transaction.sendNewMessage(message, Transaction.NO_THREAD_ID);
```

### Envoyer un MMS

```java
// Configuration spéciale pour MMS
Settings settings = new Settings();
settings.setMmsc("http://mms.orange.fr");
settings.setMmsProxy("proxy.orange.fr");
settings.setMmsPort(8080);

Transaction transaction = new Transaction(context, settings);

Message message = new Message("Hello with image", "+33123456789");
message.setImage(bitmap);  // Ajouter l'image

transaction.sendNewMessage(message, threadId);
```

### Utiliser le système d'envoi natif (Android 5.0+)

```java
Settings settings = new Settings();
settings.setUseSystemSending(true);  // Utiliser l'API système

Transaction transaction = new Transaction(context, settings);
Message message = new Message("Hello", "+33123456789");
transaction.sendNewMessage(message, threadId);
```

---

## Points Clés pour Intégration

### 1. **Configuration APN Automatique**
La libraire peut récupérer automatiquement les paramètres APN depuis la SIM via `ApnUtils.java`

### 2. **Gestion de la Connectivité**
La libraire gère automatiquement:
- La vérification de la connectivité WiFi/données
- Les paramètres proxy
- Les tentatives de reconnexion

### 3. **Support Multi-Versions Android**
- Support des vieux appareils (API 14+)
- Optimisations pour versions modernes
- Utilisation des APIs système when available

### 4. **MMS Encoder/Decoder**
Code complèt pour encoder/décoder MMS avec:
- Support SMIL (Synchronized Multimedia Integration Language)
- Gestion des pièces jointes
- Compression d'images

---

## Avantages pour Votre Projet OVH SMS

1. **Libraire Mature et Éprouvée** - Utilisée dans plusieurs apps populaires (Sliding Messaging Pro, EvolveSMS, Pulse)
2. **Code Complet et Documenté** - Plus de 43 000 lignes bien structurées
3. **Support MMS** - Pas juste SMS, mais aussi MMS avec images
4. **Gestion des Rapports de Livraison** - Suivre l'état des messages
5. **Compatibilité Rétroactive** - Support de très vieilles versions Android
6. **Liberté d'Intégration** - Peut être utilisée comme librairie ou embarquée

---

## Recommandations

### Pour une Intégration OVH SMS:
1. **Étudier les classes principales**: Message, Transaction, Settings
2. **Adapter les APN Settings** pour OVH
3. **Implémenter les receivers** pour tracking
4. **Gérer les permissions** correctement
5. **Tester** avec des numéros OVH réels

### Points à Vérifier:
- [ ] Les paramètres MMSC/proxy OVH corrects
- [ ] La gestion des erreurs réseau
- [ ] Les permissions sur Android 6.0+
- [ ] Les restrictions d'arrière-plan Android 8+

---

## Fichiers Clés à Consulter

1. **README.md** - Guide complet d'utilisation
2. **library/src/main/java/com/klinker/android/send_message/Transaction.java** - Cœur du système
3. **library/src/main/java/com/klinker/android/send_message/Message.java** - Structure des messages
4. **library/src/main/java/com/klinker/android/send_message/Settings.java** - Configuration
5. **upgrading_for_marshmallow.md** - Guide pour Android 6.0+
6. **upgrading_for_o.md** - Guide pour Android 8.0+

---

**Date d'Analyse**: 3 Avril 2026  
**État du Dépôt**: Cloné localement à `D:\MISESERVICE\apps\android-smsmms`

