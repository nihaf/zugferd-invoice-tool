package de.zugferd.invoicetool.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Konfiguration für die temporäre Dateispeicherung.
 */
@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    /**
     * Storage-Eigenschaften aus application.yml.
     */
    @Bean
    @ConfigurationProperties(prefix = "storage")
    public StorageProperties storageProperties() {
        return new StorageProperties();
    }

    /**
     * Initialisiert das Upload-Verzeichnis beim Start.
     */
    @Bean
    public StorageDirectoryInitializer storageDirectoryInitializer(StorageProperties props) {
        return new StorageDirectoryInitializer(props);
    }

    static class StorageDirectoryInitializer {
        StorageDirectoryInitializer(StorageProperties props) {
            try {
                Path uploadDir = Path.of(props.getUploadDir());
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                    log.info("Created upload directory: {}", uploadDir.toAbsolutePath());
                }

                Path outputDir = Path.of(props.getOutputDir());
                if (!Files.exists(outputDir)) {
                    Files.createDirectories(outputDir);
                    log.info("Created output directory: {}", outputDir.toAbsolutePath());
                }
            } catch (IOException e) {
                log.error("Failed to create storage directories", e);
                throw new RuntimeException("Could not initialize storage directories", e);
            }
        }
    }
    
    /**
     * Konfigurationsklasse für Storage-Einstellungen.
     */
    public static class StorageProperties {
        private String uploadDir = "/tmp/zugferd/uploads";
        private String outputDir = "/tmp/zugferd/output";
        private int cleanupIntervalMinutes = 5;
        private int fileRetentionMinutes = 30;
        
        public String getUploadDir() { return uploadDir; }
        public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
        
        public String getOutputDir() { return outputDir; }
        public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
        
        public int getCleanupIntervalMinutes() { return cleanupIntervalMinutes; }
        public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) { this.cleanupIntervalMinutes = cleanupIntervalMinutes; }
        
        public int getFileRetentionMinutes() { return fileRetentionMinutes; }
        public void setFileRetentionMinutes(int fileRetentionMinutes) { this.fileRetentionMinutes = fileRetentionMinutes; }
        
        public Path getUploadPath() { return Path.of(uploadDir); }
        public Path getOutputPath() { return Path.of(outputDir); }
    }
}
