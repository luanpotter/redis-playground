FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy only dependency definitions first for better caching
COPY gradle ./gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN ./gradlew --no-daemon dependencies --no-build-cache

# Now copy source code
COPY src ./src
RUN ./gradlew --no-daemon installDist --no-build-cache

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/install/kotlin-backend /app
EXPOSE 8080
ENTRYPOINT ["/app/bin/kotlin-backend"]
