package de.zugferd.invoicetool.model;

import java.util.List;

/**
 * Ergebnis der PDF/A-3 Validierung durch VeraPDF.
 */
public record ValidationResult(
    boolean valid,
    String profileName,
    List<ValidationError> errors,
    List<ValidationWarning> warnings,
    long processingTimeMs
) {
    /**
     * Ein Validierungsfehler.
     */
    public record ValidationError(
        String ruleId,
        String specification,
        String clause,
        String description,
        String context
    ) {}
    
    /**
     * Eine Validierungswarnung.
     */
    public record ValidationWarning(
        String ruleId,
        String message
    ) {}
    
    /**
     * Erstellt ein erfolgreiches Validierungsergebnis.
     */
    public static ValidationResult success(String profileName, long processingTimeMs) {
        return new ValidationResult(true, profileName, List.of(), List.of(), processingTimeMs);
    }
    
    /**
     * Erstellt ein fehlgeschlagenes Validierungsergebnis.
     */
    public static ValidationResult failure(
            String profileName, 
            List<ValidationError> errors, 
            List<ValidationWarning> warnings,
            long processingTimeMs) {
        return new ValidationResult(false, profileName, errors, warnings, processingTimeMs);
    }
    
    /**
     * Prüft ob Warnungen vorhanden sind.
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
    
    /**
     * Anzahl der Fehler.
     */
    public int errorCount() {
        return errors != null ? errors.size() : 0;
    }
    
    /**
     * Anzahl der Warnungen.
     */
    public int warningCount() {
        return warnings != null ? warnings.size() : 0;
    }
}
