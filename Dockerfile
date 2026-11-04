FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle chmod +x gradlew && ./gradlew --no-daemon bootJar generatorJar

FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=build /app/build/libs/app.jar app.jar
COPY --from=build /app/build/libs/generator.jar generator.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
