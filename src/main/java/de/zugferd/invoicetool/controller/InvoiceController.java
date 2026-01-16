package de.zugferd.invoicetool.controller;

import de.zugferd.invoicetool.model.InvoiceFormData;
import de.zugferd.invoicetool.model.ProcessingStatus;
import de.zugferd.invoicetool.service.InvoiceService;
import de.zugferd.invoicetool.service.StorageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller für die E-Rechnungs-Web-UI.
 */
@Controller
public class InvoiceController {
    
    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);
    
    private final StorageService storageService;
    private final InvoiceService invoiceService;
    
    public InvoiceController(StorageService storageService, InvoiceService invoiceService) {
        this.storageService = storageService;
        this.invoiceService = invoiceService;
    }
    
    /**
     * Zeigt die Startseite mit Upload-Formular.
     */
    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }
    
    /**
     * Verarbeitet den PDF-Upload.
     */
    @PostMapping("/upload")
    public String uploadPdf(@RequestParam("file") MultipartFile file,
                            RedirectAttributes redirectAttributes) {
        
        log.info("Received file upload: {} ({} bytes)", 
            file.getOriginalFilename(), file.getSize());
        
        try {
            String sessionId = storageService.createSession(file);
            return "redirect:/metadata/" + sessionId;
            
        } catch (Exception e) {
            log.error("Upload failed", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }
    
    /**
     * Zeigt das Metadaten-Eingabeformular.
     */
    @GetMapping("/metadata/{sessionId}")
    public String showMetadataForm(@PathVariable String sessionId, Model model) {
        
        ProcessingStatus status = storageService.getStatusOrThrow(sessionId);
        
        // Prüfen ob Upload-Status
        if (!(status instanceof ProcessingStatus.Uploaded uploaded)) {
            return "redirect:/result/" + sessionId;
        }
        
        // Formular-Daten initialisieren
        InvoiceFormData formData = new InvoiceFormData();
        formData.initializeDefaults();
        
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("originalFilename", uploaded.originalFilename());
        model.addAttribute("fileSize", formatFileSize(uploaded.fileSizeBytes()));
        model.addAttribute("invoiceForm", formData);
        model.addAttribute("units", getAvailableUnits());
        model.addAttribute("vatRates", getCommonVatRates());
        model.addAttribute("countries", getCountryCodes());
        
        return "metadata-form";
    }
    
    /**
     * Verarbeitet das Metadaten-Formular und generiert die E-Rechnung.
     */
    @PostMapping("/generate/{sessionId}")
    public String generateInvoice(@PathVariable String sessionId,
                                  @Valid @ModelAttribute("invoiceForm") InvoiceFormData formData,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            ProcessingStatus status = storageService.getStatusOrThrow(sessionId);
            if (status instanceof ProcessingStatus.Uploaded uploaded) {
                model.addAttribute("sessionId", sessionId);
                model.addAttribute("originalFilename", uploaded.originalFilename());
                model.addAttribute("fileSize", formatFileSize(uploaded.fileSizeBytes()));
                model.addAttribute("units", getAvailableUnits());
                model.addAttribute("vatRates", getCommonVatRates());
                model.addAttribute("countries", getCountryCodes());
            }
            return "metadata-form";
        }
        
        try {
            var metadata = formData.toInvoiceMetadata();
            invoiceService.generateInvoice(sessionId, metadata);
            return "redirect:/result/" + sessionId;
            
        } catch (Exception e) {
            log.error("Invoice generation failed", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/metadata/" + sessionId;
        }
    }
    
    /**
     * Zeigt das Ergebnis der E-Rechnung-Generierung.
     */
    @GetMapping("/result/{sessionId}")
    public String showResult(@PathVariable String sessionId, Model model) {

        ProcessingStatus status = storageService.getStatusOrThrow(sessionId);

        model.addAttribute("sessionId", sessionId);
        model.addAttribute("status", status);
        model.addAttribute("statusType", status.getClass().getSimpleName());
        
        // Pattern Matching für Status-spezifische Daten
        switch (status) {
            case ProcessingStatus.Completed completed -> {
                model.addAttribute("validationResult", completed.validationResult());
                model.addAttribute("metadata", completed.metadata());
                model.addAttribute("canDownload", true);
            }
            case ProcessingStatus.Failed failed -> {
                model.addAttribute("errorMessage", failed.errorMessage());
                model.addAttribute("errorDetails", failed.errorDetails());
                model.addAttribute("canDownload", false);
            }
            case ProcessingStatus.Processing _ -> {
                model.addAttribute("canDownload", false);
            }
            default -> {
                return "redirect:/metadata/" + sessionId;
            }
        }
        
        return "result";
    }
    
    /**
     * Fügt eine neue Rechnungsposition hinzu (AJAX).
     */
    @PostMapping("/metadata/{sessionId}/add-item")
    public String addItem(@PathVariable String sessionId,
                         @ModelAttribute("invoiceForm") InvoiceFormData formData,
                         Model model) {
        formData.addItem();
        
        ProcessingStatus status = storageService.getStatusOrThrow(sessionId);
        if (status instanceof ProcessingStatus.Uploaded uploaded) {
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("originalFilename", uploaded.originalFilename());
            model.addAttribute("fileSize", formatFileSize(uploaded.fileSizeBytes()));
        }
        model.addAttribute("units", getAvailableUnits());
        model.addAttribute("vatRates", getCommonVatRates());
        model.addAttribute("countries", getCountryCodes());
        
        return "metadata-form";
    }
    
    /**
     * Entfernt eine Rechnungsposition (AJAX).
     */
    @PostMapping("/metadata/{sessionId}/remove-item/{index}")
    public String removeItem(@PathVariable String sessionId,
                            @PathVariable int index,
                            @ModelAttribute("invoiceForm") InvoiceFormData formData,
                            Model model) {
        formData.removeItem(index);
        
        ProcessingStatus status = storageService.getStatusOrThrow(sessionId);
        if (status instanceof ProcessingStatus.Uploaded uploaded) {
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("originalFilename", uploaded.originalFilename());
            model.addAttribute("fileSize", formatFileSize(uploaded.fileSizeBytes()));
        }
        model.addAttribute("units", getAvailableUnits());
        model.addAttribute("vatRates", getCommonVatRates());
        model.addAttribute("countries", getCountryCodes());
        
        return "metadata-form";
    }
    
    /**
     * Löscht eine Session und zugehörige Dateien.
     */
    @PostMapping("/cleanup/{sessionId}")
    public String cleanup(@PathVariable String sessionId,
                         RedirectAttributes redirectAttributes) {
        invoiceService.cleanup(sessionId);
        redirectAttributes.addFlashAttribute("message", "session.deleted");
        return "redirect:/";
    }
    
    // === Helper Methods ===
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
    
    private java.util.Map<String, String> getAvailableUnits() {
        return java.util.Map.of(
            "C62", "Stück",
            "HUR", "Stunde",
            "DAY", "Tag",
            "KGM", "Kilogramm",
            "MTR", "Meter",
            "LTR", "Liter"
        );
    }
    
    private java.util.List<String> getCommonVatRates() {
        return java.util.List.of("0.00", "7.00", "19.00");
    }
    
    private java.util.Map<String, String> getCountryCodes() {
        return java.util.Map.ofEntries(
            java.util.Map.entry("DE", "Deutschland"),
            java.util.Map.entry("AT", "Österreich"),
            java.util.Map.entry("CH", "Schweiz"),
            java.util.Map.entry("FR", "Frankreich"),
            java.util.Map.entry("IT", "Italien"),
            java.util.Map.entry("NL", "Niederlande"),
            java.util.Map.entry("BE", "Belgien"),
            java.util.Map.entry("PL", "Polen"),
            java.util.Map.entry("ES", "Spanien"),
            java.util.Map.entry("GB", "Vereinigtes Königreich")
        );
    }
}
