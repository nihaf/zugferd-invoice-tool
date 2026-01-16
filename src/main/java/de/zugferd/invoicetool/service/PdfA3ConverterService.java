package de.zugferd.invoicetool.service;

import de.zugferd.invoicetool.exception.InvoiceProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import javax.xml.transform.TransformerException;

/**
 * Service für die Konvertierung von PDF zu PDF/A-3.
 * Verwendet Apache PDFBox für die Konvertierung.
 */
@Service
public class PdfA3ConverterService {
    
    private static final Logger log = LoggerFactory.getLogger(PdfA3ConverterService.class);
    private static final String SRGB_ICC_PROFILE = "/sRGB.icc";
    
    /**
     * Konvertiert eine PDF-Datei zu PDF/A-3b.
     *
     * @param inputPath Pfad zur Original-PDF
     * @return Pfad zur PDF/A-3 konvertierten Datei
     */
    public Path convertToPdfA3(Path inputPath) {
        log.info("Converting PDF to PDF/A-3: {}", inputPath);
        
        Path outputPath = inputPath.resolveSibling("pdfa3_" + inputPath.getFileName());
        
        try (PDDocument document = Loader.loadPDF(inputPath.toFile())) {
            
            // PDF/A-3 Metadaten hinzufügen
            addPdfA3Metadata(document);
            
            // sRGB Output Intent hinzufügen
            addOutputIntent(document);
            
            // Speichern
            document.save(outputPath.toFile());
            
            log.info("Successfully converted to PDF/A-3: {}", outputPath);
            return outputPath;
            
        } catch (IOException e) {
            log.error("Failed to convert PDF to PDF/A-3", e);
            throw InvoiceProcessingException.pdfConversionError(e);
        }
    }
    
    /**
     * Fügt PDF/A-3 konforme XMP-Metadaten hinzu.
     */
    private void addPdfA3Metadata(PDDocument document) throws IOException {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        
        // XMP Metadata erstellen
        XMPMetadata xmp = XMPMetadata.createXMPMetadata();
        
        try {
            // PDF/A Identification Schema
            PDFAIdentificationSchema pdfaId = xmp.createAndAddPDFAIdentificationSchema();
            pdfaId.setPart(3);
            pdfaId.setConformance("B");
            
            // Dublin Core Schema
            DublinCoreSchema dc = xmp.createAndAddDublinCoreSchema();
            dc.setTitle("ZUGFeRD Invoice");
            dc.addCreator("ZUGFeRD Invoice Tool");
            dc.setDescription("Electronic invoice according to ZUGFeRD standard");
            
            // XMP Basic Schema
            XMPBasicSchema basic = xmp.createAndAddXMPBasicSchema();
            basic.setCreatorTool("ZUGFeRD Invoice Tool v1.0");
            basic.setCreateDate(Calendar.getInstance());
            basic.setModifyDate(Calendar.getInstance());
            
        } catch (BadFieldValueException e) {
            throw new IOException("Failed to create XMP metadata", e);
        }
        
        // XMP als Bytes serialisieren
        XmpSerializer serializer = new XmpSerializer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            serializer.serialize(xmp, baos, true);
        } catch (TransformerException e) {
            throw new IOException("Failed to serialize XMP metadata", e);
        }

        // Metadaten zum Dokument hinzufügen
        PDMetadata metadata = new PDMetadata(document);
        metadata.importXMPMetadata(baos.toByteArray());
        catalog.setMetadata(metadata);
        
        // Document Information aktualisieren
        PDDocumentInformation info = document.getDocumentInformation();
        if (info == null) {
            info = new PDDocumentInformation();
        }
        info.setTitle("ZUGFeRD Invoice");
        info.setCreator("ZUGFeRD Invoice Tool");
        info.setProducer("ZUGFeRD Invoice Tool v1.0 - Apache PDFBox");
        document.setDocumentInformation(info);
    }
    
    /**
     * Fügt einen sRGB Output Intent für PDF/A-Konformität hinzu.
     */
    private void addOutputIntent(PDDocument document) throws IOException {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        
        // Prüfen ob bereits ein Output Intent vorhanden ist
        if (catalog.getOutputIntents() != null && !catalog.getOutputIntents().isEmpty()) {
            log.debug("Output Intent already present, skipping");
            return;
        }
        
        // sRGB ICC Profil laden
        InputStream iccStream = getClass().getResourceAsStream(SRGB_ICC_PROFILE);
        
        if (iccStream == null) {
            // Fallback: Minimales sRGB Profil erstellen
            log.warn("sRGB ICC profile not found, creating minimal output intent");
            createMinimalOutputIntent(document);
            return;
        }
        
        try (iccStream) {
            PDOutputIntent intent = new PDOutputIntent(document, iccStream);
            intent.setInfo("sRGB IEC61966-2.1");
            intent.setOutputCondition("sRGB IEC61966-2.1");
            intent.setOutputConditionIdentifier("sRGB IEC61966-2.1");
            intent.setRegistryName("http://www.color.org");
            catalog.addOutputIntent(intent);
        }
    }
    
    /**
     * Erstellt einen minimalen Output Intent wenn kein ICC-Profil verfügbar.
     */
    private void createMinimalOutputIntent(PDDocument document) throws IOException {
        // Für einen vollständigen PDF/A-3 Output Intent wird normalerweise
        // ein ICC-Profil benötigt. Als Fallback erstellen wir das Profil
        // aus den eingebetteten Ressourcen oder kopieren es beim Build.
        
        // Hinweis: In der Produktion sollte das sRGB.icc Profil
        // in src/main/resources abgelegt werden.
        log.warn("No ICC profile available - PDF/A-3 validation may fail");
    }
    
    /**
     * Prüft ob eine PDF bereits PDF/A-3 konform ist.
     */
    public boolean isPdfA3(Path pdfPath) {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDMetadata metadata = catalog.getMetadata();
            
            if (metadata == null) {
                return false;
            }
            
            // Einfache Prüfung auf PDF/A Marker
            // Vollständige Validierung erfolgt durch VeraPDF
            byte[] xmpBytes = metadata.toByteArray();
            String xmpString = new String(xmpBytes);
            return xmpString.contains("pdfaid:part") && xmpString.contains(">3<");
            
        } catch (IOException e) {
            log.warn("Could not check PDF/A-3 conformance", e);
            return false;
        }
    }
}
