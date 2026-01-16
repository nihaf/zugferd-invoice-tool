package de.zugferd.invoicetool.exception;

/**
 * Exception für Fehler bei der Rechnungsverarbeitung.
 */
public class InvoiceProcessingException extends RuntimeException {
    
    private final String errorCode;
    private final String details;
    
    public InvoiceProcessingException(String message) {
        super(message);
        this.errorCode = "PROCESSING_ERROR";
        this.details = null;
    }
    
    public InvoiceProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PROCESSING_ERROR";
        this.details = cause != null ? cause.getMessage() : null;
    }
    
    public InvoiceProcessingException(String errorCode, String message, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
    
    public InvoiceProcessingException(String errorCode, String message, String details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getDetails() {
        return details;
    }
    
    // Factory methods for common errors
    public static InvoiceProcessingException pdfReadError(String filename, Throwable cause) {
        return new InvoiceProcessingException(
            "PDF_READ_ERROR",
            "Fehler beim Lesen der PDF-Datei: " + filename,
            cause.getMessage(),
            cause
        );
    }
    
    public static InvoiceProcessingException pdfConversionError(Throwable cause) {
        return new InvoiceProcessingException(
            "PDF_CONVERSION_ERROR",
            "Fehler bei der PDF/A-3 Konvertierung",
            cause.getMessage(),
            cause
        );
    }
    
    public static InvoiceProcessingException zugferdGenerationError(Throwable cause) {
        return new InvoiceProcessingException(
            "ZUGFERD_GENERATION_ERROR",
            "Fehler bei der ZUGFeRD-Generierung",
            cause.getMessage(),
            cause
        );
    }
    
    public static InvoiceProcessingException sessionNotFound(String sessionId) {
        return new InvoiceProcessingException(
            "SESSION_NOT_FOUND",
            "Sitzung nicht gefunden: " + sessionId,
            "Die Sitzung ist abgelaufen oder existiert nicht."
        );
    }
    
    public static InvoiceProcessingException fileTooLarge(long size, long maxSize) {
        return new InvoiceProcessingException(
            "FILE_TOO_LARGE",
            "Datei ist zu groß",
            "Maximale Größe: " + (maxSize / 1024 / 1024) + " MB, Hochgeladen: " + (size / 1024 / 1024) + " MB"
        );
    }
    
    public static InvoiceProcessingException invalidFileType(String contentType) {
        return new InvoiceProcessingException(
            "INVALID_FILE_TYPE",
            "Ungültiger Dateityp",
            "Erwartet: application/pdf, Erhalten: " + contentType
        );
    }
}
