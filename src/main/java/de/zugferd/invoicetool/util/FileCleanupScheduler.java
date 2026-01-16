package de.zugferd.invoicetool.util;

import de.zugferd.invoicetool.config.StorageConfig.StorageProperties;
import de.zugferd.invoicetool.model.ProcessingStatus;
import de.zugferd.invoicetool.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduled Task für die automatische Bereinigung alter Sessions.
 */
@Component
public class FileCleanupScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(FileCleanupScheduler.class);
    
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    
    public FileCleanupScheduler(StorageService storageService, StorageProperties storageProperties) {
        this.storageService = storageService;
        this.storageProperties = storageProperties;
    }
    
    /**
     * Bereinigt abgelaufene Sessions in regelmäßigen Abständen.
     * Standard: alle 5 Minuten.
     */
    @Scheduled(fixedRateString = "#{${storage.cleanup-interval-minutes:5} * 60000}")
    public void cleanupExpiredSessions() {
        log.debug("Running scheduled cleanup of expired sessions");
        
        int retentionMinutes = storageProperties.getFileRetentionMinutes();
        Instant expirationThreshold = Instant.now().minus(Duration.ofMinutes(retentionMinutes));
        
        int cleanedCount = 0;
        
        for (String sessionId : storageService.getAllSessionIds()) {
            try {
                var statusOpt = storageService.getStatus(sessionId);
                if (statusOpt.isEmpty()) {
                    continue;
                }
                
                ProcessingStatus status = statusOpt.get();
                
                // Session löschen wenn:
                // 1. Sie bereits heruntergeladen wurde
                // 2. Sie älter als die Retention-Zeit ist
                boolean shouldClean = switch (status) {
                    case ProcessingStatus.Downloaded _ -> true;
                    default -> status.timestamp().isBefore(expirationThreshold);
                };
                
                if (shouldClean) {
                    storageService.deleteSession(sessionId);
                    cleanedCount++;
                    log.info("Cleaned up session: {} (status: {})", 
                        sessionId, status.getClass().getSimpleName());
                }
                
            } catch (Exception e) {
                log.error("Error cleaning up session: {}", sessionId, e);
            }
        }
        
        if (cleanedCount > 0) {
            log.info("Cleanup completed: {} sessions removed", cleanedCount);
        }
    }
}
