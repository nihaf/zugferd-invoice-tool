package de.zugferd.invoicetool.service;

import de.zugferd.invoicetool.config.AppConfig.ZugferdProperties;
import de.zugferd.invoicetool.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ZugferdGeneratorService.
 */
class ZugferdGeneratorServiceTest {
    
    @TempDir
    Path tempDir;
    
    private ZugferdGeneratorService zugferdGeneratorService;
    private PdfA3ConverterService pdfA3ConverterService;
    
    @BeforeEach
    void setUp() {
        ZugferdProperties properties = new ZugferdProperties();
        properties.setProfile("EN16931");
        properties.setVersion("2.3");
        
        pdfA3ConverterService = new PdfA3ConverterService();
        zugferdGeneratorService = new ZugferdGeneratorService(properties, pdfA3ConverterService);
    }
    
    @Test
    @DisplayName("Should create valid InvoiceMetadata with all fields")
    void shouldCreateValidInvoiceMetadata() {
        // Given
        var seller = createTestParty("Seller GmbH", "DE123456789");
        var buyer = createTestParty("Buyer AG", "DE987654321");
        var items = List.of(
            InvoiceItem.of("Consulting Services", 10, 150.00, 19.0),
            InvoiceItem.of("Development Work", 5, 200.00, 19.0)
        );
        
        // When
        var metadata = InvoiceMetadata.builder()
            .invoiceNumber("INV-2024-001")
            .issueDate(LocalDate.now())
            .dueDate(LocalDate.now().plusDays(30))
            .seller(seller)
            .buyer(buyer)
            .items(items)
            .currency(Currency.getInstance("EUR"))
            .paymentTerms("Zahlbar innerhalb von 30 Tagen")
            .build();
        
        // Then
        assertNotNull(metadata);
        assertEquals("INV-2024-001", metadata.invoiceNumber());
        assertEquals(2, metadata.items().size());
        assertEquals(seller, metadata.seller());
        assertEquals(buyer, metadata.buyer());
    }
    
    @Test
    @DisplayName("Should calculate correct totals")
    void shouldCalculateCorrectTotals() {
        // Given
        var items = List.of(
            new InvoiceItem("Item 1", new BigDecimal("2"), new BigDecimal("100.00"), 
                           new BigDecimal("19.00"), "C62"),
            new InvoiceItem("Item 2", new BigDecimal("3"), new BigDecimal("50.00"), 
                           new BigDecimal("19.00"), "C62")
        );
        
        var metadata = InvoiceMetadata.builder()
            .invoiceNumber("TEST-001")
            .issueDate(LocalDate.now())
            .seller(createTestParty("Seller", "DE123"))
            .buyer(createTestParty("Buyer", "DE456"))
            .items(items)
            .build();
        
        // When
        BigDecimal totalNet = metadata.totalNetAmount();
        BigDecimal totalVat = metadata.totalVatAmount();
        BigDecimal totalGross = metadata.totalGrossAmount();
        
        // Then
        // Item 1: 2 * 100 = 200, Item 2: 3 * 50 = 150, Total = 350
        assertEquals(new BigDecimal("350.00"), totalNet);
        // VAT: 350 * 0.19 = 66.50
        assertEquals(new BigDecimal("66.50"), totalVat);
        // Gross: 350 + 66.50 = 416.50
        assertEquals(new BigDecimal("416.50"), totalGross);
    }
    
    @Test
    @DisplayName("Should group VAT by rate")
    void shouldGroupVatByRate() {
        // Given
        var items = List.of(
            new InvoiceItem("Standard VAT Item", new BigDecimal("1"), new BigDecimal("100.00"), 
                           new BigDecimal("19.00"), "C62"),
            new InvoiceItem("Reduced VAT Item", new BigDecimal("1"), new BigDecimal("100.00"), 
                           new BigDecimal("7.00"), "C62")
        );
        
        var metadata = InvoiceMetadata.builder()
            .invoiceNumber("TEST-002")
            .issueDate(LocalDate.now())
            .seller(createTestParty("Seller", "DE123"))
            .buyer(createTestParty("Buyer", "DE456"))
            .items(items)
            .build();
        
        // When
        var vatBreakdown = metadata.vatBreakdown();
        
        // Then
        assertEquals(2, vatBreakdown.size());
        assertEquals(new BigDecimal("19.00"), vatBreakdown.get(new BigDecimal("19.00")));
        assertEquals(new BigDecimal("7.00"), vatBreakdown.get(new BigDecimal("7.00")));
    }
    
    @Test
    @DisplayName("InvoiceItem should calculate amounts correctly")
    void invoiceItemShouldCalculateAmountsCorrectly() {
        // Given
        var item = new InvoiceItem(
            "Test Product",
            new BigDecimal("5"),
            new BigDecimal("25.00"),
            new BigDecimal("19.00"),
            "C62"
        );
        
        // When/Then
        assertEquals(new BigDecimal("125.00"), item.netAmount());
        assertEquals(new BigDecimal("23.75"), item.vatAmount());
        assertEquals(new BigDecimal("148.75"), item.grossAmount());
    }
    
    @Test
    @DisplayName("Address should normalize country code to uppercase")
    void addressShouldNormalizeCountryCode() {
        // Given/When
        var address = new Address("Musterstr. 1", "Berlin", "10115", "de");
        
        // Then
        assertEquals("DE", address.countryCode());
    }
    
    @Test
    @DisplayName("BankDetails should normalize IBAN")
    void bankDetailsShouldNormalizeIban() {
        // Given/When
        var bankDetails = new BankDetails(
            "de89 3704 0044 0532 0130 00",
            "COBADEFFXXX",
            "Commerzbank",
            "Max Mustermann"
        );
        
        // Then
        assertEquals("DE89370400440532013000", bankDetails.iban());
        assertEquals("COBADEFFXXX", bankDetails.bic());
    }
    
    @Test
    @DisplayName("BankDetails should format IBAN correctly")
    void bankDetailsShouldFormatIban() {
        // Given
        var bankDetails = new BankDetails(
            "DE89370400440532013000",
            null, null, null
        );
        
        // When
        String formatted = bankDetails.formattedIban();
        
        // Then
        assertEquals("DE89 3704 0044 0532 0130 00", formatted);
    }
    
    @Test
    @DisplayName("Party should detect presence of VAT ID")
    void partyShouldDetectVatId() {
        // Given
        var partyWithVat = createTestParty("Company A", "DE123456789");
        var partyWithoutVat = new Party(
            "Company B",
            new Address("Street", "City", "12345", "DE"),
            null, null, null, null
        );
        
        // Then
        assertTrue(partyWithVat.hasVatId());
        assertFalse(partyWithoutVat.hasVatId());
    }
    
    // Helper method to create test party
    private Party createTestParty(String name, String vatId) {
        return new Party(
            name,
            new Address("Teststraße 1", "Berlin", "10115", "DE"),
            vatId,
            "test@example.com",
            "+49 30 12345678",
            "Max Mustermann"
        );
    }
}
