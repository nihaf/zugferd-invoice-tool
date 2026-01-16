package de.zugferd.invoicetool.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Komplette Metadaten einer E-Rechnung nach ZUGFeRD EN16931.
 */
public record InvoiceMetadata(
    @NotBlank(message = "{validation.invoice.number.required}")
    @Size(max = 50)
    String invoiceNumber,
    
    @NotNull(message = "{validation.invoice.issueDate.required}")
    LocalDate issueDate,
    
    LocalDate dueDate,
    
    @NotNull(message = "{validation.invoice.seller.required}")
    @Valid
    Party seller,
    
    @NotNull(message = "{validation.invoice.buyer.required}")
    @Valid
    Party buyer,
    
    @NotEmpty(message = "{validation.invoice.items.required}")
    @Valid
    List<InvoiceItem> items,
    
    @Valid
    BankDetails bankDetails,
    
    @Size(max = 500)
    String paymentTerms,
    
    @Size(max = 1000)
    String notes,
    
    @NotNull
    Currency currency,
    
    @Size(max = 50)
    String buyerReference,
    
    @Size(max = 50)
    String orderReference
) {
    /**
     * Kompakte Konstruktor mit Defaults.
     */
    public InvoiceMetadata {
        if (currency == null) {
            currency = Currency.getInstance("EUR");
        }
        if (items == null) {
            items = List.of();
        }
    }
    
    /**
     * Berechnet den Gesamtnettobetrag aller Positionen.
     */
    public BigDecimal totalNetAmount() {
        return items.stream()
            .map(InvoiceItem::netAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Berechnet den Gesamt-MwSt-Betrag.
     */
    public BigDecimal totalVatAmount() {
        return items.stream()
            .map(InvoiceItem::vatAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Berechnet den Gesamtbruttobetrag.
     */
    public BigDecimal totalGrossAmount() {
        return totalNetAmount().add(totalVatAmount());
    }
    
    /**
     * Gruppiert MwSt-Beträge nach Steuersatz.
     */
    public Map<BigDecimal, BigDecimal> vatBreakdown() {
        return items.stream()
            .collect(Collectors.groupingBy(
                InvoiceItem::vatRate,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    InvoiceItem::vatAmount,
                    BigDecimal::add
                )
            ));
    }
    
    /**
     * Gruppiert Nettobeträge nach Steuersatz.
     */
    public Map<BigDecimal, BigDecimal> netAmountsByVatRate() {
        return items.stream()
            .collect(Collectors.groupingBy(
                InvoiceItem::vatRate,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    InvoiceItem::netAmount,
                    BigDecimal::add
                )
            ));
    }
    
    /**
     * Prüft ob Zahlungsdaten vollständig sind.
     */
    public boolean hasPaymentDetails() {
        return bankDetails != null && bankDetails.iban() != null;
    }
    
    /**
     * Builder-Pattern für schrittweise Erstellung.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder-Klasse für InvoiceMetadata.
     */
    public static class Builder {
        private String invoiceNumber;
        private LocalDate issueDate;
        private LocalDate dueDate;
        private Party seller;
        private Party buyer;
        private List<InvoiceItem> items = List.of();
        private BankDetails bankDetails;
        private String paymentTerms;
        private String notes;
        private Currency currency = Currency.getInstance("EUR");
        private String buyerReference;
        private String orderReference;
        
        public Builder invoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }
        
        public Builder issueDate(LocalDate issueDate) {
            this.issueDate = issueDate;
            return this;
        }
        
        public Builder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }
        
        public Builder seller(Party seller) {
            this.seller = seller;
            return this;
        }
        
        public Builder buyer(Party buyer) {
            this.buyer = buyer;
            return this;
        }
        
        public Builder items(List<InvoiceItem> items) {
            this.items = items;
            return this;
        }
        
        public Builder bankDetails(BankDetails bankDetails) {
            this.bankDetails = bankDetails;
            return this;
        }
        
        public Builder paymentTerms(String paymentTerms) {
            this.paymentTerms = paymentTerms;
            return this;
        }
        
        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }
        
        public Builder currency(Currency currency) {
            this.currency = currency;
            return this;
        }
        
        public Builder buyerReference(String buyerReference) {
            this.buyerReference = buyerReference;
            return this;
        }
        
        public Builder orderReference(String orderReference) {
            this.orderReference = orderReference;
            return this;
        }
        
        public InvoiceMetadata build() {
            return new InvoiceMetadata(
                invoiceNumber, issueDate, dueDate, seller, buyer,
                items, bankDetails, paymentTerms, notes, currency,
                buyerReference, orderReference
            );
        }
    }
}
