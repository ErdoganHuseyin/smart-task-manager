# ---------- Stage 1: build the jar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy only the build files first. As long as pom.xml does not change,
# Docker reuses this cached layer and does not download dependencies again.
COPY mvnw pom.xml ./
COPY .mvn .mvn
# On Windows, Git may check out mvnw with CRLF line endings, which Linux cannot run.
# Strip the \r characters and make sure the script is executable.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -B -q dependency:go-offline

# Now the source code (changes often, so it comes last).
COPY src src
# Tests already run in CI; here we only package the app.
RUN ./mvnw -B -q package -DskipTests \
    && cp target/*.jar app.jar

# ---------- Stage 2: the small image that actually runs ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Do not run as root: if the app is ever compromised, the attacker has fewer rights.
RUN useradd --system --uid 1001 spring
USER spring

COPY --from=build /workspace/app.jar app.jar

EXPOSE 8080
# Use at most 75% of the container's memory limit for the Java heap.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
