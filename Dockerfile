# ===========================================
# ZUGFeRD Invoice Tool - Dockerfile
# Multi-stage build for optimized image size
# ===========================================

# Stage 1: Build
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src src

# Build the application
RUN ./gradlew build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:25-jre

# Labels
LABEL maintainer="ZUGFeRD Invoice Tool"
LABEL description="E-Invoice Generator with ZUGFeRD/Factur-X support"
LABEL version="1.0.0"

# Create non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

# Create directories for temp files
RUN mkdir -p /app/temp/uploads /app/temp/output && \
    chown -R appuser:appgroup /app

# Copy JAR from builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy sRGB ICC profile if available
# COPY --from=builder /app/src/main/resources/sRGB.icc /app/sRGB.icc

# Switch to non-root user
USER appuser

# Environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m --enable-preview"
ENV SPRING_PROFILES_ACTIVE=docker

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
