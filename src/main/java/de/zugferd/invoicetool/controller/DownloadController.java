package de.zugferd.invoicetool.controller;

import de.zugferd.invoicetool.service.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.StandardCharsets;

/**
 * Controller für den Download der generierten E-Rechnungen.
 */
@Controller
public class DownloadController {
    
    private static final Logger log = LoggerFactory.getLogger(DownloadController.class);
    
    private final InvoiceService invoiceService;
    
    public DownloadController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }
    
    /**
     * Lädt die generierte E-Rechnung herunter.
     */
    @GetMapping("/download/{sessionId}")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable String sessionId) {
        log.info("Download requested for session: {}", sessionId);
        
        byte[] pdfContent = invoiceService.downloadInvoice(sessionId);
        String filename = invoiceService.getDownloadFilename(sessionId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename(filename, StandardCharsets.UTF_8)
            .build());
        headers.setContentLength(pdfContent.length);
        
        log.info("Sending PDF download: {} ({} bytes)", filename, pdfContent.length);
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfContent);
    }
    
    /**
     * Zeigt eine Vorschau der E-Rechnung im Browser.
     */
    @GetMapping("/preview/{sessionId}")
    public ResponseEntity<byte[]> previewInvoice(@PathVariable String sessionId) {
        log.info("Preview requested for session: {}", sessionId);
        
        byte[] pdfContent = invoiceService.downloadInvoice(sessionId);
        String filename = invoiceService.getDownloadFilename(sessionId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
            .filename(filename, StandardCharsets.UTF_8)
            .build());
        headers.setContentLength(pdfContent.length);
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfContent);
    }
}
