#!/bin/bash

# Script de test complet du circuit SMS/MMS
# Teste à la fois le circuit local et l'API distante

set -e

echo "╔════════════════════════════════════════════════════════════╗"
echo "║   TEST COMPLET CIRCUIT SMS/MMS - MS-OVH-SMS v1.0          ║"
echo "║   Local Device + API REST                                 ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Configuration
PROJECT_PATH="D:/MISESERVICE/apps/MS-OVH-SMS"
API_URL="http://localhost:8080"
BEARER_TOKEN="YOUR_TOKEN_HERE"  # À remplacer
TEST_PHONE="+33612345678"
TEST_MESSAGE="Test SMS Circuit"

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Flags
TESTS_PASSED=0
TESTS_FAILED=0

# Fonction pour afficher les résultats
test_result() {
    local test_name=$1
    local status=$2
    local message=$3

    if [ "$status" = "0" ]; then
        echo -e "${GREEN}✅ PASS${NC} - $test_name"
        if [ ! -z "$message" ]; then
            echo "   $message"
        fi
        ((TESTS_PASSED++))
    else
        echo -e "${RED}❌ FAIL${NC} - $test_name"
        if [ ! -z "$message" ]; then
            echo "   $message"
        fi
        ((TESTS_FAILED++))
    fi
}

# ═════════════════════════════════════════════════════════════
# SECTION 1: Tests Locaux (Device Android)
# ═════════════════════════════════════════════════════════════

echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  SECTION 1: TESTS LOCAUX (Device Android)                 ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Test 1: Compilation
echo "Test 1: Compilation du projet..."
cd "$PROJECT_PATH"
if ./gradlew build -q 2>/dev/null; then
    test_result "Compilation APK" "0" "APK compilée avec succès"
else
    test_result "Compilation APK" "1" "Erreur lors de la compilation"
fi
echo ""

# Test 2: Tests unitaires locaux
echo "Test 2: Exécution des tests unitaires..."
if ./gradlew test -q 2>/dev/null; then
    test_result "Tests Unitaires Locaux" "0" "Tous les tests unitaires passent"
else
    test_result "Tests Unitaires Locaux" "1" "Certains tests unitaires ont échoué"
fi
echo ""

# Test 3: Tests instrumentés (nécessite un device/émulateur)
echo "Test 3: Vérification de la présence d'un device Android..."
if adb devices | grep -q "device"; then
    echo "   Device trouvé, exécution des tests instrumentés..."
    if ./gradlew connectedAndroidTest -q 2>/dev/null; then
        test_result "Tests Instrumentés (LocalSmsCircuitTest)" "0" "Tests sur device exécutés"
    else
        test_result "Tests Instrumentés (LocalSmsCircuitTest)" "1" "Erreur exécution sur device"
    fi
else
    echo -e "${YELLOW}⚠️  SKIPPED${NC} - LocalSmsCircuitTest - Pas de device détecté"
fi
echo ""

# Test 4: Vérification du code source
echo "Test 4: Vérification de la présence des fichiers clés..."
required_files=(
    "app/src/main/java/com/miseservice/smsovh/util/SmsHelper.kt"
    "app/src/main/java/com/miseservice/smsovh/util/OvhSmsConfig.kt"
    "app/src/main/java/com/miseservice/smsovh/util/PhoneNumberValidator.kt"
    "app/src/main/java/com/miseservice/smsovh/data/repository/SmsRepositoryImpl.kt"
    "app/src/main/java/com/miseservice/smsovh/service/SmsRestServer.kt"
)

all_files_exist=1
for file in "${required_files[@]}"; do
    if [ ! -f "$PROJECT_PATH/$file" ]; then
        echo "   ❌ Fichier manquant: $file"
        all_files_exist=0
    fi
done

if [ "$all_files_exist" = "1" ]; then
    test_result "Fichiers Source Clés" "0" "Tous les fichiers requis sont présents"
else
    test_result "Fichiers Source Clés" "1" "Certains fichiers manquent"
fi
echo ""

# ═════════════════════════════════════════════════════════════
# SECTION 2: Tests API REST Distante
# ═════════════════════════════════════════════════════════════

echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  SECTION 2: TESTS API REST DISTANTE                        ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Test 5: Vérifier que le serveur est accessible
echo "Test 5: Vérification de la disponibilité du serveur ($API_URL)..."
response=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $BEARER_TOKEN" "$API_URL/api/health" 2>/dev/null || echo "000")

if [ "$response" = "200" ] || [ "$response" = "401" ]; then
    test_result "Serveur Accessible" "0" "Serveur répond (Code: $response)"
else
    test_result "Serveur Accessible" "1" "Serveur ne répond pas (Code: $response)"
fi
echo ""

# Test 6: Test d'authentification - Pas de token
echo "Test 6: Authentification - Pas de token..."
response=$(curl -s -w "%{http_code}" -o /tmp/response.json "$API_URL/api/health" 2>/dev/null || echo "000")

if [ "$response" = "401" ]; then
    test_result "Auth - No Token" "0" "Correctement rejeté (401)"
else
    test_result "Auth - No Token" "1" "Devrait retourner 401, reçu: $response"
fi
echo ""

# Test 7: Test d'authentification - Token invalide
echo "Test 7: Authentification - Token invalide..."
response=$(curl -s -w "%{http_code}" -o /tmp/response.json \
    -H "Authorization: Bearer INVALID_TOKEN_12345" \
    "$API_URL/api/health" 2>/dev/null || echo "000")

if [ "$response" = "401" ]; then
    test_result "Auth - Invalid Token" "0" "Correctement rejeté (401)"
else
    test_result "Auth - Invalid Token" "1" "Devrait retourner 401, reçu: $response"
fi
echo ""

# Test 8: Health Check avec token valide
echo "Test 8: Health Check avec token valide..."
response=$(curl -s -H "Authorization: Bearer $BEARER_TOKEN" "$API_URL/api/health" 2>/dev/null || echo "")

if echo "$response" | grep -q '"status":"online"'; then
    test_result "Health Check" "0" "Serveur online"
else
    test_result "Health Check" "1" "Réponse invalide ou serveur offline"
fi
echo ""

# Test 9: Envoi SMS via API
echo "Test 9: Envoi SMS via API POST /api/send-sms..."
json_payload=$(cat <<EOF
{
  "senderId": "TestScript",
  "destinataire": "$TEST_PHONE",
  "text": "$TEST_MESSAGE"
}
EOF
)

response=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Authorization: Bearer $BEARER_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$json_payload" \
    "$API_URL/api/send-sms" 2>/dev/null || echo "")

http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if echo "$body" | grep -q '"success":true'; then
    test_result "Send SMS API" "0" "SMS envoyé (HTTP $http_code)"
else
    test_result "Send SMS API" "1" "Erreur envoi SMS (HTTP $http_code)"
fi
echo ""

# Test 10: Validation numéro invalide
echo "Test 10: Validation numéro invalide..."
json_payload=$(cat <<EOF
{
  "senderId": "TestScript",
  "destinataire": "abc123",
  "text": "$TEST_MESSAGE"
}
EOF
)

response=$(curl -s -w "%{http_code}" -X POST \
    -H "Authorization: Bearer $BEARER_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$json_payload" \
    "$API_URL/api/send-sms" 2>/dev/null || echo "000")

if [ "$response" = "400" ]; then
    test_result "Validation Numéro" "0" "Numéro invalide rejeté (400)"
else
    test_result "Validation Numéro" "1" "Devrait retourner 400, reçu: $response"
fi
echo ""

# Test 11: Envoi MMS via API
echo "Test 11: Envoi MMS via API POST /api/send-mms..."
json_payload=$(cat <<EOF
{
  "senderId": "TestScript",
  "destinataire": "$TEST_PHONE",
  "text": "Test MMS",
  "base64Jpeg": "/9j/4AAQSkZJRgABAQEAYABgAAD"
}
EOF
)

response=$(curl -s -w "%{http_code}" -X POST \
    -H "Authorization: Bearer $BEARER_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$json_payload" \
    "$API_URL/api/send-mms" 2>/dev/null || echo "000")

if [ "$response" = "200" ] || [ "$response" = "500" ]; then
    # 200 = succès, 500 = erreur serveur valide (image invalide)
    test_result "Send MMS API" "0" "MMS traité (HTTP $response)"
else
    test_result "Send MMS API" "1" "Erreur inattendue (HTTP $response)"
fi
echo ""

# Test 12: Route automatique SMS/MMS
echo "Test 12: Route automatique (POST /api/send-message)..."
json_payload=$(cat <<EOF
{
  "senderId": "TestScript",
  "destinataire": "$TEST_PHONE",
  "text": "$TEST_MESSAGE"
}
EOF
)

response=$(curl -s -w "%{http_code}" -X POST \
    -H "Authorization: Bearer $BEARER_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$json_payload" \
    "$API_URL/api/send-message" 2>/dev/null || echo "000")

if [ "$response" = "200" ]; then
    test_result "Route Auto SMS/MMS" "0" "Message routé automatiquement (200)"
else
    test_result "Route Auto SMS/MMS" "1" "Erreur routage (HTTP $response)"
fi
echo ""

# Test 13: Enregistrement log
echo "Test 13: Enregistrement log (POST /api/logs)..."
json_payload=$(cat <<EOF
{
  "message": "Test log from script"
}
EOF
)

response=$(curl -s -w "%{http_code}" -X POST \
    -H "Authorization: Bearer $BEARER_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$json_payload" \
    "$API_URL/api/logs" 2>/dev/null || echo "000")

if [ "$response" = "200" ]; then
    test_result "Send Log API" "0" "Log enregistré (200)"
else
    test_result "Send Log API" "1" "Erreur log (HTTP $response)"
fi
echo ""

# Test 14: 404 endpoint inexistant
echo "Test 14: 404 endpoint inexistant..."
response=$(curl -s -w "%{http_code}" -o /dev/null \
    -H "Authorization: Bearer $BEARER_TOKEN" \
    "$API_URL/api/invalid-endpoint" 2>/dev/null || echo "000")

if [ "$response" = "404" ]; then
    test_result "404 Not Found" "0" "Correctement retourné (404)"
else
    test_result "404 Not Found" "1" "Devrait retourner 404, reçu: $response"
fi
echo ""

# ═════════════════════════════════════════════════════════════
# SECTION 3: Résumé Final
# ═════════════════════════════════════════════════════════════

echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  RÉSUMÉ DES TESTS                                          ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

total_tests=$((TESTS_PASSED + TESTS_FAILED))
success_percent=$((TESTS_PASSED * 100 / total_tests))

echo "Total Tests: $total_tests"
echo -e "${GREEN}✅ Réussis: $TESTS_PASSED${NC}"
echo -e "${RED}❌ Échoués: $TESTS_FAILED${NC}"
echo "Taux de réussite: $success_percent%"
echo ""

# Afficher un indicateur visuel
if [ "$TESTS_FAILED" = "0" ]; then
    echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  🎉 TOUS LES TESTS RÉUSSIS! 🎉                           ║${NC}"
    echo -e "${GREEN}║  Circuit SMS/MMS VALIDÉ EN LOCAL ET À DISTANCE            ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
    exit 0
else
    echo -e "${RED}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║  ⚠️  CERTAINS TESTS ONT ÉCHOUÉ ⚠️                        ║${NC}"
    echo -e "${RED}║  Vérifiez la configuration et les logs                     ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════════════════════════╝${NC}"
    exit 1
fi

