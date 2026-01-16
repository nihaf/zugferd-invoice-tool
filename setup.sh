#!/bin/bash
# ===========================================
# ZUGFeRD Invoice Tool - Setup Script
# ===========================================

set -e

echo "🚀 ZUGFeRD Invoice Tool Setup"
echo "=============================="

# Check for Java
if ! command -v java &> /dev/null; then
    echo "❌ Java nicht gefunden. Bitte Java 25 installieren."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
echo "✓ Java Version: $JAVA_VERSION"

# Check for Gradle (optional, will use wrapper)
if command -v gradle &> /dev/null; then
    GRADLE_VERSION=$(gradle --version | grep "Gradle" | head -n 1)
    echo "✓ $GRADLE_VERSION"
fi

# Initialize Gradle Wrapper if not present
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo ""
    echo "📦 Initialisiere Gradle Wrapper..."
    
    if command -v gradle &> /dev/null; then
        gradle wrapper --gradle-version 8.12
        echo "✓ Gradle Wrapper initialisiert"
    else
        echo "⚠️  Gradle nicht gefunden. Bitte manuell initialisieren:"
        echo "   gradle wrapper --gradle-version 8.12"
        echo ""
        echo "   Oder Gradle Wrapper JAR manuell herunterladen:"
        echo "   https://services.gradle.org/distributions/gradle-8.12-bin.zip"
    fi
fi

# Download sRGB ICC Profile if not present
ICC_FILE="src/main/resources/sRGB.icc"
if [ ! -f "$ICC_FILE" ]; then
    echo ""
    echo "🎨 Lade sRGB ICC-Profil herunter..."
    
    mkdir -p src/main/resources
    
    if command -v curl &> /dev/null; then
        curl -sL -o "$ICC_FILE" \
            "https://www.color.org/sRGB_v4_ICC_preference.icc" 2>/dev/null || \
        curl -sL -o "$ICC_FILE" \
            "https://raw.githubusercontent.com/nickylin/sRGB-ICC-Profile/master/sRGB%20Profile.icc" 2>/dev/null || \
        echo "⚠️  ICC-Profil konnte nicht heruntergeladen werden."
        
        if [ -f "$ICC_FILE" ] && [ -s "$ICC_FILE" ]; then
            echo "✓ sRGB ICC-Profil heruntergeladen"
        fi
    else
        echo "⚠️  curl nicht gefunden. Bitte ICC-Profil manuell herunterladen."
    fi
fi

# Create temp directories
echo ""
echo "📁 Erstelle temporäre Verzeichnisse..."
mkdir -p /tmp/zugferd/uploads /tmp/zugferd/output 2>/dev/null || true
echo "✓ Verzeichnisse erstellt"

echo ""
echo "=============================="
echo "✅ Setup abgeschlossen!"
echo ""
echo "Nächste Schritte:"
echo "  1. ./gradlew build      # Projekt bauen"
echo "  2. ./gradlew bootRun    # Anwendung starten"
echo "  3. http://localhost:8080 öffnen"
echo ""
