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

Note: Java 21 preview features are enabled (`--enable-preview`), configured in build.gradle.kts. This enables sealed interfaces with pattern matching and other modern Java features. Virtual threads are used for async I/O operations.

## Architecture Overview

This is a Spring Boot 4.x web application for creating ZUGFeRD 2.3 / Factur-X compliant e-invoices from uploaded PDFs.

### Processing Flow

1. User uploads PDF via `InvoiceController` → `StorageService` creates session with unique ID
2. User fills invoice metadata form (seller, buyer, line items), can switch language via `LocaleController`
3. `InvoiceService.generateInvoice()` orchestrates the workflow:
   - `PdfA3ConverterService` converts PDF to PDF/A-3 format (if needed)
   - `ZugferdGeneratorService` embeds ZUGFeRD XML using Mustang library
   - `ValidationService` validates PDF/A-3 compliance using VeraPDF
4. User downloads result via `DownloadController`

### Session-Based State Machine

`ProcessingStatus` is a sealed interface (Java 21) with 5 states: `Uploaded` → `Processing` → `Completed`/`Failed` → `Downloaded`

Sessions are stored in-memory (ConcurrentHashMap) with temp files cleaned up by `FileCleanupScheduler` (runs every 5 minutes).

### Key Services

- `InvoiceService`: Orchestrates the complete e-invoice generation workflow (async with virtual threads)
- `StorageService`: Manages file storage and session state in `/tmp/zugferd/` (in-memory with ConcurrentHashMap)
- `ZugferdGeneratorService`: Wraps Mustang library for ZUGFeRD XML generation (EN16931 profile) and PDF embedding
- `ValidationService`: Wraps VeraPDF library for PDF/A-3B validation with detailed error reporting
- `PdfA3ConverterService`: Converts PDF to PDF/A-3 using Apache PDFBox (adds XMP metadata and sRGB output intent)

### Key Components

- `InvoiceController`: Main workflow (upload, metadata form, generation, results) with AJAX endpoints for dynamic line items
- `DownloadController`: Handles PDF downloads and preview
- `LocaleController`: Language switching (German ↔ English)
- `FileCleanupScheduler`: Scheduled cleanup task (every 5 min, deletes sessions older than 30 min)
- `GlobalExceptionHandler`: Centralized error handling with `@ControllerAdvice`

### Frontend

Thymeleaf templates with custom CSS framework (Pico CSS-inspired). i18n via `messages.properties` (German) and `messages_en.properties`. Vanilla JavaScript for file upload (drag & drop), form interactions, and AJAX line item management.

### Configuration

Key settings in `application.yml`:
- `zugferd:` ZUGFeRD settings (profile: EN16931, version: 2.3, validation: enabled, max file size: 10 MB, session timeout: 30 min)
- `storage:` File storage paths (upload/output dirs in `/tmp/zugferd/`), cleanup intervals (5 min), file retention (30 min)
- `invoice.defaults:` Pre-filled form values for seller, payment info, and currency defaults

### Domain Model

- `ProcessingStatus`: Sealed interface with 5 record implementations using Java 21 pattern matching
- `InvoiceMetadata`: Record with builder pattern, includes validation annotations and calculation helpers
- `InvoiceItem`, `Party`, `Address`, `BankDetails`: Record-based value objects
- `ValidationResult`: VeraPDF validation outcome with nested error/warning records
