plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "de.zugferd"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Web Stack
    implementation(libs.bundles.spring.web)
    
    // ZUGFeRD / Mustang Library for E-Invoice Generation
    implementation(libs.mustang)

    // JAXB Runtime (required for ZUGFeRD XML generation on Java 11+)
    implementation(libs.jaxb.runtime)
    
    // PDF Handling
    implementation(libs.pdfbox)
    
    // VeraPDF for PDF/A-3 Validation
    implementation(libs.bundles.verapdf)
    
    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.commons.io)
    
    
    // Testing
    testImplementation(libs.bundles.testing)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "--enable-preview"
    ))
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}

tasks.bootRun {
    jvmArgs("--enable-preview")
}

springBoot {
    mainClass = "de.zugferd.invoicetool.ZugferdInvoiceToolApplication"
}
