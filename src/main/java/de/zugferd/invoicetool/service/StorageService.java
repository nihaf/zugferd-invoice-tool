package de.zugferd.invoicetool.service;

import de.zugferd.invoicetool.config.StorageConfig.StorageProperties;
import de.zugferd.invoicetool.exception.InvoiceProcessingException;
import de.zugferd.invoicetool.model.ProcessingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service für die temporäre Dateiverwaltung.
 * Verwaltet Sessions und zugehörige Dateien.
 */
@Service
public class StorageService {
    
    private static final Logger log = LoggerFactory.getLogger(StorageService.class);
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    
    private final StorageProperties storageProperties;
    private final Map<String, ProcessingStatus> sessions = new ConcurrentHashMap<>();
    
    public StorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }
    
    /**
     * Erstellt eine neue Session und speichert die hochgeladene PDF.
     *
     * @param file Die hochgeladene PDF-Datei
     * @return Die Session-ID
     */
    public String createSession(MultipartFile file) {
        validateFile(file);
        
        String sessionId = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename() != null 
            ? file.getOriginalFilename() 
            : "upload.pdf";
        
        Path sessionDir = storageProperties.getUploadPath().resolve(sessionId);
        try {
            Files.createDirectories(sessionDir);
            Path targetPath = sessionDir.resolve("original.pdf");
            
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            var status = new ProcessingStatus.Uploaded(
                sessionId,
                Instant.now(),
                targetPath,
                originalFilename,
                file.getSize()
            );
            sessions.put(sessionId, status);
            
            log.info("Created session {} for file: {} ({} bytes)", 
                sessionId, originalFilename, file.getSize());
            
            return sessionId;
            
        } catch (IOException e) {
            log.error("Failed to store uploaded file for session {}", sessionId, e);
            throw new InvoiceProcessingException("Fehler beim Speichern der Datei", e);
        }
    }
    
    /**
     * Validiert die hochgeladene Datei.
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvoiceProcessingException("EMPTY_FILE", "Keine Datei hochgeladen", null);
        }
        
        if (file.getSize() > storageProperties.getUploadPath().toFile().getUsableSpace()) {
            throw InvoiceProcessingException.fileTooLarge(file.getSize(), storageProperties.getUploadPath().toFile().getUsableSpace());
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(PDF_CONTENT_TYPE)) {
            throw InvoiceProcessingException.invalidFileType(contentType);
        }
    }
    
    /**
     * Gibt den aktuellen Status einer Session zurück.
     */
    public Optional<ProcessingStatus> getStatus(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }
    
    /**
     * Gibt den Status zurück oder wirft eine Exception.
     */
    public ProcessingStatus getStatusOrThrow(String sessionId) {
        return getStatus(sessionId)
            .orElseThrow(() -> InvoiceProcessingException.sessionNotFound(sessionId));
    }
    
    /**
     * Aktualisiert den Status einer Session.
     */
    public void updateStatus(String sessionId, ProcessingStatus status) {
        if (!sessions.containsKey(sessionId)) {
            throw InvoiceProcessingException.sessionNotFound(sessionId);
        }
        sessions.put(sessionId, status);
        log.debug("Updated session {} status to: {}", sessionId, status.getClass().getSimpleName());
    }
    
    /**
     * Gibt den Pfad für die generierte E-Rechnung zurück.
     */
    public Path getOutputPath(String sessionId) {
        return storageProperties.getOutputPath().resolve(sessionId).resolve("e-invoice.pdf");
    }
    
    /**
     * Gibt den Pfad der Original-PDF zurück.
     */
    public Path getOriginalPdfPath(String sessionId) {
        return storageProperties.getUploadPath().resolve(sessionId).resolve("original.pdf");
    }
    
    /**
     * Bereitet das Output-Verzeichnis vor.
     */
    public Path prepareOutputDirectory(String sessionId) throws IOException {
        Path outputDir = storageProperties.getOutputPath().resolve(sessionId);
        Files.createDirectories(outputDir);
        return outputDir;
    }
    
    /**
     * Löscht alle Dateien einer Session.
     */
    public void deleteSession(String sessionId) {
        try {
            // Upload-Verzeichnis löschen
            Path uploadDir = storageProperties.getUploadPath().resolve(sessionId);
            deleteDirectoryRecursively(uploadDir);
            
            // Output-Verzeichnis löschen
            Path outputDir = storageProperties.getOutputPath().resolve(sessionId);
            deleteDirectoryRecursively(outputDir);
            
            sessions.remove(sessionId);
            log.info("Deleted session: {}", sessionId);
            
        } catch (IOException e) {
            log.error("Failed to delete session {}", sessionId, e);
        }
    }
    
    /**
     * Löscht ein Verzeichnis rekursiv.
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var paths = Files.walk(directory)) {
                paths.sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("Could not delete: {}", path);
                        }
                    });
            }
        }
    }
    
    /**
     * Gibt alle Session-IDs zurück.
     */
    public java.util.Set<String> getAllSessionIds() {
        return sessions.keySet();
    }
    
    /**
     * Prüft ob eine Session existiert.
     */
    public boolean sessionExists(String sessionId) {
        return sessions.containsKey(sessionId);
    }
    
    /**
     * Markiert eine Session als heruntergeladen.
     */
    public void markAsDownloaded(String sessionId) {
        var currentStatus = getStatusOrThrow(sessionId);
        if (currentStatus instanceof ProcessingStatus.Completed completed) {
            var downloaded = new ProcessingStatus.Downloaded(
                sessionId,
                Instant.now(),
                completed.generatedPdfPath()
            );
            sessions.put(sessionId, downloaded);
            log.info("Session {} marked as downloaded", sessionId);
        }
    }
}
