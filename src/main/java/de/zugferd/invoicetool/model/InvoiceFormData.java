package de.zugferd.invoicetool.model;

import de.zugferd.invoicetool.config.AppConfig;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * Formular-Binding-Klasse für Thymeleaf.
 * Mutable Klasse für einfaches Form-Binding.
 */
public class InvoiceFormData {
    
    // Rechnungsdaten
    @NotBlank(message = "{validation.invoice.number.required}")
    private String invoiceNumber;
    
    @NotNull(message = "{validation.invoice.issueDate.required}")
    private LocalDate issueDate;
    
    private LocalDate dueDate;
    
    private String currency = "EUR";
    
    private String buyerReference;
    private String orderReference;
    private String paymentTerms;
    private String notes;
    
    // Verkäufer
    @NotBlank(message = "{validation.party.name.required}")
    private String sellerName;
    private String sellerStreet;
    private String sellerCity;
    private String sellerPostalCode;
    private String sellerCountryCode = "DE";
    private String sellerVatId;
    private String sellerEmail;
    private String sellerPhone;
    private String sellerContactName;
    
    // Käufer
    @NotBlank(message = "{validation.party.name.required}")
    private String buyerName;
    private String buyerStreet;
    private String buyerCity;
    private String buyerPostalCode;
    private String buyerCountryCode = "DE";
    private String buyerVatId;
    private String buyerEmail;
    private String buyerPhone;
    private String buyerContactName;
    
    // Bankverbindung
    private String bankIban;
    private String bankBic;
    private String bankName;
    private String bankAccountHolder;
    
    // Rechnungspositionen
    private List<ItemFormData> items = new ArrayList<>();
    
    /**
     * Formular-Daten für eine Rechnungsposition.
     */
    public static class ItemFormData {
        @NotBlank
        private String description;
        
        @NotNull
        @DecimalMin("0.001")
        private BigDecimal quantity = BigDecimal.ONE;
        
        @NotNull
        @DecimalMin("0.00")
        private BigDecimal unitPrice = BigDecimal.ZERO;
        
        @NotNull
        private BigDecimal vatRate = new BigDecimal("19.00");
        
        private String unit = "C62"; // Stück
        
        // Getters and Setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        
        public BigDecimal getVatRate() { return vatRate; }
        public void setVatRate(BigDecimal vatRate) { this.vatRate = vatRate; }
        
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        
        public InvoiceItem toInvoiceItem() {
            return new InvoiceItem(description, quantity, unitPrice, vatRate, unit);
        }
    }
    
    /**
     * Konvertiert Formulardaten zu InvoiceMetadata Record.
     */
    public InvoiceMetadata toInvoiceMetadata() {
        var sellerAddress = new Address(sellerStreet, sellerCity, sellerPostalCode, sellerCountryCode);
        var seller = new Party(sellerName, sellerAddress, sellerVatId, sellerEmail, sellerPhone, sellerContactName);
        
        var buyerAddress = new Address(buyerStreet, buyerCity, buyerPostalCode, buyerCountryCode);
        var buyer = new Party(buyerName, buyerAddress, buyerVatId, buyerEmail, buyerPhone, buyerContactName);
        
        BankDetails bankDetails = null;
        if (bankIban != null && !bankIban.isBlank()) {
            bankDetails = new BankDetails(bankIban, bankBic, bankName, bankAccountHolder);
        }
        
        var invoiceItems = items.stream()
            .map(ItemFormData::toInvoiceItem)
            .toList();
        
        return new InvoiceMetadata(
            invoiceNumber,
            issueDate,
            dueDate,
            seller,
            buyer,
            invoiceItems,
            bankDetails,
            paymentTerms,
            notes,
            Currency.getInstance(currency),
            buyerReference,
            orderReference
        );
    }
    
    /**
     * Fügt eine neue leere Position hinzu.
     */
    public void addItem() {
        items.add(new ItemFormData());
    }
    
    /**
     * Entfernt eine Position nach Index.
     */
    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }
    
    /**
     * Initialisiert mit einer Standard-Position.
     */
    public void initializeDefaults() {
        initializeDefaults(null);
    }

    /**
     * Initialisiert mit einer Standard-Position und optional mit Vorgaben aus der Konfiguration.
     *
     * @param invoiceDefaults Vorgaben aus application.yml oder null
     */
    public void initializeDefaults(AppConfig.InvoiceDefaults invoiceDefaults) {
        if (items.isEmpty()) {
            addItem();
        }

        // Rechnungsdaten
        if (issueDate == null) {
            issueDate = LocalDate.now();
        }
        if (dueDate == null) {
            dueDate = issueDate.plusDays(30);
        }

        // Vorgaben aus Konfiguration anwenden, falls vorhanden
        if (invoiceDefaults != null) {
            // Seller defaults
            AppConfig.InvoiceDefaults.SellerDefaults sellerDefaults = invoiceDefaults.getSeller();
            if (sellerDefaults != null) {
                if (sellerDefaults.getName() != null) {
                    sellerName = sellerDefaults.getName();
                }
                if (sellerDefaults.getVatId() != null) {
                    sellerVatId = sellerDefaults.getVatId();
                }
                if (sellerDefaults.getStreet() != null) {
                    sellerStreet = sellerDefaults.getStreet();
                }
                if (sellerDefaults.getPostalCode() != null) {
                    sellerPostalCode = sellerDefaults.getPostalCode();
                }
                if (sellerDefaults.getCity() != null) {
                    sellerCity = sellerDefaults.getCity();
                }
                if (sellerDefaults.getCountryCode() != null) {
                    sellerCountryCode = sellerDefaults.getCountryCode();
                }
                if (sellerDefaults.getEmail() != null) {
                    sellerEmail = sellerDefaults.getEmail();
                }
                if (sellerDefaults.getPhone() != null) {
                    sellerPhone = sellerDefaults.getPhone();
                }
            }

            // Payment defaults
            AppConfig.InvoiceDefaults.PaymentDefaults paymentDefaults = invoiceDefaults.getPayment();
            if (paymentDefaults != null) {
                if (paymentDefaults.getIban() != null) {
                    bankIban = paymentDefaults.getIban();
                }
                if (paymentDefaults.getBic() != null) {
                    bankBic = paymentDefaults.getBic();
                }
                if (paymentDefaults.getBankName() != null) {
                    bankName = paymentDefaults.getBankName();
                }
                if (paymentDefaults.getTerms() != null) {
                    paymentTerms = paymentDefaults.getTerms();
                }
            }

            // Invoice defaults
            AppConfig.InvoiceDefaults.InvoiceSettings invoiceSettings = invoiceDefaults.getInvoice();
            if (invoiceSettings != null && invoiceSettings.getCurrency() != null) {
                currency = invoiceSettings.getCurrency();
            }
        }
    }
    
    // Getters and Setters
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getBuyerReference() { return buyerReference; }
    public void setBuyerReference(String buyerReference) { this.buyerReference = buyerReference; }
    
    public String getOrderReference() { return orderReference; }
    public void setOrderReference(String orderReference) { this.orderReference = orderReference; }
    
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    
    public String getSellerStreet() { return sellerStreet; }
    public void setSellerStreet(String sellerStreet) { this.sellerStreet = sellerStreet; }
    
    public String getSellerCity() { return sellerCity; }
    public void setSellerCity(String sellerCity) { this.sellerCity = sellerCity; }
    
    public String getSellerPostalCode() { return sellerPostalCode; }
    public void setSellerPostalCode(String sellerPostalCode) { this.sellerPostalCode = sellerPostalCode; }
    
    public String getSellerCountryCode() { return sellerCountryCode; }
    public void setSellerCountryCode(String sellerCountryCode) { this.sellerCountryCode = sellerCountryCode; }
    
    public String getSellerVatId() { return sellerVatId; }
    public void setSellerVatId(String sellerVatId) { this.sellerVatId = sellerVatId; }
    
    public String getSellerEmail() { return sellerEmail; }
    public void setSellerEmail(String sellerEmail) { this.sellerEmail = sellerEmail; }
    
    public String getSellerPhone() { return sellerPhone; }
    public void setSellerPhone(String sellerPhone) { this.sellerPhone = sellerPhone; }
    
    public String getSellerContactName() { return sellerContactName; }
    public void setSellerContactName(String sellerContactName) { this.sellerContactName = sellerContactName; }
    
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    
    public String getBuyerStreet() { return buyerStreet; }
    public void setBuyerStreet(String buyerStreet) { this.buyerStreet = buyerStreet; }
    
    public String getBuyerCity() { return buyerCity; }
    public void setBuyerCity(String buyerCity) { this.buyerCity = buyerCity; }
    
    public String getBuyerPostalCode() { return buyerPostalCode; }
    public void setBuyerPostalCode(String buyerPostalCode) { this.buyerPostalCode = buyerPostalCode; }
    
    public String getBuyerCountryCode() { return buyerCountryCode; }
    public void setBuyerCountryCode(String buyerCountryCode) { this.buyerCountryCode = buyerCountryCode; }
    
    public String getBuyerVatId() { return buyerVatId; }
    public void setBuyerVatId(String buyerVatId) { this.buyerVatId = buyerVatId; }
    
    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String buyerEmail) { this.buyerEmail = buyerEmail; }
    
    public String getBuyerPhone() { return buyerPhone; }
    public void setBuyerPhone(String buyerPhone) { this.buyerPhone = buyerPhone; }
    
    public String getBuyerContactName() { return buyerContactName; }
    public void setBuyerContactName(String buyerContactName) { this.buyerContactName = buyerContactName; }
    
    public String getBankIban() { return bankIban; }
    public void setBankIban(String bankIban) { this.bankIban = bankIban; }
    
    public String getBankBic() { return bankBic; }
    public void setBankBic(String bankBic) { this.bankBic = bankBic; }
    
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    
    public String getBankAccountHolder() { return bankAccountHolder; }
    public void setBankAccountHolder(String bankAccountHolder) { this.bankAccountHolder = bankAccountHolder; }
    
    public List<ItemFormData> getItems() { return items; }
    public void setItems(List<ItemFormData> items) { this.items = items; }
}
