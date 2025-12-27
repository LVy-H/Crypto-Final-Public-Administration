FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY build/libs/identity-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
