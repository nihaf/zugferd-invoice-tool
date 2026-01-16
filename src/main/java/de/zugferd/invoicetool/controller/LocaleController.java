package de.zugferd.invoicetool.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * REST Controller für den on-the-fly Sprachwechsel.
 */
@RestController
@RequestMapping("/api")
public class LocaleController {
    
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("de", "en");
    
    private final LocaleResolver localeResolver;
    
    public LocaleController(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }
    
    /**
     * Wechselt die Sprache und speichert sie im Cookie.
     *
     * @param lang Sprachcode (de/en)
     */
    @PostMapping("/locale")
    public ResponseEntity<Map<String, String>> changeLocale(
            @RequestParam("lang") String lang,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        if (!SUPPORTED_LANGUAGES.contains(lang.toLowerCase())) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "Unsupported language",
                    "supported", String.join(", ", SUPPORTED_LANGUAGES)
                ));
        }
        
        Locale newLocale = Locale.forLanguageTag(lang);
        localeResolver.setLocale(request, response, newLocale);
        
        return ResponseEntity.ok(Map.of(
            "language", lang,
            "locale", newLocale.toLanguageTag(),
            "message", "Language changed successfully"
        ));
    }
    
    /**
     * Gibt die aktuelle Sprache zurück.
     */
    @GetMapping("/locale")
    public ResponseEntity<Map<String, Object>> getCurrentLocale(HttpServletRequest request) {
        Locale currentLocale = localeResolver.resolveLocale(request);
        
        return ResponseEntity.ok(Map.of(
            "language", currentLocale.getLanguage(),
            "locale", currentLocale.toLanguageTag(),
            "displayName", currentLocale.getDisplayLanguage(currentLocale),
            "supported", SUPPORTED_LANGUAGES
        ));
    }
}
