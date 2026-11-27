# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Hardened non-root runtime stage (ISO/IEC 27001 A.8.9, A.8.25)
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system --gid 1001 appgroup \
    && useradd --system --uid 1001 --gid appgroup --shell /usr/sbin/nologin appuser

COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

USER appuser:appgroup

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
