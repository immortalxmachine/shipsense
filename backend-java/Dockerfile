# ---- Build stage: compile + package with the Maven wrapper ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy the wrapper first and pre-fetch dependencies (better layer caching).
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Copy sources and build the jar (tests run in CI, not during the image build).
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# ---- Run stage: slim JRE with just the jar ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
