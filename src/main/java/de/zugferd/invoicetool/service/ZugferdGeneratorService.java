package de.zugferd.invoicetool.service;

import de.zugferd.invoicetool.config.AppConfig.ZugferdProperties;
import de.zugferd.invoicetool.exception.InvoiceProcessingException;
import de.zugferd.invoicetool.model.InvoiceItem;
import de.zugferd.invoicetool.model.InvoiceMetadata;
import org.mustangproject.ZUGFeRD.ZUGFeRDExporterFromA3;
import org.mustangproject.ZUGFeRD.Profiles;
import org.mustangproject.ZUGFeRD.IExportableTransaction;
import org.mustangproject.ZUGFeRD.IZUGFeRDExportableProduct;
import org.mustangproject.ZUGFeRD.IZUGFeRDExportableItem;
import org.mustangproject.ZUGFeRD.IZUGFeRDExportableTradeParty;
import org.mustangproject.ZUGFeRD.IZUGFeRDExportableContact;
import org.mustangproject.ZUGFeRD.IZUGFeRDAllowanceCharge;
import org.mustangproject.ZUGFeRD.IZUGFeRDPaymentTerms;
import org.mustangproject.ZUGFeRD.IZUGFeRDTradeSettlementPayment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.time.ZoneId;
import java.util.List;

/**
 * Service für die Generierung von ZUGFeRD E-Rechnungen.
 * Verwendet die Mustang-Bibliothek für die XML-Generierung und PDF-Einbettung.
 */
@Service
public class ZugferdGeneratorService {
    
    private static final Logger log = LoggerFactory.getLogger(ZugferdGeneratorService.class);
    
    private final ZugferdProperties zugferdProperties;
    private final PdfA3ConverterService pdfA3ConverterService;
    
    public ZugferdGeneratorService(ZugferdProperties zugferdProperties, 
                                    PdfA3ConverterService pdfA3ConverterService) {
        this.zugferdProperties = zugferdProperties;
        this.pdfA3ConverterService = pdfA3ConverterService;
    }
    
    /**
     * Generiert eine ZUGFeRD E-Rechnung aus einer PDF und Metadaten.
     *
     * @param inputPdfPath Pfad zur Original-PDF
     * @param outputPdfPath Pfad für die E-Rechnung
     * @param metadata Rechnungsmetadaten
     */
    public void generateInvoice(Path inputPdfPath, Path outputPdfPath, InvoiceMetadata metadata) {
        log.info("Generating ZUGFeRD invoice: {} -> {}", inputPdfPath, outputPdfPath);
        
        try {
            // Zuerst PDF/A-3 konvertieren
            Path pdfA3Path = pdfA3ConverterService.convertToPdfA3(inputPdfPath);
            
            // Output-Verzeichnis erstellen
            Files.createDirectories(outputPdfPath.getParent());
            
            // ZUGFeRD-Export durchführen
            ZUGFeRDExporterFromA3 exporter = new ZUGFeRDExporterFromA3();
            exporter.setProducer("ZUGFeRD Invoice Tool");
            exporter.setCreator("ZUGFeRD Invoice Tool v1.0");
            exporter.setZUGFeRDVersion(2);
            exporter.setProfile(Profiles.getByName("EN16931"));
            
            exporter.load(pdfA3Path.toString());
            exporter.setTransaction(createTransaction(metadata));
            exporter.export(outputPdfPath.toString());
            
            log.info("Successfully generated ZUGFeRD invoice: {}", outputPdfPath);
            
            // Temporäre PDF/A-3 Datei löschen falls sie separat erstellt wurde
            if (!pdfA3Path.equals(inputPdfPath)) {
                Files.deleteIfExists(pdfA3Path);
            }
            
        } catch (IOException e) {
            log.error("Failed to generate ZUGFeRD invoice", e);
            throw InvoiceProcessingException.zugferdGenerationError(e);
        } catch (Exception e) {
            log.error("Unexpected error during ZUGFeRD generation", e);
            throw InvoiceProcessingException.zugferdGenerationError(e);
        }
    }
    
    /**
     * Erstellt eine ZUGFeRD-Transaction aus den Metadaten.
     */
    private IExportableTransaction createTransaction(InvoiceMetadata metadata) {
        return new IExportableTransaction() {
            
            @Override
            public String getNumber() {
                return metadata.invoiceNumber();
            }
            
            @Override
            public Date getIssueDate() {
                return Date.from(metadata.issueDate()
                    .atStartOfDay(ZoneId.systemDefault()).toInstant());
            }
            
            @Override
            public Date getDueDate() {
                if (metadata.dueDate() != null) {
                    return Date.from(metadata.dueDate()
                        .atStartOfDay(ZoneId.systemDefault()).toInstant());
                }
                return null;
            }
            
            @Override
            public IZUGFeRDExportableTradeParty getSender() {
                return createTradeParty(metadata.seller());
            }
            
            @Override
            public IZUGFeRDExportableTradeParty getRecipient() {
                return createTradeParty(metadata.buyer());
            }
            
            @Override
            public IZUGFeRDExportableItem[] getZFItems() {
                return metadata.items().stream()
                    .map(item -> createItem(item))
                    .toArray(IZUGFeRDExportableItem[]::new);
            }
            
            @Override
            public String getCurrency() {
                return metadata.currency().getCurrencyCode();
            }
            
            @Override
            public String getPaymentTermDescription() {
                return metadata.paymentTerms();
            }
            
            @Override
            public String getReferenceNumber() {
                return metadata.buyerReference();
            }
            
            @Override
            public IZUGFeRDExportableTradeParty getPayee() {
                return null; // Same as sender
            }
            
            @Override
            public String getOwnOrganisationFullPlaintextInfo() {
                return metadata.notes();
            }

            @Override
            public Date getDeliveryDate() {
                // Use issue date as delivery date if not specified
                return getIssueDate();
            }

            @Override
            public IZUGFeRDTradeSettlementPayment[] getTradeSettlementPayment() {
                if (metadata.bankDetails() != null) {
                    return new IZUGFeRDTradeSettlementPayment[] {
                        new IZUGFeRDTradeSettlementPayment() {
                            @Override
                            public String getOwnIBAN() {
                                return metadata.bankDetails().iban();
                            }

                            @Override
                            public String getOwnBIC() {
                                return metadata.bankDetails().bic();
                            }

                            @Override
                            public String getAccountName() {
                                return metadata.bankDetails().bankName();
                            }
                        }
                    };
                }
                return null;
            }
        };
    }
    
    /**
     * Erstellt eine TradeParty aus Party-Daten.
     */
    private IZUGFeRDExportableTradeParty createTradeParty(de.zugferd.invoicetool.model.Party party) {
        return new IZUGFeRDExportableTradeParty() {
            @Override
            public String getName() {
                return party.name();
            }
            
            @Override
            public String getStreet() {
                return party.address().street();
            }
            
            @Override
            public String getZIP() {
                return party.address().postalCode();
            }
            
            @Override
            public String getLocation() {
                return party.address().city();
            }
            
            @Override
            public String getCountry() {
                return party.address().countryCode();
            }
            
            @Override
            public String getVATID() {
                return party.vatId();
            }
            
            @Override
            public String getEmail() {
                return party.email();
            }
            
            @Override
            public IZUGFeRDExportableContact getContact() {
                if (party.contactName() != null || party.phone() != null || party.email() != null) {
                    return new IZUGFeRDExportableContact() {
                        @Override
                        public String getName() {
                            return party.contactName();
                        }
                        
                        @Override
                        public String getPhone() {
                            return party.phone();
                        }
                        
                        @Override
                        public String getEMail() {
                            return party.email();
                        }
                        
                        @Override
                        public String getFax() {
                            return null;
                        }
                    };
                }
                return null;
            }
        };
    }
    
    /**
     * Erstellt ein ZUGFeRD-Item aus einer Rechnungsposition.
     */
    private IZUGFeRDExportableItem createItem(InvoiceItem item) {
        return new IZUGFeRDExportableItem() {
            @Override
            public IZUGFeRDExportableProduct getProduct() {
                return new IZUGFeRDExportableProduct() {
                    @Override
                    public String getName() {
                        return item.description();
                    }
                    
                    @Override
                    public String getDescription() {
                        return item.description();
                    }
                    
                    @Override
                    public String getUnit() {
                        return item.unit();
                    }
                    
                    @Override
                    public BigDecimal getVATPercent() {
                        return item.vatRate();
                    }
                };
            }
            
            @Override
            public BigDecimal getQuantity() {
                return item.quantity();
            }
            
            @Override
            public BigDecimal getPrice() {
                return item.unitPrice();
            }

            @Override
            public BigDecimal getBasisQuantity() {
                return BigDecimal.ONE;
            }
            
            @Override
            public IZUGFeRDAllowanceCharge[] getItemAllowances() {
                return new IZUGFeRDAllowanceCharge[0];
            }
            
            @Override
            public IZUGFeRDAllowanceCharge[] getItemCharges() {
                return new IZUGFeRDAllowanceCharge[0];
            }
        };
    }
}
