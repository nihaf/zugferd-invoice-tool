package de.zugferd.invoicetool.service;

import de.zugferd.invoicetool.model.ValidationResult;
import de.zugferd.invoicetool.model.ValidationResult.ValidationError;
import de.zugferd.invoicetool.model.ValidationResult.ValidationWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.verapdf.core.EncryptedPdfException;
import org.verapdf.core.ModelParsingException;
import org.verapdf.core.ValidationException;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.VeraPDFFoundry;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.TestAssertion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Service für die PDF/A-3 Validierung mit VeraPDF.
 */
@Service
public class ValidationService {
    
    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);
    
    static {
        // VeraPDF Foundry initialisieren
        VeraGreenfieldFoundryProvider.initialise();
    }
    
    /**
     * Validiert eine PDF-Datei gegen den PDF/A-3B Standard.
     *
     * @param pdfPath Pfad zur zu validierenden PDF
     * @return Validierungsergebnis
     */
    public ValidationResult validatePdfA3(Path pdfPath) {
        log.info("Validating PDF/A-3 conformance: {}", pdfPath);
        
        long startTime = System.currentTimeMillis();
        
        try (var inputStream = Files.newInputStream(pdfPath)) {
            
            VeraPDFFoundry foundry = Foundries.defaultInstance();
            PDFAFlavour flavour = PDFAFlavour.PDFA_3_B;
            
            try (PDFAParser parser = foundry.createParser(inputStream, flavour)) {
                PDFAValidator validator = foundry.createValidator(flavour, false);
                org.verapdf.pdfa.results.ValidationResult result = validator.validate(parser);
                
                long processingTime = System.currentTimeMillis() - startTime;
                
                if (result.isCompliant()) {
                    log.info("PDF/A-3 validation successful in {}ms", processingTime);
                    return ValidationResult.success(flavour.getId(), processingTime);
                } else {
                    List<ValidationError> errors = extractErrors(result);
                    List<ValidationWarning> warnings = extractWarnings(result);
                    
                    log.warn("PDF/A-3 validation failed with {} errors and {} warnings", 
                        errors.size(), warnings.size());
                    
                    return ValidationResult.failure(
                        flavour.getId(), 
                        errors, 
                        warnings, 
                        processingTime
                    );
                }
            }
            
        } catch (ModelParsingException e) {
            log.error("Failed to parse PDF for validation", e);
            return createErrorResult("PARSE_ERROR", 
                "PDF konnte nicht geparst werden: " + e.getMessage(),
                System.currentTimeMillis() - startTime);
                
        } catch (EncryptedPdfException e) {
            log.error("PDF is encrypted", e);
            return createErrorResult("ENCRYPTED_PDF", 
                "PDF ist verschlüsselt und kann nicht validiert werden",
                System.currentTimeMillis() - startTime);
                
        } catch (ValidationException e) {
            log.error("Validation error", e);
            return createErrorResult("VALIDATION_ERROR", 
                "Validierungsfehler: " + e.getMessage(),
                System.currentTimeMillis() - startTime);
                
        } catch (IOException e) {
            log.error("IO error during validation", e);
            return createErrorResult("IO_ERROR", 
                "Fehler beim Lesen der Datei: " + e.getMessage(),
                System.currentTimeMillis() - startTime);
        }
    }
    
    /**
     * Extrahiert Fehler aus dem VeraPDF-Ergebnis.
     */
    private List<ValidationError> extractErrors(org.verapdf.pdfa.results.ValidationResult result) {
        List<ValidationError> errors = new ArrayList<>();
        
        for (TestAssertion assertion : result.getTestAssertions()) {
            if (assertion.getStatus() == TestAssertion.Status.FAILED) {
                errors.add(new ValidationError(
                    assertion.getRuleId().getClause(),
                    assertion.getRuleId().getSpecification().getId(),
                    assertion.getRuleId().getClause(),
                    assertion.getMessage(),
                    assertion.getLocation() != null ? assertion.getLocation().getContext() : null
                ));
            }
        }
        
        return errors;
    }
    
    /**
     * Extrahiert Warnungen aus dem VeraPDF-Ergebnis.
     */
    private List<ValidationWarning> extractWarnings(org.verapdf.pdfa.results.ValidationResult result) {
        List<ValidationWarning> warnings = new ArrayList<>();

        for (TestAssertion assertion : result.getTestAssertions()) {
            if (assertion.getStatus() == TestAssertion.Status.UNKNOWN) {
                warnings.add(new ValidationWarning(
                    assertion.getRuleId().getClause(),
                    assertion.getMessage()
                ));
            }
        }

        return warnings;
    }
    
    /**
     * Erstellt ein Fehler-Validierungsergebnis.
     */
    private ValidationResult createErrorResult(String ruleId, String message, long processingTime) {
        return ValidationResult.failure(
            "PDF/A-3B",
            List.of(new ValidationError(ruleId, "VeraPDF", ruleId, message, null)),
            List.of(),
            processingTime
        );
    }
    
    /**
     * Schnelle Prüfung ob die Datei ein gültiges PDF ist.
     */
    public boolean isValidPdf(Path pdfPath) {
        try (var inputStream = Files.newInputStream(pdfPath)) {
            byte[] header = new byte[5];
            int read = inputStream.read(header);
            
            // PDF Header prüfen: %PDF-
            return read == 5 && 
                   header[0] == '%' && 
                   header[1] == 'P' && 
                   header[2] == 'D' && 
                   header[3] == 'F' && 
                   header[4] == '-';
                   
        } catch (IOException e) {
            log.warn("Could not verify PDF header", e);
            return false;
        }
    }
}
