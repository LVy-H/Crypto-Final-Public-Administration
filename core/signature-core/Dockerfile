FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY core/signature-core/build/libs/signature-core-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
