# Stage 1: Build JAR file
FROM gradle:8.12-jdk24 AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN ./gradlew build -x test --no-daemon

# Stage 2: Run Application
FROM eclipse-temurin:24-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
