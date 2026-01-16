# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

```bash
# Run application (available at http://localhost:8080)
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "de.zugferd.invoicetool.service.ZugferdGeneratorServiceTest"

# Run a single test method
./gradlew test --tests "de.zugferd.invoicetool.service.ZugferdGeneratorServiceTest.testMethodName"

# Build JAR
./gradlew build

# Docker build and run
docker build -t zugferd-invoice-tool .
docker-compose up -d
```

Note: Java preview features are enabled (`--enable-preview`), configured in build.gradle.kts.

## Architecture Overview

This is a Spring Boot 4.x web application for creating ZUGFeRD 2.3 / Factur-X compliant e-invoices from uploaded PDFs.

### Processing Flow

1. User uploads PDF via `InvoiceController` → `StorageService` creates session with unique ID
2. User fills invoice metadata form (seller, buyer, line items)
3. `InvoiceService.generateInvoice()` orchestrates the workflow:
   - `ZugferdGeneratorService` converts PDF to PDF/A-3 and embeds ZUGFeRD XML using Mustang library
   - `ValidationService` validates PDF/A-3 compliance using VeraPDF
4. User downloads result via `DownloadController`

### Session-Based State Machine

`ProcessingStatus` is a sealed interface with states: `Uploaded` → `Processing` → `Completed`/`Failed`

Sessions are stored in-memory with temp files cleaned up by `FileCleanupScheduler`.

### Key Services

- `StorageService`: Manages file storage and session state in `/tmp/zugferd/`
- `ZugferdGeneratorService`: Wraps Mustang library for ZUGFeRD XML generation and PDF embedding
- `ValidationService`: Wraps VeraPDF for PDF/A-3 validation
- `PdfA3ConverterService`: Handles PDF to PDF/A-3 conversion

### Frontend

Thymeleaf templates with Bootstrap 5. i18n via `messages.properties` (German) and `messages_en.properties`.

### Configuration

Key settings in `application.yml` under `zugferd:` (profile, version, validation) and `storage:` (directories, cleanup intervals).
