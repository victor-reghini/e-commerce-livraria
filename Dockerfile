# ==============================================================================
# Multi-stage Dockerfile para E-commerce Backend (Java 21 LTS + Spring Boot 3)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Packaging
# ------------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Caching de dependências Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compilação e empacotamento da aplicação
COPY src ./src
RUN mvn clean package -DskipTests -B

# Extração de camadas do Spring Boot para inicialização ultra-rápida (Layered JAR)
RUN java -Djarmode=layertools -jar target/ecommerce-backend-*.jar extract

# ------------------------------------------------------------------------------
# Stage 2: Runtime Container Enxuto & Seguro
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

LABEL maintainer="Plataforma E-commerce Multi-Tenant"

# Instalação de utilitários mínimos e criação de usuário não-root
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget curl && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd -r appgroup && useradd -r -g appgroup -s /sbin/nologin appuser

WORKDIR /app

# Cópia das camadas extraídas do Spring Boot JAR
COPY --from=builder /build/dependencies/ ./
COPY --from=builder /build/spring-boot-loader/ ./
COPY --from=builder /build/snapshot-dependencies/ ./
COPY --from=builder /build/application/ ./

# Permissões do usuário não-root
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

# Flags otimizadas da JVM para containers e Project Loom Virtual Threads
ENV JAVA_OPTS="-XX:+UseG1GC \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.threads.virtual.enabled=true"

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q 'UP' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

