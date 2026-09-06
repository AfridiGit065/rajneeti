# ============================================================
#  RAJNEETI – Multi-Stage Dockerfile
#  Produces a minimal, non-root production image.
#
#  Build:  docker build -t rajneeti-backend:latest .
#  Run:    docker run -p 8080:8080 \
#            -e DB_HOST=mysql \
#            -e DB_PORT=3306 \
#            -e DB_NAME=rajneeti_db \
#            -e DB_USERNAME=rajneeti_user \
#            -e DB_PASSWORD=your_password \
#            -e JWT_SECRET=your_base64_encoded_secret \
#            rajneeti-backend:latest
# ============================================================

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Cache dependencies separately from source for faster layer cache hits
COPY pom.xml .
COPY .mvn/ .mvn/
# Uncomment if using the Maven wrapper:
# COPY mvnw .
# RUN chmod +x mvnw

# Download dependencies (offline cache layer)
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B --no-transfer-progress 2>/dev/null || true

# Copy source and build
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -B --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as a non-root user
RUN addgroup -S rajneeti && adduser -S rajneeti -G rajneeti

WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=builder /build/target/rajneeti-backend-*.jar app.jar

# Create logs directory owned by app user
RUN mkdir -p logs && chown rajneeti:rajneeti logs

USER rajneeti

# Expose application port
EXPOSE 8080

# ── JVM Tuning ────────────────────────────────────────────────────────────────
# Container-aware heap sizing; adjust -Xmx for your deployment environment.
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom \
               -Dfile.encoding=UTF-8 \
               -Duser.timezone=UTC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# ── Healthcheck ───────────────────────────────────────────────────────────────
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/health || exit 1
