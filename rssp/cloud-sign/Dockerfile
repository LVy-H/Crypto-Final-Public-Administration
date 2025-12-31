FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY rssp/cloud-sign/build/libs/cloud-sign-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
