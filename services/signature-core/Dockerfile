FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY build/libs/signature-core-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
