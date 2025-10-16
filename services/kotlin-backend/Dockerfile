FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew --no-daemon clean installDist
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/install/kotlin-backend /app
EXPOSE 8080
ENTRYPOINT ["/app/bin/kotlin-backend"]
