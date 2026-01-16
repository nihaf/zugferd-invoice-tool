package de.zugferd.invoicetool.service;

import de.zugferd.invoicetool.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationService.
 */
class ValidationServiceTest {
    
    @TempDir
    Path tempDir;
    
    private ValidationService validationService;
    
    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }
    
    @Test
    @DisplayName("Should detect valid PDF header")
    void shouldDetectValidPdfHeader() throws IOException {
        // Given
        Path pdfFile = tempDir.resolve("test.pdf");
        // PDF header: %PDF-1.4
        Files.write(pdfFile, new byte[]{'%', 'P', 'D', 'F', '-', '1', '.', '4'});
        
        // When
        boolean isValid = validationService.isValidPdf(pdfFile);
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    @DisplayName("Should detect invalid PDF header")
    void shouldDetectInvalidPdfHeader() throws IOException {
        // Given
        Path notPdfFile = tempDir.resolve("test.txt");
        Files.write(notPdfFile, "This is not a PDF".getBytes());
        
        // When
        boolean isValid = validationService.isValidPdf(notPdfFile);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    @DisplayName("Should return false for non-existent file")
    void shouldReturnFalseForNonExistentFile() {
        // Given
        Path nonExistent = tempDir.resolve("does-not-exist.pdf");
        
        // When
        boolean isValid = validationService.isValidPdf(nonExistent);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    @DisplayName("Should return false for empty file")
    void shouldReturnFalseForEmptyFile() throws IOException {
        // Given
        Path emptyFile = tempDir.resolve("empty.pdf");
        Files.createFile(emptyFile);
        
        // When
        boolean isValid = validationService.isValidPdf(emptyFile);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    @DisplayName("ValidationResult success should have correct properties")
    void validationResultSuccessShouldHaveCorrectProperties() {
        // Given/When
        ValidationResult result = ValidationResult.success("PDF/A-3B", 150);
        
        // Then
        assertTrue(result.valid());
        assertEquals("PDF/A-3B", result.profileName());
        assertEquals(150, result.processingTimeMs());
        assertEquals(0, result.errorCount());
        assertEquals(0, result.warningCount());
        assertFalse(result.hasWarnings());
    }
    
    @Test
    @DisplayName("ValidationResult failure should contain errors")
    void validationResultFailureShouldContainErrors() {
        // Given
        var errors = java.util.List.of(
            new ValidationResult.ValidationError(
                "6.1.2", "PDF/A-3", "6.1.2", 
                "The document does not conform to PDF/A-3", 
                "/Root"
            )
        );
        var warnings = java.util.List.of(
            new ValidationResult.ValidationWarning("7.1", "Minor issue detected")
        );
        
        // When
        ValidationResult result = ValidationResult.failure("PDF/A-3B", errors, warnings, 200);
        
        // Then
        assertFalse(result.valid());
        assertEquals(1, result.errorCount());
        assertEquals(1, result.warningCount());
        assertTrue(result.hasWarnings());
        assertEquals("6.1.2", result.errors().get(0).ruleId());
    }
}
