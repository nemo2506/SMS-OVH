# Release Notes v1.0

## OVH SMS Client - Version 1.0

### 🎉 Features

- **SMS Sending via REST API**: Send SMS messages through a local REST server
- **OVH API Integration**: Full support for OVH SMS API with authentication
- **Settings Persistence**: All settings are persisted locally using Room Database
- **Service Control**: Start/Stop REST server with service status monitoring
- **Token Management**: Secure API token generation and management
- **Phone Number Validation**: Smart phone number normalization with country prefix support
- **User Interface**: Clean Material 3 Compose-based UI with multiple configuration tabs

### 📦 Build Information

- **Application ID**: com.miseservice.smsovh
- **Version Code**: 1
- **Version Name**: 1.0
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

### 🔧 Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Clean Architecture
- **Dependency Injection**: Hilt
- **Database**: Room
- **HTTP Client**: OkHttp
- **REST Server**: NanoHttpd

### 📋 API Endpoints

#### SMS Sending
- `POST /api/sms/send`
- Body: `{ senderId, recipient, message }`
- Required Header: `X-API-Token`

#### OVH SMS Sending
- `POST /api/sms/send-ovh`
- Supports full OVH API configuration
- Required Header: `X-API-Token`

#### Logs
- `GET /api/logs`
- Returns server activity logs
- Required Header: `X-API-Token`

### 🚀 Installation

1. Download the APK file: `MS-SMS-OVH-v1.0.apk`
2. Enable installation from unknown sources on your Android device
3. Install the application
4. Configure your settings:
   - Set your sender ID
   - Configure recipient number
   - (Optional) Add OVH API credentials for OVH SMS support

### ⚙️ Configuration

#### For Standard SMS
- Sender ID: Your SMS sender identification
- Recipient: Phone number to send SMS to
- Message: SMS message content

#### For OVH API
- App Key: OVH application key
- App Secret: OVH application secret
- Consumer Key: OVH consumer key
- Service Name: OVH SMS service name
- Endpoint: OVH API endpoint (e.g., ovh-eu)
- Country Prefix: Default country code for phone numbers

#### Server Settings
- Host IP: Server IP address (auto-detected)
- REST Port: Port for REST server (default: 8080)
- Service Toggle: Enable/Disable REST server

### 🔒 Security

- Unique API tokens per installation
- Secure token storage using Android Keystore
- All API requests require valid token authentication

### 📝 Changelog

- Initial release v1.0
- OVH SMS API integration
- REST server with SMS endpoints
- Material 3 UI with Jetpack Compose
- Full settings persistence with Room
- Token management and security

### 🐛 Known Issues

None reported in this release.

### 📞 Support

For issues or feature requests, please visit: https://github.com/nemo2506/SMS-OVH

---
**Release Date**: 2026-04-04  
**Commit**: 3b060c5  
**Author**: MARC NAVARRO

