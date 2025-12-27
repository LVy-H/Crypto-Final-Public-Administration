FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY build/libs/validation-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
