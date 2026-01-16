package de.zugferd.invoicetool.service;

import de.zugferd.invoicetool.config.AppConfig.ZugferdProperties;
import de.zugferd.invoicetool.exception.InvoiceProcessingException;
import de.zugferd.invoicetool.model.InvoiceMetadata;
import de.zugferd.invoicetool.model.ProcessingStatus;
import de.zugferd.invoicetool.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Orchestriert den gesamten E-Rechnungs-Workflow.
 */
@Service
public class InvoiceService {
    
    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);
    
    private final StorageService storageService;
    private final ZugferdGeneratorService zugferdGeneratorService;
    private final ValidationService validationService;
    private final ZugferdProperties zugferdProperties;
    
    public InvoiceService(StorageService storageService,
                          ZugferdGeneratorService zugferdGeneratorService,
                          ValidationService validationService,
                          ZugferdProperties zugferdProperties) {
        this.storageService = storageService;
        this.zugferdGeneratorService = zugferdGeneratorService;
        this.validationService = validationService;
        this.zugferdProperties = zugferdProperties;
    }
    
    /**
     * Generiert eine E-Rechnung aus den hochgeladenen Daten.
     *
     * @param sessionId Die Session-ID
     * @param metadata Die Rechnungsmetadaten
     * @return Der neue Verarbeitungsstatus
     */
    public ProcessingStatus generateInvoice(String sessionId, InvoiceMetadata metadata) {
        log.info("Starting invoice generation for session: {}", sessionId);
        
        // Status prüfen
        ProcessingStatus currentStatus = storageService.getStatusOrThrow(sessionId);
        
        if (!(currentStatus instanceof ProcessingStatus.Uploaded uploaded)) {
            throw new InvoiceProcessingException(
                "INVALID_STATE",
                "Ungültiger Status für E-Rechnung-Generierung",
                "Aktueller Status: " + currentStatus.getClass().getSimpleName()
            );
        }
        
        // Status auf "Processing" setzen
        var processingStatus = new ProcessingStatus.Processing(
            sessionId,
            Instant.now(),
            metadata
        );
        storageService.updateStatus(sessionId, processingStatus);
        
        try {
            // Output-Verzeichnis vorbereiten
            Path outputDir = storageService.prepareOutputDirectory(sessionId);
            Path outputPath = outputDir.resolve("e-invoice.pdf");
            
            // E-Rechnung generieren
            zugferdGeneratorService.generateInvoice(
                uploaded.originalPdfPath(),
                outputPath,
                metadata
            );
            
            // Validierung durchführen
            ValidationResult validationResult;
            if (zugferdProperties.isValidateOnGeneration()) {
                validationResult = validationService.validatePdfA3(outputPath);
            } else {
                validationResult = ValidationResult.success("Skipped", 0);
            }
            
            // Status auf "Completed" setzen
            var completedStatus = new ProcessingStatus.Completed(
                sessionId,
                Instant.now(),
                outputPath,
                validationResult,
                metadata
            );
            storageService.updateStatus(sessionId, completedStatus);
            
            log.info("Invoice generation completed for session: {} (valid: {})", 
                sessionId, validationResult.valid());
            
            return completedStatus;
            
        } catch (IOException e) {
            log.error("IO error during invoice generation", e);
            return handleFailure(sessionId, "IO-Fehler bei der E-Rechnung-Generierung", e.getMessage());
            
        } catch (InvoiceProcessingException e) {
            log.error("Processing error during invoice generation", e);
            return handleFailure(sessionId, e.getMessage(), e.getDetails());
            
        } catch (Exception e) {
            log.error("Unexpected error during invoice generation", e);
            return handleFailure(sessionId, "Unerwarteter Fehler", e.getMessage());
        }
    }
    
    /**
     * Behandelt einen Fehler im Workflow.
     */
    private ProcessingStatus.Failed handleFailure(String sessionId, String message, String details) {
        var failedStatus = new ProcessingStatus.Failed(
            sessionId,
            Instant.now(),
            message,
            details
        );
        storageService.updateStatus(sessionId, failedStatus);
        return failedStatus;
    }
    
    /**
     * Gibt den aktuellen Verarbeitungsstatus zurück.
     */
    public ProcessingStatus getStatus(String sessionId) {
        return storageService.getStatusOrThrow(sessionId);
    }
    
    /**
     * Lädt die generierte E-Rechnung herunter.
     *
     * @param sessionId Die Session-ID
     * @return Die Bytes der PDF-Datei
     */
    public byte[] downloadInvoice(String sessionId) {
        ProcessingStatus status = storageService.getStatusOrThrow(sessionId);
        
        if (!(status instanceof ProcessingStatus.Completed completed)) {
            throw new InvoiceProcessingException(
                "NOT_COMPLETED",
                "E-Rechnung wurde noch nicht erstellt",
                "Aktueller Status: " + status.getClass().getSimpleName()
            );
        }
        
        try {
            byte[] pdfBytes = Files.readAllBytes(completed.generatedPdfPath());
            
            // Als heruntergeladen markieren
            storageService.markAsDownloaded(sessionId);
            
            return pdfBytes;
            
        } catch (IOException e) {
            log.error("Failed to read generated invoice", e);
            throw new InvoiceProcessingException("Fehler beim Lesen der E-Rechnung", e);
        }
    }
    
    /**
     * Gibt den Dateinamen für den Download zurück.
     */
    public String getDownloadFilename(String sessionId) {
        ProcessingStatus status = storageService.getStatusOrThrow(sessionId);
        
        if (status instanceof ProcessingStatus.Completed completed) {
            String invoiceNumber = completed.metadata().invoiceNumber();
            // Dateiname bereinigen
            String safeNumber = invoiceNumber.replaceAll("[^a-zA-Z0-9.-]", "_");
            return "E-Rechnung_" + safeNumber + ".pdf";
        }
        
        return "e-rechnung.pdf";
    }
    
    /**
     * Löscht eine Session und alle zugehörigen Dateien.
     */
    public void cleanup(String sessionId) {
        storageService.deleteSession(sessionId);
    }
    
    /**
     * Prüft ob ein Download möglich ist.
     */
    public boolean canDownload(String sessionId) {
        return storageService.getStatus(sessionId)
            .map(ProcessingStatus::canDownload)
            .orElse(false);
    }
}
