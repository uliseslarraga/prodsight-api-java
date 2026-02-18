# ---------- build stage ----------
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy pom first for better layer caching
COPY pom.xml .
COPY src ./src

# Build fat jar (adjust if you use a different build profile)
RUN mvn -q -DskipTests package

# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Optional: run as non-root
RUN useradd -m appuser
USER appuser

# Copy built jar (Spring Boot default target/*.jar)
COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
