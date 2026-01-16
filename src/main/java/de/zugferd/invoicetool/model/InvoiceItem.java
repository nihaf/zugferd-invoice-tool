package de.zugferd.invoicetool.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Eine einzelne Rechnungsposition.
 */
public record InvoiceItem(
    @NotBlank(message = "{validation.item.description.required}")
    @Size(max = 500)
    String description,
    
    @NotNull(message = "{validation.item.quantity.required}")
    @DecimalMin(value = "0.001", message = "{validation.item.quantity.positive}")
    BigDecimal quantity,
    
    @NotNull(message = "{validation.item.unitPrice.required}")
    @DecimalMin(value = "0.00", message = "{validation.item.unitPrice.positive}")
    BigDecimal unitPrice,
    
    @NotNull(message = "{validation.item.vatRate.required}")
    @DecimalMin(value = "0.00", message = "{validation.item.vatRate.positive}")
    BigDecimal vatRate,
    
    @NotBlank(message = "{validation.item.unit.required}")
    @Size(max = 10)
    String unit
) {
    /**
     * Standardeinheiten gemäß UN/ECE Recommendation 20.
     */
    public static final String UNIT_PIECE = "C62";      // Stück
    public static final String UNIT_HOUR = "HUR";       // Stunde
    public static final String UNIT_DAY = "DAY";        // Tag
    public static final String UNIT_KILOGRAM = "KGM";   // Kilogramm
    public static final String UNIT_METER = "MTR";      // Meter
    public static final String UNIT_LITER = "LTR";      // Liter
    
    /**
     * Berechnet den Nettobetrag (Menge × Einzelpreis).
     */
    public BigDecimal netAmount() {
        return quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Berechnet den MwSt-Betrag.
     */
    public BigDecimal vatAmount() {
        return netAmount()
            .multiply(vatRate)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Berechnet den Bruttobetrag.
     */
    public BigDecimal grossAmount() {
        return netAmount().add(vatAmount());
    }
    
    /**
     * Factory-Methode für einfache Erstellung mit Standard-Einheit.
     */
    public static InvoiceItem of(String description, double quantity, double unitPrice, double vatRate) {
        return new InvoiceItem(
            description,
            BigDecimal.valueOf(quantity),
            BigDecimal.valueOf(unitPrice),
            BigDecimal.valueOf(vatRate),
            UNIT_PIECE
        );
    }
}
