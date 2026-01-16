package de.zugferd.invoicetool.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Partei (Verkäufer oder Käufer) einer Rechnung.
 */
public record Party(
    @NotBlank(message = "{validation.party.name.required}")
    @Size(max = 255)
    String name,
    
    @NotNull(message = "{validation.party.address.required}")
    @Valid
    Address address,
    
    @Size(max = 50)
    String vatId,
    
    @Email(message = "{validation.party.email.invalid}")
    @Size(max = 255)
    String email,
    
    @Size(max = 50)
    String phone,
    
    @Size(max = 100)
    String contactName
) {
    /**
     * Kompakte Konstruktor für VAT-ID Normalisierung.
     */
    public Party {
        vatId = vatId != null ? vatId.replaceAll("\\s+", "").toUpperCase() : null;
    }
    
    /**
     * Prüft ob eine USt-ID vorhanden ist.
     */
    public boolean hasVatId() {
        return vatId != null && !vatId.isBlank();
    }
}
