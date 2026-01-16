package de.zugferd.invoicetool.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller für den Sprachwechsel.
 * Der tatsächliche Sprachwechsel wird durch den LocaleChangeInterceptor durchgeführt,
 * dieser Controller leitet nur auf die Referrer-Seite zurück.
 */
@Controller
public class LocaleController {

    /**
     * Wechselt die Sprache anhand des lang-Parameters und leitet auf die vorherige Seite zurück.
     * Der lang-Parameter wird vom LocaleChangeInterceptor verarbeitet.
     *
     * @param lang Sprachcode (de/en)
     * @param request HTTP-Request für Referrer
     * @return Redirect zur Referrer-Seite oder zur Startseite
     */
    @GetMapping("/locale")
    public String changeLocale(
            @RequestParam(value = "lang", defaultValue = "de") String lang,
            HttpServletRequest request) {

        // Referrer ermitteln oder zur Startseite zurückkehren
        String referrer = request.getHeader("referer");
        if (referrer != null && !referrer.isEmpty()) {
            return "redirect:" + referrer;
        }
        return "redirect:/";
    }
}
