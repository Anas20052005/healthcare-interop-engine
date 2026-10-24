# Multi-stage Dockerfile for Healthcare Interoperability Engine Backend

# Stage 1: Build JAR using Maven and OpenJDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Copy POM and download dependencies for caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build application jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal, secure JRE 17 runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as non-root user for security hardening
RUN groupadd -r appgroup && useradd -r -g appgroup -s /bin/false appuser

# Copy built JAR from builder stage
COPY --from=builder /workspace/target/healthcare-interop-engine-*.jar /app/app.jar

# Set ownership
RUN chown -R appuser:appgroup /app
USER appuser

# Expose backend port
EXPOSE 8080

# Configure JVM flags suitable for container environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
