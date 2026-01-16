package de.zugferd.invoicetool.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Adressdaten für Verkäufer oder Käufer.
 */
public record Address(
    @NotBlank(message = "{validation.address.street.required}")
    @Size(max = 255)
    String street,
    
    @NotBlank(message = "{validation.address.city.required}")
    @Size(max = 100)
    String city,
    
    @NotBlank(message = "{validation.address.postalCode.required}")
    @Size(max = 20)
    String postalCode,
    
    @NotBlank(message = "{validation.address.country.required}")
    @Size(min = 2, max = 2, message = "{validation.address.country.iso}")
    String countryCode
) {
    /**
     * Kompakte Konstruktor-Validierung und Normalisierung.
     */
    public Address {
        countryCode = countryCode != null ? countryCode.toUpperCase() : null;
    }
    
    /**
     * Formatierte Adresse für Anzeige.
     */
    public String formatted() {
        return "%s, %s %s, %s".formatted(street, postalCode, city, countryCode);
    }
}
