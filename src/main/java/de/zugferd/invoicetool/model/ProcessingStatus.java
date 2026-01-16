package de.zugferd.invoicetool.model;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Status der Rechnungsverarbeitung als Sealed Interface.
 * Ermöglicht Pattern Matching in switch-Ausdrücken.
 */
public sealed interface ProcessingStatus {
    
    /**
     * Sitzungs-ID für diese Verarbeitung.
     */
    String sessionId();
    
    /**
     * Zeitstempel der Statusänderung.
     */
    Instant timestamp();
    
    /**
     * PDF wurde hochgeladen, wartet auf Metadaten.
     */
    record Uploaded(
        String sessionId,
        Instant timestamp,
        Path originalPdfPath,
        String originalFilename,
        long fileSizeBytes
    ) implements ProcessingStatus {}
    
    /**
     * Metadaten wurden eingegeben, Verarbeitung gestartet.
     */
    record Processing(
        String sessionId,
        Instant timestamp,
        InvoiceMetadata metadata
    ) implements ProcessingStatus {}
    
    /**
     * E-Rechnung wurde erfolgreich erstellt.
     */
    record Completed(
        String sessionId,
        Instant timestamp,
        Path generatedPdfPath,
        ValidationResult validationResult,
        InvoiceMetadata metadata
    ) implements ProcessingStatus {}
    
    /**
     * Verarbeitung ist fehlgeschlagen.
     */
    record Failed(
        String sessionId,
        Instant timestamp,
        String errorMessage,
        String errorDetails
    ) implements ProcessingStatus {}
    
    /**
     * Datei wurde heruntergeladen und kann gelöscht werden.
     */
    record Downloaded(
        String sessionId,
        Instant timestamp,
        Path downloadedFilePath
    ) implements ProcessingStatus {}
    
    /**
     * Pattern Matching Helper für Status-Beschreibung.
     */
    default String statusDescription() {
        return switch (this) {
            case Uploaded u -> "PDF hochgeladen: " + u.originalFilename();
            case Processing p -> "Verarbeitung läuft...";
            case Completed c -> c.validationResult().valid() 
                ? "E-Rechnung erfolgreich erstellt" 
                : "E-Rechnung erstellt (mit Validierungswarnungen)";
            case Failed f -> "Fehler: " + f.errorMessage();
            case Downloaded d -> "Heruntergeladen";
        };
    }
    
    /**
     * Prüft ob der Status ein Endstatus ist.
     */
    default boolean isTerminal() {
        return switch (this) {
            case Downloaded d -> true;
            case Failed f -> true;
            default -> false;
        };
    }
    
    /**
     * Prüft ob Download möglich ist.
     */
    default boolean canDownload() {
        return this instanceof Completed;
    }
}
