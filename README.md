# ZUGFeRD E-Rechnungs-Tool

Ein webbasiertes Tool zur Erstellung von PDF/A-3 konformen E-Rechnungen nach dem ZUGFeRD 2.3 / Factur-X Standard.

## 🚀 Features

- ✅ **ZUGFeRD 2.3 / Factur-X 1.0** konform
- ✅ **PDF/A-3** konforme Ausgabedateien
- ✅ **EN16931** Profil (EU-Standard)
- ✅ **VeraPDF** Validierung
- ✅ **Mehrsprachig** (Deutsch / Englisch) mit On-the-fly Sprachwechsel
- ✅ **Docker** ready
- ✅ **Keine Datenbank** erforderlich

## 📋 Voraussetzungen

- **Java 25** (mit Preview Features)
- **Gradle 8.12+** (wird über Wrapper bereitgestellt)
- **Docker** (optional, für Container-Deployment)

## 🛠️ Installation

### 1. Repository klonen

```bash
git clone <repository-url>
cd zugferd-invoice-tool
```

### 2. Gradle Wrapper initialisieren

Falls der Gradle Wrapper noch nicht vorhanden ist:

```bash
gradle wrapper --gradle-version 8.12
```

### 3. sRGB ICC-Profil hinzufügen

Für vollständige PDF/A-3 Konformität wird ein sRGB ICC-Profil benötigt:

```bash
# Download sRGB Profil von ICC
curl -o src/main/resources/sRGB.icc \
  https://www.color.org/sRGB_v4_ICC_preference.icc
```

### 4. Anwendung starten

```bash
./gradlew bootRun
```

Die Anwendung ist dann unter http://localhost:8080 erreichbar.

## 🐳 Docker

### Build und Start

```bash
# Image bauen
docker build -t zugferd-invoice-tool .

# Container starten
docker run -p 8080:8080 zugferd-invoice-tool
```

### Mit Docker Compose

```bash
docker-compose up -d
```

## 📖 Verwendung

### 1. PDF hochladen

Laden Sie eine bestehende PDF-Rechnung hoch.

### 2. Metadaten eingeben

Füllen Sie die erforderlichen Rechnungsdaten aus:
- Rechnungsnummer und Datum
- Verkäufer- und Käuferdaten
- Rechnungspositionen
- Zahlungsinformationen (optional)

### 3. E-Rechnung generieren

Klicken Sie auf "E-Rechnung erstellen". Das Tool:
1. Konvertiert die PDF zu PDF/A-3
2. Generiert das ZUGFeRD-XML
3. Bettet das XML in die PDF ein
4. Validiert das Ergebnis mit VeraPDF

### 4. Herunterladen

Laden Sie die fertige E-Rechnung herunter.

## 🏗️ Architektur

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Container                      │
│  ┌───────────────────────────────────────────────────┐  │
│  │              Spring Boot Application               │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌───────────┐  │  │
│  │  │  Controller │  │   Service   │  │  Storage  │  │  │
│  │  │  (Web UI)   │──│  (Business) │──│  (Temp)   │  │  │
│  │  └─────────────┘  └─────────────┘  └───────────┘  │  │
│  │         │                │                        │  │
│  │         ▼                ▼                        │  │
│  │  ┌─────────────┐  ┌─────────────┐                │  │
│  │  │  Thymeleaf  │  │  ZUGFeRD    │                │  │
│  │  │  Templates  │  │  Generator  │                │  │
│  │  └─────────────┘  └─────────────┘                │  │
│  │                          │                        │  │
│  │                          ▼                        │  │
│  │                   ┌─────────────┐                │  │
│  │                   │   VeraPDF   │                │  │
│  │                   │  Validator  │                │  │
│  │                   └─────────────┘                │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## 🔧 Konfiguration

Die Konfiguration erfolgt über `application.yml`:

```yaml
# ZUGFeRD Einstellungen
zugferd:
  profile: EN16931
  version: "2.3"
  max-file-size-bytes: 10485760  # 10 MB
  session-timeout-minutes: 30
  validate-on-generation: true

# Storage Einstellungen
storage:
  upload-dir: /tmp/zugferd/uploads
  output-dir: /tmp/zugferd/output
  cleanup-interval-minutes: 5
  file-retention-minutes: 30
```

## 📚 Technologie-Stack

| Komponente | Technologie |
|------------|-------------|
| Sprache | Java 25 |
| Framework | Spring Boot 4.x |
| Build | Gradle 8.x (Kotlin DSL) |
| Frontend | Thymeleaf + Bootstrap 5 |
| PDF | Apache PDFBox 3.x |
| ZUGFeRD | Mustang Library |
| Validierung | VeraPDF |

## 🧪 Tests

```bash
# Unit Tests ausführen
./gradlew test

# Mit Coverage Report
./gradlew test jacocoTestReport
```

## 📁 Projektstruktur

```
zugferd-invoice-tool/
├── build.gradle.kts          # Build-Konfiguration
├── settings.gradle.kts       # Projekt-Settings
├── gradle/
│   └── libs.versions.toml    # Version Catalog
├── Dockerfile
├── docker-compose.yml
└── src/
    ├── main/
    │   ├── java/de/zugferd/invoicetool/
    │   │   ├── config/       # Spring Konfiguration
    │   │   ├── controller/   # Web Controller
    │   │   ├── service/      # Business Logic
    │   │   ├── model/        # Datenmodelle (Records)
    │   │   ├── exception/    # Exception Handling
    │   │   └── util/         # Utilities
    │   └── resources/
    │       ├── templates/    # Thymeleaf Templates
    │       ├── static/       # CSS, JS
    │       └── messages*.properties  # i18n
    └── test/                 # Unit Tests
```

## 🌐 Internationalisierung

Die Sprache kann jederzeit über das Dropdown in der Navigation gewechselt werden:
- 🇩🇪 Deutsch (Standard)
- 🇬🇧 English

Der Sprachwechsel erfolgt via AJAX und Cookie, sodass die Präferenz gespeichert bleibt.

## ⚠️ Einschränkungen (MVP)

- Keine Stapelverarbeitung (nur eine Datei gleichzeitig)
- Keine Datenbank-Persistenz
- Keine Benutzerauthentifizierung
- Session-Timeout nach 30 Minuten

## 📄 Lizenz

MIT License

## 🤝 Mitwirken

Pull Requests sind willkommen! Für größere Änderungen bitte zuerst ein Issue eröffnen.

## 📞 Support

Bei Fragen oder Problemen erstellen Sie bitte ein Issue im Repository.

---

**Powered by:**
- [Mustang Project](https://github.com/ZUGFeRD/mustangproject) - ZUGFeRD Library
- [VeraPDF](https://verapdf.org/) - PDF/A Validation
- [Apache PDFBox](https://pdfbox.apache.org/) - PDF Processing
