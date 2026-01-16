package de.zugferd.invoicetool.controller;

import de.zugferd.invoicetool.model.ProcessingStatus;
import de.zugferd.invoicetool.service.InvoiceService;
import de.zugferd.invoicetool.service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for InvoiceController.
 */
@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private StorageService storageService;
    
    @MockitoBean
    private InvoiceService invoiceService;
    
    @Test
    @DisplayName("Should display index page")
    void shouldDisplayIndexPage() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"));
    }
    
    @Test
    @DisplayName("Should handle PDF upload and redirect to metadata form")
    void shouldHandlePdfUpload() throws Exception {
        // Given
        String sessionId = "test-session-123";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-invoice.pdf",
            "application/pdf",
            "%PDF-1.4 test content".getBytes()
        );
        
        when(storageService.createSession(any())).thenReturn(sessionId);
        
        // When/Then
        mockMvc.perform(multipart("/upload").file(file))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/metadata/" + sessionId));
    }
    
    @Test
    @DisplayName("Should display metadata form for uploaded session")
    void shouldDisplayMetadataForm() throws Exception {
        // Given
        String sessionId = "test-session-456";
        var uploadedStatus = new ProcessingStatus.Uploaded(
            sessionId,
            Instant.now(),
            Path.of("/tmp/test.pdf"),
            "invoice.pdf",
            12345L
        );
        
        when(storageService.getStatusOrThrow(sessionId)).thenReturn(uploadedStatus);
        
        // When/Then
        mockMvc.perform(get("/metadata/{sessionId}", sessionId))
            .andExpect(status().isOk())
            .andExpect(view().name("metadata-form"))
            .andExpect(model().attributeExists("invoiceForm"))
            .andExpect(model().attributeExists("sessionId"));
    }
    
    @Test
    @DisplayName("Should redirect to result for completed session")
    void shouldRedirectToResultForCompletedSession() throws Exception {
        // Given
        String sessionId = "completed-session";
        var completedStatus = new ProcessingStatus.Completed(
            sessionId,
            Instant.now(),
            Path.of("/tmp/output.pdf"),
            de.zugferd.invoicetool.model.ValidationResult.success("PDF/A-3B", 100),
            null
        );
        
        when(storageService.getStatusOrThrow(sessionId)).thenReturn(completedStatus);
        
        // When/Then
        mockMvc.perform(get("/metadata/{sessionId}", sessionId))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/result/" + sessionId));
    }
}
