package de.zugferd.invoicetool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ZUGFeRD Invoice Tool - E-Rechnung Generator
 * 
 * Erstellt PDF/A-3 konforme E-Rechnungen nach dem ZUGFeRD-Standard.
 */
@SpringBootApplication
@EnableScheduling
public class ZugferdInvoiceToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZugferdInvoiceToolApplication.class, args);
    }
}
