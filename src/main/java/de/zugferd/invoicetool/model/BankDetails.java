package de.zugferd.invoicetool.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Bankverbindung für Zahlungen.
 */
public record BankDetails(
    @NotBlank(message = "{validation.bank.iban.required}")
    @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{4,30}$", message = "{validation.bank.iban.invalid}")
    String iban,
    
    @Pattern(regexp = "^[A-Z]{6}[A-Z0-9]{2,5}$", message = "{validation.bank.bic.invalid}")
    String bic,
    
    @Size(max = 100)
    String bankName,
    
    @Size(max = 100)
    String accountHolder
) {
    /**
     * Kompakte Konstruktor für IBAN-Normalisierung.
     */
    public BankDetails {
        iban = iban != null ? iban.replaceAll("\\s+", "").toUpperCase() : null;
        bic = bic != null ? bic.replaceAll("\\s+", "").toUpperCase() : null;
    }
    
    /**
     * Formatierte IBAN mit Leerzeichen alle 4 Zeichen.
     */
    public String formattedIban() {
        if (iban == null) return "";
        return iban.replaceAll("(.{4})", "$1 ").trim();
    }
}
