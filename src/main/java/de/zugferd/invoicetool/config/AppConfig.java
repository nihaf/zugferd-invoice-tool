package de.zugferd.invoicetool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Zentrale Anwendungskonfiguration.
 */
@Configuration
public class AppConfig {
    
    /**
     * Virtual Thread Executor für asynchrone Operationen (Java 21+).
     * Nutzt Virtual Threads für effiziente I/O-Operationen.
     */
    @Bean
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    
    /**
     * Anwendungseinstellungen aus application.yml.
     */
    @Bean
    @ConfigurationProperties(prefix = "zugferd")
    public ZugferdProperties zugferdProperties() {
        return new ZugferdProperties();
    }
    
    /**
     * Konfigurationsklasse für ZUGFeRD-spezifische Einstellungen.
     */
    public static class ZugferdProperties {
        private String profile = "EN16931";
        private String version = "2.3";
        private long maxFileSizeBytes = 10 * 1024 * 1024; // 10 MB
        private int sessionTimeoutMinutes = 30;
        private boolean validateOnGeneration = true;
        
        public String getProfile() { return profile; }
        public void setProfile(String profile) { this.profile = profile; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public long getMaxFileSizeBytes() { return maxFileSizeBytes; }
        public void setMaxFileSizeBytes(long maxFileSizeBytes) { this.maxFileSizeBytes = maxFileSizeBytes; }
        
        public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
        public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }
        
        public boolean isValidateOnGeneration() { return validateOnGeneration; }
        public void setValidateOnGeneration(boolean validateOnGeneration) { this.validateOnGeneration = validateOnGeneration; }
    }
}
