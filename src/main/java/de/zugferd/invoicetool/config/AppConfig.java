package de.zugferd.invoicetool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Zentrale Anwendungskonfiguration.
 */
@Configuration
public class AppConfig {
    
    /**
     * Virtual Thread Executor für asynchrone Operationen (Java 21+).
     * Nutzt Virtual Threads für effiziente I/O-Operationen.
     */
    @Bean
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    
    /**
     * Anwendungseinstellungen aus application.yml.
     */
    @Bean
    @ConfigurationProperties(prefix = "zugferd")
    public ZugferdProperties zugferdProperties() {
        return new ZugferdProperties();
    }

    /**
     * Invoice Defaults Eigenschaften.
     */
    @Bean
    @ConfigurationProperties(prefix = "invoice.defaults")
    public InvoiceDefaults invoiceDefaults() {
        return new InvoiceDefaults();
    }
    
    /**
     * Konfigurationsklasse für ZUGFeRD-spezifische Einstellungen.
     */
    public static class ZugferdProperties {
        private String profile = "EN16931";
        private String version = "2.3";
        private long maxFileSizeBytes = 10 * 1024 * 1024; // 10 MB
        private int sessionTimeoutMinutes = 30;
        private boolean validateOnGeneration = true;

        public String getProfile() { return profile; }
        public void setProfile(String profile) { this.profile = profile; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public long getMaxFileSizeBytes() { return maxFileSizeBytes; }
        public void setMaxFileSizeBytes(long maxFileSizeBytes) { this.maxFileSizeBytes = maxFileSizeBytes; }

        public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
        public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }

        public boolean isValidateOnGeneration() { return validateOnGeneration; }
        public void setValidateOnGeneration(boolean validateOnGeneration) { this.validateOnGeneration = validateOnGeneration; }
    }

    /**
     * Konfigurationsklasse für Rechnungsvorgaben.
     */
    public static class InvoiceDefaults {
        private SellerDefaults seller = new SellerDefaults();
        private PaymentDefaults payment = new PaymentDefaults();
        private InvoiceSettings invoice = new InvoiceSettings();

        public SellerDefaults getSeller() { return seller; }
        public void setSeller(SellerDefaults seller) { this.seller = seller; }

        public PaymentDefaults getPayment() { return payment; }
        public void setPayment(PaymentDefaults payment) { this.payment = payment; }

        public InvoiceSettings getInvoice() { return invoice; }
        public void setInvoice(InvoiceSettings invoice) { this.invoice = invoice; }

        public static class SellerDefaults {
            private String name;
            private String vatId;
            private String street;
            private String postalCode;
            private String city;
            private String countryCode;
            private String email;
            private String phone;

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }

            public String getVatId() { return vatId; }
            public void setVatId(String vatId) { this.vatId = vatId; }

            public String getStreet() { return street; }
            public void setStreet(String street) { this.street = street; }

            public String getPostalCode() { return postalCode; }
            public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

            public String getCity() { return city; }
            public void setCity(String city) { this.city = city; }

            public String getCountryCode() { return countryCode; }
            public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

            public String getEmail() { return email; }
            public void setEmail(String email) { this.email = email; }

            public String getPhone() { return phone; }
            public void setPhone(String phone) { this.phone = phone; }
        }

        public static class PaymentDefaults {
            private String iban;
            private String bic;
            private String bankName;
            private String terms;

            public String getIban() { return iban; }
            public void setIban(String iban) { this.iban = iban; }

            public String getBic() { return bic; }
            public void setBic(String bic) { this.bic = bic; }

            public String getBankName() { return bankName; }
            public void setBankName(String bankName) { this.bankName = bankName; }

            public String getTerms() { return terms; }
            public void setTerms(String terms) { this.terms = terms; }
        }

        public static class InvoiceSettings {
            private String currency = "EUR";

            public String getCurrency() { return currency; }
            public void setCurrency(String currency) { this.currency = currency; }
        }
    }
}
