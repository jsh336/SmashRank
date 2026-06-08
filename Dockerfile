# ========================================================
#  Dockerfile — SmashRank Spring Boot App
# ========================================================

# --- Fase 1: Build con Maven + JDK 21 ---
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copiar POM y descargar dependencias primero (cache de capas)
COPY pom.xml .
RUN mvn dependency:go-offline -B 2>/dev/null || true

# Copiar fuentes y compilar
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Fase 2: Imagen de ejecución mínima ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario no-root por seguridad
RUN addgroup -S smashrank && adduser -S smashrank -G smashrank
USER smashrank

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
