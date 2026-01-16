package de.zugferd.invoicetool.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Globale Fehlerbehandlung für die Web-Anwendung.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(InvoiceProcessingException.class)
    public String handleInvoiceProcessingException(InvoiceProcessingException ex, Model model) {
        log.error("Invoice processing error: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);
        
        model.addAttribute("errorCode", ex.getErrorCode());
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorDetails", ex.getDetails());
        
        return "error";
    }
    
    @ExceptionHandler(ValidationException.class)
    public String handleValidationException(ValidationException ex, Model model) {
        log.error("Validation error: {}", ex.getMessage(), ex);
        
        model.addAttribute("errorCode", "VALIDATION_ERROR");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("validationResult", ex.getValidationResult());
        
        return "error";
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, 
            RedirectAttributes redirectAttributes) {
        log.warn("File upload too large: {}", ex.getMessage());
        
        redirectAttributes.addFlashAttribute("error", "error.file.tooLarge");
        return "redirect:/";
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException(IllegalArgumentException ex, Model model) {
        log.warn("Bad request: {}", ex.getMessage());
        
        model.addAttribute("errorCode", "BAD_REQUEST");
        model.addAttribute("errorMessage", ex.getMessage());
        
        return "error";
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, Model model) {
        log.error("Unexpected error", ex);
        
        model.addAttribute("errorCode", "INTERNAL_ERROR");
        model.addAttribute("errorMessage", "Ein unerwarteter Fehler ist aufgetreten.");
        model.addAttribute("errorDetails", ex.getMessage());
        
        return "error";
    }
}
