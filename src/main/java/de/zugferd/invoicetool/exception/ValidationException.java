package de.zugferd.invoicetool.exception;

import de.zugferd.invoicetool.model.ValidationResult;

/**
 * Exception für PDF/A-3 Validierungsfehler.
 */
public class ValidationException extends RuntimeException {
    
    private final ValidationResult validationResult;
    
    public ValidationException(String message, ValidationResult validationResult) {
        super(message);
        this.validationResult = validationResult;
    }
    
    public ValidationException(String message, ValidationResult validationResult, Throwable cause) {
        super(message, cause);
        this.validationResult = validationResult;
    }
    
    public ValidationResult getValidationResult() {
        return validationResult;
    }
    
    /**
     * Gibt eine formatierte Fehlermeldung zurück.
     */
    public String getFormattedErrors() {
        if (validationResult == null || validationResult.errors().isEmpty()) {
            return "Keine Details verfügbar";
        }
        
        var sb = new StringBuilder();
        for (var error : validationResult.errors()) {
            sb.append("- [%s] %s%n".formatted(error.ruleId(), error.description()));
        }
        return sb.toString();
    }
}
