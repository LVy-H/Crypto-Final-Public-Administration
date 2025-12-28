FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY build/libs/api-gateway-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
